plugins {
    kotlin("jvm") version "2.2.20"
    `java-library`
    id("org.jetbrains.dokka") version "1.9.20"
    id("com.vanniktech.maven.publish") version "0.30.0"
}

group = "com.docstencil"
version = "0.2.0"

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect:2.2.20")
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(8)
}

tasks.test {
    useJUnitPlatform()
    maxHeapSize = "2g"
}

// Expose test classes as an artifact for other projects to use
configurations {
    create(
        "testArtifacts",
        Action {
            extendsFrom(configurations.testRuntimeClasspath.get())
        },
    )
}

tasks.register<Jar>("testJar") {
    archiveClassifier.set("tests")
    from(sourceSets["test"].output)
}

artifacts {
    add("testArtifacts", tasks["testJar"])
}

// Add testJar to the maven publication
afterEvaluate {
    publishing {
        publications.withType<MavenPublication> {
            artifact(tasks["testJar"])
        }
    }
}

mavenPublishing {
    publishToMavenCentral(com.vanniktech.maven.publish.SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()

    coordinates("com.docstencil", "docstencil-core", version.toString())

    pom {
        name.set("DocStencil Core")
        description.set("A Kotlin-based document templating engine for Office documents")
        url.set("https://docstencil.com")
        inceptionYear.set("2025")

        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
            license {
                name.set("MIT License")
                url.set("https://opensource.org/licenses/MIT")
            }
        }

        developers {
            developer {
                id.set("docstencil")
                name.set("DocStencil Team")
                email.set("contact@docstencil.com")
            }
        }

        scm {
            connection.set("scm:git:git://github.com/docstencil/docstencil-core.git")
            developerConnection.set("scm:git:ssh://github.com/docstencil/docstencil-core.git")
            url.set("https://github.com/docstencil/docstencil-core")
        }
    }
}
