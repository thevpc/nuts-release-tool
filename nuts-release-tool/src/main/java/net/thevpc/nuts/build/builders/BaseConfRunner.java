/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.thevpc.nuts.build.builders;

import net.thevpc.nuts.*;
import net.thevpc.nuts.build.util.*;
import net.thevpc.nuts.cmdline.NArg;
import net.thevpc.nuts.cmdline.NCmdLine;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.util.NAssert;
import net.thevpc.nuts.util.NMsg;

/**
 * @author vpc
 */
public class BaseConfRunner extends AbstractRunner {


    public static final String NUTS_RELEASE_CONF = "nuts-release.conf";

    @Override
    public boolean configureFirst(NCmdLine cmdLine) {
        NArg c = cmdLine.peek().orNull();
        switch (c.key()) {
            case "--root": {
                cmdLine.matcher().matchEntry((v) -> {
                    //already processed
                    //context().nutsRootFolder = NPath.of(v).toAbsolute().normalize();
                }).require();
                return true;
            }
            case "--conf": {
                cmdLine.matcher().matchEntry((v) -> {
                            //already processed
                            //context().confFile = (NPath.of(v).isDirectory() ? NPath.of(v).resolve(NUTS_RELEASE_CONF) : NPath.of(v)).toAbsolute().normalize()
                        }
                ).require();
                return true;
            }
            case "--debug": {
                return cmdLine.matcher().matchFlag((v)
                        -> context().nutsDebugArg = "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005"
                ).anyMatch();
            }
            case "--trace": {
                return cmdLine.matcher().matchFlag((v) -> context().trace = v.booleanValue()).anyMatch();
            }
            case "--verbose": {
                return cmdLine.matcher().matchFlag((v) -> context().verbose = v.booleanValue()).anyMatch();
            }
            // actions
            case "publish": {
                return cmdLine.matcher().matchFlag((v) -> context().publish = v.booleanValue()).anyMatch();
            }
        }
        return false;
    }

    public BaseConfRunner() {
        super();
    }

    @Override
    public void configureBeforeOptions(NCmdLine cmdLine) {
        NCmdLine cmdLine2=cmdLine.copy();
        while (cmdLine2.hasNext()) {
            if(cmdLine2.matcher()
                    .with("--root").matchEntry(a->{
                        NPath newRoot = NPath.of(a.stringValue()).toAbsolute().normalize();
                        NReleaseUtils.ensureNutsRepoFolder(newRoot);
                        context().nutsRootFolder = newRoot;
                    })
                    .with("--conf").matchEntry(a->{
                        NPath conf = NPath.of(a.stringValue()).toAbsolute().normalize();
                        context().confFile = conf;
                    })
                    .noMatch()){
                cmdLine2.skip();
            }
        }
        if (context().nutsRootFolder == null) {
            NPath newRoot = NPath.ofUserDirectory();
            NReleaseUtils.ensureNutsRepoFolder(newRoot);
            context().nutsRootFolder = newRoot;
        }
        if (context().confFile == null) {
            if (NPath.of(NUTS_RELEASE_CONF).isRegularFile()) {
                context().confFile = NPath.of(NUTS_RELEASE_CONF).toAbsolute().normalize();
            }
        }
        if (context().confFile == null) {
            if(NPath.of("nuts-release-tool/"+NUTS_RELEASE_CONF).isRegularFile()) {
                context().confFile = NPath.of("nuts-release-tool/"+NUTS_RELEASE_CONF).toAbsolute().normalize();
            }
        }
        if (context().confFile == null) {
            if(NPath.of("nuts-release-tool/nuts-release-tool/"+NUTS_RELEASE_CONF).isRegularFile()) {
                context().confFile = NPath.of("nuts-release-tool/nuts-release-tool/"+NUTS_RELEASE_CONF).toAbsolute().normalize();
            }
        }
        if (context().confFile == null) {
            throw new NIllegalArgumentException(NMsg.ofC("missing %s", NPath.of(NUTS_RELEASE_CONF).toAbsolute().normalize()));
        }
        if (context().confFile.isDirectory()) {
            context().confFile=context().confFile.resolve(NUTS_RELEASE_CONF);
            throw new NIllegalArgumentException(NMsg.ofC("missing %s", NPath.of(NUTS_RELEASE_CONF).toAbsolute().normalize()));
        }
        if (!context().confFile.isRegularFile()) {
            throw new NIllegalArgumentException(NMsg.ofC("missing %s", context().confFile));
        }else{
            context().loadConfig(context().confFile,cmdLine);
        }
        context().websiteProjectFolder = context().nutsRootFolder.resolve("documentation/website");
        context().repositoryProjectFolder = context().nutsRootFolder.resolve("documentation/repo");
        context().setVar("root", context().nutsRootFolder.toString());
    }

    @Override
    public void configureAfterOptions() {

        NAssert.requireNonBlank(context().nutsStableApiVersion, "nutsStableApiVersion");
        NAssert.requireNonBlank(context().nutsLtsVersion, "nutsStableAppVersion");
        NAssert.requireNonBlank(context().nutsStableRuntimeVersion, "nutsStableRuntimeVersion");
    }

    @Override
    public void run() {

    }


}
