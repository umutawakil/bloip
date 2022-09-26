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
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-web")
	//implementation("org.springframework.boot:spring-boot-starter-jetty")
	implementation("org.springframework.boot:spring-boot-starter-thymeleaf")

	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.0") //TODO: It doesn't make sense that I need to manually add this given the start-test module above

	implementation("org.jsoup:jsoup:1.15.3")

	runtimeOnly( "mysql:mysql-connector-java")
	//testImplementation("com.h2database:h2")

	implementation("com.auth0:java-jwt:4.0.0")
	implementation("org.bouncycastle:bcprov-jdk15on:1.70")
	implementation("commons-codec:commons-codec:1.15")
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
