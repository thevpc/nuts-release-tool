/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.thevpc.nuts.build.base;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;

import net.thevpc.nuts.build.builders.BaseConfRunner;
import net.thevpc.nuts.build.util.NReleaseUtils;
import net.thevpc.nuts.elem.*;
import net.thevpc.nuts.io.NOut;
import net.thevpc.nuts.cmdline.NCmdLine;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.util.NFunction;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.nuts.util.NOptional;

/**
 * @author vpc
 */
public class NutsBuildRunnerContext {

    public Map<String, String> vars = new HashMap<>();
    public String home = System.getProperty("user.home");
    public NPath nutsRootFolder;
    public boolean publish;
    public boolean buildSite = false;

    public NPath websiteProjectFolder;
    public NPath repositoryProjectFolder;
    public NPath confFileTson;
    public String nutsDebugArg = null;

    public String nutsLtsApiVersion = null;
    public String nutsLtsAppVersion = null;
    public String nutsLtsRuntimeVersion = null;
    private String remoteTheVpcSshConnection;
    public NElement confRoot;

    public boolean verbose = false;
    public boolean trace = false;

    public NOptional<String> getRemoteTheVpcSshHost() {
        return getVar("REMOTE_NUTS_THEVPC_DEPLOY_SERVER");
    }

    public NOptional<String> getRemoteTheVpcSshConnection() {
        return NOptional.of(remoteTheVpcSshConnection).orElseGetOptionalFrom(
                () -> {
                    String s = getRemoteTheVpcSshHost().orElse(System.getProperty("user.home"))
                            + ":"
                            + getRemoteTheVpcSshHost().orElse("thevpc.net");
                    return NOptional.of(s);
                }
        );
    }

    public void setRemoteTheVpcSshConnection(String remoteTheVpcSshConnection) {
        this.remoteTheVpcSshConnection = remoteTheVpcSshConnection;
    }

    public String getRemoteTheVpcSshUser() {
        return getVar("REMOTE_NUTS_THEVPC_DEPLOY_USER").get();
    }

    public Function<String, Object> varMapper() {
        return new NFunction<String, Object>() {
            @Override
            public Object apply(String s) {
                if (vars.containsKey(s)) {
                    return vars.get(s);
                }
                Properties p = System.getProperties();
                Object r = p.get(s);
                if (r != null) {
                    return r;
                }
                switch (s) {
                    case "remoteTheVpcSshUser": {
                        return getRemoteTheVpcSshUser();
                    }
                    case "stableApiVersion": {
                        return nutsLtsApiVersion;
                    }
                    case "stableAppVersion": {
                        return nutsLtsAppVersion;
                    }
                    case "stableRuntimeVersion": {
                        return nutsLtsRuntimeVersion;
                    }
                    case "root":
                    case "rootFolder":
                    case "nutsRootFolder": {
                        return nutsRootFolder;
                    }
                    case "websiteProjectFolder": {
                        return websiteProjectFolder;
                    }
                    case "repositoryProjectFolder": {
                        return repositoryProjectFolder;
                    }
                }
                return null;
            }
        };
    }

    public NOptional<String> getVar(String key) {
        return NOptional.ofNamed(vars.get(key),key);
    }

    public void setVar(String key, String value) {
        this.vars.put(key, value);
    }

    public Map<String, NElement> loadConfigNamedPairs() {
        return NReleaseUtils.asNamedPairs(confRoot);
    }

    public void loadConfig(NCmdLine cmdLine) {
        if (this.confFileTson.isRegularFile()) {
            NOut.println(NMsg.ofC("loadConfig %s", this.confFileTson));
            confRoot = NElementReader.ofTson().read(confFileTson);
            NPath local = this.confFileTson.resolveSibling(BaseConfRunner.NUTS_RELEASE_CONF_TSON_LOCAL);
            if(local.isRegularFile()){
                NElement confRoot2 = NElementReader.ofTson().read(local);
                NElementBuilder confRootBuilder = confRoot.builder();
                if(confRootBuilder.type()==NElementType.FRAGMENT){
                    NFragmentElementBuilder confRootBuilderF=(NFragmentElementBuilder) confRootBuilder;
                    if(confRoot2.type()==NElementType.FRAGMENT){
                        confRootBuilderF.addAll(confRoot2.asFragment().get().children());
                    }else if(confRoot2.type()==NElementType.OBJECT){
                        confRootBuilderF.addAll(confRoot2.asObject().get().children());
                    }
                }else if(confRootBuilder.type()==NElementType.OBJECT){
                    NObjectElementBuilder confRootBuilderF=(NObjectElementBuilder) confRootBuilder;
                    if(confRoot2.type()==NElementType.FRAGMENT){
                        confRootBuilderF.addAll(confRoot2.asFragment().get().children());
                    }else if(confRoot2.type()==NElementType.OBJECT){
                        confRootBuilderF.addAll(confRoot2.asObject().get().children());
                    }
                }
                confRoot=confRootBuilder.build();
            }
        }
    }
}
