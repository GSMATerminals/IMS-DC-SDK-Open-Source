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
    alias(libs.plugins.kotlin.android)
}

val libraryVersion = "1.0.0"
android {
    namespace = "com.ct.ertclib.dc.net"

    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        viewBinding = true
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = false
        }
        val release by getting {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}

androidComponents {
    onVariants(selector().all()) { variant ->
        afterEvaluate {
            tasks.named("assemble${variant.name.capitalize()}").configure {
                doLast {
                    val buildType = variant.buildType ?: "unknown"
                    val originalFile = file("${layout.buildDirectory.get()}/outputs/aar/${project.name}-${buildType}.aar")
                    if (originalFile.exists()) {
                        val newFile = file("${layout.buildDirectory.get()}/outputs/aar/net-${buildType}-${libraryVersion}.aar")
                        originalFile.renameTo(newFile)
                    }
                }
            }
        }
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.okhttp)

    // room
    kapt(libs.androidx.room.compiler)
    api("io.ktor:ktor-client-core:2.3.12")
    api("io.ktor:ktor-client-cio:2.3.12")
    api("io.ktor:ktor-client-websockets:2.3.12")

    api("io.livekit:livekit-android:2.25.2")
}