package com.icthh.xm.ms.entity.config.jsonb;

import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.junit.Assert.assertEquals;

import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.XmEntity_;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;

@Slf4j
public class JsonPostgresConfigIntTest extends AbstractJsonPostgresIntTest {

    public static final String FIRST_DATA_KEY = "firstDataKey";
    public static final String SECOND_DATA_KEY = "secondDataKey";
    public static final String FIRST_DATA_VALUE = "firstDataValue";
    public static final String SECOND_DATA_VALUE = "secondDataValue";

    @Autowired
    private XmEntityRepository entityRepository;

    @Test
    public void searchXmEntityByJsonbDataTest() {
        XmEntity firstEntity = createEntity(Map.of(FIRST_DATA_KEY, FIRST_DATA_VALUE));
        XmEntity secondEntity = createEntity(Map.of(FIRST_DATA_KEY, SECOND_DATA_VALUE));
        XmEntity thirdEntity = createEntity(Map.of());
        List<XmEntity> xmEntities = entityRepository.saveAll(List.of(firstEntity, secondEntity, thirdEntity));

        List<XmEntity> entities = entityRepository.findAll(Specification.where((root, query, cb) ->
                cb.equal(
                    cb.function(
                        "jsonb_extract_path_text",
                        String.class,
                        root.get(XmEntity_.DATA),
                        cb.literal(FIRST_DATA_KEY)),
                    FIRST_DATA_VALUE)
            )
        );
        assertEquals(entities.size(), 1);
        assertEquals(entities.get(0).getData().get(FIRST_DATA_KEY), FIRST_DATA_VALUE);
    }

    public static XmEntity createEntity(Map<String, Object> data) {
        return new XmEntity()
            .typeKey("TYPE1")
            .key(randomUUID())
            .name("name")
            .startDate(now())
            .updateDate(now())
            .data(data);
    }

}
