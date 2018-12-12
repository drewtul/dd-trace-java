package datadog.trace.tracer;

import static java.lang.Math.max;

/**
 * Class that encapsulations notion of a given timestamp, or instant in time.
 *
 * <p>Timestamps are created by a [@link Clock} instance.
 */
class Timestamp {

  private final Clock clock;
  private final long nanoTicks;

  /**
   * Create timestamp for a given clock and given nanoTicks state
   *
   * @param clock clock instance
   * @param nanoTicks current nanoTicks
   */
  Timestamp(final Clock clock, final long nanoTicks) {
    this.clock = clock;
    this.nanoTicks = nanoTicks;
  }

  /**
   * Get current time in nanoseconds.
   *
   * <p>This is intentionally hidden because current time with nanoseconds precision can only be
   * meaningfully compared for the same {@link Clock} instance.
   *
   * @return epoch time in nano seconds.
   */
  long getEpochTimeNano() {
    return clock.getStartTimeNano() + (clock.nanoTicks() - nanoTicks);
  }

  /**
   * @return clock instance used by this timestamp
   */
  public Clock getClock() {
    return clock;
  }

  /** @return duration in nanoseconds from this time stamp to current time. */
  public long getDuration() {
    return getDuration(clock.createCurrentTimestamp());
  }

  /**
   * Get duration in nanoseconds from this time stamp to provided finish timestamp.
   *
   * @param finishTimestamp finish timestamp to use as period's end.
   * @return duration in nanoseconds.
   */
  public long getDuration(final Timestamp finishTimestamp) {
    if (clock != finishTimestamp.clock) {
      clock
          .getTracer()
          .reportError(
              "Trying to find duration between two timestamps created by clocks. Current clock: %s, finish timestamp clock: %s",
              clock, finishTimestamp.clock);
    }
    return max(0, finishTimestamp.getEpochTimeNano() - getEpochTimeNano());
  }

  /**
   * Duration in nanoseconds for external finish time.
   *
   * <p>Note: since we can only get time with millisecond precision in Java this ends up being
   * effectively millisecond precision duration converted to nanoseconds.
   *
   * @param finishTimeNanoseconds
   * @return
   */
  public long getDuration(final long finishTimeNanoseconds) {
    return max(0, finishTimeNanoseconds - clock.getStartTimeNano());
  }
}
