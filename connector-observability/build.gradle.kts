plugins {
    id("connector-conventions")
}

dependencies {
    val libs = project.extensions.getByType<org.gradle.api.artifacts.VersionCatalogsExtension>().named("libs")
    api(project(":connector-core"))
    implementation(libs.findLibrary("opentelemetry-api").get())
    implementation(libs.findLibrary("opentelemetry-sdk").get())
}
