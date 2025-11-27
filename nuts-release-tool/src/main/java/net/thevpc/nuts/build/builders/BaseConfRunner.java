/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.thevpc.nuts.build.builders;

import net.thevpc.nuts.build.util.*;
import net.thevpc.nuts.cmdline.NArg;
import net.thevpc.nuts.cmdline.NCmdLine;
import net.thevpc.nuts.core.NSession;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.util.NAssert;
import net.thevpc.nuts.util.NIllegalArgumentException;
import net.thevpc.nuts.text.NMsg;

import java.util.Map;
import java.util.logging.Level;

/**
 * @author vpc
 */
public class BaseConfRunner extends AbstractRunner {


    public static final String NUTS_RELEASE_CONF_TSON = "nuts-release-tool.tson";

    @Override
    public boolean configureFirst(NCmdLine cmdLine) {
        NArg c = cmdLine.peek().orNull();
        return false;
    }

    public BaseConfRunner() {
        super();
    }

    @Override
    public void configureBeforeOptions(NCmdLine cmdLine) {
        if (context().nutsRootFolder == null) {
            NPath newRoot = NPath.ofUserDirectory();
            NReleaseUtils.ensureNutsRepoFolder(newRoot);
            context().nutsRootFolder = newRoot;
        }
        context().confFileTson=resolveConfTson();
        for (Map.Entry<String, String> e : NopsEnv.getNopsEnv().entrySet()) {
            context().setVar(e.getKey(), e.getValue());
        }
        if (!context().confFileTson.isRegularFile()) {
            throw new NIllegalArgumentException(NMsg.ofC("missing %s", context().confFileTson));
        }
        context().loadConfig(cmdLine);

        context().websiteProjectFolder = context().nutsRootFolder.resolve("documentation/website");
        context().repositoryProjectFolder = context().nutsRootFolder.resolve("documentation/repo");
        context().setVar("root", context().nutsRootFolder.toString());

        for (Map.Entry<String, NElement> e : NReleaseUtils.asNamedPairs(context().confRoot.asObject().orNull()).entrySet()) {
            switch (e.getKey()) {
                case "trace": {
                    context().trace=e.getValue().asBooleanValue().orElse(false);
                    NSession.of().setTrace(context().trace);
                    break;
                }
                case "verbose": {
                    context().verbose=e.getValue().asBooleanValue().orElse(false);
                    if(context().verbose) {
                        NSession.of().setLogTermLevel(Level.FINEST);
                    }
                    break;
                }
                case "publish": {
                    context().publish=e.getValue().asBooleanValue().orElse(false);
                    break;
                }
                case "debug": {
                    if(e.getValue().asBooleanValue().orElse(false)) {
                        context().nutsDebugArg = "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005";
                    }
                    break;
                }
            }
        }
        if (context().confRoot.asObject().isPresent()) {
            for (Map.Entry<String, NElement> e : NReleaseUtils.asNamedPairs(context().confRoot.asObject().get().get("vars").orNull()).entrySet()) {
                context().vars.put(e.getKey(), e.getValue().asStringValue().get());
            }
        }
    }

    private NPath resolveConfTson() {
        if (context().confFileTson == null) {
            if (NPath.of(NUTS_RELEASE_CONF_TSON).isRegularFile()) {
                return NPath.of(NUTS_RELEASE_CONF_TSON).toAbsolute().normalize();
            }
        }
        if (context().confFileTson == null) {
            if (NPath.of("nuts-release-tool/" + NUTS_RELEASE_CONF_TSON).isRegularFile()) {
                return NPath.of("nuts-release-tool/" + NUTS_RELEASE_CONF_TSON).toAbsolute().normalize();
            }
        }
        if (context().confFileTson == null) {
            if (NPath.of("nuts-release-tool/nuts-release-tool/" + NUTS_RELEASE_CONF_TSON).isRegularFile()) {
                return  NPath.of("nuts-release-tool/nuts-release-tool/" + NUTS_RELEASE_CONF_TSON).toAbsolute().normalize();
            }
        }
        if (context().confFileTson == null) {
            throw new NIllegalArgumentException(NMsg.ofC("missing %s", NPath.of(NUTS_RELEASE_CONF_TSON).toAbsolute().normalize()));
        }
        if (context().confFileTson.isDirectory()) {
            return  context().confFileTson.resolve(NUTS_RELEASE_CONF_TSON);
            //throw new NIllegalArgumentException(NMsg.ofC("missing %s", NPath.of(NUTS_RELEASE_CONF_TSON).toAbsolute().normalize()));
        }
        return NPath.of(NUTS_RELEASE_CONF_TSON).toAbsolute().normalize();
    }

    @Override
    public void configureAfterOptions() {
        NAssert.requireNonBlank(context().nutsLtsApiVersion, "nutsStableApiVersion");
        NAssert.requireNonBlank(context().nutsLtsAppVersion, "nutsStableAppVersion");
        NAssert.requireNonBlank(context().nutsLtsRuntimeVersion, "nutsStableRuntimeVersion");
    }

    @Override
    public void run() {

    }


}
