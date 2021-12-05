package no.mnemonic.commons.logging;

import java.io.PrintStream;

public class ConsoleLoggerImpl implements Logger {

  private enum Level {
    FATAL, ERROR, WARN, INFO, DEBUG
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
    log(Level.WARN, ex, formattedMessage, args);
  }

  public void info(Throwable ex, String formattedMessage, Object... args) {
    log(Level.INFO, ex, formattedMessage, args);
  }

  public void debug(Throwable ex, String formattedMessage, Object... args) {
    log(Level.DEBUG, ex, formattedMessage, args);
  }

  @Override
  public boolean isDebug() {
    return true;
  }

  @Override
  public boolean isInfo() {
    return true;
  }

  //private methods

  private synchronized void log(Level level, String message, Object... args) {
    if (message == null) return;
    if (args == null || args.length == 0) {
      selectStream(level).println(message);
    } else {
      selectStream(level).println(String.format(message, args));
    }
  }

  private synchronized void log(Level level, Throwable ex, String message, Object... args) {
    if (message == null) return;
    if (args == null || args.length == 0) {
      selectStream(level).println(message);
    } else {
      selectStream(level).println(String.format(message, args));
    }
    if (ex != null) {
      ex.printStackTrace(selectStream(level));
    }
  }

  private PrintStream selectStream(Level level) {
    switch(level) {
      case FATAL:
      case ERROR:
        return System.err;
      default:
        return System.out;
    }
  }

}
