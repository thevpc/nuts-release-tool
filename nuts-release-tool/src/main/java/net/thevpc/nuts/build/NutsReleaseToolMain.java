package net.thevpc.nuts.build;

import net.thevpc.nuts.*;
import net.thevpc.nuts.build.base.NutsReleaseToolRunner;
import net.thevpc.nuts.build.builders.*;

@NAppDefinition
public class NutsReleaseToolMain  {

    public static void main(String[] args) {
        NApp.builder(args).run();
    }

    @NAppRunner
    public void run() {
        NSession session = NSession.of();
        //always yes!!
        session.copy().yes()
                .runWith(() -> {
                    NutsReleaseToolRunner nutsBuildRunner = new NutsReleaseToolRunner(
                            new BaseConfRunner(),
                            new JarsRunner(),
                            new ReposRunner(),
                            new InstallerRunner(),
                            new SiteRunner()
                    );
                    nutsBuildRunner.run(
                            NApp.of().getCmdLine()
                    );
                });
    }

}
