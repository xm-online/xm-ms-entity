package com.icthh.xm.ms.entity.config;

import com.icthh.xm.ms.entity.AbstractOracleIntTest;
import com.icthh.xm.ms.entity.AbstractPostgresIntTest;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

/**
 * Releases the resources shared by the database-backed integration tests as early as possible: once the last
 * test class extending a given base class has finished, its Spring context is closed and its container stopped,
 * so the rest of the suite does not run next to an idle Oracle or PostgreSQL and a second cached context.
 *
 * <p>Registered through {@code META-INF/services/org.junit.platform.launcher.TestExecutionListener}, so it applies
 * to Gradle and IDE runs alike. Everything is also released when the test plan ends, as a safety net.
 */
public class DatabaseTestResourcesListener implements TestExecutionListener {

    private static final Map<Class<?>, Runnable> RELEASERS = Map.of(
        AbstractOracleIntTest.class, AbstractOracleIntTest::releaseSharedResources,
        AbstractPostgresIntTest.class, AbstractPostgresIntTest::releaseSharedResources);

    private final Map<Class<?>, Set<UniqueId>> pendingClasses = new ConcurrentHashMap<>();

    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        pendingClasses.clear();
        testPlan.getRoots().stream()
            .flatMap(root -> testPlan.getDescendants(root).stream())
            .forEach(this::register);
    }

    private void register(TestIdentifier identifier) {
        testClass(identifier).ifPresent(testClass -> RELEASERS.keySet().stream()
            .filter(base -> base.isAssignableFrom(testClass))
            .forEach(base -> pendingClasses
                .computeIfAbsent(base, ignored -> ConcurrentHashMap.newKeySet())
                .add(identifier.getUniqueIdObject())));
    }

    @Override
    public void executionSkipped(TestIdentifier identifier, String reason) {
        classDone(identifier);
    }

    @Override
    public void executionFinished(TestIdentifier identifier, TestExecutionResult result) {
        classDone(identifier);
    }

    private void classDone(TestIdentifier identifier) {
        pendingClasses.forEach((base, pending) -> {
            if (pending.remove(identifier.getUniqueIdObject()) && pending.isEmpty()) {
                RELEASERS.get(base).run();
            }
        });
    }

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {
        RELEASERS.values().forEach(Runnable::run);
    }

    private static Optional<Class<?>> testClass(TestIdentifier identifier) {
        return identifier.getSource()
            .filter(ClassSource.class::isInstance)
            .map(ClassSource.class::cast)
            .map(ClassSource::getJavaClass);
    }
}
