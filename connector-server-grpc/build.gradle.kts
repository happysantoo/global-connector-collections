plugins {
    id("connector-conventions")
}

dependencies {
    val libs = project.extensions.getByType<org.gradle.api.artifacts.VersionCatalogsExtension>().named("libs")
    api(project(":connector-core"))
    implementation(libs.findLibrary("grpc-netty-shaded").get())
    implementation(libs.findLibrary("grpc-protobuf").get())
    implementation(libs.findLibrary("grpc-stub").get())
    implementation(libs.findLibrary("spring-boot-autoconfigure").get())
    implementation(libs.findLibrary("spring-boot-starter-actuator").get())
}
