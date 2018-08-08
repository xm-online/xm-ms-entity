package com.icthh.xm.ms.entity.service.impl;

import static com.google.common.collect.ImmutableMap.of;
import static org.mockito.Matchers.refEq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.icthh.xm.ms.entity.domain.FunctionContext;
import com.icthh.xm.ms.entity.repository.FunctionContextRepository;
import com.icthh.xm.ms.entity.repository.search.FunctionContextSearchRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class FunctionContextServiceImplUnitTest {

    @InjectMocks
    private FunctionContextServiceImpl functionContextService;

    @Mock
    private FunctionContextRepository functionContextRepository;

    @Mock
    private FunctionContextSearchRepository functionContextSearchRepository;

    @Mock
    private StartUpdateDateGenerationStrategy startUpdateDateGenerationStrategy;

    @Test
    public void saveFunctionContextWithNoEntity() {
        when(functionContextRepository.save(refEq(mockFunctionContext()))).thenReturn(mockFunctionContext());
        when(functionContextSearchRepository.save(refEq(mockFunctionContext()))).thenReturn(mockFunctionContext());
        functionContextService.save(mockFunctionContext());
        verify(functionContextRepository).save(refEq(mockFunctionContext()));
        verify(functionContextSearchRepository).save(refEq(mockFunctionContext()));
    }

    private FunctionContext mockFunctionContext() {
        FunctionContext functionContext = new FunctionContext();
        functionContext.setData(of("some data", "some value"));
        functionContext.setDescription("description");
        functionContext.setTypeKey("TYPE_KEY");
        return functionContext;
    }

}
