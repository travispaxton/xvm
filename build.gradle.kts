/*
 * Main build file for the XVM project, producing the XDK.
 */

import java.io.File

group   = "org.xvm"
version = "0.4.3"

plugins {
    idea
}

allprojects {
    apply(plugin = "idea")
    idea {
        // TODO: if you want parts of paths in resulting files (*.iml, etc.) to be replaced by variables (Files)
        //   pathVariables GRADLE_HOME: file('~/cool-software/gradle')

        module {
            setOutputDir(file("build/classes/main"));
            setTestOutputDir(file("build/classes/test"))
        }
    }

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
        // Add -Xlint:all, to make the command line build show exactly the same warnings as the problems tab in the IDE.
        options.compilerArgs.add("-Xlint:all")
        // TODO: Work in progress. Will be completed or left out of this MR.
//        javaCompiler.set(javaToolchains.compilerFor {
//            languageVersion.set(JavaLanguageVersion.of(17))
//        })
    }

//    tasks.withType<JavaExec>().configureEach {
//        javaLauncher.set(javaToolchains.launcherFor {
//            languageVersion.set(JavaLanguageVersion.of(17))
//        })
//    }
}

/**
 * Regarding build directory, this is said:
 *   When the project is configured via a build tool like Gradle or Maven, IDEA will change the compile output directory to match that used by those tools, such as target or build
 *   owever, it may not necessarily do this for all build tools. It'll depend on the level of support IDEA provides for a particular build tool. You can manually modify the output
 *   directory used in Project Structure (Ctrl+Shift+Alt+S or ⌘;). There is a project wide setting in Project Settings > Project and then a setting for each module in
 *   Project Settings > Modules > "Paths" tab.
 *
 *   I had the same problem with IntelliJ 2021.1. In my case the Project Settings > Modules > Paths was correctly set, yet it still duplicated everything from the \target to \out.
 *   In addition to that it could also not detect any of my Unit Tests even when I right-click directly on the Unit Test.
 *   What did work for me was that I had to close IntelliJ and manually delete the .idea directory and re-import the entire project.
 */

// TODO: Use IDEA project plugin to generate project.
//   1) Set buildDirs to mirror the Gradle command line ones.
//   2) Import code style standard from Gene
//   3) Can we tag the existing config as an IDEA based one, if we generate the project with 1)? Hopefully. No config required.
//   4) Bump default memory for shared build task in the .idea configs as well?
//
// TODO: Add a script: generate / check if exists (and then sanity check) idea project, and then run openIdea

// TODO: Fix warnings.

println(subprojects.forEach { p ->
    println("name "+ p.name)
    println("projectDir" + p.projectDir)
    println("buildDir " + p.buildDir)
    println("componenents: "+ p.components)
    println("defaulttasks: " + p.defaultTasks)
    println("tasks " + p.tasks)
})

tasks.register("build") {
    group = "Build"
    dependsOn(project("xdk:").tasks["build"])
}

// https://github.com/gradle/gradle/blob/master/subprojects/ide/src/main/java/org/gradle/plugins/ide/idea/IdeaPlugin.java

