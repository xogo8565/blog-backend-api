// Spring을 쓰는 모듈의 공통 설정. 버전은 Spring Boot BOM이 관리한다.
plugins {
    id("java-conventions")
}

val libs = the<VersionCatalogsExtension>().named("libs")
fun library(name: String) = libs.findLibrary(name).get()

dependencies {
    implementation(platform(library("spring-boot-dependencies")))
    annotationProcessor(platform(library("spring-boot-dependencies")))
    testImplementation(library("spring-boot-starter-test"))
}
