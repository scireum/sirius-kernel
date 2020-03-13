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
        jcenter()
    }
    dependencies {
        classpath("com.scireum:sirius-parent:1.0-SNAPSHOT")
    }
}


plugins {
    id("java-library")
}

apply(plugin = "sirius-parent")

dependencies {
    api("org.apache.commons:commons-math3:3.6.1")

    // Provides the tools to load the system configuration
    api("com.typesafe:config:1.3.4")
    // Useful helper classes by Google
    api("com.google.guava:guava:21.0")
    // The logging framework we use
    api("log4j:log4j:1.2.17")
    //Required logging bridge to make slf4j log to log4j
    api("org.slf4j:slf4j-log4j12:1.7.28")
    // JSR305 annotations like @Nonnull etc
    api("com.google.code.findbugs:jsr305:3.0.1")
    // Used to auto-start Docker environments
    api("com.palantir.docker.compose:docker-compose-rule-core:1.3.0")
}

