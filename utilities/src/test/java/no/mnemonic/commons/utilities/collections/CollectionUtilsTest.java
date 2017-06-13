package no.mnemonic.commons.utilities.collections;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import static org.junit.Assert.*;

public class CollectionUtilsTest {

  @Test
  public void testIsEmpty() {
    assertTrue(CollectionUtils.isEmpty(null));
    assertTrue(CollectionUtils.isEmpty(Collections.emptyList()));
    assertTrue(CollectionUtils.isEmpty(Collections.emptySet()));
    assertFalse(CollectionUtils.isEmpty(Collections.singletonList(1)));
    assertFalse(CollectionUtils.isEmpty(Collections.singleton(1)));
    assertFalse(CollectionUtils.isEmpty(Arrays.asList(1, 2, 3)));
    assertFalse(CollectionUtils.isEmpty(new HashSet<>(Arrays.asList(1, 2, 2, 3))));
  }

  @Test
  public void testSize() {
    assertEquals(0, CollectionUtils.size(null));
    assertEquals(0, CollectionUtils.size(Collections.emptyList()));
    assertEquals(0, CollectionUtils.size(Collections.emptySet()));
    assertEquals(1, CollectionUtils.size(Collections.singletonList(1)));
    assertEquals(1, CollectionUtils.size(Collections.singleton(1)));
    assertEquals(3, CollectionUtils.size(Arrays.asList(1, 2, 3)));
    assertEquals(3, CollectionUtils.size(new HashSet<>(Arrays.asList(1, 2, 2, 3))));
  }

}
