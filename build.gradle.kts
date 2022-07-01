import org.jetbrains.kotlin.ir.backend.js.compile

plugins {
    val kotlinVersion = "1.7.0"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion
    id("net.mamoe.mirai-console") version "2.11.1"

}
group = "com.example"
version = "0.1.5"
repositories {
    maven("https://maven.aliyun.com/repository/public")
    mavenCentral()
    jcenter()
    mavenCentral()
}

dependencies{
    implementation ("com.beust:klaxon:5.5")
    implementation ("io.github.evanrupert:excelkt:1.0.2")
}


