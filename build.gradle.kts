/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

plugins {
    id("java-library")
    id("com.scireum.sirius-parent") version "11.0.6"
    id("org.sonarqube") version "3.4.0.2513"
    id("io.github.joselion.pretty-jupiter") version "2.2.0"
}

dependencies {
    // Provides the tools to load the system configuration
    api("com.typesafe:config:1.4.2")
    // Useful helper classes by Google
    api("com.google.guava:guava:31.1-jre")
    // Required logging bridge to make slf4j log to our logging system
    api("org.slf4j:slf4j-jdk14:2.0.3")
    // JSR305 annotations like @Nonnull etc
    api("com.google.code.findbugs:jsr305:3.0.2")

    // Used to auto-start Docker environments
    api("com.palantir.docker.compose:docker-compose-rule-core:1.8.0") {
        exclude(group = "com.palantir.conjure.java.runtime", module = "conjure-java-jackson-serialization")
        exclude(group = "com.palantir.docker.compose", module = "docker-compose-rule-events-api-objects")
    }
    // Required, as the version provided by docker-compose-rule-core has security issues
    api("commons-io:commons-io:2.11.0")
    // Required, as the version provided by docker-compose-rule-core has security issues
    api("com.fasterxml.jackson.core:jackson-databind")
    api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    // Required, as the version provided by docker-compose-rule-core has security issues
    api("org.yaml:snakeyaml:2.0")
}

sonarqube {
    properties {
        property("sonar.sourceEncoding", "UTF-8")
    }
}
