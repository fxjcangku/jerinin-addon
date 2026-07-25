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
    // New Mojang format: 26.1 or 26.1.2 → ~26.1
    val newMatch = Regex("""^(\d{2})\.([1-9]\d*)(?:\.([1-9]\d*))?$""").matchEntire(version)
    if (newMatch != null) {
        val (year, drop) = newMatch.destructured
        return "~$year.$drop"
    }
    // Old format: 1.21.1 → 1.21.1
    val oldMatch = Regex("""^(\d+)\.(\d+)\.(\d+)$""").matchEntire(version)
    if (oldMatch != null) {
        return version
    }
    error("Invalid Minecraft version format: $version")
}

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
        jvmArgs("-XX:+IgnoreUnrecognizedVMOptions", "-Xmx4G", "-Xms2G", "-Dorg.lwjgl.opengl.Display.allowSoftwareOpenGL=true")
    }
}
