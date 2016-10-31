package no.mnemonic.commons.utilities.collections;

import java.util.Collection;

public class CollectionUtils {

  private CollectionUtils() {
  }

  /**
   * Null-safe operation to test if a collection is empty.
   *
   * @param collection Collection to be tested.
   * @param <T>        Type of collection elements.
   * @return Returns true if the collection is null or contains no elements.
   */
  public static <T> boolean isEmpty(Collection<T> collection) {
    return collection == null || collection.isEmpty();
  }

  /**
   * Null-safe operation to determine the size of a collection.
   *
   * @param collection Collection to compute size for.
   * @return Returns 0 if the collection is null, otherwise the number of elements in the collection.
   */
  public static int size(Collection collection) {
    return collection == null ? 0 : collection.size();
  }

}
