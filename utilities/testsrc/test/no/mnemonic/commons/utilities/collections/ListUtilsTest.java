package test.no.mnemonic.commons.utilities.collections;

import no.mnemonic.commons.utilities.collections.ListUtils;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;

public class ListUtilsTest {

  @Test
  public void testListReturnsNewList() {
    List<Integer> expected = Arrays.asList(1, 2, 3);

    assertEquals(expected, ListUtils.list(1, 2, 3));
    assertEquals(expected, ListUtils.list(Arrays.asList(1, 2, 3).iterator()));
    assertEquals(expected, ListUtils.list(Arrays.asList(1, 2, 3)));
  }

  @Test
  public void testListWithNullInputReturnsEmptyList() {
    List<Integer> expected = new ArrayList<>();

    assertEquals(expected, ListUtils.list());
    assertEquals(expected, ListUtils.list((Iterator) null));
    assertEquals(expected, ListUtils.list((Collection) null));
  }

  @Test
  public void testListWithMappingReturnsNewList() {
    List<String> expected = Arrays.asList("1", "2", "3");

    assertEquals(expected, ListUtils.list(Object::toString, 1, 2, 3));
    assertEquals(expected, ListUtils.list(Arrays.asList(1, 2, 3).iterator(), Object::toString));
    assertEquals(expected, ListUtils.list(Arrays.asList(1, 2, 3), Object::toString));
  }

  @Test
  public void testListWithMappingAndNullInputReturnsEmptyList() {
    List<String> expected = new ArrayList<>();

    assertEquals(expected, ListUtils.list(Object::toString));
    assertEquals(expected, ListUtils.list((Iterator) null, Object::toString));
    assertEquals(expected, ListUtils.list((Collection) null, Object::toString));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testListWithVarargsAndNullMappingThrowsException() {
    ListUtils.list(null, 1, 2, 3);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testListWithIteratorAndNullMappingThrowsException() {
    ListUtils.list(Arrays.asList(1, 2, 3).iterator(), null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testListWithCollectionAndNullMappingThrowsException() {
    ListUtils.list(Arrays.asList(1, 2, 3), null);
  }

  @Test
  public void testAddToList() {
    assertEquals(Arrays.asList(1, 2, 3), ListUtils.addToList(new ArrayList<>(Arrays.asList(1, 2)), 3));
    assertEquals(Collections.singletonList(3), ListUtils.addToList(null, 3));
    assertEquals(Arrays.asList(1, 2), ListUtils.addToList(new ArrayList<>(Arrays.asList(1, 2)), null));
  }

  @Test
  public void testConcatenate() {
    assertEquals(Arrays.asList(1, 2, 3, 4, 5), ListUtils.concatenate(Arrays.asList(1, 2), Arrays.asList(3, 4, 5)));
    assertEquals(Arrays.asList(1, 2, 1, 2), ListUtils.concatenate(Arrays.asList(1, 2), Arrays.asList(1, 2)));
    assertEquals(Arrays.asList(1, 2), ListUtils.concatenate(Arrays.asList(1, 2), null));
    assertEquals(Arrays.asList(1, 2), ListUtils.concatenate(null, Arrays.asList(1, 2)));
    assertEquals(Arrays.asList(1, 2), ListUtils.concatenate(null, Arrays.asList(1, 2), null));
    assertEquals(new ArrayList<Integer>(), ListUtils.concatenate());
    assertEquals(new ArrayList<Integer>(), ListUtils.concatenate(null));
  }

}
