package no.mnemonic.commons.metrics;

import no.mnemonic.commons.logging.Logger;
import no.mnemonic.commons.logging.Logging;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A timer context is a helper to register timing metrics for your code.
 *
 * Example:
 * <code>
 *   LongAdder executionTimeInMillis = new LongAdder();
 *   try (TimerContext ignored = TimerContext.timerMillis(executionTimeInMillis::add) {
 *     //do some work
 *   }
 *   System.out.println("Milliseconds required for work: " + executionTimeInMillis);
 * </code>
 *
 * For long-living worker components, a good use case is to define a LongAdder in your component instance
 * for measuring accumulated time for an operation, by appending to it for every execution.
 */
public class TimerContext implements AutoCloseable {

  private static final Logger LOGGER = Logging.getLogger(TimerContext.class);
  private static Clock clock = Clock.systemUTC();

  enum MetricType {

    millis(clock::millis),
    nanos(() -> TimeUnit.MILLISECONDS.toNanos(clock.millis())),
    seconds(() -> TimeUnit.MILLISECONDS.toSeconds(clock.millis()));

    private final Supplier<Long> currentSupplier;

    MetricType(Supplier<Long> currentSupplier) {
      this.currentSupplier = currentSupplier;
    }

    public long getCurrent() {
      return currentSupplier.get();
    }
  }

  private final Consumer<Long> metric;
  private final MetricType type;
  private final long startTime;

  private TimerContext(Consumer<Long> metric, MetricType type) {
    if (metric == null) throw new IllegalArgumentException("Metric not set");
    if (type == null) throw new IllegalArgumentException("Type not set");
    this.metric = metric;
    this.type = type;
    this.startTime = type.getCurrent();
  }

  @Override
  public void close() {
    try {
      metric.accept(type.getCurrent() - startTime);
    } catch (Throwable e) {
      LOGGER.warning(e, "Error updating timer metric");
    }
  }

  public static TimerContext timerMillis(Consumer<Long> metric) {
    return new TimerContext(metric, MetricType.millis);
  }

  public static TimerContext timerNanos(Consumer<Long> metric) {
    return new TimerContext(metric, MetricType.nanos);
  }

  public static TimerContext timerSeconds(Consumer<Long> metric) {
    return new TimerContext(metric, MetricType.seconds);
  }

  static void setClock(Clock clock) {
    TimerContext.clock = clock;
  }

}
