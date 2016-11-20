package no.mnemonic.commons.logging;

import org.junit.Before;
import org.junit.Test;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ConsoleLoggerTest {

  private PrintStream origOut, origErr;
  private Interceptor outInterceptor;
  private Interceptor errInterceptor;

  @Before
  public void prepare() {
    origOut = System.out;
    outInterceptor = new Interceptor(origOut);
    System.setOut(outInterceptor);

    origErr = System.err;
    errInterceptor = new Interceptor(origErr);
    System.setErr(errInterceptor);
  }

  @Test
  public void testDebugLogging() {
    Logger logger = new ConsoleLoggerImpl();
    logger.debug("log");
    logger.debug("log", null);
    logger.debug("log", null, null);
    logger.debug("log %d", 1);

    assertEquals("log", outInterceptor.getLines().get(0));
    assertEquals("log", outInterceptor.getLines().get(1));
    assertEquals("log", outInterceptor.getLines().get(2));
    assertEquals("log 1", outInterceptor.getLines().get(3));
  }

  @Test
  public void testErrorLogging() {
    Logger logger = new ConsoleLoggerImpl();
    logger.error("log");
    logger.error("log", null);
    logger.error("log", null, null);
    logger.error("log %d", 1);

    assertEquals("log", errInterceptor.getLines().get(0));
    assertEquals("log", errInterceptor.getLines().get(1));
    assertEquals("log", errInterceptor.getLines().get(2));
    assertEquals("log 1", errInterceptor.getLines().get(3));
  }

  @Test
  public void testDebugExceptionLogging() {
    Logger logger = new ConsoleLoggerImpl();
    logger.debug(new Exception(), "log %d", 1);

    assertEquals("log 1", outInterceptor.getLines().get(0));
    assertEquals("java.lang.Exception", outInterceptor.getLines().get(1));
    assertTrue(outInterceptor.getLines().get(2).startsWith("\tat no.mnemonic.commons.logging.ConsoleLoggerTest"));
  }

  private class Interceptor extends PrintStream {

    List<String> lines = new ArrayList<>();

    public Interceptor(OutputStream out) {
      super(out, true);
    }

    @Override
    public void print(String s) {//do what ever you like
      lines.add(s);
      super.print(s);
    }

    public List<String> getLines() {
      return lines;
    }
  }

}
