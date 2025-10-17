/*
 *   Copyright 2025-China Telecom Research Institute.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *        https://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

plugins {
    id("newcall.android.library")
    id("kotlin-kapt")
}

android {
    namespace = "com.ct.ertclib.dc.core"

    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        javaCompileOptions {
            annotationProcessorOptions {
                arguments += mapOf(
                    "room.schemaLocation" to "$projectDir/schemas",
                    "room.incremental" to "true"
                )
            }
        }

        ndk{
            abiFilters += listOf("armeabi-v7a", "arm64-v8a")
        }
    }

    buildFeatures {
        viewBinding = true
        aidl = true
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = false
        }
        val release by getting {
            isMinifyEnabled = false
//            proguardFiles(
//                getDefaultProguardFile("proguard-android-optimize.txt"),
//                "proguard-rules.pro"
//            )
        }
    }

    sourceSets["main"].jniLibs.srcDir("${rootProject.projectDir}\\libs")

}

dependencies {
    compileOnly(files("${rootProject.projectDir}\\libs\\ctec-release-1.0.0.aar"))
    compileOnly(files("${rootProject.projectDir}\\libs\\base-release.aar"))
    implementation(project(":oemec"))
    api(libs.appcompat)
    implementation(files("${rootProject.projectDir}\\libs\\xstream-1.4.9.jar"))
    implementation(libs.localbroadcastmanager)
    implementation(libs.okhttp)
    // room
    implementation(libs.androidx.room.runtime)
    kapt(libs.androidx.room.compiler)
    implementation (libs.androidx.room.ktx)
    // paging
    implementation(libs.paging.runtime)
    implementation(libs.room.paging)
    implementation(libs.material)
    implementation(libs.glide)
    implementation(libs.glide.transformations)
    implementation(libs.lucksiege.pictureselector)
    implementation(libs.lucksiege.camerax)
    implementation(libs.zelory.compressor)
    implementation(libs.gson)
    implementation(libs.commons.io)

    api(libs.koin.android)
    implementation (libs.androidx.preference.ktx)
    compileOnly(files("${rootProject.projectDir}\\libs\\XXPermissions-18.2.aar"))
    compileOnly(files("${rootProject.projectDir}\\libs\\DSBridge-Android-3.0.0.aar"))
    compileOnly(files("${rootProject.projectDir}\\libs\\Common-4.1.12.aar"))
    compileOnly(files("${rootProject.projectDir}\\libs\\FilePicker-4.1.12.aar"))
    compileOnly(files("${rootProject.projectDir}\\libs\\reflect_helper-release.aar"))
    api(libs.androidutils)

    testImplementation(libs.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.apache.compress)
    implementation(libs.navigation.fragment.ktx)
    implementation(libs.material)
    implementation(libs.constraintlayout)
}