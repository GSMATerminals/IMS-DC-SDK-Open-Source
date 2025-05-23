/*
 *   Copyright 2025-China Telecom Research Institute.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
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
    namespace = "com.ct.ertclib.dc.feature.testing"

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
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}

dependencies {
    implementation(project(":core"))

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)

    // room
    implementation(libs.androidx.room.runtime)
    kapt(libs.androidx.room.compiler)
    compileOnly(files("${rootProject.projectDir}\\libs\\XXPermissions-18.2.aar"))
    compileOnly(files("${rootProject.projectDir}\\libs\\base-release.aar"))
}