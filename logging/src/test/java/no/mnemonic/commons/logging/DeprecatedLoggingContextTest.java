package no.mnemonic.commons.logging;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static no.mnemonic.commons.logging.DeprecatedLoggingContext.DEPRECATED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DeprecatedLoggingContextTest {

  @Before
  public void setUp() {
    TestLoggingContext loggingContext = new TestLoggingContext();

    Logging.setProvider(new LoggingProvider() {
      @Override
      public Logger getLogger(String name) {
        return new ConsoleLoggerImpl();
      }

      @Override
      public LoggingContext getLoggingContext() {
        return loggingContext;
      }
    });
  }

  @After
  public void cleanUp() {
    Logging.setProvider(null);
  }

  @Test
  public void testSettingLoggingContextVariable() {
    try (DeprecatedLoggingContext ignored = DeprecatedLoggingContext.create()) {
      assertEquals("true", Logging.getLoggingContext().get(DEPRECATED));
    }

    assertFalse(Logging.getLoggingContext().containsKey(DEPRECATED));
  }

  @Test
  public void testSettingLoggingContextVariableWithNestedScope() {
    try (DeprecatedLoggingContext ignored1 = DeprecatedLoggingContext.create()) {
      try (DeprecatedLoggingContext ignored2 = DeprecatedLoggingContext.create()) {
        assertTrue(Logging.getLoggingContext().containsKey(DEPRECATED));
      }
      assertTrue(Logging.getLoggingContext().containsKey(DEPRECATED));
    }

    assertFalse(Logging.getLoggingContext().containsKey(DEPRECATED));
  }

  @Test
  public void testLoggingContextIsAlreadySet() {
    Logging.getLoggingContext().put(DEPRECATED, "true");

    try (DeprecatedLoggingContext ignored = DeprecatedLoggingContext.create()) {
      assertTrue(Logging.getLoggingContext().containsKey(DEPRECATED));
    }

    assertTrue(Logging.getLoggingContext().containsKey(DEPRECATED));
  }
}