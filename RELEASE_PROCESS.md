## Upgrading ErrorProne:

- Update the `errorprone.version` in the `pom.xml`
- Copy the content of the `docs/bugpatterns` folder or the ErrorProne project into `src/main/resources/errorprone`
- Update the rules count in `src/main/java/com/github/erroraway/sonarqube/ErrorAwayRulesDefinition.java`

## Releasing a version

- Commit the `pom.xml` with a stable version (e.g. 1.2.3) and push, the build will deploy the plugin to maven central
- Commit the `pom.xml` with the next snapshot version (e.g. 1.2.4-SNAPSHOT)
- Create a Github release on the commit corresponding to stable version

