// 모든 컨텍스트를 조립해 하나의 애플리케이션으로 실행한다. 비즈니스 코드는 두지 않는다.
plugins {
    id("spring-conventions")
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":libs:core-kernel"))
    implementation(project(":libs:id-generator"))
    implementation(libs.spring.boot.starter.webmvc)
    implementation(libs.spring.boot.starter.actuator)
}
