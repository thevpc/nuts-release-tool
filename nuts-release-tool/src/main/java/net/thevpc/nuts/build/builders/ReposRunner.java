/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.thevpc.nuts.build.builders;

import net.thevpc.nuts.build.util.*;
import net.thevpc.nuts.cmdline.NArg;
import net.thevpc.nuts.cmdline.NCmdLine;
import net.thevpc.nuts.command.NExec;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.util.NMaps;
import net.thevpc.nuts.text.NMsg;

import java.util.Map;

/**
 * @author vpc
 */
public class ReposRunner extends AbstractRunner {

    boolean repoPreview = false;
    boolean repoPublic = false;

    @Override
    public void configureBeforeOptions(NCmdLine cmdLine) {
        for (Map.Entry<String, NElement> e : NReleaseUtils.asNamedPairs(context().confRoot.asObject().orNull()).entrySet()) {
            switch (e.getKey()) {
                case "build-repo-nuts-preview": {
                    repoPreview=e.getValue().asBooleanValue().orElse(false);
                    break;
                }
                case "build-repo-nuts-public": {
                    repoPublic=e.getValue().asBooleanValue().orElse(false);
                    break;
                }
            }
        }
    }

    @Override
    public void configureAfterOptions() {

    }

    @Override
    public boolean configureFirst(NCmdLine cmdLine) {
        NArg c = cmdLine.peek().orNull();
        return false;
    }

    public ReposRunner() {
        super();
    }

    @Override
    public void run() {
        if (repoPreview) {
            echoV("**** $v (nuts settings update stats)...", NMaps.of("v", NMsg.ofStyledKeyword("build-nuts-preview")));
            NExec.of()
                    .addCommand("settings", "update", "stats")
                    .addCommand(context().nutsRootFolder.resolve("../nuts-repos/nuts-preview"))
                    .failFast()
                    .run();
        }
        if (repoPublic) {
            echoV("**** $v (nuts settings update stats)...", NMaps.of("v", NMsg.ofStyledKeyword("build-nuts-public")));
            NExec.of()
                    .addCommand("settings", "update", "stats")
                    .addCommand(context().nutsRootFolder.resolve("../nuts-repos/nuts-public"))
                    .failFast()
                    .run();
        }
    }

}
