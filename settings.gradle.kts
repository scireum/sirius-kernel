pluginManagement {
    repositories {
        mavenLocal()
        maven {
            name = "scireum-mvn"
            url = uri("https://mvn.scireum.com")
        }
        gradlePluginPortal()
    }
}

rootProject.name = "sirius-kernel"
