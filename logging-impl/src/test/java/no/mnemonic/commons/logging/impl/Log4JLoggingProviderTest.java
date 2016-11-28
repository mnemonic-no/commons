package no.mnemonic.commons.logging.impl;

import no.mnemonic.commons.logging.Logger;
import no.mnemonic.commons.logging.Logging;
import org.junit.Test;

public class Log4JLoggingProviderTest {

  @Test
  public void testLog4JProvider() {
    Logging.setProvider(new Log4JLoggingProvider());
    Logging.getLogger("myname").debug("log");
    Logging.getLogger((String)null).debug("log");
    Logging.getLogger((Class)null).debug("log");
    Logging.getLogger(getClass()).debug("log");
    Logging.getLogger("myname").debug(null);
  }

  @Test
  public void testDebugLogging() {
    Logger logger = new Log4JLoggingProvider().getLogger("name");
    logger.debug("log");
    logger.debug("log", null);
    logger.debug("log", null, null);
    logger.debug("log %d", 1);
    logger.debug(new Exception(), "log %d", 1);
    logger.debug(new Exception(), "log %d", null);
    logger.debug(new Exception(), null);
    logger.debug(new Exception(), null, null);
    logger.debug(null);
    logger.debug((Throwable)null, null);
  }

  @Test
  public void testErrorLogging() {
    Logger logger = new Log4JLoggingProvider().getLogger("name");
    logger.error("log");
    logger.error("log", null);
    logger.error("log", null, null);
    logger.error("log %d", 1);
    logger.error(new Exception(), "log %d", 1);
    logger.error(new Exception(), "log %d", null);
    logger.error(new Exception(), null);
    logger.error(new Exception(), null, null);
    logger.error(null);
    logger.error((Throwable)null, null);
  }


}
