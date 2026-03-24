package com.icthh.xm.ms.entity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.icthh.xm.commons.search.ElasticsearchOperations;
import com.icthh.xm.commons.tenant.TenantContext;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.ms.entity.AbstractJupiterUnitTest;
import com.icthh.xm.ms.entity.config.ApplicationProperties;
import com.icthh.xm.ms.entity.config.IndexConfiguration;
import com.icthh.xm.ms.entity.config.MappingConfiguration;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.repository.XmEntityRepositoryInternal;
import com.icthh.xm.ms.entity.repository.search.XmEntitySearchRepository;
import jakarta.persistence.EntityManager;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class ElasticsearchIndexServiceUnitTest extends AbstractJupiterUnitTest {

    private static final String TENANT_KEY = "XM";
    public static final String INDEX_KEY = TENANT_KEY.toLowerCase() + "_xmentity";

    private ElasticsearchIndexServiceImpl service;

    @Mock
    private ElasticsearchOperations elasticsearchOperations;
    @Mock
    private XmEntityRepositoryInternal xmEntityRepository;
    @Mock
    private XmEntitySearchRepository xmEntitySearchRepository;

    @Mock
    private EntityManager entityManager;
    @Mock
    private ApplicationProperties applicationProperties;
    @Mock
    TenantContextHolder tenantContextHolder;
    @Mock
    TenantContext tenantContext;
    @Mock
    MappingConfiguration mappingConfiguration;
    @Mock
    IndexConfiguration indexConfiguration;

    @Mock
    XmEntitySpecService xmEntitySpecService;

    @BeforeEach
    public void before() {
        MockitoAnnotations.initMocks(this);
        when(applicationProperties.getElasticBatchSize()).thenReturn(100);
        service = new ElasticsearchIndexServiceImpl(xmEntityRepository, xmEntitySearchRepository, elasticsearchOperations,
            tenantContextHolder, mappingConfiguration, indexConfiguration, null, entityManager, applicationProperties,
            xmEntitySpecService);
        service.setSelf(service);
    }

    @Test
    public void reindexAll() {
        prepareInternal();

        service.reindexAll();

        verifyInternal();
    }

    @SneakyThrows
    private void prepareInternal() {
        Class<XmEntity> entityClass = XmEntity.class;

        when(elasticsearchOperations.composeIndexName(TENANT_KEY)).thenReturn(INDEX_KEY);
        when(tenantContext.getTenantKey()).thenReturn(Optional.of(new TenantKey(TENANT_KEY)));
        when(tenantContextHolder.getContext()).thenReturn(tenantContext);
        when(xmEntityRepository.count(notNull())).thenReturn(10L);
        when(xmEntityRepository.findAll(notNull(), eq(PageRequest.of(0, 100)))).thenReturn(
            new PageImpl<>(Collections.singletonList(createObject(entityClass))));
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    private void verifyInternal() {

        Class<XmEntity> entityClass = XmEntity.class;

        verify(elasticsearchOperations).composeIndexName(eq(TENANT_KEY));
        verify(elasticsearchOperations).deleteIndex("xm_xmentity");
        verify(elasticsearchOperations).createIndex("xm_xmentity");
        verify(elasticsearchOperations).putMapping(entityClass);

        verify(xmEntityRepository, times(4)).count(notNull());

        ArgumentCaptor<List> list = ArgumentCaptor.forClass(List.class);
        verify(xmEntitySearchRepository).saveAll(list.capture());

        assertThat(list.getValue()).containsExactly(createObject(entityClass));
    }

    @SneakyThrows
    private static <T> T createObject(Class<T> entityClass) {
        T instance = entityClass.newInstance();
        Field id = ReflectionUtils.findField(entityClass, "id");
        id.setAccessible(true);
        ReflectionUtils.setField(ReflectionUtils.findField(entityClass, "id"), instance, 777L);
        return instance;
    }
}
