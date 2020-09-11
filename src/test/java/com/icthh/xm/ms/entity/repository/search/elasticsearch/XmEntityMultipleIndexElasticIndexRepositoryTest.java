package com.icthh.xm.ms.entity.repository.search.elasticsearch;

import static com.icthh.xm.ms.entity.repository.search.elasticsearch.XmEntityMultipleIndexElasticIndexRepository.INDEX_TYPE_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.icthh.xm.ms.entity.config.IndexConfiguration;
import com.icthh.xm.ms.entity.config.MappingConfiguration;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import com.icthh.xm.ms.entity.repository.search.elasticsearch.index.ElasticIndexNameResolver;
import com.icthh.xm.ms.entity.service.XmEntitySpecService;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.query.DeleteQuery;
import org.springframework.data.elasticsearch.core.query.IndexQuery;

@RunWith(MockitoJUnitRunner.class)
public class XmEntityMultipleIndexElasticIndexRepositoryTest {

    public static final String TYPE_KEY = "ORGANIZATION";
    @Mock
    private ElasticsearchTemplate elasticsearchTemplate;
    @Mock
    private MappingConfiguration mappingConfiguration;
    @Mock
    private IndexConfiguration indexConfiguration;
    @Mock
    private XmEntitySpecService xmEntitySpecService;
    @Mock
    private ElasticIndexNameResolver elasticIndexNameResolver;
    @InjectMocks
    XmEntityMultipleIndexElasticIndexRepository xmEntityMultipleIndexElasticIndexRepository;

    @Test
    public void saveTest() {

        XmEntity entity = createEntity();
        when(elasticIndexNameResolver.resolve(eq(entity.getTypeKey()))).thenReturn(TYPE_KEY);

        xmEntityMultipleIndexElasticIndexRepository.save(entity);

        ArgumentCaptor<IndexQuery> indexQuery = ArgumentCaptor.forClass(IndexQuery.class);
        verify(elasticsearchTemplate).index(indexQuery.capture());
        assertEquals(indexQuery.getValue().getObject(), entity);
        assertEquals(indexQuery.getValue().getIndexName(), TYPE_KEY);
    }


    @Test
    public void saveAllTest() {

        XmEntity entity = createEntity();
        when(elasticIndexNameResolver.resolve(eq(entity.getTypeKey()))).thenReturn(TYPE_KEY);

        xmEntityMultipleIndexElasticIndexRepository.saveAll(List.of(entity));

        ArgumentCaptor<List> list = ArgumentCaptor.forClass(List.class);
        verify(elasticsearchTemplate).bulkIndex(list.capture());
        assertEquals(list.getValue().size(), 1);
        assertEquals(((IndexQuery)list.getValue().get(0)).getObject(), entity);
        assertEquals(((IndexQuery)list.getValue().get(0)).getIndexName(), TYPE_KEY);
    }

    @Test
    public void deleteTest() {

        XmEntity entity = createEntity();

        xmEntityMultipleIndexElasticIndexRepository.delete(entity);

        ArgumentCaptor<DeleteQuery> deleteQuery = ArgumentCaptor.forClass(DeleteQuery.class);
        verify(elasticsearchTemplate).delete(deleteQuery.capture(), eq(XmEntity.class));
        assertEquals(deleteQuery.getValue().getIndex(), TYPE_KEY);
    }

    @Test
    public void reindexTest() {

        when(xmEntitySpecService.findAllTypes())
            .thenReturn(List.of(createTypeKey("GOOGLE"), createTypeKey("MS"), createTypeKey("ADOBE")));
        when(mappingConfiguration.getMapping(anyString())).thenReturn("{}");
        when(elasticIndexNameResolver.resolve(anyString())).thenAnswer(i -> i.getArguments()[0]);
        when(indexConfiguration.getMapping(anyString())).thenReturn("{}");

        long count = xmEntityMultipleIndexElasticIndexRepository.handleReindex(specification -> 1L);

        assertEquals(count, 3);
        verify(elasticsearchTemplate, times(3)).deleteIndex(anyString());
        verify(elasticsearchTemplate, times(3)).createIndex(anyString(), eq("{}"));
        verify(elasticsearchTemplate, times(3)).putMapping(anyString(), eq(INDEX_TYPE_NAME), eq("{}"));
    }


    private TypeSpec createTypeKey(String typeKey){
        return new TypeSpec(){{
            setKey(typeKey);
        }};
    }


    private XmEntity createEntity(){
        return new XmEntity(){{
            setId(0L);
            typeKey(TYPE_KEY);
        }};

    }
}
