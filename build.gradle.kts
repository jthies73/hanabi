plugins {
    kotlin("jvm") version "2.3.0" // Kotlin version to use
    application
}

group = "org.example" // A company name, for example, `org.jetbrains`
version = "1.0-SNAPSHOT" // Version to assign to the built artifact

application {
    mainClass.set("MainKt")
}

repositories { // Sources of dependencies. See 1️⃣
    mavenCentral() // Maven Central Repository. See 2️⃣
}

dependencies { // All the libraries you want to use. See 3️⃣
    // Copy dependencies' names after you find them in a repository
    implementation(kotlin("stdlib")) // The Kotlin standard library
    testImplementation(kotlin("test")) // The Kotlin test library
}

tasks.test { // See 4️⃣
    useJUnitPlatform() // JUnitPlatform for tests. See 5️⃣
}