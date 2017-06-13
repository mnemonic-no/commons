package no.mnemonic.commons.utilities.collections;

import org.junit.Test;

import java.util.*;

import static no.mnemonic.commons.utilities.collections.MapUtils.Pair.T;
import static no.mnemonic.commons.utilities.collections.MapUtils.pair;
import static org.junit.Assert.*;

public class MapUtilsTest {

  @Test
  public void testMapExistingMap() {
    Map<String, Integer> expected = createMap(3);
    assertNotSame(expected, MapUtils.map(expected));
    assertEquals(expected, MapUtils.map(expected));
    assertEquals(new HashMap<>(), MapUtils.map((Map<String, Integer>)null));
  }

  @Test
  public void testMapReturnsNewMap() {
    Map<String, Integer> expected = createMap(3);

    assertEquals(expected, MapUtils.map(pair("1", 1), pair("2", 2), pair("3", 3)));
    assertEquals(expected, MapUtils.map(T("1", 1), T("2", 2), T("3", 3)));
    assertEquals(expected, MapUtils.map(Arrays.asList(T("1", 1), T("2", 2), T("3", 3)).iterator()));
    assertEquals(expected, MapUtils.map(Arrays.asList(T("1", 1), T("2", 2), T("3", 3))));
  }

  @Test
  public void testMapWithNullInputReturnsEmptySet() {
    Map<String, Integer> expected = new HashMap<>();

    assertEquals(expected, MapUtils.map());
    assertEquals(expected, MapUtils.map((Iterator<MapUtils.Pair<String, Integer>>) null));
    assertEquals(expected, MapUtils.map((Collection<MapUtils.Pair<String, Integer>>) null));
  }

  @Test
  public void testMapWithMappingReturnsNewMap() {
    Map<String, Integer> expected = createMap(3);

    assertEquals(expected, MapUtils.map(i -> T(String.valueOf(i), i), 1, 2, 3));
    assertEquals(expected, MapUtils.map(Arrays.asList(1, 2, 3).iterator(), i -> T(String.valueOf(i), i)));
    assertEquals(expected, MapUtils.map(Arrays.asList(1, 2, 3), i -> T(String.valueOf(i), i)));
  }

