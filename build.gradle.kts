import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    kotlin("jvm") version "2.2.21"
    id("com.gradleup.shadow") version "8.3.9"
}

group = "dev.etran"
version = "1.0.0"

val bedwarsApiVersion = "25.2"

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://repo.andrei1058.com/releases/")
}

dependencies {
    // Compile against the OLDEST supported Bukkit API (1.8.8). Anything that compiles here
    // exists on every newer server version too, which is what gives us 1.8 -> 26.x support.
    compileOnly("org.spigotmc:spigot-api:1.8.8-R0.1-SNAPSHOT") { isTransitive = false }

    // BedWars1058 API. Resolution order:
    //   1. -PbedwarsApiJar=/path/to/jar   (explicit override)
    //   2. any jar dropped into ./libs    (e.g. your server's BedWars1058-<version>.jar)
    //   3. the andrei1058 Maven repository
    val explicitJar = (findProperty("bedwarsApiJar") as String?)?.let { file(it) }
    val libsJars = fileTree("libs") { include("*.jar") }
    when {
        explicitJar != null -> compileOnly(files(explicitJar))
        !libsJars.isEmpty -> compileOnly(libsJars)
        else -> compileOnly("com.andrei1058.bedwars:bedwars-api:$bedwarsApiVersion") { isTransitive = false }
    }

    // Only needed so Kotlin can resolve annotations referenced by the BedWars API.
    compileOnly("org.jetbrains:annotations:24.1.0")

    testImplementation(kotlin("test"))
    // Transitive dependencies a real server provides, needed to load org.bukkit.ChatColor in tests.
    testRuntimeOnly("com.google.guava:guava:31.1-jre")
    testRuntimeOnly("commons-lang:commons-lang:2.6")
    testRuntimeOnly("net.md-5:bungeecord-chat:1.16-R0.4")
}

// Unit tests need the same API classes the addon compiles against.
configurations.testImplementation { extendsFrom(configurations.compileOnly.get()) }

kotlin {
    // Java 8 bytecode runs on every JVM that can host a BedWars1058 server.
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_1_8)
        apiVersion.set(KotlinVersion.KOTLIN_2_2)
        languageVersion.set(KotlinVersion.KOTLIN_2_2)
        freeCompilerArgs.addAll("-Xjvm-default=all")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks {
    processResources {
        val props = mapOf("version" to project.version)
        inputs.properties(props)
        filesMatching("plugin.yml") { expand(props) }
    }

    shadowJar {
        archiveBaseName.set("BedWars1058-WinFix")
        archiveClassifier.set("")
        // The addon is loaded by Bukkit next to dozens of other plugins, so the Kotlin runtime
        // must be relocated or it would clash with other plugins shipping a different version.
        relocate("kotlin", "dev.etran.bedwarswinfix.libs.kotlin")
        relocate("kotlinx", "dev.etran.bedwarswinfix.libs.kotlinx")
        exclude(
            "META-INF/*.kotlin_module", "META-INF/maven/**", "META-INF/versions/**", "DebugProbesKt.bin",
            "org/intellij/**", "org/jetbrains/annotations/**",
        )
    }

    test { useJUnitPlatform() }

    jar { enabled = false }
    build { dependsOn(shadowJar) }
}
