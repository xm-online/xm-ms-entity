package com.icthh.xm.ms.entity;

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

    private static String PROJECT_ROOT = Paths.get("").toAbsolutePath().toString();
    private static String XM_REPOSITORY_HOME;

    private static final String XM_MS_NAME = "entity";

    private static final String XM_REPOSITORY_TENANTS = "config/tenants";
    private static final String XM_REPOSITORY_MS_TEST = XM_MS_NAME + "/test";
    private static final String XM_REPOSITORY_MS_LEP = XM_MS_NAME + "/lep";

    private static final String LEP_TEST_HOME = "src/test/lep";
    private static final String LEP_SCRIPT_HOME = "src/main/lep";

    private static final String LEP_TEST_EXISTS_REGEX = ".*tenants/.*/" + XM_MS_NAME + "/test";

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
            .filter(path -> path.toString().matches(LEP_TEST_EXISTS_REGEX))
            .peek(path -> System.out.println("Found tests for tenant: " + SymLink.extractTenant(path)))
            .flatMap(SymLink::of)
            .filter(symLink -> !Files.exists(symLink.from))
            .peek(SymLink::createSymLink)
            .count();

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
                       .replaceAll(".*/tenants/", "")
                       .replaceAll("/.*", "");
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
