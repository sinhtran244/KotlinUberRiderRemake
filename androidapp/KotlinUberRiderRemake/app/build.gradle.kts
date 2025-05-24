plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
    alias(libs.plugins.google.android.libraries.mapsplatform.secrets.gradle.plugin)

}

android {
    namespace = "com.example.kotlinuberriderremake"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.kotlinuberriderremake"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.play.services.maps)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.firebaseui:firebase-ui-auth:9.0.0")
    //Material
    implementation("com.google.android.material:material:1.1.0")

    //RxJavaD
    implementation("io.reactivex.rxjava2:rxandroid:2.1.1")
    implementation("io.reactivex.rxjava2:rxjava:2.2.9")

    implementation("com.firebaseui:firebase-ui-auth:9.0.0")

    implementation("com.karumi:dexter:6.1.2")
    implementation("com.google.firebase:firebase-database:21.0.0")


    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
    implementation ("androidx.navigation:navigation-fragment-ktx:2.5.3")
    implementation ("androidx.navigation:navigation-ui-ktx:2.5.3")

    //location
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // Circle Image
    implementation ("de.hdodenhof:circleimageview:3.1.0")

    implementation ("com.google.firebase:firebase-messaging:23.4.0")

    //Geofire
    implementation("com.google.android.material:material:1.1.0")

    implementation("com.firebase:geofire-android-common:3.2.0") // Common module
    implementation("com.google.firebase:firebase-database:21.0.0")
    implementation("com.firebase:geofire-android:3.2.0")

    implementation("com.google.android.gms:play-services-auth:21.1.1")

    implementation(platform("com.google.firebase:firebase-bom:33.0.0"))
    //Retrofit
    implementation("com.squareup.retrofit2:adapter-rxjava2:2.6.1")
    implementation ("com.squareup.retrofit2:converter-scalars:2.6.1")
    implementation ("com.squareup.retrofit2:converter-gson:2.6.1")

    //sliding up
    implementation ("com.sothree.slidinguppanel:library:3.3.1")

    //GG place
    implementation ("com.google.android.libraries.places:places:4.2.0")

    implementation ("com.mikepenz:iconics-core:5.3.0")

    // chọn một hoặc nhiều icon pack, ví dụ Community Material
    implementation ("com.mikepenz:community-material-typeface:5.5.55.0")
}