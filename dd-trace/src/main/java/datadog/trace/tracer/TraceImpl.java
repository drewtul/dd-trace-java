package datadog.trace.tracer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

public class TraceImpl implements Trace {

  /* We use weakly referenced sets to track 'in-flight' spans and continuations. We use span/continuation's
  finalizer to notify trace that span/continuation is being GCed.
  If any part of the trace (span or continuation) has been was finished (closed) via GC then trace would be
  marked as 'invlaid' and will not be reported the the backend. Instead only writer's counter would be incremented.
  This allows us not to report traces that have wrong timing information.
  Note: instead of using {@link WeakHashMap} we may want to consider using more fancy implementations from
  {@link datadog.trace.agent.tooling.WeakMapSuppliers}. If we do this care should be taken to avoid creating
  cleanup threads per trace.
   */
  private final Set<Span> inFlightSpans =
    Collections.newSetFromMap(new WeakHashMap<Span, Boolean>());
  private final Set<Continuation> inFlightContinuations =
    Collections.newSetFromMap(new WeakHashMap<Continuation, Boolean>());

  /** Strong refs to spans which are closed */
  private final List<Span> finishedSpans = new ArrayList();

  private final Tracer tracer;
  private final Clock clock;
  private final Span rootSpan;
  private boolean invalid = false;
  private boolean finished = false;

  /**
   * Create a new Trace.
   *
   * @param tracer the Tracer to apply settings from.
   */
  TraceImpl(
    final Tracer tracer,
    final SpanContext rootSpanParentContext,
    final Timestamp rootSpanStartTimestamp) {
    this.tracer = tracer;
    clock = rootSpanStartTimestamp.getClock();
    rootSpan = new SpanImpl(this, rootSpanParentContext, rootSpanStartTimestamp);
    inFlightSpans.add(rootSpan);
  }

  @Override
  public Tracer getTracer() {
    return tracer;
  }

  @Override
  public Span getRootSpan() {
    return rootSpan;
  }

  @Override
  public Timestamp createCurrentTimestamp() {
    return clock.createCurrentTimestamp();
  }

  @Override
  public synchronized Span createSpan(
    final SpanContext parentContext, final Timestamp startTimestamp) {
    checkTraceFinished("create span");
    if (parentContext == null) {
      throw new TraceException("Got null parent context, trace: " + this);
    }
    if (!parentContext.getTraceId().equals(rootSpan.getContext().getTraceId())) {
      throw new TraceException(
        String.format(
          "Wrong trace id when creating a span. Got %s, expected %s",
          parentContext.getTraceId(), rootSpan.getContext().getTraceId()));
    }
    final Span span = new SpanImpl(this, parentContext, startTimestamp);
    inFlightSpans.add(span);
    return span;
  }

  @Override
  public synchronized Continuation createContinuation(final Span span) {
    checkTraceFinished("create continuation");
    if (span == null) {
      throw new TraceException("Got null parent span, trace: " + this);
    }
    if (!span.getContext().getTraceId().equals(rootSpan.getContext().getTraceId())) {
      throw new TraceException(
        String.format(
          "Wrong trace id when creating a span. Got %s, expected %s",
          span.getContext().getTraceId(), rootSpan.getContext().getTraceId()));
    }
    final Continuation continuation = new ContinuationImpl(this, span);
    inFlightContinuations.add(continuation);
    return continuation;
  }

  synchronized void finishSpan(final Span span, final boolean invalid) {
    checkTraceFinished("finish span");
    if (!inFlightSpans.contains(span)) {
      tracer.reportError("Trace doesn't contain continuation to finish: %s, trace: %s", span, this);
      return;
    }
    if (invalid) {
      this.invalid = true;
    }
    inFlightSpans.remove(span);
    finishedSpans.add(span);
    checkAndWriteTrace();
  }

  synchronized void finishContinuation(final Continuation continuation, final boolean invalid) {
    checkTraceFinished("finish continuation");
    if (!inFlightContinuations.contains(continuation)) {
      tracer.reportError(
        "Trace doesn't contain continuation to finish: %s, trace: %s", continuation, this);
      return;
    }
    if (invalid) {
      this.invalid = true;
    }
    inFlightContinuations.remove(continuation);
    checkAndWriteTrace();
  }

  private void checkAndWriteTrace() {
    if (inFlightSpans.isEmpty() && inFlightContinuations.isEmpty()) {
      if (invalid) {
        tracer.getWriter().incrementTraceCount();
      } else {
        final Trace trace = runInterceptorsBeforeTraceWritten(this);
        if (trace != null && tracer.getSampler().sample(trace)) {
          tracer.getWriter().write(trace);
        }
      }
      finished = true;
    }
  }

  private Trace runInterceptorsBeforeTraceWritten(Trace trace) {
    final List<Interceptor> interceptors = tracer.getInterceptors();
    // Run interceptors in 'reverse' order
    for (int i = interceptors.size() - 1; i >= 0; i--) {
      trace = interceptors.get(i).beforeTraceWritten(trace);
      if (trace == null) {
        break;
      }
    }
    return trace;
  }

  private void checkTraceFinished(final String action) {
    if (finished) {
      tracer.reportError("Cannot %s, trace has already been finished: %s", action, this);
    }
  }
}
