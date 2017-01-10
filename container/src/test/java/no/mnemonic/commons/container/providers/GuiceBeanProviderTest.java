package no.mnemonic.commons.container.providers;

import com.google.inject.*;
import com.google.inject.name.Named;
import org.junit.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import static org.junit.Assert.*;

public class GuiceBeanProviderTest {

  @Test
  public void testGetBeans() {
    GuiceBeanProvider provider = new GuiceBeanProvider(new ExplicitMappingModule());
    Map<String, Object> beans = provider.getBeans();
    System.out.println(beans);
  }

  @Test
  public void testGetUnavailableInstanceNotPresent() {
    GuiceBeanProvider provider = new GuiceBeanProvider(new ExplicitMappingModule());
    assertFalse(provider.getBean(BeanProvider.class).isPresent());
  }

  @Test
  public void testGetInterfaceInstanceWithExplicitMappingModule() {
    GuiceBeanProvider provider = new GuiceBeanProvider(new ExplicitMappingModule());
    Optional<MyOtherInterface> instance = provider.getBean(MyOtherInterface.class);
    assertTrue(instance.isPresent());
    assertNotNull(instance.get());
  }

  @Test
  public void testGetInterfaceInstanceWithConcreteMappingModule() {
    GuiceBeanProvider provider = new GuiceBeanProvider(new ConcreteMappingModule());
    Optional<MyOtherInterface> instance = provider.getBean(MyOtherInterface.class);
    assertTrue(instance.isPresent());
    assertNotNull(instance.get());
  }

  @Test
  public void testGetConcreteInstanceWithConcreteMappingModule() {
    GuiceBeanProvider provider = new GuiceBeanProvider(new ConcreteMappingModule());
    Optional<MyClass> instance = provider.getBean(MyClass.class);
    assertTrue(instance.isPresent());
    assertNotNull(instance.get());
  }

  @Test
  public void testGetConcreteInstanceWithExplicitMappingModule() {
    GuiceBeanProvider provider = new GuiceBeanProvider(new ExplicitMappingModule());
    Optional<MyClass> instance = provider.getBean(MyClass.class);
    assertTrue(instance.isPresent());
    assertNotNull(instance.get());
  }

  @Test
  public void testNamedPropertyNotResolved() {
    GuiceBeanProvider provider = new GuiceBeanProvider(new ExplicitMappingModule());
    Optional<MyClass> instance = provider.getBean(MyClass.class);
    assertNull(instance.get().propertyValue);
  }

  @Test
  public void testNamedPropertyResolvesProperties() {
    Properties props = new Properties();
    props.put("prop", "value");
    GuiceBeanProvider provider = new GuiceBeanProvider(props, new ExplicitMappingModule());
    Optional<MyClass> instance = provider.getBean(MyClass.class);
    assertEquals("value", instance.get().propertyValue);
  }

  @Test
  public void testGetBeansMapWithExplicitMapping() {
    GuiceBeanProvider provider = new GuiceBeanProvider(new ExplicitMappingModule());
    assertEquals(2, provider.getBeans(MyInterface.class).size());
    assertEquals(1, provider.getBeans(MyOtherInterface.class).size());
  }

  @Test
  public void testGetBeansMapWithAnnotatedMapping() {
    GuiceBeanProvider provider = new GuiceBeanProvider(new AnnotatedMappingModule());
    assertEquals(3, provider.getBeans(MyInterface.class).size());
  }

  @Test
  public void testGetBeansMapWithConcreteMapping() {
    GuiceBeanProvider provider = new GuiceBeanProvider(new ConcreteMappingModule());
    assertEquals(2, provider.getBeans(MyInterface.class).size());
    assertEquals(1, provider.getBeans(MyOtherInterface.class).size());
  }

  @Test
  public void testGetBeansMapWithProviderMapping() {
    GuiceBeanProvider provider = new GuiceBeanProvider(new ProviderMappingModule());
    provider.getBeans().forEach((k,v)-> System.out.println(k+": " + v));
    assertEquals(3, provider.getBeans(MyInterface.class).size());
    assertEquals(2, provider.getBeans(MyOtherInterface.class).size());
    assertEquals(2, provider.getBeans(MyOtherClass.class).size());
  }

  @Test
  public void testKeysWithExplicitMappings() {
    GuiceBeanProvider provider = new GuiceBeanProvider(new ExplicitMappingModule());
    Map<String, MyInterface> m = provider.getBeans(MyInterface.class);
    assertEquals(2, m.size());
    assertTrue(m.get("MyInterface") instanceof MyClass);
    assertTrue(m.get("MyOtherInterface") instanceof MyOtherClass);
  }

