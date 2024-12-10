# SonarQube Error Prone Plugin
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=erroraway_sonar-erroraway-plugin&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=erroraway_sonar-erroraway-plugin)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=erroraway_sonar-erroraway-plugin&metric=coverage)](https://sonarcloud.io/summary/new_code?id=erroraway_sonar-erroraway-plugin)

This [SonarQube](https://www.sonarqube.org/) plugin uses [Error Prone](https://errorprone.info/) and reports findings in your project.
Additionally, some Error Prone plugins are included:
- [Uber's NullAway](https://github.com/uber/NullAway)
- [errorprone-slf4j](https://github.com/KengoTODA/errorprone-slf4j)
- [Picnic Error Prone Support](https://github.com/PicnicSupermarket/error-prone-support)

## Usage

Enable a quality profile including some rules, for NullAway you will need to configure the list of annotated packages

## Compatibility

The plugin is compatible with SonarQube from version 9.9 LTS through 10.x.

The Sonar analyzer and Error Prone must run on JDK 11 or newer but can analyze Java 8 code.
When running on JDK 16 or newer add the following options due to [JEP 396](https://openjdk.java.net/jeps/396):

```
--add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED
--add-exports=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED
--add-exports=jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED
--add-exports=jdk.compiler/com.sun.tools.javac.model=ALL-UNNAMED
--add-exports=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED
--add-exports=jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED
--add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED
--add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED
--add-opens=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED
--add-opens=jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED
```

See [.mvn/jvm.config](sonar-erroraway-sonar-plugin/src/test/resources/projects/simple/.mvn/jvm.config) for a way to do it with Maven and [gradle.properties](sonar-erroraway-sonar-plugin/src/test/resources/projects/simple/gradle.properties) for a way to do it with Gradle

From SonarQybe 10.6 the scanner also auto provisions a JRE and runs the analysis off that JVM. Since the JRE does not include the required compiler module, this needs to be disabled with `sonar.scanner.skipJreProvisioning=true`.

When these options are not set you will receive errors: 
```
Exception in thread "main" java.util.ServiceConfigurationError: com.google.errorprone.bugpatterns.BugChecker: Provider ... could not be instantiated
...
Caused by: java.lang.IllegalAccessError: class ... (in unnamed module @...) cannot access class com.sun.tools.javac.code.Symbol (in module jdk.compiler) because module jdk.compiler does not export com.sun.tools.javac.code to unnamed module @...
```

In SonarQube 10.5 the new feature to only download required plugins causes a NoClassDefFoundError. The workaround for this issue is to enable the `sonar.plugins.downloadOnlyRequired` option on the server AND on the analyzer: `-Dsonar.plugins.downloadOnlyRequired=false`

## NullAway configuration

NullAway needs to be configured with the `nullaway.annotated.packages` option, for instance:

```
nullaway.annotated.packages=com.foo,org.bar
```

## Developing the ErrorAway plugin

Running unit and integration tests:

```
mvn verify -Dsonar.server.version=10.5.1.90531 -Dsonar-java.version=7.34.0.35958 -Dsonar.web.port=9001
```
