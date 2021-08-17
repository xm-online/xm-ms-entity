package com.icthh.xm.ms.entity.config.jsonb;

import static com.icthh.xm.ms.entity.config.jsonb.JsonPostgresConfigIntTest.FIRST_DATA_KEY;
import static com.icthh.xm.ms.entity.config.jsonb.JsonPostgresConfigIntTest.FIRST_DATA_VALUE;
import static com.icthh.xm.ms.entity.config.jsonb.JsonPostgresConfigIntTest.SECOND_DATA_KEY;
import static com.icthh.xm.ms.entity.config.jsonb.JsonPostgresConfigIntTest.SECOND_DATA_VALUE;
import static com.icthh.xm.ms.entity.config.jsonb.JsonPostgresConfigIntTest.createEntity;
import static org.junit.Assert.assertEquals;

import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.XmEntity_;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import java.util.List;
import java.util.Map;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Transactional
public class JsonbCriteriaBuilderIntTest extends AbstractJsonPostgresIntTest {

    @Autowired
    private XmEntityRepository entityRepository;

    @Test
    public void equalJsonbObjectTest() {
        XmEntity firstEntity = createEntity(Map.of(FIRST_DATA_KEY, FIRST_DATA_VALUE));
        XmEntity secondEntity = createEntity(Map.of(FIRST_DATA_KEY, SECOND_DATA_VALUE));
        XmEntity thirdEntity = createEntity(Map.of());
        entityRepository.saveAll(List.of(firstEntity, secondEntity, thirdEntity));

        List<XmEntity> entities = entityRepository.findAll(Specification.where((root, query, cb) -> {
            JsonbCriteriaBuilder jsonbCriteriaBuilder = new JsonbCriteriaBuilder(cb);
            return jsonbCriteriaBuilder.equal(root, "$.firstDataKey", SECOND_DATA_VALUE);
        }));

        assertEquals(entities.size(), 1);
        assertEquals(entities.get(0).getData().get(FIRST_DATA_KEY), SECOND_DATA_VALUE);

    }

    @Test
    public void equalJsonbJsonbTest() {
        XmEntity firstEntity = createEntity(Map.of(FIRST_DATA_KEY, FIRST_DATA_VALUE));
        XmEntity secondEntity = createEntity(Map.of(FIRST_DATA_KEY, SECOND_DATA_VALUE));
        XmEntity thirdEntity = createEntity(Map.of());
        entityRepository.saveAll(List.of(firstEntity, secondEntity, thirdEntity));
        XmEntity fourthEntity = createEntity(
            Map.of(FIRST_DATA_KEY, FIRST_DATA_VALUE, SECOND_DATA_KEY, FIRST_DATA_VALUE));

        fourthEntity = entityRepository.save(fourthEntity);

        List<XmEntity> entities = entityRepository.findAll(Specification.where((root, query, cb) -> {
            JsonbCriteriaBuilder jsonbCriteriaBuilder = new JsonbCriteriaBuilder(cb);
            return jsonbCriteriaBuilder.equal(root, "$.firstDataKey", root, "$.secondDataKey");
        }));

        assertEquals(entities.size(), 1);
        assertEquals(entities.get(0).getId(), fourthEntity.getId());
    }

    @Test
    public void equalJsonbPathTest() {
        XmEntity firstEntity = createEntity(Map.of());
        firstEntity = entityRepository.save(firstEntity);
        firstEntity.setData(Map.of(SECOND_DATA_KEY, firstEntity.getId()));
        firstEntity = entityRepository.save(firstEntity);

        XmEntity secondEntity = createEntity(Map.of(FIRST_DATA_KEY, firstEntity.getId()));
        secondEntity = entityRepository.save(secondEntity);

        List<XmEntity> entities = entityRepository.findAll(Specification.where((root, query, cb) -> {
            JsonbCriteriaBuilder jsonbCriteriaBuilder = new JsonbCriteriaBuilder(cb);
            return jsonbCriteriaBuilder.equal(root, "$.secondDataKey", root.get(XmEntity_.id));
        }));

        assertEquals(entities.size(), 1);
        assertEquals(entities.get(0).getId(), firstEntity.getId());

        //select * cross join where a.jsonb = b.id
        entities = entityRepository.findAll(Specification.where((root, query, cb) -> {
            JsonbCriteriaBuilder jsonbCriteriaBuilder = new JsonbCriteriaBuilder(cb);
            Root<XmEntity> second = query.from(XmEntity.class);
            CriteriaQuery<?> where = query.where(
                jsonbCriteriaBuilder.equal(root, "$.firstDataKey", second.get(XmEntity_.id)));
            return where.getRestriction();
        }));

        assertEquals(entities.size(), 1);
        assertEquals(entities.get(0).getId(), secondEntity.getId());
    }

