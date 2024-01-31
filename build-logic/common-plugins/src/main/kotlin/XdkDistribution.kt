import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.internal.os.OperatingSystem

class XdkDistribution(project: Project): XdkProjectBuildLogic(project) {
    companion object {
        const val DISTRIBUTION_TASK_GROUP = "distribution"
        const val MAKENSIS = "makensis"

        private const val BUILD_NUMBER = "BUILD_NUMBER"
        private const val CI = "CI"
        private const val OS_MAC = "macosx"
        private const val OS_WINDOWS = "windows"
        private const val OS_LINUX = "linux"

        private val currentOs = OperatingSystem.current()
        private val isCiEnabled = System.getenv(CI) == "true"

        val distributionTasks = listOfNotNull("distTar", "distZip", "distExe")
        val supportedOsNames = listOfNotNull(OS_MAC, OS_LINUX, OS_WINDOWS)
        val launcherSymlinkNames = listOfNotNull("xcc", "xec", "xtc")

        fun resolveLauncherFile(localDistDir: Provider<Directory>, osName: String = getCurrentOsName()): RegularFile {
            return localDistDir.get().file("libexec/bin/${getLauncherFileName(osName)}")
        }

        fun getCurrentOsName(): String {
            if (currentOs.isMacOsX) {
                return OS_MAC
            } else if (currentOs.isLinux) {
                return OS_LINUX
            } else if (currentOs.isWindows) {
                return OS_WINDOWS
            }
            throw UnsupportedOperationException("Unknown OS: $currentOs")
        }

        private fun getLauncherFileName(osName: String = getCurrentOsName()): String {
            return when (osName) {
                OS_MAC -> "macos_launcher"
                OS_LINUX -> "linux_launcher"
                OS_WINDOWS -> "windows_launcher.exe"
                else -> throw UnsupportedOperationException("Unknown OS: $osName ($currentOs)")
            }
        }
     }

    init {
        logger.info("""
            $prefix Configuring XVM distribution: '$this'
            $prefix   Name        : '$distributionName'
            $prefix   Version     : '$distributionVersion'
            $prefix   Current OS  : '$currentOs'
            $prefix   Environment:
            $prefix       CI             : '$isCiEnabled' (CI property can be overwritten)
            $prefix       GITHUB_ACTIONS : '${System.getenv("GITHUB_ACTIONS") ?: "[not set]"}'
        """.trimIndent())
    }

    val distributionName: String get() = project.name // Default: "xdk"

    val distributionVersion: String get() = buildString {
        append(project.version)
        if (isCiEnabled) {
            val buildNumber = System.getenv(BUILD_NUMBER) ?: ""
            val gitCommitHash = project.executeCommand("git", "rev-parse", "HEAD")
            if (buildNumber.isNotEmpty() || gitCommitHash.isNotEmpty()) {
                logger.warn("This is a CI run, BUILD_NUMBER and git hash must both be available: (BUILD_NUMBER='$buildNumber', commit='$gitCommitHash')")
                return@buildString
            }
            append("-ci-$buildNumber+$gitCommitHash")
        }
    }

    fun getLocalDistDir(osName: String = getCurrentOsName()): Provider<Directory> {
        return project.compositeRootBuildDirectory.dir("dist/$osName")
    }

    fun shouldCreateWindowsDistribution(): Boolean {
        val runDistExe = project.getXdkPropertyBoolean("org.xtclang.install.distExe", false)
        if (runDistExe) {
            logger.info("$prefix 'distExe' task is enabled; will attempt to build Windows installer.")
            if (XdkBuildLogic.findExecutableOnPath(MAKENSIS) == null) {
                throw project.buildException("Illegal configuration; project is set to weave a Windows installer, but '$MAKENSIS' is not on the PATH.")
            }
            return true
        }
        logger.warn("$prefix 'distExe' is disabled for building distributions. Only 'tar.gz' and 'zip' are allowed.")
        return false
    }

    override fun toString(): String {
        return "$distributionName-$distributionVersion"
    }
}
