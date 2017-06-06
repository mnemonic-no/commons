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
public class MetricsData implements Metrics, AppendMembers {

  private static final long serialVersionUID = 4462404396917034700L;

  //properties

  private final Map<String, Number> data = new ConcurrentHashMap<>();

  //interface methods

  @Override
  public String toString() {
    return AppendUtils.toString(this);
  }

  @Override
  public void appendMembers(StringBuilder buf) {
    AppendUtils.appendField(buf, "data", data);
  }

  //protected and private methods

  //accessors

  public Map<String, Number> getData() {
    return data;
  }

  public Map<String, Metrics> getSubMetrics() {
    return null;
  }

  public boolean hasData() {
    return true;
  }

  public boolean hasSubMetrics() {
    return false;
  }

  /**
   * Add metric data item to this object.
   *
   * @param key   key of the metric to add
   * @param value the metric value
   * @throws MetricException if key or value is empty
   */
  @SuppressWarnings("WeakerAccess")
  public MetricsData addData(String key, Number value) throws MetricException {
    if (StringUtils.isBlank(key)) throw new MetricException("Cannot add metric without a key");
    if (value == null) throw new MetricException("Metric value cannot be null");
    this.data.put(key, value);
    return this;
  }
}
