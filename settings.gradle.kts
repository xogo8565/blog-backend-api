rootProject.name = "blog-backend"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

include(
    "libs:core-kernel",
    "libs:id-generator",
    "mains:monolith-main",
)
