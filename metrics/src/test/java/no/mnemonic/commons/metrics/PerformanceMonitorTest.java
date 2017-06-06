package no.mnemonic.commons.metrics;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.time.Clock;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

public class PerformanceMonitorTest {

  private AtomicLong clockTime = new AtomicLong(100000L);
  private Clock clock = Mockito.mock(Clock.class);
  private PerformanceMonitor monitor = new PerformanceMonitor(SECONDS, 10, 1);

  @Before
  public void setup() {
    PerformanceMonitor.setClock(clock);
    when(clock.millis()).thenAnswer(i -> clockTime.get());
  }

  @After
  public void cleanup() {
    PerformanceMonitor.setClock(Clock.systemUTC());
  }

  @Test
  public void basicProperties() {
    assertEquals(1000, monitor.getResolution());
    assertEquals(10000, monitor.getMemory());
  }

  @Test
  public void totalAccumulated() {
    monitor.invoked(100);
    clockTime.addAndGet(10000);
    monitor.invoked(100);
    assertEquals(2, monitor.getTotalInvocations());
    assertEquals(200, monitor.getTotalTimeSpent());
  }

  @Test(expected = IllegalArgumentException.class)
  public void zeroTimeframe() {
    monitor.getInvocationsLast(MILLISECONDS, 0);
  }

  @Test(expected = IllegalArgumentException.class)
  public void negativeTimeframe() {
    monitor.getInvocationsLast(MILLISECONDS, -10);
  }

  @Test
  public void tooLongTimeframeIsIgnored() {
    monitor.getInvocationsLast(SECONDS, 100);
  }

  @Test
  public void monitorNoInvocations() {
    assertEquals(0, monitor.getInvocationsLast(SECONDS, 1));
    assertEquals(0, monitor.getInvocationsLast(SECONDS, 10));
  }

  @Test
  public void monitorInvocationSingleTimeframe() {
    monitor.invoked();
    assertEquals(1, monitor.getInvocationsLast(SECONDS, 10));
    monitor.invoked().invoked().invoked();
    assertEquals(4, monitor.getInvocationsLast(SECONDS, 10));
  }

  @Test
  public void monitorAddMultipleInvocations() {
    monitor.invoked(100, 1000);
    assertEquals(100, monitor.getInvocationsLast(SECONDS, 10));
    assertEquals(1000, monitor.getTimeSpentLast(SECONDS, 10));
  }

  @Test
  public void monitorInvocationAcrossTimeframes() {
    monitor.invoked();
    assertEquals(1, monitor.getInvocationsLast(SECONDS, 1));
    assertEquals(1, monitor.getInvocationsLast(SECONDS, 10));

    clockTime.addAndGet(1000);
    assertEquals(0, monitor.getInvocationsLast(SECONDS, 1));
    assertEquals(1, monitor.getInvocationsLast(SECONDS, 10));

    monitor.invoked().invoked().invoked();
    assertEquals(3, monitor.getInvocationsLast(SECONDS, 1));
    assertEquals(4, monitor.getInvocationsLast(SECONDS, 10));
  }

  @Test
  public void monitorExpiresTimeframe() {
    monitor.invoked();
    assertEquals(1, monitor.getInvocationsLast(SECONDS, 10));

    clockTime.addAndGet(10000);
    assertEquals(0, monitor.getInvocationsLast(SECONDS, 10));

    monitor.invoked();
    assertEquals(1, monitor.getInvocationsLast(SECONDS, 10));
  }

  @Test
  public void calculateExecutionTime() {
    monitor.invoked(100);
    assertEquals(1, monitor.getInvocationsLast(SECONDS, 10));
    assertEquals(100, monitor.getTimeSpentLast(SECONDS, 10));
    assertEquals(100, monitor.getTimeSpentPerInvocationLast(SECONDS, 10), 0.01);
    assertEquals(1 / 10.0, monitor.getInvocationsPerSecondLast(SECONDS, 10), 0.01);
    assertEquals(100 / 10.0, monitor.getTimeSpentPerSecondLast(SECONDS, 10), 0.01);

    monitor.invoked(1000);
    assertEquals(2, monitor.getInvocationsLast(SECONDS, 10));
    assertEquals(1100, monitor.getTimeSpentLast(SECONDS, 10));
    assertEquals(550, monitor.getTimeSpentPerInvocationLast(SECONDS, 10), 0.01);
    assertEquals(2 / 10.0, monitor.getInvocationsPerSecondLast(SECONDS, 10), 0.01);
    assertEquals(1100 / 10.0, monitor.getTimeSpentPerSecondLast(SECONDS, 10), 0.01);
  }
}
