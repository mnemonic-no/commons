package no.mnemonic.commons.container.plugins.impl;

import no.mnemonic.commons.component.Dependency;
import no.mnemonic.commons.container.plugins.ComponentDependencyResolver;
import no.mnemonic.commons.logging.Logger;
import no.mnemonic.commons.logging.Logging;
import no.mnemonic.commons.utilities.collections.SetUtils;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static no.mnemonic.commons.utilities.collections.SetUtils.set;

public class FieldAnnotationDependencyResolver implements ComponentDependencyResolver {

  private static final Logger LOGGER = Logging.getLogger(FieldAnnotationDependencyResolver.class);

  @Override
  public Collection<Object> resolveDependencies(Object object) {
    Collection<Object> dependencies = new HashSet<>();
    if (object == null) return dependencies;
    for (Field f : findDependencyFields(object.getClass())) {
      try {
        f.setAccessible(true);
        Object dep = f.get(object);
        if (dep == null) continue;
        dependencies.add(dep);
      } catch (IllegalAccessException e) {
        LOGGER.warning(String.format("Error checking for dependency: %s", object));
      }
    }
    return dependencies;
  }

  private Set<Field> findDependencyFields(Class objectClass) {
    if (objectClass == null) return set();
    return SetUtils.union(
        set(objectClass.getDeclaredFields()).stream()
            .filter(f -> f.isAnnotationPresent(Dependency.class))
            .collect(Collectors.toSet()),
        findDependencyFields(objectClass.getSuperclass())
    );
  }

}
