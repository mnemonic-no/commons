package no.mnemonic.commons.component;

/**
 * This interface allows singleton providers to declare their provided objects, so that
 * the dependency resolvers are able to map the providers as their real dependencies.
 *
 * This helps solve the problem of ProviderA with LifeCycleAspect creating ClassA, which is a dependency for ClassB.
 * If ClassB only declares ClassA as a dependency, the resolver will not be able to detect ProviderA as a real dependency.
 * But if ProviderA is declared a DependencyProvider which returns ClassA, then the dependency resolvers can use this
 * information to backtrack ProviderA as a real dependency of ClassB.
 *
 */
public interface DependencyProvider {

  /**
   * Method must be called AFTER constructing the beans, before lifecycle methods are performed
   *
   * @return the provided dependency which will be reported as the real dependency from depending objects.
   * May return null if the object is not provided yet.
   */
  Object getProvidedDependency();

}