package org.xtclang.plugin.tasks;

import org.gradle.api.file.Directory;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RelativePath;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.xtclang.plugin.XtcProjectDelegate;

import javax.inject.Inject;
import java.io.File;

import static org.xtclang.plugin.XtcPluginConstants.XDK_JAVATOOLS_ARTIFACT_ID;
import static org.xtclang.plugin.XtcPluginConstants.XDK_VERSION_PATH;
import static org.xtclang.plugin.XtcPluginConstants.XDK_CONFIG_NAME_INCOMING;
import static org.xtclang.plugin.XtcPluginConstants.XDK_CONFIG_NAME_INCOMING_ZIP;
import static org.xtclang.plugin.XtcPluginConstants.XTC_MODULE_FILE_EXTENSION;

@CacheableTask
public class XtcExtractXdkTask extends XtcDefaultTask {
    private static final String ARCHIVE_EXTENSION = "zip";

    @Inject
    public XtcExtractXdkTask(final XtcProjectDelegate project) {
        super(project);
    }

    private static boolean isXdkArchive(final File file) {
        return ARCHIVE_EXTENSION.equals(XtcProjectDelegate.getFileExtension(file));
    }

    @InputFiles
    @PathSensitive(PathSensitivity.ABSOLUTE)
    FileCollection getInputXdkArchive() {
        return project.filesFrom(XDK_CONFIG_NAME_INCOMING_ZIP, XDK_CONFIG_NAME_INCOMING);
    }

    @OutputDirectory
    Provider<Directory> getOutputXtcModules() {
        return project.getXdkContentsDir();
    }

    @TaskAction
    public void extractXdk() {
        start();

        // The task is configured at this point. We should indeed have found a zip archive from some xdkDistributionProvider somewhere.
        final var archives = project
                .filesFrom(true, XDK_CONFIG_NAME_INCOMING_ZIP, XDK_CONFIG_NAME_INCOMING)
                .filter(XtcExtractXdkTask::isXdkArchive);

        if (archives.isEmpty()) {
            project.info("{} Project does NOT depend on the XDK; {} is a nop.", prefix, getName());
            return;
        }

        // If there are no archives, we do not depend on the XDK.
        final var archiveFile = archives.getSingleFile();

        // TODO: Should probably add a configuration xdkJavaToolsProvider.
        project.getProject().copy(config -> {
            project.info("{} CopySpec: XDK archive file dependency: {}", prefix, archiveFile);
            config.from(project.getProject().zipTree(archiveFile));
            config.include(
                    "**/*." + XTC_MODULE_FILE_EXTENSION,
                    "**/" + XDK_JAVATOOLS_ARTIFACT_ID + "*jar",
                    XDK_VERSION_PATH);
            config.eachFile(fileCopyDetails -> fileCopyDetails.setRelativePath(new RelativePath(true, fileCopyDetails.getName())));
            config.setIncludeEmptyDirs(false);
            config.into(getOutputXtcModules());
        });

        project.info("{} Finished unpacking XDK archive: {} -> {}.", prefix, archiveFile.getAbsolutePath(), getOutputXtcModules().get());
    }
}
