/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

group = "com.scireum.sirius-kernel"
version = "1"

plugins {
    id 'java-library'
}

repositories {
    jcenter()
}

dependencies {
    api 'org.apache.commons:commons-math3:3.6.1'

    implementation 'com.google.guava:guava:26.0-jre'

    testImplementation 'junit:junit:4.12'
}

