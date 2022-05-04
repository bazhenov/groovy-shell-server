## Build

Use `maven-assembly` plugin to build and create archive of `groovy-shell-server`:

```
$ mvn package
```

Archives will be placed in `groovy-shell-server/target/`.

# Deploy

Deploying to Maven Central:

```
$ mvn clean deploy -Ppublic
```

On Java 16 and higher following variable should be exported before deploy:

```
export MAVEN_OPTS="${MAVEN_OPTS} --add-opens=java.base/java.util=ALL-UNNAMED \
  --add-opens=java.base/java.lang.reflect=ALL-UNNAMED \
  --add-opens=java.base/java.text=ALL-UNNAMED \
  --add-opens=java.desktop/java.awt.font=ALL-UNNAMED"
```