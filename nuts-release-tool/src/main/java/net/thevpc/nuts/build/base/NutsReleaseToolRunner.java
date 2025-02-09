package net.thevpc.nuts.build.base;

import net.thevpc.nuts.NApp;
import net.thevpc.nuts.NOut;
import net.thevpc.nuts.build.util.AbstractRunner;
import net.thevpc.nuts.cmdline.NCmdLine;
import net.thevpc.nuts.cmdline.NCmdLineConfigurable;
import net.thevpc.nuts.spi.NScopeType;
import net.thevpc.nuts.time.NChronometer;
import net.thevpc.nuts.util.NMsg;

public class NutsReleaseToolRunner {

    private AbstractRunner[] runners;

    public NutsReleaseToolRunner(AbstractRunner... runners) {
        this.runners = runners;
    }

    private void configure(NCmdLine args) {
        for (AbstractRunner runner : runners) {
            runner.configureBeforeOptions(args);
        }
        NCmdLineConfigurable.configure(cmdLine -> {
            for (AbstractRunner runner : runners) {
                if (runner.configureFirst(cmdLine)) {
                    return true;
                }
            }
            return false;
        }, false, args.toStringArray(), "nuts-builder");
        for (AbstractRunner runner : runners) {
            runner.configureAfterOptions();
        }
    }

    public NutsBuildRunnerContext context() {
        NutsBuildRunnerContext s = (NutsBuildRunnerContext) NApp.of().getProperty(NutsBuildRunnerContext.class.getName(), NScopeType.SESSION).orNull();
        if (s == null) {
            s = new NutsBuildRunnerContext();
            NApp.of().setProperty(NutsBuildRunnerContext.class.getName(), NScopeType.SESSION, s);
        }
        return s;
    }


    public void run(NCmdLine args) {
        NChronometer chrono = NChronometer.startNow();
        NOut.println("##nuts-release-tool## started");
        configure(args);
        for (AbstractRunner runner : runners) {
            runner.run();
        }
        NOut.println(NMsg.ofC("##nuts-release-tool## finished in %s",chrono.stop()));
    }



}
