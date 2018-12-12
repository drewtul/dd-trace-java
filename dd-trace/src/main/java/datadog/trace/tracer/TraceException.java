package datadog.trace.tracer;

/**
 * Tracer-specific runtime exception.
 *
 * <p>TODO: add constructors javadoc
 */
public class TraceException extends RuntimeException {

  public TraceException() {
    super();
  }

  public TraceException(final String message) {
    super(message);
  }

  public TraceException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public TraceException(final Throwable cause) {
    super(cause);
  }
}
