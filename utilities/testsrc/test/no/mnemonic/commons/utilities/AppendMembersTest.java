package test.no.mnemonic.commons.utilities;

import no.mnemonic.commons.utilities.AppendMembers;
import no.mnemonic.commons.utilities.AppendUtils;
import org.junit.Test;

import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;

public class AppendMembersTest {

  @Test
  public void testNullBean() {
    assertEquals("", AppendUtils.toString(null));
    assertEquals("", extractValue(buf->AppendUtils.appendAnonField(buf, null)));
    assertEquals("", extractValue(buf->AppendUtils.appendField(buf, "name", null)));
    assertEquals("", extractValue(buf->AppendUtils.appendIdField(buf, null)));
    assertEquals("", extractValue(buf->AppendUtils.appendBean(buf, null)));
  }

  @Test
  public void testAppendMembers() {
    assertEquals("[Bean 1: false]", new Bean(1, null, false).toString());
    assertEquals("[Bean 1: name=value false]", new Bean(1, "value", false).toString());
  }

  private String extractValue(Consumer<StringBuilder> consumer) {
    StringBuilder buf = new StringBuilder();
    consumer.accept(buf);
    return buf.toString();
  }

  private static class Bean implements AppendMembers {

    private long id;
    private String name;
    private boolean bool;

    Bean(long id, String name, boolean bool) {
      this.id = id;
      this.name = name;
      this.bool = bool;
    }

    @Override
    public void appendMembers(StringBuilder buf) {
      AppendUtils.appendIdField(buf, id);
      AppendUtils.appendField(buf, "name", name);
      AppendUtils.appendAnonField(buf, bool);
    }

    @Override
    public String toString() {
      return AppendUtils.toString(this);
    }
  }
}
