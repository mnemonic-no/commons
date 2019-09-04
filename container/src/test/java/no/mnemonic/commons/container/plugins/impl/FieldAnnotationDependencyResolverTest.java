package no.mnemonic.commons.container.plugins.impl;

import com.google.inject.Inject;
import com.google.inject.Provider;
import no.mnemonic.commons.component.Dependency;
import no.mnemonic.commons.component.DependencyProvider;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicReference;

import static no.mnemonic.commons.utilities.collections.SetUtils.set;
import static org.junit.Assert.assertEquals;

public class FieldAnnotationDependencyResolverTest {

  @Test
  public void testResolveDependencies() {
    assertEquals(set("dep1", "dep2"), new FieldAnnotationDependencyResolver().resolveDependencies(new MyComponent()));
  }

  @Test
  public void testResolveDependenciesOnNull() {
    assertEquals(set(), new FieldAnnotationDependencyResolver().resolveDependencies(null));
  }

  @Test
  public void testGetDependencyProvider() {
    MyDependencyProvider provider = new MyDependencyProvider();
    MyDependency dependency = provider.get();
    MyDependingClass dependingClass= new MyDependingClass(dependency);

    FieldAnnotationDependencyResolver resolver = new FieldAnnotationDependencyResolver();
    resolver.scan(set(provider, dependency, dependingClass));
    assertEquals(set(provider, dependency), resolver.resolveDependencies(dependingClass));
  }

  @Test
  public void testGetDependencyProviderReturnsNull() {
    MyDependencyProvider provider = new MyDependencyProvider();
    MyDependency dependency = new MyDependency();
    MyDependingClass dependingClass= new MyDependingClass(dependency);

    FieldAnnotationDependencyResolver resolver = new FieldAnnotationDependencyResolver();
    resolver.scan(set(provider, dependency, dependingClass));
    assertEquals(set(dependency), resolver.resolveDependencies(dependingClass));
  }


  private static class MyComponent{
    @Dependency
    private String dep1 = "dep1";
    @Dependency
    private String dep2 = "dep2";
    private String dep3 = "dep3";
  }

  public static class MyDependingClass {

    @Dependency
    private final MyDependency myDependency;

    @Inject
    public MyDependingClass(MyDependency myDependency) {
      this.myDependency = myDependency;
    }
  }

  static class MyDependencyProvider implements Provider<MyDependency>, DependencyProvider {

    private final AtomicReference<MyDependency> obj = new AtomicReference<>();

    @Override
    public Object getProvidedDependency() {
      return obj.get();
    }

    @Override
    public MyDependency get() {
      obj.set(new MyDependency());
      return obj.get();
    }

  }

  public static class MyDependency {
  }
}