  @Test
  public void testMapWithMappingAndNullInputReturnsEmptyMap() {
    Map<String, Integer> expected = new HashMap<>();

    assertEquals(expected, MapUtils.map(i -> T(String.valueOf(i), i)));
    assertEquals(expected, MapUtils.map((Iterator<Integer>) null, i -> T(String.valueOf(i), i)));
    assertEquals(expected, MapUtils.map((Collection<Integer>) null, i -> T(String.valueOf(i), i)));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testMapWithVarargsAndNullMappingThrowsException() {
    MapUtils.map(null, 1, 2, 3);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testMapWithIteratorAndNullMappingThrowsException() {
    MapUtils.map(Arrays.asList(1, 2, 3).iterator(), null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testMapWithCollectionAndNullMappingThrowsException() {
    MapUtils.map(Arrays.asList(1, 2, 3), null);
  }

  @Test
  public void testAddToMap() {
    assertEquals(createMap(3), MapUtils.addToMap(createMap(2), "3", 3));
    assertEquals(Collections.singletonMap("3", 3), MapUtils.addToMap(null, "3", 3));
    assertEquals(createMap(2), MapUtils.addToMap(createMap(2), null, 3));
    assertEquals(createMap(2), MapUtils.addToMap(createMap(2), "3", null));
  }

  @Test
  public void testConcatenate() {
    assertEquals(createMap(5), MapUtils.concatenate(createMap(3), createMap(2, 3)));
    assertEquals(createMap(3), MapUtils.concatenate(createMap(3), null));
    assertEquals(createMap(3), MapUtils.concatenate(null, createMap(3)));
    assertEquals(createMap(3), MapUtils.concatenate(null, createMap(3), null));
    assertEquals(new HashMap<String, Integer>(), MapUtils.concatenate());
    assertEquals(new HashMap<String, Integer>(), MapUtils.concatenate(null));
  }

  @Test
  public void testConcatenateSameKeysAreOverriddenByLastValue() {
    Map<String, Integer> expected = MapUtils.map(Arrays.asList(1, 2, 3), i -> T(String.valueOf(i), i * 2));
    assertEquals(expected, MapUtils.concatenate(createMap(3), expected));
  }

  @Test
  public void testModifyMap() {
    assertEquals(createMap(3, 1), MapUtils.modifyMap(createMap(3), Collections.singletonMap("4", 4), Collections.singletonList("1")));
    assertEquals(createMap(3), MapUtils.modifyMap(createMap(3), Collections.singletonMap("4", 4), Collections.singletonList("4")));
    assertEquals(createMap(4), MapUtils.modifyMap(createMap(3), Collections.singletonMap("4", 4), new HashSet<>()));
    assertEquals(createMap(4), MapUtils.modifyMap(createMap(3), Collections.singletonMap("4", 4), null));
    assertEquals(createMap(2), MapUtils.modifyMap(createMap(3), new HashMap<>(), Collections.singletonList("3")));
    assertEquals(createMap(2), MapUtils.modifyMap(createMap(3), null, Collections.singletonList("3")));
    assertEquals(createMap(2), MapUtils.modifyMap(createMap(2), new HashMap<>(), Collections.singletonList("3")));
    assertEquals(new HashMap<>(), MapUtils.modifyMap(new HashMap<>(), new HashMap<>(), new HashSet<>()));
    assertNull(MapUtils.modifyMap(null, new HashMap<>(), new HashSet<>()));
  }

  @Test
  public void testModifyMapReturnsOriginMap() {
    Map<String, Integer> originMap = createMap(3);
    assertSame(originMap, MapUtils.modifyMap(originMap, Collections.singletonMap("4", 4), Collections.singletonList("1")));
  }

  @Test
  public void testFilterMap() {
    assertEquals(createMap(3), MapUtils.filterMap(createMap(5), Arrays.asList("1", "2", "3")));
    assertEquals(new HashMap<>(), MapUtils.filterMap(createMap(3), Arrays.asList("4", "5")));
    assertEquals(new HashMap<>(), MapUtils.filterMap(createMap(3), new HashSet<>()));
    assertEquals(new HashMap<>(), MapUtils.filterMap(createMap(3), null));
    assertEquals(new HashMap<>(), MapUtils.filterMap(new HashMap<>(), Arrays.asList("1", "2", "3")));
    assertEquals(new HashMap<>(), MapUtils.filterMap(null, Arrays.asList("1", "2", "3")));
  }

  @Test
  public void testFilterMapReturnsNewMap() {
    Map<String, Integer> originMap = createMap(5);
    assertNotSame(originMap, MapUtils.filterMap(originMap, Arrays.asList("1", "2", "3")));
  }

  @Test
  public void testIsEmpty() {
    assertTrue(MapUtils.isEmpty(null));
    assertTrue(MapUtils.isEmpty(Collections.emptyMap()));
    assertFalse(MapUtils.isEmpty(Collections.singletonMap("key", "value")));
    assertFalse(MapUtils.isEmpty(createMap(3)));
  }

  @Test
  public void testSize() {
    assertEquals(0, MapUtils.size(null));
    assertEquals(0, MapUtils.size(Collections.emptyMap()));
    assertEquals(1, MapUtils.size(Collections.singletonMap("key", "value")));
    assertEquals(3, MapUtils.size(createMap(3)));
  }

  private Map<String, Integer> createMap(int numberOfEntries) {
    return createMap(numberOfEntries, 0);
  }

  private Map<String, Integer> createMap(int numberOfEntries, int offset) {
    HashMap<String, Integer> map = new HashMap<>();
    for (int i = offset + 1; i <= offset + numberOfEntries; i++) {
      map.put(String.valueOf(i), i);
    }
    return map;
  }

}
