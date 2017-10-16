mnemonic commons
================

*mnemonic commons* provides a set of reusable Java utilities bundled and distributed as multiple jar files. Following the Don't-Repeat-Yourself principle those libraries contain common functionality which we have written over the years for our projects. We publish them as Open Source with the hope that they might be useful to others as well.

## Usage

[![Javadocs](https://javadoc.io/badge/no.mnemonic.commons/component.svg?color=orange&label=component)](https://javadoc.io/doc/no.mnemonic.commons/component)
[![Javadocs](https://javadoc.io/badge/no.mnemonic.commons/container.svg?color=orange&label=container)](https://javadoc.io/doc/no.mnemonic.commons/container)
[![Javadocs](https://javadoc.io/badge/no.mnemonic.commons/junit-docker.svg?color=orange&label=junit-docker)](https://javadoc.io/doc/no.mnemonic.commons/junit-docker)
[![Javadocs](https://javadoc.io/badge/no.mnemonic.commons/logging.svg?color=orange&label=logging)](https://javadoc.io/doc/no.mnemonic.commons/logging)
[![Javadocs](https://javadoc.io/badge/no.mnemonic.commons/logging-log4j.svg?color=orange&label=logging-log4j)](https://javadoc.io/doc/no.mnemonic.commons/logging-log4j)
[![Javadocs](https://javadoc.io/badge/no.mnemonic.commons/metrics.svg?color=orange&label=metrics)](https://javadoc.io/doc/no.mnemonic.commons/metrics)
[![Javadocs](https://javadoc.io/badge/no.mnemonic.commons/testtools.svg?color=orange&label=testtools)](https://javadoc.io/doc/no.mnemonic.commons/testtools)
[![Javadocs](https://javadoc.io/badge/no.mnemonic.commons/utilities.svg?color=orange&label=utilities)](https://javadoc.io/doc/no.mnemonic.commons/utilities)

## Installation

All libraries provided by *mnemonic commons* are directly available from Maven Central. Just declare a dependency in your pom.xml and start using it:

```xml
<dependency>
  <groupId>no.mnemonic.commons</groupId>
  <artifactId>${artifactId}</artifactId>
  <version>${version}</version>
</dependency>
```

Replace ${artifactId} and ${version} with the library and version you want to use.

## Requirements

None, dependencies will be handled by Maven automatically.

## Known issues

See [Issues](https://github.com/mnemonic-no/commons/issues).

## Contributing

See the [CONTRIBUTING.md](CONTRIBUTING.md) file.

## License

*mnemonic commons* is released under the ISC License. See the bundled LICENSE file for details.