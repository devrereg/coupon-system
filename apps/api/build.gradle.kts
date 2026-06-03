plugins {
	kotlin("jvm") version "1.9.25"
	kotlin("plugin.spring") version "1.9.25"
	id("org.springframework.boot") version "3.5.14"
	id("io.spring.dependency-management") version "1.1.7"
	kotlin("plugin.jpa") version "1.9.25"
}

group = "kr.co.mindrepublic"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-data-redis")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	// Flyway: 스키마는 마이그레이션 SQL로 추적하고 Hibernate는 validate만(ddl-auto: validate).
	// Boot 3.5/Flyway 10+에서 PostgreSQL은 flyway-core 외에 DB별 모듈이 별도로 필요하다.
	implementation("org.flywaydb:flyway-core")
	implementation("org.flywaydb:flyway-database-postgresql")
	runtimeOnly("org.postgresql:postgresql")
	runtimeOnly("io.micrometer:micrometer-registry-prometheus")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}

allOpen {
	annotation("jakarta.persistence.Entity")
	annotation("jakarta.persistence.MappedSuperclass")
	annotation("jakarta.persistence.Embeddable")
}

tasks.test {
	// 기본 빌드에서는 동시성 테스트(@Tag("concurrency"))를 제외한다.
	// 의도적으로 red 상태인 Day 3 깨짐 재현 테스트가 build 를 깨지 않게 하기 위함.
	useJUnitPlatform {
		excludeTags("concurrency")
	}
}

// 동시성 깨짐 재현 테스트만 온디맨드로 실행: ./gradlew concurrencyTest
tasks.register<Test>("concurrencyTest") {
	useJUnitPlatform {
		includeTags("concurrency")
	}
	shouldRunAfter(tasks.test)
}
