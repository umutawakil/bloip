import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	id("org.springframework.boot") version "2.7.0"
	id("io.spring.dependency-management") version "1.0.11.RELEASE"
	kotlin("jvm") version "1.7.10"
	kotlin("plugin.spring") version "1.6.21"
	kotlin("plugin.jpa") version "1.6.21"
}

group = "com.bloip"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_18

repositories {
	mavenCentral()
}

/*configurations {
	implementation.configure {
		exclude(module = "spring-boot-starter-tomcat")
		exclude("org.apache.tomcat")
	}
}*/

dependencies {
	implementation("org.hibernate:hibernate-core:5.6.14.Final")
	implementation("org.hibernate:hibernate-entitymanager:5.6.14.Final")
	implementation("javax.persistence:javax.persistence-api:2.2")
	implementation("org.springframework:spring-orm:5.3.25")
	implementation("com.zaxxer:HikariCP:5.0.1")

	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
	implementation("nz.net.ultraq.thymeleaf:thymeleaf-layout-dialect:3.1.0")
	//implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")

	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	implementation("org.junit.jupiter:junit-jupiter-api:5.9.0")

	implementation("org.jsoup:jsoup:1.15.3")

	runtimeOnly( "mysql:mysql-connector-java")

	implementation("com.auth0:java-jwt:4.0.0")
	implementation("org.bouncycastle:bcprov-jdk15on:1.70")
	implementation("commons-codec:commons-codec:1.15")
	implementation("software.amazon.awssdk:utils:2.17.281")
	implementation("javax.validation:validation-api:2.0.1.Final")
	implementation("org.hibernate:hibernate-validator:8.0.0.Final")
	implementation("org.json:json:20220924")
	implementation("com.amazonaws:aws-java-sdk-mediaconvert:1.12.326")
	implementation("com.amazonaws:aws-java-sdk-sqs:1.12.327")
	implementation("org.springframework.boot:spring-boot-starter-websocket:2.7.5")
	implementation("com.amazonaws:aws-java-sdk-translate:1.12.334")
	implementation("com.amazonaws:aws-java-sdk-sns:1.12.346")
	implementation("commons-validator:commons-validator:1.7")
	implementation("com.amazonaws:aws-java-sdk-ses:1.12.349")
	implementation("org.seleniumhq.selenium:selenium-java:4.7.0")
	implementation("org.seleniumhq.selenium:htmlunit-driver:4.6.0")
	implementation("com.amazonaws:aws-java-sdk-s3:1.12.353")

	implementation("com.auth0:java-jwt:4.2.2")
	implementation("com.google.firebase:firebase-admin:9.1.1")
}

tasks.withType<KotlinCompile> {
	kotlinOptions {
		freeCompilerArgs = listOf("-Xjsr305=strict")
		jvmTarget = "18"
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}
