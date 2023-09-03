package com.github.keepjunitrunning.executor;

import com.github.keepjunitrunning.ResourcesPlugin;
import com.sun.nio.file.SensitivityWatchEventModifier;

import java.io.File;
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
            try (final WatchService watchService = FileSystems.getDefault().newWatchService()) {
                path.register(watchService, new WatchEvent.Kind[]{StandardWatchEventKinds.ENTRY_MODIFY}, SensitivityWatchEventModifier.HIGH);
                while (true) {
                    final WatchKey wk = watchService.take();

                    Thread.sleep(50);
                    for (WatchEvent<?> event : wk.pollEvents()) {
                        //we only register "ENTRY_MODIFY" so the context is always a Path.
                        final Path changed = (Path) event.context();
                        TestStarter.log("changed" + changed.toFile().getAbsolutePath());
                        String[] params = Files.readAllLines(file.toPath(),
                                Charset.defaultCharset()).stream()
                                .filter(line -> !line.isEmpty())
                                .toArray(String[]::new);
                        TestStarter.log("testName: " + java.util.Arrays.toString(params));
                        TestStarter.log("testName.length: " + params.length);

                        solutionToStart.accept(params);
                    }
                    // reset the key
                    boolean valid = wk.reset();
                    if (!valid) {
                        TestStarter.log("Key has been unregisterede");
                    }
                }
            }

        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static void log(String message) {
        if (ENABLE_LOGGING) {
            System.out.println(message);
        }
    }
}
