plugins {
    kotlin("jvm")
    id("java-library")
}

group = "sunsetsatellite"
version = "1.0.0"
base.archivesName = "sunlite"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(8)
}

dependencies {
    implementation("com.google.code.gson:gson:2.10.1")
    implementation(kotlin("stdlib-jdk8"))

    // lsp
    implementation("org.eclipse.lsp4j:org.eclipse.lsp4j:0.24.0")
    implementation("org.eclipse.lsp4j:org.eclipse.lsp4j.jsonrpc:0.24.0")
}

task("lsp", Jar::class) {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    manifest {
        attributes["Main-Class"] = "sunsetsatellite.lsp.sunlite.LSPLauncher"
        exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
    }
    from(configurations.runtimeClasspath.get().map {
        if (it.isDirectory) it else zipTree(it)
    })
    archiveFileName.set("sunlite-lsp.jar")
    with(tasks.jar.get())
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    manifest {
        attributes["Main-Class"] = "sunsetsatellite.lang.sunlite.Sunlite"
        exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    archiveFileName.set("sunlite.jar")
}