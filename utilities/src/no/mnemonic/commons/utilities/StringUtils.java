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

}
