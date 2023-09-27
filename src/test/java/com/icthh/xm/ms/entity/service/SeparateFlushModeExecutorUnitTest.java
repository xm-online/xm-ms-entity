package com.icthh.xm.ms.entity.service;

import com.icthh.xm.ms.entity.AbstractUnitTest;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import javax.persistence.EntityManager;
import java.util.function.Supplier;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

/**
 * Unit test for SeparateFlushModeExecutor.
 */
public class SeparateFlushModeExecutorUnitTest extends AbstractUnitTest {

    private static final FlushMode DEFAULT_FLUSH_MODE = FlushMode.AUTO;

    @InjectMocks
    private SeparateFlushModeExecutor separateFlushModeExecutor;

    @Mock
    private EntityManager entityManager;

    @Mock
    private Session session;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(session.getHibernateFlushMode()).thenReturn(DEFAULT_FLUSH_MODE);

    }

    @Test
    public void shouldSetFlushModeAndReturnPrevious() {
        FlushMode requiredFlushMode = FlushMode.ALWAYS;
        Supplier taskMock = Mockito.mock(Supplier.class);
        separateFlushModeExecutor.doInSeparateFlushMode(requiredFlushMode, taskMock);
        InOrder inOrder = inOrder(session, taskMock);

        //first should set required flush mode to session
        inOrder.verify(session).setHibernateFlushMode(eq(requiredFlushMode));
        //then should execute task with required flush mode
        inOrder.verify(taskMock).get();
        //then should set previous flush mode back
        inOrder.verify(session).setHibernateFlushMode(eq(DEFAULT_FLUSH_MODE));
    }

    @Test
    public void shouldSetManualFlushModeAndReturnPrevious() {
        Supplier taskMock = Mockito.mock(Supplier.class);
        separateFlushModeExecutor.doWithoutFlush(taskMock);
        InOrder inOrder = inOrder(session, taskMock);

        //first should set MANUAL flush mode to session
        inOrder.verify(session).setHibernateFlushMode(eq(FlushMode.MANUAL));
        //then should execute task with required flush mode
        inOrder.verify(taskMock).get();
        //then should set previous flush mode back
        inOrder.verify(session).setHibernateFlushMode(eq(DEFAULT_FLUSH_MODE));
    }

}
