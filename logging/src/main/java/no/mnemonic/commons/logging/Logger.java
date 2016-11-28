package no.mnemonic.commons.logging;

public interface Logger {

  void fatal(String formattedMessage, Object... args);

  void error(String formattedMessage, Object... args);

  void warning(String formattedMessage, Object... args);

  void info(String formattedMessage, Object... args);

  void debug(String formattedMessage, Object... args);

  void fatal(Throwable ex, String formattedMessage, Object... args);

  void error(Throwable ex, String formattedMessage, Object... args);

  void warning(Throwable ex, String formattedMessage, Object... args);

  void info(Throwable ex, String formattedMessage, Object... args);

  void debug(Throwable ex, String formattedMessage, Object... args);

  boolean isDebug();

  boolean isInfo();

}