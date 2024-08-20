package com.icthh.xm.ms.entity.config.jsonb;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepScriptConstants.BINDING_KEY_AUTH_CONTEXT;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.junit.Assert.assertEquals;

import com.icthh.xm.commons.migration.db.jsonb.JsonbExpression;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.ms.entity.AbstractJupiterSpringBootTest;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.XmEntity_;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import java.util.List;
import java.util.Map;

import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Slf4j
@Transactional
@ActiveProfiles("pg-test")
@Testcontainers
public class JsonbCriteriaBuilderIntTest extends AbstractJupiterSpringBootTest {

    public static final String FIRST_DATA_KEY = "firstDataKey";
    public static final String SECOND_DATA_KEY = "secondDataKey";
    public static final String FIRST_DATA_VALUE = "firstDataValue";
    public static final String SECOND_DATA_VALUE = "secondDataValue";

    @Container
    public static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:16.3")
        .withExposedPorts(5432)
        .withDatabaseName("entity")
        .withUsername("sa")
        .withPassword("sa");

    @DynamicPropertySource
    static void setPostgreSQLContainer(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
        log.info("spring.datasource.url: {}", postgreSQLContainer.getJdbcUrl());
    }

    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Autowired
    private LepManager lepManager;

    @Autowired
    private XmAuthenticationContextHolder xmAuthenticationContextHolder;

    @Autowired
    private XmEntityRepository entityRepository;

    @Autowired
    private JsonbExpression jsonbExpression;

    @BeforeEach
    @BeforeTransaction
    public void beforeTransaction() {
        TenantContextUtils.setTenant(tenantContextHolder, "TEST");
        lepManager.beginThreadContext(scopedContext -> {
            scopedContext.setValue(THREAD_CONTEXT_KEY_TENANT_CONTEXT, tenantContextHolder.getContext());
            scopedContext.setValue(BINDING_KEY_AUTH_CONTEXT, xmAuthenticationContextHolder.getContext());
        });
    }

    @AfterEach
    public void tearDown() {
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
        lepManager.endThreadContext();
    }

