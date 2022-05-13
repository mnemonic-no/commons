package no.mnemonic.commons.logging;

import java.util.Map;

public interface LoggingContext extends AutoCloseable {

  /**
   * Clears the context
   */
  void clear();

  /**
   * @return all context variables set in this context in an unmodifiable map
   */
  Map<String, String> getAll();

  /**
   * Determines whether key exists in the context
   * @param key the key to verify
   * @return  true if key exists, false otherwise
   */
  boolean containsKey(String key);

  /**
   * Gets the context value identified by key parameter
   * @param key the key to fetch context value
   * @return  The value associated with the key or null if not exist
   */
  String get(String key);

  /**
   * Puts a context value as identified with the key parameter
   * @param key context key name
   * @param value context value
   */
  void put(String key, String value);

  /**
   * Removes the context identified by key parameter
   * @param key The key to remove
   */
  void remove(String key);

  /**
   * Closes this context, default implementation execute clear()
   * @throws Exception if this context cannot be closed
   */
  @Override
  default void close() throws Exception {
    clear();
  }
}
