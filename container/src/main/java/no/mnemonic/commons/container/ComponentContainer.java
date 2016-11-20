package no.mnemonic.commons.container;

import no.mnemonic.commons.component.*;
import no.mnemonic.commons.container.plugins.ComponentContainerPlugin;
import no.mnemonic.commons.container.providers.BeanProvider;
import no.mnemonic.commons.container.providers.SimpleBeanProvider;
import no.mnemonic.commons.logging.Logger;
import no.mnemonic.commons.logging.Logging;
import no.mnemonic.commons.utilities.collections.SetUtils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static no.mnemonic.commons.component.ComponentState.*;
import static no.mnemonic.commons.utilities.collections.ListUtils.list;
import static no.mnemonic.commons.utilities.collections.SetUtils.set;

/**
 * ComponentContainer
 */
public class ComponentContainer implements Component, ComponentListener, ComponentListenerAspect, ComponentStatusAspect, ComponentStateAspect {

  //nodes and state
  private final BeanProvider beans;
  private final Set<LifecycleAspect> initializedComponents = Collections.synchronizedSet(new HashSet<>());
  private final Map<String, ComponentNode> nodes = new ConcurrentHashMap<>();
  private final Map<Object, ComponentNode> objectNodeMap = new ConcurrentHashMap<>();
  //parent and child containers
  private final ComponentContainer parent;
  private final AtomicReference<ComponentState> state = new AtomicReference<>(NOT_STARTED);
  private final Collection<ComponentContainer> childContainers = Collections.synchronizedCollection(new ArrayList<>());
  //timestamps and metrics
  private final AtomicLong lastStoppingNotificationTimestamp = new AtomicLong();

  //listeners
  private Collection<ComponentListener> componentListeners = new HashSet<>();
  private Collection<ContainerListener> containerListeners = new HashSet<>();

  private final Object STATE_LOCK = new Object();
  private final Logger LOGGER = Logging.getLogger(ComponentContainer.class.getName());

  //creators

  /**
   * @param beans             beans which are administered by this container
   * @param parent            component container
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
  public void notifyComponentStopping(Component component) {
    if (getComponentState().isTerminal()) {
      if (getLogger().isDebug())
        getLogger().debug("Component " + component + " notified us of current shutdown");
    }
    if (objectNodeMap.containsKey(component)) fireContainerStopping();
  }

  @Override
  public void notifyComponentStopped(Component component) {
    //if notifying component is part of this container, destroy this container now (unless already terminating)
    if (initializedComponents.contains(component) && !getComponentState().isTerminal()) {
      getLogger().warning("Component " + component + " stopped, destroying container " + this);
      try {
        this.destroy();
      } catch (Exception e) {
        getLogger().error("Error when calling destroy", e);
      }
    }
  }

  //public methods

  /**
   * Initialize this container, and any subcontainers it may have
   */
  public void initialize() {
    try {
      if (getComponentState() != NOT_STARTED) return;
      // update state
      setState(INITIALIZING);

      // initialize parent container if not already so
      if (parent != null) {
        // make sure parent is initialized first
        parent.initialize();
      } else {
        // create a thread that will shutdown the container
        new ShutdownTask(this);
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
      for (ContainerListener l : containerListeners) {
        l.notifyContainerStarted(this);
      }
    } catch (RuntimeException e) {
      getLogger().error("Error initializing container", e);
      //if startup fails, make sure to exit the container
      destroy();
      throw e;
    }
  }

  /**
   * Destroy only the current container (along with any child containers which cannot survive without their parent)
   * Parent containers are untouched
   */
  public void destroy() {
    try {
      // only allow one thread to attempt shutdown for any container
      synchronized (STATE_LOCK) {
        if (getComponentState().isTerminal()) return;
        setState(STOPPING);
        STATE_LOCK.notifyAll();
      }

      getLogger().warning("Shutting down...");
      fireContainerStopping();

      // shut down child containers first
      getChildContainers().forEach(ComponentContainer::destroy);

      // stop all nodes in this container
      nodes.values().forEach(this::stopNode);

      // stop nodes not registered in initial dependency graph
      set(initializedComponents).forEach(LifecycleAspect::stopComponent);
      initializedComponents.clear();

    } catch (RuntimeException e) {
      getLogger().error("Error in destroy()", e);
    } finally {

      // remove parent reference
      if (parent != null) {
        synchronized (parent.STATE_LOCK) {
          parent.childContainers.remove(this);
        }
      }

      getLogger().warning("Shutdown complete");
      setState(STOPPED);
      nodes.clear();

      //notify componentListeners that we are done
      componentListeners.forEach(l -> l.notifyComponentStopped(this));
      containerListeners.forEach(l -> l.notifyContainerDestroyed(this));
    }

  }

  // ***************************** private methods

  private Logger getLogger() {
    return LOGGER;
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
      new Thread(() -> {
        componentListeners.forEach(l -> l.notifyComponentStopped(ComponentContainer.this));
        containerListeners.forEach(l -> l.notifyContainerDestroying(ComponentContainer.this));
      }).start();
    }
  }

  private void setState(ComponentState state) {
    synchronized (STATE_LOCK) {
      this.state.set(state);
      STATE_LOCK.notifyAll();
    }
  }

  // private methods

