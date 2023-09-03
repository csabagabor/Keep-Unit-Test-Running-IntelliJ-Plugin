package com.github.keepjunitrunning.action;

import com.github.keepjunitrunning.helper.FileUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;

public class ResetPluginAction extends AnAction {

    @Override
    public void actionPerformed(final AnActionEvent event) {
        final Project project = PlatformDataKeys.PROJECT.getData(event.getDataContext());
        FileUtil.getTempFile(project).delete();
    }

    @Override
    public String toString() {
        return "Reset Keep Unit Test Running";
    }
}
