plugins {
    kotlin("jvm") version "1.8.20"
    application
}

group = "cn.awalol"
version = "1.0-SNAPSHOT"

repositories {
    maven("https://maven.aliyun.com/repository/public/")
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("com.alibaba.fastjson2","fastjson2","2.0.29")
    implementation("com.alibaba.fastjson2:fastjson2-kotlin:2.0.29")
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(11)
}

application {
    mainClass.set("MainKt")
}