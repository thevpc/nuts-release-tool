package net.thevpc.nuts.build.util;

import net.thevpc.nuts.artifact.NDescriptor;
import net.thevpc.nuts.artifact.NDescriptorParser;
import net.thevpc.nuts.artifact.NDescriptorStyle;
import net.thevpc.nuts.util.NIllegalArgumentException;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.util.NMsg;
import net.thevpc.nuts.util.NOptional;

public class NReleaseUtils {
    public static void ensureNutsRepoFolder(NPath currentDir) {
        if (!isNutsRepoFolder(currentDir)) {
            throw new NIllegalArgumentException(NMsg.ofC("you must run release tool under nuts github repository root folder. Now we are under %s", currentDir));
        }
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
