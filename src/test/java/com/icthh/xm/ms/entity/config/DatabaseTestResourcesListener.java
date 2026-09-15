package com.icthh.xm.ms.entity.config;

import com.icthh.xm.ms.entity.AbstractOracleIntTest;
import com.icthh.xm.ms.entity.AbstractPostgresIntTest;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;

/**
 * Releases the resources shared by the database-backed integration tests when the test plan ends: the Spring
 * contexts are closed and the containers stopped, so the JVM shuts down without waiting for Ryuk and the
 * context cache shutdown hook.
 *
 * <p>The release deliberately happens only at the end of the plan, never after the last database class. JPA
 * entity listeners such as {@code XmEntityElasticSearchListener} reach their Spring beans through static fields
 * that the most recently refreshed context overwrites, so closing a database context while H2-backed classes
 * are still pending makes those classes call into a closed context. {@link DatabaseClassesLastOrderer} runs the
 * database classes last, which gives the rest of the suite the same benefit without that hazard.
 *
 * <p>Registered through {@code META-INF/services/org.junit.platform.launcher.TestExecutionListener}, so it applies
 * to Gradle and IDE runs alike.
 */
public class DatabaseTestResourcesListener implements TestExecutionListener {

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {
        AbstractPostgresIntTest.releaseSharedResources();
        AbstractOracleIntTest.releaseSharedResources();
    }
}
