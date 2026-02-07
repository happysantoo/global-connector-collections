plugins {
    id("connector-conventions")
}

dependencies {
    val libs = project.extensions.getByType<org.gradle.api.artifacts.VersionCatalogsExtension>().named("libs")
    api(project(":connector-core"))
    api(project(":connector-journal"))
    api(project(":connector-transformation"))
    api(project(":connector-observability"))
    implementation(libs.findLibrary("spring-context").get())
    implementation(libs.findLibrary("spring-jdbc").get())
    implementation(libs.findLibrary("spring-boot-autoconfigure").get())
    compileOnly(libs.findLibrary("opentelemetry-api").get())
}
