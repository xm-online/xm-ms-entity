package com.icthh.xm.ms.entity.repository.search.elasticsearch;

import static org.mockito.Mockito.verify;

import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.ms.entity.AbstractUnitTest;
import com.icthh.xm.ms.entity.config.IndexConfiguration;
import com.icthh.xm.ms.entity.config.MappingConfiguration;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.repository.XmEntityRepositoryInternal;
import com.icthh.xm.ms.entity.repository.search.XmEntitySearchRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;

@RunWith(MockitoJUnitRunner.class)
public class XmEntitySingleIndexElasticRepositoryTest  {


    @Mock
    private XmEntityRepositoryInternal xmEntityRepository;
    @Mock
    private XmEntitySearchRepository xmEntitySearchRepository;
    @Mock
    private ElasticsearchTemplate elasticsearchTemplate;
    @Mock
    TenantContextHolder tenantContextHolder;
    @Mock
    MappingConfiguration mappingConfiguration;
    @Mock
    IndexConfiguration indexConfiguration;

    @InjectMocks
    private XmEntitySingleIndexElasticRepository repo;

    @Test
    public void handleReindexTest() {
        repo.handleReindex(specification -> 0L);

        Class<XmEntity> entityClass = XmEntity.class;
        verify(elasticsearchTemplate).deleteIndex(entityClass);
        verify(elasticsearchTemplate).createIndex(entityClass);
        verify(elasticsearchTemplate).putMapping(entityClass);
    }
}
