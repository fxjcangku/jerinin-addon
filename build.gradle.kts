import proguard.gradle.ProGuardTask

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("com.guardsquare:proguard-gradle:7.4.2")
    }
}

plugins {
    alias(libs.plugins.fabric.loom)
}

base {
    archivesName = properties["archives_base_name"] as String
    version = libs.versions.mod.version.get()
    group = properties["maven_group"] as String
}

repositories {
    maven {
        name = "meteor-maven"
        url = uri("https://maven.meteordev.org/releases")
    }
    maven {
        name = "meteor-maven-snapshots"
        url = uri("https://maven.meteordev.org/snapshots")
    }
    maven {
        name = "modrinth"
        url = uri("https://api.modrinth.com/maven")
    }
}

dependencies {
    // Fabric
    minecraft(libs.minecraft)
    mappings(libs.yarn)
    modImplementation(libs.fabric.loader)

    // Meteor
    modImplementation(libs.meteor.client)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(libs.versions.jdk.get().toInt()))
    }
}

fun toMinecraftCompat(version: String): String {
    val newMatch = Regex("""^(\d{2})\.([1-9]\d*)(?:\.([1-9]\d*))?$""").matchEntire(version)
    if (newMatch != null) {
        val (year, drop) = newMatch.destructured
        return "~$year.$drop"
    }
    val oldMatch = Regex("""^(\d+)\.(\d+)\.(\d+)$""").matchEntire(version)
    if (oldMatch != null) {
        return version
    }
    error("Invalid Minecraft version format: $version")
}

// ================================================================
//  混  淆  任  务  (编译 class → 乱码 class, 源码不变)
// ================================================================
tasks.register<ProGuardTask>("obfuscate") {
    dependsOn(tasks.remapJar)

    val remappedJar = tasks.remapJar.get().archiveFile.get().asFile
    val obfuscatedDir = layout.buildDirectory.dir("obfuscated").get().asFile

    injars(remappedJar)
    outjars(File(obfuscatedDir, remappedJar.name))

    // 用编译类路径当 libraryjars
    libraryjars(
        files(
            sourceSets.main.get().compileClasspath,
            "${System.getProperty("java.home")}/jmods/java.base.jmod"
        )
    )

    configuration(file("proguard.pro"))
}

// FIXME: ProGuard 尚不支持 JDK 25 (class version 69), 混淆暂跳过
// tasks.remapJar {
//     finalizedBy(tasks.named("obfuscate"))
// }

tasks {
    processResources {
        val propertyMap = mapOf(
            "version" to project.version,
            "minecraft_version" to toMinecraftCompat(libs.versions.minecraft.get()),
            "jdk_version" to libs.versions.jdk.get(),
        )

        inputs.properties(propertyMap)
        filesMatching("fabric.mod.json") {
            expand(propertyMap)
        }
    }

    jar {
        inputs.property("archivesName", project.base.archivesName.get())

        from("LICENSE") {
            rename { "${it}_${inputs.properties["archivesName"]}" }
        }
    }

    withType<JavaCompile>().configureEach {
        options.compilerArgs.addAll(
            listOf(
                "-Xlint:deprecation",
                "-Xlint:unchecked"
            )
        )
    }

    runClient {
        jvmArgs("-XX:+IgnoreUnrecognizedVMOptions", "-Xmx10G", "-Xms512M", "-Dorg.lwjgl.opengl.Display.allowSoftwareOpenGL=true")
    }
}
