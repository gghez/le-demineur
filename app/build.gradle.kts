import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Local, never-committed config: signing credentials and optional Play Games ids.
// When absent (e.g. CI without secrets), signing/leaderboards degrade gracefully.
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "fr.ghez.demineur"
    compileSdk = 35

    defaultConfig {
        applicationId = "fr.ghez.demineur"
        minSdk = 24
        targetSdk = 35
        // Tag-driven on CI: the release workflow exports VERSION_CODE / VERSION_NAME
        // from the pushed git tag (vX.Y.Z). Local builds fall back to 1 / "1.0".
        versionCode = (System.getenv("VERSION_CODE") ?: "1").toInt()
        versionName = System.getenv("VERSION_NAME") ?: "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Play Games ids are read from local.properties (never committed). When
        // absent, the generated strings are empty; PlayGamesLeaderboard treats a
        // blank id as "not configured" and every call degrades silently, so the app
        // builds and runs fine without a Play Console account.
        resValue("string", "game_services_project_id", localProps.getProperty("playGamesAppId", ""))
        resValue("string", "leaderboard_beginner", localProps.getProperty("leaderboardBeginner", ""))
        resValue("string", "leaderboard_intermediate", localProps.getProperty("leaderboardIntermediate", ""))
        resValue("string", "leaderboard_expert", localProps.getProperty("leaderboardExpert", ""))
    }
    signingConfigs {
        // Release signing is configured only when local.properties provides the
        // upload keystore. Otherwise the "release" config is absent; the release
        // build type then fails fast (see below) so we never ship an unsigned AAB.
        val ksPath = localProps.getProperty("RELEASE_STORE_FILE")
        if (ksPath != null) create("release") {
            storeFile = file(ksPath)
            storePassword = localProps.getProperty("RELEASE_STORE_PASSWORD")
            keyAlias = localProps.getProperty("RELEASE_KEY_ALIAS")
            keyPassword = localProps.getProperty("RELEASE_KEY_PASSWORD")
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

// Fail fast on an unsigned release. Releases are CI-only (tag-driven): CI always
// writes the signing props into local.properties, so a missing "release" signing
// config means a misconfigured release attempt — never ship an unsigned AAB.
// Guard on the resolved task graph so debug builds, unit tests, and any non-release
// task (assembleDebug, testDebugUnitTest, …) are never affected.
gradle.taskGraph.whenReady {
    val buildsRelease = allTasks.any { task ->
        task.project == project &&
            (task.name.contains("Release") &&
                (task.name.startsWith("assemble") ||
                    task.name.startsWith("bundle") ||
                    task.name.startsWith("package")))
    }
    if (buildsRelease && android.signingConfigs.findByName("release") == null) {
        throw GradleException(
            "Release signing not configured: set RELEASE_STORE_FILE/" +
                "RELEASE_STORE_PASSWORD/RELEASE_KEY_ALIAS/RELEASE_KEY_PASSWORD in " +
                "local.properties — releases are CI-only (tag-driven)",
        )
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.material3)
    debugImplementation(libs.androidx.ui.tooling)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.play.services.games)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
}
