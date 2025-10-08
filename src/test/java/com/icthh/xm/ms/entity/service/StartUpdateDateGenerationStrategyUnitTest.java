package com.icthh.xm.ms.entity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.icthh.xm.ms.entity.AbstractJupiterUnitTest;
import com.icthh.xm.ms.entity.domain.FunctionContext;
import com.icthh.xm.ms.entity.repository.FunctionContextRepository;
import com.icthh.xm.ms.entity.service.impl.StartUpdateDateGenerationStrategy;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.time.Instant;

/**
 * Unit test for StartUpdateDateGenerationStrategy.
 */
public class StartUpdateDateGenerationStrategyUnitTest extends AbstractJupiterUnitTest {

    private static final Instant MOCK_START_DATE = Instant.ofEpochMilli(100);
    private static final Instant MOCK_UPDATE_DATE = Instant.ofEpochMilli(200);

    private static final Instant UPDATED_START_DATE = Instant.ofEpochMilli(1000);
    private static final Instant UPDATED_UPDATE_DATE = Instant.ofEpochMilli(2000);

    @Spy
    StartUpdateDateGenerationStrategy strategy;

    @Mock
    FunctionContextRepository repository;

    @BeforeEach
    public void init() {
        MockitoAnnotations.initMocks(this);

        when(strategy.generateStartDate()).thenReturn(MOCK_START_DATE);
        when(strategy.generateUpdateDate()).thenReturn(MOCK_UPDATE_DATE);

    }

    @Test
    public void testGenerateStartDateNullCheck() {

        checkNullParameter(() -> {
            strategy.preProcessStartDate(null, null, null, null, null);
        }, "entity can not be null");
        checkNullParameter(() -> {
            strategy.preProcessStartDate(new Object(), null, null, null, null);
        }, "repository can not be null");
        checkNullParameter(() -> {
            strategy.preProcessStartDate(new FunctionContext(), null, repository, null, null);
        }, "startDateSetter can not be null");
        checkNullParameter(() -> {
            strategy.preProcessStartDate(new FunctionContext(), null, repository, FunctionContext::setStartDate, null);
        }, "startDateGetter can not be nul");

    }

    @Test
    public void testGenerateStartDate() {

        FunctionContext context = new FunctionContext();

        assertThat(context.getStartDate()).isNull();
        assertThat(context.getUpdateDate()).isNull();

        strategy.preProcessStartDate(context,
                                     context.getId(),
                                     repository,
                                     FunctionContext::setStartDate,
                                     FunctionContext::getStartDate);

        assertThat(context.getStartDate()).isEqualTo(MOCK_START_DATE);
        assertThat(context.getUpdateDate()).isNull();

    }

    @Test
    public void testGenerateStartUpdateDate() {

        FunctionContext context = new FunctionContext();

        assertThat(context.getStartDate()).isNull();
        assertThat(context.getUpdateDate()).isNull();

        strategy.preProcessStartUpdateDates(context,
                                            context.getId(),
                                            repository,
                                            FunctionContext::setStartDate,
                                            FunctionContext::getStartDate,
                                            FunctionContext::setUpdateDate);

        assertThat(context.getStartDate()).isEqualTo(MOCK_START_DATE);
        assertThat(context.getUpdateDate()).isEqualTo(MOCK_UPDATE_DATE);

    }

    @Test
    public void testOverrideInputDates() {

        FunctionContext context = new FunctionContext();

        context.setStartDate(UPDATED_START_DATE);
        context.setUpdateDate(UPDATED_UPDATE_DATE);

        assertThat(context.getStartDate()).isEqualTo(UPDATED_START_DATE);
        assertThat(context.getUpdateDate()).isEqualTo(UPDATED_UPDATE_DATE);

        strategy.preProcessStartUpdateDates(context,
                                            context.getId(),
                                            repository,
                                            FunctionContext::setStartDate,
                                            FunctionContext::getStartDate,
                                            FunctionContext::setUpdateDate);

        assertThat(context.getStartDate()).isEqualTo(MOCK_START_DATE);
        assertThat(context.getUpdateDate()).isEqualTo(MOCK_UPDATE_DATE);

    }

    @Test
    public void testUpdateDateChanging() {

        FunctionContext context = new FunctionContext();

        assertThat(context.getStartDate()).isNull();
        assertThat(context.getUpdateDate()).isNull();

        strategy.preProcessStartUpdateDates(context,
                                            context.getId(),
                                            repository,
                                            FunctionContext::setStartDate,
                                            FunctionContext::getStartDate,
                                            FunctionContext::setUpdateDate);

        assertThat(context.getStartDate()).isEqualTo(MOCK_START_DATE);
        assertThat(context.getUpdateDate()).isEqualTo(MOCK_UPDATE_DATE);

        Instant updateDate = Instant.now();

        when(strategy.generateUpdateDate()).thenReturn(updateDate);

        strategy.preProcessStartUpdateDates(context,
                                            context.getId(),
                                            repository,
                                            FunctionContext::setStartDate,
                                            FunctionContext::getStartDate,
                                            FunctionContext::setUpdateDate);

        assertThat(context.getStartDate()).isEqualTo(MOCK_START_DATE);
        assertThat(context.getUpdateDate()).isEqualTo(updateDate);

    }

    @Test
    public void testStartDateNotChanging() {

        FunctionContext oldContext = new FunctionContext();

        assertThat(oldContext.getStartDate()).isNull();
        assertThat(oldContext.getUpdateDate()).isNull();

        strategy.preProcessStartUpdateDates(oldContext,
                                            oldContext.getId(),
                                            repository,
                                            FunctionContext::setStartDate,
                                            FunctionContext::getStartDate,
                                            FunctionContext::setUpdateDate);

        assertThat(oldContext.getStartDate()).isEqualTo(MOCK_START_DATE);
        assertThat(oldContext.getUpdateDate()).isEqualTo(MOCK_UPDATE_DATE);

        Instant updateDate = Instant.now();

        when(strategy.generateUpdateDate()).thenReturn(updateDate);
        when(repository.findById(1L)).thenReturn(Optional.of(oldContext));

        FunctionContext newContext = new FunctionContext();
        // set ID to trigger findOne from repo.
        newContext.setId(1L);
        // try to override startDate
        newContext.setStartDate(Instant.now());

        strategy.preProcessStartUpdateDates(newContext,
                                            newContext.getId(),
                                            repository,
                                            FunctionContext::setStartDate,
                                            FunctionContext::getStartDate,
                                            FunctionContext::setUpdateDate);

        verify(repository).findById(1L);

        System.out.println(newContext);

        assertThat(newContext.getStartDate()).isEqualTo(MOCK_START_DATE);
        assertThat(newContext.getUpdateDate()).isEqualTo(updateDate);

    }

    private void checkNullParameter(Runnable r, String exMessage) {

        try {
            r.run();
            Assertions.fail("expected NullPointerException!");
        } catch (NullPointerException e) {
            assertThat(e.getMessage()).contains(exMessage);
        }

    }

}
