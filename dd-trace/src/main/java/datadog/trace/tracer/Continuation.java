package datadog.trace.tracer;

/**
 * Continuations are used to prevent a trace from reporting without creating a span.
 *
 * <p>All spans are thread safe. * *
 *
 * <p>To create a Span, see {@link Trace#createContinuation(Span parentSpan)}
 */
interface Continuation {
  /**
   * Close the continuation. Continuation's trace will not block reporting on account of this
   * continuation.
   */
  void close();

  /** @return parent span used to create this continuation. */
  Span getSpan();
}
