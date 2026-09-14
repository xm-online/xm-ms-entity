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
 * test class extending any of the database base classes has finished, the Spring contexts are closed and the
 * containers stopped, so the rest of the suite (the H2-backed classes) does not run next to an idle Oracle or
 * PostgreSQL and two more cached contexts.
 *
 * <p>The contexts are released together, not per database. JPA entity listeners such as
 * {@code XmEntityElasticSearchListener} reach their Spring beans through static fields that the most recently
 * refreshed context overwrites, so closing the Oracle context while PostgreSQL classes are still pending would
 * make those classes call into a closed context.
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
        boolean removed = false;
        for (Set<UniqueId> pending : pendingClasses.values()) {
            removed |= pending.remove(identifier.getUniqueIdObject());
        }
        if (removed && pendingClasses.values().stream().allMatch(Set::isEmpty)) {
            RELEASERS.values().forEach(Runnable::run);
        }
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
