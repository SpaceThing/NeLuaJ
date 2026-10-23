import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.jvm.tasks.Jar
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.compile.JavaCompile

plugins {
    id("com.android.library")
}

abstract class StageLiquidGlassSources : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceDirectory: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun stage() {
        project.delete(outputDirectory.get().asFile)
        project.copy {
            from(sourceDirectory)
            exclude("**/MainActivity.java")
            into(outputDirectory)
        }
    }
}

val stageLiquidGlassSources = tasks.register<StageLiquidGlassSources>("stageLiquidGlassSources") {
    sourceDirectory.set(rootProject.layout.projectDirectory.dir("../LiquidGlass/app/src/main/java"))
    outputDirectory.set(layout.buildDirectory.dir("generated/liquidglass/java"))
}

android {
    namespace = "com.liquidglass.java.miba"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

androidComponents {
    onVariants { variant ->
        variant.sources.java?.addGeneratedSourceDirectory(stageLiquidGlassSources) {
            it.outputDirectory
        }
    }

}

val liquidGlassClassesJar = tasks.register<Jar>("liquidGlassClassesJar") {
    dependsOn("compileReleaseJavaWithJavac")
    archiveFileName.set("liquidglass-classes.jar")
    destinationDirectory.set(layout.buildDirectory.dir("intermediates/liquidglassDex"))
    from(provider {
        tasks.named<JavaCompile>("compileReleaseJavaWithJavac")
            .get().destinationDirectory.get().asFile
    })
    include("com/liquidglass/java/miba/**/*.class")
    exclude("com/liquidglass/java/miba/MainActivity*.class")
    exclude("com/liquidglass/java/miba/R.class", "com/liquidglass/java/miba/R$*.class")
    exclude("com/liquidglass/java/miba/BuildConfig.class")
}

val standaloneDexDirectory = layout.buildDirectory.dir("intermediates/liquidglassDex/d8")
val androidSdkDirectory = androidComponents.sdkComponents.sdkDirectory
val dexLiquidGlass = tasks.register<JavaExec>("dexLiquidGlass") {
    dependsOn(liquidGlassClassesJar)
    mainClass.set("com.android.tools.r8.D8")
    classpath = files(androidSdkDirectory.map {
        it.file("build-tools/${android.buildToolsVersion}/lib/d8.jar")
    })
    val platformJar = androidSdkDirectory.map {
        it.file("platforms/android-37.0/android.jar")
    }
    inputs.file(liquidGlassClassesJar.flatMap { it.archiveFile })
    inputs.file(platformJar)
    inputs.property("minApi", 26)
    outputs.dir(standaloneDexDirectory)
    doFirst {
        val output = standaloneDexDirectory.get().asFile
        output.mkdirs()
        setArgs(listOf(
            "--release", "--min-api", "26",
            "--lib", platformJar.get().asFile.absolutePath,
            "--output", output.absolutePath,
            liquidGlassClassesJar.get().archiveFile.get().asFile.absolutePath,
        ))
    }
}

val exportLiquidGlassDex = tasks.register<Copy>("exportLiquidGlassDex") {
    dependsOn(dexLiquidGlass)
    from(standaloneDexDirectory.map { it.file("classes.dex") })
    into(layout.buildDirectory.dir("bin"))
    rename { "LiquidGlass.dex" }
}
