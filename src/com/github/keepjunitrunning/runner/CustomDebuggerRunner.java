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

        final Promise<RunContentDescriptor> runContentDescriptorPromise;
        if (wasRun) {
            // Create a no-op promise, since we don't actually want to have IntelliJ start up a new process.
            // Instead, we have already updated the default.txt file in doPreExecute(), which triggers the next run.
            runContentDescriptorPromise = new AsyncPromise<>();
        } else {
            runContentDescriptorPromise = super.doExecuteAsync(state, env);
        }
        Promise<RunContentDescriptor> thenAsync = runContentDescriptorPromise.thenAsync(runContentDescriptorResult -> {
            RunContentDescriptor runContentDescriptor = wasRun ? null : runContentDescriptorResult;
            doPostExecute(state, env.getProject(), runContentDescriptor, wasRun);
            AsyncPromise<RunContentDescriptor> finalResultPromise = new AsyncPromise<>();
            finalResultPromise.setResult(runContentDescriptor);
            return finalResultPromise;
        });
        if (wasRun) {
            ((AsyncPromise)runContentDescriptorPromise).setResult(null);
        }
        return thenAsync;
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
