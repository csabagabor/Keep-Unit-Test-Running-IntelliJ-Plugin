package com.github.keepjunitrunning.helper;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtilRt;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Paths;

public class FileUtil {
    private static final Logger log = Logger.getInstance(FileUtil.class);
    public static final String PLUGIN_TEMP_DIR = "keeprunning";
    public static final String PLUGIN_FILE_NAME = "default.txt";
    public static boolean FIRST_LOAD = true;

    public static File getTempFile(Project project) {
        //append hash of project name to the created temp directory so that the plugin can be used to start tests in different projects at the same time
        int hashOfProjectName = Math.abs(project.getName().hashCode());
        return Paths.get(getTempDirPath() + File.separator + (PLUGIN_TEMP_DIR + hashOfProjectName) + File.separator + PLUGIN_FILE_NAME).toFile();
    }

    public static void writeToTempFile(Project project, @NotNull String[] linesToWrite) throws UnsupportedEncodingException,
            FileNotFoundException {

        writeToFile(project, getTempDirPath(), linesToWrite);
    }

    private static String getTempDirPath() {
        return FileUtilRt.getTempDirectory();
    }

    private static void writeToFile(Project project, @NotNull String parentDirectoryName, @NotNull String[] linesToWrite) throws UnsupportedEncodingException,
            FileNotFoundException {

        int hashOfProjectName = Math.abs(project.getName().hashCode());
        File parent = new File(parentDirectoryName, (PLUGIN_TEMP_DIR + hashOfProjectName));
        parent.mkdirs();

        File file = new File(parent, PLUGIN_FILE_NAME);
        file.deleteOnExit();
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file),
                    "UTF-8"));

            for (String line : linesToWrite) {
                writer.println(line);
            }
            writer.println("");
        } catch (UnsupportedEncodingException | FileNotFoundException e) {
            log.error(e);
            throw e;
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }
}
