package no.mnemonic.commons.metrics;

import no.mnemonic.commons.utilities.AppendMembers;
import no.mnemonic.commons.utilities.AppendUtils;
import no.mnemonic.commons.utilities.StringUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A metric is a hierarchical data object for a set of measurement values.
 * There may be a set of key/value pairs for this metric, or a set of sub metrics
 */
public class MetricsGroup implements Metrics, AppendMembers {

  private static final long serialVersionUID = 6636687481582715425L;

  //properties

  private final Map<String, Metrics> subMetrics = new ConcurrentHashMap<>();

  //interface methods


  @Override
  public String toString() {
    return AppendUtils.toString(this);
  }

  public void appendMembers(StringBuilder buf) {
    AppendUtils.appendField(buf, "subMetrics", subMetrics.keySet());
  }

  //protected and private methods

  //accessors

  public Map<String, Number> getData() {
    return null;
  }

  public Map<String, Metrics> getSubMetrics() {
    return subMetrics;
  }

  public boolean hasData() {
    return false;
  }

  public boolean hasSubMetrics() {
    return true;
  }

  /**
   * Add a submetrics group to this metrics group
   * @param name name of the group
   * @param subMetrics the metrics group
   * @throws MetricException if name or metric object is empty
   */
  @SuppressWarnings("WeakerAccess")
  public MetricsGroup addSubMetrics(String name, Metrics subMetrics) throws MetricException {
    if (StringUtils.isBlank(name)) throw new MetricException("Cannot add submetric without a name");
    if (subMetrics == null) throw new MetricException("Submetrics cannot be null");
    this.subMetrics.put(name, subMetrics);
    return this;
  }

}
