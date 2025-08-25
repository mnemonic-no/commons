# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [0.6.1] - 2025-08-24
### Deprecated
ARGUS-52403
- Deprecated module `jupiter-docker`

### Upgrade notes
We are no longer maintaining the `jupiter-docker` module, and it will
be removed in the near future.

Projects using this module should switch to using `testcontainers` instead.

## [0.6.0] - 2025-08-24
### Updated
ARGUS-52379
- Changed build target version to JDK 17

### Upgrade notes
- Clients must now be running on JDK17 or newer to use this library.

## [0.5.6] - 2025-04-15
### Updated
ARGUS-50749
- Make `DockerExtension.isContainerReachable` in subclasses return false instead of exception
  when `execCreate` fails (e.g. due to container not yet reachable)

## [0.5.5] - 2025-04-15
### Updated
ARGUS-50254
- Fix build pipeline

## [0.5.4] - 2025-04-15
### Updated
ARGUS-50749 
- Expose docker logs from `DockerExtension` for debugging

## [0.5.3] - 2025-03-25
### Updated
ARGUS-50254
- Add build pipeline

## [0.5.2] - 2025-03-10
### Updated
ARGUS-49752 
- Improve logging on component failure

## [0.5.1] - 2024-11-05
### Updated
ARGUS-48064
- Add `-E` option to BootStrap cmdline, which will make BootStrap treat any environment variable as a system property.
- This allows injecting environment variables directly 

## [0.5.0] - 2024-11-05
### Updated
ARGUS-47032
- Upgraded dependencies
- Deprecated junit-docker package

## [0.4.23] - 2023-03-11
### Changed
ARGUS-34837
- Changed `ComponentContainer` methods `initialize()` and `destroy()` to run 
  startup and shutdown in parallel threads where possible
  to reduce startup/shutdown times.

## [0.4.22] - 2023-03-03
### Fixed
ARGUS-33367
- Upgraded nexus-staging-maven-plugin in order to fix deployment to Maven Central.

## [0.4.21] - 2023-02-21
### Changed
ARGUS-32474
- Made project build with JDK17 (failed on javadoc generation).
- Upgraded RestEasy to the latest version compatible with docker-client.
- Upgraded other dependencies to the newest minor/bugfix versions.

## [0.4.20] - 2022-11-08
### Added
ARGUS-31958
- Added readFullStream(InputStream is):byte[] method to StreamUtils

## [0.4.19] - 2022-11-03
### Added
ARGUS-31803
- Added default uncaught exception handling to ComponentContainer to ensure logging of all exceptions.

## [0.4.18] - 2022-11-02
### Added
ARGUS-31800
- Added `AppendUtils.appendCollection()` which handles collections with a given max size.

## [0.4.17] - 2022-10-25
### Changed
ARGUS-28077
- DockerExtension and DockerResource will now pull docker images by default
  -  This behavior can be turned off by setting setSkipPullDockerImage(true)

## [0.4.16] - 2022-10-13
### Fixed
ARGUS-31350
- Fixed ClassCastException in GuiceBeanProvider for beans with recursive generic types, such as `Map<String, List<String>>`.

## [0.4.15] - 2022-09-20
### Fixed
ARGUS-30895
- Fixed an issue in GuiceBeanProvider where beans with generic types were omitted due to duplicated keys.
- Changed GuiceBeanProvider to throw an IllegalStateException if duplicated keys are detected.

## [0.4.14] - 2022-08-25
### Added
ARGUS-29930
- Added `SetUtils.ifEmpty(set, defaultValue)` which returns `set(defaultValue)` if the set is empty/null

## [0.4.13] - 2022-08-25
### Added
ARGUS-29459
- Added `LocalLoggingContext` to simplify setting a specific logging context for a specified block of code.
- Added `DeprecatedLoggingContext` to simplify adding a "deprecated" logging context variable for a specified block of code.

## [0.4.12] - 2022-05-12
### Changed
ARGUS-28992
- Exposing all context variables in `LoggingContext.getAll()`

## [0.4.11] - 2022-01-10
### Changed
ARGUS-26745
- Upgraded log4j to fix CVE-2021-44832 and CVE-2021-45105.
- Upgraded other dependencies to the newest versions.

## [0.4.10] - 2021-12-15
### Changed
ARGUS-26379
- Upgraded log4j to fix CVE-2021-45046 aka Log4Shell.

## [0.4.9] - 2021-12-10
### Changed
ARGUS-26379
- Upgraded log4j to fix CVE-2021-44228 aka Log4Shell.
- Upgraded other dependencies to the newest versions.

## [0.4.8] - 2021-12-06
### Changed
ARGUS-26251
- Log message as raw (unformatted) message if there are no string parameters.

## [0.4.7] - 2021-09-13
### Added
ARGUS-24979
- Added option `skipReachabilityCheck` to `DockerResource` and `DockerExtension` which will avoid the reachability check
against the running Docker container. Useful in the cases where applications implement their own logic.

## [0.4.6] - 2021-09-09
### Changed
ARGUS-24906
- Upgraded dependencies to the newest versions.

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