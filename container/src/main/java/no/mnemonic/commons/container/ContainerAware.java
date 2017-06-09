package no.mnemonic.commons.container;

/**
 * Interface for plugins which need a reference to its container.
 * Any component defined in a container implementing this interface will be registered before the container initializes.
 */
public interface ContainerAware {

  void registerContainerAware(ComponentContainer parent);

}
