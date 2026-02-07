plugins {
    id("connector-conventions") apply false
}

subprojects {
    apply(plugin = "connector-conventions")

    group = "com.example.connector"
    version = "0.1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}
