package no.mnemonic.commons.container.providers;

import no.mnemonic.commons.container.providers.SimpleBeanProvider;
import org.junit.Test;

import java.util.Map;

import static no.mnemonic.commons.utilities.collections.SetUtils.set;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class SimpleBeanProviderTest {

  @Test(expected = IllegalArgumentException.class)
  public void testProviderNullBeans() {
    SimpleBeanProvider provider = new SimpleBeanProvider(null);
  }

  @Test
  public void testProviderGetBean() {
    SimpleBeanProvider provider = new SimpleBeanProvider(set(1L, 2));
    //noinspection OptionalGetWithoutIsPresent
    assertEquals(Long.valueOf(1), provider.getBean(Long.class).get());
  }

  @Test
  public void testProviderGetBeanNotResolving() {
    SimpleBeanProvider provider = new SimpleBeanProvider(set(1L, 2));
    //noinspection OptionalGetWithoutIsPresent
    assertFalse(provider.getBean(String.class).isPresent());
  }

  @Test
  public void testProviderGetBeans() {
    SimpleBeanProvider provider = new SimpleBeanProvider(set(1L, 2L));
    //noinspection OptionalGetWithoutIsPresent
    Map<String, Long> results = provider.getBeans(Long.class);
    assertEquals(set(1L, 2L), set(results.values()));
  }

}
