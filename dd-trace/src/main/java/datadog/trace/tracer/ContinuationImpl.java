package datadog.trace.tracer;

/** Concrete implementation of a continuation */
public class ContinuationImpl implements Continuation {
  private final TraceImpl trace;
  private final Span span;
  private boolean closed = false;

  ContinuationImpl(final TraceImpl trace, final Span span) {
    this.trace = trace;
    this.span = span;
  }

  @Override
  public synchronized void close() {
    if (closed) {
      reportUsageError("Attempted to close continuation that is already closed: %s", this);
    } else {
      closed = true;
      trace.finishContinuation(this, false);
    }
  }

  @Override
  public Span getSpan() {
    return span;
  }

  @Override
  protected synchronized void finalize() {
    if (!closed) {
      reportUsageError(
          "Closing continuation due to GC, this will prevent trace from being reported: %s", trace);
      trace.finishContinuation(this, true);
    }
  }

  private void reportUsageError(final String message, final Object... args) {
    trace.getTracer().reportError(message);
  }
}
