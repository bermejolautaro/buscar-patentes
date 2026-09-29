import org.jetbrains.kotlin.gradle.dsl.JvmTarget

/**
 * Lo que comparten el Android y el iPhone (D1 de la 006): reglas, base, pantallas y mapa.
 *
 * Lo que es de cada sistema vive en `androidMain` e `iosMain`, detrás de los `expect` del
 * contrato P. El iPhone se compila solo en la nube: en Windows los targets de iOS se saltean.
 */
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

kotlin {
    android {
        namespace = "ar.lauta.buscarpatentes.shared"
        compileSdk = 37
        minSdk = 26

        withHostTestBuilder {}

        // Los íconos de las pantallas son recursos de Compose, que en el Android viajan como
        // recursos de la biblioteca.
        androidResources.enable = true

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    // D3: el iPhone real, y el simulador solo para correr las pruebas comunes en la nube.
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.cmp.runtime)
            implementation(libs.cmp.foundation)
            implementation(libs.cmp.ui)
            implementation(libs.cmp.material3)
            implementation(libs.cmp.resources)
            implementation(libs.cmp.backhandler)
            implementation(libs.maplibre.compose)
            implementation(libs.room.runtime)
            implementation(libs.sqlite.bundled)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.io.core)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        androidMain.dependencies {
            implementation(libs.maplibre.compose.opengl.android)
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.activity.compose)
            implementation(libs.play.services.location)
        }
    }
}

compose.resources {
    packageOfResClass = "ar.lauta.buscarpatentes.recursos"
}

// Las pruebas comunes también corren en la JVM de la PC, y el respaldo abre archivos con el SQLite
// incluido. El del Android trae la biblioteca nativa del teléfono, que la PC no carga: se toma la
// de escritorio del mismo release y se le indica a la JVM dónde está.
val sqliteDeEscritorio: Configuration by configurations.creating

// Room genera el código de la base para cada target (D6).
dependencies {
    add("kspAndroid", libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)
    sqliteDeEscritorio("androidx.sqlite:sqlite-bundled-jvm:${libs.versions.sqliteBundled.get()}") {
        isTransitive = false
    }
}

val nativoDeSqlite = tasks.register<Sync>("nativoDeSqlite") {
    val so = System.getProperty("os.name").lowercase()
    val arm = System.getProperty("os.arch") == "aarch64"
    val carpeta = when {
        "win" in so -> "windows_x64"
        "mac" in so -> if (arm) "osx_arm64" else "osx_x64"
        else -> if (arm) "linux_arm64" else "linux_x64"
    }
    from({ zipTree(sqliteDeEscritorio.singleFile) }) {
        include("natives/$carpeta/*")
        eachFile { path = name }
        includeEmptyDirs = false
    }
    into(layout.buildDirectory.dir("sqlite-nativo"))
}

tasks.withType<Test>().configureEach {
    dependsOn(nativoDeSqlite)
    systemProperty("java.library.path", layout.buildDirectory.dir("sqlite-nativo").get().asFile.path)
}
