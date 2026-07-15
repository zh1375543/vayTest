import com.android.build.gradle.internal.api.ApkVariantOutputImpl
import java.text.SimpleDateFormat
import java.util.Date

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt")
    id("kotlin-parcelize")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

android {
    namespace = "com.vaycore.finance"
    compileSdk{
        version = release(36) {
            minorApiLevel = 1
        }
    }
    signingConfigs {
        create("key") {
            keyAlias = "vays"
            keyPassword = "123456"
            storeFile = file("../vaycomon.jks")
            storePassword = "123456"
        }
    }

    defaultConfig {
        applicationId = "com.eta.gaan.tc.puhunan"
        minSdk = 23
        targetSdk = 36
        versionCode = 100
        versionName = "1.0.0"
        multiDexEnabled = true
        signingConfig = signingConfigs.getByName("key")
        flavorDimensions("environment")
        vectorDrawables.useSupportLibrary = true

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        named("debug") {
            isDebuggable = true
            isZipAlignEnabled = true
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("key")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                file("proguard-rules.pro")
            )
        }
        named("release") {
            isDebuggable = false
            isZipAlignEnabled = true
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("key")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                file("proguard-rules.pro")
            )
        }
    }
    productFlavors {
        create("dev") {
            dimension = "environment"
            buildConfigField("String", "HTTP_HOST", "\"http://ph-cash-api.cc006e2ab86b64d7e843cbb0d774deebb.cn-hangzhou.alicontainer.com/\"")
            buildConfigField("String", "TRACK_HOST", "\"http://test-burying-point.cn-hangzhou.log.aliyuncs.com/logstores/survey-staging/\"")
        }
        create("prod") {
            dimension = "environment"
            buildConfigField("String", "HTTP_HOST", "\"https://api.papavay.com/\"")
            buildConfigField("String", "TRACK_HOST", "\"https://log.papavay.com/logstores/survey-prod/\"")
        }
    }
    applicationVariants.all {
        outputs.all {
            val output = this
            val flavorName = flavorName // dev or prod
            val buildType = buildType.name // debug or release
            val newApkName = "VayBee_${flavorName}_${versionName}(${versionCode})_${buildType}_" +
                    "${SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(Date())}.apk"

            (output as ApkVariantOutputImpl).outputFileName = newApkName
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar"))))
    // ui
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.swiperefreshlayout)

    // jetpack
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.process)

    // Utility libraries
    implementation(libs.github.xxpermissions)
    implementation(libs.coil)
    implementation(libs.coil.network.okhttp)
//    implementation(libs.glide)

    // Networking
    implementation(libs.okhttp)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.logging.interceptor)

    // GoogleID
    implementation(libs.google.identifier)

    // Analytics
    implementation(libs.appsflyer)
    implementation(libs.installreferrer)

    // Liveness detection
    implementation("${libs.df.liveness.silent.sdk.get()}@aar")
    implementation("${libs.df.liveness.silent.ui.get()}@aar")

    //google
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.appcheck.playintegrity)
    implementation(libs.review)
    implementation(libs.gms.play.services.auth.api.phone)
    implementation(libs.play.services.auth)
}
