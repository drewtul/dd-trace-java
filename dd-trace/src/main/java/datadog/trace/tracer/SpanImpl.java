package datadog.trace.tracer;

import datadog.trace.api.DDTags;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Concrete implementation of a span
 */
public class SpanImpl implements Span {

  private final TraceImpl trace;

  private final SpanContext context;
  private final Timestamp startTimestamp;

  /* Note: some fields are volatile so we could make getters non synchronized.
  Alternatively we could make getters synchronized, but this may create more contention.
   */
  private volatile Long durationNano = null;

  private volatile String name;
  private volatile String resource;
  private volatile String service;

  private volatile String type;
  private volatile boolean errored = false;

  private final Map<String, Object> meta = new HashMap<>();

  private final List<Interceptor> interceptors;

  /**
   * Create a span with the a specific startTimestamp timestamp.
   *
   * @param trace The trace to associate this span with.
   * @param parentContext identifies the parent of this span. May be null.
   * @param startTimestamp timestamp when this span was started.
   */
  SpanImpl(final TraceImpl trace, final SpanContext parentContext, final Timestamp startTimestamp) {
    this.trace = trace;

    context = SpanContextImpl.fromParent(parentContext);
    this.startTimestamp = startTimestamp;
    service = trace.getTracer().getDefaultServiceName();
    interceptors = trace.getTracer().getInterceptors();

    for (final Interceptor interceptor : interceptors) {
      interceptor.spanStarted(this);
    }
  }

  @Override
  public Trace getTrace() {
    return trace;
  }

  @Override
  public synchronized void finish() {
    if (isFinished()) {
      reportUsageError("Attempted to finish span that is already finished: %s", this);
    } else {
      finishSpan(startTimestamp.getDuration(), false);
    }
  }

  @Override
  public synchronized void finish(final long finishTimestampNanoseconds) {
    if (isFinished()) {
      reportUsageError("Attempted to finish span that is already finish: %s", this);
    } else {
      finishSpan(startTimestamp.getDuration(finishTimestampNanoseconds), false);
    }
  }

  @Override
  public Timestamp getStartTimestamp() {
    return startTimestamp;
  }

  @Override
  public boolean isFinished() {
    return durationNano != null;
  }

  @Override
  public SpanContext getContext() {
    return context;
  }

  @Override
  public Object getMeta(final String key) {
    return meta.get(key);
  }

  protected synchronized void setMeta(final String key, final Object value) {
    if (isFinished()) {
      reportSetterUsageError("meta value " + key);
    } else {
      if (value == null) {
        meta.remove(key);
      } else {
        meta.put(key, value);
      }
    }
  }

  @Override
  public void setMeta(final String key, final String value) {
    setMeta(key, (Object) value);
  }

  @Override
  public void setMeta(final String key, final boolean value) {
    setMeta(key, (Object) value);
  }

  @Override
  public void setMeta(final String key, final Number value) {
    setMeta(key, (Object) value);
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public synchronized void setName(final String name) {
    if (isFinished()) {
      reportSetterUsageError("name");
    } else {
      this.name = name;
    }
  }

  @Override
  public String getResource() {
    return resource;
  }

  @Override
  public void setResource(final String resource) {
    if (isFinished()) {
      reportSetterUsageError("resource");
    } else {
      this.resource = resource;
    }
  }

  @Override
  public String getService() {
    return service;
  }

  @Override
  public void setService(final String service) {
    if (isFinished()) {
      reportSetterUsageError("service");
    } else {
      this.service = service;
    }
  }

  @Override
  public String getType() {
    return type;
  }

  @Override
  public synchronized void setType(final String type) {
    if (isFinished()) {
      reportSetterUsageError("type");
    } else {
      this.type = type;
    }
  }

  @Override
  public boolean isErrored() {
    return errored;
  }

  @Override
  public synchronized void attachThrowable(final Throwable throwable) {
    if (isFinished()) {
      reportSetterUsageError("throwable");
    } else {
      setErrored(true);

      setMeta(DDTags.ERROR_MSG, throwable.getMessage());
      setMeta(DDTags.ERROR_TYPE, throwable.getClass().getName());

      final StringWriter errorString = new StringWriter();
      throwable.printStackTrace(new PrintWriter(errorString));
      setMeta(DDTags.ERROR_STACK, errorString.toString());
    }
  }

  @Override
  public synchronized void setErrored(final boolean errored) {
    if (isFinished()) {
      reportSetterUsageError("errored");
    } else {
      this.errored = errored;
    }
  }

  @Override
  protected synchronized void finalize() {
    if (!isFinished()) {
      reportUsageError(
        "Finishing span due to GC, this will prevent trace from being reported: %s", this);
      finishSpan(startTimestamp.getDuration(), true);
    }
  }

  private void finishSpan(final long duration, final boolean fromGC) {
    // Run interceptors in 'reverse' order
    for (int i = interceptors.size() - 1; i >= 0; i--) {
      interceptors.get(i).spanStarted(this);
    }
    durationNano = duration;
    trace.finishSpan(this, fromGC);
  }

  private void reportUsageError(final String message, final Object... args) {
    trace.getTracer().reportError(message);
  }

  private void reportSetterUsageError(final String fieldName) {
    reportUsageError("Attempted to set '%s' when span is already finished: %s", fieldName, this);
  }
}
