package no.mnemonic.commons.logging;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

class TestLoggingContext implements LoggingContext {

  private static final ThreadLocal<Map<String, String>> variables = new ThreadLocal<>();

  @Override
  public void clear() {
    getVariables().clear();
  }

  @Override
  public boolean containsKey(String key) {
    return getVariables().containsKey(key);
  }

  @Override
  public String get(String key) {
    return getVariables().get(key);
  }

  @Override
  public Map<String, String> getAll() {
    return Collections.unmodifiableMap(getVariables());
  }

  @Override
  public void put(String key, String value) {
    getVariables().put(key, value);
  }

  @Override
  public void remove(String key) {
    getVariables().remove(key);
  }

  private Map<String, String> getVariables() {
    if (variables.get() == null) {
      variables.set(new HashMap<>());
    }
    return variables.get();
  }
}
