plugins {
    kotlin("jvm")
}

group = "sunsetsatellite"
version = "1.0.0"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(8)
}

dependencies {
    implementation("com.google.code.gson:gson:2.10.1")
    implementation(kotlin("stdlib-jdk8"))
}