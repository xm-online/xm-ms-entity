package com.icthh.xm.ms.entity.repository.search.elasticsearch.index;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import com.icthh.xm.ms.entity.config.elasticsearch.ElasticsearchConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ElasticMultipleIndexNameResolverTest {

    @Mock
    private ElasticsearchConfiguration.IndexName indexName;

    @InjectMocks
    ElasticMultipleIndexNameResolver elasticMultipleIndexNameResolver;

    @Test
    public void resolveIndexNameTest() {
        when(indexName.getPrefix()).thenReturn("xm_");
        assertEquals(elasticMultipleIndexNameResolver.resolve(null), null);
        assertEquals(elasticMultipleIndexNameResolver.resolve("ABC"), "xm_abc");
        assertEquals(elasticMultipleIndexNameResolver.resolve("ABC-DFG"), "xm_abc_dfg");
        assertEquals(elasticMultipleIndexNameResolver.resolve("ABC-DFG.BLAH"), "xm_abc_dfg");
        assertEquals(elasticMultipleIndexNameResolver.resolve("ABC-DFG.BLAH-BLAH"), "xm_abc_dfg");
    }
}
