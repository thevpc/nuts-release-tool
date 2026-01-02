/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.thevpc.nuts.build.base;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;

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
    public String remoteTheVpcSshUser = System.getProperty("user.name");
    public String remoteTheVpcSshHost = "thevpc.net";
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
        return NOptional.of(remoteTheVpcSshHost);
    }

    public void setRemoteTheVpcSshHost(String remoteTheVpcSshHost) {
        this.remoteTheVpcSshHost = remoteTheVpcSshHost;
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
        return remoteTheVpcSshUser;
    }

    public void setRemoteTheVpcSshUser(String remoteUser) {
        this.remoteTheVpcSshUser = remoteUser;
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

    public void setVar(String key, String value) {
        this.vars.put(key, value);
    }

    public void loadConfig(NCmdLine cmdLine) {
        if (this.confFileTson.isRegularFile()) {
            NOut.println(NMsg.ofC("loadConfig %s", this.confFileTson));
            confRoot = NElementParser.ofTson().parse(confFileTson);

        }
    }
}
