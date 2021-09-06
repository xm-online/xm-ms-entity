package com.icthh.xm.ms.entity;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

/**
 * Utility class which scans XM_REPOSITORY_HOME for tests and add all needed symlinks to run those tests.
 * <p/>
 * Class is used as script by CI pipeline, so MUST NOT not contains any external dependencies.
 */
public class LepTestLinkScanner {

    private static String SEPARATOR = File.separator;

    private static String PROJECT_ROOT = Paths.get("").toAbsolutePath().toString();
    private static String XM_REPOSITORY_HOME;

    private static final String XM_MS_NAME = "entity";

    private static final String XM_REPOSITORY_TENANTS = "config".concat(SEPARATOR).concat("tenants");
    private static final String XM_REPOSITORY_MS_TEST = XM_MS_NAME.concat(SEPARATOR).concat("test");
    private static final String XM_REPOSITORY_MS_LEP = XM_MS_NAME.concat(SEPARATOR).concat("lep");
    private static final String XM_COMMONS_MS_LEP = "commons".concat(SEPARATOR).concat("lep");

    private static final String LEP_TEST_HOME = "src".concat(SEPARATOR).concat("test").concat(SEPARATOR).concat("lep");
    private static final String LEP_SCRIPT_HOME = "src".concat(SEPARATOR).concat("main").concat(SEPARATOR).concat("lep");

    private enum RegexpPatterns {
        WIN(".*tenants\\\\.*\\\\%s\\\\test", ".*\\\\tenants\\\\", "\\\\.*"),
        LINUX(".*tenants/.*/%s/test",".*/tenants/", "/.*");

        RegexpPatterns(String testRegexp, String pattern1, String pattern2) {
            this.testRegexp = testRegexp;
            this.pattern1 = pattern1;
            this.pattern2 = pattern2;
        }

        private String testRegexp;
        private String pattern1;
        private String pattern2;

        private String testExistPattern(String service) {
            return String.format(this.testRegexp, service);
        }

        private static RegexpPatterns currentOs() {
            if (System.getProperty("os.name").toUpperCase().contains("WIN")) {
                return WIN;
            }
            return LINUX;
        }

    }

    /**
     * Entry point.
     * @param args
     *          0 - XM_REPOSITORY_HOME
     * @throws IOException in case of error occurs
     */
    public static void main(String[] args) throws IOException {

        if (args.length != 1) {
            System.err.println("please specify input arguments: <XM_REPOSITORY_HOME>");
            return;
        }

        XM_REPOSITORY_HOME = args[0];

        System.out.println("PROJECT_ROOT = " + PROJECT_ROOT);
        System.out.println("XM_REPOSITORY_HOME = " + XM_REPOSITORY_HOME);

        long count = Files
            .walk(Paths.get(XM_REPOSITORY_HOME))
            .filter(path -> path.toString().matches(RegexpPatterns.currentOs().testExistPattern(XM_MS_NAME)))
            .peek(path -> System.out.println("Found tests for tenant: " + SymLink.extractTenant(path)))
            .flatMap(SymLink::of)
            .filter(symLink -> !Files.exists(symLink.from))
            .peek(SymLink::createSymLink)
            .count();

        Path envCommonsPath = Paths.get(XM_REPOSITORY_HOME, XM_REPOSITORY_TENANTS, XM_COMMONS_MS_LEP);
        if (Files.exists(envCommonsPath)) {
            System.out.println("Create env commons link");
            new SymLink(
                    Paths.get(PROJECT_ROOT, LEP_SCRIPT_HOME, XM_COMMONS_MS_LEP),
                    envCommonsPath
            ).createSymLink();
            count++;
        }

        System.out.println("created links count: " + count);
    }

    public static class SymLink {
        Path from;
        Path to;

        SymLink(Path from, Path to) {
            this.from = from;
            this.to = to;
        }

        static Stream<SymLink> of(Path testsPath) {
            return Stream.of(createLink(testsPath, LEP_TEST_HOME, XM_REPOSITORY_MS_TEST),
                             createLink(testsPath, LEP_SCRIPT_HOME, XM_REPOSITORY_MS_LEP));
        }

        private static SymLink createLink(Path testsPath, String srcHome, String repoMsPath) {

            String tenant = extractTenant(testsPath);

            Path from = Paths.get(PROJECT_ROOT, srcHome, tenant, repoMsPath);
            Path to = Paths.get(XM_REPOSITORY_HOME, XM_REPOSITORY_TENANTS, tenant, repoMsPath);

            return new SymLink(from, to);
        }

        private static String extractTenant(Path path) {
            return path.toString()
                       .replaceAll(RegexpPatterns.currentOs().pattern1, "")
                       .replaceAll(RegexpPatterns.currentOs().pattern2, "");
        }

        void createSymLink() {
            try {
                Path parent = from.getParent();
                if (!parent.toFile().exists()) {
                    System.out.println("create directory: " + parent);
                    Files.createDirectories(parent);
                }
                System.out.println("Create link: " + this);
                Files.createSymbolicLink(from, to);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public String toString() {
            return from + " --> " + to;
        }
    }

}
