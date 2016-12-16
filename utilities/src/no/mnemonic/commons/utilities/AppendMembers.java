package no.mnemonic.commons.utilities;

/**
 * Interface for classes that know how to append its fields to a StringBuilder.
 */
public interface AppendMembers {
  /**
   * Append fields to the given StringBuilder.
   * @param buf string buffer
   */
  void appendMembers(StringBuilder buf);
}