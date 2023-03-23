/*
 * Main build file for the XVM project, producing the XDK.
 */

group   = "org.xvm"
version = "0.4.3"

allprojects {
    configurations.all {
        resolutionStrategy.dependencySubstitution {
            substitute(module("org.xtclang.xvm:javatools_utils"  )).using(project(":javatools_utils"))
            substitute(module("org.xtclang.xvm:javatools_unicode")).using(project(":javatools_unicode"))
            substitute(module("org.xtclang.xvm:javatools"        )).using(project(":javatools"))
        }
    }

    repositories {
        mavenCentral {
            content {
                excludeGroup("org.xtclang.xvm")
            }
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
    }
}

tasks.register("build") {
    group       = "Build"
    description = "Build all projects"

    dependsOn(project("xdk:").tasks["build"])
}

/*
 * gitClean:
 *
 * This is a task to clean up all files in the source tree that are not under source control,
 * with the exception of individual IDE configurations.
 *
 * This task is a dry run; it will only list what it would like to delete and it's recommended
 * to run this first as a safety measure. To actually perform the deletions, use the task
 */
tasks.register<Exec>("gitClean") {
    group = "other"
    description = "Does a dry run of git clean, recursively from the repo root, but only prints what would be removed. The .idea configuration directory is exempt."
    commandLine("git", "clean", "-nfxd", "-e", ".idea")
}

/*
 * gitCleanLive:
 *
 * This is a task to clean up all files in the source tree that are not under source control,
 * with the exception of individual IDE configurations.
 *
 * WARNING: Use with caution, and to be safe, it is recommended that you run the task 'gitClean'
 * first, to get a list of the files and directories that will be purged.
 */
tasks.register<Exec>("gitCleanLive") {
    group = "other"
    description = "Deletes all files not under source control, recursively from the repo root. The .idea configuration directory is exempt."
    commandLine("git", "clean", "-fxd", "-e", ".idea")
}