  /**
   * Set all special purpose objects to special interfaces
   */
  private void handleContainerPlugins() {
    beans.getBeans(ComponentContainerPlugin.class).forEach((k, v)->{
      //noinspection unchecked
      v.handleBeans(beans.getBeans(v.getBeanInterface()));
    });
  }

  /**
   * Validate all components that implement ValidationAspect.
   */
  private void validate() {
    ValidationContext validationContext = new ValidationContext();
    beans.getBeans(ValidationAspect.class).values().forEach(b -> b.validate(validationContext));

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

  /**
   * Activate active components
   */
  private void activate() {
    try {
      getLogger().info("Initializing " + this);
      nodes.values().forEach(this::startNode);
      if (getLogger().isInfo()) getLogger().info("Initialization complete");
    } catch (Exception e) {
      getLogger().error("Caught exception during initialization", e);
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
  private void startNode(ComponentNode n) {
    if (n.isStarted()) return;
    n.setStarted(true);

    // first start all components which we have an initialization dependency to
    n.getInitializationDependencies().forEach(this::startNode);

    // then start this component
    if (n.getObject() instanceof LifecycleAspect) {
      getLogger().info("Starting " + n.getObjectName() + "/" + n.getObject());
      ((LifecycleAspect) n.getObject()).startComponent();
      // mark component as initialized
      initializedComponents.add((LifecycleAspect) n.getObject());
    }
  }

  /**
   * Stop the given node component. Resolves dependencies, to stop depending
   * components first, and successive components afterwords.
   *
   * @param n node to stop
   */
  private void stopNode(ComponentNode n) {
    if (!n.isStarted()) return;
    n.setStarted(false);

    // first stop all components which we have a destruction dependency to
    n.getDestructionDependencies().forEach(this::stopNode);

    // then stop/destroyAll this component
    if (n.getObject() instanceof LifecycleAspect) {
      if (getLogger().isInfo()) getLogger().info("Destroying " + n.getObjectName() + "/" + n.getObject());
      System.err.println("* Destroying " + n.getObjectName() + "/" + n.getObject());
      try {
        ((LifecycleAspect) n.getObject()).stopComponent();
      } catch (Exception e) {
        getLogger().error("Error calling stopComponent on " + n.getObject(), e);
      }
      if (getLogger().isDebug())
        getLogger().debug("Finished stopComponent for component " + n.getObjectName());

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
      ComponentNode n = new ComponentNode(oid, o);
      nodes.put(oid, n);
      objectNodeMap.put(o, n);
    });
  }

  private Collection<Object> getDependencies(ComponentNode node) {
    Collection<Object> dependencies = new ArrayList<>();
    if (node.getObject() instanceof DependencyAspect) {
      DependencyAspect da = (DependencyAspect) node.getObject();
      dependencies.addAll(da.getDependencies());
    } else {

      Object object = node.getObject();
      Class objectClass = object.getClass();

      for (Method m : findDependencyGetters(objectClass)) {
        try {
          m.setAccessible(true);
            Object o = m.invoke(node.getObject());
            if (o == null) continue;
            dependencies.add(o);
        } catch (IllegalAccessException | InvocationTargetException e) {
          getLogger().warning(String.format("Error checking for dependency: Node %s property %s", node.getObjectName(), m.getName()));
        }
      }

      for (Field f : findDependencyFields(objectClass)) {
        try {
          f.setAccessible(true);
          Object o = f.get(object);
          if (o == null) continue;
          dependencies.add(o);
        } catch (IllegalAccessException e) {
          getLogger().warning(String.format("Error checking for dependency: Node %s property %s", node.getObjectName(), f.getName()));
        }
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

  private Set<Field> findDependencyFields(Class objectClass) {
    if (objectClass == null) return set();
    return SetUtils.union(
        set(objectClass.getDeclaredFields()).stream()
            .filter(f -> f.isAnnotationPresent(Dependency.class))
            .collect(Collectors.toSet()),
        findDependencyFields(objectClass.getSuperclass())
    );
  }

  private void resolveDependsOn(ComponentNode node) {
    //check for Dependency annotations on getters
    for (Object dependency : getDependencies(node)) {
      //noinspection StatementWithEmptyBody
      if (dependency == null) {
        //nothing to do, object is not set
      } else if (dependency instanceof Collection) {
        //add dependency to each member of collection
        for (Object oo : (Collection) dependency) {
          ComponentNode dependencyNode = objectNodeMap.get(oo);
          if (dependencyNode == null) continue;
          node.addInitializationDependency(dependencyNode);
          dependencyNode.addDestructionDependency(node);
        }
      } else {
        //add dependency to object
        ComponentNode dependencyNode = objectNodeMap.get(dependency);
        if (dependencyNode == null) continue;
        node.addInitializationDependency(dependencyNode);
        dependencyNode.addDestructionDependency(node);
      }
    }
  }

  private void resolveDependencies() {
    // now; create dependencies
    nodes.keySet().forEach(oid -> resolveDependsOn(nodes.get(oid)));
  }

  /**
   * Special thread which is started upon container shutdown call. When run, it
   * shuts down all containers
   */
  private class ShutdownTask implements Runnable {

    private ComponentContainer rootContainer;

    ShutdownTask(ComponentContainer rootContainer) {
      this.rootContainer = rootContainer;
      Runtime.getRuntime().addShutdownHook(new Thread(this));
      if (getLogger().isInfo()) rootContainer.getLogger().info("Shutdownhook added");
    }

    public void run() {
      try {
        // usedShutdownThread = true;
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
