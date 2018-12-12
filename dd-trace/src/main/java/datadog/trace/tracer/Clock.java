package datadog.trace.tracer;

import java.util.concurrent.TimeUnit;

/**
 * // FIXME: rewrite this javadoc A simple wrapper for system clock that aims to provide the current
 * time
 *
 * <p>
 *
 * <p>
 *
 * <p>
 *
 * <p>The JDK provides two clocks:
 * <li>one in nanoseconds, for precision, but it can only use to measure durations
 * <li>one in milliseconds, for accuracy, useful to provide epoch time
 *
 *     <p>
 *
 *     <p>At this time, we are using a millis precision (converted to micros) in order to guarantee
 *     consistency between the span start times and the durations
 */
public class Clock {

  private final Tracer tracer;
  /**
   * Trace start time in nano seconds measured up to a millisecond accuracy
   */
  private final long startTimeNano;
  /**
   * Nano second ticks value at trace start
   */
  private final long startNanoTicks;

  public Clock(final Tracer tracer) {
    this.tracer = tracer;
    startTimeNano = epochTimeNano();
    startNanoTicks = nanoTicks();
  }

  /**
   * @return {@link Tracer} that created this clock.
   */
  public Tracer getTracer() {
    return tracer;
  }

  /**
   * Create new timestamp instance for current time.
   *
   * @return new timestamp capturing current time.
   */
  public Timestamp createCurrentTimestamp() {
    return new Timestamp(this, nanoTicks());
  }

  /**
   * Get the current nanos ticks (i.e. System.nanoTime()), this method can't be use for date
   * accuracy (only duration calculations).
   *
   * @return The current nanos ticks.
   */
  long nanoTicks() {
    return System.nanoTime();
  }

  /**
   * Get the current epoch time in micros.
   *
   * <p>Note: The actual precision is the millis.
   *
   * @return the current epoch time in micros.
   */
  long epochTimeMicro() {
    return TimeUnit.MILLISECONDS.toMicros(System.currentTimeMillis());
  }

  /**
   * Get the current epoch time in nanos.
   *
   * <p>Note: The actual precision is the millis. This will overflow ~290 years after epoch.
   *
   * @return the current epoch time in nanos.
   */
  long epochTimeNano() {
    return TimeUnit.MILLISECONDS.toNanos(System.currentTimeMillis());
  }

  /**
   * Get time this clock instance was created in nanos.
   *
   * @return the time this clock instance was created in nanos.
   */
  long getStartTimeNano() {
    return startTimeNano;
  }
}
