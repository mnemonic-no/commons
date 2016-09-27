package test.no.mnemonic.commons.dummy;

import no.mnemonic.commons.dummy.Dummy;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DummyTest {

  @Test
  public void testDoSomething() {
    assertEquals("Hello World!", Dummy.doSomething());
  }

}
