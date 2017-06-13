package no.mnemonic.commons.utilities;

import org.junit.Test;

import static org.junit.Assert.*;

public class StringUtilsTest {

  @Test
  public void testIsEmpty() {
    assertTrue(StringUtils.isEmpty(null));
    assertTrue(StringUtils.isEmpty(""));
    assertFalse(StringUtils.isEmpty(" "));
    assertFalse(StringUtils.isEmpty("test"));
    assertFalse(StringUtils.isEmpty(" test "));
  }

  @Test
  public void testIsBlank() {
    assertTrue(StringUtils.isBlank(null));
    assertTrue(StringUtils.isBlank(""));
    assertTrue(StringUtils.isBlank(" "));
    assertFalse(StringUtils.isBlank("test"));
    assertFalse(StringUtils.isBlank(" test "));
  }

  @Test
  public void testLength() {
    assertEquals(0, StringUtils.length(null));
    assertEquals(0, StringUtils.length(""));
    assertEquals(1, StringUtils.length(" "));
    assertEquals(4, StringUtils.length("test"));
    assertEquals(6, StringUtils.length(" test "));
  }

}
