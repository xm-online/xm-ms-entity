package com.icthh.xm.ms.entity.service.impl;

import static com.google.common.collect.ImmutableMap.of;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.icthh.xm.ms.entity.AbstractJupiterUnitTest;
import com.icthh.xm.ms.entity.domain.FunctionContext;
import com.icthh.xm.ms.entity.repository.FunctionContextRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class FunctionContextServiceImplUnitTest extends AbstractJupiterUnitTest {

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