    @Test
    public void notEqualJsonbObjectTest() {
        XmEntity firstEntity = createEntity(Map.of(FIRST_DATA_KEY, FIRST_DATA_VALUE));
        XmEntity secondEntity = createEntity(Map.of(FIRST_DATA_KEY, SECOND_DATA_VALUE));
        XmEntity thirdEntity = createEntity(Map.of());
        entityRepository.saveAll(List.of(firstEntity, secondEntity, thirdEntity));

        List<XmEntity> entities = entityRepository.findAll(Specification.where((root, query, cb) -> {
            JsonbCriteriaBuilder jsonbCriteriaBuilder = new JsonbCriteriaBuilder(cb);
            return jsonbCriteriaBuilder.notEqual(root, "$.firstDataKey", SECOND_DATA_VALUE);
        }));

        assertEquals(entities.size(), 1);
        assertEquals(entities.get(0).getData().get(FIRST_DATA_KEY), FIRST_DATA_VALUE);

    }

    @Test
    public void notEqualJsonbJsonbTest() {
        XmEntity firstEntity = createEntity(
            Map.of(FIRST_DATA_KEY, FIRST_DATA_VALUE, SECOND_DATA_KEY, SECOND_DATA_VALUE));
        firstEntity = entityRepository.save(firstEntity);
        XmEntity fourthEntity = createEntity(
            Map.of(FIRST_DATA_KEY, FIRST_DATA_VALUE, SECOND_DATA_KEY, FIRST_DATA_VALUE));
        entityRepository.save(fourthEntity);

        List<XmEntity> entities = entityRepository.findAll(Specification.where((root, query, cb) -> {
            JsonbCriteriaBuilder jsonbCriteriaBuilder = new JsonbCriteriaBuilder(cb);
            return jsonbCriteriaBuilder.notEqual(root, "$.firstDataKey", root, "$.secondDataKey");
        }));

        assertEquals(entities.size(), 1);
        assertEquals(entities.get(0).getId(), firstEntity.getId());
    }

    @Test
    public void notEqualJsonbPathTest() {
        XmEntity firstEntity = createEntity(Map.of());
        firstEntity = entityRepository.save(firstEntity);
        firstEntity.setData(Map.of(SECOND_DATA_KEY, firstEntity.getId()));
        firstEntity = entityRepository.save(firstEntity);

        XmEntity secondEntity = createEntity(Map.of(SECOND_DATA_KEY, firstEntity.getId()));
        secondEntity = entityRepository.save(secondEntity);

        List<XmEntity> entities = entityRepository.findAll(Specification.where((root, query, cb) -> {
            JsonbCriteriaBuilder jsonbCriteriaBuilder = new JsonbCriteriaBuilder(cb);
            return jsonbCriteriaBuilder.notEqual(root, "$.secondDataKey", root.get(XmEntity_.id));
        }));

        assertEquals(entities.size(), 1);
        assertEquals(entities.get(0).getId(), secondEntity.getId());

    }

