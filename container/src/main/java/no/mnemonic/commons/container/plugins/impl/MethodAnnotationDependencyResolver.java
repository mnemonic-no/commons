package no.mnemonic.commons.container.plugins.impl;

import no.mnemonic.commons.component.Dependency;
import no.mnemonic.commons.container.plugins.ComponentDependencyResolver;
import no.mnemonic.commons.logging.Logger;
import no.mnemonic.commons.logging.Logging;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static no.mnemonic.commons.utilities.collections.SetUtils.set;

public class MethodAnnotationDependencyResolver implements ComponentDependencyResolver {

  private static final Logger LOGGER = Logging.getLogger(MethodAnnotationDependencyResolver.class);

  @Override
  public Collection<Object> resolveDependencies(Object object) {
    Collection<Object> dependencies = new HashSet<>();
    if (object == null) return dependencies;
    for (Method m : findDependencyGetters(object.getClass())) {
      try {
        m.setAccessible(true);
        Object dep = m.invoke(object);
        if (dep == null) continue;
        dependencies.add(dep);
      } catch (IllegalAccessException | InvocationTargetException e) {
        LOGGER.warning(String.format("Error checking for dependency: %s", object));
      }
    }
    return dependencies;
  }

  private Set<Method> findDependencyGetters(Class objectClass) {
    return set(objectClass.getMethods()).stream()
        .filter(m -> m.isAnnotationPresent(Dependency.class))
        .filter(m -> m.getParameterTypes().length == 0)
        .collect(Collectors.toSet());
  }

}
