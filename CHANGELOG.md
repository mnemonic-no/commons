# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [0.4.5] - 2021-04-26
### Changed
ARGUS-22965
- Upgraded Guice to version 5.0.1. The container module supports both version 4.2 and 5.0 of Guice and the dependency is
declared with the provided scope. Because of that, users can decide which version of Guice to utilize.
- Upgraded other dependencies to the newest bugfix versions.

## [0.4.4] - 2021-04-19
### Changed
ARGUS-22838
- Upgraded dependencies to the newest versions.

## [0.4.3] - 2021-02-24
### Added
ARGUS-22023
- Added function for subtracting one set from the other: SetUtils.difference

## [0.4.2] - 2020-12-15
### Changed
ARGUS-20874
- Upgraded all dependencies to the newest versions.
- Force upgraded httpclient and smallrye-config to fix vulnerabilities from transitive dependencies.
- Use RESTEasy as JAX-RS client instead of Jersey for docker-client.

## [0.4.1] - 2020-11-27
### Changed
ARGUS-18834
- ListUtils.list() and SetUtils.set() will omit any empty elements in the input array/collection.

## [0.4.0] - 2020-04-23
### Added
ARGUS-17574
- Added jupiter-docker module as a port of junit-docker to JUnit 5, i.e. implementing DockerResource (and subclasses) as JUnit 5 extensions.
- DockerExtension (and subclasses) no longer expose methods to manually truncate data. Instead, all extensions now perform
truncation automatically after each test using @AfterEach semantics.

Upgrade notes:
- Instead of using @ClassRule register the new extensions using @RegisterExtension.
- Remove manual truncation of data with truncate() or deleteIndices() (these methods have been removed).
- ElasticSearchDockerExtension no longer deletes all indices by default. Specify "_all" when registering the extension instead.

## [0.3.6] - 2020-04-21
### Changed
ARGUS-17574
- Switched to a maintained fork of Spotify's docker-client (org.mandas:docker-client).

## [0.3.5] - 2020-03-10
### Changed
ARGUS-17415
- Increased default offset range in AvailablePortFinder and added method to allow specifying a custom offset range.

## [0.3.4] - 2020-02-14
### Changed
ARGUS-17074
- Upgrade JUnit from 4.12 to 4.13.
- Upgrade Log4j from 2.12.1 to 2.13.0.
- Upgrade Mockito from 3.0.0 to 3.2.4.
- Upgrade Spring from 5.1.9.RELEASE to 5.2.3.RELEASE.

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