    @Test
    public void greaterThanJsonbObjectTest() {
        XmEntity firstEntity = createEntity(Map.of(FIRST_DATA_KEY, 1));
        XmEntity secondEntity = createEntity(Map.of(FIRST_DATA_KEY, 3));
        firstEntity = entityRepository.save(firstEntity);
        secondEntity = entityRepository.save(secondEntity);

        List<XmEntity> entities = entityRepository.findAll(Specification.where((root, query, cb) -> {
            JsonbCriteriaBuilder jsonbCriteriaBuilder = new JsonbCriteriaBuilder(cb);
            return jsonbCriteriaBuilder.greaterThan(root, "$.firstDataKey", 2);
        }));

        assertEquals(entities.size(), 1);
        assertEquals(entities.get(0).getId(), secondEntity.getId());

        firstEntity.setData(Map.of(FIRST_DATA_KEY, "AAA"));
        secondEntity.setData(Map.of(FIRST_DATA_KEY, "A"));
        entityRepository.saveAll(List.of(firstEntity, secondEntity));

        entities = entityRepository.findAll(Specification.where((root, query, cb) -> {
            JsonbCriteriaBuilder jsonbCriteriaBuilder = new JsonbCriteriaBuilder(cb);
            return jsonbCriteriaBuilder.greaterThan(root, "$.firstDataKey", "AA");
        }));

        assertEquals(entities.size(), 1);
        assertEquals(entities.get(0).getId(), firstEntity.getId());

    }

    @Test
    public void greaterThanJsonbJsonbTest() {
        XmEntity firstEntity = createEntity(
            Map.of(FIRST_DATA_KEY, 1, SECOND_DATA_KEY, 2));
        firstEntity = entityRepository.save(firstEntity);
        XmEntity secondEntity = createEntity(
            Map.of(FIRST_DATA_KEY, 2, SECOND_DATA_KEY, 1));
        entityRepository.save(secondEntity);

        List<XmEntity> entities = entityRepository.findAll(Specification.where((root, query, cb) -> {
            JsonbCriteriaBuilder jsonbCriteriaBuilder = new JsonbCriteriaBuilder(cb);
            return jsonbCriteriaBuilder.greaterThan(root, "$.firstDataKey", root, "$.secondDataKey");
        }));

        assertEquals(entities.size(), 1);
        assertEquals(entities.get(0).getId(), secondEntity.getId());

        firstEntity.setData(Map.of(FIRST_DATA_KEY, "AAA", SECOND_DATA_KEY, "A"));
        secondEntity.setData(Map.of(FIRST_DATA_KEY, "A", SECOND_DATA_KEY, "AAA"));
        entityRepository.saveAll(List.of(firstEntity, secondEntity));

        entities = entityRepository.findAll(Specification.where((root, query, cb) -> {
            JsonbCriteriaBuilder jsonbCriteriaBuilder = new JsonbCriteriaBuilder(cb);
            return jsonbCriteriaBuilder.greaterThan(root, "$.firstDataKey", root, "$.secondDataKey");
        }));

        assertEquals(entities.size(), 1);
        assertEquals(entities.get(0).getId(), firstEntity.getId());
    }

    @Test
    public void greaterThanJsonbPathTest() {
        XmEntity firstEntity = createEntity(Map.of());
        firstEntity = entityRepository.save(firstEntity);
        firstEntity.setData(Map.of(FIRST_DATA_KEY, firstEntity.getId() + 1));
        firstEntity = entityRepository.save(firstEntity);

        XmEntity secondEntity = createEntity(Map.of());
        secondEntity = entityRepository.save(secondEntity);
        secondEntity.setData(Map.of(FIRST_DATA_KEY, secondEntity.getId()));
        secondEntity = entityRepository.save(secondEntity);

        List<XmEntity> entities = entityRepository.findAll(Specification.where((root, query, cb) -> {
            JsonbCriteriaBuilder jsonbCriteriaBuilder = new JsonbCriteriaBuilder(cb);
            return jsonbCriteriaBuilder.greaterThan(root, "$.firstDataKey", root.get(XmEntity_.id));
        }));

        assertEquals(entities.size(), 1);
        assertEquals(entities.get(0).getId(), firstEntity.getId());
    }

