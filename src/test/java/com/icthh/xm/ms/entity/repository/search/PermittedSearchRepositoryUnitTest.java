package com.icthh.xm.ms.entity.repository.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.commons.permission.service.PermissionCheckService;
import com.icthh.xm.commons.permission.service.translator.SpelTranslator;
import com.icthh.xm.ms.entity.config.ApplicationProperties;
import com.icthh.xm.ms.entity.config.elasticsearch.ElasticsearchQueryTimeoutGuard;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.repository.search.translator.SpelToElasticTranslator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class PermittedSearchRepositoryUnitTest {

    private static final String PRIVILEGE = "XMENTITY.SEARCH";

    @Mock
    private PermissionCheckService permissionCheckService;

    @Mock
    private SpelToElasticTranslator spelToElasticTranslator;

    @Mock
    private ElasticsearchTemplate elasticsearchTemplate;

    @Mock
    private ElasticsearchQueryTimeoutGuard elasticsearchQueryTimeoutGuard;

    @Mock
    private ApplicationProperties applicationProperties;

    @Mock
    private ObjectMapper objectMapper;

    @Captor
    private ArgumentCaptor<Map<String, Object>> contextParamsCaptor;

    @InjectMocks
    private PermittedSearchRepository repository;

    /**
     * The query and the searched type are what let a filter LEP narrow only the requests it is
     * meant to, instead of running for every search of every type.
     */
    @Test
    public void passesQueryAndEntityClassToTheFilterLep() {
        repository.buildPermittedQuery("typeKey:LEAD", PRIVILEGE, XmEntity.class);

        verify(permissionCheckService).createCondition(any(), eq(PRIVILEGE), any(SpelTranslator.class),
            contextParamsCaptor.capture());
        assertThat(contextParamsCaptor.getValue())
            .containsEntry("query", "typeKey:LEAD")
            .containsEntry("entityClass", XmEntity.class);
    }

    @Test
    public void permittedConditionIsAndedWithTheUserQuery() {
        given(permissionCheckService.createCondition(any(), any(), any(), any()))
            .willReturn("data.owner:\"user-key-1\"");

        assertThat(repository.buildPermittedQuery("typeKey:LEAD", PRIVILEGE, XmEntity.class))
            .isEqualTo("typeKey:LEAD AND (data.owner:\"user-key-1\")");
    }

    @Test
    public void permittedConditionBecomesTheWholeQueryWhenUserQueryIsBlank() {
        given(permissionCheckService.createCondition(any(), any(), any(), any()))
            .willReturn("data.owner:\"user-key-1\"");

        assertThat(repository.buildPermittedQuery("", PRIVILEGE, XmEntity.class))
            .isEqualTo("data.owner:\"user-key-1\"");
    }

    @Test
    public void userQueryIsLeftAloneWithoutAPermittedCondition() {
        given(permissionCheckService.createCondition(any(), any(), any(), any()))
            .willReturn("");

        assertThat(repository.buildPermittedQuery("typeKey:LEAD", PRIVILEGE, XmEntity.class))
            .isEqualTo("typeKey:LEAD");
    }
}
