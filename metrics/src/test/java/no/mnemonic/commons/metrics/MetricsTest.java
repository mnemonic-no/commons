package no.mnemonic.commons.metrics;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class MetricsTest {

  @Test
  public void testMetricAddData() throws MetricException {
    MetricsData m = new MetricsData()
            .addData("key1", 1)
            .addData("key2", 2);
    assertEquals(2, m.getData().size());
    assertEquals(1, m.getData("key1"));
    assertNull(m.getData("unknownkey"));
  }

  @Test(expected = MetricException.class)
  public void testMetricAddNullValue() throws MetricException {
    new MetricsData().addData("key1", null);
  }

  @Test(expected = MetricException.class)
  public void testMetricAddNullKeyName() throws MetricException {
    new MetricsData().addData(null, 1);
  }

  @Test(expected = MetricException.class)
  public void testMetricAddNullSubmetricName() throws MetricException {
    new MetricsGroup().addSubMetrics(null, new MetricsData());
  }

  @Test(expected = MetricException.class)
  public void testMetricAddNullSubmetrics() throws MetricException {
    new MetricsGroup().addSubMetrics("sub1", null);
  }

  @Test
  public void testMetricAddSubMetric() throws MetricException {
    Metrics sub1 = new MetricsData();
    Metrics sub2 = new MetricsData();
    MetricsGroup m = new MetricsGroup()
            .addSubMetrics("sub1", sub1)
            .addSubMetrics("sub2", sub2);
    assertEquals(2, m.getSubMetrics().size());
    assertSame(sub1, m.getSubMetrics("sub1"));
  }
}
