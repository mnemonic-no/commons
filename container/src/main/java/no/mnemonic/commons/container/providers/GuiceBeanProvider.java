package no.mnemonic.commons.container.providers;

import com.google.inject.*;
import com.google.inject.name.Names;
import com.google.inject.spi.DefaultBindingScopingVisitor;
import no.mnemonic.commons.utilities.collections.SetUtils;

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
    injector.getAllBindings().values().stream()
        .forEach(b-> b.acceptScopingVisitor(new DefaultBindingScopingVisitor(){
          @Override
          public Object visitScope(Scope scope) {
            if (scope == Scopes.SINGLETON) {
              Object obj = b.getProvider().get();
              if (searchType.isInstance(obj)) {
                //noinspection unchecked
                result.put(createKey(b.getKey()), (T) obj);
              }
            }
            return null;
          }
        }));
    return result;
  }

  @Override
  public Map<String, Object> getBeans() {
    return getBeans(Object.class);
  }

  private String createKey(Key nameKey) {
    String result = nameKey.getTypeLiteral().getRawType().getSimpleName();
    if (nameKey.getAnnotationType() != null) result = result + "-" + nameKey.getAnnotationType().getSimpleName();
    return result;
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
