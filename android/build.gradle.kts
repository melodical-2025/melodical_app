// ✅ Firebase classpath를 포함한 buildscript 설정
buildscript {
    repositories {
        google()            // ✅ 반드시 있어야 함
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.1.4")
        classpath("com.google.gms:google-services:4.4.2") // ✅ Firebase용
    }
}

// ✅ Firebase 및 의존성 다운로드를 위한 저장소 설정
allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

// ✅ Flutter 멀티 모듈 build 폴더 재배치 설정
val newBuildDir: Directory = rootProject.layout.buildDirectory.dir("../../build").get()
rootProject.layout.buildDirectory.value(newBuildDir)

subprojects {
    val newSubprojectBuildDir: Directory = newBuildDir.dir(project.name)
    project.layout.buildDirectory.value(newSubprojectBuildDir)
}

subprojects {
    project.evaluationDependsOn(":app")
}

// ✅ clean 태스크 정의
tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
