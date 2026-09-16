package com.icthh.xm.ms.entity.config;

import com.icthh.xm.ms.entity.AbstractOracleIntTest;
import com.icthh.xm.ms.entity.AbstractPostgresIntTest;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.ClassOrdererContext;

/**
 * Runs the test classes backed by a real database (PostgreSQL or Oracle containers) after every other class,
 * keeping the relative order of all other classes unchanged.
 *
 * <p>Two reasons. The H2 part of the suite, which is most of it, then runs without an idle container and two
 * more cached Spring contexts next to it. And the database contexts can be closed as soon as the plan ends
 * without breaking anybody: JPA entity listeners such as {@code XmEntityElasticSearchListener} reach their
 * Spring beans through static fields that the most recently refreshed context overwrites, so a class that
 * saves entities after a database context has been closed would call into that closed context.
 *
 * <p>Registered through {@code junit-platform.properties}, so it applies to Gradle and IDE runs alike.
 */
public class DatabaseClassesLastOrderer implements ClassOrderer {

    private static final List<Class<?>> DATABASE_BASES = List.of(AbstractPostgresIntTest.class, AbstractOracleIntTest.class);

    @Override
    public void orderClasses(ClassOrdererContext context) {
        context.getClassDescriptors().sort(Comparator.comparingInt(descriptor -> rank(descriptor.getTestClass())));
    }

    private static int rank(Class<?> testClass) {
        for (int i = 0; i < DATABASE_BASES.size(); i++) {
            if (DATABASE_BASES.get(i).isAssignableFrom(testClass)) {
                return i + 1;
            }
        }
        return 0;
    }
}
