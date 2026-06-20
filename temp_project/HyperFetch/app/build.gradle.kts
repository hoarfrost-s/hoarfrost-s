
plugins {
    id("com.android.application")
    id("kotlin-android")
    id("org.jetbrains.kotlin.plugin.compose")
    //id("com.google.devtools.ksp") version "2.0.21-1.0.28"
	id("org.jetbrains.kotlin.kapt")  // 使用 kapt
}

android {
    namespace = "com.hyperfetch"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.hyperfetch"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        vectorDrawables {
            useSupportLibrary = true
        }

        /**
        ksp {
		
            arg("room.schemaLocation", "$projectDir/schemas") // 导出数据库
            arg("room.incremental", "true") // 增量编译
            //arg("room.expandProjection", "true") // 展开投影
			arg("room.verify", "false") // 禁用验证
        }*/
		kapt {
            arguments {
                arg("room.schemaLocation", "$projectDir/schemas")
                arg("room.incremental", "true")
                arg("room.expandProjection", "true")
				arg("room.verify", "false") // 禁用验证
            
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.11"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.fromTarget("17"))
    }
}
/**
// 提取 Linux/aarch64 原生库到 app/native-libs 目录
tasks.register<Sync>("setupSqliteNative") {
    description = "Extract SQLite JDBC native library for Room compiler on Android"
    from(zipTree(sqliteJdbc.singleFile)) {
        include("org/sqlite/native/Linux/aarch64/libsqlitejdbc.so")
    }
    into(file("native-libs"))
}

// 让所有 ksp 任务依赖提取任务
afterEvaluate {
    tasks.matching { it.name.startsWith("ksp") }.configureEach {
        dependsOn("setupSqliteNative")
    }
}
**/
/**
// 在 dependencies 块中添加 sqlite-jdbc 依赖（仅用于提取原生库）
val sqliteJdbc: Configuration by configurations.creating {
    isTransitive = false // 关键：禁止传递依赖，只保留sqlite-jdbc本身
}
**/
dependencies {
    //sqliteJdbc("org.xerial:sqlite-jdbc:3.45.1.0")
	//ksp(files("libs/sqlite-native-shim.jar"))
	//kapt(files("libs/room-verifier-stub.jar"))
    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2025.06.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Activity & Lifecycle
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.5")

    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    //ksp
	//kapt("androidx.room:room-compiler:2.6.1")
	// 使用本地修改后的room-compiler，而不是从仓库下载
    kapt(files("../tmp/room-compiler-2.6.1.jar"))

    // OkHttp
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // EventBus
    implementation("org.greenrobot:eventbus:3.3.1")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.10.0")

    // Core
    implementation("androidx.core:core-ktx:1.15.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Gson for JSON
    implementation("com.google.code.gson:gson:2.11.0")
}
/**
// 欺骗类加载器
val createSqliteNativeShimJar by tasks.registering(Jar::class) {
    description = "Create SQLite native library shim for Android build environment"
    group = "build"
    
    from(zipTree(sqliteJdbc.singleFile)) {
        include("org/sqlite/native/Linux/aarch64/libsqlitejdbc.so")
        eachFile {
            path = path.replace("org/sqlite/native/Linux", "org/sqlite/native/Linux-Android")
        }
    }
    archiveFileName.set("sqlite-native-shim.jar")
    destinationDirectory.set(layout.buildDirectory.dir("sqlite-shim"))
}

dependencies {
    ksp(files(createSqliteNativeShimJar.map { it.archiveFile }))
}

afterEvaluate {
    tasks.matching { it.name.startsWith("ksp") }.configureEach {
        dependsOn(createSqliteNativeShimJar)
    }
}
**/
/**
// 关键：在 KSP 任务执行前，将 shim JAR 添加到编译 classpath
afterEvaluate {
    tasks.withType<com.google.devtools.ksp.gradle.KspTask>().configureEach {
        // 确保 shim JAR 存在
        val shimJar = file("${projectDir}/libs/sqlite-native-shim.jar")
        if (shimJar.exists()) {
            // 添加到 KSP 编译器的 classpath
            (this as org.gradle.api.tasks.compile.JavaCompile).classpath = 
                classpath.plus(files(shimJar))
        }
    }
}
**/