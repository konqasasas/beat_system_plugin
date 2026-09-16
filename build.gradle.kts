import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService

plugins {
    java
}

group = "dev.konqasasas"
version = "0.1.0-SNAPSHOT"
val pluginVersion = version.toString()

repositories {
    maven {
        name = "spigotSnapshots"
        url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
        mavenContent {
            snapshotsOnly()
        }
    }
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:26.2-R0.1-SNAPSHOT")
    compileOnly("net.dmulloy2:ProtocolLib:5.4.0")
    implementation("com.google.code.gson:gson:2.14.0")
    implementation("org.yaml:snakeyaml:2.7")

    testImplementation("org.junit.jupiter:junit-jupiter:5.13.4")
    testImplementation("org.spigotmc:spigot-api:26.2-R0.1-SNAPSHOT")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

val serverRuntimePlugins = configurations.create("serverRuntimePlugins")
dependencies {
    serverRuntimePlugins("net.dmulloy2:ProtocolLib:5.4.0:all") {
        isTransitive = false
    }
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from({
        configurations.runtimeClasspath.get().map { dependency ->
            if (dependency.isDirectory) dependency else zipTree(dependency)
        }
    })
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = 25
    options.compilerArgs.add("-Xlint:deprecation")
}

tasks.processResources {
    filteringCharset = "UTF-8"
    inputs.property("version", pluginVersion)
    filesMatching("plugin.yml") {
        expand("version" to pluginVersion)
    }
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        exceptionFormat = TestExceptionFormat.FULL
    }
}

val runDirectory = layout.projectDirectory.dir("run")
val pluginDirectory = runDirectory.dir("plugins")

tasks.register<Copy>("deployRuntimePlugins") {
    group = "development"
    description = "Copies required server-side plugin dependencies into run/plugins."
    from(serverRuntimePlugins)
    into(pluginDirectory)
}

tasks.register<Copy>("deployPlugin") {
    group = "development"
    description = "Builds the plugin and copies its JAR into run/plugins."
    dependsOn(tasks.build)
    dependsOn("deployRuntimePlugins")
    from(tasks.jar.flatMap { it.archiveFile })
    into(pluginDirectory)
}

val javaToolchains = extensions.getByType<JavaToolchainService>()

tasks.register<Exec>("runServer") {
    group = "development"
    description = "Deploys the plugin and starts the local Spigot 26.2 server."
    dependsOn("deployPlugin")
    workingDir(runDirectory)
    standardInput = System.`in`

    doFirst {
        val serverJarName = providers.gradleProperty("spigotJar")
            .getOrElse("spigot-26.2.jar")
        val serverJar = runDirectory.file(serverJarName).asFile

        if (!serverJar.isFile) {
            throw GradleException(
                "Spigot server JAR was not found at ${serverJar.absolutePath}. " +
                    "Generate it with BuildTools (`java -jar BuildTools.jar --rev 26.2`) " +
                    "and copy it to run/spigot-26.2.jar."
            )
        }

        val javaLauncher = javaToolchains.launcherFor {
            languageVersion = JavaLanguageVersion.of(25)
        }.get()
        commandLine(
            javaLauncher.executablePath.asFile.absolutePath,
            "-Dfile.encoding=UTF-8",
            "-Dsun.stdout.encoding=UTF-8",
            "-Dsun.stderr.encoding=UTF-8",
            "-Xms1G",
            "-Xmx2G",
            "-jar",
            serverJar.absolutePath,
            "nogui"
        )
    }
}
