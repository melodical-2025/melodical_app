plugins {
    id("com.android.application")
    id("kotlin-android")

    // ✅ Firebase용 Google Services 플러그인 추가
    id("com.google.gms.google-services")

    // ✅ Flutter Gradle Plugin은 마지막에 적용
    id("dev.flutter.flutter-gradle-plugin")
}
repositories {
    google()
    mavenCentral()
    maven { url = uri("https://devrepo.kakao.com/nexus/content/groups/public/") }
}

android {
    namespace = "com.example.melodical"
    compileSdk = flutter.compileSdkVersion
    ndkVersion = "27.0.12077973"

    defaultConfig {
        applicationId = "com.example.melodical"
        manifestPlaceholders["applicationName"] = "Melodical"
        minSdk = flutter.minSdkVersion
        targetSdk = flutter.targetSdkVersion
        versionCode = flutter.versionCode
        versionName = flutter.versionName
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("debug")
        }
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }
}

flutter {
    source = "../.."
}
dependencies {
    // filebase BOM
    implementation(platform("com.google.firebase:firebase-bom:32.1.0"))
    implementation("com.google.firebase:firebase-auth-ktx")

    // Google Signin
    implementation("com.google.android.gms:play-services-auth:20.5.0")

    // Kakao SDK
    implementation("com.kakao.sdk:v2-auth:2.10.0")
    implementation("com.kakao.sdk:v2-user:2.10.0")
}