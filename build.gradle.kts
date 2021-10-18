plugins {
    kotlin("jvm") version "1.5.31"
    id("com.vanniktech.maven.publish") version "0.13.0"
    id("org.jetbrains.dokka") version "1.4.32"
    jacoco
}

version = "0.2.0"
group = "io.github.coteji"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.github.coteji:coteji-core:0.2.0")
    implementation("io.github.microutils:kotlin-logging-jvm:2.0.11")
    implementation("org.slf4j:slf4j-api:1.7.32")
    implementation("ch.qos.logback:logback-classic:1.2.6")
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.1")
    testImplementation("org.assertj:assertj-core:3.21.0")
    api("com.github.kittinunf.fuel:fuel:2.3.1")
    api("com.github.kittinunf.fuel:fuel-gson:2.3.1")
    api("com.google.code.gson:gson:2.8.8")
}

jacoco {
    toolVersion = "0.8.7"
}

tasks.jacocoTestReport {
    reports {
        xml.required.set(true)
        csv.required.set(false)
    }
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = "0.88".toBigDecimal()
            }
        }
    }
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
    finalizedBy(tasks.jacocoTestReport)
}