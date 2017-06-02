package no.mnemonic.commons.metrics;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

public class TimerContextTest {

  static Clock clock;
  private Instant instant;

  @BeforeClass
  public static void beforeAll() {
    clock = Mockito.mock(Clock.class);
    TimerContext.setClock(clock);
  }

  @AfterClass
  public static void afterAll() {
    TimerContext.setClock(Clock.systemUTC());
  }

  @Before
  public void setup() {
    instant = Instant.ofEpochMilli(10000);
    when(clock.millis()).thenAnswer(i -> instant.toEpochMilli());
    when(clock.instant()).thenAnswer(i -> instant);
  }

  @Test
  public void testTimerContextReportsMillis() throws InterruptedException {
    AtomicLong metric = new AtomicLong();
    try (TimerContext ignored = TimerContext.timerMillis(metric::addAndGet)) {
      instant = Instant.ofEpochMilli(11000);
    }
    assertEquals(metric.get(), 1000);
  }

  @Test
  public void testTimerContextReportsSeconds() throws InterruptedException {
    AtomicLong metric = new AtomicLong();
    try (TimerContext ignored = TimerContext.timerSeconds(metric::addAndGet)) {
      instant = Instant.ofEpochMilli(12000);
    }
    assertEquals(metric.get(), 2);
  }

  @Test
  public void testTimerContextReportsNanos() throws InterruptedException {
    AtomicLong metric = new AtomicLong();
    try (TimerContext ignored = TimerContext.timerNanos(metric::addAndGet)) {
      instant = Instant.ofEpochMilli(10001);
    }
    assertEquals(metric.get(), 1000000);
  }

  @Test(expected = IllegalArgumentException.class)
  public void millisTimerContextThrowsIllegalArgumentIfConsumerIsNull() {
    TimerContext.timerMillis(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void nanoTimerContextThrowsIllegalArgumentIfConsumerIsNull() {
    TimerContext.timerNanos(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void secondsTimerContextThrowsIllegalArgumentIfConsumerIsNull() {
    TimerContext.timerSeconds(null);
  }

}