    @Test
    public void greaterThanOrEqualToJsonbObjectTest() {
        XmEntity firstEntity = createEntity(Map.of(FIRST_DATA_KEY, 1));
        XmEntity secondEntity = createEntity(Map.of(FIRST_DATA_KEY, 3));
        firstEntity = entityRepository.save(firstEntity);
        secondEntity = entityRepository.save(secondEntity);

        List<XmEntity> entities = entityRepository.findAll(Specification.where((root, query, cb) -> {
            JsonbCriteriaBuilder jsonbCriteriaBuilder = new JsonbCriteriaBuilder(cb);
            return jsonbCriteriaBuilder.greaterThanOrEqualTo(root, "$.firstDataKey", 3);
        }));

        assertEquals(entities.size(), 1);
        assertEquals(entities.get(0).getId(), secondEntity.getId());

        firstEntity.setData(Map.of(FIRST_DATA_KEY, "AAA"));
        secondEntity.setData(Map.of(FIRST_DATA_KEY, "A"));
        entityRepository.saveAll(List.of(firstEntity, secondEntity));

        entities = entityRepository.findAll(Specification.where((root, query, cb) -> {
            JsonbCriteriaBuilder jsonbCriteriaBuilder = new JsonbCriteriaBuilder(cb);
            return jsonbCriteriaBuilder.greaterThanOrEqualTo(root, "$.firstDataKey", "AAA");
        }));

        assertEquals(entities.size(), 1);
        assertEquals(entities.get(0).getId(), firstEntity.getId());

    }

    @Test
    public void greaterThanOrEqualToJsonbJsonbTest() {
        XmEntity firstEntity = createEntity(
            Map.of(FIRST_DATA_KEY, 1, SECOND_DATA_KEY, 2));
        firstEntity = entityRepository.save(firstEntity);
        XmEntity secondEntity = createEntity(
            Map.of(FIRST_DATA_KEY, 2, SECOND_DATA_KEY, 2));
        entityRepository.save(secondEntity);

        List<XmEntity> entities = entityRepository.findAll(Specification.where((root, query, cb) -> {
            JsonbCriteriaBuilder jsonbCriteriaBuilder = new JsonbCriteriaBuilder(cb);
            return jsonbCriteriaBuilder.greaterThanOrEqualTo(root, "$.firstDataKey", root, "$.secondDataKey");
        }));

        assertEquals(entities.size(), 1);
        assertEquals(entities.get(0).getId(), secondEntity.getId());

        firstEntity.setData(Map.of(FIRST_DATA_KEY, "AAA", SECOND_DATA_KEY, "AAA"));
        secondEntity.setData(Map.of(FIRST_DATA_KEY, "A", SECOND_DATA_KEY, "AAA"));
        entityRepository.saveAll(List.of(firstEntity, secondEntity));

        entities = entityRepository.findAll(Specification.where((root, query, cb) -> {
            JsonbCriteriaBuilder jsonbCriteriaBuilder = new JsonbCriteriaBuilder(cb);
            return jsonbCriteriaBuilder.greaterThanOrEqualTo(root, "$.firstDataKey", root, "$.secondDataKey");
        }));

        assertEquals(entities.size(), 1);
        assertEquals(entities.get(0).getId(), firstEntity.getId());
    }

    @Test
    public void greaterThanOrEqualToJsonbPathTest() {
        XmEntity firstEntity = createEntity(Map.of());
        firstEntity = entityRepository.save(firstEntity);
        firstEntity.setData(Map.of(FIRST_DATA_KEY, firstEntity.getId() - 1));
        firstEntity = entityRepository.save(firstEntity);

        XmEntity secondEntity = createEntity(Map.of());
        secondEntity = entityRepository.save(secondEntity);
        secondEntity.setData(Map.of(FIRST_DATA_KEY, secondEntity.getId()));
        secondEntity = entityRepository.save(secondEntity);

        List<XmEntity> entities = entityRepository.findAll(Specification.where((root, query, cb) -> {
            JsonbCriteriaBuilder jsonbCriteriaBuilder = new JsonbCriteriaBuilder(cb);
            return jsonbCriteriaBuilder.greaterThanOrEqualTo(root, "$.firstDataKey", root.get(XmEntity_.id));
        }));

        assertEquals(entities.size(), 1);
        assertEquals(entities.get(0).getId(), secondEntity.getId());
    }

