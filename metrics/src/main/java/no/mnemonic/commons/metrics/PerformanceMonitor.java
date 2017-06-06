package no.mnemonic.commons.metrics;

import no.mnemonic.commons.logging.Logger;
import no.mnemonic.commons.logging.Logging;
import no.mnemonic.commons.utilities.AppendMembers;
import no.mnemonic.commons.utilities.AppendUtils;

import java.io.Serializable;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Stream;

/**
 * A PerformanceMonitor is a utility to simplify collecting invocation count and timing stats.
 * It divides time into timeslots (based on resolution), and will remember all timeslots for a defined
 * memory period.
 *
 * When adding invocation stats, the monitor will always calculate the current timeslot, and keep previous timeslots in memory for the defined period.
 * This allows simple fetching of average execution stats for any period within the defined memory period (down to the defined resolution)
 *
 * <code>
 *   //create a monitor which remembers for 10 minutes, and with a data resolution of 1 second
 *   PerformanceMonitor codeBlockMonitor = new PerformanceMonitor(TimeUnit.SECONDS, 600, 1)
 *
 *   //.... component code
 *
 *   try (TimerContext timer = TimerContext.ofMillis(codeBlockMonitor::invoked) {
 *     //do some time consuming task
 *   }
 *
 *   //.... metrics collection
 *
 *   metrics.addMetric("invocationsPerSec10secAverage", codeBlockMonitor.getInvocationsPerSecondLast(TimeUnit.Seconds, 10));
 *   metrics.addMetric("invocationsPerSec60secAverage", codeBlockMonitor.getInvocationsPerSecondLast(TimeUnit.Seconds, 60));
 * </code>
 */
@SuppressWarnings("WeakerAccess")
public class PerformanceMonitor implements Serializable {

  private static Logger LOGGER = Logging.getLogger(PerformanceMonitor.class);

  private static Clock clock = Clock.systemUTC();
  private final long resolution;
  private final long memory;

  //variables
  private final LinkedList<DataPoint> datapoints = new LinkedList<>();
  private final LongAdder totalInvocations = new LongAdder();
  private final LongAdder totalTimeSpent = new LongAdder();

  /**
   * @param timeUnit   the timeUnit used for memory and resolution
   * @param memory     the number of timeUnits this monitor remembers
   * @param resolution resolution in timeUnits
   */
  @SuppressWarnings("WeakerAccess")
  public PerformanceMonitor(TimeUnit timeUnit, long memory, long resolution) {
    if (timeUnit == null) throw new IllegalArgumentException("TimeUnit not set");
    if (resolution < 1) throw new IllegalArgumentException("Resolution invalid");
    if (memory <= resolution) throw new IllegalArgumentException("Memory must be greater than resolution");
    this.memory = timeUnit.toMillis(memory);
    this.resolution = timeUnit.toMillis(resolution);
  }

  //public methods

  /**
   * @return the configured resolution for this monitor, in milliseconds
   */
  public long getResolution() {
    return resolution;
  }

  /**
   * @return the configured memory period for this monitor, in milliseconds
   */
  public long getMemory() {
    return memory;
  }

  /**
   * @return the total number of invocations for this monitor (regardless of memory)
   */
  public long getTotalInvocations() {
    return totalInvocations.longValue();
  }

  /**
   * @return the total millisecond execution time registered for this monitor (regardless of memory)
   */
  public long getTotalTimeSpent() {
    return totalTimeSpent.longValue();
  }

  /**
   * Register an invocation without time stats. This will increment the invocation counter of the current timeslot,
   * but not the timespent counter.
   *
   * @return the monitor itself
   */
  public PerformanceMonitor invoked() {
    invoked(0);
    return this;
  }

  /**
   * Register an invocation with time spent. This will increment the invocation counter, and add the timeSpentInMillis to the
   * timeSpent counter of the current timeslot.
   *
   * @param timeSpentInMillis time spent for the invocation, in milliseconds
   * @return the monitor itself
   */
  @SuppressWarnings("UnusedReturnValue")
  public PerformanceMonitor invoked(long timeSpentInMillis) {
    invoked(1, timeSpentInMillis);
    return this;
  }

  /**
   * Register invocations and execution time. This will add the given number of invocations and milliseconds of execution time
   * to the current timeslot.
   *
   * @param invocations number of invocations to add
   * @param timeSpentInMillis time spent for these invocations, in milliseconds
   * @return the monitor itself
   */
  @SuppressWarnings("UnusedReturnValue")
  public PerformanceMonitor invoked(long invocations, long timeSpentInMillis) {
    synchronized (this) {
      DataPoint d = getOrCreateHead();
      d.invocations.add(invocations);
      d.timeSpent.add(timeSpentInMillis);
      totalInvocations.add(invocations);
      totalTimeSpent.add(timeSpentInMillis);
      return this;
    }
  }

  /**
   * Query the memory for registered invocations. If the query specifies a timeframe
   * which goes beyond the memory window, the timeslots outside the memory window will be ignored.
   * The resulting stats may not correctly reflect the query.
   *
   * @param timeUnit timeunit to query in
   * @param timeframe the number of timeunits to query
   * @return the sum of invocations registered within the specified timeframe
   */
  public long getInvocationsLast(TimeUnit timeUnit, long timeframe) {
    return reducedTimeframeStream(timeUnit.toMillis(timeframe)).invocations;
  }

