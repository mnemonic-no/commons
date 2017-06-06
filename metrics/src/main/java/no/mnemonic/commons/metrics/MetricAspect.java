package no.mnemonic.commons.metrics;

/**
 * Aspect for all objects offering a set of metrics.
 * Metrics should be readable at low-cost and without blocking for any significant amount of time.
 * They will typically be read periodically (every X minutes?) for reporting to external systems.
 */
public interface MetricAspect {

  /**
   *
   * @return a metric for this object
   * @throws MetricException if an error occurs retrieving this data
   */
  Metrics getMetrics() throws MetricException;

}
