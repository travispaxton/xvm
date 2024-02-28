/*
 * Build file for the Unicode tools portion of the XDK.
 *
 * Technically, this only needs to be built and run when new versions of the Unicode standard are
 * released, and when that occurs, the code in Char.x also has to be updated (to match the .dat file
 * data) using the values in the *.txt files that are output by running this.
 */

import de.undercouch.gradle.tasks.download.Download

plugins {
    alias(libs.plugins.xdk.build.java)
    alias(libs.plugins.download)
    alias(libs.plugins.tasktree)
}

dependencies {
    implementation(libs.bundles.unicode)
    implementation(libs.javatools.utils)
}

val xtcUnicodeProducer by configurations.registering {
    isCanBeResolved = false
    isCanBeConsumed = true
    outgoing.artifact(tasks.processResources)
    attributes {
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named("unicodeDir"))
    }
}

val unicodeUcdUrl = "https://unicode.org/Public/UCD/latest/ucdxml/ucd.all.flat.zip"
//val processedResourcesDir = tasks.processResources.get().outputs.files.singleFile

/**
 * Type safe "jar" task accessor.
 */
val jar by tasks.existing(Jar::class)

/**
 * Download the ucd zip file from the unicode site, if it does not exist.
 */
val downloadUcdFlatZip by tasks.registering(Download::class) {
    onlyIf {
        getXdkPropertyBoolean("org.xtclang.unicode.rebuild", false) // TODO inputproperty
    }
    src(unicodeUcdUrl)
    overwrite(false)
    onlyIfModified(true)
    quiet(false)
    dest(project.mkdir(project.layout.buildDirectory.dir("ucd")))
}

/**
 * Build unicode tables, and put them under the build directory.
 *
 * For a normal run, the unicode resources are already copied to the build directory
 * by the processResources task, which as part of any default Java Plugin build lifecycle,
 * will copy the src/<sourceSet>/resources directory to build/resources/<sourceSet>
 * In that case, when resolveUnicodeTables is set to false, the only thing this task does
 * is add the processResources outputs as its own outputs. If it's true, we will overwrite
 * those resources to the build folder, and optionally, copy them to replace the source
 * folder resources.
 *
 * We never execute this task explicitly, but we do declare a consumable coniguration that
 * contains the output of this task, forcing it to run (and maybe rebuild unicode files) if
 * anyone wants to resolve the config. The lib_ecstasy project adds this configuration to
 * its incoming resources, which means that lib_ecstasy will include them in the ecstasy.xtc
 * module. All we need to do is add the configuration as a resource for lib_ecstasy.
 */

// processResources:
//   If we aren't rebuilding unicode, they just copy the resources to build and add them to the xtcUnicodeProducer config.
//   Otherwise, the
private val rebuildUnicode =  getXdkPropertyBoolean("org.xtclang.unicode.rebuild", false)
private val resourceOutputs = provider {
    sourceSets.main.get().output.resourcesDir
}

val processUnicodeTables by tasks.registering {
    outputs.dir(resourceOutputs)
    dependsOn(jar)

    val unicodeJar = jar.map { it.archiveFile.get().asFile }
    val unicodeUcdZip = downloadUcdFlatZip.map { it.outputs.files.singleFile }
    inputs.file(unicodeJar)
    inputs.file(unicodeUcdZip)
    // TODO: Depend on source.

    doLast {
        if (rebuildUnicode) {
            logger.lifecycle("$prefix Rebuilding unicode tables...")
            val localUcdZip = unicodeUcdZip.get()
            logger.lifecycle("$prefix Downloaded unicode file: ${localUcdZip.absolutePath}")
            javaexec {
                mainClass = "org.xvm.tool.BuildUnicodeTables"
                classpath(configurations.runtimeClasspath)
                classpath(unicodeJar)
                args(localUcdZip.absolutePath, File(resourceOutputs.get(), "ecstasy/text"))
            }
        }
        printTaskInputs()
        printTaskOutputs()
        printTaskDependencies()
    }
}

// We have correct outputs by extending the existing Jave processResources task.
val processResources by tasks.existing {
    inputs.properties("rebuildUnicode" to rebuildUnicode)
    if (rebuildUnicode) {
        dependsOn(processUnicodeTables)
    }
}
