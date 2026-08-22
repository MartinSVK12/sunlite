plugins {
    kotlin("jvm")
    application
}

group = "sunsetsatellite"
version = "1.0.0"
base.archivesName = "sunlite"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("sunsetsatellite.sunlite.lang.Sunlite")
}

dependencies {
    implementation("com.google.code.gson:gson:2.10.1")
    implementation(kotlin("stdlib"))

    // lsp
    implementation("org.eclipse.lsp4j:org.eclipse.lsp4j:1.0.0")
    implementation("org.eclipse.lsp4j:org.eclipse.lsp4j.jsonrpc:1.0.0")

    // Source: https://mvnrepository.com/artifact/org.apache.commons/commons-compress
    implementation("org.apache.commons:commons-compress:1.28.0")
}

/*task("lsp", Jar::class) {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    manifest {
        attributes["Main-Class"] = "sunsetsatellite.sunlite.lsp.LSPLauncher"
        exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
    }
    from(configurations.runtimeClasspath.get().map {
        if (it.isDirectory) it else zipTree(it)
    })
    archiveFileName.set("sunlite-lsp.jar")
    with(tasks.jar.get())
}*/