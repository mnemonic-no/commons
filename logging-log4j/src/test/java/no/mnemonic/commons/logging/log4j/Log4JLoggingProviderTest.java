package no.mnemonic.commons.logging.log4j;

import no.mnemonic.commons.logging.Logger;
import no.mnemonic.commons.logging.Logging;
import org.junit.Test;

import java.util.IllegalFormatException;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class Log4JLoggingProviderTest {

  @Test
  public void testDefaultLoggingProviderPicksUpConfig() {
    Logging.setProvider(null);
    assertTrue(Logging.getLogger("myname") instanceof Log4JLoggingProvider.Log4JLogger);
  }

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
  public void testSkipFormattingOnNoArgs() {
    Logging.setProvider(new Log4JLoggingProvider());
    //a string with formatting syntax is treated as a raw string if there are no params
    Logging.getLogger("myname").debug("unformattedString%s%s");
    //this should fail because the formatted string requires two params, but we only provide one
    assertThrows(IllegalFormatException.class, ()->Logging.getLogger("myname").debug("formattedString%s%s", "firstarg"));
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
