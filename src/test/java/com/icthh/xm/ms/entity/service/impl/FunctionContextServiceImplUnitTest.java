package com.icthh.xm.ms.entity.service.impl;

import static com.google.common.collect.ImmutableMap.of;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.icthh.xm.ms.entity.AbstractUnitTest;
import com.icthh.xm.ms.entity.domain.FunctionContext;
import com.icthh.xm.ms.entity.repository.FunctionContextRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class FunctionContextServiceImplUnitTest extends AbstractUnitTest {

    @InjectMocks
    private FunctionContextServiceImpl functionContextService;

    @Mock
    private FunctionContextRepository functionContextRepository;

    @Mock
    private StartUpdateDateGenerationStrategy startUpdateDateGenerationStrategy;

    @Test
    public void saveFunctionContextWithNoEntity() {
        when(functionContextRepository.save(refEq(mockFunctionContext()))).thenReturn(mockFunctionContext());
        functionContextService.save(mockFunctionContext());
        verify(functionContextRepository).save(refEq(mockFunctionContext()));
    }

    private FunctionContext mockFunctionContext() {
        FunctionContext functionContext = new FunctionContext();
        functionContext.setData(of("some data", "some value"));
        functionContext.setDescription("description");
        functionContext.setTypeKey("TYPE_KEY");
        return functionContext;
    }

}
