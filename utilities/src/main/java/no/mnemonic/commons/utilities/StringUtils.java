package no.mnemonic.commons.utilities;

public class StringUtils {

  private StringUtils() {
  }

  /**
   * Tests if a string is empty (null or "").
   *
   * @param str String to be tested.
   * @return Returns true if a string is null or "".
   */
  public static boolean isEmpty(String str) {
    return str == null || str.length() == 0;
  }

  /**
   * Tests if a string is blank (null, "" or only whitespace).
   *
   * @param str String to be tested.
   * @return Returns true if a string is null, "" or only contains whitespace.
   */
  public static boolean isBlank(String str) {
    return str == null || str.trim().length() == 0;
  }

  /**
   * Null-safe operation to determine the length of a string.
   *
   * @param str String to compute length for.
   * @return Returns 0 if 'str' is null, otherwise the length of 'str'.
   */
  public static int length(String str) {
    return str == null ? 0 : str.length();
  }

  /**
   * Simple method to test if an input string can be parsed to an integer.
   * The input can be negative (with a minus sign immediately prefixing the numbers).
   * No number separators or spaces will be accepted.
   *
   * The method will return true even if the integer string is too large to be represented by a
   * Java Integer or Long.
   *
   * If the method returns true, the input string should be parseable with Integer.parseInt or Long.parseLong
   * with the exception of integer overflow.
   *
   * @param str string to test
   * @return true if string can be parsed to a integer number
   */
  public static boolean isInteger(String str) {
    return str != null && str.matches("(\\-)?\\d+");
  }

}
