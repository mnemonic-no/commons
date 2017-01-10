package no.mnemonic.commons.container.plugins.impl;

import no.mnemonic.commons.component.Dependency;
import no.mnemonic.commons.container.plugins.impl.FieldAnnotationDependencyResolver;
import no.mnemonic.commons.utilities.collections.SetUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FieldAnnotationDependencyResolverTest {

  @Test
  public void testResolveDependencies() {
    assertEquals(SetUtils.set("dep1", "dep2"), new FieldAnnotationDependencyResolver().resolveDependencies(new MyComponent()));
  }

  @Test
  public void testResolveDependenciesOnNull() {
    assertEquals(SetUtils.set(), new FieldAnnotationDependencyResolver().resolveDependencies(null));
  }

  private static class MyComponent{
    @Dependency
    private String dep1 = "dep1";
    @Dependency
    private String dep2 = "dep2";
    private String dep3 = "dep3";
  }
}
