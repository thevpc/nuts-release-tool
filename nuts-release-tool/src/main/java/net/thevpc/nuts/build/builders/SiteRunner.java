/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.thevpc.nuts.build.builders;

import net.thevpc.nuts.artifact.NId;
import net.thevpc.nuts.build.util.AbstractRunner;
import net.thevpc.nuts.build.util.Mvn;
import net.thevpc.nuts.cmdline.NArg;
import net.thevpc.nuts.cmdline.NCmdLine;

import net.thevpc.nuts.command.NExec;
import net.thevpc.nuts.core.NWorkspace;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.elem.NElementReader;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nsite.context.NSiteContext;
import net.thevpc.nsite.context.ProjectNSiteContext;
import net.thevpc.nuts.collections.NMaps;
import net.thevpc.nsite.NSiteProjectConfig;
import net.thevpc.nsite.javadoc.NSiteJavadoc;
import net.thevpc.nsite.javadoc.NSiteJavadocConfig;
import net.thevpc.nuts.io.NPathOption;
import net.thevpc.nuts.util.NAssert;
import net.thevpc.nuts.text.NMsg;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author vpc
 */
public class SiteRunner extends AbstractRunner {


    public SiteRunner() {
        super();
    }


    @Override
    public void configureBeforeOptions(NCmdLine cmdLine) {
        for (Map.Entry<String, NElement> e : context().loadConfigNamedPairs().entrySet()) {
            switch (e.getKey()) {
                case "build-site": {
                    context().buildSite = e.getValue().asBooleanValue().orElse(context().buildSite);
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
        if (context().buildSite) {
            runSite();
        }
    }

    private void runSite() {
        echoV("**** $v (nuts)...", NMaps.of("v", NMsg.ofStyledKeyword("build-nuts-site")));
        prepareJars();
        prepareVersions();
        runGithubRepository();
        runGithubDocumentationWebsite();
        if (context().publish) {
            runCopyThevpcNetScripts();
        }
    }

    private void runCopyThevpcNetScripts() {
        String REMOTE_NUTS_THEVPC_DEPLOY_PATH = context().getVar("REMOTE_NUTS_THEVPC_DEPLOY_PATH").get();
        if (!REMOTE_NUTS_THEVPC_DEPLOY_PATH.endsWith("/")) {
            REMOTE_NUTS_THEVPC_DEPLOY_PATH += "/";
        }


        NExec.ofSystem(
                "rsync", "-avz", "--progress", "-e", "ssh", "--info=progress2", "--human-readable",
                context().nutsRootFolder + "/scripts/thevpc.net/",
                context().getRemoteTheVpcSshUser() + "@" + context().getRemoteTheVpcSshHost().get() + ":" + REMOTE_NUTS_THEVPC_DEPLOY_PATH
        ).run();
    }


    private Map<String, Object> prepareVars() {
        Map<String, Object> vars = new HashMap<>();
//        String latestJarLocation = "https://raw.githubusercontent.com/thevpc/nuts-preview/master/net/thevpc/nuts/nuts/" + latestApiVersion + "/nuts-" + latestApiVersion + ".jar";
//        String stableJarLocation = "https://repo.maven.apache.org/maven2/net/thevpc/nuts/nuts/" + stableApiVersion + "/nuts-" + stableApiVersion + ".jar";

        String latestJarLocation = "https://maven.thevpc.net/" + Mvn.jar(NWorkspace.of().appId());

        vars.putAll(context().vars);
        vars.put("buildTime", new SimpleDateFormat("yyyy-MM-dd-HHmmss").format(new Date()));
        {
            List<NElement> children = NElementReader.ofTson().read(context().websiteProjectFolder.resolve("src/include/versions/versions.json"))
                    .asArray().get().children();

            String latestVersion = "v" + NWorkspace.of().apiId().version().toString();
            List<String> allNonLatestVersions = children.stream().map(x -> x.asObject().get())
                    .map(x->x.get("id").get().asStringValue().get())
                    .filter(x->!x.equals(latestVersion))
                    .collect(Collectors.toList());
            vars.put("allNonLatestVersions", allNonLatestVersions);
        }
        {//stable
            NAssert.requireNamedNonBlank(context().nutsStableAppVersion, "nutsStableAppVersion");
            NAssert.requireNamedNonBlank(context().nutsStableApiVersion, "nutsStableApiVersion");
            NAssert.requireNamedNonBlank(context().nutsStableRuntimeVersion, "nutsStableRuntimeVersion");

            NId stableApiId = NWorkspace.of().apiId().builder().version(context().nutsStableApiVersion).build();
            NId stableAppId = NWorkspace.of().appId().builder().version(context().nutsStableAppVersion).build();
            NId stableRuntimeId = NWorkspace.of().runtimeId().builder().version(context().nutsStableRuntimeVersion).build();

            String stableJarLocation = "https://maven.thevpc.net/" + Mvn.jar(stableAppId);

            vars.put("stableApiId", stableApiId.toString());
            vars.put("stableApiVersion", stableApiId.version().toString());

            vars.put("stableAppId", stableAppId.toString());
            vars.put("stableAppVersion", stableAppId.version().toString());

            vars.put("stableRuntimeId", stableRuntimeId.toString());
            vars.put("stableRuntimeVersion", stableRuntimeId.version().toString());

            vars.put("stableJarLocation", stableJarLocation);
        }

        {
            NId latestApiId = NWorkspace.of().apiId();
            NId latestRuntimeId = NWorkspace.of().runtimeId();
            NId latestAppId = NWorkspace.of().appId();

            vars.put("latestApiId", latestApiId.toString());
            vars.put("latestApiVersion", latestApiId.version().toString());
            vars.put("latestRuntimeId", latestRuntimeId.toString());
            vars.put("latestRuntimeVersion", latestRuntimeId.version().toString());
            vars.put("latestAppId", latestAppId.toString());
            vars.put("latestAppVersion", latestAppId.version().toString());
            vars.put("latestJarLocation", latestJarLocation);
        }
        {
            vars.put("jarLocation", vars.get("latestJarLocation"));
            vars.put("apiId", vars.get("latestApiId"));
            vars.put("apiVersion", vars.get("latestApiVersion"));
            vars.put("appId", vars.get("latestAppId"));
            vars.put("appVersion", vars.get("latestAppVersion"));
            vars.put("runtimeId", vars.get("latestRuntimeId"));
            vars.put("runtimeVersion", vars.get("latestRuntimeVersion"));
        }
        {
            NPath versionsFile = context().websiteProjectFolder.resolve("src/include/versions/versions.json");
            if (versionsFile.exists()) {
                try {
                    vars.put("docVersionsFile", versionsFile.toString());
                } catch (Exception ex) {
                    // ignore
                }
            }
        }
        return vars;
    }

    private void prepareJars() {
        NId latestAppId = NWorkspace.of().appId();
        NId stableAppId = latestAppId.builder().version(context().nutsStableAppVersion).build();
        NPath.of(Mvn.localMaven() + "/" + Mvn.jar(latestAppId))
                .copyTo(context().websiteProjectFolder.resolve("src/resources/download/nuts-latest.jar")
                );

        context().websiteProjectFolder.resolve("src/resources/download/nuts-latest.jar").copyTo(
                context().websiteProjectFolder.resolve("src/resources/download/nuts-app-" + latestAppId.version() + ".jar")
        );


        if (!context().websiteProjectFolder.resolve("src/resources/download/nuts-stable.jar").exists()) {
            NPath.of("https://repo1.maven.org/maven2/" + Mvn.jar(stableAppId))
                    .copyTo(context().websiteProjectFolder.resolve("src/resources/download/nuts-stable.jar")
                    );
        }
        if (!context().websiteProjectFolder.resolve("src/resources/download/nuts-app-" + stableAppId.version() + ".jar").exists()) {
            context().websiteProjectFolder.resolve("src/resources/download/nuts-stable.jar").copyTo(
                    context().websiteProjectFolder.resolve("src/resources/download/nuts-app-" + stableAppId.version() + ".jar")
            );
        }
    }

    private void prepareVersions() {
        echoC("**** %s %s (nuts)...", NMsg.ofStyledKeyword("prepare nsite versions"), NMsg.ofStyledSuccess("repository"));
        List<NElement> children = NElementReader.ofTson().read(context().websiteProjectFolder.resolve("src/include/versions/versions.json"))
                .asArray().get().children();
        String latestVersion = "v" + NWorkspace.of().apiId().version().toString();
        prepareCommandHelp(latestVersion);
        prepareApiDoc(latestVersion);
        for (String version : children.stream().map(x -> x.asObject().get().get("id").get().asStringValue().get()).collect(Collectors.toList())) {
            NPath vFolder = context().websiteProjectFolder.resolve("src/main/versions/" + version);
            vFolder.ensureEmptyDirectory();
            context().websiteProjectFolder.resolve("src/include/template/v1/doc-*.html")
                    .walkGlob(NPathOption.SORTED)
                    .forEach(x -> {
                        String s2 = x.readString()
                                .replace("[[templateLatestVersion]]", latestVersion)
                                .replace("[[templateCurrentVersion]]", version)
                                ;
                        vFolder.resolve(x.name()).writeString(s2);
                    });
        }
    }

    private void prepareCommandHelp(String latestVersion) {
        NPath helpDir = context().websiteProjectFolder.resolve("src/include/versions/" + latestVersion + "/doc-nuts-help");
        helpDir.mkdirs();

        NPath folderInfo = helpDir.resolve(".folder-info.md");
        if (!folderInfo.exists()) {
            folderInfo.writeString("---\ntitle: Nuts Command Help\n---\nRaw command-line help syntax and options for Nuts commands.\n");
        }

        NPath runtimeFolder = context().nutsRootFolder.resolve("core/nuts-runtime/src/main/resources/net/thevpc/nuts/runtime");
        NPath nutsHelp = runtimeFolder.resolve("nuts-help.ntf");
        if (nutsHelp.exists()) {
            String content = "---\nid: nuts-help\ntitle: nuts\n---\n\n" + nutsHelp.readString();
            helpDir.resolve("010-nuts.ntf").writeString(content);
        }

        List<String> orderedCommands = Arrays.asList(
                "exec", "which", "fetch", "install", "uninstall",
                "check-updates", "update", "search", "deploy", "push",
                "settings", "welcome", "info", "version", "help", "license", "bundle"
        );

        Set<String> processed = new HashSet<>();
        int counter = 20;
        for (String cmd : orderedCommands) {
            NPath cmdFile = runtimeFolder.resolve("command/" + cmd + ".ntf");
            if (cmdFile.exists()) {
                String fileName = String.format("%03d-%s.ntf", counter, cmd);
                String content = "---\nid: " + cmd + "-help\ntitle: " + cmd + "\n---\n\n" + cmdFile.readString();
                helpDir.resolve(fileName).writeString(content);
                processed.add(cmd + ".ntf");
                counter += 10;
            }
        }

        NPath commandFolder = runtimeFolder.resolve("command");
        if (commandFolder.isDirectory()) {
            for (NPath otherFile : commandFolder.stream().sorted().collect(Collectors.toList())) {
                if (otherFile.name().endsWith(".ntf") && !processed.contains(otherFile.name())) {
                    String cmd = otherFile.name().substring(0, otherFile.name().length() - 4);
                    String fileName = String.format("%03d-%s.ntf", counter, cmd);
                    String content = "---\nid: " + cmd + "-help\ntitle: " + cmd + "\n---\n\n" + otherFile.readString();
                    helpDir.resolve(fileName).writeString(content);
                    counter += 10;
                }
            }
        }
    }

    private void prepareApiDoc(String latestVersion) {
        NPath apiDir = context().websiteProjectFolder.resolve("src/include/versions/" + latestVersion + "/doc-api");
        apiDir.mkdirs();
        NPath apiSource = context().nutsRootFolder.resolve("core/nuts-api/src/main/java");
        NPath bootSource = context().nutsRootFolder.resolve("core/nuts-boot/src/main/java");
        if (apiSource.isDirectory()) {
            echoC("**** %s %s (nuts)...", NMsg.ofStyledKeyword("generate javadoc"), NMsg.ofStyledSuccess("nuts-api"));
            NSiteJavadocConfig config = new NSiteJavadocConfig()
                    .addSourceRoot(apiSource.toString())
                    .setTargetDir(apiDir.toString())
                    .setTitle("Nuts Core API (Javadoc)")
                    .setDescription("Comprehensive API reference for the Nuts Core API (nuts-api).");
            if (bootSource.isDirectory()) {
                config.addSourceRoot(bootSource.toString());
            }
            NSiteJavadoc.generate(config);
        }
    }

    private void runGithubRepository() {
        echoC("**** %s %s (nuts)...", NMsg.ofStyledKeyword("nsite"), NMsg.ofStyledSuccess("repository"));
        NSiteProjectConfig config = new NSiteProjectConfig()
                .setContextName("nuts-release-tool/repository")
                .setProjectPath(context().repositoryProjectFolder.toString())
                .addSource(context().websiteProjectFolder.resolve("src/main/METADATA").toString())
                .setTargetFolder(context().nutsRootFolder.toString());
        NSiteContext templateProject = new ProjectNSiteContext();
        templateProject.setVars(prepareVars());
        templateProject.run(config);
    }

    private void runGithubDocumentationWebsite() {
        echoC("**** %s %s (nuts)...", NMsg.ofStyledKeyword("nsite"), NMsg.ofStyledSuccess("documentation"));
        NSiteProjectConfig config = new NSiteProjectConfig()
                .setContextName("nuts-release-tool/documentation")
                .setProjectPath(context().websiteProjectFolder.toString())
                .setClean(true)
                .setTargetFolder(context().nutsRootFolder.resolve("docs").toString());
        NSiteContext templateProject = new ProjectNSiteContext();
        templateProject.setVars(prepareVars());
        templateProject.run(config);


//        NInstallCmd.of("ndocusaurus").run();
//        echo("**** $v (nuts)...", NMaps.of("v", NMsg.ofStyled("ndocusaurus", NTextStyle.keyword())));
//        String workdir = context().NUTS_WEBSITE_BASE.toString();
//        DocusaurusProject docusaurusProject = new DocusaurusProject(workdir,
//                Paths.get(workdir).resolve(".dir-template").resolve("src").toString());
//        DocusaurusCtrl docusaurusCtrl = new DocusaurusCtrl(docusaurusProject)
//                .setBuildWebSite(true)
//                .setStartWebSite(false)
//                .setBuildPdf(true)
//                .setAutoInstallNutsPackages(NWorkspace.of()
//                        .getBootOptions().getConfirm().orElse(NConfirmationMode.ASK) == NConfirmationMode.YES)
//                .setVars(prepareVars());
//
//        docusaurusCtrl.run();

    }
}
