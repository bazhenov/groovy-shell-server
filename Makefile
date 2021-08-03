.PHONY: deploy

MAVEN_OPTS=--add-opens=java.base/java.util=ALL-UNNAMED\
	--add-opens=java.base/java.lang.reflect=ALL-UNNAMED\
	--add-opens=java.base/java.text=ALL-UNNAMED\
	--add-opens=java.desktop/java.awt.font=ALL-UNNAMED

# At the moment specific MAVEN_OPTS should be provided to bypass Java Module System restrictions 
# see: https://issues.sonatype.org/browse/OSSRH-66257

deploy:
	MAVEN_OPTS="${MAVEN_OPTS}" mvn clean deploy