package no.mnemonic.commons.logging.log4j;

import no.mnemonic.commons.logging.LoggingContext;
import no.mnemonic.commons.utilities.StringUtils;
import org.apache.logging.log4j.ThreadContext;

import java.util.Collections;
import java.util.Map;

public class Log4jLoggingContext implements LoggingContext {

  @Override
  public void clear() {
    ThreadContext.clearAll();
  }

  @Override
  public Map<String, String> getAll() {
    return Collections.unmodifiableMap(ThreadContext.getContext());
  }

  @Override
  public boolean containsKey(String key) {
    if (StringUtils.isBlank(key)) {
      return false;
    }
    return ThreadContext.containsKey(key);
  }

  @Override
  public String get(String key) {
    if (StringUtils.isBlank(key)) {
      return null;
    }
    return ThreadContext.get(key);
  }

  @Override
  public void put(String key, String value) {
    if (StringUtils.isBlank(key) || StringUtils.isBlank(value)) {
      return;
    }
    ThreadContext.put(key, value);
  }

  @Override
  public void remove(String key) {
    if (StringUtils.isBlank(key)) {
      return;
    }
    ThreadContext.remove(key);
  }
}
