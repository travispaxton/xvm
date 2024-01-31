import org.jetbrains.kotlin.gradle.utils.`is`

/*
 * Build file for the common Java utilities classes used by various Java projects in the XDK.
 */

plugins {
    alias(libs.plugins.xdk.build.java)
    alias(libs.plugins.tasktree)
}

val xdkJavaToolsUtilsProvider by configurations.registering {
    description = "Provider configuration of the XVM javatools_utils classes."
    isCanBeResolved = false
    isCanBeConsumed = true
/*    attributes {
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.CLASSES))
    }*/
}

// createProcessResourcesTask
//   sourceset.getProcessResourcesTaskName
val processResources by tasks.existing {
    findProperty("apa")
    outputs.upToDateWhen { false }
    logger.info("WHAT")
    considerNeverUpToDate()
    System.err.println(sourceSets.main.isPresent)
    printTaskDependencies()
    doLast {
        logger.lifecycle("$prefix Data.")
        logger.lifecycle("Say what.")
        printTaskDependencies()
    }
}
