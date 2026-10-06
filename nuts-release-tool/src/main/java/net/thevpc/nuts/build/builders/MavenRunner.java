/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.thevpc.nuts.build.builders;

import net.thevpc.nuts.build.util.AbstractRunner;
import net.thevpc.nuts.build.util.NFileLines;
import net.thevpc.nuts.cmdline.NArg;
import net.thevpc.nuts.cmdline.NCmdLine;
import net.thevpc.nuts.command.NExec;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.util.*;

import java.awt.*;
import java.util.Map;

/**
 * @author vpc
 */
public class MavenRunner extends AbstractRunner {

    boolean buildMaven = true;

    public MavenRunner() {
        super();
    }

    @Override
    public void configureBeforeOptions(NCmdLine cmdLine) {
        for (Map.Entry<String, NElement> e : context().loadConfigNamedPairs().entrySet()) {
            switch (e.getKey()) {
                case "build-maven": {
                    buildMaven = e.getValue().asBooleanValue().orElse(buildMaven);
                    break;
                }

            }
        }
    }

    @Override
    public boolean configureFirst(NCmdLine cmdLine) {
        NArg c = cmdLine.peek().orNull();
        return false;
    }

    @Override
    public void run() {
        if (buildMaven) {
            String versionLiteralRegex="\"(?<var>[0-9.-]+)\"";
            NFileLines.replaceLine(
                    context().nutsRootFolder.resolve("core/nuts-boot/src/main/java/net/thevpc/nuts/boot/NBootWorkspace.java"),
                    "String\\s+NUTS_BOOT_VERSION\\s*=\\s*"+versionLiteralRegex,
                    context().nutsLatestBootVersion
            );
            NFileLines.replaceLine(
                    context().nutsRootFolder.resolve("core/nuts-api/src/main/java/net/thevpc/nuts/Nuts.java"),
                    "static\\s+final\\s+NVersion\\s+version\\s*=\\s*NVersion\\.of\\("+versionLiteralRegex+"\\)",
                    context().nutsLatestApiVersion
            );
            NFileLines.replaceLine(
                    context().nutsRootFolder.resolve("core/nuts-runtime/src/main/java/net/thevpc/nuts/runtime/standalone/workspace/DefaultNWorkspace.java"),
                    "static\\s+final\\s+String\\s+RUNTIME_VERSION\\s*=\\s*"+versionLiteralRegex,
                    context().nutsLatestRuntimeVersion
            );
            NFileLines.replaceLine(
                    context().nutsRootFolder.resolve("installers/nuts-installer/src/main/java/net/thevpc/nuts/installer/NutsInstaller.java"),
                    "static\\s+final\\s+String\\s+VERSION\\s*=\\s*"+versionLiteralRegex,
                    context().nutsLatestRuntimeVersion
            );

            NExec.ofSystem("mvn", "clean", "install", "-DskipTests")
                    .env("JAVA_HOME",context().getVar("JAVA17_HOME").get())
                    .directory(context().nutsRootFolder)
                    .failFast(true)
                    .run();
        }
    }
}
