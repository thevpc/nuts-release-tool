/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.thevpc.nuts.build.builders;

import net.thevpc.nuts.Nuts;
import net.thevpc.nuts.build.util.*;
import net.thevpc.nuts.cmdline.NArg;
import net.thevpc.nuts.cmdline.NCmdLine;

import net.thevpc.nuts.core.NWorkspace;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.io.NPath;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Map;

/**
 * @author vpc
 */
public class InstallerRunner extends AbstractRunner {

    NamedStringParam NUTS_JAVA_HOME = NParams.ofString("NUTS_JAVA_HOME", null);
    NamedStringParam NUTS_INSTALLER_BUILD_JAVA_HOME = NParams.ofString("NUTS_INSTALLER_BUILD_JAVA_HOME", null);
    NamedStringParam NUTS_GRAALVM_DIR = NParams.ofString("NUTS_GRAALVM_DIR", null);

    NamedStringParam INSTALLER_JRE8_LINUX64 = NParams.ofString("INSTALLER_JRE8_LINUX64", null);
    NamedStringParam INSTALLER_JRE8_LINUX32 = NParams.ofString("INSTALLER_JRE8_LINUX32", null);
    NamedStringParam INSTALLER_JRE8_WINDOWS64 = NParams.ofString("INSTALLER_JRE8_WINDOWS64", null);
    NamedStringParam INSTALLER_JRE8_WINDOWS32 = NParams.ofString("INSTALLER_JRE8_WINDOWS32", null);
    NamedStringParam INSTALLER_JRE8_MAC64 = NParams.ofString("INSTALLER_JRE8_MAC64", null);
    boolean buildNative = false;
    boolean buildInstaller = false;
    boolean buildBin = false;

    @Override
    public void configureBeforeOptions(NCmdLine cmdLine) {
        for (Map.Entry<String, NElement> e : context().loadConfigNamedPairs().entrySet()) {
            switch (e.getKey()) {
                case "build-native": {
                    buildNative=e.getValue().asBooleanValue().orElse(buildNative);
                    break;
                }
                case "build-installer": {
                    buildInstaller=e.getValue().asBooleanValue().orElse(buildInstaller);
                    break;
                }
                case "build-bin": {
                    buildBin=e.getValue().asBooleanValue().orElse(buildBin);
                    break;
                }
            }
        }
    }

    @Override
    public void configureAfterOptions() {
        NUTS_JAVA_HOME.update(context()).ensureDirectory();
        NUTS_INSTALLER_BUILD_JAVA_HOME.update(context()).ensureDirectory();
        NUTS_GRAALVM_DIR.update(context()).ensureDirectory();
    }

    @Override
    public boolean configureFirst(NCmdLine cmdLine) {
        NArg c = cmdLine.peek().orNull();
        return false;
    }

    public InstallerRunner() {
        super();
    }


    @Override
    public void run() {
        NativeBuilder r = new NativeBuilder();
        r.setJpackageHome(NUTS_INSTALLER_BUILD_JAVA_HOME.update(context()).ensureDirectory().getValue());
        r.setGraalvmHome(NUTS_GRAALVM_DIR.update(context()).ensureDirectory().getValue());
        r.setJre8Linux64(INSTALLER_JRE8_LINUX64.update(context()).ensureRegularFile().getValue());
        r.setJre8Linux32(INSTALLER_JRE8_LINUX32.update(context()).ensureRegularFile().getValue());
        r.setJre8Windows64(INSTALLER_JRE8_WINDOWS64.update(context()).ensureRegularFile().getValue());
        r.setJre8Windows32(INSTALLER_JRE8_WINDOWS32.update(context()).ensureRegularFile().getValue());
        r.setJre8Mac64(INSTALLER_JRE8_MAC64.update(context()).ensureRegularFile().getValue());
        r.setVendor("thevpc");
        r.setCopyright("(c) 2018-"+ LocalDate.now().getYear() +" thevpc");
        r.setIcons(Arrays.asList(context().nutsRootFolder.resolve("documentation/media/nuts-icon.icns")));

        NPath sharedDistFolder = context().nutsRootFolder.resolve("installers/dist").resolve(Nuts.version().toString());
        NPath thevpcNutsVer = remoteTheVpcNutsPath().resolve(Nuts.version().toString());


//        NPath thevpcNutsVerWithSsh = NPath.of(NConnectionString.of(context().getRemoteTheVpcSshConnection().get()).builder().setProtocol("ssh").setPath(thevpcNutsVer.toString()).build());
//        NPathType type = thevpcNutsVerWithSsh.type();
        if (buildInstaller) {
            r.setSupported(NativeBuilder.PackageType.PORTABLE);
            if(buildNative){
                r.addSupported(NativeBuilder.PackageType.NATIVE,NativeBuilder.PackageType.BIN,NativeBuilder.PackageType.JRE_BUNDLE);
            }
            r.setMainClass("net.thevpc.nuts.installer.NutsInstaller");
            r.setProjectFolder(context().nutsRootFolder.resolve("installers/nuts-installer"), null, null);
            r.setDist(sharedDistFolder);
            r.setProfilingArgs(new String[]{"--build-native-profiling"});
            r.build();
            if (context().publish) {
                remoteMkdirs(thevpcNutsVer.toString());
//                thevpcNutsVerWithSsh.mkdirs();
                for (NPath nPath : r.getGeneratedFiles()) {
                    upload(nPath, thevpcNutsVer.resolve(nPath.name()).toString());
                }
                for (NPath nPath : r.getGeneratedDigestFiles()) {
                    upload(nPath, thevpcNutsVer.resolve(nPath.name()).toString());
                }
            }
        }

        if (buildBin) {
            r.setSupported(NativeBuilder.PackageType.PORTABLE);
            if(buildNative){
                r.addSupported(NativeBuilder.PackageType.NATIVE,NativeBuilder.PackageType.BIN,NativeBuilder.PackageType.JRE_BUNDLE);
            }
            r.setMainClass("net.thevpc.nuts.NutsApp");
            r.setProjectFolder(context().nutsRootFolder.resolve("core/nuts-app-full"), null, "nuts-app-full-"+ NWorkspace.of().runtimeId().version() +".jar");
            r.setDist(sharedDistFolder);
            r.setProfilingArgs(new String[]{"--sandbox","--verbose"});
            r.build();
            if (context().publish) {
                remoteMkdirs(thevpcNutsVer.toString());
//                thevpcNutsVerWithSsh.mkdirs();
                for (NPath nPath : r.getGeneratedFiles()) {
                    upload(nPath, thevpcNutsVer.resolve(nPath.name()).toString());
                }
                for (NPath nPath : r.getGeneratedDigestFiles()) {
                    upload(nPath, thevpcNutsVer.resolve(nPath.name()).toString());
                }
            }
        }

    }


}
