package no.mnemonic.commons.logging;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class LoggingTest {

  @Test
  public void testDefaultLogger() {
    assertTrue(Logging.getLogger("myname") instanceof ConsoleLoggerImpl);
    assertTrue(Logging.getLogger(getClass()) instanceof ConsoleLoggerImpl);
    assertTrue(Logging.getLogger((String)null) instanceof ConsoleLoggerImpl);
    assertTrue(Logging.getLogger((Class)null) instanceof ConsoleLoggerImpl);
  }

}
