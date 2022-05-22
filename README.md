# SonarQube Error Prone Plugin
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=erroraway_sonar-erroraway-plugin&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=erroraway_sonar-erroraway-plugin)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=erroraway_sonar-erroraway-plugin&metric=coverage)](https://sonarcloud.io/summary/new_code?id=erroraway_sonar-erroraway-plugin)

This [SonarQube](https://www.sonarqube.org/) uses [Error Prone](https://errorprone.info/) and reports findings in your project.
Additionally, some Error Prone plugins are included:
- [Uber's NullAway](https://github.com/uber/NullAway)
- [errorprone-slf4j](https://github.com/KengoTODA/errorprone-slf4j)
- [Autodispose2](https://uber.github.io/AutoDispose/lint-check/)

## Usage

Enable a quality profile including some rules, for NullAway you will need to configure the list of annotated packages

## Compatibility

The plugin is compatible with SonarQube 8.9 LTS and 9.4.

The Sonar analyzer and Error Prone must run on JDK 11 or newer but can analyze Java 8 code.
When running on JDK 16 or newer add the following options due to [JEP 396](https://openjdk.java.net/jeps/396):

```
--add-exports jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED
--add-exports jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED
--add-exports jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED
--add-exports jdk.compiler/com.sun.tools.javac.model=ALL-UNNAMED
--add-exports jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED
--add-exports jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED
--add-exports jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED
--add-exports jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED
--add-opens jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED
--add-opens jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED
```

## NullAway configuration

NullAway needs to be configured with the `nullaway.annotated.packages` option, for instance:

```
nullaway.annotated.packages=com.foo,org.bar
```