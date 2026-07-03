/*
 * Copyright 2025-China Telecom Research Institute.
 * All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.ct.ertclib.dc.buildlogic.convention.NCBuildType
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

plugins {
    id("newcall.android.application")
}

android {
    namespace = "com.ct.ertclib.dc.app"

    defaultConfig {
        applicationId = "com.ct.ertclib.dc"
        versionCode = 12
        versionName = "26.2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk{
            abiFilters += listOf("arm64-v8a")
        }
    }

    buildFeatures {
        viewBinding = true
        aidl = true
    }

    signingConfigs {
//        create("release") {
//            storeFile = file("xxx.jks")
//            storePassword = "xxx"
//            keyAlias = "xxx"
//            keyPassword = "xxx"
//        }
    }

    flavorDimensions += listOf("vendor")

    productFlavors {
        create("normal") {
            manifestPlaceholders["mainPermissionName"] = "com.ct.ertclib.dc.main_permission"
            manifestPlaceholders["getFeaturePermissionName"] = "com.ct.ertclib.dc.GET_FEATURE"
            manifestPlaceholders["fileProviderAuthority"] = "com.ct.ertclib.dc.fileprovider"
            manifestPlaceholders["sdkProviderAuthority"] = "com.ct.ertclib.dc.provider"
            manifestPlaceholders["providerPermissionName"] = "com.ct.ertclib.dc.provider.PERMISSION"
        }
        create("dialer") {
            manifestPlaceholders["mainPermissionName"] = "com.ct.ertclib.dc.main_permission"
            manifestPlaceholders["getFeaturePermissionName"] = "com.ct.ertclib.dc.GET_FEATURE"
            manifestPlaceholders["fileProviderAuthority"] = "com.ct.ertclib.dc.fileprovider"
            manifestPlaceholders["sdkProviderAuthority"] = "com.ct.ertclib.dc.provider"
            manifestPlaceholders["providerPermissionName"] = "com.ct.ertclib.dc.provider.PERMISSION"
        }
        create("lab") {
            applicationId = "com.ct.ertclib.dc.lab"
            manifestPlaceholders["mainPermissionName"] = "com.ct.ertclib.dc.lab.main_permission"
            manifestPlaceholders["getFeaturePermissionName"] = "com.ct.ertclib.dc.lab.GET_FEATURE"
            manifestPlaceholders["fileProviderAuthority"] = "com.ct.ertclib.dc.lab.fileprovider"
            manifestPlaceholders["sdkProviderAuthority"] = "com.ct.ertclib.dc.lab.provider"
            manifestPlaceholders["providerPermissionName"] = "com.ct.ertclib.dc.lab.provider.PERMISSION"
        }
        create("local") {
            applicationId = "com.ct.ertclib.dc.local"
            manifestPlaceholders["mainPermissionName"] = "com.ct.ertclib.dc.local.main_permission"
            manifestPlaceholders["getFeaturePermissionName"] = "com.ct.ertclib.dc.local.GET_FEATURE"
            manifestPlaceholders["fileProviderAuthority"] = "com.ct.ertclib.dc.local.fileprovider"
            manifestPlaceholders["sdkProviderAuthority"] = "com.ct.ertclib.dc.local.provider"
            manifestPlaceholders["providerPermissionName"] = "com.ct.ertclib.dc.local.provider.PERMISSION"
        }
    }
    productFlavors.all {
        manifestPlaceholders["CHANNEL_VALUE"] = name
    }

    sourceSets {
        getByName("lab") {
            manifest.srcFile("src/desk/AndroidManifest.xml")
        }
        getByName("local") {
            manifest.srcFile("src/desk/AndroidManifest.xml")
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = NCBuildType.DEBUG.applicationIdSuffix
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
            isDebuggable = true
        }
        getByName("release") {
            applicationIdSuffix = NCBuildType.RELEASE.applicationIdSuffix
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    applicationVariants.all {
        val buildType = buildType.name
        outputs.all {
            if (this is com.android.build.gradle.internal.api.ApkVariantOutputImpl) {
                val dateFormat = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                var apkName = "CtCallSDK_v${versionName}_${versionCode}_${dateFormat}_${flavorName}"
                if (buildType == "release") {
                    apkName = "${apkName}_release"
                } else if (buildType == "debug") {
                    apkName = "${apkName}_debug"
                }
                outputFileName = "$apkName.apk"
            }
        }
    }


    configurations {
        // implementation.exclude group: 'org.jetbrains' , module:'annotations'
        implementation { exclude(group = "com.intellij", module = "annotations") }
    }

    // 不压缩
//    packaging {
//        dex {
//            useLegacyPackaging = false
//        }
//        jniLibs {
//            useLegacyPackaging = false
//        }
//    }
}

dependencies {

    implementation(fileTree("${rootProject.projectDir}\\libs") {
        include("*.aar")  // 只匹配 .aar 文件
    })

    implementation(project(":core"))

    implementation(libs.apache.compress)
    implementation(libs.navigation.fragment.ktx)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.lucksiege.pictureselector)
    implementation(libs.gson)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    implementation(libs.refresh.layout.kernel)
    implementation(libs.refresh.header.classics)
    implementation(libs.refresh.footer.classics)
    implementation(libs.broccoli)
    implementation (libs.androidx.preference.ktx)
    implementation(libs.glide)
    implementation(libs.glide.transformations)
    implementation(libs.commons.io)
    api(libs.androidutils)
    api(libs.koin.android)
    implementation(libs.okhttp)
    // room
    implementation(libs.androidx.room.runtime)
    implementation (libs.androidx.room.ktx)

    implementation(libs.lucksiege.camerax)
    implementation(libs.androidx.sqlite.sqlite.framework)
    implementation(libs.constraintlayout)
    implementation(libs.paging.runtime)
    implementation(libs.room.paging)
    implementation(libs.lucksiege.pictureselector)
    implementation(libs.lucksiege.camerax)

    testImplementation(libs.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.gms.safetynet)

}

afterEvaluate {
    android.applicationVariants.forEach { variant ->
        val flavorName = variant.flavorName

        if (flavorName == "lab" || flavorName == "local") {
            // 获取该 variant 的 implementation 配置
            val configurationName = "${variant.name}Implementation"

            // 动态添加依赖
            project.dependencies.add(configurationName, project(":testing"))

            println("✓ Added testing to ${variant.name}")
        } else {
            println("✗ Skipped testing for ${variant.name}")
        }
    }
}