    @Test
    public void searchXmEntityByJsonbDataTest() {
        XmEntity firstEntity = createEntity(Map.of(FIRST_DATA_KEY, FIRST_DATA_VALUE));
        XmEntity secondEntity = createEntity(Map.of(FIRST_DATA_KEY, SECOND_DATA_VALUE));
        XmEntity thirdEntity = createEntity(Map.of());
        entityRepository.saveAll(List.of(firstEntity, secondEntity, thirdEntity));

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

    @Test
    public void equalJsonbObjectTest() {
        XmEntity firstEntity = createEntity(Map.of(FIRST_DATA_KEY, FIRST_DATA_VALUE));
        XmEntity secondEntity = createEntity(Map.of(FIRST_DATA_KEY, SECOND_DATA_VALUE));
        XmEntity thirdEntity = createEntity(Map.of());
        entityRepository.saveAll(List.of(firstEntity, secondEntity, thirdEntity));

        List<XmEntity> entities = entityRepository.findAll(Specification.where((root, query, cb) -> {
            JsonbCriteriaBuilder jsonbCriteriaBuilder = new JsonbCriteriaBuilder(cb, jsonbExpression);
            return jsonbCriteriaBuilder.equalText(root, "$.firstDataKey", SECOND_DATA_VALUE);
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
            JsonbCriteriaBuilder jsonbCriteriaBuilder = new JsonbCriteriaBuilder(cb, jsonbExpression);
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
            JsonbCriteriaBuilder jsonbCriteriaBuilder = new JsonbCriteriaBuilder(cb, jsonbExpression);
            return jsonbCriteriaBuilder.equal(root, "$.secondDataKey", root.get(XmEntity_.id));
        }));

        assertEquals(entities.size(), 1);
        assertEquals(entities.get(0).getId(), firstEntity.getId());

        //select * cross join where a.jsonb = b.id
        entities = entityRepository.findAll(Specification.where((root, query, cb) -> {
            JsonbCriteriaBuilder jsonbCriteriaBuilder = new JsonbCriteriaBuilder(cb, jsonbExpression);
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
            JsonbCriteriaBuilder jsonbCriteriaBuilder = new JsonbCriteriaBuilder(cb, jsonbExpression);
            return jsonbCriteriaBuilder.notEqualText(root, "$.firstDataKey", SECOND_DATA_VALUE);
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
            JsonbCriteriaBuilder jsonbCriteriaBuilder = new JsonbCriteriaBuilder(cb, jsonbExpression);
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
            JsonbCriteriaBuilder jsonbCriteriaBuilder = new JsonbCriteriaBuilder(cb, jsonbExpression);
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
            JsonbCriteriaBuilder jsonbCriteriaBuilder = new JsonbCriteriaBuilder(cb, jsonbExpression);
            return jsonbCriteriaBuilder.greaterThan(root, "$.firstDataKey", 2);
        }));

        assertEquals(entities.size(), 1);
        assertEquals(entities.get(0).getId(), secondEntity.getId());

        firstEntity.setData(Map.of(FIRST_DATA_KEY, "AAA"));
        secondEntity.setData(Map.of(FIRST_DATA_KEY, "A"));
        entityRepository.saveAll(List.of(firstEntity, secondEntity));

        entities = entityRepository.findAll(Specification.where((root, query, cb) -> {
            JsonbCriteriaBuilder jsonbCriteriaBuilder = new JsonbCriteriaBuilder(cb, jsonbExpression);
            return jsonbCriteriaBuilder.greaterThanText(root, "$.firstDataKey", "AA");
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
            JsonbCriteriaBuilder jsonbCriteriaBuilder = new JsonbCriteriaBuilder(cb, jsonbExpression);
            return jsonbCriteriaBuilder.greaterThan(root, "$.firstDataKey", root, "$.secondDataKey");
        }));

        assertEquals(entities.size(), 1);
        assertEquals(entities.get(0).getId(), secondEntity.getId());

        firstEntity.setData(Map.of(FIRST_DATA_KEY, "AAA", SECOND_DATA_KEY, "A"));
        secondEntity.setData(Map.of(FIRST_DATA_KEY, "A", SECOND_DATA_KEY, "AAA"));
        entityRepository.saveAll(List.of(firstEntity, secondEntity));

        entities = entityRepository.findAll(Specification.where((root, query, cb) -> {
            JsonbCriteriaBuilder jsonbCriteriaBuilder = new JsonbCriteriaBuilder(cb, jsonbExpression);
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
            JsonbCriteriaBuilder jsonbCriteriaBuilder = new JsonbCriteriaBuilder(cb, jsonbExpression);
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
            JsonbCriteriaBuilder jsonbCriteriaBuilder = new JsonbCriteriaBuilder(cb, jsonbExpression);
            return jsonbCriteriaBuilder.greaterThanOrEqualTo(root, "$.firstDataKey", 3);
        }));

        assertEquals(entities.size(), 1);
        assertEquals(entities.get(0).getId(), secondEntity.getId());

        firstEntity.setData(Map.of(FIRST_DATA_KEY, "AAA"));
        secondEntity.setData(Map.of(FIRST_DATA_KEY, "A"));
        entityRepository.saveAll(List.of(firstEntity, secondEntity));

        entities = entityRepository.findAll(Specification.where((root, query, cb) -> {
            JsonbCriteriaBuilder jsonbCriteriaBuilder = new JsonbCriteriaBuilder(cb, jsonbExpression);
            return jsonbCriteriaBuilder.greaterThanOrEqualToText(root, "$.firstDataKey", "AAA");
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
            JsonbCriteriaBuilder jsonbCriteriaBuilder = new JsonbCriteriaBuilder(cb, jsonbExpression);
            return jsonbCriteriaBuilder.greaterThanOrEqualTo(root, "$.firstDataKey", root, "$.secondDataKey");
        }));

        assertEquals(entities.size(), 1);
        assertEquals(entities.get(0).getId(), secondEntity.getId());

        firstEntity.setData(Map.of(FIRST_DATA_KEY, "AAA", SECOND_DATA_KEY, "AAA"));
        secondEntity.setData(Map.of(FIRST_DATA_KEY, "A", SECOND_DATA_KEY, "AAA"));
        entityRepository.saveAll(List.of(firstEntity, secondEntity));

        entities = entityRepository.findAll(Specification.where((root, query, cb) -> {
            JsonbCriteriaBuilder jsonbCriteriaBuilder = new JsonbCriteriaBuilder(cb, jsonbExpression);
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
            JsonbCriteriaBuilder jsonbCriteriaBuilder = new JsonbCriteriaBuilder(cb, jsonbExpression);
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
            JsonbCriteriaBuilder jsonbCriteriaBuilder = new JsonbCriteriaBuilder(cb, jsonbExpression);
            return jsonbCriteriaBuilder.lessThan(root, "$.firstDataKey", 2);
        }));

        assertEquals(entities.size(), 1);
        assertEquals(entities.get(0).getId(), firstEntity.getId());

        firstEntity.setData(Map.of(FIRST_DATA_KEY, "AAA"));
        secondEntity.setData(Map.of(FIRST_DATA_KEY, "A"));
        entityRepository.saveAll(List.of(firstEntity, secondEntity));

        entities = entityRepository.findAll(Specification.where((root, query, cb) -> {
            JsonbCriteriaBuilder jsonbCriteriaBuilder = new JsonbCriteriaBuilder(cb, jsonbExpression);
            return jsonbCriteriaBuilder.lessThanText(root, "$.firstDataKey", "AA");
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
            JsonbCriteriaBuilder jsonbCriteriaBuilder = new JsonbCriteriaBuilder(cb, jsonbExpression);
            return jsonbCriteriaBuilder.lessThan(root, "$.firstDataKey", root, "$.secondDataKey");
        }));

        assertEquals(entities.size(), 1);
        assertEquals(entities.get(0).getId(), firstEntity.getId());

        firstEntity.setData(Map.of(FIRST_DATA_KEY, "AAA", SECOND_DATA_KEY, "A"));
        secondEntity.setData(Map.of(FIRST_DATA_KEY, "A", SECOND_DATA_KEY, "AAA"));
        entityRepository.saveAll(List.of(firstEntity, secondEntity));

        entities = entityRepository.findAll(Specification.where((root, query, cb) -> {
            JsonbCriteriaBuilder jsonbCriteriaBuilder = new JsonbCriteriaBuilder(cb, jsonbExpression);
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
            JsonbCriteriaBuilder jsonbCriteriaBuilder = new JsonbCriteriaBuilder(cb, jsonbExpression);
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
            JsonbCriteriaBuilder jsonbCriteriaBuilder = new JsonbCriteriaBuilder(cb, jsonbExpression);
            return jsonbCriteriaBuilder.lessThanOrEqualTo(root, "$.firstDataKey", 1);
        }));

        assertEquals(entities.size(), 1);
        assertEquals(entities.get(0).getId(), firstEntity.getId());

        firstEntity.setData(Map.of(FIRST_DATA_KEY, "AAA"));
        secondEntity.setData(Map.of(FIRST_DATA_KEY, "A"));
        entityRepository.saveAll(List.of(firstEntity, secondEntity));

        entities = entityRepository.findAll(Specification.where((root, query, cb) -> {
            JsonbCriteriaBuilder jsonbCriteriaBuilder = new JsonbCriteriaBuilder(cb, jsonbExpression);
            return jsonbCriteriaBuilder.lessThanOrEqualToText(root, "$.firstDataKey", "A");
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
            JsonbCriteriaBuilder jsonbCriteriaBuilder = new JsonbCriteriaBuilder(cb, jsonbExpression);
            return jsonbCriteriaBuilder.lessThanOrEqualTo(root, "$.firstDataKey", root, "$.secondDataKey");
        }));

        assertEquals(entities.size(), 1);
        assertEquals(entities.get(0).getId(), secondEntity.getId());

        firstEntity.setData(Map.of(FIRST_DATA_KEY, "AAA", SECOND_DATA_KEY, "AAA"));
        secondEntity.setData(Map.of(FIRST_DATA_KEY, "AAAA", SECOND_DATA_KEY, "AAA"));
        entityRepository.saveAll(List.of(firstEntity, secondEntity));

        entities = entityRepository.findAll(Specification.where((root, query, cb) -> {
            JsonbCriteriaBuilder jsonbCriteriaBuilder = new JsonbCriteriaBuilder(cb, jsonbExpression);
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
            JsonbCriteriaBuilder jsonbCriteriaBuilder = new JsonbCriteriaBuilder(cb, jsonbExpression);
            return jsonbCriteriaBuilder.lessThanOrEqualTo(root, "$.firstDataKey", root.get(XmEntity_.id));
        }));

        assertEquals(entities.size(), 1);
        assertEquals(entities.get(0).getId(), secondEntity.getId());
    }

    @Test
    public void jsonExtractPathTextTestOneValuePath() {
        XmEntity firstEntity = createEntity(Map.of(FIRST_DATA_KEY, "AAA"));
        firstEntity = entityRepository.save(firstEntity);

        String jpql = "SELECT jsonb_to_string(e.data, :firstDataKey) FROM XmEntity e";

        Map<String, Object> queryParams = Map.of(FIRST_DATA_KEY, new String[]{FIRST_DATA_KEY});

        List<?> firstDataKeys = entityRepository.findAll(jpql, queryParams);

        assertEquals(firstDataKeys.size(), 1);
        assertEquals(firstDataKeys.get(0), "AAA");
    }

    @Test
    public void jsonExtractPathTextTestMultipleValuePath() {
        XmEntity firstEntity = createEntity(Map.of(FIRST_DATA_KEY, Map.of(SECOND_DATA_KEY, "AAA")));
        firstEntity = entityRepository.save(firstEntity);

        String jpql = "SELECT jsonb_to_string(e.data, :firstDataKey) FROM XmEntity e";

        Map<String, Object> queryParams = Map.of(FIRST_DATA_KEY, new String[]{FIRST_DATA_KEY, SECOND_DATA_KEY});

        List<?> firstDataKeys = entityRepository.findAll(jpql, queryParams);

        assertEquals(firstDataKeys.size(), 1);
        assertEquals(firstDataKeys.get(0), "AAA");
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
