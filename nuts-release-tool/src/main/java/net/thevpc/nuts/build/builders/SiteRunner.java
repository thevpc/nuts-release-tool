/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.thevpc.nuts.build.builders;

import net.thevpc.nuts.artifact.NId;
import net.thevpc.nuts.build.util.AbstractRunner;
import net.thevpc.nuts.build.util.Mvn;
import net.thevpc.nuts.build.util.NReleaseUtils;
import net.thevpc.nuts.cmdline.NArg;
import net.thevpc.nuts.cmdline.NCmdLine;

import net.thevpc.nuts.core.NWorkspace;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nsite.context.NSiteContext;
import net.thevpc.nsite.context.ProjectNSiteContext;
import net.thevpc.nuts.util.NMaps;
import net.thevpc.nsite.NSiteProjectConfig;
import net.thevpc.nuts.util.NAssert;
import net.thevpc.nuts.text.NMsg;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author vpc
 */
public class SiteRunner extends AbstractRunner {


    public SiteRunner() {
        super();
    }


    @Override
    public void configureBeforeOptions(NCmdLine cmdLine) {
        for (Map.Entry<String, NElement> e : NReleaseUtils.asNamedPairs(context().confRoot.asObject().orNull()).entrySet()) {
            switch (e.getKey()) {
                case "build-site": {
                    context().buildSite=e.getValue().asBooleanValue().orElse(false);
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
        runGithubRepository();
        runGithubDocumentationWebsite();
    }


    private Map<String, Object> prepareVars() {
        Map<String, Object> vars = new HashMap<>();
//        String latestJarLocation = "https://raw.githubusercontent.com/thevpc/nuts-preview/master/net/thevpc/nuts/nuts/" + latestApiVersion + "/nuts-" + latestApiVersion + ".jar";
//        String stableJarLocation = "https://repo.maven.apache.org/maven2/net/thevpc/nuts/nuts/" + stableApiVersion + "/nuts-" + stableApiVersion + ".jar";

        String latestJarLocation = "https://maven.thevpc.net/" + Mvn.jar(NWorkspace.of().getAppId());

        vars.putAll(context().vars);
        vars.put("buildTime", new SimpleDateFormat("yyyy-MM-dd-HHmmss").format(new Date()));
        {//stable
            NAssert.requireNonBlank(context().nutsLtsAppVersion, "nutsStableVersion");
            NAssert.requireNonBlank(context().nutsLtsApiVersion, "nutsApiStableVersion");
            NAssert.requireNonBlank(context().nutsLtsRuntimeVersion, "runtimeStableVersion");

            NId stableApiId = NWorkspace.of().getApiId().builder().setVersion(context().nutsLtsApiVersion).build();
            NId stableAppId = NWorkspace.of().getAppId().builder().setVersion(context().nutsLtsAppVersion).build();
            NId stableRuntimeId = NWorkspace.of().getRuntimeId().builder().setVersion(context().nutsLtsRuntimeVersion).build();

            String stableJarLocation = "https://maven.thevpc.net/" + Mvn.jar(stableAppId);

            vars.put("stableApiId", stableApiId.toString());
            vars.put("stableApiVersion", stableApiId.getVersion().toString());

            vars.put("stableAppId", stableAppId.toString());
            vars.put("stableAppVersion", stableAppId.getVersion().toString());

            vars.put("stableRuntimeId", stableRuntimeId.toString());
            vars.put("stableRuntimeVersion", stableRuntimeId.getVersion().toString());

            vars.put("stableJarLocation", stableJarLocation);
        }

        {
            NId latestApiId = NWorkspace.of().getApiId();
            NId latestRuntimeId = NWorkspace.of().getRuntimeId();
            NId latestAppId = NWorkspace.of().getAppId();

            vars.put("latestApiId", latestApiId.toString());
            vars.put("latestApiVersion", latestApiId.getVersion().toString());
            vars.put("latestRuntimeId", latestRuntimeId.toString());
            vars.put("latestRuntimeVersion", latestRuntimeId.getVersion().toString());
            vars.put("latestAppId", latestAppId.toString());
            vars.put("latestAppVersion", latestAppId.getVersion().toString());
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
        return vars;
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
        NPath.of(Mvn.localMaven() + "/" + Mvn.jar(NWorkspace.of().getAppId()))
                .copyTo(context().websiteProjectFolder.resolve("src/resources/download/nuts-standard.jar")
                );
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
        NPath.of(Mvn.localMaven() + "/" + Mvn.jar(NWorkspace.of().getAppId()))
                .copyTo(context().websiteProjectFolder.resolve("src/resources/nuts-standard.jar")
                );
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
