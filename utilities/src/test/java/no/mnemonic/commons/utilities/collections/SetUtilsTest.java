package no.mnemonic.commons.utilities.collections;

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class SetUtilsTest {

  @Test
  public void testSetReturnsNewSet() {
    Set<Integer> expected = new HashSet<>(Arrays.asList(1, 2, 3));

    assertEquals(expected, SetUtils.set(1, 2, 2, 3));
    assertEquals(expected, SetUtils.set(Arrays.asList(1, 2, 2, 3).iterator()));
    assertEquals(expected, SetUtils.set(Arrays.asList(1, 2, 2, 3)));
  }

  @Test
  public void testSetWithNullInputReturnsEmptySet() {
    Set<Integer> expected = new HashSet<>();

    assertEquals(expected, SetUtils.set());
    assertEquals(expected, SetUtils.set((Iterator) null));
    assertEquals(expected, SetUtils.set((Collection) null));
  }

  @Test
  public void testSetOmitsEmptyElements() {
    Set<Integer> expected = new HashSet<>(Arrays.asList(1, 2, 3));

    assertEquals(expected, SetUtils.set(Arrays.asList(1, 2, null, 3)));
    assertEquals(expected, SetUtils.set(new HashSet<>(Arrays.asList(1, 2, null, 3))));
    assertEquals(expected, SetUtils.set(ListUtils.list(1, 2, null, 3)));

    assertEquals(expected, SetUtils.set(Arrays.asList(1, 2, null, 3).iterator()));
    assertEquals(expected, SetUtils.set(new HashSet<>(Arrays.asList(1, 2, null, 3))));
    assertEquals(expected, SetUtils.set(ListUtils.list(1, 2, null, 3)));
  }

  @Test
  public void testSetWithMappingOmitsEmptyElements() {
    Set<Long> expected = new HashSet<>(Arrays.asList(1L, 2L, 3L));

    assertEquals(expected, SetUtils.set(Arrays.asList(1, 2, null, 3), Long::valueOf));
    assertEquals(expected, SetUtils.set(new HashSet<>(Arrays.asList(1, 2, null, 3)), Long::valueOf));
    assertEquals(expected, SetUtils.set(ListUtils.list(1, 2, null, 3), Long::valueOf));

    assertEquals(SetUtils.set(), SetUtils.set(Arrays.asList(1, 2, null, 3), i->null));
    assertEquals(SetUtils.set(), SetUtils.set(ListUtils.list(1, 2, null, 3), i->null));
    assertEquals(SetUtils.set(), SetUtils.set(ListUtils.list(1, 2, null, 3).iterator(), i->null));
  }

  @Test
  public void testSetWithMappingReturnsNewSet() {
    Set<String> expected = new HashSet<>(Arrays.asList("1", "2", "3"));

    assertEquals(expected, SetUtils.set(Object::toString, 1, 2, 2, 3));
    assertEquals(expected, SetUtils.set(Arrays.asList(1, 2, 2, 3).iterator(), Object::toString));
    assertEquals(expected, SetUtils.set(Arrays.asList(1, 2, 2, 3), Object::toString));
  }

  @Test
  public void testSetWithMappingAndNullInputReturnsEmptySet() {
    Set<String> expected = new HashSet<>();

    assertEquals(expected, SetUtils.set(Object::toString));
    assertEquals(expected, SetUtils.set((Iterator) null, Object::toString));
    assertEquals(expected, SetUtils.set((Collection) null, Object::toString));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSetWithVarargsAndNullMappingThrowsException() {
    SetUtils.set(null, 1, 2, 3);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSetWithIteratorAndNullMappingThrowsException() {
    SetUtils.set(Arrays.asList(1, 2, 3).iterator(), null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSetWithCollectionAndNullMappingThrowsException() {
    SetUtils.set(Arrays.asList(1, 2, 3), null);
  }

  @Test
  public void testAddToSet() {
    assertEquals(new HashSet<>(Arrays.asList(1, 2, 3)), SetUtils.addToSet(new HashSet<>(Arrays.asList(1, 2)), 3));
    assertEquals(Collections.singleton(3), SetUtils.addToSet(null, 3));
    assertEquals(new HashSet<>(Arrays.asList(1, 2)), SetUtils.addToSet(new HashSet<>(Arrays.asList(1, 2)), null));
  }

  @Test
  public void testIn() {
    assertTrue(SetUtils.in(1, 1, 2, 3));
    assertFalse(SetUtils.in(1, 2, 3));
    assertFalse(SetUtils.in(1));
    assertFalse(SetUtils.in(1, null));
  }

  @Test
  public void testIntersection() {
    assertEquals(new HashSet<>(Arrays.asList(2, 3)), SetUtils.intersection(new HashSet<>(Arrays.asList(1, 2, 3)), new HashSet<>(Arrays.asList(2, 3, 4))));
    assertEquals(new HashSet<>(Arrays.asList(2, 3)), SetUtils.intersection(new HashSet<>(Arrays.asList(1, 2, 3)), new HashSet<>(Arrays.asList(2, 3, 4)), new HashSet<>(Arrays.asList(1, 2, 3))));
    assertEquals(new HashSet<>(), SetUtils.intersection(new HashSet<>(Arrays.asList(1, 2)), new HashSet<>(Arrays.asList(3, 4))));
    assertEquals(new HashSet<>(), SetUtils.intersection(new HashSet<>(Arrays.asList(1, 2, 3)), new HashSet<>(Arrays.asList(3, 4)), new HashSet<>(Arrays.asList(4, 5))));
    assertEquals(new HashSet<>(), SetUtils.intersection(new HashSet<>(), new HashSet<>(Arrays.asList(3, 4))));
    assertEquals(new HashSet<>(), SetUtils.intersection(new HashSet<>(Arrays.asList(1, 2)), new HashSet<>()));
    assertEquals(new HashSet<>(), SetUtils.intersection(null, new HashSet<>(Arrays.asList(3, 4))));
    assertEquals(new HashSet<>(), SetUtils.intersection(new HashSet<>(Arrays.asList(1, 2)), null));
    assertEquals(new HashSet<>(), SetUtils.intersection(new HashSet<>()));
    assertEquals(new HashSet<>(), SetUtils.intersection(null));
  }

  @Test
  public void testIntersects() {
    assertTrue(SetUtils.intersects(new HashSet<>(Arrays.asList(1, 2, 3)), new HashSet<>(Arrays.asList(2, 3, 4))));
    assertTrue(SetUtils.intersects(new HashSet<>(Arrays.asList(1, 2, 3)), new HashSet<>(Arrays.asList(2, 3, 4)), new HashSet<>(Arrays.asList(1, 2, 3))));
    assertFalse(SetUtils.intersects(new HashSet<>(Arrays.asList(1, 2)), new HashSet<>(Arrays.asList(3, 4))));
    assertFalse(SetUtils.intersects(new HashSet<>(Arrays.asList(1, 2, 3)), new HashSet<>(Arrays.asList(3, 4)), new HashSet<>(Arrays.asList(4, 5))));
    assertFalse(SetUtils.intersects(new HashSet<>(), new HashSet<>(Arrays.asList(3, 4))));
    assertFalse(SetUtils.intersects(new HashSet<>(Arrays.asList(1, 2)), new HashSet<>()));
    assertFalse(SetUtils.intersects(null, new HashSet<>(Arrays.asList(3, 4))));
    assertFalse(SetUtils.intersects(new HashSet<>(Arrays.asList(1, 2)), null));
    assertFalse(SetUtils.intersects(new HashSet<>()));
    assertFalse(SetUtils.intersects(null));
  }

  @Test
  public void testUnion() {
    assertEquals(new HashSet<>(Arrays.asList(1, 2, 3, 4)), SetUtils.union(new HashSet<>(Arrays.asList(1, 2, 3)), new HashSet<>(Arrays.asList(2, 3, 4))));
    assertEquals(new HashSet<>(Arrays.asList(1, 2, 3, 4)), SetUtils.union(new HashSet<>(Arrays.asList(1, 2)), new HashSet<>(Arrays.asList(3, 4))));
    assertEquals(new HashSet<>(Arrays.asList(1, 2, 3)), SetUtils.union(Collections.singleton(1), Collections.singleton(2), Collections.singleton(3)));
    assertEquals(new HashSet<>(Arrays.asList(3, 4)), SetUtils.union(new HashSet<>(), new HashSet<>(Arrays.asList(3, 4))));
    assertEquals(new HashSet<>(Arrays.asList(1, 2)), SetUtils.union(new HashSet<>(Arrays.asList(1, 2)), new HashSet<>()));
    assertEquals(new HashSet<>(Arrays.asList(3, 4)), SetUtils.union(null, new HashSet<>(Arrays.asList(3, 4))));
    assertEquals(new HashSet<>(Arrays.asList(1, 2)), SetUtils.union(new HashSet<>(Arrays.asList(1, 2)), null));
    assertEquals(new HashSet<>(), SetUtils.union(new HashSet<>()));
    assertEquals(new HashSet<>(), SetUtils.union(null));
  }

  @Test
  public void testModifySet() {
    assertEquals(new HashSet<>(Arrays.asList(2, 3)), SetUtils.modifySet(new HashSet<>(Arrays.asList(1, 2)), Collections.singleton(3), Collections.singleton(1)));
    assertEquals(new HashSet<>(Arrays.asList(1, 2)), SetUtils.modifySet(new HashSet<>(Arrays.asList(1, 2)), Collections.singleton(3), Collections.singleton(3)));
    assertEquals(new HashSet<>(Arrays.asList(1, 2, 3)), SetUtils.modifySet(new HashSet<>(Arrays.asList(1, 2)), Collections.singleton(3), new HashSet<>()));
    assertEquals(new HashSet<>(Arrays.asList(1, 2, 3)), SetUtils.modifySet(new HashSet<>(Arrays.asList(1, 2)), Collections.singleton(3), null));
    assertEquals(new HashSet<>(Arrays.asList(1, 2)), SetUtils.modifySet(new HashSet<>(Arrays.asList(1, 2, 3)), new HashSet<>(), Collections.singleton(3)));
    assertEquals(new HashSet<>(Arrays.asList(1, 2)), SetUtils.modifySet(new HashSet<>(Arrays.asList(1, 2, 3)), null, Collections.singleton(3)));
    assertEquals(new HashSet<>(Arrays.asList(1, 2)), SetUtils.modifySet(new HashSet<>(Arrays.asList(1, 2)), new HashSet<>(), Collections.singleton(3)));
    assertEquals(new HashSet<>(), SetUtils.modifySet(new HashSet<>(), new HashSet<>(), new HashSet<>()));
    assertNull(SetUtils.modifySet(null, new HashSet<>(), new HashSet<>()));
  }

  @Test
  public void testModifySetReturnsOriginSet() {
    Set<Integer> originSet = new HashSet<>(Arrays.asList(1, 2));
    assertSame(originSet, SetUtils.modifySet(originSet, Collections.singleton(3), Collections.singleton(1)));
  }

  @Test
  public void testDifference() {
    assertEquals(new HashSet<>(Arrays.asList(1, 2)), SetUtils.difference(new HashSet<>(Arrays.asList(1, 2, 3)), new HashSet<>(Arrays.asList(3, 4, 5))));
    assertEquals(new HashSet<>(Arrays.asList(1, 2, 3)), SetUtils.difference(new HashSet<>(Arrays.asList(1, 2, 3)), new HashSet<>(Arrays.asList(4, 5, 6))));
    assertEquals(new HashSet<>(Collections.singletonList(1)), SetUtils.difference(new HashSet<>(Arrays.asList(1, 2, 3)), new HashSet<>(Arrays.asList(2, 3, 4))));
    assertEquals(new HashSet<>(Collections.emptyList()), SetUtils.difference(null, new HashSet<>(Arrays.asList(3, 4, 5))));
    assertEquals(new HashSet<>(Collections.emptyList()), SetUtils.difference(null, null));
    assertEquals(new HashSet<>(Arrays.asList(1, 2, 3)), SetUtils.difference(new HashSet<>(Arrays.asList(1, 2, 3)), null));
    assertEquals(new HashSet<>(Collections.emptyList()), SetUtils.difference(new HashSet<>(Arrays.asList(1, 2, 3)), new HashSet<>(Arrays.asList(1, 2, 3))));
    assertEquals(new HashSet<>(Arrays.asList(1, 2, 3)), SetUtils.difference(new HashSet<>(Arrays.asList(1, 2, 3)), new HashSet<>()));
    assertEquals(new HashSet<>(Collections.emptyList()), SetUtils.difference(new HashSet<>(), new HashSet<>(Arrays.asList(1, 2, 3))));
  }

  @Test
  public void testIfEmpty() {
    assertEquals(SetUtils.set("a","b","c"), SetUtils.ifEmpty(SetUtils.set("a","b","c"), "dv"));
    assertEquals(SetUtils.set("dv"), SetUtils.ifEmpty(SetUtils.set(), "dv"));
    assertEquals(SetUtils.set("dv"), SetUtils.ifEmpty(null, "dv"));
  }
}
