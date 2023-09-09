package com.github.keepjunitrunning.executor;

import com.github.keepjunitrunning.ResourcesPlugin;
import io.methvin.watcher.DirectoryChangeEvent;
import io.methvin.watcher.DirectoryChangeListener;
import io.methvin.watcher.DirectoryWatcher;
import io.methvin.watcher.hashing.FileHasher;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.security.Permission;
import java.util.Arrays;
import java.util.function.Consumer;

public class TestStarter {
    private static boolean ENABLE_LOGGING = false;

    protected static class ExitException extends SecurityException {
        private static final long serialVersionUID = -2373497274896992856L;
        public final int status;

        public ExitException(int status) {
            super("There is no escape!");
            this.status = status;
        }
    }

    private static class NoExitSecurityManager extends SecurityManager {
        @Override
        public void checkPermission(Permission perm) {
            // allow anything.
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // allow anything.
        }

        @Override
        public void checkExit(int status) {
            super.checkExit(status);
            throw new ExitException(status);
        }
    }

    private static void start(String className, String[] args) {
        try {
            TestStarter.log("loading class:" + className);
            Method meth = Class.forName(className).getMethod("main", String[].class);
            TestStarter.log("invoking method on class:" + className);
            meth.invoke(null, (Object) args); // static method doesn't have an instance
        } catch (ExitException | InvocationTargetException e) {
        } catch (IllegalAccessException | ClassNotFoundException | NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        ENABLE_LOGGING = Boolean.parseBoolean(System.getenv(ResourcesPlugin.ENABLE_LOG_ENVIRONMENT_VARIABLE));
        System.out.println("[KEEP RUNNING] started, logging:" + ENABLE_LOGGING);
        String className = args[args.length - 1];
        File file = new File(args[args.length - 2]);

        file.deleteOnExit();
        Runtime.getRuntime().addShutdownHook(new Thread(file::delete));
        Path path = Paths.get(file.getParent());

        args = Arrays.copyOf(args, args.length - 2);
        Consumer<String[]> solutionToStart = (params) -> start(className, params);

        try {
            System.setSecurityManager(new NoExitSecurityManager());
        } catch (Throwable e) {
            // System.exit() is removed from the "+2" classes
            TestStarter.log("trying alternative solution");
            solutionToStart = (params) -> start(className + "2", params);
        }
        try {
            TestStarter.log("args:" + java.util.Arrays.toString(args));
            TestStarter.log("args.length:" + args.length);
            solutionToStart.accept(args);

            Thread.sleep(50);
            TestStarter.log("path:" + path);
            DirectoryChangeListener directoryChangeListener = getDirectoryChangeListener(solutionToStart, file);
            DirectoryWatcher directoryWatcher = DirectoryWatcher.builder()
                    .path(path)
                    .fileHasher(FileHasher.LAST_MODIFIED_TIME)
                    .listener(directoryChangeListener)
                    .build();
            directoryWatcher.watch();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private static DirectoryChangeListener getDirectoryChangeListener(Consumer<String[]> solutionToStart, File file) {
        return new DirectoryChangeListener() {
            boolean watching = true;
            @Override
            public void onEvent(DirectoryChangeEvent event) throws IOException {
                switch (event.eventType()) {
                    case MODIFY:
                        final Path changed = event.path();
                        log("changed:" + changed.toFile().getAbsolutePath());
                        final String[] params2 = Files.readAllLines(file.toPath(),
                                Charset.defaultCharset()).stream().filter(line -> !line.isEmpty()).toArray(String[]::new);
                        log("testName: " + Arrays.toString(params2));
                        log("testName.length: " + params2.length);
                        solutionToStart.accept(params2);
                        break;
                    case CREATE:
                        break;
                    case DELETE:
                        //Step the watcher when the file is deleted
                        watching = false;
                        break;
                }
            }

            @Override
            public boolean isWatching() {
                return watching;
            }
        };
    }

    public static void log(String message) {
        if (ENABLE_LOGGING) {
            System.out.println(message);
        }
    }
}
