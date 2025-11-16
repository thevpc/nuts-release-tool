package net.thevpc.nuts.build.util;

import net.thevpc.nuts.artifact.NDescriptor;
import net.thevpc.nuts.artifact.NDescriptorParser;
import net.thevpc.nuts.artifact.NDescriptorStyle;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.elem.NElementType;
import net.thevpc.nuts.elem.NOperatorElement;
import net.thevpc.nuts.elem.NPairElement;
import net.thevpc.nuts.util.NIllegalArgumentException;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.nuts.util.NOptional;

import java.util.LinkedHashMap;
import java.util.Map;

public class NReleaseUtils {
    public static void ensureNutsRepoFolder(NPath currentDir) {
        if (!isNutsRepoFolder(currentDir)) {
            throw new NIllegalArgumentException(NMsg.ofC("you must run release tool under nuts github repository root folder. Now we are under %s", currentDir));
        }
    }

    public static Map<String, NElement> asNamedPairs(NElement elems) {
        LinkedHashMap<String, NElement> pars = new LinkedHashMap<>();
        if (elems != null && elems.asObject().isPresent()) {
            for (NElement c : elems.asObject().get().children()) {
                if (c.isNamedPair()) {
                    NPairElement p = c.asPair().get();
                    String key = p.key().asStringValue().get();
                    pars.put(key, p.value());
                } else if (
                        c.isBinaryOperator(NElementType.OP_EQ)
                                || c.isBinaryOperator(NElementType.OP_COLON_EQ)
                ) {
                    NOperatorElement o = c.asOperator().get();
                    NElement f = o.first().get();
                    NElement s = o.second().get();
                    if (f.isAnyString()) {
                        pars.put(f.asStringValue().get(), s);
                    }
                }
            }
        }
        return  pars;
    }

    public static boolean isNutsRepoFolder(NPath currentDir) {
        if (!currentDir.isDirectory()) {
            return false;
        }
        if (!currentDir.resolve("pom.xml").isRegularFile()) {
            return false;
        }
        NOptional<NDescriptor> desc = NDescriptorParser.of().setDescriptorStyle(NDescriptorStyle.MAVEN).parse(currentDir.resolve("pom.xml"));
        if (!desc.isPresent()) {
            return false;
        }
        if (!desc.get().getId().getArtifactId().equals("nuts-community-builder")) {
            return false;
        }
        if (!currentDir.resolve("documentation/website").isDirectory()) {
            return false;
        }
        if (!currentDir.resolve("documentation/repo").isDirectory()) {
            return false;
        }
        return true;
    }
}
