package test.no.mnemonic.commons.utilities;

import no.mnemonic.commons.utilities.ObjectUtils;
import org.junit.Test;

import static org.junit.Assert.*;

public class ObjectUtilsTest {

  @Test
  public void notNullReturnsValueOnNotNull() {
    Object value = new Object();
    assertEquals(value, ObjectUtils.notNull(value, "notThrown"));
  }

  @Test
  public void notNullThrowsExceptionOnNull() {
    try {
      ObjectUtils.notNull(null, "test");
      fail();
    } catch (Exception e) {
      assertEquals("test", e.getMessage());
    }
  }

  @Test
  public void ifNullReturnsValueOnNotNull() {
    Object value = new Object();
    assertEquals(value, ObjectUtils.ifNull(value, "defaultValue"));
    assertEquals(value, ObjectUtils.ifNull(value, () -> "defaultValue"));
  }

  @Test
  public void ifNullReturnsDefaultValueOnNull() {
    assertEquals("defaultValue", ObjectUtils.ifNull(null, "defaultValue"));
    assertEquals("defaultValue", ObjectUtils.ifNull(null, () -> "defaultValue"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void ifNullWithoutSupplierThrowsException() {
    ObjectUtils.ifNull(42, null);
  }

  @Test
  public void ifNotNullReturnsConvertedValue() {
    assertEquals("convertedValue", ObjectUtils.ifNotNull(42, (v) -> "convertedValue"));
    assertEquals("convertedValue", ObjectUtils.ifNotNull(42, (v) -> "convertedValue", "nullValue"));
  }

  @Test
  public void ifNotNullReturnsNullValue() {
    assertNull(ObjectUtils.ifNotNull(null, (v) -> "convertedValue"));
    assertEquals("nullValue", ObjectUtils.ifNotNull(null, (v) -> "convertedValue", "nullValue"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void ifNotNullWithoutConverterThrowsException() {
    ObjectUtils.ifNotNull(42, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void ifNotNullWithNullValueWithoutConverterThrowsException() {
    ObjectUtils.ifNotNull(42, null, "nullValue");
  }

}
