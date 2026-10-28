plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
}

android {
  namespace = "com.arady.phone"
  compileSdk = 36

  defaultConfig {
    applicationId = "com.arady.phone"
    minSdk = 26
    targetSdk = 35
    versionCode = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1
    versionName = "2.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  // Do not remove or modify these signingConfigs. They are necessary for building, installing,
  // and updating Android apps in AI Studio as well as CI GitHub Actions releases.
  signingConfigs {
    create("release") {
      val phoneKeystore = file("phone.jks")
      val hasPhoneKey = phoneKeystore.exists() && phoneKeystore.length() > 0 &&
          !System.getenv("PHONE_KEYSTORE_PASSWORD").isNullOrEmpty()
      if (hasPhoneKey) {
        storeFile = phoneKeystore
        storePassword = System.getenv("PHONE_KEYSTORE_PASSWORD")
        keyAlias = "phone"
        keyPassword = System.getenv("PHONE_KEY_PASSWORD")
      } else {
        val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
        storeFile = file(keystorePath)
        storePassword = System.getenv("STORE_PASSWORD")
        keyAlias = "upload"
        keyPassword = System.getenv("KEY_PASSWORD")
      }
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      signingConfig = signingConfigs.getByName("debugConfig")
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  buildFeatures {
    compose = true
    buildConfig = true
  }

  sourceSets {
    getByName("main").java.directories.add("src/main/kotlin")
    getByName("test").java.directories.add("src/test/kotlin")
  }

  testOptions {
    unitTests {
      isIncludeAndroidResources = true
    }
  }

  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation("androidx.compose.foundation:foundation")
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.kotlinx.coroutines.android)

  testImplementation(libs.junit)
}
