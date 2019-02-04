package com.icthh.xm.ms.entity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.icthh.xm.commons.tenant.PrivilegedTenantContext;
import com.icthh.xm.commons.tenant.TenantContext;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.ms.entity.config.MappingConfiguration;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import com.icthh.xm.ms.entity.repository.search.XmEntitySearchRepository;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.util.ReflectionUtils;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@RunWith(MockitoJUnitRunner.class)
public class ElasticsearchIndexServiceUnitTest {

    @InjectMocks
    private ElasticsearchIndexService service;
    @Mock
    private XmEntityRepository xmEntityRepository;
    @Mock
    private XmEntitySearchRepository xmEntitySearchRepository;
    @Mock
    private ElasticsearchTemplate elasticsearchTemplate;
    @Mock
    TenantContextHolder tenantContextHolder;
    @Mock
    MappingConfiguration mappingConfiguration;

    @Before
    public void before() {
        service.setSelfReference(service);

        TenantContext tenantContext = mock(TenantContext.class);
        when(tenantContext.getTenantKey()).thenReturn(Optional.of(TenantKey.valueOf("XM")));
        when(tenantContextHolder.getContext()).thenReturn(tenantContext);

        PrivilegedTenantContext privilegedTenantContext = mock(PrivilegedTenantContext.class);
        when(tenantContextHolder.getPrivilegedContext()).thenReturn(privilegedTenantContext);
    }

    @Test
    public void reindexAll() {
        prepareInternal(XmEntity.class, xmEntityRepository);

        service.reindexAll();

        verifyInternal(XmEntity.class, xmEntityRepository, xmEntitySearchRepository);
    }

    @SneakyThrows
    private <T, ID extends Serializable> void prepareInternal(Class<T> entityClass,
        JpaRepository<T, ID> jpaRepository) {
        when(jpaRepository.count()).thenReturn(10L);
        when(jpaRepository.findAll(new PageRequest(0, 100))).thenReturn(
            new PageImpl<>(Collections.singletonList(createObject(entityClass))));
    }

    @SneakyThrows
    private <T, ID extends Serializable> void verifyInternal(Class<T> entityClass, JpaRepository<T, ID> jpaRepository,
        ElasticsearchRepository<T, ID> elasticsearchRepository) {
        verify(elasticsearchTemplate).deleteIndex(entityClass);
        verify(elasticsearchTemplate).createIndex(entityClass);
        verify(elasticsearchTemplate).putMapping(entityClass);

        verify(jpaRepository, times(4)).count();

        ArgumentCaptor<List> list = ArgumentCaptor.forClass(List.class);
        verify(elasticsearchRepository).save(list.capture());

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
