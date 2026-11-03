// 커버리지 게이트: core 모듈(와 libs)에만 적용한다. 라인·브랜치 80% 미만이면 check가 실패한다.
plugins {
    jacoco
}

val libs = the<VersionCatalogsExtension>().named("libs")

jacoco {
    toolVersion = libs.findVersion("jacoco").get().requiredVersion
}

// 측정 제외 목록. 항목마다 제외하는 이유를 주석으로 남긴다.
val coverageExclusions = listOf<String>(
    // 예: "**/SomeGeneratedClass*", // 이유: 코드 생성기가 만든 클래스
)

tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.named("test"))
    reports {
        xml.required = true
        html.required = true
    }
    if (coverageExclusions.isNotEmpty()) {
        classDirectories.setFrom(
            files(classDirectories.files.map { fileTree(it) { exclude(coverageExclusions) } })
        )
    }
}

tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    dependsOn(tasks.named("jacocoTestReport"))
    if (coverageExclusions.isNotEmpty()) {
        classDirectories.setFrom(
            files(classDirectories.files.map { fileTree(it) { exclude(coverageExclusions) } })
        )
    }
    violationRules {
        rule {
            limit {
                counter = "LINE"
                minimum = "0.80".toBigDecimal()
            }
            limit {
                counter = "BRANCH"
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}

tasks.named("check") {
    dependsOn(tasks.named("jacocoTestCoverageVerification"))
}
