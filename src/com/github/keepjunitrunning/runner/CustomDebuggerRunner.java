package com.github.keepjunitrunning.runner;

import com.github.keepjunitrunning.executor.CustomDebuggerExecutor;
import com.intellij.debugger.impl.GenericDebuggerRunner;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.target.TargetEnvironmentAwareRunProfileState;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.concurrency.AsyncPromise;
import org.jetbrains.concurrency.Promise;

public class CustomDebuggerRunner extends GenericDebuggerRunner implements CommonExecutor {
    private static final Logger LOG = Logger.getInstance(CustomDebuggerRunner.class);

    @NotNull
    public String getRunnerId() {
        return CustomDebuggerExecutor.WITH_PARALLEL_RUNNER;
    }


    @Override
    protected @NotNull Promise<RunContentDescriptor> doExecuteAsync(@NotNull TargetEnvironmentAwareRunProfileState state, @NotNull ExecutionEnvironment env) throws ExecutionException {
        boolean wasRun = doPreExecute(state, env);

        Promise<RunContentDescriptor> runContentDescriptorPromise = super.doExecuteAsync(state, env);

        return runContentDescriptorPromise.thenAsync(runContentDescriptorResult -> {
            RunContentDescriptor runContentDescriptor = wasRun ? null : runContentDescriptorResult;
            doPostExecute(state, env.getProject(), runContentDescriptor, wasRun);
            AsyncPromise<RunContentDescriptor> finalResultPromise = new AsyncPromise<>();
            finalResultPromise.setResult(runContentDescriptor);
            return finalResultPromise;
        });
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
        return executorId.equals(CustomDebuggerExecutor.WITH_PARALLEL_RUNNER);
    }
}
