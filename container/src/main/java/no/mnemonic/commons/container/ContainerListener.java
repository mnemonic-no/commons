package no.mnemonic.commons.container;

/**
 * Interface for plugins which need to be notified when container is stopping/starting.
 * Any component defined in a container implementing this interface will be picked up before container initializes.
 */
public interface ContainerListener {

  /**
   * Notify listeners that this container has been started.
   * It is invoked after all components in this container are started.
   *
   * @param container which has started
   */
  void notifyContainerStarted(ComponentContainer container);

  /**
   * Notify listeners that this container will be destroyed. It is invoked before any
   * components in this container are shut down.
   * If this operation blocks, it will block the progress of shutting down the components
   *
   * @param container which will be destroyed
   */
  void notifyContainerDestroying(ComponentContainer container);

  /**
   * Notify listeners that this container has been destroyed.
   * It is invoked after all components in this container are shut down.
   *
   * @param container which has been destroyed
   */
  void notifyContainerDestroyed(ComponentContainer container);

  /**
   * Notify listeners that a new container is about to initialize
   *
   * @param parent       reference to the calling container
   * @param subcontainer reference to the initializing subcontainer
   */
  void notifyInitializingSubcontainer(ComponentContainer parent, ComponentContainer subcontainer);

}
