package com.icthh.xm.ms.entity.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.commons.permission.service.PermissionCheckService;
import com.icthh.xm.commons.tenant.PlainTenant;
import com.icthh.xm.commons.tenant.Tenant;
import com.icthh.xm.commons.tenant.TenantContext;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.ms.entity.AbstractUnitTest;
import com.icthh.xm.ms.entity.config.IndexConfiguration;
import com.icthh.xm.ms.entity.config.MappingConfiguration;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.repository.XmEntityRepositoryInternal;
import com.icthh.xm.ms.entity.repository.search.XmEntitySearchRepository;
import com.icthh.xm.ms.entity.repository.search.translator.SpelToElasticTranslator;
import com.icthh.xm.ms.entity.service.search.ElasticsearchTemplateWrapper;
import lombok.SneakyThrows;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.DefaultResultMapper;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.util.ReflectionUtils;

import javax.persistence.EntityManager;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;

@RunWith(MockitoJUnitRunner.class)
public class ElasticsearchIndexServiceUnitTest extends AbstractUnitTest {

    private ElasticsearchIndexService service;
    private ElasticsearchTemplateWrapper elasticsearchTemplateWrapper;
    @Mock
    private XmEntityRepositoryInternal xmEntityRepository;
    @Mock
    private XmEntitySearchRepository xmEntitySearchRepository;

    @Mock
    private ElasticsearchTemplate elasticsearchTemplate;

    @Mock
    private EntityManager entityManager;
    @Mock
    TenantContextHolder tenantContextHolder;
    @Mock
    MappingConfiguration mappingConfiguration;
    @Mock
    IndexConfiguration indexConfiguration;
    @Mock
    private PermissionCheckService permissionCheckService;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);

        Class<XmEntity> entityClass = XmEntity.class;
        when(tenantContextHolder.getContext()).thenReturn(new TenantContext() {
            @Override
            public boolean isInitialized() {
                return true;
            }
            @Override
            public Optional<Tenant> getTenant() {
                return Optional.of(new PlainTenant(new TenantKey("XM")));
            }
            @Override
            public Optional<TenantKey> getTenantKey() {
                return getTenant().map(Tenant::getTenantKey);
            }
        });

        when(tenantContextHolder.getTenantKey()).thenReturn("XM");

        when(xmEntityRepository.count(null)).thenReturn(10L);
        when(xmEntityRepository.findAll(null, PageRequest.of(0, 100))).thenReturn(
            new PageImpl<>(Collections.singletonList(createObject(entityClass))));

        elasticsearchTemplateWrapper = new ElasticsearchTemplateWrapper(tenantContextHolder, elasticsearchTemplate,
            new ObjectMapper(), new DefaultResultMapper(), permissionCheckService, new SpelToElasticTranslator());

        service = new ElasticsearchIndexService(
            xmEntityRepository, xmEntitySearchRepository , elasticsearchTemplateWrapper, tenantContextHolder, mappingConfiguration,
            indexConfiguration, Executors.newSingleThreadExecutor(), entityManager);

        service.setSelfReference(service);
    }

    @Test
    public void reindexAll() {

        service.reindexAll();

        verifyInternal();
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    private  void verifyInternal() {

        Class<XmEntity> entityClass = XmEntity.class;

        verify(elasticsearchTemplate).deleteIndex("xm_xmentity");
        verify(elasticsearchTemplate).createIndex("xm_xmentity");
        verify(elasticsearchTemplate).putMapping(eq("xm_xmentity"), eq("xmentity"), anyString());

        verify(xmEntityRepository, times(4)).count(any());

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