  @Test
  public void testKeysWithConcreteMapping() {
    GuiceBeanProvider provider = new GuiceBeanProvider(new ConcreteMappingModule());
    Map<String, MyInterface> m = provider.getBeans(MyInterface.class);
    assertEquals(2, m.size());
    assertTrue(m.get("MyClass") instanceof MyClass);
    assertTrue(m.get("MyOtherClass") instanceof MyOtherClass);
  }

  @Test
  public void testKeysWithAnnotatedBindings() {
    GuiceBeanProvider provider = new GuiceBeanProvider(new AnnotatedMappingModule());
    Map<String, MyInterface> m = provider.getBeans(MyInterface.class);
    assertEquals(3, m.size());
    assertTrue(m.get("MyInterface-FirstAnnotation") instanceof MyClass);
    assertTrue(m.get("MyInterface-SecondAnnotation") instanceof MyOtherClass);
    assertTrue(m.get("MyOtherInterface-SecondAnnotation") instanceof MyOtherClass);
  }

  @Test
  public void testKeysWithProviderBinding() {
    GuiceBeanProvider provider = new GuiceBeanProvider(new ProviderMappingModule());
    Map<String, MyInterface> m = provider.getBeans(MyInterface.class);
    assertEquals(3, m.size());
    assertTrue(m.get("MyInterface-FirstAnnotation") instanceof MyClass);
    assertTrue(m.get("MyInterface-SecondAnnotation") instanceof MyOtherClass);
    assertTrue(m.get("MyOtherInterface-SecondAnnotation") instanceof MyOtherClass);
  }

  public class ExplicitMappingModule extends AbstractModule {
    @Override
    public void configure() {
      bind(MyInterface.class).to(MyClass.class).in(Singleton.class);
      bind(MyOtherInterface.class).to(MyOtherClass.class).in(Singleton.class);
    }
  }

  public class AnnotatedMappingModule extends AbstractModule {
    @Override
    public void configure() {
      bind(MyInterface.class).annotatedWith(FirstAnnotation.class).to(MyClass.class).in(Singleton.class);
      bind(MyInterface.class).annotatedWith(SecondAnnotation.class).to(MyOtherClass.class).in(Singleton.class);
      bind(MyOtherInterface.class).annotatedWith(SecondAnnotation.class).to(MyOtherClass.class).in(Singleton.class);
    }
  }

  public class ProviderMappingModule extends AbstractModule {
    @Override
    public void configure() {
      bind(MyInterface.class).annotatedWith(FirstAnnotation.class).toProvider(MyProvider.class).in(Singleton.class);
      bind(MyInterface.class).annotatedWith(SecondAnnotation.class).toProvider(MyExtraProvider.class).in(Singleton.class);
      bind(MyOtherInterface.class).annotatedWith(SecondAnnotation.class).toProvider(MyOtherProvider.class).in(Singleton.class);
    }
  }

  public class ConcreteMappingModule extends AbstractModule {
    @Override
    public void configure() {
      bind(MyClass.class).in(Singleton.class);
      bind(MyOtherClass.class).in(Singleton.class);
    }
  }

  public static class MyClass implements MyInterface {
    @Inject(optional = true)
    @Named(value = "prop")
    public String propertyValue;

    @Override
    public String toString() {
      return getClass().getSimpleName();
    }
  }

  static class MyProvider implements Provider<MyInterface> {
    @Override
    public MyInterface get() {
      return new MyClass();
    }
  }

  static class MyOtherProvider implements Provider<MyOtherInterface> {
    @Override
    public MyOtherInterface get() {
      return new MyOtherClass();
    }
  }

  static class MyExtraProvider implements Provider<MyInterface> {
    @Override
    public MyInterface get() {
      return new MyOtherClass();
    }
  }

  public static class MyOtherClass implements MyOtherInterface {
    @Inject(optional = true)
    @Named(value = "prop")
    public String propertyValue;

    @Override
    public String toString() {
      return getClass().getSimpleName();
    }
  }

  @Target(ElementType.FIELD)
  @Retention(RetentionPolicy.RUNTIME)
  @BindingAnnotation
  public @interface FirstAnnotation{}

  @Target(ElementType.FIELD)
  @Retention(RetentionPolicy.RUNTIME)
  @BindingAnnotation
  public @interface SecondAnnotation{}

  public interface MyInterface {
  }

  public interface MyOtherInterface extends MyInterface {
  }



}
