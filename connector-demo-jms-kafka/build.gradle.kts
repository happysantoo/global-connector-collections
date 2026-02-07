plugins {
    id("connector-conventions")
    id("org.springframework.boot") version "3.4.0"
    id("io.spring.dependency-management") version "1.1.6"
}

dependencies {
    val libs = project.extensions.getByType<org.gradle.api.artifacts.VersionCatalogsExtension>().named("libs")
    implementation(project(":connector-spring-boot-starter"))
    implementation(project(":connector-server-jms"))
    implementation(project(":connector-client-kafka"))
    implementation(project(":connector-journal"))
    implementation(libs.findLibrary("spring-boot-starter-web").get())
    implementation(libs.findLibrary("spring-boot-starter-jdbc").get())
    implementation(libs.findLibrary("spring-kafka").get())
    implementation(libs.findLibrary("spring-jms").get())
    implementation(libs.findLibrary("resilience4j-spring-boot3").get())
    implementation("org.springframework.boot:spring-boot-starter-artemis:3.4.0")
    runtimeOnly(libs.findLibrary("h2").get())
}
