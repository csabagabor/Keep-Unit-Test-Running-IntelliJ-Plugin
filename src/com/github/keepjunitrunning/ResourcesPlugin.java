package com.github.keepjunitrunning;

import com.intellij.openapi.util.IconLoader;

import javax.swing.*;

public abstract class ResourcesPlugin {

    public static final Icon RUN_13 = IconLoader.getIcon("/icons/run16.svg");

    public static final Icon RUN = IconLoader.getIcon("/icons/run16.svg");

    public static final Icon DEBUG_13 = IconLoader.getIcon("/icons/debug16.svg");

    public static final Icon DEBUG = IconLoader.getIcon("/icons/debug16.svg");

    public static final String ENABLE_LOG_ENVIRONMENT_VARIABLE = "keep_running_env";

    public static final String KEEP_RUNNER_DATA_KEY = "keep_runner";
}
