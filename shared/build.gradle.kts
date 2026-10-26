plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "stanissay.shopping.shared"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 34
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.ui.util)
    implementation(libs.foundation)
    implementation(libs.kotlinx.serialization.json)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.database)
    implementation(libs.firebase.auth)
    //noinspection LoginCredentials
    implementation(libs.androidx.credentials)
    //noinspection LoginCredentials
    implementation(libs.androidx.credentials.play.services.auth)
    //noinspection LoginCredentials
    implementation(libs.googleid)
    implementation(libs.androidx.datastore.preferences)
}