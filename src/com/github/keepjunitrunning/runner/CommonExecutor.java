package com.github.keepjunitrunning.runner;

import com.github.keepjunitrunning.helper.FileUtil;
import com.github.keepjunitrunning.helper.UIHelper;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionManager;
import com.intellij.execution.JavaTestFrameworkRunnableState;
import com.intellij.execution.configurations.JavaCommandLine;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.testframework.SearchForTestsTask;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

public interface CommonExecutor {

    String UNIT_TEST_STARTER_CLASS_NAME = "com.github.keepjunitrunning.executor.TestStarter";
    Key<Boolean> KEEP_RUNNER_KEY = new Key<>("ResourcesPlugin.KEEP_RUNNER_DATA_KEY");

    default boolean doPreExecute(final @NotNull RunProfileState state, final @NotNull ExecutionEnvironment env) throws ExecutionException {

        try {
            Project project = env.getProject();
            File tempFile = FileUtil.getTempFile(project);


            boolean fileExists = tempFile.exists() && !FileUtil.FIRST_LOAD;
            FileUtil.FIRST_LOAD = false;

            RunConfigurationBase runProfile = (RunConfigurationBase) env.getRunProfile();
            runProfile.setAllowRunningInParallel(true);

            JavaParameters javaParameters = ((JavaCommandLine) state).getJavaParameters();
            javaParameters.getClassPath().addFirst(PathManager.getJarPathForClass(UIHelper.class));

            //write params to file before appending to the list
            FileUtil.writeToTempFile(project, javaParameters.getProgramParametersList().getArray());

            javaParameters.getProgramParametersList().add(tempFile.getAbsolutePath());
            javaParameters.getProgramParametersList().add(javaParameters.getMainClass());
            javaParameters.setMainClass(UNIT_TEST_STARTER_CLASS_NAME);


            return fileExists;
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    default void doPostExecute(final @NotNull RunProfileState state,
                               final @NotNull Project project,
                               final RunContentDescriptor runContentDescriptor,
                               boolean wasRun) {
        if (wasRun) {
            if (state instanceof JavaTestFrameworkRunnableState) {
                try {
                    SearchForTestsTask searchingForTestsTask = ((JavaTestFrameworkRunnableState) state).createSearchingForTestsTask();
                    if (searchingForTestsTask != null) {
                        searchingForTestsTask.startSearch();
                        try {
                            ProcessHandler[] processes = ExecutionManager.getInstance(project).getRunningProcesses();
                            Optional<ProcessHandler> anyKeepProcessStarted = Arrays.stream(processes).filter(p -> Boolean.TRUE.equals(p.getCopyableUserData(KEEP_RUNNER_KEY))).findAny();
                            anyKeepProcessStarted.ifPresent(ps -> ps.addProcessListener(new ProcessAdapter() {
                                @Override
                                public void processTerminated(@NotNull ProcessEvent event) {
                                    ps.removeProcessListener(this);
                                    searchingForTestsTask.ensureFinished();
                                    super.processTerminated(event);
                                }
                            }));

                        } catch (Throwable e) {
                            e.printStackTrace();
                        }
                    }
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            }
        }

        if (runContentDescriptor == null) {
            return;
        }

        final ProcessHandler processHandler = runContentDescriptor.getProcessHandler();

        if (processHandler != null) {
            processHandler.putCopyableUserData(KEEP_RUNNER_KEY, true);
            processHandler.addProcessListener(new ProcessAdapter() {
                @Override
                public void processTerminated(@NotNull ProcessEvent event) {
                    FileUtil.getTempFile(project).delete();
                }
            });
        }
    }
}

