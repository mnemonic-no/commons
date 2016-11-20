package no.mnemonic.commons.container;

import no.mnemonic.commons.component.ComponentConfigurationException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class ComponentNode {

  private final String objectName;
  private final Object object;
  private boolean started;

  private Set<ComponentNode> initializationDependencies = new HashSet<>();
  private Set<ComponentNode> destructionDependencies = new HashSet<>();

  ComponentNode(String objectName, Object object) {
    this.object = object;
    this.objectName = objectName;
  }

  @Override
  public String toString() {
    return String.format("%s / %s", objectName, object.getClass().getName());
  }

  Object getObject() {
    return object;
  }

  String getObjectName() {
    return objectName;
  }

  boolean isStarted() {
    return started;
  }

  void setStarted(boolean set) {
    this.started = set;
  }

  /**
   * @return set of nodes which should be destroyed before this node
   */
  Set<ComponentNode> getDestructionDependencies() {
    return this.destructionDependencies;
  }

  /**
   * @return set of nodes which should be initialized before this node
   */
  Set<ComponentNode> getInitializationDependencies() {
    return this.initializationDependencies;
  }

  /**
   * Add init dependency to n, i.e. n should initialize before this node
   *
   * @param n node to dependency
   */
  void addInitializationDependency(ComponentNode n) {
    List<ComponentNode> depTree = n.resolveInitializationDependencyPath(this);
    if (depTree != null) {
      throwCircularDependencyException(depTree);
    }
    this.initializationDependencies.add(n);
  }

  /**
   * Add destroy dependency to n, i.e. n should be destroyed before this node
   *
   * @param n node to dependency
   */
  void addDestructionDependency(ComponentNode n) {
    List<ComponentNode> depTree = n.resolveDestructionDependencyPath(this);
    if (depTree != null) {
      throwCircularDependencyException(depTree);
    }
    this.destructionDependencies.add(n);
  }

  //private methods


  /**
   * Check if node n is set to initialize before this node
   *
   * @param n node to check
   * @return initialization dependency path back to node n
   */
  private List<ComponentNode> resolveInitializationDependencyPath(ComponentNode n) {
    List<ComponentNode> dependencyPath = new ArrayList<>();
    dependencyPath.add(this);

    // if n is set to initialize after this, then return true
    if (this.getInitializationDependencies().contains(n)) {
      dependencyPath.add(n);
      return dependencyPath;
    }

    // if any node which is set to initialize before this node
    // (i.e. this node is initialized after that node)
    // also is set to initialize before n, then
    // return true
    for (ComponentNode previous : this.getInitializationDependencies()) {
      List<ComponentNode> prevDependencyPath = previous.resolveInitializationDependencyPath(n);
      if (prevDependencyPath != null) {
        dependencyPath.addAll(prevDependencyPath);
        return dependencyPath;
      }
    }
    // if not, there is no indication that we are set to start before n
    return null;
  }

  /**
   * Check if node n is set to destroy before this node
   *
   * @param n node to check
   * @return destruction dependency path back to node n
   */
  private List<ComponentNode> resolveDestructionDependencyPath(ComponentNode n) {
    List<ComponentNode> dependencyPath = new ArrayList<>();
    dependencyPath.add(this);

    // if n is listed to destroy before this, then return true
    if (this.getDestructionDependencies().contains(n)) {
      dependencyPath.add(n);
      return dependencyPath;
    }

    // if any node which is set to destroy before this node (we have a destroy dependency on)
    // has a destroy dependency on n, then we also have a destroy dependency on n.
    for (ComponentNode previous : this.getDestructionDependencies()) {
      List<ComponentNode> prevDependencyPath = previous.resolveDestructionDependencyPath(n);
      if (prevDependencyPath != null) {
        dependencyPath.addAll(prevDependencyPath);
        return dependencyPath;
      }
    }
    // if not, there is no indication that n is set to destroy before us
    return null;
  }

  private void throwCircularDependencyException(List<ComponentNode> depTree) {
    StringBuilder buf = new StringBuilder("Circular dependency: ");
    buf.append(this.getObjectName());
    for (ComponentNode nn : depTree) {
      buf.append(" -> ").append(nn.getObjectName());
    }
    throw new ComponentConfigurationException(buf.toString());
  }
}
