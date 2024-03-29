package no.mnemonic.commons.container.providers;

import com.google.inject.*;
import com.google.inject.Module;
import com.google.inject.name.Names;
import com.google.inject.spi.DefaultBindingScopingVisitor;
import no.mnemonic.commons.utilities.collections.SetUtils;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

public class GuiceBeanProvider implements BeanProvider{

  private final Injector injector;

  public GuiceBeanProvider(Module... modules) {
    this(null, modules);
  }

  public GuiceBeanProvider(Properties properties, Module... modules) {
    Set<Module> moduleSet = SetUtils.set(modules).stream().filter(m->m!=null).collect(Collectors.toSet());
    if (properties != null) {
      moduleSet.add(createPropertiesModule(properties));
    }
    injector = Guice.createInjector(moduleSet);
  }

  @Override
  public <T> Optional<T> getBean(Class<T> ofType) {
    Map<String, T> beans = getBeans(ofType);
    if (beans.isEmpty()) return Optional.empty();
    if (beans.size() > 1) throw new IllegalStateException("Multiple implementations found for this class: " + ofType);
    return Optional.of(beans.values().iterator().next());
  }

  @Override
  public <T> Map<String, T> getBeans(Class<T> searchType) {
    Map<String, T> result = new HashMap<>();
    injector.getAllBindings().values()
            .forEach(b -> b.acceptScopingVisitor(new DefaultBindingScopingVisitor<Void>() {
              @Override
              public Void visitScope(Scope scope) {
                if (scope == Scopes.SINGLETON) {
                  Object obj = b.getProvider().get();
                  if (searchType.isInstance(obj)) {
                    String key = createKey(b.getKey());
                    if (result.containsKey(key)) {
                      // This should usually not happen because it means that the Guice configuration is messed up.
                      // Throw an exception instead of silently omitting beans.
                      throw new IllegalStateException("Already resolved bean with key: " + key);
                    }

                    //noinspection unchecked
                    result.put(key, (T) obj);
                  }
            }
            return null;
          }
        }));
    return result;
  }

  public Injector getInjector() {
    return injector;
  }

  @Override
  public Map<String, Object> getBeans() {
    return getBeans(Object.class);
  }

  private String createKey(Key<?> nameKey) {
    String result = createKeyFromType(nameKey.getTypeLiteral().getType());

    // Handle annotation qualifier (optional).
    if (nameKey.getAnnotationType() != null) {
      result = result + "-" + nameKey.getAnnotationType().getSimpleName();
    }

    return result;
  }

  private String createKeyFromType(Type type) {
    // Case 1: Concrete class
    if (type instanceof Class) {
      return ((Class<?>) type).getSimpleName();
    }

    // Case 2: Generic type
    if (type instanceof ParameterizedType) {
      ParameterizedType parameterizedType = (ParameterizedType) type;
      // Handle raw type of the generic, such as Map, List, Set, etc.
      String prefix = parameterizedType.getRawType() instanceof Class ?
              ((Class<?>) parameterizedType.getRawType()).getSimpleName() : "UNKNOWN";
      // Handle type arguments of the generic.
      return Arrays.stream(parameterizedType.getActualTypeArguments())
              .map(this::createKeyFromType)
              .collect(Collectors.joining("-", prefix + "-", ""));
    }

    // Fallback for unhandled cases.
    return "UNKNOWN";
  }

  private Module createPropertiesModule(Properties properties) {
    return new AbstractModule() {
      @Override
      protected void configure() {
        Names.bindProperties(binder(), properties);
      }
    };
  }
}
