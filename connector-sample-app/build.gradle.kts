plugins {
    id("connector-conventions")
    id("org.springframework.boot") version "3.4.0"
    id("io.spring.dependency-management") version "1.1.6"
}

dependencies {
    val libs = project.extensions.getByType<org.gradle.api.artifacts.VersionCatalogsExtension>().named("libs")
    implementation(project(":connector-spring-boot-starter"))
    implementation(project(":connector-server-http"))
    implementation(project(":connector-journal"))
    implementation(project(":connector-client-kafka"))
    implementation(libs.findLibrary("spring-boot-starter-web").get())
    implementation(libs.findLibrary("spring-boot-starter-jdbc").get())
    implementation(libs.findLibrary("spring-kafka").get())
    runtimeOnly("com.h2database:h2:2.2.224")
}
