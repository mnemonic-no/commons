package no.mnemonic.commons.logging;

import java.util.concurrent.atomic.AtomicReference;

public class Logging {

  private static AtomicReference<LoggingProvider> provider = new AtomicReference<>(name -> new ConsoleLoggerImpl());

  public static Logger getLogger(String name) {
    if (name == null) name = "";
    return provider.get().getLogger(name);
  }

  public static Logger getLogger(Class clz) {
    return getLogger(clz == null ? null : clz.getName());
  }

  public static void setProvider(LoggingProvider implementation) {
    provider.set(implementation);
  }

}
