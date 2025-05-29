plugins {
    id("com.android.application")
    id("kotlin-android")

    // ✅ Firebase용 Google Services 플러그인 추가
    id("com.google.gms.google-services")

    // ✅ Flutter Gradle Plugin은 마지막에 적용
    id("dev.flutter.flutter-gradle-plugin")
}

android {
    namespace = "com.example.melodical"
    compileSdk = flutter.compileSdkVersion
    ndkVersion = "27.0.12077973"

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
    }

    defaultConfig {
        applicationId = "com.example.melodical"
        minSdk = flutter.minSdkVersion
        targetSdk = flutter.targetSdkVersion
        versionCode = flutter.versionCode
        versionName = flutter.versionName
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}

flutter {
    source = "../.."
}
