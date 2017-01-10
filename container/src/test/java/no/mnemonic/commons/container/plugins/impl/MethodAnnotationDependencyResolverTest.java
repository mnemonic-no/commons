package no.mnemonic.commons.container.plugins.impl;

import no.mnemonic.commons.component.Dependency;
import no.mnemonic.commons.container.plugins.impl.MethodAnnotationDependencyResolver;
import no.mnemonic.commons.utilities.collections.SetUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MethodAnnotationDependencyResolverTest {

  @Test
  public void testResolveDependencies() {
    assertEquals(SetUtils.set("dep1", "dep2"), new MethodAnnotationDependencyResolver().resolveDependencies(new MyComponent()));
  }

  @Test
  public void testResolveDependenciesOnNull() {
    assertEquals(SetUtils.set(), new MethodAnnotationDependencyResolver().resolveDependencies(null));
  }

  private static class MyComponent{
    private String dep1 = "dep1";
    private String dep2 = "dep2";
    private String dep3 = "dep3";

    @Dependency
    public String getDep1() {
      return dep1;
    }

    @Dependency
    public String getDep2() {
      return dep2;
    }

    public String getDep3() {
      return dep3;
    }
  }
}
