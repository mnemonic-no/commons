package no.mnemonic.commons.container;

import no.mnemonic.commons.component.*;
import no.mnemonic.commons.container.plugins.ComponentContainerPlugin;
import no.mnemonic.commons.container.plugins.ComponentDependencyResolver;
import no.mnemonic.commons.container.plugins.ComponentLifecycleHandler;
import no.mnemonic.commons.container.plugins.ComponentValidator;
import no.mnemonic.commons.container.plugins.impl.*;
import no.mnemonic.commons.container.providers.BeanProvider;
import no.mnemonic.commons.container.providers.SimpleBeanProvider;
import no.mnemonic.commons.logging.Logger;
import no.mnemonic.commons.logging.Logging;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.ListUtils;
import no.mnemonic.commons.utilities.collections.MapUtils;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static no.mnemonic.commons.component.ComponentState.*;
import static no.mnemonic.commons.utilities.collections.ListUtils.list;
import static no.mnemonic.commons.utilities.lambda.LambdaUtils.tryTo;

/**
 * ComponentContainer
 */
public class ComponentContainer implements Component, ComponentListener, ComponentListenerAspect, ComponentStatusAspect, ComponentStateAspect {

  //nodes and state
  private final BeanProvider beans;
  private final Set<Object> initializedComponents = Collections.synchronizedSet(new HashSet<>());
  private final Map<String, ComponentNode> nodes = new ConcurrentHashMap<>();
  private final Map<Object, ComponentNode> objectNodeMap = new ConcurrentHashMap<>();
  //parent and child containers
  private final ComponentContainer parent;
  private final AtomicReference<ComponentState> state = new AtomicReference<>(NOT_STARTED);
  private final Collection<ComponentContainer> childContainers = Collections.synchronizedCollection(new ArrayList<>());
  //timestamps and metrics
  private final AtomicLong lastStoppingNotificationTimestamp = new AtomicLong();

  //listeners
  private final Collection<ComponentListener> componentListeners = new HashSet<>();
  private final Collection<ContainerListener> containerListeners = new HashSet<>();
  private final Collection<ComponentDependencyResolver> dependencyResolvers = new HashSet<>();
  private final Collection<ComponentLifecycleHandler> lifecycleManagers = new HashSet<>();
  private final Collection<ComponentValidator> validators = new HashSet<>();

  private final Object STATE_LOCK = new Object();
  private final Logger LOGGER = Logging.getLogger(ComponentContainer.class.getName());

  //creators

  /**
   * @param beans  beans which are administered by this container
   * @param parent component container
   */
  private ComponentContainer(BeanProvider beans, ComponentContainer parent) {
    this.beans = beans;
    this.parent = parent;

    if (parent != null) {
      synchronized (parent.STATE_LOCK) {
        parent.childContainers.add(this);
      }
      this.addComponentListener(parent);
    }
  }

  public static ComponentContainer create(Object... beans) {
    return new ComponentContainer(new SimpleBeanProvider(list(beans)), null);
  }

  public static ComponentContainer create(BeanProvider provider) {
    return new ComponentContainer(provider, null);
  }

  // interface methods

  @Override
  public void addComponentListener(ComponentListener listener) {
    componentListeners.add(listener);
  }

  @Override
  public Collection<ComponentListener> getComponentListeners() {
    return Collections.unmodifiableCollection(componentListeners);
  }

  @Override
  public void removeComponentListener(ComponentListener listener) {
    componentListeners.remove(listener);
  }

  public void addContainerListener(ContainerListener listener) {
    containerListeners.add(listener);
  }

  public void removeContainerListener(ContainerListener listener) {
    containerListeners.remove(listener);
  }

  @Override
  public ComponentStatus getComponentStatus() {
    return new ComponentStatus();
  }

  @Override
  public ComponentState getComponentState() {
    return state.get();
  }

  @Override
  public void notifyComponentStopping(Object component) {
    if (getComponentState().isTerminal()) {
      if (getLogger().isDebug())
        getLogger().debug("Component " + component + " notified us of current shutdown");
    }
  }

