package no.mnemonic.commons.utilities;

import java.util.Collection;

import static no.mnemonic.commons.utilities.collections.CollectionUtils.isEmpty;
import static no.mnemonic.commons.utilities.collections.CollectionUtils.size;

/**
 * Utility to make a bean return a nice looking descriptor on toString()
 * <p>
 * To use, let bean implement AppendMembers, and use
 * <pre>
 * public void appendMembers(StringBuilder buf) {
 *
 * }
 * </pre>
 */
public class AppendUtils {
  /**
   * Creates a String from an object by calling {@link #appendBean}.
   *
   * @param bean bean to write tostring for
   * @return a tostring representation of the given bean
   */
  public static String toString(Object bean) {
    return appendBean(null, bean).toString();
  }

  /**
   * Appends an object to a StringBuilder.
   * <p>
   * Uses {@link AppendMembers#appendMembers} if implemented,
   * otherwise just calls {@link StringBuilder#append(Object)}
   *
   * @param buf  string buffer
   * @param bean bean to append
   * @return The StringBuilder
   */
  public static StringBuilder appendBean(StringBuilder buf, Object bean) {
    buf = createBuilder(buf);
    if (bean == null) return buf;
    if (bean instanceof AppendMembers) {
      return appendMembersBean(buf, (AppendMembers) bean);
    } else {
      buf.append(bean);
      return buf;
    }
  }

  /**
   * Appends an 'id' field to a StringBuilder.
   * <p>
   * The format used is the id value followed by a colon (:).
   * <p>
   * Useful as part of the implementation of {@link AppendMembers#appendMembers}.
   *
   * @param buf  string buffer
   * @param bean bean to add as ID
   * @return The StringBuilder
   */
  public static StringBuilder appendIdField(StringBuilder buf, Object bean) {
    if (bean == null) return createBuilder(buf);
    return appendBean(appendUnlessEmpty(buf, ' '), bean).append(':');
  }

  /**
   * Appends an anonymous field to a StringBuilder, i.e. a field with no name.
   * <p>
   * Useful as part of the implementation of {@link AppendMembers#appendMembers}.
   *
   * @param buf  string buffer
   * @param bean bean to add as anonymous field
   * @return The StringBuilder
   */
  public static StringBuilder appendAnonField(StringBuilder buf, Object bean) {
    if (bean == null) return createBuilder(buf);
    return appendBean(appendUnlessEmpty(buf, ' '), bean);
  }

  /**
   * Appends a named field to a StringBuilder.
   * <p>
   * The format used is the name followed by an equals sign (=) followed by the value.
   * <p>
   * Useful as part of the implementation of {@link AppendMembers#appendMembers}.
   *
   * @param buf  string buffer
   * @param name name of field
   * @param bean value of field
   * @return The StringBuilder
   */
  public static StringBuilder appendField(StringBuilder buf, String name, Object bean) {
    if (bean == null) return createBuilder(buf);
    return appendBean(appendNamePart(buf, name), bean);
  }

  /**
   * Appends a collection field to a StringBuilder.
   * <p>
   * Only append when collection is not empty.
   * <p>
   * Useful as part of the implementation of {@link AppendMembers#appendMembers}.
   *
   * @param buf  string buffer
   * @param name name of field
   * @param col  collection to be appended
   * @return The StringBuilder
   */
  public static StringBuilder appendCollection(StringBuilder buf, String name, Collection<?> col) {
    if (isEmpty(col)) {
      return createBuilder(buf);
    }
    return appendField(buf, name, col);
  }

  /**
   * Appends a collection field to a StringBuilder.
   * <p>
   * If the size of the collection is greater than maxSize only the size will be appended.
   * <p>
   * Only append when collection is not empty.
   * <p>
   * Useful as part of the implementation of {@link AppendMembers#appendMembers}.
   *
   * @param buf     string buffer
   * @param name    name of field
   * @param col     collection to be appended
   * @param maxSize maximum elements to be appended
   * @return The StringBuilder
   */
  public static StringBuilder appendCollection(StringBuilder buf, String name, Collection<?> col, int maxSize) {
    if (size(col) > maxSize) {
      return appendField(buf, name, "<size=" + size(col) + ">");
    }

    return appendCollection(buf, name, col);
  }

  /**
   * Appends an object to a StringBuilder, unless the StringBuilder is empty.
   *
   * @param buf  string buffer
   * @param bean bean to add unless empty
   * @return The StringBuilder
   */
  public static StringBuilder appendUnlessEmpty(StringBuilder buf, Object bean) {
    buf = createBuilder(buf);
    if (buf.length() > 0) {
      appendBean(buf, bean);
    }
    return buf;
  }

  //private methods

  /**
   * Returns a new StringBuilder if passed null, otherwise just return the passed in StringBuilder.
   *
   * @param buf string buffer
   * @return a string builder
   */
  private static StringBuilder createBuilder(StringBuilder buf) {
    if (buf == null) {
      return new StringBuilder();
    } else {
      return buf;
    }
  }

  private static StringBuilder appendNamePart(StringBuilder buf, String name) {
    return appendUnlessEmpty(createBuilder(buf), ' ').append(name).append('=');
  }

  /**
   * Appends an instance of {@link AppendMembers} to a StringBuilder by using
   * {@link Class#getSimpleName} and {@link AppendMembers#appendMembers}.
   *
   * @return The StringBuilder
   */
  private static StringBuilder appendMembersBean(StringBuilder buf, AppendMembers bean) {
    buf = createBuilder(buf);
    buf.append("[");
    buf.append(bean.getClass().getSimpleName());
    bean.appendMembers(buf);
    buf.append("]");
    return buf;
  }

}