    @Test
    public void lessThanJsonbObjectTest() {
        XmEntity firstEntity = createEntity(Map.of(FIRST_DATA_KEY, 1));
        XmEntity secondEntity = createEntity(Map.of(FIRST_DATA_KEY, 3));
        firstEntity = entityRepository.save(firstEntity);
        secondEntity = entityRepository.save(secondEntity);

        List<XmEntity> entities = entityRepository.findAll(Specification.where((root, query, cb) -> {
            JsonbCriteriaBuilder jsonbCriteriaBuilder = new JsonbCriteriaBuilder(cb);
            return jsonbCriteriaBuilder.lessThan(root, "$.firstDataKey", 2);
        }));

        assertEquals(entities.size(), 1);
        assertEquals(entities.get(0).getId(), firstEntity.getId());

        firstEntity.setData(Map.of(FIRST_DATA_KEY, "AAA"));
        secondEntity.setData(Map.of(FIRST_DATA_KEY, "A"));
        entityRepository.saveAll(List.of(firstEntity, secondEntity));

        entities = entityRepository.findAll(Specification.where((root, query, cb) -> {
            JsonbCriteriaBuilder jsonbCriteriaBuilder = new JsonbCriteriaBuilder(cb);
            return jsonbCriteriaBuilder.lessThan(root, "$.firstDataKey", "AA");
        }));

        assertEquals(entities.size(), 1);
        assertEquals(entities.get(0).getId(), secondEntity.getId());

    }

    @Test
    public void lessThanJsonbJsonbTest() {
        XmEntity firstEntity = createEntity(
            Map.of(FIRST_DATA_KEY, 1, SECOND_DATA_KEY, 2));
        firstEntity = entityRepository.save(firstEntity);
        XmEntity secondEntity = createEntity(
            Map.of(FIRST_DATA_KEY, 2, SECOND_DATA_KEY, 1));
        entityRepository.save(secondEntity);

        List<XmEntity> entities = entityRepository.findAll(Specification.where((root, query, cb) -> {
            JsonbCriteriaBuilder jsonbCriteriaBuilder = new JsonbCriteriaBuilder(cb);
            return jsonbCriteriaBuilder.lessThan(root, "$.firstDataKey", root, "$.secondDataKey");
        }));

        assertEquals(entities.size(), 1);
        assertEquals(entities.get(0).getId(), firstEntity.getId());

        firstEntity.setData(Map.of(FIRST_DATA_KEY, "AAA", SECOND_DATA_KEY, "A"));
        secondEntity.setData(Map.of(FIRST_DATA_KEY, "A", SECOND_DATA_KEY, "AAA"));
        entityRepository.saveAll(List.of(firstEntity, secondEntity));

        entities = entityRepository.findAll(Specification.where((root, query, cb) -> {
            JsonbCriteriaBuilder jsonbCriteriaBuilder = new JsonbCriteriaBuilder(cb);
            return jsonbCriteriaBuilder.lessThan(root, "$.firstDataKey", root, "$.secondDataKey");
        }));

        assertEquals(entities.size(), 1);
        assertEquals(entities.get(0).getId(), secondEntity.getId());
    }

    @Test
    public void lessThanJsonbPathTest() {
        XmEntity firstEntity = createEntity(Map.of());
        firstEntity = entityRepository.save(firstEntity);
        firstEntity.setData(Map.of(FIRST_DATA_KEY, firstEntity.getId()));
        firstEntity = entityRepository.save(firstEntity);

        XmEntity secondEntity = createEntity(Map.of());
        secondEntity = entityRepository.save(secondEntity);
        secondEntity.setData(Map.of(FIRST_DATA_KEY, secondEntity.getId() - 1));
        secondEntity = entityRepository.save(secondEntity);

        List<XmEntity> entities = entityRepository.findAll(Specification.where((root, query, cb) -> {
            JsonbCriteriaBuilder jsonbCriteriaBuilder = new JsonbCriteriaBuilder(cb);
            return jsonbCriteriaBuilder.lessThan(root, "$.firstDataKey", root.get(XmEntity_.id));
        }));

        assertEquals(entities.size(), 1);
        assertEquals(entities.get(0).getId(), secondEntity.getId());
    }

