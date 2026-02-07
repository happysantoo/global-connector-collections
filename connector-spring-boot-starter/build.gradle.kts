plugins {
    id("connector-conventions")
    id("org.springframework.boot") version "3.4.0"
    id("io.spring.dependency-management") version "1.1.6"
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = false
}

tasks.named<Jar>("jar") {
    enabled = true
}

// Starter main classes (actuator config) are exercised by tests but JaCoCo may not attribute when loaded from context
tasks.named<org.gradle.testing.jacoco.tasks.JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    violationRules {
        setFailOnViolation(false)
    }
}

dependencies {
    val libs = project.extensions.getByType<org.gradle.api.artifacts.VersionCatalogsExtension>().named("libs")
    api(project(":connector-spring"))
    implementation(project(":connector-server-http"))
    implementation(libs.findLibrary("spring-boot-starter").get())
    implementation(libs.findLibrary("spring-boot-starter-actuator").get())
    testImplementation("org.springframework.boot:spring-boot-starter-test:3.4.0")
    testImplementation(libs.findLibrary("spring-boot-starter-web").get())
    testImplementation(libs.findLibrary("spring-boot-starter-jdbc").get())
    testImplementation(libs.findLibrary("h2").get())
}
