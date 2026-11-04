// 모든 Java 모듈의 공통 설정: 툴체인, 정적 분석, 테스트 태스크
plugins {
    java
    checkstyle
    id("com.diffplug.spotless")
    id("com.github.spotbugs")
}

val libs = the<VersionCatalogsExtension>().named("libs")
fun version(name: String) = libs.findVersion(name).get().requiredVersion
fun library(name: String) = libs.findLibrary(name).get()

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(version("java"))
    }
}

dependencies {
    testImplementation(platform(library("spring-boot-dependencies")))
    testImplementation(library("junit-jupiter"))
    testImplementation(library("assertj-core"))
    testRuntimeOnly(library("junit-platform-launcher"))
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-parameters", "-Xlint:unchecked,deprecation"))
}

// 포맷: google-java-format 버전을 고정해 로컬과 CI 결과를 맞춘다
spotless {
    java {
        target("src/**/*.java")
        googleJavaFormat(version("google-java-format"))
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
}

checkstyle {
    toolVersion = version("checkstyle")
    configFile = rootProject.file("config/checkstyle/checkstyle.xml")
    maxWarnings = 0
}

spotbugs {
    excludeFilter = rootProject.file("config/spotbugs/exclude.xml")
}

tasks.named<com.github.spotbugs.snom.SpotBugsTask>("spotbugsMain") {
    reports.create("html") { required = true }
    reports.create("xml") { required = true }
}

tasks.named<com.github.spotbugs.snom.SpotBugsTask>("spotbugsTest") {
    enabled = false
}

// test는 전체, unitTest와 integrationTest는 @Tag("integration") 기준으로 나눠 실행한다
tasks.test {
    useJUnitPlatform()
}

val testSourceSet = sourceSets.test.get()

tasks.register<Test>("unitTest") {
    description = "integration 태그가 없는 테스트만 실행한다."
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    testClassesDirs = testSourceSet.output.classesDirs
    classpath = testSourceSet.runtimeClasspath
    useJUnitPlatform { excludeTags("integration") }
    failOnNoDiscoveredTests = false
}

tasks.register<Test>("integrationTest") {
    description = "integration 태그가 붙은 테스트(Testcontainers 등)만 실행한다. Docker가 필요하다."
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    testClassesDirs = testSourceSet.output.classesDirs
    classpath = testSourceSet.runtimeClasspath
    useJUnitPlatform { includeTags("integration") }
    failOnNoDiscoveredTests = false
    shouldRunAfter("unitTest")
}