    @Test
    public void lessThanOrEqualToJsonbObjectTest() {
        XmEntity firstEntity = createEntity(Map.of(FIRST_DATA_KEY, 1));
        XmEntity secondEntity = createEntity(Map.of(FIRST_DATA_KEY, 3));
        firstEntity = entityRepository.save(firstEntity);
        secondEntity = entityRepository.save(secondEntity);

        List<XmEntity> entities = entityRepository.findAll(Specification.where((root, query, cb) -> {
            JsonbCriteriaBuilder jsonbCriteriaBuilder = new JsonbCriteriaBuilder(cb);
            return jsonbCriteriaBuilder.lessThanOrEqualTo(root, "$.firstDataKey", 1);
        }));

        assertEquals(entities.size(), 1);
        assertEquals(entities.get(0).getId(), firstEntity.getId());

        firstEntity.setData(Map.of(FIRST_DATA_KEY, "AAA"));
        secondEntity.setData(Map.of(FIRST_DATA_KEY, "A"));
        entityRepository.saveAll(List.of(firstEntity, secondEntity));

        entities = entityRepository.findAll(Specification.where((root, query, cb) -> {
            JsonbCriteriaBuilder jsonbCriteriaBuilder = new JsonbCriteriaBuilder(cb);
            return jsonbCriteriaBuilder.lessThanOrEqualTo(root, "$.firstDataKey", "A");
        }));

        assertEquals(entities.size(), 1);
        assertEquals(entities.get(0).getId(), secondEntity.getId());

    }

    @Test
    public void lessThanOrEqualToJsonbJsonbTest() {
        XmEntity firstEntity = createEntity(
            Map.of(FIRST_DATA_KEY, 3, SECOND_DATA_KEY, 2));
        firstEntity = entityRepository.save(firstEntity);
        XmEntity secondEntity = createEntity(
            Map.of(FIRST_DATA_KEY, 2, SECOND_DATA_KEY, 2));
        entityRepository.save(secondEntity);

        List<XmEntity> entities = entityRepository.findAll(Specification.where((root, query, cb) -> {
            JsonbCriteriaBuilder jsonbCriteriaBuilder = new JsonbCriteriaBuilder(cb);
            return jsonbCriteriaBuilder.lessThanOrEqualTo(root, "$.firstDataKey", root, "$.secondDataKey");
        }));

        assertEquals(entities.size(), 1);
        assertEquals(entities.get(0).getId(), secondEntity.getId());

        firstEntity.setData(Map.of(FIRST_DATA_KEY, "AAA", SECOND_DATA_KEY, "AAA"));
        secondEntity.setData(Map.of(FIRST_DATA_KEY, "AAAA", SECOND_DATA_KEY, "AAA"));
        entityRepository.saveAll(List.of(firstEntity, secondEntity));

        entities = entityRepository.findAll(Specification.where((root, query, cb) -> {
            JsonbCriteriaBuilder jsonbCriteriaBuilder = new JsonbCriteriaBuilder(cb);
            return jsonbCriteriaBuilder.lessThanOrEqualTo(root, "$.firstDataKey", root, "$.secondDataKey");
        }));

        assertEquals(entities.size(), 1);
        assertEquals(entities.get(0).getId(), firstEntity.getId());
    }

    @Test
    public void lessThanOrEqualToJsonbPathTest() {
        XmEntity firstEntity = createEntity(Map.of());
        firstEntity = entityRepository.save(firstEntity);
        firstEntity.setData(Map.of(FIRST_DATA_KEY, firstEntity.getId() + 1));
        firstEntity = entityRepository.save(firstEntity);

        XmEntity secondEntity = createEntity(Map.of());
        secondEntity = entityRepository.save(secondEntity);
        secondEntity.setData(Map.of(FIRST_DATA_KEY, secondEntity.getId()));
        secondEntity = entityRepository.save(secondEntity);

        List<XmEntity> entities = entityRepository.findAll(Specification.where((root, query, cb) -> {
            JsonbCriteriaBuilder jsonbCriteriaBuilder = new JsonbCriteriaBuilder(cb);
            return jsonbCriteriaBuilder.lessThanOrEqualTo(root, "$.firstDataKey", root.get(XmEntity_.id));
        }));

        assertEquals(entities.size(), 1);
        assertEquals(entities.get(0).getId(), secondEntity.getId());
    }

}
