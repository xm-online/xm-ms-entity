package com.icthh.xm.ms.entity.repository.search.elasticsearch.index;

import static com.icthh.xm.ms.entity.config.elasticsearch.CustomMappingElasticsearchEntityInformation.XMENTITY_SUFFIX;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import com.icthh.xm.ms.entity.config.elasticsearch.ElasticsearchConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ElasticSingleIndexNameResolverTest {


    @Mock
    private ElasticsearchConfiguration.IndexName indexName;

    @InjectMocks
    ElasticSingleIndexNameResolver elasticSingleIndexNameResolver;

    @Test
    public void name() {
        when(indexName.getPrefix()).thenReturn("xm_");
        String index = elasticSingleIndexNameResolver.resolve("oracle");
        assertEquals(index, "xm_" + XMENTITY_SUFFIX);

    }
}
