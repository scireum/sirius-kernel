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
        classpath("com.scireum:sirius-parent:1.0-RC1")
    }
}


plugins {
    id("java-library")
}

apply(plugin = "com.scireum.sirius-parent")

dependencies {
    api("org.apache.commons:commons-math3:3.6.1")

    // Provides the tools to load the system configuration
    api("com.typesafe:config:1.4.1")
    // Useful helper classes by Google
    api("com.google.guava:guava:31.0.1-jre")
    // Required logging bridge to make slf4j log to our logging system
    api("org.slf4j:slf4j-jdk14:1.7.32")
    // JSR305 annotations like @Nonnull etc
    api("com.google.code.findbugs:jsr305:3.0.2")
    // Used to auto-start Docker environments
    api("com.palantir.docker.compose:docker-compose-rule-core:1.7.0")

    testImplementation("org.junit.platform:junit-platform-runner:1.9.0")
}

