package net.thevpc.nuts.build.util;

import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.elem.NElementReader;
import net.thevpc.nuts.elem.NPairElement;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.platform.NStoreType;

import java.util.*;

public class NopsEnv {
    public static Map<String, String> getNopsEnv() {
        Map<String, String> map = new LinkedHashMap<>();
        for (NPath p : new NPath[]{
                NPath.ofUserHome().resolve("env-world/env/_install/"),
                NPath.ofUserHome().resolve("env-world/env/"),
                NPath.ofUserStore(NStoreType.CONF).resolve("nops"),
                NPath.ofUserHome()
        }) {
            p = p.resolve(".nops.tson");
            if (p.isRegularFile()) {
                List<NPath> confs = new ArrayList<>();
                for (NPath nPath : p.getParent().list()) {
                    if (nPath.getName().endsWith(".nops.tson")) {
                        confs.add(nPath);
                    }
                }
                confs.sort(Comparator.comparing(x -> x.getName()));
                for (NPath conf : confs) {
                    NElement parsed = NElementReader.ofTson().read(conf);
                    class InternalDoer {
                        void processEnv(NElement env){
                            for (NElement nElement : env.asObject().get().children()) {
                                if (nElement.isNamedPair()) {
                                    NPairElement pair = nElement.asPair().get();
                                    String k = pair.key().asStringValue().orNull();
                                    String v = pair.value().asStringValue().orNull();
                                    if (k != null && v != null) {
                                        map.put(k, v);
                                    }
                                }
                            }
                        }
                    }
                    InternalDoer doer = new InternalDoer();
                    if (parsed.isNamedObject("env")) {
                        doer.processEnv(parsed);
                    }else if(parsed.isObject()) {
                        for (NElement child : parsed.asObject().get().children()) {
                            if (child.isNamedObject("env")) {
                                doer.processEnv(child);
                            }
                        }
                    }
                }
            }
        }
        return map;
    }
}
