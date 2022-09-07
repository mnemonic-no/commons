package no.mnemonic.commons.utilities.collections;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class SetUtils {

  private SetUtils() {
  }

  /**
   * Creates a set from its arguments.
   *
   * @param values Values to be added to the set.
   * @param <T>    Type of value parameters.
   * @return A set containing all values.
   */
  @SafeVarargs
  public static <T> Set<T> set(T... values) {
    if (values == null) return new HashSet<>();
    return set(Arrays.asList(values));
  }

  /**
   * Creates a set from an iterator.
   *
   * @param iterator An iterator which values are added to the set.
   * @param <T>      Type of iterator values.
   * @return A set containing all values supplied by the given iterator.
   */
  public static <T> Set<T> set(Iterator<T> iterator) {
    return set(iterator, v -> v);
  }

  /**
   * Creates a set from another collection.
   *
   * @param collection A collection which values are added to the set.
   * @param <T>        Type of collection values.
   * @return A set containing all values contained in the given collection, omitting null elements.
   */
  public static <T> Set<T> set(Collection<T> collection) {
    if (collection == null) return new HashSet<>();
    return collection.stream().filter(Objects::nonNull).collect(Collectors.toSet());
  }

  /**
   * Creates a set from its arguments using a mapping function converting all values.
   *
   * @param mapping A mapping function applied to all values.
   * @param values  Values to be added to the set.
   * @param <T>     Type of value parameters before conversion.
   * @param <V>     Type of values in the returned set after conversion.
   * @return A set containing all values converted using the mapping function.
   */
  @SafeVarargs
  public static <T, V> Set<V> set(Function<T, V> mapping, T... values) {
    if (values == null) return set();
    return set(Arrays.asList(values), mapping);
  }

  /**
   * Creates a set from an iterator using a mapping function converting all values.
   *
   * @param iterator An iterator which values are added to the set.
   * @param mapping  A mapping function applied to all values.
   * @param <T>      Type of iterator values before conversion.
   * @param <V>      Type of values in the returned set after conversion.
   * @return A set containing all values supplied by the given iterator and converted using the mapping function.
   */
  public static <T, V> Set<V> set(Iterator<T> iterator, Function<T, V> mapping) {
    if (mapping == null) throw new IllegalArgumentException("Mapping function not set!");
    if (iterator == null) return set();
    return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false)
            .filter(Objects::nonNull)
            .map(mapping)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
  }

  /**
   * Creates a set from another collection using a mapping function converting all values.
   *
   * @param collection A collection which values are added to the set.
   * @param mapping    A mapping function applied to all values.
   * @param <T>        Type of collection values before conversion.
   * @param <V>        Type of values in the returned set after conversion.
   * @return A set containing all values contained in the given collection and converted using the mapping function, omitting null elements.
   */
  public static <T, V> Set<V> set(Collection<T> collection, Function<T, V> mapping) {
    if (mapping == null) throw new IllegalArgumentException("Mapping function not set!");
    if (collection == null) return set();
    return collection.stream()
            .filter(Objects::nonNull)
            .map(mapping)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
  }

  /**
   * Adds an element to a set unless the element is null.
   * <p>
   * A new set is created if the provided set is null.
   *
   * @param set     Set to which the element will be added.
   * @param element Element to add.
   * @param <T>     Type of elements.
   * @return Set including added element.
   */
  public static <T> Set<T> addToSet(Set<T> set, T element) {
    if (set == null) set = set();
    if (element == null) return set;
    set.add(element);
    return set;
  }

  /**
   * Returns true if 'value' is among the argument list of 'values'.
   *
   * @param value  Value to find.
   * @param values Values to search through.
   * @param <T>    Type of values.
   * @return True if 'value' is present in the argument list.
   */
  @SafeVarargs
  public static <T> boolean in(T value, T... values) {
    if (set(values).isEmpty()) return false;

    for (T element : values) {
      if (Objects.equals(value, element)) return true;
    }

    return false;
  }

  /**
   * Returns the intersection of multiple sets.
   * <p>
   * The original sets are not altered.
   *
   * @param sets Multiple sets which will be intersected.
   * @param <T>  Type of values.
   * @return A set forming the intersection of the given sets.
   */
  @SafeVarargs
  public static <T> Set<T> intersection(Set<T>... sets) {
    if (sets == null) return set();
    Set<T> result = set(sets[0]);
    for (int i = 1; i < sets.length; i++) {
      result = intersectTwoSets(result, sets[i]);
    }
    return result;
  }

  /**
   * Tests if multiple sets have common values, i.e. their intersection is not empty.
   *
   * @param sets Multiple sets to test for common values.
   * @param <T>  Type of values.
   * @return True if the given sets intersect.
   */
  @SafeVarargs
  public static <T> boolean intersects(Set<T>... sets) {
    return !intersection(sets).isEmpty();
  }

  /**
   * Returns the union of multiple sets.
   * <p>
   * The original sets are not altered.
   *
   * @param sets Multiple sets which will be united.
   * @param <T>  Type of values.
   * @return A set forming the union of the given sets.
   */
  @SafeVarargs
  public static <T> Set<T> union(Set<T>... sets) {
    if (sets == null) return set();
    Set<T> result = set();
    for (Set<T> s : sets) {
      if (s != null) result.addAll(s);
    }
    return result;
  }

  /**
   * Applies add/remove modifications to a set.
   * <p>
   * This operation permits adding to and removing from one set in a single operation.
   *
   * @param originSet Set to be modified.
   * @param addSet    Set containing elements to be added.
   * @param removeSet Set containing elements to be removed.
   * @param <T>       Type of values.
   * @return Same instance of 'originSet' passed in after modification.
   */
  public static <T> Set<T> modifySet(Set<T> originSet, Set<T> addSet, Set<T> removeSet) {
    if (originSet == null) return null;
    if (!CollectionUtils.isEmpty(addSet)) originSet.addAll(addSet);
    if (!CollectionUtils.isEmpty(removeSet)) originSet.removeAll(removeSet);
    return originSet;
  }

  /**
   * Subtracts every item from @param a that is also present in @param b.
   * Items present in @param b, but not in @param a are ignored.
   * @param a The set to subtract items from
   * @param b The set to compare with
   * @param <T> The type of values
   * @return A new set containing every item in @param a that is not present in @param b.
   */
  public static <T> Set<T> difference(Set<T> a, Set<T> b) {
    Set<T> result = set(a);
    if (a == null || b == null) return result;
    result.removeIf(b::contains);
    return result;
  }

  /**
   * Returns the provided set, or a set with the default value if the provided set is empty/null
   * @param values the values to return
   * @param defaultValue the default value to return if values is null/empty
   * @return values if set is not empty, or set(defaultValue) if values is empty or null
   * @param <T> value type
   */
  public static <T> Set<T> ifEmpty(Set<T> values, T defaultValue) {
    if (CollectionUtils.isEmpty(values)) {
      return set(defaultValue);
    }
    return values;
  }


  private static <T> Set<T> intersectTwoSets(Set<T> a, Set<T> b) {
    if (a == null || b == null) return set();
    Set<T> result = new HashSet<>(a);
    result.retainAll(b);
    return result;
  }

}
