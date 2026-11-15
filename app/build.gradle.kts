import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val signingPropertiesFile = rootProject.file("keystore.properties")
val signingProperties = Properties().apply {
    if (signingPropertiesFile.isFile) signingPropertiesFile.inputStream().use(::load)
}
fun signingValue(propertyName: String, environmentName: String): String? =
    signingProperties.getProperty(propertyName)?.takeIf(String::isNotBlank)
        ?: System.getenv(environmentName)?.takeIf(String::isNotBlank)

val releaseStoreFile = signingValue("storeFile", "ROTATION_RELEASE_STORE_FILE")
val releaseStorePassword = signingValue("storePassword", "ROTATION_RELEASE_STORE_PASSWORD")
val releaseKeyAlias = signingValue("keyAlias", "ROTATION_RELEASE_KEY_ALIAS")
val releaseKeyPassword = signingValue("keyPassword", "ROTATION_RELEASE_KEY_PASSWORD")
val hasReleaseSigning = listOf(releaseStoreFile, releaseStorePassword, releaseKeyAlias, releaseKeyPassword).all { it != null }

android {
    namespace = "com.rotationcontrol.force"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.rotationcontrol.force"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }
    buildFeatures { viewBinding = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    signingConfigs {
        create("release") {
            if (hasReleaseSigning) {
                storeFile = file(requireNotNull(releaseStoreFile))
                storePassword = requireNotNull(releaseStorePassword)
                keyAlias = requireNotNull(releaseKeyAlias)
                keyPassword = requireNotNull(releaseKeyPassword)
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            // R8 full mode is the default for AGP 8+. Framework entry points are also retained below.
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (hasReleaseSigning) signingConfig = signingConfigs.getByName("release")
        }
        debug { isMinifyEnabled = false }
    }
    packaging {
        jniLibs { useLegacyPackaging = false }
        resources { excludes += setOf("META-INF/DEPENDENCIES", "META-INF/LICENSE*", "META-INF/NOTICE*") }
    }
    // AGP 8.7+ emits 16 KB aligned native libraries when packaged uncompressed.
    lint { abortOnError = false }
}

if (!hasReleaseSigning) {
    logger.warn("Release signing credentials not found. assembleRelease/bundleRelease will produce an unsigned artifact. Set keystore.properties or ROTATION_RELEASE_* environment variables for a Play upload build.")
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("com.google.android.gms:play-services-ads:23.6.0")
    implementation("com.google.android.ump:user-messaging-platform:3.1.0")
}
