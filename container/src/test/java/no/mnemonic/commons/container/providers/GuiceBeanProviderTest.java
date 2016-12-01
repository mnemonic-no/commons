package no.mnemonic.commons.container.providers;

import com.google.inject.Binder;
import com.google.inject.Inject;
import com.google.inject.Module;
import com.google.inject.name.Named;
import no.mnemonic.commons.container.providers.GuiceBeanProvider;
import org.junit.Test;

import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import static org.junit.Assert.*;

public class GuiceBeanProviderTest {

  @Test
  public void testGetBeans() {
    GuiceBeanProvider provider = new GuiceBeanProvider(new TestModule(), null);
    Map<String, Object> beans = provider.getBeans();
    System.out.println(beans);
  }

  @Test
  public void testGetUnavailableInstanceNotPresent() {
    GuiceBeanProvider provider = new GuiceBeanProvider(new TestModule(), null);
    assertFalse(provider.getBean(MyOtherInterface.class).isPresent());
  }

  @Test
  public void testGetBeanInstance() {
    GuiceBeanProvider provider = new GuiceBeanProvider(new TestModule(), null);
    Optional<MyInterface> instance = provider.getBean(MyInterface.class);
    assertTrue(instance.isPresent());
    assertNotNull(instance.get());
  }

  @Test
  public void testNamedPropertyNotResolved() {
    GuiceBeanProvider provider = new GuiceBeanProvider(new TestModule(), null);
    Optional<MyClass> instance = provider.getBean(MyClass.class);
    assertNull(instance.get().propertyValue);
  }

  @Test
  public void testNamedPropertyResolvesProperties() {
    Properties props = new Properties();
    props.put("prop", "value");
    GuiceBeanProvider provider = new GuiceBeanProvider(new TestModule(), props);
    Optional<MyClass> instance = provider.getBean(MyClass.class);
    assertEquals("value", instance.get().propertyValue);
  }

  @Test
  public void testGetBeansMap() {
    GuiceBeanProvider provider = new GuiceBeanProvider(new TestModule(), null);
    Map<String, MyInterface> instances = provider.getBeans(MyInterface.class);
    assertEquals(1, instances.size());
    Map.Entry<String, MyInterface> e = instances.entrySet().iterator().next();
    assertTrue(e.getValue() instanceof MyClass);
  }

  public class TestModule implements Module {
    @Override
    public void configure(Binder binder) {
      binder.bind(MyInterface.class).to(MyClass.class);
    }
  }

  public static class MyClass implements MyInterface {
    @Inject(optional = true)
    @Named(value = "prop")
    public String propertyValue;
  }

  public interface MyInterface {
  }

  public interface MyOtherInterface {
  }



}
