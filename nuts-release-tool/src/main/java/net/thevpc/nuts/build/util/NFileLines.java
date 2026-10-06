package net.thevpc.nuts.build.util;

import net.thevpc.nuts.io.NIOException;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.nuts.util.NIllegalArgumentException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class NFileLines {
    private static final String VAR_OPEN = "(?<var>";

    private NFileLines() {
    }

    /**
     * Replaces, in the FIRST line that contains a match of {@code pattern}, only the text captured by the
     * {@code (?<var>...)} named group with {@code newValue}. The rest of the line is left untouched.
     * <ul>
     *   <li>{@code pattern} is a regular java.util.regex; the line only needs to CONTAIN a match (find())</li>
     *   <li>it must contain exactly one {@code (?<var>...)} named group</li>
     *   <li>only the first matching line is modified (later matches are left untouched)</li>
     *   <li>throws if no line matches</li>
     *   <li>preserves LF / CRLF / CR terminators and the trailing newline; no write if nothing changes</li>
     * </ul>
     * Example: {@code replaceLine(f, "String NUTS_BOOT_VERSION = \"(?<var>[0-9.-]+)\";", "1.2.0")}
     *
     * @param newValue literal replacement text (no regex escaping needed, no line break)
     */
    public static void replaceLine(NPath file, String pattern, String newValue) {
        if (file == null) {
            throw new NIllegalArgumentException(NMsg.ofP("missing file"));
        }
        if (pattern == null || pattern.isEmpty()) {
            throw new NIllegalArgumentException(NMsg.ofP("missing pattern"));
        }
        if (newValue == null) {
            throw new NIllegalArgumentException(NMsg.ofP("missing new value"));
        }
        if (newValue.indexOf('\n') >= 0 || newValue.indexOf('\r') >= 0) {
            throw new NIllegalArgumentException(NMsg.ofP("new value must not contain line breaks"));
        }
        checkVarGroup(pattern);
        Pattern p;
        try {
            p = Pattern.compile(pattern);
        } catch (PatternSyntaxException ex) {
            throw new NIllegalArgumentException(NMsg.ofC("invalid pattern '%s': %s", pattern, ex.getMessage()));
        }
        if (!file.isRegularFile()) {
            throw new NIOException(NMsg.ofC("file not found or not a regular file: %s", file));
        }

        String content = file.readString();
        int n = content.length();
        int pos = 0;
        while (pos < n) {
            int eol = pos;
            while (eol < n && content.charAt(eol) != '\n' && content.charAt(eol) != '\r') {
                eol++;
            }
            int next = eol;
            if (next < n) {
                if (content.charAt(next) == '\r' && next + 1 < n && content.charAt(next + 1) == '\n') {
                    next += 2;
                } else {
                    next++;
                }
            }
            Matcher m = p.matcher(content.substring(pos, eol));
            if (m.find() && m.start("var") >= 0) {
                String updated = content.substring(0, pos + m.start("var"))
                        + newValue
                        + content.substring(pos + m.end("var"));
                if (!updated.equals(content)) {
                    file.writeString(updated);
                }
                return;
            }
            pos = next;
        }
        throw new NIllegalArgumentException(NMsg.ofC("no line matching /%s/ in %s", pattern, file));
    }

    /** Example: set NUTS_BOOT_VERSION in NBootWorkspace.java, keeping everything else on the line. */
    public static void setBootVersion(NPath nBootWorkspaceJava, String version) {
        if (version == null || !version.matches("[0-9A-Za-z.+_-]+")) {
            throw new NIllegalArgumentException(NMsg.ofC("invalid version: %s", version));
        }
        replaceLine(nBootWorkspaceJava, "String NUTS_BOOT_VERSION = \"(?<var>[0-9.-]+)\";", version);
    }

    /** Checks the pattern contains exactly one (?<var>...) named group. */
    private static void checkVarGroup(String pattern) {
        int count = 0;
        for (int i = pattern.indexOf(VAR_OPEN); i >= 0; i = pattern.indexOf(VAR_OPEN, i + 1)) {
            if (!isEscaped(pattern, i)) {
                count++;
            }
        }
        if (count != 1) {
            throw new NIllegalArgumentException(
                    NMsg.ofC("pattern must contain exactly one %s...) group (found %s): %s", VAR_OPEN, count, pattern));
        }
    }

    private static boolean isEscaped(String s, int index) {
        int backslashes = 0;
        for (int k = index - 1; k >= 0 && s.charAt(k) == '\\'; k--) {
            backslashes++;
        }
        return backslashes % 2 == 1;
    }

}