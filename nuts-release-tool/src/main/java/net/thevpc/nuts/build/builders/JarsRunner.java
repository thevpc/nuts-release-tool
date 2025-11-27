package net.thevpc.nuts.build.builders;

import net.thevpc.nuts.artifact.NId;
import net.thevpc.nuts.build.util.AbstractRunner;
import net.thevpc.nuts.build.util.NReleaseUtils;
import net.thevpc.nuts.cmdline.NArg;
import net.thevpc.nuts.cmdline.NCmdLine;
import net.thevpc.nuts.core.NWorkspace;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.nuts.util.*;

import net.thevpc.nuts.build.util.Mvn;

import java.util.Map;

public class JarsRunner extends AbstractRunner {
    public boolean keepStamp = false;
    public boolean updateVersion = false;
    public Boolean productionMode = null;

    public boolean buildJars = false;
    public JarsRunner() {
        super();
    }

    @Override
    public void configureBeforeOptions(NCmdLine cmdLine) {
        for (Map.Entry<String, NElement> e : NReleaseUtils.asNamedPairs(context().confRoot.asObject().orNull()).entrySet()) {
            switch (e.getKey()) {
                case "update-version": {
                    updateVersion=e.getValue().asBooleanValue().orElse(false);
                    break;
                }
                case "keep-stamp": {
                    keepStamp=e.getValue().asBooleanValue().orElse(false);
                    break;
                }
                case "production-mode": {
                    productionMode=e.getValue().asBooleanValue().orElse(false);
                    break;
                }
                case "lts-api-version": {
                    context().nutsLtsApiVersion =e.getValue().asStringValue().get();
                    break;
                }
                case "lts-app-version": {
                    context().nutsLtsAppVersion =e.getValue().asStringValue().get();
                    break;
                }
                case "lts-runtime-version": {
                    context().nutsLtsRuntimeVersion =e.getValue().asStringValue().get();
                    break;
                }
                case "remote-ssh-host": {
                    context().remoteTheVpcSshUser=e.getValue().asStringValue().get();
                    break;
                }
            }
        }
    }

    @Override
    public void configureAfterOptions() {
        context().setRemoteTheVpcSshConnection(
                NStringUtils.firstNonBlank(NMsg.ofV(
                        NStringUtils.trim(context().vars.get("PROD_SSH_CONNECTION"))
                        , context().varMapper()).toString(),context().getRemoteTheVpcSshUser() + "@thevpc.net")
        );
    }


    @Override
    public boolean configureFirst(NCmdLine cmdLine) {
        NArg c = cmdLine.peek().orNull();

        return false;
    }

    @Override
    public void run() {
//        if (buildJars) {
            if (context().publish) {
                runNutsPublishMaven();
                runNutsPublishStandard();
                runNutsPublishLts();
            }
//        }
    }

    private void runNutsPublishMaven() {
        echoV("**** publish $nuts maven...", NMaps.of("nuts", NMsg.ofStyledKeyword("nuts")));
        String nutsFolder = Mvn.folder(NId.get("net.thevpc:nuts").get());
        upload(localMvn().resolve(nutsFolder), removeMvn().resolve(nutsFolder));
        remoteCopyFolder(removeMvn().resolve(nutsFolder), remoteThevpcMavenPath().resolve(nutsFolder));
    }

    private void runNutsPublishStandard() {
        echoV("**** publish $nuts standard...", NMaps.of("nuts", NMsg.ofStyledKeyword("nuts")));
        NPath latestJarPath = localMvn().resolve(Mvn.jar(NWorkspace.of().getAppId()));
        latestJarPath.copyTo(context().websiteProjectFolder.resolve("src/resources/download").resolve(latestJarPath.getName()));
        latestJarPath.copyTo(context().websiteProjectFolder.resolve("src/resources/download").resolve("nuts-standard.jar"));
        boolean starndardIsStable = NWorkspace.of().getRuntimeId().getVersion().toString().equals(context().nutsLtsRuntimeVersion);
        if(starndardIsStable){
            latestJarPath.copyTo(context().websiteProjectFolder.resolve("src/resources/download").resolve("nuts-lts.jar"));
        }
        remoteMkdirs(remoteTheVpcNutsPath().toString());
        remoteCopyFile(latestJarPath, remoteTheVpcNutsPath().resolve("nuts-standard.jar"));
        if(starndardIsStable){
            remoteCopyFile(latestJarPath, remoteTheVpcNutsPath().resolve("nuts-lts.jar"));
        }
    }


    private void runNutsPublishLts() {
        echoV("**** publish $nuts stable...", NMaps.of("nuts", NMsg.ofStyledKeyword("nuts")));
        NAssert.requireNonBlank(context().nutsLtsAppVersion,"nutsAppStableVersion");
        String jarName = NWorkspace.of().getAppId().getArtifactId() + "-"+context().nutsLtsAppVersion + ".jar";
//        NPath.of("https://repo1.maven.org/maven2/" + Mvn.jar(NWorkspace.of().getAppId().builder().setVersion(context().nutsStableVersion).build()))
//                        .copyTo(context().nutsRootFolder.resolve("installers/nuts-release-tool/dist").resolve(jarName));

        NPath localJarLts = context().websiteProjectFolder.resolve("src/resources/download").resolve(jarName);
        if(!localJarLts.isRegularFile()){
            throw new NIllegalArgumentException(NMsg.ofC("unable to find nuts LTS jar at : %s", localJarLts));
        }
//        NPath.of("https://maven.thevpc.net/" + Mvn.jar(NWorkspace.of().getAppId().builder().setVersion(context().nutsStableAppVersion).build()))
//                        .copyTo(localJarLts);

        localJarLts.copyTo(context().websiteProjectFolder.resolve("src/resources/download").resolve("nuts-lts.jar"));

        remoteMkdirs(remoteTheVpcNutsPath().toString());
        upload(
                localJarLts
                , remoteTheVpcNutsPath().resolve("nuts-lts.jar")
        );
    }



}
