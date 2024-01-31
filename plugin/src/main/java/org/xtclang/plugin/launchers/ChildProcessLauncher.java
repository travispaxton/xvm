package org.xtclang.plugin.launchers;

import org.gradle.api.Project;
import org.gradle.process.ExecResult;
import org.xtclang.plugin.XtcLauncherTaskExtension;
import org.xtclang.plugin.tasks.XtcLauncherTask;

/**
 * Launcher logic that runs the XTC launchers from classes on the classpath.
 */
public class ChildProcessLauncher<E extends XtcLauncherTaskExtension, T extends XtcLauncherTask<E>> extends JavaExecLauncher<E, T> {
    public ChildProcessLauncher(final Project project, final T task) {
        super(project, task);
    }

    @Override
    public ExecResult apply(final CommandLine cmd) {
        throw new UnsupportedOperationException("Implement me!");
/*

        final var builder = resultBuilder(cmd);
        return createExecResult(builder.execResult(project.getProject().javaexec(spec -> {
            redirectIo(builder, spec);
            spec.classpath(javaToolsJar);
            spec.getMainClass().set(cmd.getMainClassName());
            spec.args(cmd.toList());
            spec.jvmArgs(cmd.getJvmArgs());
            spec.setIgnoreExitValue(true);
        })));
    }*/
    }
}
