//
// Taken from the original Cucumber CLI source.
// Removed the System.exit() call from within the main method.
//

package io.cucumber.core.cli;

import io.cucumber.core.options.CommandlineOptionsParser;
import io.cucumber.core.options.CucumberProperties;
import io.cucumber.core.options.CucumberPropertiesParser;
import io.cucumber.core.options.RuntimeOptions;
import io.cucumber.core.runtime.Runtime;


public class Main2 {

    public static void main(String... argv) {
        run(argv, Thread.currentThread().getContextClassLoader());
    }

    public static byte run(String[] argv, ClassLoader classLoader) {
        RuntimeOptions propertiesFileOptions = (new CucumberPropertiesParser()).parse(CucumberProperties.fromPropertiesFile()).build();
        RuntimeOptions environmentOptions = (new CucumberPropertiesParser()).parse(CucumberProperties.fromEnvironment()).build(propertiesFileOptions);
        RuntimeOptions systemOptions = (new CucumberPropertiesParser()).parse(CucumberProperties.fromSystemProperties()).build(environmentOptions);
        RuntimeOptions runtimeOptions = (new CommandlineOptionsParser()).parse(argv).addDefaultFormatterIfAbsent().addDefaultSummaryPrinterIfAbsent().build(systemOptions);
        Runtime runtime = Runtime.builder().withRuntimeOptions(runtimeOptions).withClassLoader(classLoader).build();
        runtime.run();
        return runtime.exitStatus();
    }
}
