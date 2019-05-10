package no.mnemonic.commons.utilities.collections;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MapUtils {

  private MapUtils() {
  }

  /**
   * Creates a map from a map
   *
   * @param map   Map to create new map from
   * @param <K>   Type of keys.
   * @param <V>   Type of values.
   * @return A new map containing the entries of the provided map, or an empty map if the provided argument was null
   */
  public static <K, V> Map<K, V> map(Map<K, V> map) {
    if (map == null) return new HashMap<>();
    return new HashMap<>(map);
  }
  /**
   * Creates a map from key-value pairs.
   *
   * @param pairs Pairs converted to key-value entries in the map.
   * @param <K>   Type of keys.
   * @param <V>   Type of values.
   * @return A map containing the key-value pairs.
   */
  @SafeVarargs
  public static <K, V> Map<K, V> map(Pair<K, V>... pairs) {
    if (pairs == null) return new HashMap<>();
    return map(Arrays.asList(pairs));
  }

  /**
   * Creates a map from an iterator supplying key-value pairs.
   *
   * @param iterator An iterator supplying key-value pairs which are added to the map.
   * @param <K>      Type of keys.
   * @param <V>      Type of values.
   * @return A map containing all key-value pairs supplied by the given iterator.
   */
  public static <K, V> Map<K, V> map(Iterator<Pair<K, V>> iterator) {
    return map(iterator, p -> p);
  }

  /**
   * Creates a map from a collection containing key-value pairs.
   *
   * @param collection A collection containing key-value pairs which are added to the map.
   * @param <K>        Type of keys.
   * @param <V>        Type of values.
   * @return A map containing all key-value pairs contained in the given collection.
   */
  public static <K, V> Map<K, V> map(Collection<Pair<K, V>> collection) {
    return map(collection, p -> p);
  }

  /**
   * Creates a map from its arguments using a mapping function to extract key-value pairs.
   *
   * @param mapping A mapping function applied to all values to extract key-value pairs.
   * @param values  Values which are added to the map.
   * @param <K>     Type of keys.
   * @param <V>     Type of values.
   * @param <T>     Type of argument values.
   * @return A map containing all key-value pairs extracted from the values.
   */
  @SafeVarargs
  public static <K, V, T> Map<K, V> map(Function<T, Pair<K, V>> mapping, T... values) {
    if (values == null) return new HashMap<>();
    return map(Arrays.asList(values), mapping);
  }

  /**
   * Creates a map from an iterator using a mapping function to extract key-value pairs.
   *
   * @param iterator An iterator which values are added to the map.
   * @param mapping  A mapping function applied to all values supplied by 'iterator' to extract key-value pairs.
   * @param <K>      Type of keys.
   * @param <V>      Type of values.
   * @param <T>      Type of elements supplied by 'iterator'.
   * @return A map containing all key-value pairs extracted from the given iterator.
   */
  public static <K, V, T> Map<K, V> map(Iterator<T> iterator, Function<T, Pair<K, V>> mapping) {
    if (mapping == null) throw new IllegalArgumentException("Mapping function not set!");
    if (iterator == null) return new HashMap<>();
    Map<K, V> result = new HashMap<>();
    iterator.forEachRemaining(o -> {
      Pair<K, V> p = mapping.apply(o);
      result.put(p.fst, p.snd);
    });
    return result;
  }

  /**
   * Creates a map from a collection using a mapping function to extract key-value pairs.
   *
   * @param collection A collection which values are added to the map.
   * @param mapping    A mapping function applied to all values contained in 'collection' to extract key-value pairs.
   * @param <K>        Type of keys.
   * @param <V>        Type of values.
   * @param <T>        Type of elements in 'collection'.
   * @return A map containing all key-value pairs extracted from the given collection.
   */
  public static <K, V, T> Map<K, V> map(Collection<T> collection, Function<T, Pair<K, V>> mapping) {
    if (mapping == null) throw new IllegalArgumentException("Mapping function not set!");
    if (collection == null) return new HashMap<>();
    Map<K, V> result = new HashMap<>();
    collection.forEach(o -> {
      Pair<K, V> p = mapping.apply(o);
      result.put(p.fst, p.snd);
    });
    return result;
  }

  /**
   * Adds a key-value pair to a map unless the key or the value are null.
   * <p>
   * A new map is created if the provided map is null.
   *
   * @param map   Map to which the key-value pair will be added.
   * @param key   Key in the map.
   * @param value Value added for the given key.
   * @param <K>   Type of keys.
   * @param <V>   Type of values.
   * @return Map including added key-value pair.
   */
  public static <K, V> Map<K, V> addToMap(Map<K, V> map, K key, V value) {
    if (map == null) map = new HashMap<>();
    if (key == null || value == null) return map;
    map.put(key, value);
    return map;
  }

  /**
   * Concatenates multiple maps into one new map.
   * <p>
   * If the same key exists in multiple maps the value stored in the last map will be used.
   *
   * @param maps Multiple maps which will be concatenated.
   * @param <K>  Type of keys.
   * @param <V>  Type of values.
   * @return A map containing all key-value pairs from the given maps.
   */
  @SafeVarargs
  public static <K, V> Map<K, V> concatenate(Map<K, V>... maps) {
    if (maps == null) return new HashMap<>();
    Map<K, V> result = new HashMap<>();
    for (Map<K, V> m : maps) {
      if (m != null) result.putAll(m);
    }
    return result;
  }

  /**
   * Applies add/remove modifications to a map.
   * <p>
   * This operation permits adding to and removing from one map in a single operation.
   *
   * @param originMap  Map to be modified.
   * @param addMap     Map containing key-value pairs to be added.
   * @param removeKeys Collection containing keys to be removed.
   * @param <K>        Type of keys.
   * @param <V>        Type of values.
   * @return Same instance of 'originMap' passed in after modification.
   */
  public static <K, V> Map<K, V> modifyMap(Map<K, V> originMap, Map<K, V> addMap, Collection<K> removeKeys) {
    if (originMap == null) return null;
    if (!isEmpty(addMap)) originMap.putAll(addMap);
    if (!CollectionUtils.isEmpty(removeKeys)) removeKeys.forEach(originMap::remove);
    return originMap;
  }

  /**
   * Creates a subset of the given map based on a collection of keys. The original map is not altered.
   *
   * @param originMap  Map to be filtered.
   * @param retainKeys Keys to retain in subset map.
   * @param <K>        Type of keys.
   * @param <V>        Type of values.
   * @return A new map containing only the keys listed in 'retainKeys'.
   */
  public static <K, V> Map<K, V> filterMap(Map<K, V> originMap, Collection<K> retainKeys) {
    if (originMap == null || retainKeys == null) return new HashMap<>();
    return originMap
            .entrySet()
            .stream()
            .filter(e -> retainKeys.contains(e.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  /**
   * Null-safe operation to test if a map is empty.
   *
   * @param map Map to be tested.
   * @param <K> Type of keys in map.
   * @param <V> Type of values in map.
   * @return Returns true if the map is null or contains no elements.
   */
  public static <K, V> boolean isEmpty(Map<K, V> map) {
    return map == null || map.isEmpty();
  }

  /**
   * Null-safe operation to determine the size of a map.
   *
   * @param map Map to compute size for.
   * @param <K> Type of keys in map.
   * @param <V> Type of values in map.
   * @return Returns 0 if the map is null, otherwise the number of elements in the map.
   */
  public static <K, V> int size(Map<K, V> map) {
    return map == null ? 0 : map.size();
  }

  /**
   * Convenience method to create a key/value {@link Pair}.
   *
   * @param key Key
   * @param val Value
   * @param <K> Type of key
   * @param <V> Type of value
   * @return A key/value pair
   */
  public static <K, V> Pair<K, V> pair(K key, V val) {
    return Pair.T(key, val);
  }

  public final static class Pair<A, B> {

    private final A fst;
    private final B snd;

    private Pair(A fst, B snd) {
      this.fst = fst;
      this.snd = snd;
    }

    public static <A, B> Pair<A, B> T(A fst, B snd) {
      return new Pair<>(fst, snd);
    }

  }

}
