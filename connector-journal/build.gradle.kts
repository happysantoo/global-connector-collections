plugins {
    id("connector-conventions")
}

dependencies {
    val libs = project.extensions.getByType<org.gradle.api.artifacts.VersionCatalogsExtension>().named("libs")
    api(project(":connector-core"))
    implementation(libs.findLibrary("spring-jdbc").get())
    implementation(libs.findLibrary("spring-context").get())
    testImplementation(libs.findLibrary("h2").get())
}
