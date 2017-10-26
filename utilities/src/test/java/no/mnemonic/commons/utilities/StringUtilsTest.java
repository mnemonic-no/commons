package no.mnemonic.commons.utilities;

import org.junit.Test;

import static no.mnemonic.commons.utilities.StringUtils.*;
import static org.junit.Assert.*;

public class StringUtilsTest {

  @Test
  public void testIsEmpty() {
    assertTrue(isEmpty(null));
    assertTrue(isEmpty(""));
    assertFalse(isEmpty(" "));
    assertFalse(isEmpty("test"));
    assertFalse(isEmpty(" test "));
  }

  @Test
  public void testIsBlank() {
    assertTrue(isBlank(null));
    assertTrue(isBlank(""));
    assertTrue(isBlank(" "));
    assertFalse(isBlank("test"));
    assertFalse(isBlank(" test "));
  }

  @Test
  public void testLength() {
    assertEquals(0, length(null));
    assertEquals(0, length(""));
    assertEquals(1, length(" "));
    assertEquals(4, length("test"));
    assertEquals(6, length(" test "));
  }

  @Test
  public void testIsInteger() {
    assertFalse(isInteger(null));
    assertFalse(isInteger(""));
    assertFalse(isInteger("a"));
    assertFalse(isInteger("1 "));
    assertFalse(isInteger(" 1"));
    assertFalse(isInteger("1 2"));

    assertTrue(isInteger("1"));
    assertTrue(isInteger("-1"));
    assertTrue(isInteger("0"));
    assertTrue(isInteger("999999999999999999999999999999999999999999999999999999999999999999999"));
  }

  @Test
  public void testIsUUID() {
    assertFalse(isUUID(null));
    assertFalse(isUUID(""));
    assertFalse(isUUID("a"));
    assertFalse(isUUID("1"));
    assertFalse(isUUID("g0000000-0000-0000-0000-000000000000"));
    assertFalse(isUUID("]0000000-0000-0000-0000-000000000000"));
    assertFalse(isUUID("00000000-0000-0000-0000-000000000000 "));
    assertFalse(isUUID(" 00000000-0000-0000-0000-000000000000"));

    assertTrue(isUUID("00000000-0000-0000-0000-000000000000"));
    assertTrue(isUUID("abcdef12-0000-0000-0000-000000000000"));
    assertTrue(isUUID("ABCDEF12-0000-0000-0000-000000000000"));
  }
}
