import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.50"
}

group = "org.example"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()

    flatDir {
        dirs("libs")
    }
}

buildscript {
    extra.set("picocliVersion", "3.9.3")
}

dependencies {
    val picocliVersion: String by rootProject.extra

    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))

    implementation(files("libs/datadsl.jar"))
    implementation(files("libs/intermediate-domain-metamodel.jar"))
    implementation(files("libs/servicedsl.jar"))
    implementation(files("libs/intermediate-service-metamodel.jar"))
    implementation(files("libs/technologydsl.jar"))
    implementation(files("libs/model-processing.jar"))

    implementation("info.picocli:picocli:$picocliVersion")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "9"
}

tasks.withType<Jar> {
    manifest {
        attributes("Main-Class" to "org.example.static_analyzer.StaticAnalyzer")
    }
}

val allDependencies = task("allDependencies", type = Jar::class) {
    archiveClassifier.set("all-dependencies")

    // Build fat JAR
    from(configurations.compileClasspath.get().filter{ it.exists() }.map { if (it.isDirectory) it else zipTree(it) })
    with(tasks["jar"] as CopySpec)

    manifest {
        attributes("Main-Class" to "org.example.static_analyzer.StaticAnalyzerKt")

        // Prevent security exception from JAR verifier
        exclude("META-INF/*.DSA", "META-INF/*.RSA", "META-INF/*.SF")
    }
}