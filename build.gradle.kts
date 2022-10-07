/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

group = "com.scireum.sirius-kernel"
version = "DEVELOPMENT-SNAPSHOT"

buildscript {
    repositories {
        mavenLocal()
        maven("https://mvn.scireum.com")
    }
    dependencies {
        classpath("com.scireum:sirius-parent:1.0-RC2")
    }
}


plugins {
    id("java-library")
}

apply(plugin = "com.scireum.sirius-parent")

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
    api("com.fasterxml.jackson.core:jackson-databind:2.14.0-rc1")
    // Required, as the version provided by docker-compose-rule-core has security issues
    api("org.yaml:snakeyaml:1.32")
}

