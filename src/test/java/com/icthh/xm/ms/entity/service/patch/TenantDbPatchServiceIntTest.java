package com.icthh.xm.ms.entity.service.patch;

import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.ms.entity.AbstractSpringBootTest;
import com.icthh.xm.ms.entity.domain.Attachment;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import com.icthh.xm.ms.entity.service.AttachmentService;
import com.icthh.xm.ms.entity.service.SeparateTransactionExecutor;
import com.icthh.xm.ms.entity.service.XmEntityService;
import com.icthh.xm.ms.entity.service.patch.model.CreateSequencePatch;
import com.icthh.xm.ms.entity.service.patch.model.DropIndexPatch;
import com.icthh.xm.ms.entity.service.patch.model.DropSequencePatch;
import com.icthh.xm.ms.entity.service.patch.model.GeneralCreateIndexPatch;
import com.icthh.xm.ms.entity.service.patch.model.JsonPathCreateIndexPatch;
import com.icthh.xm.ms.entity.service.patch.model.XmTenantChangeSet;
import com.icthh.xm.ms.entity.service.patch.model.XmTenantPatch;
import com.icthh.xm.ms.entity.service.patch.model.XmTenantPatchType;
import com.icthh.xm.ms.entity.service.patch.model.XmTenantPatchValidationException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;

import javax.persistence.EntityManager;
import javax.validation.ConstraintViolation;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_AUTH_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

@Slf4j
@Transactional
@ContextConfiguration(initializers = {TenantDbPatchServiceIntTest.Initializer.class})
@ActiveProfiles("pg-test")
public class TenantDbPatchServiceIntTest extends AbstractSpringBootTest {

    @Autowired
    TenantDbPatchService tenantDbPatchService;
    @Autowired
    XmEntityRepository xmEntityRepository;
    @Autowired
    XmEntityService xmEntityService;
    @Autowired
    AttachmentService attachmentService;
    @Autowired
    TenantContextHolder tenantContextHolder;
    @Autowired
    SeparateTransactionExecutor separateTransactionExecutor;
    @Autowired
    JdbcTemplate jdbcTemplate;
    @Autowired
    private XmAuthenticationContextHolder authContextHolder;
    @Autowired
    private LepManager lepManager;
    @Autowired
    private EntityManager em;


    @ClassRule
    public static PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer("postgres:12.7")
            .withDatabaseName("entity")
            .withUsername("sa")
            .withPassword("sa");

