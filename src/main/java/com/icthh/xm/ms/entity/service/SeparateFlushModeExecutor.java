package com.icthh.xm.ms.entity.service;

import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.function.Supplier;

@Component
public class SeparateFlushModeExecutor {

    private EntityManager entityManager;

    @PersistenceContext
    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * Hibernate flushes the persistence context before executing a query that uses any database table
     * for which your persistence context contains any pending changes. If you want to change this behaviour,
     * use this method.
     * <p>
     * This method will set MANUAL flush mode to the current transaction, and after provided task is executed
     * it will set the previous flush mode back. MANUAL flush mode deactivates all automatic flushes and requires
     * the application to trigger the flushes automatically. For example, it is useful in database interceptors
     * or domain event leps, to prevent stack overflows for executing queries to database.
     *
     * @param supplier task to execute without flush.
     * @param <T> possible return type.
     * @return task execution result.
     */
    public <T> T doWithoutFlush(Supplier<T> supplier) {
        return doInSeparateFlushMode(FlushMode.MANUAL, supplier);
    }

    /**
     * This method will set provided flush mode to the current transaction, and after provided task is executed
     * it will set the previous flush mode.
     * @link <a href="https://thorben-janssen.com/flushmode-in-jpa-and-hibernate/">Flush mode descriptions.</a>
     *
     * @param requiredFlushMode hibernate flushing strategy that will be set for the task execution.
     * @param supplier task to execute in specified flush mode.
     * @param <T> possible return type.
     * @return task execution result.
     */
    public <T> T doInSeparateFlushMode(FlushMode requiredFlushMode, Supplier<T> supplier) {
        Session session = entityManager.unwrap(Session.class);
        FlushMode previousFlushMode = session.getHibernateFlushMode();
        try {
            session.setHibernateFlushMode(requiredFlushMode);
            return supplier.get();
        } finally {
            session.setHibernateFlushMode(previousFlushMode);
        }
    }

}
