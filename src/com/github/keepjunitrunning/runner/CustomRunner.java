package com.github.keepjunitrunning.runner;

import com.github.keepjunitrunning.executor.CustomRunnerExecutor;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.impl.DefaultJavaProgramRunner;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.ui.RunContentDescriptor;
import org.jetbrains.annotations.NotNull;


public class CustomRunner extends DefaultJavaProgramRunner implements CommonExecutor {

    @NotNull
    public String getRunnerId() {
        return CustomRunnerExecutor.WITH_PARALLEL_RUNNER;
    }

    @Override
    protected RunContentDescriptor doExecute(final @NotNull RunProfileState state, final @NotNull ExecutionEnvironment env) throws ExecutionException {

        boolean wasRun = doPreExecute(state, env);
        RunContentDescriptor runContentDescriptor = wasRun ? null : super.doExecute(state, env);
        doPostExecute(state, env.getProject(), runContentDescriptor, wasRun);

        return runContentDescriptor;
    }

    @Override
    public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
        return executorId.equals(CustomRunnerExecutor.WITH_PARALLEL_RUNNER);
    }
}
