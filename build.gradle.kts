plugins {
    java
    id("org.springframework.boot") version "4.1.0"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"
description = "backend"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    // 用户系统: BCrypt 密码哈希（版本由 Spring Boot BOM 管理）
    implementation("org.springframework.security:spring-security-crypto")
    // 用户系统: JWT（api 编译期，impl/jackson 运行期）
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    runtimeOnly("org.postgresql:postgresql")
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation("dev.langchain4j:langchain4j-spring-boot4-starter:1.18.0-beta28")
    implementation("dev.langchain4j:langchain4j-open-ai-spring-boot4-starter:1.18.0-beta28")
    implementation("dev.langchain4j:langchain4j-embeddings-all-minilm-l6-v2:1.18.0-beta28")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
