package com.icthh.xm.ms.entity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

/**
 * Utility class which scans XM_REPOSITORY_HOME for tests and add all needed symlinks to run those tests.
 * <p/>
 * Candidate to use as simple script by built pipeline thus does ton contains any external dependencies.
 */
public class LepTestLinkScanner {

    private static String PROJECT_ROOT = Paths.get("").toAbsolutePath().toString();
    private static String XM_REPOSITORY_HOME;

    private static final String XM_REPOSITORY_TENANTS = "config/tenants";
    private static final String XM_REPOSITORY_ENTITY_TEST = "entity/test";
    private static final String XM_REPOSITORY_ENTITY_LEP = "entity/lep";

    private static final String LEP_TEST_HOME = "src/test/groovy/com/icthh/xm/lep/tenant";
    private static final String LEP_SCRIPT_HOME = "src/test/resources/lep/custom";

    private static final String LEP_TEST_EXISTS_REGEX = ".*tenants/.*/entity/test";

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
            return Stream.of(createTestLink(testsPath), createLepLink(testsPath));
        }

        private static SymLink createTestLink(Path testsPath) {

            String tenant = extractTenant(testsPath);

            Path from = Paths.get(PROJECT_ROOT, LEP_TEST_HOME, tenant.toLowerCase());
            Path to = Paths.get(XM_REPOSITORY_HOME, XM_REPOSITORY_TENANTS, tenant, XM_REPOSITORY_ENTITY_TEST);

            return new SymLink(from, to);

        }

        private static SymLink createLepLink(Path testsPath) {

            String tenant = extractTenant(testsPath);

            Path from = Paths.get(PROJECT_ROOT, LEP_SCRIPT_HOME, tenant.toLowerCase());
            Path to = Paths.get(XM_REPOSITORY_HOME, XM_REPOSITORY_TENANTS, tenant, XM_REPOSITORY_ENTITY_LEP);

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
