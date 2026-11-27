package com.icthh.xm.ms.entity.config;

import com.icthh.xm.ms.entity.AbstractOracleIntTest;
import com.icthh.xm.ms.entity.AbstractPostgresIntTest;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.ClassDescriptor;
import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.ClassOrdererContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.test.context.ActiveProfiles;

/**
 * Orders the test classes by the Spring context they use: classes of the shared default context first, then the
 * classes with their own {@code @ActiveProfiles}, grouped by profile, then the classes backed by a real database
 * (PostgreSQL, then Oracle). Inside a group the order is the discovered one.
 *
 * <p>Why. JPA entity listeners such as {@code XmEntityElasticSearchListener} reach their Spring beans through static
 * fields that the most recently refreshed context overwrites. A context created in the middle of the run would take
 * the listeners over from the shared default context, so the classes of one context must run as one block. The
 * database contexts, which are closed as soon as the plan ends, must run last: a class that saves entities after
 * that would call into a closed context. As a side effect the H2 part of the suite runs without an idle container.
 *
 * <p>Registered through {@code junit-platform.properties}, so it applies to Gradle and IDE runs alike.
 */
public class DatabaseClassesLastOrderer implements ClassOrderer {

    private static final List<Class<?>> DATABASE_BASES = List.of(AbstractPostgresIntTest.class, AbstractOracleIntTest.class);
    private static final int PROFILE_RANK = 1;
    private static final int FIRST_DATABASE_RANK = 2;

    @Override
    public void orderClasses(ClassOrdererContext context) {
        context.getClassDescriptors().sort(Comparator
            .comparingInt((ClassDescriptor descriptor) -> rank(descriptor.getTestClass()))
            .thenComparing(descriptor -> profiles(descriptor.getTestClass())));
    }

    private static int rank(Class<?> testClass) {
        for (int i = 0; i < DATABASE_BASES.size(); i++) {
            if (DATABASE_BASES.get(i).isAssignableFrom(testClass)) {
                return i + FIRST_DATABASE_RANK;
            }
        }
        return profiles(testClass).isEmpty() ? 0 : PROFILE_RANK;
    }

    /** Profile names of the class, joined, or an empty string when it uses the default profile. */
    private static String profiles(Class<?> testClass) {
        ActiveProfiles activeProfiles = AnnotatedElementUtils.findMergedAnnotation(testClass, ActiveProfiles.class);
        return activeProfiles == null ? "" : String.join(",", activeProfiles.profiles());
    }
}
