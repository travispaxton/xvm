import XdkBuildLogic.Companion.XDK_ARTIFACT_NAME_MACK_DIR
import org.gradle.api.attributes.Category.CATEGORY_ATTRIBUTE
import org.gradle.api.attributes.Category.LIBRARY
import org.gradle.api.attributes.LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE
import org.xtclang.plugin.tasks.XtcCompileTask

/*
 * Build file for the Ecstasy core library of the XDK.
 *
 * This project builds the ecstasy.xtc anb mack.xtc core library files.
 */

plugins {
    id("org.xtclang.build.xdk.versioning")
    alias(libs.plugins.xtc)
}

val xdkTurtle by configurations.registering {
    isCanBeResolved = true
    isCanBeConsumed = false
    attributes {
        attribute(CATEGORY_ATTRIBUTE, objects.named(LIBRARY))
        attribute(LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(XDK_ARTIFACT_NAME_MACK_DIR))
    }
}

dependencies {
    // TODO: Find out why xdkJavaTools is not an unstable API, while xdkTurtle and xdkUnicode are.
    xdkJavaTools(libs.javatools)
    // A dependency declaration like this works equally well if we are working with an included build/project or with an artifact. This is exactly what we want.
    @Suppress("UnstableApiUsage")
    xdkTurtle(libs.javatools.turtle)
}

val compileXtc by tasks.existing(XtcCompileTask::class) {
    outputFilename("mack.xtc" to "javatools_turtle.xtc")
}

/**
 * Set up source sets. The XTC main source set needs the turtle module as part of the compile, i.e. "mack.x", as it
 * cannot build standalone, for bootstrapping reasons. It would really just be simpler to move mack.x to live beside
 * ecstasy.x, but right now we want to transition to the Gradle build logic without changing semantics form the old
 * world. This shows the flexibility of being Source Set aware, through.
 */
sourceSets {
    main {
        xtc {
            // mack.x is in a different project, and does not build on its own, hence we add it to the lib_ecstasy source set instead.
            srcDir(xdkTurtle)
        }
    }
}

// TODO Add resource processing for unicode