plugins {
    kotlin("plugin.spring") version "1.9.25"
    id("org.springframework.boot") version "3.4.4"
    id("io.spring.dependency-management") version "1.1.7"
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.ai:spring-ai-bom:${property("springAiVersion")}")
    }
}

dependencies {
    api("org.springframework.ai:spring-ai-ollama-spring-boot-starter")
    api("org.springframework.ai:spring-ai-qdrant-store")

    testImplementation("org.springframework.boot:spring-boot-starter-test")

    implementation(project(":core:common"))
}