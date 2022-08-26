package no.mnemonic.commons.logging;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;


public class LocalLoggingContextTest {

  private static final ExecutorService testExecutor = Executors.newSingleThreadExecutor();

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

  @Test
  public void testSetContext() {
    LoggingContext lctx = Logging.getLoggingContext();
    lctx.put("existingkey", "existingvalue");
    assertNull(lctx.get("key"));
    assertEquals("existingvalue", lctx.get("existingkey"));
    try (LocalLoggingContext ctx = LocalLoggingContext.create().using("key", "value").using("existingkey", "newvalue")) {
      assertEquals("value", lctx.get("key"));
      assertEquals("newvalue", lctx.get("existingkey"));
    }
    assertNull(lctx.get("key"));
    assertEquals("existingvalue", lctx.get("existingkey"));
  }

  @Test
  public void testSubmitRunnable() throws ExecutionException, InterruptedException {
    LoggingContext lctx = Logging.getLoggingContext();
    try (LocalLoggingContext llctx = LocalLoggingContext.create().using("key", "value").using("key2", "value2")) {
      assertEquals("value", lctx.get("key"));
      testExecutor.submit(llctx.running(() -> {
        LoggingContext tlctx = Logging.getLoggingContext();
        assertEquals("value", tlctx.get("key")); //duplicated from parent LocalLoggingContext
      })).get();
    }
  }

  @Test
  public void testSubmitCallable() throws ExecutionException, InterruptedException {
    LoggingContext lctx = Logging.getLoggingContext();
    try (LocalLoggingContext llctx = LocalLoggingContext.create().using("key", "value").using("key2", "value2")) {
      assertEquals("value", lctx.get("key"));
      testExecutor.submit(llctx.running(() -> {
        LoggingContext tlctx = Logging.getLoggingContext();
        assertEquals("value", tlctx.get("key")); //duplicated from parent LocalLoggingContext
        return "result";
      })).get();
    }
  }

  @Test
  public void testDuplicateContext() throws ExecutionException, InterruptedException {
    LoggingContext lctx = Logging.getLoggingContext();
    lctx.put("existingkey", "existingvalue");

    try (LocalLoggingContext llctx = LocalLoggingContext.create().using("key", "value").using("key2", "value2")) {
      assertEquals("existingvalue", lctx.get("existingkey"));
      assertEquals("value", lctx.get("key"));
      assertEquals("value2", lctx.get("key2"));

      testExecutor.submit(() -> {
        LoggingContext tlctx = Logging.getLoggingContext();
        assertNull(tlctx.get("existingkey"));
        assertNull(tlctx.get("key"));

        try (LocalLoggingContext tllctx = llctx.duplicate().using("threadkey", "threadvalue").using("key2", "newvalue2")) {
          assertEquals("existingvalue", tlctx.get("existingkey")); //duplicated from original loggingcontext
          assertEquals("value", tlctx.get("key")); //duplicated from parent LocalLoggingContext
          assertEquals("newvalue2", tlctx.get("key2")); //overridden in this LocalLoggingContext
          assertEquals("threadvalue", tlctx.get("threadkey")); //set in this LocalLoggingContext
        }
        assertNull(tlctx.get("threadkey")); //reset on this LocalLoggingContext closed
        assertNull(tlctx.get("key2"));  //reset on this LocalLoggingContext closed
      }).get();
      assertNull(lctx.get("threadkey")); //never set in parent LoggingContext
      assertEquals("value2", lctx.get("key2")); //still has value set in parent LocalLoggingContext
    }
    assertNull(lctx.get("key2")); //reset on parent LocalLoggingContext close
  }
}