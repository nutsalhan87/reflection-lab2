plugins {
    id("java")
    application
    id("com.gradleup.shadow") version "9.3.2"
}

group = "org.labs"
version = "1.0"

application {
    mainClass.set("org.labs.GenerateExample")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.reflections:reflections:0.10.2")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.shadowJar {
    archiveClassifier.set("")
    manifest {
        attributes["Main-Class"] = application.mainClass.get()
    }
}

tasks.test {
    useJUnitPlatform()
}