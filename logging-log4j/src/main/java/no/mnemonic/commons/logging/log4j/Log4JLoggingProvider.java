package no.mnemonic.commons.logging.log4j;


import no.mnemonic.commons.logging.Logger;
import no.mnemonic.commons.logging.LoggingProvider;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;

public class Log4JLoggingProvider implements LoggingProvider {

  @Override
  public Logger getLogger(String name) {
    return new Log4JLogger(name);
  }

  public static class Log4JLogger implements Logger {
    private final org.apache.logging.log4j.Logger log4jLogger;
    public Log4JLogger(String name) {
      this.log4jLogger = LogManager.getLogger(name);
    }

    public void fatal(String formattedMessage, Object... args) {
      log(Level.FATAL, formattedMessage, args);
    }

    public void error(String formattedMessage, Object... args) {
      log(Level.ERROR, formattedMessage, args);
    }

    public void warning(String formattedMessage, Object... args) {
      log(Level.WARN, formattedMessage, args);
    }

    public void info(String formattedMessage, Object... args) {
      log(Level.INFO, formattedMessage, args);
    }

    public void debug(String formattedMessage, Object... args) {
      log(Level.DEBUG, formattedMessage, args);
    }

    public void fatal(Throwable ex, String formattedMessage, Object... args) {
      log(Level.FATAL, ex, formattedMessage, args);
    }

    public void error(Throwable ex, String formattedMessage, Object... args) {
      log(Level.ERROR, ex, formattedMessage, args);
    }

    public void warning(Throwable ex, String formattedMessage, Object... args) {
      log(Level.WARN, formattedMessage, args);
    }

    public void info(Throwable ex, String formattedMessage, Object... args) {
      log(Level.INFO, ex, formattedMessage, args);
    }

    public void debug(Throwable ex, String formattedMessage, Object... args) {
      log(Level.DEBUG, ex, formattedMessage, args);
    }

    public boolean isDebug() {
      return log4jLogger.isDebugEnabled();
    }

    public boolean isInfo() {
      return log4jLogger.isInfoEnabled();
    }

    //private methods

    private synchronized void log(Level level, String formattedMessage, Object... args) {
      if (formattedMessage == null) return;
      log4jLogger.log(level, String.format(formattedMessage, args));
    }

    private synchronized void log(Level level, Throwable ex, String formattedMessage, Object... args) {
      if (formattedMessage == null) return;
      log4jLogger.log(level, String.format(formattedMessage, args), ex);
    }

  }
}
