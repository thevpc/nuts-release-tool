package net.thevpc.nuts.build.builders;

import net.thevpc.nuts.*;
import net.thevpc.nuts.build.util.AbstractRunner;
import net.thevpc.nuts.cmdline.NArg;
import net.thevpc.nuts.cmdline.NCmdLine;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.util.*;

import net.thevpc.nuts.build.util.Mvn;

public class JarsRunner extends AbstractRunner {

    public boolean buildJars = false;
    public JarsRunner() {
        super();
    }

    @Override
    public void configureBeforeOptions(NCmdLine cmdLine) {
    }

    @Override
    public void configureAfterOptions() {
        context().setRemoteTheVpcSshConnexion(
                NStringUtils.firstNonBlank(NMsg.ofV(
                        NStringUtils.trim(context().vars.get("PROD_SSH_CONNEXION"))
                        , context().varMapper()).toString(),context().getRemoteTheVpcSshUser() + "@thevpc.net")
        );
    }


    @Override
    public boolean configureFirst(NCmdLine cmdLine) {
        NArg c = cmdLine.peek().orNull();
        switch (c.key()) {
            case "--update-version": {
                return cmdLine.matcher().matchFlag((v) -> context().updateVersion = v.booleanValue()).anyMatch();
            }
            case "--keep-stamp": {
                return cmdLine.matcher().matchFlag((v) -> context().keepStamp = v.booleanValue()).anyMatch();
            }
            case "--production-mode": {
                return cmdLine.matcher().matchFlag((v) -> context().productionMode = v.booleanValue()).anyMatch();
            }

            case "--stable-api-version": {
                return cmdLine.matcher().matchEntry((v) -> context().nutsStableApiVersion = v.stringValue()).anyMatch();
            }
            case "--stable-app-version": {
                return cmdLine.matcher().matchEntry((v) -> context().nutsStableAppVersion = v.stringValue()).anyMatch();
            }
            case "--stable-runtime-version": {
                return cmdLine.matcher().matchEntry((v) -> context().nutsStableRuntimeVersion = v.stringValue()).anyMatch();
            }

            case "--remote-ssh-user": {
                return cmdLine.matcher().matchEntry((v) -> context().remoteTheVpcSshUser = v.stringValue()).anyMatch();
            }
            case "--remote-ssh-host": {
                return cmdLine.matcher().matchEntry((v) -> context().remoteTheVpcSshUser = v.stringValue()).anyMatch();
            }
//            case "build-jars": {
//                return cmdLine.selector().withNextFlag((v, a, s) -> buildJars = v);
//                return true;
//            }
        }
        return false;
    }

    @Override
    public void run() {
//        if (buildJars) {
            if (context().publish) {
                runNutsPublishMaven();
                runNutsPublishPreview();
                runNutsPublishStable();
            }
//        }
    }

    private void runNutsPublishMaven() {
        echoV("**** publish $nuts maven...", NMaps.of("nuts", NMsg.ofStyledKeyword("nuts")));
        String nutsFolder = Mvn.folder(NId.get("net.thevpc:nuts").get());
        upload(localMvn().resolve(nutsFolder), removeMvn().resolve(nutsFolder));
        remoteCopyFolder(removeMvn().resolve(nutsFolder), remoteThevpcMavenPath().resolve(nutsFolder));
    }

    private void runNutsPublishPreview() {
        echoV("**** publish $nuts preview...", NMaps.of("nuts", NMsg.ofStyledKeyword("nuts")));
        NPath latestJarPath = localMvn().resolve(Mvn.jar(NWorkspace.of().getAppId()));
        latestJarPath.copyTo(context().websiteProjectFolder.resolve("src/resources/download").resolve(latestJarPath.getName()));
        latestJarPath.copyTo(context().websiteProjectFolder.resolve("src/resources/download").resolve("nuts-preview.jar"));
        boolean previewIsStable = NWorkspace.of().getRuntimeId().getVersion().toString().equals(context().nutsStableRuntimeVersion);
        if(previewIsStable){
            latestJarPath.copyTo(context().websiteProjectFolder.resolve("src/resources/download").resolve("nuts-stable.jar"));
        }
        remoteMkdirs(remoteTheVpcNutsPath().toString());
        remoteCopyFile(latestJarPath, remoteTheVpcNutsPath().resolve("nuts-preview.jar"));
        if(previewIsStable){
            remoteCopyFile(latestJarPath, remoteTheVpcNutsPath().resolve("nuts-stable.jar"));
        }
    }


    private void runNutsPublishStable() {
        echoV("**** publish $nuts stable...", NMaps.of("nuts", NMsg.ofStyledKeyword("nuts")));
        NAssert.requireNonBlank(context().nutsStableAppVersion,"nutsAppStableVersion");
        String jarName = NWorkspace.of().getAppId().getArtifactId() + "-"+context().nutsStableAppVersion + ".jar";
//        NPath.of("https://repo1.maven.org/maven2/" + Mvn.jar(NWorkspace.of().getAppId().builder().setVersion(context().nutsStableVersion).build()))
//                        .copyTo(context().nutsRootFolder.resolve("installers/nuts-release-tool/dist").resolve(jarName));

        NPath localJarStable = context().websiteProjectFolder.resolve("src/resources/download").resolve(jarName);
        if(!localJarStable.isRegularFile()){
            throw new NIllegalArgumentException(NMsg.ofC("unable to find nuts stable jar at : %s", localJarStable));
        }
//        NPath.of("https://maven.thevpc.net/" + Mvn.jar(NWorkspace.of().getAppId().builder().setVersion(context().nutsStableAppVersion).build()))
//                        .copyTo(localJarStable);

        localJarStable.copyTo(context().websiteProjectFolder.resolve("src/resources/download").resolve("nuts-stable.jar"));

        remoteMkdirs(remoteTheVpcNutsPath().toString());
        upload(
                localJarStable
                , remoteTheVpcNutsPath().resolve("nuts-stable.jar")
        );
    }



}
