package no.mnemonic.commons.logging;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

public class Logging {

  private static final String LOGGING_PROPERTY_FILE = "META-INF/no.mnemonic.commons.logging.Logging.properties";
  private static final String LOGGING_PROPERTY_KEY = "provider.class";
  private final static AtomicReference<LoggingProvider> provider = new AtomicReference<>();

  public static Logger getLogger(String name) {
    if (name == null) name = "";
    return getProvider().getLogger(name);
  }

  public static LoggingContext getLoggingContext() {
    return getProvider().getLoggingContext();
  }

  public static Logger getLogger(Class clz) {
    return getLogger(clz == null ? null : clz.getName());
  }

  public static void setProvider(LoggingProvider implementation) {
    provider.set(implementation);
  }

  //resolve provider

  private static LoggingProvider getProvider() {
    if (provider.get() != null) return provider.get();
    provider.set(resolveProvider());
    return provider.get();
  }

  private static LoggingProvider resolveProvider() {
    try (InputStream propertyStream = Logging.class.getClassLoader().getResourceAsStream(LOGGING_PROPERTY_FILE)) {
      if (propertyStream == null) return createDefaultProvider();
      Properties props = new Properties();
      props.load(propertyStream);
      if (props.containsKey(LOGGING_PROPERTY_KEY)) return loadProvider(props.getProperty(LOGGING_PROPERTY_KEY));
      return createDefaultProvider();
    } catch (IOException e) {
      return createDefaultProvider();
    }
  }

  private static LoggingProvider loadProvider(String providerClassName) {
    try {
      Class providerClz = Class.forName(providerClassName);
      return (LoggingProvider) providerClz.newInstance();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static LoggingProvider createDefaultProvider() {
    System.err.println("ERROR: no.mnemonic.commons.logging.Logging: No logging provider found, using console logger as default. Add implementation package to classpath.");
    return new LoggingProvider() {
      @Override
      public Logger getLogger(String name) {
        return new ConsoleLoggerImpl();
      }

      @Override
      public LoggingContext getLoggingContext() {
        return new LoggingContext() {
          @Override
          public void clear() {
            // do nothing
          }

          @Override
          public Map<String, String> getAll() {
            return Collections.emptyMap();
          }

          @Override
          public boolean containsKey(String key) {
            return false;
          }

          @Override
          public String get(String key) {
            return null;
          }

          @Override
          public void put(String key, String value) {
            // do nothing
          }

          @Override
          public void remove(String key) {
            // do nothing
          }
        };
      }
    };
  }

}
