plugins {
    id("connector-conventions")
}

dependencies {
    api(project(":connector-core"))
    api(project(":connector-observability"))
}
