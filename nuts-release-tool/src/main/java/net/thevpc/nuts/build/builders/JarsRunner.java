package net.thevpc.nuts.build.builders;

import net.thevpc.nuts.artifact.NId;
import net.thevpc.nuts.build.util.AbstractRunner;
import net.thevpc.nuts.cmdline.NArg;
import net.thevpc.nuts.cmdline.NCmdLine;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.nuts.util.*;

import net.thevpc.nuts.build.util.Mvn;
import net.thevpc.nuts.collections.NMaps;

import java.util.Map;

public class JarsRunner extends AbstractRunner {
    public boolean keepStamp = false;
    public boolean updateVersion = false;
    public Boolean productionMode = null;

    public JarsRunner() {
        super();
    }

    @Override
    public void configureBeforeOptions(NCmdLine cmdLine) {
        for (Map.Entry<String, NElement> e : context().loadConfigNamedPairs().entrySet()) {
            switch (e.getKey()) {
                case "update-version": {
                    updateVersion = e.getValue().asBooleanValue().orElse(updateVersion);
                    break;
                }
                case "keep-stamp": {
                    keepStamp = e.getValue().asBooleanValue().orElse(keepStamp);
                    break;
                }
                case "production-mode": {
                    productionMode = e.getValue().asBooleanValue().orElse(productionMode);
                    break;
                }
                case "stable-api-version": {
                    context().nutsStableApiVersion = e.getValue().asStringValue().orElse(context().nutsStableApiVersion);
                    break;
                }
                case "stable-app-version": {
                    context().nutsStableAppVersion = e.getValue().asStringValue().orElse(context().nutsStableAppVersion);
                    break;
                }
                case "stable-runtime-version": {
                    context().nutsStableRuntimeVersion = e.getValue().asStringValue().orElse(context().nutsStableRuntimeVersion);
                    break;
                }
            }
        }
    }

    @Override
    public void configureAfterOptions() {
        context().setRemoteTheVpcSshConnection(
                NStringUtils.firstNonBlankStripped(NMsg.ofV(
                        context().vars.get("PROD_SSH_CONNECTION")
                        , context().varMapper()).toString(), context().getRemoteTheVpcSshUser() + "@thevpc.net")
        );
    }


    @Override
    public boolean configureFirst(NCmdLine cmdLine) {
        NArg c = cmdLine.peek().orNull();

        return false;
    }

    @Override
    public void run() {
        if (context().publish) {
            runNutsPublishMaven();
        }
    }

    private void runNutsPublishMaven() {
        echoV("**** publish $nuts maven...", NMaps.of("nuts", NMsg.ofStyledKeyword("nuts")));
        String nutsFolder = Mvn.folder(NId.get("net.thevpc:nuts").get());
        upload(localMvn().resolve(nutsFolder), remoteThevpcMavenPath().resolve(nutsFolder));
    }



}
