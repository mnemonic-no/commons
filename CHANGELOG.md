# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [0.3.3] - 2019-09-10
### Changed
ARGUS-14772
- Avoid duplicate objects in dependency tree, when same singleton object is registered via multiple identifiers

## [0.3.2] - 2019-09-09
### Changed
- Upgrade Log4j from 2.11.2 to 2.12.1.
- Upgrade Mockito from 2.27.0 to 3.0.0.
- Upgrade Spring from 5.1.7.RELEASE to 5.1.9.RELEASE.

## [0.3.1] - 2019-09-03
### Added
ARGUS-14665 
- New interface DependencyProvider allows provider singletons to declare their
provided objects, to allow dependency resolvers to backtrack the providers from their provided objects.
- Added method scan() to ComponentDependencyResolver, allowing resolver to detect DependencyProvider 
implementations before resolving dependencies.
- Updated ComponentContainer to invoke ComponentDependencyResolver.scan() before resolving dependencies.  

### Usage
- All Singleton providers with a LifeCycleAspect should implement DependencyProvider and return the provided object
when getProvidedDependency() is invoked. If provider has not created any object yet, returning null is acceptable.
- This will improve dependency detection for dependencies declared on provided objects, where the LifecycleAspect
is on the provider itself.

```java
class MyDependency{...}

@Singleton
class MyDependingObject implements LifecycleAspect {
  /** This dependency points to an object without a LifecycleAspect. 
   *  However, since the provider declares it as a provided object, the provider will also be returned as a dependency. 
  **/
  @Dependency
  MyDependency dependency;
}

/**
* Provider declares itself as a DependencyProvider, and will return the provided object on getProvidedDependency()
*/
@Singleton
class MyDependencyProvider implements Provider<MyDependency>, DependencyProvider, LifecycleAspect {
  
  MyDependency obj;
  
  MyDependency get() {
    obj = new MyDependency();
    return obj;
  }  
  
  Object getProvidedDependency() {
    return obj;
  }

}
```