  /**
   * Query the memory for registered execution time. If the query specifies a timeframe
   * which goes beyond the memory window, the timeslots outside the memory window will be ignored.
   * The resulting stats may not correctly reflect the query.
   *
   * @param timeUnit timeunit to query in
   * @param timeframe the number of timeunits to query
   * @return the sum of timeSpent registered within the specified timeframe
   */
  @SuppressWarnings("SameParameterValue")
  public long getTimeSpentLast(TimeUnit timeUnit, long timeframe) {
    return reducedTimeframeStream(timeUnit.toMillis(timeframe)).timeSpent;
  }

  /**
   * Query the memory for registered execution time. If the query specifies a timeframe
   * which goes beyond the memory window, the timeslots outside the memory window will be ignored.
   * The resulting stats may not correctly reflect the query.
   *
   * @param timeUnit timeunit to query in
   * @param timeframe the number of timeunits to query
   * @return the sum of timeSpent registered within the specified timeframe, divided by the number of seconds this
   * timeframe represents.
   */
  @SuppressWarnings("SameParameterValue")
  public double getTimeSpentPerSecondLast(TimeUnit timeUnit, long timeframe) {
    long seconds = timeUnit.toSeconds(timeframe);
    return reducedTimeframeStream(timeUnit.toMillis(timeframe)).timeSpent / ((double)seconds);
  }

  /**
   * Query the memory for registered execution time per execution. If the query specifies a timeframe
   * which goes beyond the memory window, the timeslots outside the memory window will be ignored.
   * The resulting stats may not correctly reflect the query.
   *
   * @param timeUnit timeunit to query in
   * @param timeframe the number of timeunits to query
   * @return the sum of execution time divided by the total number of executions within the specified timeframe
   */
  @SuppressWarnings("SameParameterValue")
  public double getTimeSpentPerInvocationLast(TimeUnit timeUnit, long timeframe) {
    Data reduced = reducedTimeframeStream(timeUnit.toMillis(timeframe));
    return reduced.timeSpent / ((double)reduced.invocations);
  }

  /**
   * Query the memory for registered executions. If the query specifies a timeframe
   * which goes beyond the memory window, the timeslots outside the memory window will be ignored.
   * The resulting stats may not correctly reflect the query.
   *
   * @param timeUnit timeunit to query in
   * @param timeframe the number of timeunits to query
   * @return the sum executions within the specified timeframe, divided by the number of seconds this
   * timeframe represents.
   */
  @SuppressWarnings("SameParameterValue")
  public double getInvocationsPerSecondLast(TimeUnit timeUnit, long timeframe) {
    long seconds = timeUnit.toSeconds(timeframe);
    return reducedTimeframeStream(timeUnit.toMillis(timeframe)).invocations / ((double)seconds);
  }

  //private methods

  private Data reducedTimeframeStream(long timeframe) {
    return createTimeframeStream(timeframe)
            .reduce((d1, d2) -> new Data(
                    d1.invocations + d2.invocations,
                    d1.timeSpent + d2.timeSpent
            ))
            .orElse(new Data(0, 0));
  }

  private Stream<Data> createTimeframeStream(long timeframe) {
    long firstTime = checkTimeframeStart(timeframe);
    return iterateDataPoints()
            .filter(dp -> dp.timestamp > firstTime)
            .map(Data::new);
  }

  private Stream<DataPoint> iterateDataPoints() {
    synchronized (this) {
      //ensure we have a head for the current timeframe
      getOrCreateHead();
      //stream from new arraylist to make use of the resulting stream thread safe
      return new ArrayList<>(datapoints).stream();
    }
  }

  private DataPoint getOrCreateHead() {
    synchronized (this) {
      long currentSlot = slotTime(clock.millis());
      if (datapoints.isEmpty() || datapoints.getFirst().timestamp < currentSlot) {
        if (LOGGER.isDebug()) LOGGER.debug("Adding new datapoint at %d", currentSlot);
        //add new datapoint at head of list
        datapoints.add(0, new DataPoint(currentSlot));
        //remove any trailing timeframes which have fallen outside memory
        while (isExpired(datapoints.getLast().timestamp)) {
          if (LOGGER.isDebug()) LOGGER.debug("Pruning datapoint %s", datapoints.getLast());
          datapoints.removeLast();
        }
      }
      return datapoints.getFirst();
    }
  }

  private long checkTimeframeStart(long timeframe) {
    if (timeframe < 1) throw new IllegalArgumentException("Invalid timeframe: " + timeframe);
    if (timeframe > memory) LOGGER.warning("Using larger timeframe than defined memory, will give inaccurate results");
    return clock.millis() - timeframe;
  }

  private boolean isExpired(long timestamp) {
    return timestamp < (slotTime(clock.millis()) - memory);
  }

  private long slotTime(long timestamp) {
    return (timestamp / resolution) * resolution;
  }

  private static class Data {
    final int invocations;
    final long timeSpent;

    Data(DataPoint dp) {
      this(dp.invocations.intValue(), dp.timeSpent.longValue());
    }

    Data(int invocations, long timeSpent) {
      this.invocations = invocations;
      this.timeSpent = timeSpent;
    }
  }

  private static class DataPoint implements AppendMembers {
    final long timestamp;
    final LongAdder invocations = new LongAdder();
    final LongAdder timeSpent = new LongAdder();

    DataPoint(long timestamp) {
      this.timestamp = timestamp;
    }

    @Override
    public void appendMembers(StringBuilder buf) {
      AppendUtils.appendField(buf, "timestamp", timestamp);
      AppendUtils.appendField(buf, "invocations", invocations);
      AppendUtils.appendField(buf, "timeSpent", timeSpent);
    }

    @Override
    public String toString() {
      return AppendUtils.toString(this);
    }
  }

  //for testing
  static void setClock(Clock clock) {
    PerformanceMonitor.clock = clock;
  }
}
