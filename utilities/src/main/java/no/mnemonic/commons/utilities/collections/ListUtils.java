package no.mnemonic.commons.utilities.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class ListUtils {

  private ListUtils() {
  }

  /**
   * Creates a list from its arguments.
   *
   * @param values Values to be added to the list.
   * @param <T>    Type of value parameters.
   * @return A list containing all values.
   */
  @SafeVarargs
  public static <T> List<T> list(T... values) {
    if (values == null) return new ArrayList<>();
    return list(Arrays.asList(values));
  }

  /**
   * Creates a list from an iterator.
   *
   * @param iterator An iterator which values are added to the list.
   * @param <T>      Type of iterator values.
   * @return A list containing all values supplied by the given iterator.
   */
  public static <T> List<T> list(Iterator<T> iterator) {
    return list(iterator, v -> v);
  }

  /**
   * Creates a list from another collection.
   *
   * @param collection A collection which values are added to the list.
   * @param <T>        Type of collection values.
   * @return A list containing all values contained in the given collection, omitting null elements.
   */
  public static <T> List<T> list(Collection<T> collection) {
    if (collection == null) return new ArrayList<>();
    return collection.stream().filter(Objects::nonNull).collect(Collectors.toList());
  }

  /**
   * Creates a list from its arguments using a mapping function converting all values.
   *
   * @param mapping A mapping function applied to all values.
   * @param values  Values to be added to the list.
   * @param <T>     Type of value parameters before conversion.
   * @param <V>     Type of values in the returned list after conversion.
   * @return A list containing all values converted using the mapping function.
   */
  @SafeVarargs
  public static <T, V> List<V> list(Function<T, V> mapping, T... values) {
    if (values == null) return list();
    return list(Arrays.asList(values), mapping);
  }

  /**
   * Creates a list from an iterator using a mapping function converting all values.
   *
   * @param iterator An iterator which values are added to the list.
   * @param mapping  A mapping function applied to all values.
   * @param <T>      Type of iterator values before conversion.
   * @param <V>      Type of values in the returned list after conversion.
   * @return A list containing all values supplied by the given iterator and converted using the mapping function.
   */
  public static <T, V> List<V> list(Iterator<T> iterator, Function<T, V> mapping) {
    if (mapping == null) throw new IllegalArgumentException("Mapping function not set!");
    if (iterator == null) return list();
    return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false)
            .filter(Objects::nonNull)
            .map(mapping)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
  }

  /**
   * Creates a list from another collection using a mapping function converting all values.
   *
   * @param collection A collection which values are added to the list.
   * @param mapping    A mapping function applied to all values.
   * @param <T>        Type of collection values before conversion.
   * @param <V>        Type of values in the returned list after conversion.
   * @return A list containing all values contained in the given collection and converted using the mapping function, omitting null elements.
   */
  public static <T, V> List<V> list(Collection<T> collection, Function<T, V> mapping) {
    if (mapping == null) throw new IllegalArgumentException("Mapping function not set!");
    if (collection == null) return list();
    return collection.stream()
            .filter(Objects::nonNull)
            .map(mapping)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
  }

  /**
   * Adds an element to a list unless the element is null.
   * <p>
   * A new list is created if the provided list is null.
   *
   * @param list    List to which the element will be added.
   * @param element Element to add.
   * @param <T>     Type of elements.
   * @return List including added element.
   */
  public static <T> List<T> addToList(List<T> list, T element) {
    if (list == null) list = list();
    if (element == null) return list;
    list.add(element);
    return list;
  }

  /**
   * Concatenates multiple lists into one new list.
   *
   * @param lists Multiple lists which will be concatenated.
   * @param <T>   Type of list values.
   * @return A list containing all values from the given lists.
   */
  @SafeVarargs
  public static <T> List<T> concatenate(List<T>... lists) {
    if (lists == null) return list();
    List<T> result = list();
    for (List<T> l : lists) {
      if (l != null) result.addAll(l);
    }
    return result;
  }

}
