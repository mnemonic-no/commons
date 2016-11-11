package no.mnemonic.commons.utilities;

import java.util.function.Function;
import java.util.function.Supplier;

public class ObjectUtils {

  private ObjectUtils() {
  }

  /**
   * Throws a RuntimeException if 'value' is null, returns 'value' otherwise.
   *
   * @param value   Object to be tested for null.
   * @param message Message passed to the exception thrown.
   * @param <T>     Type of value parameter.
   * @return 'value' if not null, throws a RuntimeException otherwise.
   */
  public static <T> T notNull(T value, String message) {
    if (value == null) throw new RuntimeException(message);
    return value;
  }

  /**
   * Throws an exception if 'value' is null, returns 'value' otherwise.
   *
   * @param value     Object to be tested for null.
   * @param exception Exception to be thrown.
   * @param <T>       Type of value parameter.
   * @return 'value' if not null, throws an exception otherwise.
   * @throws Exception Thrown if 'value' is null.
   */
  public static <T> T notNull(T value, Exception exception) throws Exception {
    if (exception == null) throw new IllegalArgumentException("Exception was null!");
    if (value == null) throw exception;
    return value;
  }

  /**
   * Returns a default value if 'value' is null, returns 'value' otherwise.
   *
   * @param value        Value to be tested for null.
   * @param defaultValue Value to return if 'value' is null.
   * @param <T>          Type of value parameter.
   * @return Either 'value' or 'defaultValue'.
   */
  public static <T> T ifNull(T value, T defaultValue) {
    if (value != null) return value;
    return defaultValue;
  }

  /**
   * Returns a default value if 'value' is null, returns 'value' otherwise.
   *
   * @param value        Value to be tested for null.
   * @param defaultValue Supplier which provides a value to return if 'value' is null.
   * @param <T>          Type of value parameter.
   * @return Either 'value' or default value provided by supplier.
   */
  public static <T> T ifNull(T value, Supplier<T> defaultValue) {
    if (defaultValue == null) throw new IllegalArgumentException("Supplier was null!");
    if (value != null) return value;
    return defaultValue.get();
  }

  /**
   * Applies a conversion to 'value', or returns null if 'value' is null.
   *
   * @param value     Value to be converted.
   * @param converter Converter function.
   * @param <T>       Type of value parameter.
   * @param <V>       Type of return value.
   * @return Converted value, or null if 'value' was null.
   */
  public static <T, V> V ifNotNull(T value, Function<T, V> converter) {
    if (converter == null) throw new IllegalArgumentException("Converter was null!");
    if (value == null) return null;
    return converter.apply(value);
  }

  /**
   * Applies a conversion to 'value', or returns 'nullValue' if 'value' is null.
   *
   * @param value     Value to be converted.
   * @param converter Converter function.
   * @param nullValue Value to return if 'value' is null.
   * @param <T>       Type of value parameter.
   * @param <V>       Type of return value.
   * @return Converted value, or 'nullValue' if 'value' was null.
   */
  public static <T, V> V ifNotNull(T value, Function<T, V> converter, V nullValue) {
    if (converter == null) throw new IllegalArgumentException("Converter was null!");
    if (value == null) return nullValue;
    return converter.apply(value);
  }

}
