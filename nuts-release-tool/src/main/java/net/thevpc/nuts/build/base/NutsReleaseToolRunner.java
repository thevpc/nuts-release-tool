package net.thevpc.nuts.build.base;

import net.thevpc.nuts.app.NApp;
import net.thevpc.nuts.core.NSession;
import net.thevpc.nuts.io.NOut;
import net.thevpc.nuts.build.util.AbstractRunner;
import net.thevpc.nuts.cmdline.NCmdLine;
import net.thevpc.nuts.cmdline.NCmdLineConfigurable;
import net.thevpc.nuts.spi.NScopeType;
import net.thevpc.nuts.time.NChronometer;
import net.thevpc.nuts.text.NMsg;

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
        NutsBuildRunnerContext s = NSession.of().getProperty(NutsBuildRunnerContext.class).orNull();
        if (s == null) {
            s = new NutsBuildRunnerContext();
            NSession.of().setProperty(NutsBuildRunnerContext.class, s);
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
        NOut.println(NMsg.ofC("%s finished in %s",NMsg.ofStyledPrimary1("nuts-release-tool"),chrono.stop()));
    }



}
