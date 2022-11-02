package no.mnemonic.commons.utilities;

import org.junit.Test;

import java.util.Collection;
import java.util.function.Consumer;

import static no.mnemonic.commons.utilities.collections.ListUtils.list;
import static org.junit.Assert.assertEquals;

public class AppendMembersTest {

  @Test
  public void testNullBean() {
    assertEquals("", AppendUtils.toString(null));
    assertEquals("", extractValue(buf -> AppendUtils.appendAnonField(buf, null)));
    assertEquals("", extractValue(buf -> AppendUtils.appendField(buf, "name", null)));
    assertEquals("", extractValue(buf -> AppendUtils.appendCollection(buf, "col", null)));
    assertEquals("", extractValue(buf -> AppendUtils.appendCollection(buf, "col", null, 3)));
    assertEquals("", extractValue(buf -> AppendUtils.appendIdField(buf, null)));
    assertEquals("", extractValue(buf -> AppendUtils.appendBean(buf, null)));
  }

  @Test
  public void testAppendMembers() {
    assertEquals("[Bean 1: false]", new Bean(1, null, false, list()).toString());
    assertEquals("[Bean 1: name=value false collection=[1, 2]]", new Bean(1, "value", false, list(1, 2)).toString());
  }

  @Test
  public void testAppendMembersWithCollectionAndMaxSize() {
    assertEquals("[BeanWithMaxSize]", new BeanWithMaxSize(list()).toString());
    assertEquals("[BeanWithMaxSize collection=[1, 2]]", new BeanWithMaxSize(list(1, 2)).toString());
    assertEquals("[BeanWithMaxSize collection=<size=4>]", new BeanWithMaxSize(list(1, 2, 3, 4)).toString());
  }

  private String extractValue(Consumer<StringBuilder> consumer) {
    StringBuilder buf = new StringBuilder();
    consumer.accept(buf);
    return buf.toString();
  }

  private static class Bean implements AppendMembers {

    private final long id;
    private final String name;
    private final boolean bool;
    private final Collection<Integer> collection;

    Bean(long id, String name, boolean bool, Collection<Integer> collection) {
      this.id = id;
      this.name = name;
      this.bool = bool;
      this.collection = collection;
    }

    @Override
    public void appendMembers(StringBuilder buf) {
      AppendUtils.appendIdField(buf, id);
      AppendUtils.appendField(buf, "name", name);
      AppendUtils.appendAnonField(buf, bool);
      AppendUtils.appendCollection(buf, "collection", collection);
    }

    @Override
    public String toString() {
      return AppendUtils.toString(this);
    }
  }

  private static class BeanWithMaxSize implements AppendMembers {

    private final Collection<Integer> collection;

    BeanWithMaxSize(Collection<Integer> collection) {
      this.collection = collection;
    }

    @Override
    public void appendMembers(StringBuilder buf) {
      AppendUtils.appendCollection(buf, "collection", collection, 3);
    }

    @Override
    public String toString() {
      return AppendUtils.toString(this);
    }
  }
}
