/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.thevpc.nuts.build.builders;

import net.thevpc.nuts.Nuts;
import net.thevpc.nuts.artifact.NVersion;
import net.thevpc.nuts.build.util.AbstractRunner;
import net.thevpc.nuts.build.util.NReleaseUtils;
import net.thevpc.nuts.cmdline.NArg;
import net.thevpc.nuts.cmdline.NCmdLine;
import net.thevpc.nuts.command.NExecCmd;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.io.NIOException;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.nuts.util.*;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author vpc
 */
public class CompatRunner extends AbstractRunner {

    TreeSet<NVersion> allVersions = new TreeSet<>();
    boolean buildCompat = false;

    public CompatRunner() {
        super();
    }


    @Override
    public void configureBeforeOptions(NCmdLine cmdLine) {
        for (Map.Entry<String, NElement> e : NReleaseUtils.asNamedPairs(context().confRoot.asObject().orNull()).entrySet()) {
            switch (e.getKey()) {
                case "build-compat": {
                    buildCompat = e.getValue().asBooleanValue().orElse(false);
                    break;
                }
                case "all-versions": {
                    if (e.getValue().isAnyString()) {
                        for (String v : NStringUtils.split(e.getValue().asStringValue().get(), ",;", true, true)) {
                            allVersions.add(NVersion.of(v));
                        }
                    } else if (e.getValue().isAnyArray()) {
                        for (NElement child : e.getValue().asArray().get().children()) {
                            if (child.isAnyString()) {
                                for (String v : NStringUtils.split(child.asStringValue().get(), ",;", true, true)) {
                                    allVersions.add(NVersion.of(v));
                                }
                            }
                        }
                    } else if (e.getValue().isAnyObject()) {
                        for (NElement child : e.getValue().asArray().get().children()) {
                            if (child.isAnyString()) {
                                for (String v : NStringUtils.split(child.asStringValue().get(), ",;", true, true)) {
                                    allVersions.add(NVersion.of(v));
                                }
                            }
                        }
                    }
                    break;
                }
            }
        }
        if (allVersions.isEmpty()) {
            for (String v : new String[]{
                    "0.8.0",
                    "0.8.1",
                    "0.8.2",
                    "0.8.3",
                    "0.8.4",
                    "0.8.5",
                    "0.8.6",
                    "0.8.7",
                    "0.8.8"
            }) {
                allVersions.add(NVersion.of(v));
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
        if (buildCompat) {
            runCompat();
        }
    }

    private void runCompat() {
        echoV("**** $v (nuts)...", NMaps.of("v", NMsg.ofStyledKeyword("build-compat")));
        NVersion[] allVersionsArray = allVersions.toArray(new NVersion[0]);
        Map<String, Double> compatMap = new HashMap<>();
        for (int i = 0; i < allVersionsArray.length; i++) {
            for (int j = i + 1; j < allVersionsArray.length; j++) {
                String compatId = allVersionsArray[i] + "_" + allVersionsArray[j];
                compatMap.put(
                        compatId,
                        runCompat(allVersionsArray[i], allVersionsArray[j],
                                allVersionsArray[i].equals(Nuts.getVersion())
                                        || allVersionsArray[j].equals(Nuts.getVersion())
                        )
                );
            }
        }
        generateCompatFile(compatMap);
    }

    private void generateCompatFile(Map<String, Double> compatMap) {
        echoV("**** $n $v...", NMaps.of("n", NMsg.ofStyledPrimary4("nuts"), "v", NMsg.ofStyledKeyword("build-compat-matrix")));
        NVersion[] allVersionsArray = allVersions.toArray(new NVersion[0]);
        NPath file = context().websiteProjectFolder.resolve("src/main/compat.html");
        String title = "Nuts API Compatibility Matrix";
        try (PrintStream out = file.mkParentDirs().getPrintStream()) {
            out.println("<!DOCTYPE html>\n" +
                    "<html lang=\"en\">\n" +
                    "<head>\n" +
                    "    <meta charset=\"UTF-8\">\n" +
                    "    <meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\">\n" +
                    "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1, minimum-scale=1.0, shrink-to-fit=no\">\n" +
                    "    <link href=\"assets/images/favicon.ico\" rel=\"icon\"/>\n" +
                    "    <title>" + title + "</title>\n" +
                    "    <meta name=\"description\" content=\"Nuts Java Package Manager\">\n" +
                    "    <meta name=\"author\" content=\"thevpc\">\n" +
                    "    <!-- Bootstrap -->\n" +
                    "    <link rel=\"stylesheet\" type=\"text/css\" href=\"assets/vendor/bootstrap/css/bootstrap.min.css\"/>\n" +
                    "    <!-- Font Awesome Icon -->\n" +
                    "    <link rel=\"stylesheet\" type=\"text/css\" href=\"assets/vendor/font-awesome/css/all.min.css\"/>\n" +
                    "    <!-- Magnific Popup -->\n" +
                    "    <link rel=\"stylesheet\" type=\"text/css\" href=\"assets/vendor/magnific-popup/magnific-popup.min.css\"/>\n" +
                    "    <!-- Highlight Syntax -->\n" +
                    "    <link rel=\"stylesheet\" type=\"text/css\" href=\"assets/vendor/highlight.js/styles/github.css\"/>\n" +
                    "    <!-- Custom Stylesheet -->\n" +
                    "    <link rel=\"stylesheet\" type=\"text/css\" href=\"assets/css/stylesheet-landing.css\"/>\n" +
                    "    <link rel=\"stylesheet\" type=\"text/css\" href=\"assets/css/caroussel.css\"/>\n" +
                    "\n" +
                    "    <style>\n" +
                    "        table { border-collapse: collapse; width: 80%; margin: 20px auto; }\n" +
                    "        th, td { border: 1px solid #333; padding: 8px; text-align: center; }\n" +
                    "        th { background: #555; color: #fff; }\n" +
                    "        td a { text-decoration: none; color: #0066cc; }\n" +
                    "        td.compatible { background: #cfc; }\n" +
                    "        td.incompatible { background: #fcc; }\n" +
                    "        td.na { background: #ccc; }\n" +
                    "\n" +
                    "        /* hide text inside the link but keep the element there */\n" +
                    "        td.disabled a {\n" +
                    "            color: transparent;          /* hide the text */\n" +
                    "            text-decoration: none;       /* remove underline */\n" +
                    "        }\n" +
                    "\n" +
                    "    </style>\n" +
                    "</head>\n"
            );

            out.println("<body>");
            out.println("<h1 style=\"text-align:center;\">" + title + "</h1>");
            out.println("<table>");

            out.println("  <tr>");
            out.println("    <th>From \\ To</th>");
            for (int i = 0; i < allVersionsArray.length; i++) {
                out.println("    <th>" + allVersionsArray[i] + "</th>");
            }
            out.println("  </tr>");

            for (int i = 0; i < allVersionsArray.length; i++) {
                out.println("  <tr>");
                out.println("    <th>" + allVersionsArray[i] + "</th>");
                for (int j = 0; j < allVersionsArray.length; j++) {
                    if (j <= i) {
                        out.println("    <td class=\"na\">-</td>");
                    } else {
                        int finalI = i;
                        int finalJ = j;
                        String compId = allVersionsArray[finalI].toString() + "_" + allVersionsArray[finalJ].toString();
                        Double d = NOptional.of(compatMap.get(compId)).orElse(1.0);
                        String bg = background(d);
                        String fg = foregroundForBg(bg);

                        out.println(NMsg.ofV("  <td style=\"background:" + bg + ";color:" + fg + "\"><a style=\"color:" + fg + ";text-decoration:none;\" href=\"compat_reports/nuts/${i}_to_${j}/compat_report.html\" target=\"_blank\">${i}→${j} ${comp}</a></td>", v -> {
                            switch (v) {
                                case "i":
                                    return allVersionsArray[finalI].toString();
                                case "j":
                                    return allVersionsArray[finalJ].toString();
                                case "comp": {
                                    if (d == null) {
                                        return "";
                                    }
                                    if (d.equals(1.0)) {
                                        return "";
                                    }
                                    return "(" + new DecimalFormat("0.00%").format(d) + ")";
                                }
                            }
                            return null;
                        }));
                    }
                }
                out.println("  </tr>");
            }
            out.println("</table>");
            out.println("</body>");
            out.println("</html>");
        }
    }

    // Soft red → soft green, nicer than pure #FF0000 → #00FF00
    private static final NColor BAD = NColor.of32(255, 80, 80);
    private static final NColor GOOD = NColor.of32(80, 200, 80);

    public static String background(double comp) {
        comp = Math.max(0, Math.min(1, comp)); // clamp

        int r = (int) (BAD.getRed() + (GOOD.getRed() - BAD.getRed()) * comp);
        int g = (int) (BAD.getGreen() + (GOOD.getGreen() - BAD.getGreen()) * comp);
        int b = (int) (BAD.getBlue() + (GOOD.getBlue() - BAD.getBlue()) * comp);

        return String.format("#%02x%02x%02x", r, g, b);
    }

    public static String foregroundForBg(String bgHex) {
        Color c = Color.decode(bgHex);

        // WCAG-ish brightness formula
        int brightness = (c.getRed() * 299 + c.getGreen() * 587 + c.getBlue() * 114) / 1000;

        return (brightness < 128) ? "#ffffff" : "#000000";
    }

    private double runCompat(NVersion from, NVersion to, boolean force) {
        NPath reportPath = context().websiteProjectFolder.resolve(
                NMsg.ofV("src/main/compat_reports/nuts/${i}_to_${j}/compat_report.html", v -> {
                    switch (v) {
                        case "i":
                            return from.toString();
                        case "j":
                            return to.toString();
                    }
                    return null;
                }).toString()
        );
        if (!force) {
            if (reportPath.isRegularFile()) {
                echoV("**** reload $v $s1→$s2 (nuts)...", NMaps.of("v", NMsg.ofStyledKeyword("build-compat"), "s1", from, "s2", to));
                return parseCompatibility(reportPath);
            }
        }
        echoV("**** $v $s1→$s2 (nuts)...", NMaps.of("v", NMsg.ofStyledKeyword("build-compat"), "s1", from, "s2", to));
        // /documentation/website/src/main/
        NExecCmd e = NExecCmd.ofSystem(
                        "/usr/bin/japi-compliance-checker",
                        NPath.ofUserHome().resolve(NMsg.ofV(".m2/repository/net/thevpc/nuts/nuts/$v1/nuts-$v1.jar", v -> from.toString()).toString()).toString(),
                        NPath.ofUserHome().resolve(NMsg.ofV(".m2/repository/net/thevpc/nuts/nuts/$v1/nuts-$v1.jar", v -> to.toString()).toString()).toString()
                )
                .setDirectory(context().websiteProjectFolder.resolve("src/main"));
        String sout = e.getGrabbedAllString();
        int code = e.getResultCode();
        if (code == 0) {
            // no change
            return 1;
        } else if (code == 1) {
            // some changes

            return parseCompatibility(reportPath);
        } else {
            throw new NException(NMsg.ofC("unexpected result %s", code));
        }
    }

    double parseCompatibility(NPath from) {
        String comptaClass = null;
        double comptaValue = 0.0;
        try (BufferedReader br = from.getBufferedReader()) {
            while (true) {
                String line = br.readLine();
                if (line == null) {
                    break;
                }
                if (line.startsWith("<tr><th>Compatibility</th>")) {
                    line = br.readLine();
                    if (line == null) {
                        break;
                    }
                    Pattern vv = Pattern.compile("<td class='(?<c>[^']+)'>(?<v>[^%<]+)%</td>");
                    Matcher m = vv.matcher(line);
                    if (m.matches()) {
                        String c = m.group("c");
                        String v = m.group("v");
                        comptaValue = Double.parseDouble(v) * 0.01;
                    }
                    break;
                }
            }
        } catch (IOException e) {
            throw new NIOException(e);
        }
        return comptaValue;
    }
}
