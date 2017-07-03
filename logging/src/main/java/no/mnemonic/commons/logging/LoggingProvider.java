package no.mnemonic.commons.logging;

public interface LoggingProvider {

  Logger getLogger(String name);
  LoggingContext getLoggingContext();

}
