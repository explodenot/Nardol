plugins {
    java
    application
    jacoco
    alias(libs.plugins.shadow)
}

group = "com.explodenot.nardol"
version = "2.2.0"

val javaVersionStr = providers.gradleProperty("javaVersion").getOrElse("21")
val javaVersion = JavaLanguageVersion.of(javaVersionStr.toInt())

java {
    toolchain {
        languageVersion.set(javaVersion)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(javaVersionStr.toInt())
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Xlint:-processing"))
}

application {
    mainClass.set("com.explodenot.nardol.Main")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.picocli)
    implementation(libs.jline.terminal)
    implementation(libs.jline.terminal.jansi)
    implementation(libs.gson)

    annotationProcessor(libs.picocli.codegen)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit.jupiter)
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.shadowJar {
    archiveBaseName.set("nardol")
    archiveClassifier.set("all")
    manifest {
        attributes(
            "Main-Class" to "com.explodenot.nardol.Main",
            "Implementation-Title" to "nardol",
            "Implementation-Version" to project.version,
            "Enable-Native-Access" to "ALL-UNNAMED"
        )
    }
    mergeServiceFiles()

    // Exclude unwanted native libraries (only keep win x64/arm64, macOS x64/arm64, linux x64/arm64)
    exclude("org/fusesource/jansi/internal/windows_32/**")
    exclude("org/fusesource/jansi/internal/freebsd_32/**")
    exclude("org/fusesource/jansi/internal/freebsd_64/**")
    exclude("org/fusesource/jansi/internal/openbsd_64/**")
    exclude("org/fusesource/jansi/internal/sunos_**")
    exclude("org/fusesource/jansi/internal/aix_**")
    exclude("org/fusesource/jansi/internal/linux_32/**")
    exclude("org/fusesource/jansi/internal/linux_arm/**")
    exclude("org/fusesource/jansi/internal/linux_armv7/**")
    exclude("org/fusesource/jansi/internal/linux_ppc64/**")
    exclude("org/fusesource/jansi/internal/linux_s390x/**")
    exclude("org/fusesource/jansi/internal/linux_riscv64/**")
    exclude("org/fusesource/jansi/internal/osx_32/**")
    exclude("org/fusesource/jansi/internal/osx_ppc/**")

    // Minimize and keep critical classes
    minimize {
        exclude(dependency("info.picocli:picocli:.*"))
        exclude(dependency("org.jline:.*:.*"))
        exclude(dependency("com.google.code.gson:.*:.*"))
    }

    // Strip signature and licence clutter, keep THIRD-PARTY-NOTICES
    exclude("META-INF/*.SF")
    exclude("META-INF/*.DSA")
    exclude("META-INF/*.RSA")
    exclude("META-INF/INDEX.LIST")
    exclude("META-INF/DEPENDENCIES")
    exclude("META-INF/LICENSE.txt")
    exclude("META-INF/NOTICE.txt")

    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

val dependencySizeReport by tasks.registering {
    group = "verification"
    description = "Prints size breakdown of runtime dependencies"
    doLast {
        println("=== Runtime Dependencies Size Report ===")
        val config = configurations.runtimeClasspath.get()
        var total = 0L
        config.resolvedConfiguration.resolvedArtifacts.sortedByDescending { it.file.length() }.forEach { artifact ->
            val size = artifact.file.length()
            total += size
            val name = "${artifact.moduleVersion.id.group}:${artifact.name}:${artifact.moduleVersion.id.version}"
            println(String.format("%-60s %,10d bytes (%.2f MB)", name, size, size / (1024.0 * 1024.0)))
        }
        println("------------------------------------------------------------------------")
        println(String.format("%-60s %,10d bytes (%.2f MB)", "TOTAL RUNTIME DEPENDENCIES", total, total / (1024.0 * 1024.0)))
    }
}

val jarSizeCheck by tasks.registering {
    group = "verification"
    description = "Checks that the shadow jar size is below limits"
    dependsOn(tasks.shadowJar)
    doLast {
        val jarFile = tasks.shadowJar.get().archiveFile.get().asFile
        val size = jarFile.length()
        val maxLimit = providers.gradleProperty("maxJarSizeBytes").get().toLong()
        val targetLimit = providers.gradleProperty("targetJarSizeBytes").get().toLong()

        println(String.format("Jar path: %s", jarFile.absolutePath))
        println(String.format("Jar size: %,d bytes (%.2f MB)", size, size / (1024.0 * 1024.0)))
        println(String.format("Target size budget: < %,d bytes (%.2f MB)", targetLimit, targetLimit / (1024.0 * 1024.0)))
        println(String.format("Hard limit: < %,d bytes (%.2f MB)", maxLimit, maxLimit / (1024.0 * 1024.0)))

        if (size > maxLimit) {
            throw GradleException("FAT JAR SIZE EXCEEDED: %,d bytes > max limit %,d bytes!".format(size, maxLimit))
        }
        if (size > targetLimit) {
            logger.warn("WARNING: Jar size %,d bytes exceeds recommended target %,d bytes".format(size, targetLimit))
        } else {
            println("Jar size check PASSED within target budget!")
        }
    }
}

val verifyNoLegacyNames by tasks.registering {
    group = "verification"
    description = "Scans all text files to ensure no forbidden legacy tokens remain"
    doLast {
        val allowlistFile = file("config/legacy-names-allowlist.txt")
        val allowPatterns = if (allowlistFile.exists()) {
            allowlistFile.readLines()
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") }
                .map { Regex(it, RegexOption.IGNORE_CASE) }
        } else emptyList()

        val forbiddenPatterns = listOf(
            Regex("""globalping[-_ ]?lite""", RegexOption.IGNORE_CASE),
            Regex("""com\.example\.gpl""", RegexOption.IGNORE_CASE),
            Regex("""\bgpl\b""", RegexOption.IGNORE_CASE),
            Regex("""\bGpl[A-Z]\w*"""),
            Regex("""\bGPL_\w+""")
        )

        val violations = mutableListOf<String>()

        projectDir.walkTopDown()
            .filter { f ->
                val normPath = f.path.replace('\\', '/')
                f.isFile &&
                !normPath.contains("/build/") &&
                !normPath.contains("/.gradle/") &&
                !normPath.contains("/.git/") &&
                !normPath.endsWith("/legacy-names-allowlist.txt") &&
                !normPath.endsWith(".png") &&
                !normPath.endsWith(".bin") &&
                !normPath.endsWith(".key") &&
                !normPath.endsWith(".sig") &&
                !normPath.endsWith(".jar") &&
                !normPath.endsWith(".zip") &&
                !normPath.endsWith(".tar") &&
                !normPath.endsWith(".class")
            }
            .forEach { file ->
                try {
                    file.readLines().forEachIndexed { lineIdx, line ->
                        // Check if line matches an allowlisted pattern
                        val isAllowed = allowPatterns.any { it.containsMatchIn(line) }
                        if (!isAllowed) {
                            for (p in forbiddenPatterns) {
                                if (p.containsMatchIn(line)) {
                                    violations.add("${file.relativeTo(projectDir)}:${lineIdx + 1}: found legacy pattern '${p.pattern}' in line: ${line.trim()}")
                                }
                            }
                        }
                    }
                } catch (ignored: Exception) {}
            }

        if (violations.isNotEmpty()) {
            System.err.println("Legacy name violations found (${violations.size}):")
            violations.forEach { System.err.println("  $it") }
            throw GradleException("verifyNoLegacyNames FAILED: legacy tokens detected! See output above.")
        } else {
            println("verifyNoLegacyNames PASSED: No forbidden legacy tokens found.")
        }
    }
}

tasks.check {
    dependsOn(verifyNoLegacyNames)
    dependsOn(jarSizeCheck)
    dependsOn(dependencySizeReport)
}