  @Override
  public void notifyComponentStopped(Object component) {
    //if notifying component is part of this container, destroy this container now (unless already terminating)
    if (initializedComponents.contains(component) && !getComponentState().isTerminal()) {
      getLogger().warning("Component " + component + " stopped, destroying container " + this);
      try {
        this.destroy();
      } catch (Exception e) {
        getLogger().error(e, "Error when calling destroy");
      }
    }
  }

  //public methods

  /**
   * Initialize this container, and any subcontainers it may have
   *
   * @return The initialized container
   */
  public ComponentContainer initialize() {
    try {
      if (getComponentState() != NOT_STARTED) return this;
      // update state
      setState(INITIALIZING);

      // initialize parent container if not already so
      if (parent != null) {
        // make sure parent is initialized first
        parent.initialize();
      } else {
        // create a thread that will shutdown the container
        new ShutdownTask(this);
        // ensure to at least log every uncaught exception
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> LOGGER.error(e, "Uncaught exception from thread %s: %s", t.getName(), e.getMessage()));
      }

      // handle container plugins
      handleContainerPlugins();

      // create component nodes
      createNodes();
      // resolve dependencies between components
      resolveDependencies();
      // validate configuration
      validate();
      // activate components
      activate();
      // update state
      setState(STARTED);
      // notify listeners
      containerListeners.forEach(l -> tryTo(() -> l.notifyContainerStarted(this), e -> LOGGER.error(e, "Error calling notifyContainerStarted")));
    } catch (RuntimeException e) {
      getLogger().error(e, "Error initializing container");
      //if startup fails, make sure to exit the container
      destroy();
      throw e;
    }
    return this;
  }

  /**
   * Destroy only the current container (along with any child containers which cannot survive without their parent)
   * Parent containers are untouched
   */
  public ComponentContainer destroy() {
    // only allow one thread to attempt shutdown for any container
    synchronized (STATE_LOCK) {
      if (getComponentState().isTerminal()) return this;
      setState(STOPPING);
      STATE_LOCK.notifyAll();
    }

    try {
      getLogger().warning("Shutting down...");
      fireContainerStopping();

      // shut down child containers first
      getChildContainers().forEach(ComponentContainer::destroy);

      // stop all nodes in this container
      CompletableFuture.allOf(
              nodes.values().stream()
                      .filter(ComponentNode::isStarted)
                      .map(this::stopNode)
                      .toArray(CompletableFuture[]::new)
      ).get();
      getLogger().warning("Shutdown complete");

      // stop nodes not registered in initial dependency graph
      initializedComponents.clear();

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      getLogger().error(e, "destroy() interrupted");
    } catch (ExecutionException | RuntimeException e) {
      getLogger().error(e, "Error in destroy()");
    } finally {

      // remove parent reference
      if (parent != null) {
        synchronized (parent.STATE_LOCK) {
          parent.childContainers.remove(this);
        }
      }

      setState(STOPPED);
      nodes.clear();

      //notify componentListeners that we are done
      fireContainerStopped();
    }
    return this;
  }

  // ***************************** private methods

  private Logger getLogger() {
    return LOGGER;
  }

  Map<String, Object> getComponents() {
    synchronized (STATE_LOCK) {
      return MapUtils.map(nodes.entrySet(), e -> MapUtils.Pair.T(e.getKey(), e.getValue().getObject()));
    }
  }

  private Collection<ComponentContainer> getChildContainers() {
    synchronized (STATE_LOCK) {
      return Collections.unmodifiableCollection(list(childContainers));
    }
  }

  private void fireContainerStopping() {
    //avoid excessive notifications
    if (System.currentTimeMillis() - lastStoppingNotificationTimestamp.get() < 1000) return;
    if (getComponentState().isTerminal()) {
      lastStoppingNotificationTimestamp.set(System.currentTimeMillis());
      componentListeners.forEach(l -> tryTo(() -> l.notifyComponentStopping(ComponentContainer.this), e -> LOGGER.error(e, "Error calling notifyComponentStopped")));
      containerListeners.forEach(l -> tryTo(() -> l.notifyContainerDestroying(ComponentContainer.this), e -> LOGGER.error(e, "Error calling notifyContainerDestroying")));
    }
  }

  private void fireContainerStopped() {
    componentListeners.forEach(l -> tryTo(() -> l.notifyComponentStopped(this), e -> LOGGER.error(e, "Error calling notifyComponentStopped")));
    containerListeners.forEach(l -> tryTo(() -> l.notifyContainerDestroyed(this), e -> LOGGER.error(e, "Error calling notifyContainerDestroyed")));
  }

  private void setState(ComponentState state) {
    synchronized (STATE_LOCK) {
      this.state.set(state);
      STATE_LOCK.notifyAll();
    }
  }

  /**
   * Set all special purpose objects to special interfaces
   */
  private void handleContainerPlugins() {
    //make all container aware beans aware of its parent container
    beans.getBeans(ContainerAware.class).forEach((k, v) -> v.registerContainerAware(this));
    //register all plugins
    beans.getBeans(ComponentContainerPlugin.class).forEach((k, v) -> registerPlugin(v));
    //add handler plugin to register all container listeners
    registerPlugin(new ContainerListenerHandler(this));
    //add handler plugin to register all component listeners
    registerPlugin(new ComponentListenerAspectHandler(this));

    lifecycleManagers.addAll(beans.getBeans(ComponentLifecycleHandler.class).values());
    lifecycleManagers.add(new ComponentLifecycleAspectHandler());

    dependencyResolvers.addAll(beans.getBeans(ComponentDependencyResolver.class).values());
    dependencyResolvers.add(new MethodAnnotationDependencyResolver());
    dependencyResolvers.add(new FieldAnnotationDependencyResolver());

    validators.addAll(beans.getBeans(ComponentValidator.class).values());
    validators.add(new ComponentValidationAspectValidator());
  }

  /**
   * Validate all components that has a validator
   */
  private void validate() {
    ValidationContext validationContext = new ValidationContext();
    beans.getBeans().values().forEach(b -> validateBean(b, validationContext));

    for (String error : validationContext.getErrors()) {
      getLogger().error(error);
    }

    for (String warning : validationContext.getWarnings()) {
      getLogger().warning(warning);
    }

    if (!validationContext.isValid()) {
      throw new ComponentConfigurationException(validationContext);
    }
  }

  private void registerPlugin(ComponentContainerPlugin plugin) {
    Map<String, Object> targets = new HashMap<>();
    beans.getBeans().forEach((k, v) -> {
      if (plugin.appliesTo(v)) targets.put(k, v);
    });
    plugin.registerBeans(targets);
  }

  private void validateBean(Object bean, ValidationContext validationContext) {
    for (ComponentValidator v : validators) {
      if (v.appliesTo(bean)) {
        v.validate(validationContext, bean);
        return;
      }
    }
  }

  /**
   * Activate active components
   */
  private void activate() {
    try {
      getLogger().info("Initializing " + this);

      // stop all nodes in this container
      CompletableFuture.allOf(
              nodes.values().stream()
                      .map(this::startNode)
                      .toArray(CompletableFuture[]::new)
      ).get();
      getLogger().info("Initialization complete");

    } catch (Exception e) {
      getLogger().error(e, "Caught exception during initialization");
      destroy();
      throw new ComponentException(e);
    }
  }

  /**
   * Start this node component. Resolves dependencies, so any dependent objects
   * are started first, and any objects listed to be started afterwords is
   * started afterwords.
   *
   * @param n node to start
   */
  private CompletableFuture<?> startNode(ComponentNode n) {
    // first start all components which we have an initialization dependency to
    return n.startup(() -> CompletableFuture.allOf(
            n.getInitializationDependencies().stream()
                    .map(this::startNode)
                    .toArray(CompletableFuture[]::new)
    ).thenRun(() -> doStart(n)));
  }

  private void doStart(ComponentNode n) {
    // see if any lifecycle manager can start this component
    for (ComponentLifecycleHandler manager : lifecycleManagers) {
      if (!manager.appliesTo(n.getObject())) continue;
      getLogger().info("Starting " + n.getObjectName() + "/" + n.getObject());
      manager.startComponent(n.getObject());
      // mark component as initialized
      initializedComponents.add(n.getObject());
    }
  }

  /**
   * Stop the given node component. Resolves dependencies, to stop depending
   * components first, and successive components afterwords.
   *
   * @param n node to stop
   */
  private CompletableFuture<?> stopNode(ComponentNode n) {
    // first stop all components which we have a destruction dependency to
    return n.shutdown(() -> CompletableFuture.allOf(
            n.getDestructionDependencies().stream()
                    .filter(ComponentNode::isStarted)
                    .map(this::stopNode)
                    .toArray(CompletableFuture[]::new)
    ).thenRun(() -> doStop(n)));
  }

  private void doStop(ComponentNode n) {

    // see if any lifecycle manager can stop this component
    for (ComponentLifecycleHandler manager : lifecycleManagers) {
      if (!manager.appliesTo(n.getObject())) continue;
      try {
        getLogger().info("Destroying " + n.getObjectName() + "/" + n.getObject());
        manager.stopComponent(n.getObject());
        if (getLogger().isDebug()) getLogger().debug("Finished stopComponent for component " + n.getObjectName());
      } catch (Exception e) {
        getLogger().error(e, "Error calling stopComponent on " + n.getObject());
      }
      // remove initialization mark
      initializedComponents.remove(n.getObject());
    }
  }

  /**
   * Build dependency tree
   */
  @SuppressWarnings("unchecked")
  private void createNodes() {
    nodes.clear();
    objectNodeMap.clear();

    // make all nodes available
    beans.getBeans().forEach((oid, o) -> {
      //deduplicate objects, to avoid same object being registered in two different dependency nodes
      if (objectNodeMap.containsKey(o)) {
        ComponentNode n = objectNodeMap.get(o);
        nodes.put(oid, n);
        return;
      }
      ComponentNode n = new ComponentNode(oid, o);
      nodes.put(oid, n);
      objectNodeMap.put(o, n);
    });
  }

  private void resolveDependencies() {
    //make all dependency resolvers scan all objects
    dependencyResolvers.forEach(r -> r.scan(ListUtils.list(nodes.values(), ComponentNode::getObject)));
    //then resolve dependencies for each node
    nodes.keySet().forEach(oid -> resolveDependsOn(nodes.get(oid)));
  }

  private void resolveDependsOn(ComponentNode node) {
    //check for Dependency annotations on getters
    getDependencies(node).stream()
            .filter(Objects::nonNull)
            .forEach(dep -> {
              if (dep instanceof Collection) {
                //add dependency to each member of collection
                ((Collection<?>) dep).stream()
                        .map(objectNodeMap::get)
                        .filter(Objects::nonNull)
                        .forEach(depnode -> addDependency(node, depnode));
              } else {
                //add dependency to object
                ObjectUtils.ifNotNullDo(objectNodeMap.get(dep), depnode -> addDependency(node, depnode));
              }
            });
  }

  private void addDependency(ComponentNode node, ComponentNode dependencyNode) {
    node.addInitializationDependency(dependencyNode);
    dependencyNode.addDestructionDependency(node);
  }

  private Collection<?> getDependencies(ComponentNode node) {
    Collection<Object> dependencies = new HashSet<>();
    dependencyResolvers.forEach(r -> dependencies.addAll(
            ObjectUtils.ifNull(r.resolveDependencies(node.getObject()), new HashSet<>())
    ));
    return dependencies;
  }

  private class ShutdownTask implements Runnable {

    private ComponentContainer rootContainer;

    ShutdownTask(ComponentContainer rootContainer) {
      this.rootContainer = rootContainer;
      Runtime.getRuntime().addShutdownHook(new Thread(this));
      if (getLogger().isInfo()) rootContainer.getLogger().info("Shutdownhook added");
    }

    public void run() {
      try {
        rootContainer.getLogger().warning("Shutdownhook triggered");
        // drop out of this shutdownhook if container is already shut down
        synchronized (STATE_LOCK) {
          if (getComponentState().isTerminal()) {
            rootContainer.getLogger().warning("Shutdownhook aborted, container already shut down");
            return;
          }
        }
        rootContainer.destroy();
      } finally {
        rootContainer.getLogger().warning("Shutdownhook done");
      }
    }
  }

}
