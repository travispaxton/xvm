rootProject.name = "xvm"

// Version catalog, to avoid version skew between artefacts that are used by several subprojects.
dependencyResolutionManagement {
    versionCatalogs {
        create("xvmlibs") {
            library("junit", "junit:junit:4.13.2")
            library("javax-activation", "javax.activation:activation:1.1.1")
            library("jakarta-xml-bind-api", "jakarta.xml.bind:jakarta.xml.bind-api:2.3.2")
            library("jaxb-runtime", "org.glassfish.jaxb:jaxb-runtime:2.3.2")
        }
    }
}

include(":javatools_utils")     // produces javatools_utils.jar for org.xvm.utils package
include(":javatools_unicode")   // produces data files -> :lib_ecstasy/resources, only on request
include(":javatools")           // produces javatools.jar
include(":javatools_turtle")    // produces *only* a source zip file (no .xtc), and only on request
include(":javatools_bridge")    // produces *only* a source zip file (no .xtc), and only on request
include(":javatools_launcher")  // produces native executables (Win, Mac, Linux), only on request
include(":lib_ecstasy")         // produces *only* a source zip file (no .xtc), and only on request
include(":lib_aggregate")       // produces aggregate.xtc
include(":lib_collections")     // produces collections.xtc
include(":lib_crypto")          // produces crypto.xtc
include(":lib_net")             // produces net.xtc
include(":lib_json")            // produces json.xtc
include(":lib_oodb")            // produces oodb.xtc
include(":lib_imdb")            // produces imdb.xtc
include(":lib_jsondb")          // produces jsondb.xtc
include(":lib_web")             // produces web.xtc
include(":lib_xenia")           // produces xenia.xtc

// TODO(":wiki")
include(":xdk")      // builds the above modules (ecstasy.xtc, javatools_bridge.xtc, json.xtc, etc.)
// drags in Java libraries (javatools_utils, javatools), native launchers, wiki, etc.

include(":manualTests") // temporary; allowing gradle test execution
