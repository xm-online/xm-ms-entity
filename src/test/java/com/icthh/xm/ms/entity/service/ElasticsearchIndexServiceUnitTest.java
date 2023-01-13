package com.icthh.xm.ms.entity.service;

import com.icthh.xm.commons.tenant.PlainTenant;
import com.icthh.xm.commons.tenant.Tenant;
import com.icthh.xm.commons.tenant.TenantContext;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.ms.entity.service.search.ElasticsearchTemplateWrapper;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.ms.entity.AbstractUnitTest;
import com.icthh.xm.ms.entity.config.IndexConfiguration;
import com.icthh.xm.ms.entity.config.MappingConfiguration;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.repository.XmEntityRepositoryInternal;
import com.icthh.xm.ms.entity.repository.search.XmEntitySearchRepository;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.util.ReflectionUtils;

import javax.persistence.EntityManager;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@RunWith(MockitoJUnitRunner.class)
public class ElasticsearchIndexServiceUnitTest extends AbstractUnitTest {

    @InjectMocks
    private ElasticsearchIndexService service;
    @Mock
    private XmEntityRepositoryInternal xmEntityRepository;
    @Mock
    private XmEntitySearchRepository xmEntitySearchRepository;
    @Mock
    private ElasticsearchTemplateWrapper elasticsearchTemplateWrapper;

    @Mock
    private EntityManager entityManager;
    @Mock
    TenantContextHolder tenantContextHolder;
    @Mock
    MappingConfiguration mappingConfiguration;
    @Mock
    IndexConfiguration indexConfiguration;

    @Before
    public void before() {
        service.setSelfReference(service);
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
        when(tenantContextHolder.getContext()).thenReturn(new TenantContext() {
            @Override
            public boolean isInitialized() {
                return true;
            }
            @Override
            public Optional<Tenant> getTenant() {
                return Optional.of(new PlainTenant(new TenantKey("XM")));
            }
        });
        when(xmEntityRepository.count(null)).thenReturn(10L);
        when(xmEntityRepository.findAll(null, PageRequest.of(0, 100))).thenReturn(
            new PageImpl<>(Collections.singletonList(createObject(entityClass))));
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    private  void verifyInternal() {

        Class<XmEntity> entityClass = XmEntity.class;

        verify(elasticsearchTemplateWrapper).deleteIndex("xm_xmentity");
        verify(elasticsearchTemplateWrapper).createIndex("xm_xmentity");
        verify(elasticsearchTemplateWrapper).putMapping(entityClass);

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
