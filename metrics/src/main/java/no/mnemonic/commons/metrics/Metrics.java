package no.mnemonic.commons.metrics;

import no.mnemonic.commons.utilities.ObjectUtils;

import java.io.Serializable;
import java.util.Map;

/**
 * A metric is a hierarchical data object for a set of measurement values.
 * There may be a set of key/value pairs for this metric, or a set of sub metrics
 */
public interface Metrics extends Serializable {

  /**
   * For metrics with data, this will return a map of keys with metrics.
   *
   * @return a map of all metric keys->values set into this object, or null if no data
   */
  Map<String, Number> getData();

  /**
   * @param key key to fetch value for
   * @return the value set for the specified key, or null if key is not set
   */
  default Number getData(String key) {
    return ObjectUtils.ifNotNull(getData(), m -> m.get(key));
  }

  /**
   * For metrics with submetrics, this will return a map of keys with submetrics.
   *
   * @return a map of all submetrics (name -> metrics) set into this object, or null if no submetrics
   */
  Map<String, Metrics> getSubMetrics();

  /**
   * @param name name to fetch submetrics for
   * @return the metrics object set for this submetric name
   */
  default Metrics getSubMetrics(String name) {
    return ObjectUtils.ifNotNull(getSubMetrics(), s -> s.get(name));
  }

  /**
   * @return true if this metric node has data
   */
  boolean hasData();

  /**
   * @return true if this metric node has submetrics
   */
  boolean hasSubMetrics();

}
