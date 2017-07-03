package no.mnemonic.commons.logging.log4j;

import org.junit.Test;

import static org.junit.Assert.*;

public class Log4jLoggingContextTest {

  private Log4jLoggingContext context = new Log4jLoggingContext();

  @Test
  public void getByNull() throws Exception {
    assertNull(context.get(null));
  }

  @Test
  public void getByEmpty() throws Exception {
    assertNull(context.get(""));
  }

  @Test
  public void putGet() throws Exception {
    context.put("key1", "value1");
    assertEquals("value1", context.get("key1"));
  }

  @Test
  public void putNullValue() throws Exception {
    context.put("key1", null);
    assertFalse(context.containsKey("key1"));
  }

  @Test
  public void putEmptyValue() throws Exception {
    context.put("key1", "");
    assertFalse(context.containsKey("key1"));
  }

  @Test
  public void containsKey() throws Exception {
    context.put("key1", "value1");
    assertTrue(context.containsKey("key1"));
  }

  @Test
  public void containsNullKey() throws Exception {
    assertFalse(context.containsKey(null));
  }

  @Test
  public void containsEmptyKey() throws Exception {
    assertFalse(context.containsKey(""));
  }

  @Test
  public void remove() throws Exception {
    context.put("key1", "value1");
    context.remove("key1");
    assertFalse(context.containsKey("key1"));
  }

  @Test
  public void clear() throws Exception {
    context.put("key1", "value1");
    context.clear();
    assertFalse(context.containsKey("key1"));
  }

}