    static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
            TestPropertyValues.of(
                    "spring.datasource.url=" + postgreSQLContainer.getJdbcUrl(),
                    "spring.datasource.username=" + postgreSQLContainer.getUsername(),
                    "spring.datasource.password=" + postgreSQLContainer.getPassword()
            ).applyTo(configurableApplicationContext.getEnvironment());
            log.info("spring.datasource.url: {}", postgreSQLContainer.getJdbcUrl());
        }
    }

    @Before
    @BeforeTransaction
    public void beforeTransaction() {
        TenantContextUtils.setTenant(tenantContextHolder, "RESINTTEST");
    }

    @Before
    public void setup() {

        lepManager.beginThreadContext(ctx -> {
            ctx.setValue(THREAD_CONTEXT_KEY_TENANT_CONTEXT, tenantContextHolder.getContext());
            ctx.setValue(THREAD_CONTEXT_KEY_AUTH_CONTEXT, authContextHolder.getContext());
        });

    }

    @After
    public void tearDown() {
        lepManager.endThreadContext();
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
    }

    @Test
    public void testApplyPatchAndAssertSequence() {
        SeparateTransactionExecutor.Task<String> assertion = () -> {
            assertSequence();
            return "";
        };

        applyPatch(assertion);
    }

    // TODO add test by json field

    @Test(expected = TestUniqColumnIndexException.class)
    public void testApplyPatchAndAssertUniqColumnIndex() {
        SeparateTransactionExecutor.Task<String> assertion = () -> {
            var entity = xmEntityService.save(new XmEntity().typeKey("TEST_ENTITY_WITH_ATTACHMENT").name("name").key("key"));
            attachmentService.save(new Attachment().typeKey("PHOTO").name("PHOTO1").xmEntity(entity).contentUrl("uniqURL"));
            attachmentService.save(new Attachment().typeKey("DOCUMENT").name("DOCUMENT").xmEntity(entity).contentUrl("uniqURL"));
            em.flush();
            try {
                attachmentService.save(new Attachment().typeKey("PHOTO").name("PHOTO2").xmEntity(entity).contentUrl("uniqURL"));
                em.flush();
                fail();
            } catch (Exception ex) {
                assertEquals("ERROR: duplicate key value violates unique constraint \"unique_attachment_url\"\n" +
                        "  Detail: Key (content_url)=(uniqURL) already exists.", ex.getCause().getCause().getMessage());
                throw new TestUniqColumnIndexException();
            }
            return "";
        };
        applyPatch(assertion);
    }

    private static class TestUniqColumnIndexException extends RuntimeException {}

    private void applyPatch(SeparateTransactionExecutor.Task<String> assertion) {
        String configPrefix = "/config/tenants/RESINTTEST/entity/dbPatches/";

        assertNotContainsSequences(List.of("testsequencename", "sequencename1"));
        assertNotContainsIndexes(List.of("unique_attachment_url", "entity_value_index", "unique_field2_index"));

        separateTransactionExecutor.doInSeparateTransaction(() -> {
            tenantDbPatchService.onRefresh(configPrefix + "patch1.yml", loadFile("patch1.yml"));
            tenantDbPatchService.onRefresh(configPrefix + "patch2.yml", loadFile("patch2.yml"));
            tenantDbPatchService.refreshFinished(List.of(
                    configPrefix + "patch1.yml", configPrefix + "patch2.yml"
            ));
            return "";
        });

        assertContainsAllSequences(List.of("testsequencename", "sequencename1"));
        assertContainsAllIndexes(List.of("unique_attachment_url", "entity_value_index", "unique_field2_index"));

        separateTransactionExecutor.doInSeparateTransaction(assertion);

        separateTransactionExecutor.doInSeparateTransaction(() -> {
            tenantDbPatchService.onRefresh(configPrefix + "patch3.yml", loadFile("patch3.yml"));
            tenantDbPatchService.refreshFinished(List.of(
                    configPrefix + "patch1.yml", configPrefix + "patch2.yml", configPrefix + "patch3.yml"
            ));
            return "";
        });

        assertNotContainsSequences(List.of("testsequencename", "sequencename1"));
        assertNotContainsIndexes(List.of("unique_attachment_url", "entity_value_index", "unique_field2_index"));
    }

    private void assertNotContainsSequences(List<String> names) {
        var sequences = jdbcTemplate.queryForList("select * from information_schema.sequences");
        assertFalse(sequences.stream().anyMatch(it -> "resinttest".equals(it.get("sequence_schema")) && names.contains(it.get("sequence_name"))));
    }

    private void assertContainsAllSequences(List<String> names) {
        var sequences = jdbcTemplate.queryForList("select * from information_schema.sequences");
        assertEquals(names.size(), sequences.stream().filter(it -> "resinttest".equals(it.get("sequence_schema")) && names.contains(it.get("sequence_name"))).count());
    }

    private void assertNotContainsIndexes(List<String> names) {
        var indexes = jdbcTemplate.queryForList("select * from pg_indexes where tablename not like 'pg%';");
        assertFalse(indexes.stream().anyMatch(it -> "resinttest".equals(it.get("schemaname")) && names.contains(it.get("indexname"))));
    }

    private void assertContainsAllIndexes(List<String> names) {
        var indexes = jdbcTemplate.queryForList("select * from pg_indexes where tablename not like 'pg%'");
        assertEquals(names.size(), indexes.stream().filter(it -> "resinttest".equals(it.get("schemaname")) && names.contains(it.get("indexname"))).count());
    }

    private void assertSequence() {
        long sv = xmEntityRepository.getSequenceNextValString("sequenceName1");
        assertEquals(1, sv);

        sv = xmEntityRepository.getSequenceNextValString("testSequenceName");
        // startValue: 5
        assertEquals(5, sv);
        sv = xmEntityRepository.getSequenceNextValString("testSequenceName");
        assertEquals(6, sv);
        sv = xmEntityRepository.getSequenceNextValString("testSequenceName");
        assertEquals(7, sv);
        sv = xmEntityRepository.getSequenceNextValString("testSequenceName");
        // cycle: true, maxValue: 7, minValue: 3
        assertEquals(3, sv);
    }

    @SneakyThrows
    private String loadFile(String fileName) {
        return IOUtils.toString(getClass().getClassLoader().getResourceAsStream("config/patch/" + fileName), UTF_8);
    }

    @Test
    @SneakyThrows
    public void testDeserialization() {
        for(var operationType: XmTenantPatchType.values()) {
            String patch = "---\ndatabaseChangeLog:\n  - operationType: " + operationType.name();
            XmTenantPatch xmTenantPatch = tenantDbPatchService.readPatch(patch);
            XmTenantChangeSet changeSet = xmTenantPatch.getDatabaseChangeLog().get(0);
            assertEquals(changeSet.getClass(), operationType.getImplementationClass());
        }
    }

    @Test
    public void testValidationCreateSequencePatch() {
        testValidation(new CreateSequencePatch(), List.of(
                Pair.of("changeSetId", "NotBlank"),
                Pair.of("changeSetId", "NotNull"),
                Pair.of("sequenceName", "NotBlank"),
                Pair.of("sequenceName", "NotNull")
        ));
    }

    @Test
    public void testValidationGeneralCreateIndexPatch() {
        testValidation(new GeneralCreateIndexPatch(), List.of(
                Pair.of("changeSetId", "NotBlank"),
                Pair.of("changeSetId", "NotNull"),
                Pair.of("indexExpression", "NotBlank"),
                Pair.of("indexExpression", "NotNull"),
                Pair.of("indexName", "NotBlank"),
                Pair.of("indexName", "NotNull")
        ));
    }

    @Test
    public void testValidationJsonPathCreateIndexPatch() {
        testValidation(new JsonPathCreateIndexPatch(), List.of(
                Pair.of("changeSetId", "NotBlank"),
                Pair.of("changeSetId", "NotNull"),
                Pair.of("indexName", "NotBlank"),
                Pair.of("indexName", "NotNull"),
                Pair.of("jsonPath", "NotBlank"),
                Pair.of("jsonPath", "NotNull")
        ));
    }

    @Test
    public void testValidationDropIndexPatch() {
        testValidation(new DropIndexPatch(), List.of(
                Pair.of("changeSetId", "NotBlank"),
                Pair.of("changeSetId", "NotNull"),
                Pair.of("indexName", "NotBlank"),
                Pair.of("indexName", "NotNull")
        ));
    }

    @Test
    public void testValidationDropSequencePatch() {
        testValidation(new DropSequencePatch(), List.of(
                Pair.of("changeSetId", "NotBlank"),
                Pair.of("changeSetId", "NotNull"),
                Pair.of("sequenceName", "NotBlank"),
                Pair.of("sequenceName", "NotNull")
        ));
    }

    private void testValidation(XmTenantChangeSet xmTenantChangeSet, List<Pair<String, String>> errors) {
        XmTenantPatch patch = new XmTenantPatch();
        patch.getDatabaseChangeLog().add(xmTenantChangeSet);
        try {
            tenantDbPatchService.validate(patch, "fileName");
            fail();
        } catch (XmTenantPatchValidationException e) {
            var constraintViolations = new ArrayList<>(e.getConstraintViolations());
            constraintViolations.sort(Comparator.comparing(it -> it.getPropertyPath() + it.getMessageTemplate()));
            constraintViolations.forEach(it -> {
                log.info("{}, {}", it.getPropertyPath(), it.getMessageTemplate());
            });
            MutableInt i = new MutableInt(0);
            errors.forEach(it -> assertValidation(constraintViolations.get(i.getAndIncrement()), it.getLeft(), it.getRight()));
        }
    }

    private void assertValidation(ConstraintViolation<XmTenantPatch> violation,  String fieldName, String messageTemplate) {
        assertEquals("databaseChangeLog[0]." + fieldName, violation.getPropertyPath().toString());
        assertEquals("{javax.validation.constraints." + messageTemplate + ".message}", violation.getMessageTemplate());
    }

}