/*
   We could also create a custom createIdea task like below, and also extend it
   to install stuff like code standard, runConfigurations without having to set them
   up manually and so on.

   Stuff needed to configure:
      -cp xvm.javatools.test (and other apps)
      -ea -Xms1024M -Xmx1024G -Dxvm.parallelism=1
      org.xvm.runtime.TestConnector src/main/x/TestSimple.x
      Before launch: Run Gradle task 'xvm.javatools: build'

.idea/compiler.xml:

(should add:
  --enable-preview
  --add-modules jdk.incubator.concurrent
  (also maybe toggle for 19 support?)

<?xml version="1.0" encoding="UTF-8"?>
<project version="4">
  <component name="CompilerConfiguration">
    <option name="BUILD_PROCESS_HEAP_SIZE" value="1024" />
    <annotationProcessing>
      <profile default="true" name="Default" enabled="true" />
    </annotationProcessing>
    <bytecodeTargetLevel target="17">
      <module name="xvm.main" target="19" />
      <module name="xvm.test" target="19" />
    </bytecodeTargetLevel>
  </component>
  <component name="JavacSettings">
    <option name="ADDITIONAL_OPTIONS_STRING" value="-encoding UTF-8 -Xlint:all --enable-preview --add-modules jdk.incubator.concurrent" />
  </component>
</project>%

   // create IDEA run configurations from Gradle JavaExec tasks
task createRunConfigurations {
    def runConfigurationsDir = new File(".idea/runConfigurations")
    runConfigurationsDir.mkdirs()

    tasks.withType(JavaExec).each { task ->
        def taskName = task.name
        def mainClass = task.main
        def props = task.systemProperties.collect { k, v -> "-D$k=$v" }.join(' ')
        def args = task.args.join(" ")

        def writer = new FileWriter(new File(runConfigurationsDir, "${taskName}.xml"))
        def xml = new MarkupBuilder(writer)

        xml.component(name: "ProjectRunConfigurationManager") {
            configuration(default: 'false', name: taskName, type: "Application", factoryName: "Application", singleton: "true") {
                option(name: 'MAIN_CLASS_NAME', value: mainClass)
                option(name: 'VM_PARAMETERS', value: props)
                option(name: 'PROGRAM_PARAMETERS', value: args)
                option(name: 'WORKING_DIRECTORY', value: 'file://$PROJECT_DIR$')
                module(name: 'module-name')
            }
        }
    }
}

jarRepositories.xml (not needed, auto generates)

<?xml version="1.0" encoding="UTF-8"?>
<project version="4">
  <component name="RemoteRepositoriesConfiguration">
    <remote-repository>
      <option name="id" value="central" />
      <option name="name" value="Maven Central repository" />
      <option name="url" value="https://repo1.maven.org/maven2" />
    </remote-repository>
    <remote-repository>
      <option name="id" value="jboss.community" />
      <option name="name" value="JBoss Community repository" />
      <option name="url" value="https://repository.jboss.org/nexus/content/repositories/public/" />
    </remote-repository>
    <remote-repository>
      <option name="id" value="MavenRepo" />
      <option name="name" value="MavenRepo" />
      <option name="url" value="https://repo.maven.apache.org/maven2/" />
    </remote-repository>
  </component>
</project>%

gradle.xml (likely auto generated after import)
<?xml version="1.0" encoding="UTF-8"?>
<project version="4">
  <component name="GradleMigrationSettings" migrationVersion="1" />
  <component name="GradleSettings">
    <option name="linkedExternalProjectsSettings">
      <GradleProjectSettings>
        <option name="delegatedBuild" value="false" />
        <option name="testRunner" value="PLATFORM" />
        <option name="distributionType" value="DEFAULT_WRAPPED" />
        <option name="externalProjectPath" value="$PROJECT_DIR$" />
        <option name="gradleJvm" value="#JAVA_HOME" />
        <option name="modules">
          <set>
            <option value="$PROJECT_DIR$" />
            <option value="$PROJECT_DIR$/javatools" />
            <option value="$PROJECT_DIR$/javatools_bridge" />
            <option value="$PROJECT_DIR$/javatools_launcher" />
            <option value="$PROJECT_DIR$/javatools_turtle" />
            <option value="$PROJECT_DIR$/javatools_unicode" />
            <option value="$PROJECT_DIR$/javatools_utils" />
            <option value="$PROJECT_DIR$/lib_aggregate" />
            <option value="$PROJECT_DIR$/lib_collections" />
            <option value="$PROJECT_DIR$/lib_crypto" />
            <option value="$PROJECT_DIR$/lib_ecstasy" />
            <option value="$PROJECT_DIR$/lib_imdb" />
            <option value="$PROJECT_DIR$/lib_json" />
            <option value="$PROJECT_DIR$/lib_jsondb" />
            <option value="$PROJECT_DIR$/lib_net" />
            <option value="$PROJECT_DIR$/lib_oodb" />
            <option value="$PROJECT_DIR$/lib_web" />
            <option value="$PROJECT_DIR$/lib_xenia" />
            <option value="$PROJECT_DIR$/manualTests" />
            <option value="$PROJECT_DIR$/xdk" />
          </set>
        </option>
      </GradleProjectSettings>
    </option>
  </component>
</project>%

codeStyles/codeStyleConfig.xml
<component name="ProjectCodeStyleConfiguration">
  <state>
    <option name="PREFERRED_PROJECT_CODE_STYLE" value="xvm" />
  </state>
</component>%

TODO: Tokenizer from bin.



 ~/src/xtclang/xvm/.idea  gradle-intellij-unification ⇡2 *4 !1 ?1  cat encodings.xml                                                                                                                                                                                                                                                                  ✔    17:35:47
<?xml version="1.0" encoding="UTF-8"?>
<project version="4">
  <component name="Encoding">
    <file url="file://$PROJECT_DIR$/license/ccla.txt" charset="ISO-8859-1" />
  </component>
</project>%


 */

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
