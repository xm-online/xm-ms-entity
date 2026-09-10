# DB-backed Search, Filtering and JPQL Templates Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add four Elasticsearch-free search endpoints for XmEntity: full text + filter search by typeKey, link-dialog candidate search, link search by source entity, and JPQL templates, all running on Postgres with an Oracle fallback.

**Architecture:** Filters and sort are built as JPA Criteria `Specification`s (jsonb paths through the HQL function `json_query` that xm-commons registers for Postgres and Oracle). Row-level permissions are merged by parsing the xm-commons permission JPQL with Hibernate 7 `HibernateCriteriaBuilder.createQuery(hql, Class)` and ANDing the Specification onto the parsed query. Full text uses a new `xm_entity.search_text` column maintained by a JPA listener from `TypeSpec.fullTextSearch*`, indexed with `pg_trgm`. JPQL templates come from tenant config (`ENTITY` fragments with permissions, or `RAW` full queries returning rows).

**Tech Stack:** Java 25, Spring Boot 4.0.4, Hibernate 7.3.1 (`org.hibernate.query.criteria.HibernateCriteriaBuilder`), xm-commons 5.0.41 (`xm-commons-permission`, `xm-commons-migration-db`), Liquibase, PostgreSQL 14 via Testcontainers, JUnit 5, Lombok, MapStruct, Jackson 3 (`tools.jackson`) for YAML.

**Spec:** `docs/superpowers/specs/2026-09-09-db-search-and-filtering-design.md`

## Global Constraints

- Alias exposed to templates and permission merge is `entity`; xm-commons emits `returnObject`, rewrite it before merging.
- New privileges only: `XMENTITY.SEARCH.DB.QUERY`, `XMENTITY.SEARCH.DB.TO_LINK`, `LINK.SEARCH.DB.TARGETS`, `XMENTITY.SEARCH.DB.TEMPLATE`, `XMENTITY.SEARCH.DB.REINDEX` (resource), `XMENTITY.SEARCH.DB`, `LINK.SEARCH.DB` (row-level). Never reuse `XMENTITY.SEARCH`.
- Filter grammar `<field>.<op>`, ops `eq, notEq, in, notIn, contains, specified, gt, gte, lt, lte`; column whitelist `id, key, typeKey, stateKey, name, description, startDate, updateDate, endDate, createdBy, updatedBy, removed`; data fields `data.<path>`.
- `typeKey` match includes dotted subtypes by default, `includeSubTypes=false` for exact.
- Soft-deleted rows (`removed = true`) excluded unless filter has `removed.eq=true`.
- Full text: substring, case-insensitive, `ILIKE` on `search_text` (escape `\`, `%`, `_`), partial GIN `gin_trgm_ops` index on Postgres.
- Template params bound with `setParameter` only, never string substitution. Subject params `:subjectUserKey`, `:subjectLogin`, `:subjectTenant`.
- All new integration tests run on Postgres via Testcontainers (`pg-test` profile). No H2 emulation of JSON functions.
- Errors: unknown field/op/sort, bad value, missing template param → `400` `BusinessException(ERR_VALIDATION, ...)`; unknown template → `404` (`EntityNotFoundException`).
- Java code style follows the repo: Lombok `@RequiredArgsConstructor`, `@Slf4j`, 4-space indent, JPA metamodel constants (`XmEntity_.DATA`), MapStruct mappers for DTOs.
- Every public method of `XmEntityDbSearchService` and `XmEntitySearchTextReindexService` carries `@LogicExtensionPoint(value = ..., resolver = ...)` with a key resolver from `com.icthh.xm.ms.entity.lep.keyresolver` (existing `LepKeyResolver` pattern: `segments(LepMethod)` reads named params via `method.getParameter(name, Class)`).
- Commit messages are plain imperative sentences; attribution trailers follow the session policy in effect.

## Spec deviations decided in this plan

- `JsonbCriteriaBuilder` is left untouched. Filters call `cb.function(...)` directly through `JsonValueStrategy`, because the Link target search needs a `Path<XmEntity>` (not a `Root<XmEntity>`), which `JsonbCriteriaBuilder`/`CustomExpression` do not accept.
- `contains` on data fields casts `json_query(...)` to string on Postgres (`... as varchar`) instead of `jsonb_extract_path_text`, because the registered `jsonb_extract_path_text` pattern is fixed at two arguments.
- Subject param `:subjectRoleKey` is dropped (the auth context exposes an authority set, not a single role). Remaining: `:subjectUserKey`, `:subjectLogin`, `:subjectTenant`.
- RAW template scalars are returned exactly as the JDBC driver returns them (a `json_query` value arrives as a JSON string); no JSON re-parsing.

## File map

Create:
- `src/main/java/com/icthh/xm/ms/entity/repository/search/db/PermittedSpecificationRepository.java`
- `src/main/java/com/icthh/xm/ms/entity/repository/search/db/OrderProvider.java`
- `src/main/java/com/icthh/xm/ms/entity/service/search/db/filter/FilterOperator.java`
- `src/main/java/com/icthh/xm/ms/entity/service/search/db/filter/FilterCondition.java`
- `src/main/java/com/icthh/xm/ms/entity/service/search/db/filter/FilterParser.java`
- `src/main/java/com/icthh/xm/ms/entity/service/search/db/filter/XmEntityFilterSpecificationBuilder.java`
- `src/main/java/com/icthh/xm/ms/entity/service/search/db/filter/SortTranslator.java`
- `src/main/java/com/icthh/xm/ms/entity/service/search/db/dialect/JsonValueStrategy.java`
- `src/main/java/com/icthh/xm/ms/entity/service/search/db/dialect/PostgresJsonValueStrategy.java`
- `src/main/java/com/icthh/xm/ms/entity/service/search/db/dialect/OracleJsonValueStrategy.java`
- `src/main/java/com/icthh/xm/ms/entity/service/search/db/SearchTextBuilder.java`
- `src/main/java/com/icthh/xm/ms/entity/service/search/db/XmEntityDbSearchService.java`
- `src/main/java/com/icthh/xm/ms/entity/service/search/db/XmEntitySearchTextReindexService.java`
- `src/main/java/com/icthh/xm/ms/entity/service/search/db/dto/XmEntityDbSearchRequest.java`
- `src/main/java/com/icthh/xm/ms/entity/service/search/db/template/JpqlTemplate.java`
- `src/main/java/com/icthh/xm/ms/entity/service/search/db/template/JpqlTemplateType.java`
- `src/main/java/com/icthh/xm/ms/entity/service/search/db/template/XmEntityJpqlTemplatesService.java`
- `src/main/java/com/icthh/xm/ms/entity/service/search/db/template/JpqlTemplateExecutor.java`
- `src/main/java/com/icthh/xm/ms/entity/domain/listener/XmEntitySearchTextListener.java`
- `src/main/java/com/icthh/xm/ms/entity/lep/keyresolver/DbSearchRequestTypeKeyResolver.java`
- `src/main/java/com/icthh/xm/ms/entity/lep/keyresolver/EntityTypeKeyAndLinkTypeKeyResolver.java`
- `src/main/java/com/icthh/xm/ms/entity/lep/keyresolver/LinkTypeKeyParamResolver.java`
- `src/main/java/com/icthh/xm/ms/entity/lep/keyresolver/JpqlTemplateKeyResolver.java`
- `src/main/java/com/icthh/xm/ms/entity/web/rest/XmEntityDbSearchResource.java`
- `src/main/java/com/icthh/xm/ms/entity/web/rest/facade/XmEntityDbSearchFacade.java`
- `src/main/resources/config/liquibase/changelog/20260909000000_add_search_text_to_xm_entity.xml`
- `src/test/java/com/icthh/xm/ms/entity/AbstractPostgresIntTest.java`
- `src/test/java/com/icthh/xm/ms/entity/config/PostgresTestContainer.java`
- `src/test/resources/config/specs/xmentityspec-dbsearch.yml`
- `src/test/resources/config/templates/jpql-templates-dbsearch.yml`
- tests listed per task

Modify:
- `src/main/java/com/icthh/xm/ms/entity/domain/XmEntity.java` (field `searchText`, listener registration)
- `src/main/java/com/icthh/xm/ms/entity/domain/spec/TypeSpec.java` (two fields)
- `src/main/java/com/icthh/xm/ms/entity/config/ApplicationProperties.java` (two properties)
- `src/main/resources/config/application.yml`, `src/test/resources/config/application.yml` (two properties)
- `src/main/resources/config/liquibase/master.xml`
- `src/main/java/com/icthh/xm/ms/entity/web/rest/util/PaginationUtil.java` (one method)
- `src/test/resources/config/privileges/permissions.yml` (new privilege keys)

---

### Task 1: Postgres integration-test base and fixtures

**Files:**
- Create: `src/test/java/com/icthh/xm/ms/entity/config/PostgresTestContainer.java`
- Create: `src/test/java/com/icthh/xm/ms/entity/AbstractPostgresIntTest.java`
- Create: `src/test/resources/config/specs/xmentityspec-dbsearch.yml`
- Test: `src/test/java/com/icthh/xm/ms/entity/PostgresInfrastructureIntTest.java`

**Interfaces:**
- Produces: `AbstractPostgresIntTest` (abstract, `@ActiveProfiles("pg-test")`, tenant `TEST`, LEP thread context, helper `pushDbSearchSpec()`, helper `XmEntity newEntity(String typeKey, String name, Map<String,Object> data)`), constant `AbstractPostgresIntTest.TENANT = "TEST"`.
- Consumes: existing `AbstractJupiterSpringBootTest`, `XmEntitySpecService.onRefresh(key, yml)`, `ApplicationProperties.getSpecificationPathPattern()`.

Why a singleton container: every Postgres test class shares one Spring context (same profile, same JDBC URL), so Liquibase runs once. A per-class `@Container` would give each class a new URL and a new context.

- [ ] **Step 1: Create the singleton container**

```java
package com.icthh.xm.ms.entity.config;

import org.testcontainers.containers.PostgreSQLContainer;

public final class PostgresTestContainer {

    private static final PostgreSQLContainer<?> INSTANCE = new PostgreSQLContainer<>("postgres:14.17")
        .withDatabaseName("entity")
        .withUsername("sa")
        .withPassword("sa");

    private PostgresTestContainer() {
    }

    public static PostgreSQLContainer<?> getInstance() {
        if (!INSTANCE.isRunning()) {
            INSTANCE.start();
        }
        return INSTANCE;
    }
}
```

- [ ] **Step 2: Create the base class**

```java
package com.icthh.xm.ms.entity;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepScriptConstants.BINDING_KEY_AUTH_CONTEXT;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.ms.entity.config.ApplicationProperties;
import com.icthh.xm.ms.entity.config.PostgresTestContainer;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.service.XmEntitySpecService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.transaction.BeforeTransaction;

@ActiveProfiles("pg-test")
public abstract class AbstractPostgresIntTest extends AbstractJupiterSpringBootTest {

    public static final String TENANT = "TEST";

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        var container = PostgresTestContainer.getInstance();
        registry.add("spring.datasource.url", container::getJdbcUrl);
        registry.add("spring.datasource.username", container::getUsername);
        registry.add("spring.datasource.password", container::getPassword);
    }

    @Autowired
    protected TenantContextHolder tenantContextHolder;
    @Autowired
    protected LepManager lepManager;
    @Autowired
    protected XmAuthenticationContextHolder xmAuthenticationContextHolder;
    @Autowired
    protected XmEntitySpecService xmEntitySpecService;
    @Autowired
    protected ApplicationProperties applicationProperties;

    @BeforeEach
    @BeforeTransaction
    public void setUpTenantContext() {
        TenantContextUtils.setTenant(tenantContextHolder, TENANT);
        lepManager.beginThreadContext(scopedContext -> {
            scopedContext.setValue(THREAD_CONTEXT_KEY_TENANT_CONTEXT, tenantContextHolder.getContext());
            scopedContext.setValue(BINDING_KEY_AUTH_CONTEXT, xmAuthenticationContextHolder.getContext());
        });
    }

    @AfterEach
    public void tearDownTenantContext() {
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
        lepManager.endThreadContext();
    }

    @SneakyThrows
    protected void pushDbSearchSpec() {
        String yml = IOUtils.toString(
            new ClassPathResource("config/specs/xmentityspec-dbsearch.yml").getInputStream(), UTF_8);
        String key = applicationProperties.getSpecificationPathPattern().replace("{tenantName}", TENANT);
        xmEntitySpecService.onRefresh(key, yml);
        xmEntitySpecService.refreshFinished(List.of(key));
    }

    protected static XmEntity newEntity(String typeKey, String name, Map<String, Object> data) {
        return new XmEntity()
            .typeKey(typeKey)
            .key(UUID.randomUUID().toString())
            .name(name)
            .startDate(Instant.now())
            .updateDate(Instant.now())
            .data(data);
    }
}
```

If `refreshFinished` does not exist on `XmEntitySpecService`, drop that line (it exists on `XmEntityTemplatesSpecService`; check with `grep -n refreshFinished src/main/java/com/icthh/xm/ms/entity/service/XmEntitySpecService.java`).

- [ ] **Step 3: Create the spec fixture** `src/test/resources/config/specs/xmentityspec-dbsearch.yml`

```yaml
---
types:
    - key: ORDER
      name: { en: "Order" }
      isApp: true
      fullTextSearch: true
      fullTextSearchDataFields:
          - data.orderNo
          - data.customer.city
          - data.tags
      dataSpec: |
          { "type": "object", "properties": {
              "orderNo": { "type": "number" },
              "position": { "type": "number" },
              "customer": { "type": "object", "properties": { "city": { "type": "string" } } },
              "tags": { "type": "array", "items": { "type": "string" } } } }
      links:
          - key: ORDER.ITEM
            builderType: SEARCH
            typeKey: PRODUCT
            isUnique: true
            name: { en: "Items (unique)" }
          - key: ORDER.NOTE
            builderType: SEARCH
            typeKey: PRODUCT
            isUnique: false
            name: { en: "Notes" }
    - key: ORDER.EXPRESS
      name: { en: "Express order" }
    - key: PRODUCT
      name: { en: "Product" }
      fullTextSearch: false
    - key: SILENT
      name: { en: "No text search" }
```

`fullTextSearch` and `fullTextSearchDataFields` do not exist on `TypeSpec` until Task 6. The spec parser rejects unknown properties (`additionalProperties: false` in the generated schema), so until Task 6 the fixture must be loaded WITHOUT those two keys. Do this: create the file now without the two `fullTextSearch*` lines, and Task 6 adds them.

- [ ] **Step 4: Write the smoke test**

`src/test/java/com/icthh/xm/ms/entity/PostgresInfrastructureIntTest.java`

```java
package com.icthh.xm.ms.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import jakarta.persistence.EntityManager;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class PostgresInfrastructureIntTest extends AbstractPostgresIntTest {

    @Autowired
    private EntityManager em;

    @Test
    public void runsOnPostgresAndLoadsDbSearchSpec() {
        pushDbSearchSpec();

        String version = (String) em.createNativeQuery("select version()").getSingleResult();
        assertThat(version).startsWith("PostgreSQL");

        TypeSpec order = xmEntitySpecService.getTypeSpecByKey("ORDER").orElseThrow();
        assertThat(order.findLinkSpec("ORDER.ITEM")).isPresent();
        assertThat(xmEntitySpecService.getTypeSpecByKey("ORDER.EXPRESS")).isPresent();
    }
}
```

- [ ] **Step 5: Run it**

Run: `./gradlew test --tests '*PostgresInfrastructureIntTest*' -x runCategorizedTests`
Expected: PASS (Docker must be running). If Gradle complains about the `dependsOn 'clean'` ordering, run `./gradlew cleanTest test --tests ...` instead.

- [ ] **Step 6: Commit**

```bash
git add src/test/java/com/icthh/xm/ms/entity/config/PostgresTestContainer.java \
        src/test/java/com/icthh/xm/ms/entity/AbstractPostgresIntTest.java \
        src/test/resources/config/specs/xmentityspec-dbsearch.yml \
        src/test/java/com/icthh/xm/ms/entity/PostgresInfrastructureIntTest.java
git commit -m "Add shared Postgres Testcontainers base for integration tests"
```

---

### Task 2: PermittedSpecificationRepository (spike + implementation)

**Files:**
- Create: `src/main/java/com/icthh/xm/ms/entity/repository/search/db/OrderProvider.java`
- Create: `src/main/java/com/icthh/xm/ms/entity/repository/search/db/PermittedSpecificationRepository.java`
- Test: `src/test/java/com/icthh/xm/ms/entity/repository/search/db/PermittedSpecificationRepositoryIntTest.java`

**Interfaces:**
- Produces:
  ```java
  @FunctionalInterface
  public interface OrderProvider<T> { List<Order> orders(Root<T> root, CriteriaBuilder cb); }

  public class PermittedSpecificationRepository {
      public static final String ALIAS = "entity";
      public <T> Page<T> findAll(Class<T> entityClass, Specification<T> spec, OrderProvider<T> orders,
                                 Pageable pageable, String privilegeKey);
      public <T> Page<T> findAll(Class<T> entityClass, String whereFragment, Map<String, Object> params,
                                 Specification<T> spec, OrderProvider<T> orders, Pageable pageable, String privilegeKey);
  }
  ```
  `spec`, `orders`, `whereFragment` may be null. `privilegeKey` is the value the `@FindWithPermission` aspect injects (may be null → no permission condition).
- Consumes: `com.icthh.xm.commons.permission.service.PermissionCheckService#createCondition(Authentication, Object, SpelTranslator)`, `com.icthh.xm.commons.permission.service.translator.SpelToJpqlTranslator`.

- [ ] **Step 1: Write the failing test**

```java
package com.icthh.xm.ms.entity.repository.search.db;

import static org.assertj.core.api.Assertions.assertThat;

import com.icthh.xm.commons.permission.service.PermissionCheckService;
import com.icthh.xm.commons.permission.service.translator.SpelTranslator;
import com.icthh.xm.ms.entity.AbstractPostgresIntTest;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.XmEntity_;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class PermittedSpecificationRepositoryIntTest extends AbstractPostgresIntTest {

    @Autowired
    private PermittedSpecificationRepository repository;
    @Autowired
    private XmEntityRepository xmEntityRepository;
    @MockitoBean
    private PermissionCheckService permissionCheckService;

    private XmEntity mine;
    private XmEntity other;

    @BeforeEach
    public void seed() {
        mine = xmEntityRepository.save(newEntity("PERM_TEST", "mine", Map.of("orderNo", 1)).createdBy("user-1"));
        other = xmEntityRepository.save(newEntity("PERM_TEST", "other", Map.of("orderNo", 2)).createdBy("user-2"));
        xmEntityRepository.save(newEntity("PERM_TEST_OTHER_TYPE", "third", Map.of()).createdBy("user-1"));
    }

    private void permissionCondition(String jpqlWithReturnObjectAlias) {
        Mockito.when(permissionCheckService.createCondition(Mockito.any(), Mockito.eq("PRIV"), Mockito.any(SpelTranslator.class)))
            .thenReturn(jpqlWithReturnObjectAlias);
    }

    private static Specification<XmEntity> typeKey(String typeKey) {
        return (root, query, cb) -> cb.equal(root.get(XmEntity_.typeKey), typeKey);
    }

    @Test
    public void specificationAndPermissionConditionAreBothApplied() {
        permissionCondition("returnObject.createdBy = 'user-1'");

        Page<XmEntity> page = repository.findAll(XmEntity.class, typeKey("PERM_TEST"), null, PageRequest.of(0, 10), "PRIV");

        assertThat(page.getContent()).extracting(XmEntity::getId).containsExactly(mine.getId());
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    public void emptyPermissionConditionAppliesOnlySpecification() {
        permissionCondition("");

        Page<XmEntity> page = repository.findAll(XmEntity.class, typeKey("PERM_TEST"), null, PageRequest.of(0, 10), "PRIV");

        assertThat(page.getContent()).extracting(XmEntity::getId).containsExactlyInAnyOrder(mine.getId(), other.getId());
        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    @Test
    public void ordersAndPagingAreApplied() {
        permissionCondition("");
        OrderProvider<XmEntity> byNameDesc = (root, cb) -> List.of(cb.desc(root.get(XmEntity_.name)));

        Page<XmEntity> first = repository.findAll(XmEntity.class, typeKey("PERM_TEST"), byNameDesc, PageRequest.of(0, 1), "PRIV");
        Page<XmEntity> second = repository.findAll(XmEntity.class, typeKey("PERM_TEST"), byNameDesc, PageRequest.of(1, 1), "PRIV");

        assertThat(first.getContent()).extracting(XmEntity::getName).containsExactly("other");
        assertThat(second.getContent()).extracting(XmEntity::getName).containsExactly("mine");
        assertThat(first.getTotalElements()).isEqualTo(2);
    }

    @Test
    public void whereFragmentWithNamedParamsIsCombinedWithPermissionAndSpecification() {
        permissionCondition("returnObject.typeKey = 'PERM_TEST'");

        Page<XmEntity> page = repository.findAll(XmEntity.class,
            "entity.name = :name", Map.of("name", "other"),
            null, null, PageRequest.of(0, 10), "PRIV");

        assertThat(page.getContent()).extracting(XmEntity::getId).containsExactly(other.getId());
    }

    @Test
    public void nullPrivilegeKeySkipsPermissionCondition() {
        Page<XmEntity> page = repository.findAll(XmEntity.class, typeKey("PERM_TEST"), null, PageRequest.of(0, 10), null);
        assertThat(page.getTotalElements()).isEqualTo(2);
    }
}
```

- [ ] **Step 2: Run it to see it fail**

Run: `./gradlew test --tests '*PermittedSpecificationRepositoryIntTest*' -x runCategorizedTests`
Expected: compilation failure, `PermittedSpecificationRepository` does not exist.

- [ ] **Step 3: Implement**

`OrderProvider.java`:

```java
package com.icthh.xm.ms.entity.repository.search.db;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Root;
import java.util.List;

@FunctionalInterface
public interface OrderProvider<T> {
    List<Order> orders(Root<T> root, CriteriaBuilder cb);
}
```

`PermittedSpecificationRepository.java`:

```java
package com.icthh.xm.ms.entity.repository.search.db;

import com.icthh.xm.commons.permission.service.PermissionCheckService;
import com.icthh.xm.commons.permission.service.translator.SpelToJpqlTranslator;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Repository;

/**
 * Executes a JPA {@link Specification} together with the xm-commons row-level permission condition.
 * The permission SpEL is translated to JPQL by xm-commons (alias {@code returnObject}), the alias is
 * rewritten to {@value #ALIAS}, the resulting HQL is parsed into a criteria query and the specification
 * predicate is ANDed onto it.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class PermittedSpecificationRepository {

    public static final String ALIAS = "entity";
    private static final String COMMONS_ALIAS = "returnObject";

    private final EntityManager em;
    private final PermissionCheckService permissionCheckService;
    private final SpelToJpqlTranslator spelToJpqlTranslator = new SpelToJpqlTranslator();

    public <T> Page<T> findAll(Class<T> entityClass, Specification<T> spec, OrderProvider<T> orders,
                               Pageable pageable, String privilegeKey) {
        return findAll(entityClass, null, Map.of(), spec, orders, pageable, privilegeKey);
    }

    public <T> Page<T> findAll(Class<T> entityClass, String whereFragment, Map<String, Object> params,
                               Specification<T> spec, OrderProvider<T> orders, Pageable pageable,
                               String privilegeKey) {
        String where = buildWhere(whereFragment, privilegeKey);
        String entityName = em.getMetamodel().entity(entityClass).getName();
        HibernateCriteriaBuilder cb = (HibernateCriteriaBuilder) em.getCriteriaBuilder();

        JpaCriteriaQuery<T> select = cb.createQuery("select " + ALIAS + " from " + entityName + " " + ALIAS + where, entityClass);
        Root<T> root = singleRoot(select);
        applySpec(select, root, spec, cb);
        if (orders != null) {
            select.orderBy(orders.orders(root, cb));
        }
        TypedQuery<T> selectQuery = em.createQuery(select);
        bind(selectQuery, params);
        if (pageable != null && pageable.isPaged()) {
            selectQuery.setFirstResult((int) pageable.getOffset());
            selectQuery.setMaxResults(pageable.getPageSize());
        }
        List<T> content = selectQuery.getResultList();

        if (pageable == null || pageable.isUnpaged()) {
            return new PageImpl<>(content);
        }
        JpaCriteriaQuery<Long> count = cb.createQuery("select count(" + ALIAS + ") from " + entityName + " " + ALIAS + where, Long.class);
        applySpec(count, singleRoot(count), spec, cb);
        TypedQuery<Long> countQuery = em.createQuery(count);
        bind(countQuery, params);
        long total = countQuery.getSingleResult();
        return new PageImpl<>(content, pageable, total);
    }

    private String buildWhere(String whereFragment, String privilegeKey) {
        List<String> parts = new ArrayList<>();
        if (StringUtils.isNotBlank(whereFragment)) {
            parts.add("(" + whereFragment + ")");
        }
        if (privilegeKey != null) {
            String condition = permissionCheckService.createCondition(
                SecurityContextHolder.getContext().getAuthentication(), privilegeKey, spelToJpqlTranslator);
            if (StringUtils.isNotBlank(condition)) {
                parts.add("(" + condition.replace(COMMONS_ALIAS, ALIAS) + ")");
            }
        }
        return parts.isEmpty() ? "" : " where " + String.join(" and ", parts);
    }

    @SuppressWarnings("unchecked")
    private static <T> Root<T> singleRoot(JpaCriteriaQuery<?> query) {
        return (Root<T>) query.getRoots().iterator().next();
    }

    private static <T> void applySpec(JpaCriteriaQuery<?> query, Root<T> root, Specification<T> spec,
                                      HibernateCriteriaBuilder cb) {
        if (spec == null) {
            return;
        }
        Predicate predicate = spec.toPredicate(root, query, cb);
        if (predicate == null) {
            return;
        }
        Predicate existing = query.getRestriction();
        query.where(existing == null ? predicate : cb.and(existing, predicate));
    }

    private static void bind(TypedQuery<?> query, Map<String, Object> params) {
        query.getParameters().forEach(p -> {
            if (p.getName() != null && params.containsKey(p.getName())) {
                query.setParameter(p.getName(), params.get(p.getName()));
            }
        });
    }
}
```

- [ ] **Step 4: Run the test**

Run: `./gradlew test --tests '*PermittedSpecificationRepositoryIntTest*' -x runCategorizedTests`
Expected: PASS. This is the spike gate. If `getRoots()` is empty or `toPredicate` fails on the parsed root, stop and report: the fallback is a `SpelToPredicateTranslator` (not planned here); do not continue to Task 4 before this passes.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/icthh/xm/ms/entity/repository/search/db src/test/java/com/icthh/xm/ms/entity/repository/search/db
git commit -m "Add PermittedSpecificationRepository merging specifications with permission JPQL"
```

---

### Task 3: Filter grammar parser

**Files:**
- Create: `src/main/java/com/icthh/xm/ms/entity/service/search/db/filter/FilterOperator.java`
- Create: `src/main/java/com/icthh/xm/ms/entity/service/search/db/filter/FilterCondition.java`
- Create: `src/main/java/com/icthh/xm/ms/entity/service/search/db/filter/FilterParser.java`
- Test: `src/test/java/com/icthh/xm/ms/entity/service/search/db/filter/FilterParserUnitTest.java`

**Interfaces:**
- Produces:
  ```java
  public enum FilterOperator { EQ, NOT_EQ, IN, NOT_IN, CONTAINS, SPECIFIED, GT, GTE, LT, LTE;
      public String suffix(); public static Optional<FilterOperator> bySuffix(String s); public boolean isMultiValue(); }

  public record FilterCondition(String field, FilterOperator operator, List<Object> values) {
      public static final String DATA_PREFIX = "data.";
      public boolean isDataField();          // field starts with "data."
      public String dataJsonPath();          // "data.a.b" -> "$.a.b"
      public Object singleValue();           // values.get(0)
  }

  public class FilterParser {                // Spring @Component, stateless
      public static final Set<String> RESERVED_PARAMS = Set.of("page", "size", "sort", "typeKey", "query", "includeSubTypes");
      public List<FilterCondition> parseBody(Map<String, Object> filter);                      // POST
      public List<FilterCondition> parseQueryParams(Map<String, List<String>> queryParams);     // GET
  }
  ```
- Value typing: body keeps JSON types; for `in`/`notIn` the body value must be a list. GET values: `in`/`notIn` split on `,`; each scalar parsed as `Long` (`-?\d+`), else `Double` (`-?\d+\.\d+`), else `Boolean` (`true|false`), else `String`. `specified` value must be boolean.
- Errors: `BusinessException(ERR_VALIDATION, "Unknown filter operator in key: <key>")`, `"Filter operator <op> requires a list value: <key>"`, `"Filter key must be <field>.<operator>: <key>"`.

- [ ] **Step 1: Write the failing unit test**

```java
package com.icthh.xm.ms.entity.service.search.db.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.ms.entity.AbstractJupiterUnitTest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class FilterParserUnitTest extends AbstractJupiterUnitTest {

    private final FilterParser parser = new FilterParser();

    @Test
    public void parsesBodyWithTypedValues() {
        List<FilterCondition> conditions = parser.parseBody(Map.of(
            "data.order.in", List.of(1, 2, 3),
            "data.subObject.position.eq", 5,
            "name.contains", "abc",
            "removed.eq", true));

        assertThat(conditions).containsExactlyInAnyOrder(
            new FilterCondition("data.order", FilterOperator.IN, List.of(1, 2, 3)),
            new FilterCondition("data.subObject.position", FilterOperator.EQ, List.of(5)),
            new FilterCondition("name", FilterOperator.CONTAINS, List.of("abc")),
            new FilterCondition("removed", FilterOperator.EQ, List.of(true)));
    }

    @Test
    public void parsesQueryParamsAndInfersTypes() {
        List<FilterCondition> conditions = parser.parseQueryParams(Map.of(
            "data.order.in", List.of("1,2,3"),
            "data.price.gt", List.of("10.5"),
            "data.flag.specified", List.of("true"),
            "stateKey.eq", List.of("ACTIVE"),
            "page", List.of("0"), "size", List.of("20"), "sort", List.of("name,asc"),
            "typeKey", List.of("ORDER"), "query", List.of("x"), "includeSubTypes", List.of("false")));

        assertThat(conditions).containsExactlyInAnyOrder(
            new FilterCondition("data.order", FilterOperator.IN, List.of(1L, 2L, 3L)),
            new FilterCondition("data.price", FilterOperator.GT, List.of(10.5d)),
            new FilterCondition("data.flag", FilterOperator.SPECIFIED, List.of(true)),
            new FilterCondition("stateKey", FilterOperator.EQ, List.of("ACTIVE")));
    }

    @Test
    public void dataJsonPathIsDerivedFromField() {
        FilterCondition c = new FilterCondition("data.subObject.position", FilterOperator.EQ, List.of(5));
        assertThat(c.isDataField()).isTrue();
        assertThat(c.dataJsonPath()).isEqualTo("$.subObject.position");
        assertThat(new FilterCondition("name", FilterOperator.EQ, List.of("a")).isDataField()).isFalse();
    }

    @Test
    public void rejectsUnknownOperator() {
        assertThatThrownBy(() -> parser.parseBody(Map.of("name.like", "a")))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("name.like");
    }

    @Test
    public void rejectsKeyWithoutOperator() {
        assertThatThrownBy(() -> parser.parseBody(Map.of("name", "a")))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("name");
    }

    @Test
    public void rejectsScalarForInOperatorInBody() {
        assertThatThrownBy(() -> parser.parseBody(Map.of("name.in", "a")))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("name.in");
    }

    @Test
    public void rejectsNonBooleanForSpecified() {
        assertThatThrownBy(() -> parser.parseQueryParams(Map.of("name.specified", List.of("yes"))))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("name.specified");
    }

    @Test
    public void emptyOrNullFilterGivesEmptyList() {
        assertThat(parser.parseBody(null)).isEmpty();
        assertThat(parser.parseBody(Map.of())).isEmpty();
        assertThat(parser.parseQueryParams(Map.of("page", List.of("1")))).isEmpty();
    }
}
```

- [ ] **Step 2: Run to see it fail**

Run: `./gradlew test --tests '*FilterParserUnitTest*' -x runCategorizedTests`
Expected: compilation failure.

- [ ] **Step 3: Implement**

`FilterOperator.java`:

```java
package com.icthh.xm.ms.entity.service.search.db.filter;

import java.util.Arrays;
import java.util.Optional;

public enum FilterOperator {
    EQ("eq"), NOT_EQ("notEq"), IN("in"), NOT_IN("notIn"), CONTAINS("contains"),
    SPECIFIED("specified"), GT("gt"), GTE("gte"), LT("lt"), LTE("lte");

    private final String suffix;

    FilterOperator(String suffix) {
        this.suffix = suffix;
    }

    public String suffix() {
        return suffix;
    }

    public boolean isMultiValue() {
        return this == IN || this == NOT_IN;
    }

    public static Optional<FilterOperator> bySuffix(String suffix) {
        return Arrays.stream(values()).filter(op -> op.suffix.equals(suffix)).findFirst();
    }
}
```

`FilterCondition.java`:

```java
package com.icthh.xm.ms.entity.service.search.db.filter;

import java.util.List;

public record FilterCondition(String field, FilterOperator operator, List<Object> values) {

    public static final String DATA_PREFIX = "data.";

    public boolean isDataField() {
        return field.startsWith(DATA_PREFIX);
    }

    /** {@code data.a.b} → {@code $.a.b} (SQL/JSON path used by {@code json_query}). */
    public String dataJsonPath() {
        return "$." + field.substring(DATA_PREFIX.length());
    }

    public Object singleValue() {
        return values.isEmpty() ? null : values.get(0);
    }
}
```

`FilterParser.java`:

```java
package com.icthh.xm.ms.entity.service.search.db.filter;

import static com.icthh.xm.commons.exceptions.ErrorConstants.ERR_VALIDATION;

import com.icthh.xm.commons.exceptions.BusinessException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class FilterParser {

    public static final Set<String> RESERVED_PARAMS = Set.of("page", "size", "sort", "typeKey", "query", "includeSubTypes");
    private static final Pattern LONG = Pattern.compile("-?\\d+");
    private static final Pattern DOUBLE = Pattern.compile("-?\\d+\\.\\d+");

    public List<FilterCondition> parseBody(Map<String, Object> filter) {
        List<FilterCondition> result = new ArrayList<>();
        if (filter == null) {
            return result;
        }
        filter.forEach((key, value) -> {
            ParsedKey parsed = parseKey(key);
            List<Object> values;
            if (parsed.operator.isMultiValue()) {
                if (!(value instanceof Collection<?> collection)) {
                    throw new BusinessException(ERR_VALIDATION, "Filter operator " + parsed.operator.suffix()
                        + " requires a list value: " + key);
                }
                values = new ArrayList<>(collection);
            } else {
                values = List.of(requireScalar(key, parsed.operator, value));
            }
            result.add(new FilterCondition(parsed.field, parsed.operator, values));
        });
        return result;
    }

    public List<FilterCondition> parseQueryParams(Map<String, List<String>> queryParams) {
        List<FilterCondition> result = new ArrayList<>();
        if (queryParams == null) {
            return result;
        }
        queryParams.forEach((key, rawValues) -> {
            if (RESERVED_PARAMS.contains(key) || rawValues == null || rawValues.isEmpty()) {
                return;
            }
            ParsedKey parsed = parseKey(key);
            String raw = rawValues.get(0);
            List<Object> values = new ArrayList<>();
            if (parsed.operator.isMultiValue()) {
                for (String part : raw.split(",")) {
                    values.add(inferType(part.trim()));
                }
            } else {
                values.add(requireScalar(key, parsed.operator, inferType(raw)));
            }
            result.add(new FilterCondition(parsed.field, parsed.operator, values));
        });
        return result;
    }

    private static Object requireScalar(String key, FilterOperator operator, Object value) {
        if (value instanceof Collection<?>) {
            throw new BusinessException(ERR_VALIDATION, "Filter operator " + operator.suffix()
                + " requires a scalar value: " + key);
        }
        if (operator == FilterOperator.SPECIFIED && !(value instanceof Boolean)) {
            throw new BusinessException(ERR_VALIDATION, "Filter operator specified requires true or false: " + key);
        }
        return value;
    }

    private static Object inferType(String raw) {
        if (LONG.matcher(raw).matches()) {
            return Long.valueOf(raw);
        }
        if (DOUBLE.matcher(raw).matches()) {
            return Double.valueOf(raw);
        }
        if ("true".equals(raw) || "false".equals(raw)) {
            return Boolean.valueOf(raw);
        }
        return raw;
    }

    private static ParsedKey parseKey(String key) {
        int dot = key.lastIndexOf('.');
        if (dot <= 0 || dot == key.length() - 1) {
            throw new BusinessException(ERR_VALIDATION, "Filter key must be <field>.<operator>: " + key);
        }
        String field = key.substring(0, dot);
        FilterOperator operator = FilterOperator.bySuffix(key.substring(dot + 1))
            .orElseThrow(() -> new BusinessException(ERR_VALIDATION, "Unknown filter operator in key: " + key));
        return new ParsedKey(field, operator);
    }

    private record ParsedKey(String field, FilterOperator operator) {
    }
}
```

- [ ] **Step 4: Run the test**

Run: `./gradlew test --tests '*FilterParserUnitTest*' -x runCategorizedTests`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/icthh/xm/ms/entity/service/search/db/filter src/test/java/com/icthh/xm/ms/entity/service/search/db/filter
git commit -m "Add filter grammar parser for DB search"
```

---

### Task 4: JSON value dialect strategy and filter Specification builder

**Files:**
- Create: `src/main/java/com/icthh/xm/ms/entity/service/search/db/dialect/JsonValueStrategy.java`
- Create: `src/main/java/com/icthh/xm/ms/entity/service/search/db/dialect/PostgresJsonValueStrategy.java`
- Create: `src/main/java/com/icthh/xm/ms/entity/service/search/db/dialect/OracleJsonValueStrategy.java`
- Create: `src/main/java/com/icthh/xm/ms/entity/service/search/db/filter/XmEntityFilterSpecificationBuilder.java`
- Test: `src/test/java/com/icthh/xm/ms/entity/service/search/db/filter/XmEntityFilterSpecificationBuilderIntTest.java`

**Interfaces:**
- Produces:
  ```java
  public interface JsonValueStrategy {
      Expression<String> jsonValue(CriteriaBuilder cb, Path<?> dataColumn, String jsonPath); // comparable json value
      Expression<?> literal(CriteriaBuilder cb, Object value);                              // literal matching jsonValue's type
      Expression<String> jsonText(CriteriaBuilder cb, Path<?> dataColumn, String jsonPath); // text form for contains
  }
  // Beans: PostgresJsonValueStrategy (@ConditionalOnExpression url startsWith jdbc:postgresql:),
  //        OracleJsonValueStrategy   (@ConditionalOnExpression url startsWith jdbc:oracle:)

  @Component public class XmEntityFilterSpecificationBuilder {
      public static final Set<String> COLUMN_FIELDS = Set.of("id","key","typeKey","stateKey","name","description",
          "startDate","updateDate","endDate","createdBy","updatedBy","removed");
      /** entityPath resolves the XmEntity path from the query root (identity for XmEntity, root.get("target") for Link). */
      public <T> Specification<T> build(List<FilterCondition> conditions, Function<Root<T>, Path<XmEntity>> entityPath);
      public Specification<XmEntity> build(List<FilterCondition> conditions);   // entityPath = root -> root
      public <T> Specification<T> typeKey(String typeKey, boolean includeSubTypes, Function<Root<T>, Path<XmEntity>> entityPath);
      public <T> Specification<T> notRemoved(Function<Root<T>, Path<XmEntity>> entityPath); // removed is null or false
      public <T> Specification<T> fullText(String query, Function<Root<T>, Path<XmEntity>> entityPath); // ilike on searchText
      public static boolean hasRemovedCondition(List<FilterCondition> conditions);
  }
  ```
- Consumes: `FilterCondition`, `FilterOperator` (Task 3); HQL function names from `com.icthh.xm.commons.migration.db.jsonb.CustomDialect.JSON_QUERY` (`json_query`), `CustomPostgreSQLDialect.TO_JSON_B` (`to_json_b`), `CustomPostgreSQLDialect.TO_JSON_B_TEXT` (`to_json_b_text`).
- `fullText` depends on `XmEntity.searchText` from Task 6. In this task implement `fullText` but test it in Task 6.

- [ ] **Step 1: Write the failing integration test** (Postgres)

```java
package com.icthh.xm.ms.entity.service.search.db.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.ms.entity.AbstractPostgresIntTest;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class XmEntityFilterSpecificationBuilderIntTest extends AbstractPostgresIntTest {

    private static final String TYPE = "FILTER_TEST";

    @Autowired
    private XmEntityFilterSpecificationBuilder builder;
    @Autowired
    private FilterParser parser;
    @Autowired
    private XmEntityRepository repository;

    private XmEntity e1;
    private XmEntity e2;
    private XmEntity e3;

    @BeforeEach
    public void seed() {
        e1 = repository.save(newEntity(TYPE, "Alpha order", Map.of("orderNo", 1, "sub", Map.of("position", 5), "city", "Kyiv")));
        e2 = repository.save(newEntity(TYPE, "Beta order", Map.of("orderNo", 2, "sub", Map.of("position", 7), "city", "Lviv")));
        e3 = repository.save(newEntity(TYPE, "Gamma", Map.of("orderNo", 10)));
        e3.setRemoved(true);
        e3.setStateKey("CLOSED");
        e3.setStartDate(Instant.parse("2020-01-01T00:00:00Z"));
        repository.save(e3);
    }

    private List<Long> ids(Specification<XmEntity> spec) {
        return repository.findAll(Specification.where(builder.typeKey(TYPE, false, root -> root)).and(spec))
            .stream().map(XmEntity::getId).toList();
    }

    private Specification<XmEntity> filter(Map<String, Object> body) {
        return builder.build(parser.parseBody(body));
    }

    @Test
    public void dataNumericInAndEq() {
        assertThat(ids(filter(Map.of("data.orderNo.in", List.of(1, 2))))).containsExactlyInAnyOrder(e1.getId(), e2.getId());
        assertThat(ids(filter(Map.of("data.sub.position.eq", 7)))).containsExactly(e2.getId());
        assertThat(ids(filter(Map.of("data.sub.position.notEq", 7)))).containsExactly(e1.getId()); // e3 has no position → null → excluded
    }

    @Test
    public void dataNumericRangeComparesAsNumbers() {
        // textual comparison would put "10" before "2"; numeric jsonb comparison must not
        assertThat(ids(filter(Map.of("data.orderNo.gt", 2)))).containsExactly(e3.getId());
        assertThat(ids(filter(Map.of("data.orderNo.lte", 2)))).containsExactlyInAnyOrder(e1.getId(), e2.getId());
    }

    @Test
    public void dataStringEqAndContains() {
        assertThat(ids(filter(Map.of("data.city.eq", "Kyiv")))).containsExactly(e1.getId());
        assertThat(ids(filter(Map.of("data.city.contains", "YI")))).containsExactly(e1.getId());
        assertThat(ids(filter(Map.of("data.city.notIn", List.of("Kyiv"))))).containsExactly(e2.getId());
    }

    @Test
    public void dataSpecified() {
        assertThat(ids(filter(Map.of("data.sub.position.specified", false)))).containsExactly(e3.getId());
        assertThat(ids(filter(Map.of("data.city.specified", true)))).containsExactlyInAnyOrder(e1.getId(), e2.getId());
    }

    @Test
    public void columnFieldsAreTyped() {
        assertThat(ids(filter(Map.of("name.contains", "order")))).containsExactlyInAnyOrder(e1.getId(), e2.getId());
        assertThat(ids(filter(Map.of("stateKey.eq", "CLOSED")))).containsExactly(e3.getId());
        assertThat(ids(filter(Map.of("startDate.lt", "2021-01-01T00:00:00Z")))).containsExactly(e3.getId());
        assertThat(ids(filter(Map.of("id.in", List.of(e1.getId().toString()))))).containsExactly(e1.getId());
        assertThat(ids(filter(Map.of("removed.eq", true)))).containsExactly(e3.getId());
    }

    @Test
    public void typeKeyWithAndWithoutSubTypes() {
        XmEntity sub = repository.save(newEntity(TYPE + ".SUB", "Sub", Map.of()));
        XmEntity lookalike = repository.save(newEntity(TYPE + "X", "Lookalike", Map.of()));

        List<Long> withSub = repository.findAll(builder.typeKey(TYPE, true, root -> root)).stream().map(XmEntity::getId).toList();
        List<Long> exact = repository.findAll(builder.typeKey(TYPE, false, root -> root)).stream().map(XmEntity::getId).toList();

        assertThat(withSub).contains(e1.getId(), sub.getId()).doesNotContain(lookalike.getId());
        assertThat(exact).contains(e1.getId()).doesNotContain(sub.getId(), lookalike.getId());
    }

    @Test
    public void notRemovedExcludesSoftDeleted() {
        assertThat(ids(builder.notRemoved(root -> root))).containsExactlyInAnyOrder(e1.getId(), e2.getId());
        assertThat(XmEntityFilterSpecificationBuilder.hasRemovedCondition(parser.parseBody(Map.of("removed.eq", true)))).isTrue();
        assertThat(XmEntityFilterSpecificationBuilder.hasRemovedCondition(parser.parseBody(Map.of("name.eq", "x")))).isFalse();
    }

    @Test
    public void rejectsUnknownColumnAndBadValue() {
        assertThatThrownBy(() -> ids(filter(Map.of("avatarUrlRelative.eq", "x"))))
            .isInstanceOf(BusinessException.class).hasMessageContaining("avatarUrlRelative");
        assertThatThrownBy(() -> ids(filter(Map.of("startDate.eq", "not-a-date"))))
            .isInstanceOf(BusinessException.class).hasMessageContaining("startDate");
        assertThatThrownBy(() -> ids(filter(Map.of("id.eq", "abc"))))
            .isInstanceOf(BusinessException.class).hasMessageContaining("id");
    }
}
```

- [ ] **Step 2: Run to see it fail**

Run: `./gradlew test --tests '*XmEntityFilterSpecificationBuilderIntTest*' -x runCategorizedTests`
Expected: compilation failure.

- [ ] **Step 3: Implement the dialect strategies**

`JsonValueStrategy.java`:

```java
package com.icthh.xm.ms.entity.service.search.db.dialect;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;

/** DB-specific pieces of jsonb filtering. One bean is active, selected by the datasource URL. */
public interface JsonValueStrategy {

    /** Expression of the JSON value at {@code jsonPath} ({@code $.a.b}), comparable with {@link #literal}. */
    Expression<String> jsonValue(CriteriaBuilder cb, Path<?> dataColumn, String jsonPath);

    /** Literal in the same representation as {@link #jsonValue} so {@code =}, {@code in}, {@code >} work. */
    Expression<?> literal(CriteriaBuilder cb, Object value);

    /** Text form of the JSON value for case-insensitive {@code contains}. */
    Expression<String> jsonText(CriteriaBuilder cb, Path<?> dataColumn, String jsonPath);
}
```

`PostgresJsonValueStrategy.java`:

```java
package com.icthh.xm.ms.entity.service.search.db.dialect;

import static com.icthh.xm.commons.migration.db.jsonb.CustomDialect.JSON_QUERY;
import static com.icthh.xm.commons.migration.db.jsonb.CustomPostgreSQLDialect.TO_JSON_B;
import static com.icthh.xm.commons.migration.db.jsonb.CustomPostgreSQLDialect.TO_JSON_B_TEXT;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

/**
 * Postgres: {@code json_query(data, '$.path')} renders {@code jsonb_path_query_first(...)} and returns jsonb, so
 * comparisons are jsonb-to-jsonb (numbers compare numerically). Literals are wrapped with {@code to_jsonb}.
 */
@Component
@ConditionalOnExpression("'${spring.datasource.url}'.startsWith('jdbc:postgresql:')")
public class PostgresJsonValueStrategy implements JsonValueStrategy {

    @Override
    public Expression<String> jsonValue(CriteriaBuilder cb, Path<?> dataColumn, String jsonPath) {
        return cb.function(JSON_QUERY, String.class, dataColumn, cb.literal(jsonPath));
    }

    @Override
    public Expression<?> literal(CriteriaBuilder cb, Object value) {
        // to_jsonb(?::text) for strings, to_jsonb(?) for numbers and booleans (JDBC binds them typed)
        String function = value instanceof String ? TO_JSON_B_TEXT : TO_JSON_B;
        return cb.function(function, String.class, cb.literal(value));
    }

    @Override
    public Expression<String> jsonText(CriteriaBuilder cb, Path<?> dataColumn, String jsonPath) {
        return jsonValue(cb, dataColumn, jsonPath).as(String.class);
    }
}
```

`OracleJsonValueStrategy.java`:

```java
package com.icthh.xm.ms.entity.service.search.db.dialect;

import static com.icthh.xm.commons.migration.db.jsonb.CustomDialect.JSON_QUERY;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

/**
 * Oracle: {@code json_query(data, '$.path')} renders {@code json_value(...)} which returns text.
 * Limitation: numeric range operators compare textually.
 */
@Component
@ConditionalOnExpression("'${spring.datasource.url}'.startsWith('jdbc:oracle:')")
public class OracleJsonValueStrategy implements JsonValueStrategy {

    @Override
    public Expression<String> jsonValue(CriteriaBuilder cb, Path<?> dataColumn, String jsonPath) {
        return cb.function(JSON_QUERY, String.class, dataColumn, cb.literal(jsonPath));
    }

    @Override
    public Expression<?> literal(CriteriaBuilder cb, Object value) {
        return cb.literal(String.valueOf(value));
    }

    @Override
    public Expression<String> jsonText(CriteriaBuilder cb, Path<?> dataColumn, String jsonPath) {
        return jsonValue(cb, dataColumn, jsonPath);
    }
}
```

- [ ] **Step 4: Implement the Specification builder**

```java
package com.icthh.xm.ms.entity.service.search.db.filter;

import static com.icthh.xm.commons.exceptions.ErrorConstants.ERR_VALIDATION;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.XmEntity_;
import com.icthh.xm.ms.entity.service.search.db.dialect.JsonValueStrategy;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class XmEntityFilterSpecificationBuilder {

    public static final Set<String> COLUMN_FIELDS = Set.of(
        XmEntity_.ID, XmEntity_.KEY, XmEntity_.TYPE_KEY, XmEntity_.STATE_KEY, XmEntity_.NAME, XmEntity_.DESCRIPTION,
        XmEntity_.START_DATE, XmEntity_.UPDATE_DATE, XmEntity_.END_DATE, XmEntity_.CREATED_BY, XmEntity_.UPDATED_BY,
        XmEntity_.REMOVED);
    private static final char ESCAPE = '\\';

    private final JsonValueStrategy jsonValueStrategy;

    public Specification<XmEntity> build(List<FilterCondition> conditions) {
        return build(conditions, root -> root);
    }

    public <T> Specification<T> build(List<FilterCondition> conditions, Function<Root<T>, Path<XmEntity>> entityPath) {
        return (root, query, cb) -> {
            Path<XmEntity> entity = entityPath.apply(root);
            List<Predicate> predicates = new ArrayList<>();
            for (FilterCondition condition : conditions) {
                predicates.add(condition.isDataField()
                    ? dataPredicate(cb, entity, condition)
                    : columnPredicate(cb, entity, condition));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    public <T> Specification<T> typeKey(String typeKey, boolean includeSubTypes, Function<Root<T>, Path<XmEntity>> entityPath) {
        return (root, query, cb) -> {
            Path<String> path = entityPath.apply(root).get(XmEntity_.typeKey);
            Predicate exact = cb.equal(path, typeKey);
            return includeSubTypes ? cb.or(exact, cb.like(path, typeKey + ".%")) : exact;
        };
    }

    public <T> Specification<T> notRemoved(Function<Root<T>, Path<XmEntity>> entityPath) {
        return (root, query, cb) -> {
            Path<Boolean> removed = entityPath.apply(root).get(XmEntity_.removed);
            return cb.or(cb.isNull(removed), cb.isFalse(removed));
        };
    }

    public <T> Specification<T> fullText(String query, Function<Root<T>, Path<XmEntity>> entityPath) {
        return (root, cq, cb) -> ((HibernateCriteriaBuilder) cb).ilike(
            entityPath.apply(root).get(XmEntity_.searchText), "%" + escapeLike(query) + "%", ESCAPE);
    }

    public static boolean hasRemovedCondition(List<FilterCondition> conditions) {
        return conditions.stream().anyMatch(c -> XmEntity_.REMOVED.equals(c.field()));
    }

    static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    // ---- data.* ----

    private Predicate dataPredicate(CriteriaBuilder cb, Path<XmEntity> entity, FilterCondition c) {
        Path<?> data = entity.get(XmEntity_.data);
        String jsonPath = c.dataJsonPath();
        Expression<String> value = jsonValueStrategy.jsonValue(cb, data, jsonPath);
        return switch (c.operator()) {
            case EQ -> cb.equal(value, jsonValueStrategy.literal(cb, c.singleValue()));
            case NOT_EQ -> cb.notEqual(value, jsonValueStrategy.literal(cb, c.singleValue()));
            case IN -> value.in(c.values().stream().map(v -> jsonValueStrategy.literal(cb, v)).toList());
            case NOT_IN -> cb.not(value.in(c.values().stream().map(v -> jsonValueStrategy.literal(cb, v)).toList()));
            case CONTAINS -> ((HibernateCriteriaBuilder) cb).ilike(jsonValueStrategy.jsonText(cb, data, jsonPath),
                "%" + escapeLike(String.valueOf(c.singleValue())) + "%", ESCAPE);
            case SPECIFIED -> Boolean.TRUE.equals(c.singleValue()) ? cb.isNotNull(value) : cb.isNull(value);
            case GT -> cb.greaterThan(value, literalString(cb, c.singleValue()));
            case GTE -> cb.greaterThanOrEqualTo(value, literalString(cb, c.singleValue()));
            case LT -> cb.lessThan(value, literalString(cb, c.singleValue()));
            case LTE -> cb.lessThanOrEqualTo(value, literalString(cb, c.singleValue()));
        };
    }

    @SuppressWarnings("unchecked")
    private Expression<String> literalString(CriteriaBuilder cb, Object value) {
        // declared type is String because jsonValue is declared String; the DB compares jsonb (Postgres) or text (Oracle)
        return (Expression<String>) jsonValueStrategy.literal(cb, value);
    }

    // ---- columns ----

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Predicate columnPredicate(CriteriaBuilder cb, Path<XmEntity> entity, FilterCondition c) {
        if (!COLUMN_FIELDS.contains(c.field())) {
            throw new BusinessException(ERR_VALIDATION, "Unknown filter field: " + c.field());
        }
        Path path = entity.get(c.field());
        Class<?> javaType = path.getJavaType();
        List<Object> values = c.values().stream().map(v -> convert(c.field(), javaType, v)).toList();
        Object value = values.isEmpty() ? null : values.get(0);
        return switch (c.operator()) {
            case EQ -> cb.equal(path, value);
            case NOT_EQ -> cb.notEqual(path, value);
            case IN -> path.in(values);
            case NOT_IN -> cb.not(path.in(values));
            case CONTAINS -> ((HibernateCriteriaBuilder) cb).ilike(path.as(String.class),
                "%" + escapeLike(String.valueOf(c.singleValue())) + "%", ESCAPE);
            case SPECIFIED -> Boolean.TRUE.equals(c.singleValue()) ? cb.isNotNull(path) : cb.isNull(path);
            case GT -> cb.greaterThan(path, (Comparable) value);
            case GTE -> cb.greaterThanOrEqualTo(path, (Comparable) value);
            case LT -> cb.lessThan(path, (Comparable) value);
            case LTE -> cb.lessThanOrEqualTo(path, (Comparable) value);
        };
    }

    static Object convert(String field, Class<?> javaType, Object value) {
        try {
            if (value == null || javaType.isInstance(value)) {
                return value;
            }
            String s = String.valueOf(value);
            if (javaType == Long.class) {
                return Long.valueOf(s);
            }
            if (javaType == Integer.class) {
                return Integer.valueOf(s);
            }
            if (javaType == Boolean.class) {
                return Boolean.valueOf(s);
            }
            if (javaType == Instant.class) {
                return Instant.parse(s);
            }
            return s;
        } catch (NumberFormatException | DateTimeParseException e) {
            throw new BusinessException(ERR_VALIDATION, "Invalid value for filter field " + field + ": " + value);
        }
    }
}
```

Note `XmEntity_.searchText` does not exist until Task 6. To keep this task compiling, add the `searchText` field to `XmEntity` now (Task 6 Step 3 shows the exact field) and the Liquibase column (Task 6 Step 4); otherwise Hibernate schema validation fails against Postgres. Do both here if Task 6 has not been executed yet, and mark those Task 6 steps done.

- [ ] **Step 5: Run the test**

Run: `./gradlew test --tests '*XmEntityFilterSpecificationBuilderIntTest*' -x runCategorizedTests`
Expected: PASS. If `dataNumericRangeComparesAsNumbers` fails with a Postgres operator error `jsonb > jsonb`, check the rendered SQL in `show-sql` output: both sides must be jsonb (`jsonb_path_query_first(...)` and `to_jsonb(?)`).

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/icthh/xm/ms/entity/service/search/db src/test/java/com/icthh/xm/ms/entity/service/search/db
git commit -m "Add jsonb-aware filter Specification builder with Postgres and Oracle strategies"
```

---

### Task 5: Sort translation

**Files:**
- Create: `src/main/java/com/icthh/xm/ms/entity/service/search/db/filter/SortTranslator.java`
- Test: `src/test/java/com/icthh/xm/ms/entity/service/search/db/filter/SortTranslatorIntTest.java`

**Interfaces:**
- Produces:
  ```java
  @Component public class SortTranslator {
      public <T> OrderProvider<T> toOrderProvider(Sort sort, Function<Root<T>, Path<XmEntity>> entityPath);
      public OrderProvider<XmEntity> toOrderProvider(Sort sort);   // entityPath = root -> root
      public void validate(Sort sort);                              // throws BusinessException(ERR_VALIDATION) for bad property
  }
  ```
  Allowed properties: `XmEntityFilterSpecificationBuilder.COLUMN_FIELDS` and `data.<path>`. Unsorted → empty order list.
- Consumes: `OrderProvider` (Task 2), `JsonValueStrategy` (Task 4), `PermittedSpecificationRepository` (Task 2) in the test.

- [ ] **Step 1: Write the failing test**

```java
package com.icthh.xm.ms.entity.service.search.db.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.ms.entity.AbstractPostgresIntTest;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import com.icthh.xm.ms.entity.repository.search.db.PermittedSpecificationRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class SortTranslatorIntTest extends AbstractPostgresIntTest {

    private static final String TYPE = "SORT_TEST";

    @Autowired
    private SortTranslator sortTranslator;
    @Autowired
    private XmEntityFilterSpecificationBuilder builder;
    @Autowired
    private PermittedSpecificationRepository repository;
    @Autowired
    private XmEntityRepository xmEntityRepository;

    @BeforeEach
    public void seed() {
        xmEntityRepository.save(newEntity(TYPE, "b", Map.of("orderNo", 10)));
        xmEntityRepository.save(newEntity(TYPE, "a", Map.of("orderNo", 2)));
        xmEntityRepository.save(newEntity(TYPE, "c", Map.of("orderNo", 1)));
    }

    private List<String> namesSortedBy(Sort sort) {
        return repository.findAll(XmEntity.class, builder.typeKey(TYPE, false, root -> root),
                sortTranslator.toOrderProvider(sort), PageRequest.of(0, 10), null)
            .getContent().stream().map(XmEntity::getName).toList();
    }

    @Test
    public void sortsByColumn() {
        assertThat(namesSortedBy(Sort.by(Sort.Direction.ASC, "name"))).containsExactly("a", "b", "c");
        assertThat(namesSortedBy(Sort.by(Sort.Direction.DESC, "name"))).containsExactly("c", "b", "a");
    }

    @Test
    public void sortsByDataPathNumerically() {
        assertThat(namesSortedBy(Sort.by(Sort.Direction.ASC, "data.orderNo"))).containsExactly("c", "a", "b");
        assertThat(namesSortedBy(Sort.by(Sort.Direction.DESC, "data.orderNo"))).containsExactly("b", "a", "c");
    }

    @Test
    public void unsortedGivesNoOrder() {
        assertThat(namesSortedBy(Sort.unsorted())).hasSize(3);
    }

    @Test
    public void rejectsUnknownProperty() {
        assertThatThrownBy(() -> sortTranslator.validate(Sort.by("avatarUrlRelative")))
            .isInstanceOf(BusinessException.class).hasMessageContaining("avatarUrlRelative");
        assertThatThrownBy(() -> sortTranslator.validate(Sort.by("1=1; drop table xm_entity")))
            .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> namesSortedBy(Sort.by("data")))
            .isInstanceOf(BusinessException.class);
    }
}
```

- [ ] **Step 2: Run to see it fail**

Run: `./gradlew test --tests '*SortTranslatorIntTest*' -x runCategorizedTests`
Expected: compilation failure.

- [ ] **Step 3: Implement**

```java
package com.icthh.xm.ms.entity.service.search.db.filter;

import static com.icthh.xm.commons.exceptions.ErrorConstants.ERR_VALIDATION;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.XmEntity_;
import com.icthh.xm.ms.entity.repository.search.db.OrderProvider;
import com.icthh.xm.ms.entity.service.search.db.dialect.JsonValueStrategy;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SortTranslator {

    private static final Pattern DATA_PATH = Pattern.compile("^data(\\.[A-Za-z0-9_]+)+$");

    private final JsonValueStrategy jsonValueStrategy;

    public OrderProvider<XmEntity> toOrderProvider(Sort sort) {
        return toOrderProvider(sort, root -> root);
    }

    public <T> OrderProvider<T> toOrderProvider(Sort sort, Function<Root<T>, Path<XmEntity>> entityPath) {
        validate(sort);
        return (root, cb) -> {
            Path<XmEntity> entity = entityPath.apply(root);
            return sort.stream().map(order -> {
                Expression<?> expression = order.getProperty().startsWith(FilterCondition.DATA_PREFIX)
                    ? jsonValueStrategy.jsonValue(cb, entity.get(XmEntity_.data),
                        "$." + order.getProperty().substring(FilterCondition.DATA_PREFIX.length()))
                    : entity.get(order.getProperty());
                return order.isAscending() ? cb.asc(expression) : cb.desc(expression);
            }).toList();
        };
    }

    public void validate(Sort sort) {
        if (sort == null || sort.isUnsorted()) {
            return;
        }
        for (Sort.Order order : sort) {
            String property = order.getProperty();
            boolean column = XmEntityFilterSpecificationBuilder.COLUMN_FIELDS.contains(property);
            boolean dataPath = DATA_PATH.matcher(property).matches();
            if (!column && !dataPath) {
                throw new BusinessException(ERR_VALIDATION, "Unknown sort property: " + property);
            }
        }
    }
}
```

- [ ] **Step 4: Run the test**

Run: `./gradlew test --tests '*SortTranslatorIntTest*' -x runCategorizedTests`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/icthh/xm/ms/entity/service/search/db/filter/SortTranslator.java src/test/java/com/icthh/xm/ms/entity/service/search/db/filter/SortTranslatorIntTest.java
git commit -m "Add sort translation for columns and jsonb paths"
```

---

### Task 6: Search text: spec flags, column, listener, migration

**Files:**
- Modify: `src/main/java/com/icthh/xm/ms/entity/domain/spec/TypeSpec.java` (after `disablePersistentReferenceProcessingOnSave`)
- Create: `src/main/java/com/icthh/xm/ms/entity/service/search/db/SearchTextBuilder.java`
- Modify: `src/main/java/com/icthh/xm/ms/entity/domain/XmEntity.java` (field after `description`, `@EntityListeners`)
- Create: `src/main/java/com/icthh/xm/ms/entity/domain/listener/XmEntitySearchTextListener.java`
- Create: `src/main/resources/config/liquibase/changelog/20260909000000_add_search_text_to_xm_entity.xml`
- Modify: `src/main/resources/config/liquibase/master.xml`
- Modify: `src/test/resources/config/specs/xmentityspec-dbsearch.yml` (add the two `fullTextSearch*` keys to `ORDER`)
- Test: `src/test/java/com/icthh/xm/ms/entity/service/search/db/SearchTextBuilderUnitTest.java`
- Test: `src/test/java/com/icthh/xm/ms/entity/domain/listener/XmEntitySearchTextListenerIntTest.java`

**Interfaces:**
- Produces:
  ```java
  // TypeSpec
  @JsonProperty("fullTextSearch") private Boolean fullTextSearch;                       // default null (= off)
  @JsonProperty("fullTextSearchDataFields") private List<String> fullTextSearchDataFields; // "data.a.b" or "a.b"
  // XmEntity
  @JsonIgnore @Column(name = "search_text") private String searchText;  // getter/setter via Lombok like other fields
  @Component public class SearchTextBuilder { public String build(TypeSpec spec, XmEntity entity); } // null when off
  ```
- Consumes: `XmEntitySpecService.getTypeSpecByKeyWithoutFunctionFilter(String)`.

- [ ] **Step 1: Write the failing unit test for the builder**

```java
package com.icthh.xm.ms.entity.service.search.db;

import static org.assertj.core.api.Assertions.assertThat;

import com.icthh.xm.ms.entity.AbstractJupiterUnitTest;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class SearchTextBuilderUnitTest extends AbstractJupiterUnitTest {

    private final SearchTextBuilder builder = new SearchTextBuilder();

    private static XmEntity entity(Map<String, Object> data) {
        return new XmEntity().name("Alpha order").description("Big <b>one</b>").data(new HashMap<>(data));
    }

    @Test
    public void returnsNullWhenFlagOffOrSpecMissing() {
        assertThat(builder.build(null, entity(Map.of()))).isNull();
        assertThat(builder.build(TypeSpec.builder().key("T").build(), entity(Map.of()))).isNull();
        assertThat(builder.build(TypeSpec.builder().key("T").fullTextSearch(false).build(), entity(Map.of()))).isNull();
    }

    @Test
    public void joinsNameDescriptionAndConfiguredDataFields() {
        TypeSpec spec = TypeSpec.builder().key("T").fullTextSearch(true)
            .fullTextSearchDataFields(List.of("data.orderNo", "customer.city", "data.tags", "data.customer", "data.missing"))
            .build();
        XmEntity e = entity(Map.of(
            "orderNo", 42,
            "customer", Map.of("city", "Kyiv"),
            "tags", List.of("vip", "urgent")));

        String text = builder.build(spec, e);

        assertThat(text).isEqualTo("Alpha order\nBig <b>one</b>\n42\nKyiv\nvip urgent");
    }

    @Test
    public void nameAndDescriptionOnlyWhenNoDataFields() {
        TypeSpec spec = TypeSpec.builder().key("T").fullTextSearch(true).build();
        assertThat(builder.build(spec, entity(Map.of("x", 1)))).isEqualTo("Alpha order\nBig <b>one</b>");
        assertThat(builder.build(spec, new XmEntity().name("only"))).isEqualTo("only");
    }
}
```

- [ ] **Step 2: Add the TypeSpec fields and the builder**

`TypeSpec.java`, add after `disablePersistentReferenceProcessingOnSave`:

```java
    /** Enables DB full text search: name, description and {@link #fullTextSearchDataFields} are stored in xm_entity.search_text. */
    @Builder.Default
    @JsonProperty("fullTextSearch")
    private Boolean fullTextSearch = null;

    /** Data paths included into search text, e.g. {@code data.order}, {@code data.customer.city}. Prefix {@code data.} is optional. */
    @Builder.Default
    @JsonProperty("fullTextSearchDataFields")
    private List<String> fullTextSearchDataFields = null;
```

Also add `"fullTextSearch", "fullTextSearchDataFields"` to the `@JsonPropertyOrder` on `TypeSpec` if it lists properties explicitly (check the annotation at the class head; if it is absent, skip).

`SearchTextBuilder.java`:

```java
package com.icthh.xm.ms.entity.service.search.db;

import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/** Builds the denormalized text stored in xm_entity.search_text. Deterministic: name, description, configured data fields. */
@Slf4j
@Component
public class SearchTextBuilder {

    private static final String DATA_PREFIX = "data.";
    private static final String SEPARATOR = "\n";

    public String build(TypeSpec spec, XmEntity entity) {
        if (spec == null || !Boolean.TRUE.equals(spec.getFullTextSearch())) {
            return null;
        }
        List<String> parts = new ArrayList<>();
        addIfNotBlank(parts, entity.getName());
        addIfNotBlank(parts, entity.getDescription());
        List<String> fields = spec.getFullTextSearchDataFields() == null ? List.of() : spec.getFullTextSearchDataFields();
        for (String field : fields) {
            String path = field.startsWith(DATA_PREFIX) ? field.substring(DATA_PREFIX.length()) : field;
            addIfNotBlank(parts, scalarText(resolve(entity.getData(), path), field));
        }
        return String.join(SEPARATOR, parts);
    }

    private static Object resolve(Map<String, Object> data, String path) {
        Object current = data;
        for (String segment : path.split("\\.")) {
            if (!(current instanceof Map<?, ?> map)) {
                return null;
            }
            current = map.get(segment);
        }
        return current;
    }

    private static String scalarText(Object value, String field) {
        if (value == null) {
            return null;
        }
        if (value instanceof Map<?, ?>) {
            log.debug("fullTextSearchDataFields entry {} points to an object, skipped", field);
            return null;
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream()
                .filter(v -> v != null && !(v instanceof Map<?, ?>) && !(v instanceof Collection<?>))
                .map(String::valueOf)
                .collect(Collectors.joining(" "));
        }
        return String.valueOf(value);
    }

    private static void addIfNotBlank(List<String> parts, String value) {
        if (StringUtils.isNotBlank(value)) {
            parts.add(value);
        }
    }
}
```

Run: `./gradlew test --tests '*SearchTextBuilderUnitTest*' -x runCategorizedTests` → PASS.

- [ ] **Step 3: Add the entity field**

`XmEntity.java`, after the `description` field:

```java
    /**
     * Denormalized text for DB full text search (name, description, configured data fields).
     * Maintained by {@link com.icthh.xm.ms.entity.domain.listener.XmEntitySearchTextListener}; null when the type has
     * no {@code fullTextSearch: true}.
     */
    @JsonIgnore
    @Column(name = "search_text")
    private String searchText;
```

Register the listener: `@EntityListeners({AvatarUrlListener.class, XmEntityElasticSearchListener.class, XmEntitySearchTextListener.class})`.

Check that `XmEntity` exposes fluent setters via Lombok (`@Accessors(chain = true)` or explicit methods like `name(String)`). If fields have explicit fluent methods (see `typeKey(...)`, `name(...)` used in tests), add:

```java
    public XmEntity searchText(String searchText) {
        this.searchText = searchText;
        return this;
    }
```

Make sure `XmEntityDto` and `XmEntityMapper` do not pick up `searchText` (MapStruct only maps fields present on the DTO; do not add it to the DTO).

- [ ] **Step 4: Liquibase**

`src/main/resources/config/liquibase/changelog/20260909000000_add_search_text_to_xm_entity.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:ext="http://www.liquibase.org/xml/ns/dbchangelog-ext"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-latest.xsd
        http://www.liquibase.org/xml/ns/dbchangelog-ext http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-ext.xsd">

    <property name="searchTextType" value="text" dbms="postgresql"/>
    <property name="searchTextType" value="varchar" dbms="h2"/>
    <property name="searchTextType" value="clob" dbms="oracle"/>

    <changeSet id="20260909000000-1" author="ssenko">
        <addColumn tableName="xm_entity">
            <column name="search_text" type="${searchTextType}">
                <constraints nullable="true"/>
            </column>
        </addColumn>
    </changeSet>

    <!-- pg_trgm may need superuser rights; do not fail the migration when it cannot be created -->
    <changeSet id="20260909000000-2" author="ssenko" dbms="postgresql" runInTransaction="false" failOnError="false">
        <sql>CREATE EXTENSION IF NOT EXISTS pg_trgm</sql>
    </changeSet>

    <changeSet id="20260909000000-3" author="ssenko" dbms="postgresql" runInTransaction="false">
        <preConditions onFail="MARK_RAN">
            <sqlCheck expectedResult="1">select count(*) from pg_extension where extname = 'pg_trgm'</sqlCheck>
        </preConditions>
        <sql>CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_xm_entity_search_text_trgm
             ON xm_entity USING gin (search_text gin_trgm_ops) WHERE search_text IS NOT NULL</sql>
    </changeSet>

</databaseChangeLog>
```

`master.xml`: add after the `20260902000000_add_type_key_to_comment.xml` include:

```xml
    <include file="classpath:config/liquibase/changelog/20260909000000_add_search_text_to_xm_entity.xml" relativeToChangelogFile="false"/>
```

- [ ] **Step 5: Write the failing listener integration test**

```java
package com.icthh.xm.ms.entity.domain.listener;

import static org.assertj.core.api.Assertions.assertThat;

import com.icthh.xm.ms.entity.AbstractPostgresIntTest;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class XmEntitySearchTextListenerIntTest extends AbstractPostgresIntTest {

    @Autowired
    private XmEntityRepository repository;
    @Autowired
    private EntityManager em;

    @BeforeEach
    public void spec() {
        pushDbSearchSpec();
    }

    private String storedSearchText(Long id) {
        em.flush();
        em.clear();
        return (String) em.createNativeQuery("select search_text from xm_entity where id = :id")
            .setParameter("id", id).getSingleResult();
    }

    @Test
    public void fillsSearchTextOnPersistForEnabledType() {
        XmEntity order = repository.save(newEntity("ORDER", "Alpha", Map.of(
            "orderNo", 42, "customer", Map.of("city", "Kyiv"), "tags", List.of("vip"))));

        assertThat(storedSearchText(order.getId())).isEqualTo("Alpha\n42\nKyiv\nvip");
    }

    @Test
    public void leavesNullForDisabledType() {
        XmEntity silent = repository.save(newEntity("SILENT", "Quiet", Map.of("orderNo", 1)));
        assertThat(storedSearchText(silent.getId())).isNull();
    }

    @Test
    public void updatesSearchTextOnUpdate() {
        XmEntity order = repository.save(newEntity("ORDER", "Alpha", Map.of("orderNo", 1)));
        em.flush();

        order.setName("Beta");
        order.getData().put("orderNo", 2);
        repository.save(order);

        assertThat(storedSearchText(order.getId())).isEqualTo("Beta\n2");
    }

    @Test
    public void indexExistsWhenExtensionAvailable() {
        Number extensions = (Number) em.createNativeQuery("select count(*) from pg_extension where extname = 'pg_trgm'").getSingleResult();
        Number indexes = (Number) em.createNativeQuery("select count(*) from pg_indexes where indexname = 'idx_xm_entity_search_text_trgm'").getSingleResult();
        assertThat(indexes.intValue()).isEqualTo(extensions.intValue());
    }
}
```

- [ ] **Step 6: Update the fixture and implement the listener**

Add to `ORDER` in `src/test/resources/config/specs/xmentityspec-dbsearch.yml` (see Task 1 Step 3 for exact position):

```yaml
      fullTextSearch: true
      fullTextSearchDataFields:
          - data.orderNo
          - data.customer.city
          - data.tags
```

`XmEntitySearchTextListener.java`. Spring Boot registers `SpringBeanContainer` with Hibernate, so listener beans get constructor injection:

```java
package com.icthh.xm.ms.entity.domain.listener;

import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import com.icthh.xm.ms.entity.service.XmEntitySpecService;
import com.icthh.xm.ms.entity.service.search.db.SearchTextBuilder;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/** Keeps {@code xm_entity.search_text} in sync with name, description and the data fields configured in the type spec. */
@Slf4j
@Component
@RequiredArgsConstructor
public class XmEntitySearchTextListener {

    @Lazy
    private final XmEntitySpecService xmEntitySpecService;
    private final SearchTextBuilder searchTextBuilder;

    @PrePersist
    @PreUpdate
    void onPrePersistOrUpdate(XmEntity entity) {
        TypeSpec spec = xmEntitySpecService.getTypeSpecByKeyWithoutFunctionFilter(entity.getTypeKey()).orElse(null);
        entity.setSearchText(searchTextBuilder.build(spec, entity));
    }
}
```

If the context fails to start with "No default constructor for entity listener" (bean container not wired), switch to the static-setter pattern used by `XmEntityElasticSearchListener`: static fields + `@Autowired` setters + a public no-arg constructor.

- [ ] **Step 7: Run the tests**

Run: `./gradlew test --tests '*SearchTextBuilderUnitTest*' --tests '*XmEntitySearchTextListenerIntTest*' --tests '*PostgresInfrastructureIntTest*' -x runCategorizedTests`
Expected: PASS. `updatesSearchTextOnUpdate` depends on Hibernate dirty-checking picking up the `data` map mutation; if it fails, replace `order.getData().put(...)` with `order.setData(Map.of("orderNo", 2))`.

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/icthh/xm/ms/entity/domain src/main/java/com/icthh/xm/ms/entity/service/search/db/SearchTextBuilder.java \
        src/main/resources/config/liquibase src/test/resources/config/specs/xmentityspec-dbsearch.yml \
        src/test/java/com/icthh/xm/ms/entity/service/search/db/SearchTextBuilderUnitTest.java \
        src/test/java/com/icthh/xm/ms/entity/domain/listener/XmEntitySearchTextListenerIntTest.java
git commit -m "Add search_text column maintained from TypeSpec fullTextSearch settings"
```

---

### Task 7: Search endpoint: typeKey + full text + filters (GET and POST)

**Files:**
- Create: `src/main/java/com/icthh/xm/ms/entity/service/search/db/dto/XmEntityDbSearchRequest.java`
- Create: `src/main/java/com/icthh/xm/ms/entity/service/search/db/XmEntityDbSearchService.java`
- Create: `src/main/java/com/icthh/xm/ms/entity/lep/keyresolver/DbSearchRequestTypeKeyResolver.java`
- Create: `src/main/java/com/icthh/xm/ms/entity/lep/keyresolver/EntityTypeKeyAndLinkTypeKeyResolver.java`
- Create: `src/main/java/com/icthh/xm/ms/entity/lep/keyresolver/LinkTypeKeyParamResolver.java`
- Create: `src/main/java/com/icthh/xm/ms/entity/lep/keyresolver/JpqlTemplateKeyResolver.java`
- Create: `src/main/java/com/icthh/xm/ms/entity/web/rest/facade/XmEntityDbSearchFacade.java`
- Create: `src/main/java/com/icthh/xm/ms/entity/web/rest/XmEntityDbSearchResource.java`
- Modify: `src/main/java/com/icthh/xm/ms/entity/web/rest/util/PaginationUtil.java`
- Test: `src/test/java/com/icthh/xm/ms/entity/lep/keyresolver/DbSearchLepKeyResolversUnitTest.java`
- Test: `src/test/java/com/icthh/xm/ms/entity/web/rest/XmEntityDbSearchResourceIntTest.java`
- Test: `src/test/java/com/icthh/xm/ms/entity/web/rest/XmEntityDbSearchResourcePermissionIntTest.java`

**Interfaces:**
- Produces:
  ```java
  @Data public class XmEntityDbSearchRequest {
      private String typeKey; private Boolean includeSubTypes = true; private String query;
      private Map<String, Object> filter = new HashMap<>();
      public boolean includeSubTypes();   // null-safe, default true
  }

  @Service @LepService(group = "service.entity.dbsearch") public class XmEntityDbSearchService {
      @FindWithPermission("XMENTITY.SEARCH.DB") Page<XmEntity> search(XmEntityDbSearchRequest request, Pageable pageable, String privilegeKey);
  }
  public class XmEntityDbSearchFacade { Page<XmEntityDto> search(XmEntityDbSearchRequest request, Pageable pageable, String privilegeKey); }

  // PaginationUtil
  public static HttpHeaders generateDbSearchPaginationHttpHeaders(Map<String, ?> queryParams, Page page, String baseUrl);
  ```
- REST: `GET /api/_search-db/xm-entities`, `POST /api/_search-db/xm-entities`, privilege `XMENTITY.SEARCH.DB.QUERY`.
- Consumes: Tasks 2 to 6.

- [ ] **Step 1: Write the failing endpoint test**

```java
package com.icthh.xm.ms.entity.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.ms.entity.AbstractPostgresIntTest;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import com.icthh.xm.ms.entity.service.dto.XmEntityDto;
import com.icthh.xm.ms.entity.service.search.db.dto.XmEntityDbSearchRequest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Transactional
@WithMockUser(authorities = "SUPER-ADMIN")
public class XmEntityDbSearchResourceIntTest extends AbstractPostgresIntTest {

    @Autowired
    private XmEntityDbSearchResource resource;
    @Autowired
    private XmEntityRepository repository;

    private XmEntity kyiv;
    private XmEntity lviv;
    private XmEntity express;
    private XmEntity product;

    @BeforeEach
    public void seed() {
        pushDbSearchSpec();
        kyiv = repository.save(newEntity("ORDER", "Alpha", Map.of("orderNo", 1, "customer", Map.of("city", "Kyiv"), "position", 5)));
        lviv = repository.save(newEntity("ORDER", "Beta", Map.of("orderNo", 2, "customer", Map.of("city", "Lviv"), "position", 7)));
        express = repository.save(newEntity("ORDER.EXPRESS", "Gamma", Map.of("orderNo", 3)));
        product = repository.save(newEntity("PRODUCT", "Kyiv product", Map.of()));
        XmEntity removed = newEntity("ORDER", "Removed Kyiv", Map.of("orderNo", 9));
        removed.setRemoved(true);
        repository.save(removed);
    }

    private static List<Long> ids(ResponseEntity<List<XmEntityDto>> response) {
        return response.getBody().stream().map(XmEntityDto::getId).toList();
    }

    private static XmEntityDbSearchRequest request(String typeKey, String query, Map<String, Object> filter) {
        XmEntityDbSearchRequest r = new XmEntityDbSearchRequest();
        r.setTypeKey(typeKey);
        r.setQuery(query);
        r.setFilter(filter);
        return r;
    }

    @Test
    public void postSearchByTypeKeyIncludesSubTypesAndExcludesRemoved() {
        var response = resource.searchPost(request("ORDER", null, Map.of()), PageRequest.of(0, 10, Sort.by("name")));

        assertThat(ids(response)).containsExactly(kyiv.getId(), lviv.getId(), express.getId());
        assertThat(response.getHeaders().getFirst("X-Total-Count")).isEqualTo("3");
        assertThat(response.getHeaders().getFirst("Link")).contains("/api/_search-db/xm-entities").contains("typeKey=ORDER");
    }

    @Test
    public void postExactTypeKeyWhenIncludeSubTypesFalse() {
        XmEntityDbSearchRequest r = request("ORDER", null, Map.of());
        r.setIncludeSubTypes(false);
        assertThat(ids(resource.searchPost(r, PageRequest.of(0, 10)))).containsExactlyInAnyOrder(kyiv.getId(), lviv.getId());
    }

    @Test
    public void fullTextQueryMatchesNameAndDataFieldsCaseInsensitive() {
        assertThat(ids(resource.searchPost(request("ORDER", "kyi", Map.of()), PageRequest.of(0, 10)))).containsExactly(kyiv.getId());
        assertThat(ids(resource.searchPost(request("ORDER", "ALPHA", Map.of()), PageRequest.of(0, 10)))).containsExactly(kyiv.getId());
        // PRODUCT has fullTextSearch: false → search_text null → no match even though name contains Kyiv
        assertThat(ids(resource.searchPost(request("PRODUCT", "kyiv", Map.of()), PageRequest.of(0, 10)))).isEmpty();
    }

    @Test
    public void postFilterOnJsonbAndSortByJsonbDesc() {
        var response = resource.searchPost(
            request("ORDER", null, Map.of("data.orderNo.in", List.of(1, 2, 3), "data.position.gte", 5)),
            PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "data.position")));
        assertThat(ids(response)).containsExactly(lviv.getId(), kyiv.getId());
    }

    @Test
    public void removedIncludedWhenAskedExplicitly() {
        var response = resource.searchPost(request("ORDER", null, Map.of("removed.eq", true)), PageRequest.of(0, 10));
        assertThat(response.getBody()).extracting(XmEntityDto::getName).containsExactly("Removed Kyiv");
    }

    @Test
    public void getVariantBindsFlatQueryParams() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("typeKey", "ORDER");
        params.add("data.customer.city.eq", "Lviv");
        params.add("page", "0");
        params.add("size", "10");

        var response = resource.searchGet("ORDER", null, null, params, PageRequest.of(0, 10));

        assertThat(ids(response)).containsExactly(lviv.getId());
    }

    @Test
    public void rejectsBadFilterAndSort() {
        assertThatThrownBy(() -> resource.searchPost(request("ORDER", null, Map.of("name.like", "a")), PageRequest.of(0, 10)))
            .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> resource.searchPost(request("ORDER", null, Map.of()), PageRequest.of(0, 10, Sort.by("hacker"))))
            .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> resource.searchPost(request(null, null, Map.of()), PageRequest.of(0, 10)))
            .isInstanceOf(BusinessException.class).hasMessageContaining("typeKey");
    }
}
```

- [ ] **Step 2: Run to see it fail**

Run: `./gradlew test --tests '*XmEntityDbSearchResourceIntTest*' -x runCategorizedTests`
Expected: compilation failure.

- [ ] **Step 3: Implement request DTO and service**

`XmEntityDbSearchRequest.java`:

```java
package com.icthh.xm.ms.entity.service.search.db.dto;

import java.util.HashMap;
import java.util.Map;
import lombok.Data;

@Data
public class XmEntityDbSearchRequest {

    private String typeKey;
    private Boolean includeSubTypes = Boolean.TRUE;
    private String query;
    private Map<String, Object> filter = new HashMap<>();

    public boolean includeSubTypes() {
        return includeSubTypes == null || includeSubTypes;
    }
}
```

`XmEntityDbSearchService.java` (methods for Tasks 8 and 9 are added later in this same class):

```java
package com.icthh.xm.ms.entity.service.search.db;

import static com.icthh.xm.commons.exceptions.ErrorConstants.ERR_VALIDATION;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.commons.permission.annotation.FindWithPermission;
import com.icthh.xm.commons.permission.annotation.PrivilegeDescription;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.repository.search.db.PermittedSpecificationRepository;
import com.icthh.xm.ms.entity.service.search.db.dto.XmEntityDbSearchRequest;
import com.icthh.xm.ms.entity.service.search.db.filter.FilterCondition;
import com.icthh.xm.ms.entity.service.search.db.filter.SortTranslator;
import com.icthh.xm.ms.entity.service.search.db.filter.XmEntityFilterSpecificationBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import java.util.List;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@LepService(group = "service.entity.dbsearch")
@RequiredArgsConstructor
public class XmEntityDbSearchService {

    public static final String ROW_PRIVILEGE = "XMENTITY.SEARCH.DB";

    private final PermittedSpecificationRepository permittedSpecificationRepository;
    private final XmEntityFilterSpecificationBuilder specificationBuilder;
    private final SortTranslator sortTranslator;
    private final com.icthh.xm.ms.entity.service.search.db.filter.FilterParser filterParser;

    @Transactional(readOnly = true)
    @LogicExtensionPoint(value = "SearchDb", resolver = DbSearchRequestTypeKeyResolver.class)
    @FindWithPermission(ROW_PRIVILEGE)
    @PrivilegeDescription("Privilege to search xm entities in DB by typeKey, full text query and filters")
    public Page<XmEntity> search(XmEntityDbSearchRequest request, Pageable pageable, String privilegeKey) {
        if (StringUtils.isBlank(request.getTypeKey())) {
            throw new BusinessException(ERR_VALIDATION, "typeKey is required");
        }
        List<FilterCondition> conditions = filterParser.parseBody(request.getFilter());
        Specification<XmEntity> spec = buildSpecification(request.getTypeKey(), request.includeSubTypes(),
            request.getQuery(), conditions, root -> root);
        return permittedSpecificationRepository.findAll(XmEntity.class, spec,
            sortTranslator.toOrderProvider(pageable.getSort()), pageable, privilegeKey);
    }

    /** Shared by entity search, link-dialog search (Task 8) and link target search (Task 9). */
    <T> Specification<T> buildSpecification(String typeKey, boolean includeSubTypes, String query,
                                            List<FilterCondition> conditions,
                                            Function<Root<T>, Path<XmEntity>> entityPath) {
        Specification<T> spec = specificationBuilder.typeKey(typeKey, includeSubTypes, entityPath)
            .and(specificationBuilder.build(conditions, entityPath));
        if (!XmEntityFilterSpecificationBuilder.hasRemovedCondition(conditions)) {
            spec = spec.and(specificationBuilder.notRemoved(entityPath));
        }
        if (StringUtils.isNotBlank(query)) {
            spec = spec.and(specificationBuilder.fullText(query, entityPath));
        }
        return spec;
    }
}
```

Replace the fully qualified `FilterParser` field type with an import.

LEP key resolvers (all four created now, used by Tasks 7, 8, 9, 11, 12), package `com.icthh.xm.ms.entity.lep.keyresolver`, same shape as the existing `TypeKeyResolver`:

```java
@Component
public class DbSearchRequestTypeKeyResolver implements LepKeyResolver {
    @Override
    public List<String> segments(LepMethod method) {
        XmEntityDbSearchRequest request = method.getParameter("request", XmEntityDbSearchRequest.class);
        return request != null && request.getTypeKey() != null ? List.of(request.getTypeKey()) : List.of();
    }
}

@Component
public class EntityTypeKeyAndLinkTypeKeyResolver implements LepKeyResolver {
    @Override
    public List<String> segments(LepMethod method) {
        return List.of(method.getParameter("entityTypeKey", String.class), method.getParameter("linkTypeKey", String.class));
    }
}

@Component
public class LinkTypeKeyParamResolver implements LepKeyResolver {
    @Override
    public List<String> segments(LepMethod method) {
        return List.of(method.getParameter("linkTypeKey", String.class));
    }
}

@Component
public class JpqlTemplateKeyResolver implements LepKeyResolver {
    @Override
    public List<String> segments(LepMethod method) {
        return List.of(method.getParameter("template", JpqlTemplate.class).getKey());
    }
}
```

Imports per class: `com.icthh.xm.commons.lep.keyresolver.LepKeyResolver` and `com.icthh.xm.lep.api.LepMethod` (copy the exact imports from `TypeKeyResolver.java`), `java.util.List`, `org.springframework.stereotype.Component`. Parameter names must match the service method parameter names exactly (`request`, `entityTypeKey`, `linkTypeKey`, `template`); the project compiles with `-parameters` (check `build.gradle` for `options.compilerArgs << "-parameters"`; `TypeKeyResolver` relies on it too).

Unit test `src/test/java/com/icthh/xm/ms/entity/lep/keyresolver/DbSearchLepKeyResolversUnitTest.java`:

```java
package com.icthh.xm.ms.entity.lep.keyresolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.icthh.xm.lep.api.LepMethod;
import com.icthh.xm.ms.entity.AbstractJupiterUnitTest;
import com.icthh.xm.ms.entity.service.search.db.dto.XmEntityDbSearchRequest;
import com.icthh.xm.ms.entity.service.search.db.template.JpqlTemplate;
import org.junit.jupiter.api.Test;

public class DbSearchLepKeyResolversUnitTest extends AbstractJupiterUnitTest {

    @Test
    public void requestTypeKeyResolver() {
        LepMethod method = mock(LepMethod.class);
        XmEntityDbSearchRequest request = new XmEntityDbSearchRequest();
        request.setTypeKey("ORDER");
        when(method.getParameter("request", XmEntityDbSearchRequest.class)).thenReturn(request);
        assertThat(new DbSearchRequestTypeKeyResolver().segments(method)).containsExactly("ORDER");

        when(method.getParameter("request", XmEntityDbSearchRequest.class)).thenReturn(new XmEntityDbSearchRequest());
        assertThat(new DbSearchRequestTypeKeyResolver().segments(method)).isEmpty();
    }

    @Test
    public void entityAndLinkTypeKeyResolver() {
        LepMethod method = mock(LepMethod.class);
        when(method.getParameter("entityTypeKey", String.class)).thenReturn("ORDER");
        when(method.getParameter("linkTypeKey", String.class)).thenReturn("ORDER.ITEM");
        assertThat(new EntityTypeKeyAndLinkTypeKeyResolver().segments(method)).containsExactly("ORDER", "ORDER.ITEM");
        assertThat(new LinkTypeKeyParamResolver().segments(method)).containsExactly("ORDER.ITEM");
    }

    @Test
    public void templateKeyResolver() {
        LepMethod method = mock(LepMethod.class);
        JpqlTemplate template = new JpqlTemplate();
        template.setKey("MY_TPL");
        when(method.getParameter("template", JpqlTemplate.class)).thenReturn(template);
        assertThat(new JpqlTemplateKeyResolver().segments(method)).containsExactly("MY_TPL");
    }
}
```

`JpqlTemplate` is created in Task 10; if Task 10 has not run yet, create `JpqlTemplate` and `JpqlTemplateType` now exactly as specified in Task 10 Step 3 and mark that step done.

- [ ] **Step 4: Implement facade, pagination headers, resource**

`XmEntityDbSearchFacade.java`:

```java
package com.icthh.xm.ms.entity.web.rest.facade;

import com.icthh.xm.ms.entity.service.dto.XmEntityDto;
import com.icthh.xm.ms.entity.service.mapper.XmEntityMapper;
import com.icthh.xm.ms.entity.service.search.db.XmEntityDbSearchService;
import com.icthh.xm.ms.entity.service.search.db.dto.XmEntityDbSearchRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class XmEntityDbSearchFacade {

    private final XmEntityDbSearchService xmEntityDbSearchService;
    private final XmEntityMapper xmEntityMapper;

    public Page<XmEntityDto> search(XmEntityDbSearchRequest request, Pageable pageable, String privilegeKey) {
        return xmEntityDbSearchService.search(request, pageable, privilegeKey).map(xmEntityMapper::toDto);
    }
}
```

`PaginationUtil.java`, add:

```java
    /**
     * Pagination headers for DB search endpoints. Every entry of {@code queryParams} (typeKey, query, filters, sort)
     * is repeated in the Link URLs; list values are joined with a comma.
     */
    @SneakyThrows
    public static HttpHeaders generateDbSearchPaginationHttpHeaders(Map<String, ?> queryParams, Page page, String baseUrl) {
        StringBuilder queryString = new StringBuilder();
        for (Map.Entry<String, ?> entry : queryParams.entrySet()) {
            Object value = entry.getValue();
            if (value == null) {
                continue;
            }
            String text = value instanceof Collection<?> c ? StringUtils.join(c, ",") : String.valueOf(value);
            queryString.append('&').append(URLEncoder.encode(entry.getKey(), "UTF-8"))
                .append('=').append(URLEncoder.encode(text, "UTF-8"));
        }
        return generatePagination(queryString.toString(), page, baseUrl);
    }
```

Add `import java.util.Collection; import java.util.Map;` if missing.

`XmEntityDbSearchResource.java`:

```java
package com.icthh.xm.ms.entity.web.rest;

import com.icthh.xm.commons.permission.annotation.PrivilegeDescription;
import com.icthh.xm.ms.entity.service.dto.XmEntityDto;
import com.icthh.xm.ms.entity.service.search.db.dto.XmEntityDbSearchRequest;
import com.icthh.xm.ms.entity.service.search.db.filter.FilterParser;
import com.icthh.xm.ms.entity.web.rest.facade.XmEntityDbSearchFacade;
import com.icthh.xm.ms.entity.web.rest.util.PaginationUtil;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Search endpoints backed by the relational DB (no Elasticsearch). */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class XmEntityDbSearchResource {

    static final String SEARCH_URL = "/api/_search-db/xm-entities";

    private final XmEntityDbSearchFacade facade;

    @GetMapping(value = "/_search-db/xm-entities", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasPermission({'typeKey': #typeKey, 'query': #query, 'filter': #params}, 'XMENTITY.SEARCH.DB.QUERY')")
    @PrivilegeDescription("Privilege to search xm entities in DB by typeKey, full text query and filters (GET)")
    public ResponseEntity<List<XmEntityDto>> searchGet(@RequestParam String typeKey,
                                                       @RequestParam(required = false) String query,
                                                       @RequestParam(required = false) Boolean includeSubTypes,
                                                       @RequestParam MultiValueMap<String, String> params,
                                                       @ParameterObject Pageable pageable) {
        XmEntityDbSearchRequest request = new XmEntityDbSearchRequest();
        request.setTypeKey(typeKey);
        request.setQuery(query);
        request.setIncludeSubTypes(includeSubTypes);
        request.setFilter(toFilterBody(params));
        return respond(request, pageable);
    }

    @PostMapping(value = "/_search-db/xm-entities", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasPermission({'typeKey': #request.typeKey, 'query': #request.query, 'filter': #request.filter}, 'XMENTITY.SEARCH.DB.QUERY')")
    @PrivilegeDescription("Privilege to search xm entities in DB by typeKey, full text query and filters (POST)")
    public ResponseEntity<List<XmEntityDto>> searchPost(@RequestBody XmEntityDbSearchRequest request,
                                                        @ParameterObject Pageable pageable) {
        return respond(request, pageable);
    }

    private ResponseEntity<List<XmEntityDto>> respond(XmEntityDbSearchRequest request, Pageable pageable) {
        Page<XmEntityDto> page = facade.search(request, pageable, null);
        HttpHeaders headers = PaginationUtil.generateDbSearchPaginationHttpHeaders(linkParams(request, pageable), page, SEARCH_URL);
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    /** GET filter params are kept as raw strings; FilterParser.parseQueryParams typing is applied in the service via parseBody? No: see below. */
    static Map<String, Object> toFilterBody(MultiValueMap<String, String> params) {
        Map<String, Object> filter = new LinkedHashMap<>();
        params.forEach((key, values) -> {
            if (!FilterParser.RESERVED_PARAMS.contains(key) && values != null && !values.isEmpty()) {
                filter.put(key, values.get(0));
            }
        });
        return filter;
    }

    static Map<String, Object> linkParams(XmEntityDbSearchRequest request, Pageable pageable) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("typeKey", request.getTypeKey());
        params.put("query", request.getQuery());
        params.put("includeSubTypes", request.getIncludeSubTypes());
        if (request.getFilter() != null) {
            params.putAll(request.getFilter());
        }
        if (pageable.getSort().isSorted()) {
            params.put("sort", pageable.getSort().stream()
                .map(o -> o.getProperty() + "," + o.getDirection().name().toLowerCase()).toList());
        }
        return params;
    }
}
```

GET typing: `toFilterBody` keeps string values, and `FilterParser.parseBody` would treat `"1,2,3"` as a scalar string. Fix by letting the request carry a flag: add to `XmEntityDbSearchRequest` a field `private boolean rawStringValues;` (set `true` in `searchGet`), and in `XmEntityDbSearchService.search` use:

```java
        List<FilterCondition> conditions = request.isRawStringValues()
            ? filterParser.parseQueryParams(toListMap(request.getFilter()))
            : filterParser.parseBody(request.getFilter());
```

with

```java
    private static Map<String, List<String>> toListMap(Map<String, Object> filter) {
        Map<String, List<String>> result = new java.util.LinkedHashMap<>();
        if (filter != null) {
            filter.forEach((k, v) -> result.put(k, List.of(String.valueOf(v))));
        }
        return result;
    }
```

Mark `rawStringValues` with `@JsonIgnore` (Jackson 2 `com.fasterxml.jackson.annotation.JsonIgnore` and Jackson 3 `tools.jackson.annotation`? Spring Boot 4 MVC uses Jackson 3: use `tools.jackson.annotation.JsonIgnore`; check which one `XmEntityDto` uses and copy that import) so clients cannot set it. Remove the misleading javadoc line above `toFilterBody`.

- [ ] **Step 5: Run the endpoint test**

Run: `./gradlew test --tests '*XmEntityDbSearchResourceIntTest*' --tests '*DbSearchLepKeyResolversUnitTest*' -x runCategorizedTests`
Expected: PASS. `@WithMockUser(authorities = "SUPER-ADMIN")` bypasses `hasPermission` in this codebase (see how other `*ResourceIntTest` authenticate; if they use a different super role, copy it).

- [ ] **Step 6: Write the permission test**

`XmEntityDbSearchResourcePermissionIntTest.java`, modelled on `LinkResourcePermissionIntTest` (same role/permission push helpers, copied verbatim, `TENANT = "TEST"`):

```java
package com.icthh.xm.ms.entity.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.icthh.xm.commons.permission.service.PermissionService;
import com.icthh.xm.commons.permission.service.RoleService;
import com.icthh.xm.ms.entity.AbstractPostgresIntTest;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import com.icthh.xm.ms.entity.service.dto.XmEntityDto;
import com.icthh.xm.ms.entity.service.search.db.dto.XmEntityDbSearchRequest;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class XmEntityDbSearchResourcePermissionIntTest extends AbstractPostgresIntTest {

    private static final String TEST_ROLE = "DB_SEARCH_PERM_ROLE";
    private static final String ROLES_CONFIG_KEY = "/config/tenants/" + TENANT + "/roles.yml";
    private static final String PERMISSIONS_CONFIG_KEY = "/config/tenants/" + TENANT + "/permissions.yml";
    private static final String API_PRIVILEGE = "XMENTITY.SEARCH.DB.QUERY";
    private static final String SERVICE_PRIVILEGE = "XMENTITY.SEARCH.DB";
    private static final String ROLES_YML = TEST_ROLE + ":\n  name:\n    en: \"DB search permission test role\"\n";

    @Autowired private XmEntityDbSearchResource resource;
    @Autowired private RoleService roleService;
    @Autowired private PermissionService permissionService;
    @Autowired private XmEntityRepository repository;

    private XmEntity mine;
    private XmEntity other;

    @BeforeEach
    public void setup() {
        pushDbSearchSpec();
        roleService.onRefresh(ROLES_CONFIG_KEY, ROLES_YML);
        mine = repository.save(newEntity("ORDER", "mine", Map.of()).createdBy("user-1"));
        other = repository.save(newEntity("ORDER", "other", Map.of()).createdBy("user-2"));
    }

    @AfterEach
    public void cleanup() {
        roleService.onRefresh(ROLES_CONFIG_KEY, null);
        permissionService.onRefresh(PERMISSIONS_CONFIG_KEY, null);
    }

    private void pushPermissions(String apiCondition, boolean grantApi, String serviceCondition, boolean grantService) {
        StringBuilder yml = new StringBuilder("entity:\n  " + TEST_ROLE + ":\n");
        if (grantApi) {
            yml.append(entry(API_PRIVILEGE, apiCondition));
        }
        if (grantService) {
            yml.append(entry(SERVICE_PRIVILEGE, serviceCondition));
        }
        permissionService.onRefresh(PERMISSIONS_CONFIG_KEY, yml.toString());
    }

    private static String entry(String privilegeKey, String resourceCondition) {
        StringBuilder sb = new StringBuilder("  - privilegeKey: \"" + privilegeKey + "\"\n    disabled: false\n");
        if (resourceCondition != null) {
            sb.append("    resourceCondition: \"").append(resourceCondition).append("\"\n");
        }
        return sb.toString();
    }

    private static XmEntityDbSearchRequest order() {
        XmEntityDbSearchRequest r = new XmEntityDbSearchRequest();
        r.setTypeKey("ORDER");
        return r;
    }

    @Test
    @WithMockUser(authorities = TEST_ROLE)
    public void apiConditionOnTypeKeyDenies() {
        pushPermissions("#typeKey == 'PRODUCT'", true, null, true);
        assertThatThrownBy(() -> resource.searchPost(order(), PageRequest.of(0, 10))).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(authorities = TEST_ROLE)
    public void missingServicePrivilegeDenies() {
        pushPermissions(null, true, null, false);
        assertThatThrownBy(() -> resource.searchPost(order(), PageRequest.of(0, 10))).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(authorities = TEST_ROLE)
    public void rowLevelConditionFiltersResults() {
        pushPermissions(null, true, "#returnObject.createdBy == 'user-1'", true);
        var response = resource.searchPost(order(), PageRequest.of(0, 10));
        assertThat(response.getBody()).extracting(XmEntityDto::getId).containsExactly(mine.getId());
        assertThat(response.getHeaders().getFirst("X-Total-Count")).isEqualTo("1");
    }

    @Test
    @WithMockUser(authorities = TEST_ROLE)
    public void unconditionalGrantReturnsAll() {
        pushPermissions(null, true, null, true);
        assertThat(resource.searchPost(order(), PageRequest.of(0, 10)).getBody())
            .extracting(XmEntityDto::getId).containsExactlyInAnyOrder(mine.getId(), other.getId());
    }
}
```

- [ ] **Step 7: Run both tests**

Run: `./gradlew test --tests '*XmEntityDbSearchResource*IntTest*' -x runCategorizedTests`
Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/icthh/xm/ms/entity/service/search/db src/main/java/com/icthh/xm/ms/entity/web/rest \
        src/main/java/com/icthh/xm/ms/entity/lep/keyresolver src/test/java/com/icthh/xm/ms/entity/lep/keyresolver \
        src/test/java/com/icthh/xm/ms/entity/web/rest/XmEntityDbSearchResourceIntTest.java \
        src/test/java/com/icthh/xm/ms/entity/web/rest/XmEntityDbSearchResourcePermissionIntTest.java
git commit -m "Add DB search endpoint by typeKey with full text query and jsonb filters"
```

---

### Task 8: Link-dialog candidate search (isUnique aware)

**Files:**
- Modify: `src/main/java/com/icthh/xm/ms/entity/service/search/db/XmEntityDbSearchService.java`
- Modify: `src/main/java/com/icthh/xm/ms/entity/web/rest/facade/XmEntityDbSearchFacade.java`
- Modify: `src/main/java/com/icthh/xm/ms/entity/web/rest/XmEntityDbSearchResource.java`
- Test: `src/test/java/com/icthh/xm/ms/entity/web/rest/XmEntityDbSearchToLinkResourceIntTest.java`

**Interfaces:**
- Produces:
  ```java
  // service
  @FindWithPermission("XMENTITY.SEARCH.DB")
  Page<XmEntity> searchToLink(IdOrKey idOrKey, String entityTypeKey, String linkTypeKey,
                              XmEntityDbSearchRequest request, Pageable pageable, String privilegeKey);
  // facade
  Page<XmEntityDto> searchToLink(IdOrKey idOrKey, String entityTypeKey, String linkTypeKey,
                                 XmEntityDbSearchRequest request, Pageable pageable, String privilegeKey);
  // resource
  GET|POST /api/_search-db/xm-entities/{entityTypeKey}/{idOrKey}/links/{linkTypeKey}   privilege XMENTITY.SEARCH.DB.TO_LINK
  ```
  `request.typeKey` is ignored; the target typeKey is `LinkSpec.typeKey`.
- Consumes: `XmEntitySpecService.getLinkSpec(entityTypeKey, linkTypeKey)` → `Optional<LinkSpec>`; `XmEntityService.getXmEntityIdKeyTypeKey(IdOrKey)` → `XmEntityIdKeyTypeKey.getId()`; `LinkService.findLinkProjectionsBySourceIdAndTypeKey(Long, String)` → `List<LinkProjection>` with `getTarget().getId()`.

- [ ] **Step 1: Write the failing test**

```java
package com.icthh.xm.ms.entity.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.ms.entity.AbstractPostgresIntTest;
import com.icthh.xm.ms.entity.domain.Link;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.repository.LinkRepository;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import com.icthh.xm.ms.entity.service.dto.XmEntityDto;
import com.icthh.xm.ms.entity.service.search.db.dto.XmEntityDbSearchRequest;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;

@Transactional
@WithMockUser(authorities = "SUPER-ADMIN")
public class XmEntityDbSearchToLinkResourceIntTest extends AbstractPostgresIntTest {

    @Autowired private XmEntityDbSearchResource resource;
    @Autowired private XmEntityRepository repository;
    @Autowired private LinkRepository linkRepository;

    private XmEntity order;
    private XmEntity linkedProduct;
    private XmEntity freeProduct;

    @BeforeEach
    public void seed() {
        pushDbSearchSpec();
        order = repository.save(newEntity("ORDER", "Order", Map.of()));
        linkedProduct = repository.save(newEntity("PRODUCT", "Linked product", Map.of("sku", "A1")));
        freeProduct = repository.save(newEntity("PRODUCT", "Free product", Map.of("sku", "B2")));
        repository.save(newEntity("ORDER", "Another order", Map.of()));
        Link link = new Link();
        link.setTypeKey("ORDER.ITEM");
        link.setSource(order);
        link.setTarget(linkedProduct);
        link.setStartDate(Instant.now());
        linkRepository.save(link);
    }

    private List<Long> ids(String linkTypeKey, XmEntityDbSearchRequest request) {
        return resource.searchToLinkPost("ORDER", order.getId().toString(), linkTypeKey, request, PageRequest.of(0, 10))
            .getBody().stream().map(XmEntityDto::getId).toList();
    }

    @Test
    public void uniqueLinkExcludesAlreadyLinkedTargets() {
        assertThat(ids("ORDER.ITEM", new XmEntityDbSearchRequest())).containsExactly(freeProduct.getId());
    }

    @Test
    public void nonUniqueLinkReturnsAllTargetsOfLinkType() {
        assertThat(ids("ORDER.NOTE", new XmEntityDbSearchRequest()))
            .containsExactlyInAnyOrder(linkedProduct.getId(), freeProduct.getId());
    }

    @Test
    public void filterAppliesToCandidates() {
        XmEntityDbSearchRequest request = new XmEntityDbSearchRequest();
        request.setFilter(Map.of("data.sku.eq", "A1"));
        assertThat(ids("ORDER.NOTE", request)).containsExactly(linkedProduct.getId());
        assertThat(ids("ORDER.ITEM", request)).isEmpty();
    }

    @Test
    public void getVariantAndEntityKeyResolution() {
        var params = new LinkedMultiValueMap<String, String>();
        params.add("data.sku.eq", "B2");
        var response = resource.searchToLinkGet("ORDER", order.getKey(), "ORDER.ITEM", null, null, params, PageRequest.of(0, 10));
        assertThat(response.getBody()).extracting(XmEntityDto::getId).containsExactly(freeProduct.getId());
        assertThat(response.getHeaders().getFirst("X-Total-Count")).isEqualTo("1");
    }

    @Test
    public void unknownLinkTypeIsRejected() {
        assertThatThrownBy(() -> ids("NOPE", new XmEntityDbSearchRequest()))
            .isInstanceOf(BusinessException.class).hasMessageContaining("NOPE");
    }
}
```

- [ ] **Step 2: Run to see it fail**

Run: `./gradlew test --tests '*XmEntityDbSearchToLinkResourceIntTest*' -x runCategorizedTests`
Expected: compilation failure.

- [ ] **Step 3: Implement service method**

Add to `XmEntityDbSearchService` (new constructor deps: `XmEntitySpecService xmEntitySpecService`, `XmEntityService xmEntityService`, `LinkService linkService`; mark `xmEntityService` and `linkService` with `@Lazy` if a circular dependency appears):

```java
    @Transactional(readOnly = true)
    @LogicExtensionPoint(value = "SearchDbToLink", resolver = EntityTypeKeyAndLinkTypeKeyResolver.class)
    @FindWithPermission(ROW_PRIVILEGE)
    @PrivilegeDescription("Privilege to search link candidates in DB for an xm entity and link type")
    public Page<XmEntity> searchToLink(IdOrKey idOrKey, String entityTypeKey, String linkTypeKey,
                                       XmEntityDbSearchRequest request, Pageable pageable, String privilegeKey) {
        LinkSpec linkSpec = xmEntitySpecService.getLinkSpec(entityTypeKey, linkTypeKey)
            .orElseThrow(() -> new BusinessException(ERR_VALIDATION,
                "Link spec not found for entity type " + entityTypeKey + " and link type " + linkTypeKey));
        List<FilterCondition> conditions = parseConditions(request);
        Specification<XmEntity> spec = buildSpecification(linkSpec.getTypeKey(), request.includeSubTypes(),
            request.getQuery(), conditions, root -> root);

        if (Boolean.TRUE.equals(linkSpec.getIsUnique())) {
            Long sourceId = xmEntityService.getXmEntityIdKeyTypeKey(idOrKey).getId();
            Set<Long> excluded = linkService.findLinkProjectionsBySourceIdAndTypeKey(sourceId, linkTypeKey).stream()
                .map(l -> l.getTarget().getId())
                .collect(Collectors.toCollection(HashSet::new));
            excluded.add(sourceId);
            spec = spec.and((root, query, cb) -> cb.not(root.get(XmEntity_.id).in(excluded)));
        }
        return permittedSpecificationRepository.findAll(XmEntity.class, spec,
            sortTranslator.toOrderProvider(pageable.getSort()), pageable, privilegeKey);
    }

    private List<FilterCondition> parseConditions(XmEntityDbSearchRequest request) {
        return request.isRawStringValues()
            ? filterParser.parseQueryParams(toListMap(request.getFilter()))
            : filterParser.parseBody(request.getFilter());
    }
```

Refactor `search(...)` from Task 7 to call `parseConditions(request)` too.

- [ ] **Step 4: Facade and resource**

Facade:

```java
    public Page<XmEntityDto> searchToLink(IdOrKey idOrKey, String entityTypeKey, String linkTypeKey,
                                          XmEntityDbSearchRequest request, Pageable pageable, String privilegeKey) {
        return xmEntityDbSearchService.searchToLink(idOrKey, entityTypeKey, linkTypeKey, request, pageable, privilegeKey)
            .map(xmEntityMapper::toDto);
    }
```

Resource:

```java
    @GetMapping(value = "/_search-db/xm-entities/{entityTypeKey}/{idOrKey}/links/{linkTypeKey}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasPermission({'entityTypeKey': #entityTypeKey, 'idOrKey': #idOrKey, 'linkTypeKey': #linkTypeKey, 'query': #query, 'filter': #params}, 'XMENTITY.SEARCH.DB.TO_LINK')")
    @PrivilegeDescription("Privilege to search link candidates in DB for an xm entity and link type (GET)")
    public ResponseEntity<List<XmEntityDto>> searchToLinkGet(@PathVariable String entityTypeKey,
                                                             @PathVariable String idOrKey,
                                                             @PathVariable String linkTypeKey,
                                                             @RequestParam(required = false) String query,
                                                             @RequestParam(required = false) Boolean includeSubTypes,
                                                             @RequestParam MultiValueMap<String, String> params,
                                                             @ParameterObject Pageable pageable) {
        XmEntityDbSearchRequest request = new XmEntityDbSearchRequest();
        request.setQuery(query);
        request.setIncludeSubTypes(includeSubTypes);
        request.setFilter(toFilterBody(params));
        request.setRawStringValues(true);
        return respondToLink(entityTypeKey, idOrKey, linkTypeKey, request, pageable);
    }

    @PostMapping(value = "/_search-db/xm-entities/{entityTypeKey}/{idOrKey}/links/{linkTypeKey}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasPermission({'entityTypeKey': #entityTypeKey, 'idOrKey': #idOrKey, 'linkTypeKey': #linkTypeKey, 'query': #request.query, 'filter': #request.filter}, 'XMENTITY.SEARCH.DB.TO_LINK')")
    @PrivilegeDescription("Privilege to search link candidates in DB for an xm entity and link type (POST)")
    public ResponseEntity<List<XmEntityDto>> searchToLinkPost(@PathVariable String entityTypeKey,
                                                              @PathVariable String idOrKey,
                                                              @PathVariable String linkTypeKey,
                                                              @RequestBody XmEntityDbSearchRequest request,
                                                              @ParameterObject Pageable pageable) {
        return respondToLink(entityTypeKey, idOrKey, linkTypeKey, request, pageable);
    }

    private ResponseEntity<List<XmEntityDto>> respondToLink(String entityTypeKey, String idOrKey, String linkTypeKey,
                                                            XmEntityDbSearchRequest request, Pageable pageable) {
        Page<XmEntityDto> page = facade.searchToLink(IdOrKey.of(idOrKey), entityTypeKey, linkTypeKey, request, pageable, null);
        String url = String.format("/api/_search-db/xm-entities/%s/%s/links/%s", entityTypeKey, idOrKey, linkTypeKey);
        HttpHeaders headers = PaginationUtil.generateDbSearchPaginationHttpHeaders(linkParams(request, pageable), page, url);
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }
```

`linkParams` puts `typeKey` = null for this endpoint, which `generateDbSearchPaginationHttpHeaders` skips.

- [ ] **Step 5: Run the test**

Run: `./gradlew test --tests '*XmEntityDbSearchToLinkResourceIntTest*' --tests '*XmEntityDbSearchResourceIntTest*' -x runCategorizedTests`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/icthh/xm/ms/entity src/test/java/com/icthh/xm/ms/entity/web/rest/XmEntityDbSearchToLinkResourceIntTest.java
git commit -m "Add DB search of link candidates honoring LinkSpec.isUnique"
```

---

### Task 9: Search links of a source entity, filtered by target

**Files:**
- Modify: `src/main/java/com/icthh/xm/ms/entity/service/search/db/XmEntityDbSearchService.java`
- Modify: `src/main/java/com/icthh/xm/ms/entity/web/rest/facade/XmEntityDbSearchFacade.java`
- Modify: `src/main/java/com/icthh/xm/ms/entity/web/rest/XmEntityDbSearchResource.java`
- Test: `src/test/java/com/icthh/xm/ms/entity/web/rest/XmEntityDbSearchTargetsResourceIntTest.java`

**Interfaces:**
- Produces:
  ```java
  @FindWithPermission("LINK.SEARCH.DB")
  Page<Link> searchTargets(IdOrKey idOrKey, String linkTypeKey, XmEntityDbSearchRequest request, Pageable pageable, String privilegeKey);
  Page<LinkDto> XmEntityDbSearchFacade.searchTargets(...same...);
  GET|POST /api/_search-db/xm-entities/{idOrKey}/targets/{linkTypeKey}   privilege LINK.SEARCH.DB.TARGETS
  ```
  Filters, query, sort resolve against `link.target`. `request.typeKey` optional: when set, restricts target typeKey (with `includeSubTypes`).
- Consumes: `Link_.source`, `Link_.target`, `Link_.typeKey` metamodel; `LinkMapper.toDto(Link)`.

- [ ] **Step 1: Write the failing test**

```java
package com.icthh.xm.ms.entity.web.rest;

import static org.assertj.core.api.Assertions.assertThat;

import com.icthh.xm.ms.entity.AbstractPostgresIntTest;
import com.icthh.xm.ms.entity.domain.Link;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.repository.LinkRepository;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import com.icthh.xm.ms.entity.service.dto.LinkDto;
import com.icthh.xm.ms.entity.service.search.db.dto.XmEntityDbSearchRequest;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@WithMockUser(authorities = "SUPER-ADMIN")
public class XmEntityDbSearchTargetsResourceIntTest extends AbstractPostgresIntTest {

    @Autowired private XmEntityDbSearchResource resource;
    @Autowired private XmEntityRepository repository;
    @Autowired private LinkRepository linkRepository;

    private XmEntity order;
    private Link toA;
    private Link toB;

    private Link link(XmEntity source, XmEntity target, String typeKey) {
        Link link = new Link();
        link.setTypeKey(typeKey);
        link.setSource(source);
        link.setTarget(target);
        link.setStartDate(Instant.now());
        return linkRepository.save(link);
    }

    @BeforeEach
    public void seed() {
        pushDbSearchSpec();
        order = repository.save(newEntity("ORDER", "Order", Map.of()));
        XmEntity a = repository.save(newEntity("PRODUCT", "Product A", Map.of("sku", "A1", "price", 10)));
        XmEntity b = repository.save(newEntity("PRODUCT", "Product B", Map.of("sku", "B2", "price", 20)));
        toA = link(order, a, "ORDER.ITEM");
        toB = link(order, b, "ORDER.ITEM");
        link(order, a, "ORDER.NOTE");
        XmEntity otherOrder = repository.save(newEntity("ORDER", "Other", Map.of()));
        link(otherOrder, a, "ORDER.ITEM");
    }

    private List<Long> ids(XmEntityDbSearchRequest request, Sort sort) {
        return resource.searchTargetsPost(order.getId().toString(), "ORDER.ITEM", request, PageRequest.of(0, 10, sort))
            .getBody().stream().map(LinkDto::getId).toList();
    }

    @Test
    public void returnsLinksOfSourceAndTypeOnly() {
        assertThat(ids(new XmEntityDbSearchRequest(), Sort.unsorted())).containsExactlyInAnyOrder(toA.getId(), toB.getId());
    }

    @Test
    public void filtersAndSortsByTargetFields() {
        XmEntityDbSearchRequest request = new XmEntityDbSearchRequest();
        request.setFilter(Map.of("data.price.gt", 15));
        assertThat(ids(request, Sort.unsorted())).containsExactly(toB.getId());

        XmEntityDbSearchRequest byName = new XmEntityDbSearchRequest();
        byName.setFilter(Map.of("name.contains", "product"));
        assertThat(ids(byName, Sort.by(Sort.Direction.DESC, "data.price"))).containsExactly(toB.getId(), toA.getId());
    }

    @Test
    public void bodyContainsTargetDto() {
        var body = resource.searchTargetsPost(order.getId().toString(), "ORDER.ITEM", new XmEntityDbSearchRequest(), PageRequest.of(0, 10)).getBody();
        assertThat(body).allSatisfy(link -> assertThat(link.getTarget()).isNotNull());
    }
}
```

- [ ] **Step 2: Run to see it fail**

Run: `./gradlew test --tests '*XmEntityDbSearchTargetsResourceIntTest*' -x runCategorizedTests`
Expected: compilation failure.

- [ ] **Step 3: Implement**

Service:

```java
    @Transactional(readOnly = true)
    @LogicExtensionPoint(value = "SearchDbTargets", resolver = LinkTypeKeyParamResolver.class)
    @FindWithPermission("LINK.SEARCH.DB")
    @PrivilegeDescription("Privilege to search links of a source xm entity in DB, filtered by target fields")
    public Page<Link> searchTargets(IdOrKey idOrKey, String linkTypeKey, XmEntityDbSearchRequest request,
                                    Pageable pageable, String privilegeKey) {
        Long sourceId = xmEntityService.getXmEntityIdKeyTypeKey(idOrKey).getId();
        List<FilterCondition> conditions = parseConditions(request);
        Function<Root<Link>, Path<XmEntity>> target = root -> root.get(Link_.target);

        Specification<Link> spec = (root, query, cb) -> cb.and(
            cb.equal(root.get(Link_.source).get(XmEntity_.id), sourceId),
            cb.equal(root.get(Link_.typeKey), linkTypeKey));
        spec = spec.and(specificationBuilder.build(conditions, target));
        if (StringUtils.isNotBlank(request.getTypeKey())) {
            spec = spec.and(specificationBuilder.typeKey(request.getTypeKey(), request.includeSubTypes(), target));
        }
        if (!XmEntityFilterSpecificationBuilder.hasRemovedCondition(conditions)) {
            spec = spec.and(specificationBuilder.notRemoved(target));
        }
        if (StringUtils.isNotBlank(request.getQuery())) {
            spec = spec.and(specificationBuilder.fullText(request.getQuery(), target));
        }
        return permittedSpecificationRepository.findAll(Link.class, spec,
            sortTranslator.toOrderProvider(pageable.getSort(), target), pageable, privilegeKey);
    }
```

Facade (add `LinkMapper linkMapper` dependency):

```java
    public Page<LinkDto> searchTargets(IdOrKey idOrKey, String linkTypeKey, XmEntityDbSearchRequest request,
                                       Pageable pageable, String privilegeKey) {
        return xmEntityDbSearchService.searchTargets(idOrKey, linkTypeKey, request, pageable, privilegeKey).map(linkMapper::toDto);
    }
```

Resource:

```java
    @GetMapping(value = "/_search-db/xm-entities/{idOrKey}/targets/{linkTypeKey}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasPermission({'idOrKey': #idOrKey, 'linkTypeKey': #linkTypeKey, 'query': #query, 'filter': #params}, 'LINK.SEARCH.DB.TARGETS')")
    @PrivilegeDescription("Privilege to search links of a source xm entity in DB filtered by target fields (GET)")
    public ResponseEntity<List<LinkDto>> searchTargetsGet(@PathVariable String idOrKey,
                                                          @PathVariable String linkTypeKey,
                                                          @RequestParam(required = false) String typeKey,
                                                          @RequestParam(required = false) String query,
                                                          @RequestParam(required = false) Boolean includeSubTypes,
                                                          @RequestParam MultiValueMap<String, String> params,
                                                          @ParameterObject Pageable pageable) {
        XmEntityDbSearchRequest request = new XmEntityDbSearchRequest();
        request.setTypeKey(typeKey);
        request.setQuery(query);
        request.setIncludeSubTypes(includeSubTypes);
        request.setFilter(toFilterBody(params));
        request.setRawStringValues(true);
        return respondTargets(idOrKey, linkTypeKey, request, pageable);
    }

    @PostMapping(value = "/_search-db/xm-entities/{idOrKey}/targets/{linkTypeKey}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasPermission({'idOrKey': #idOrKey, 'linkTypeKey': #linkTypeKey, 'query': #request.query, 'filter': #request.filter}, 'LINK.SEARCH.DB.TARGETS')")
    @PrivilegeDescription("Privilege to search links of a source xm entity in DB filtered by target fields (POST)")
    public ResponseEntity<List<LinkDto>> searchTargetsPost(@PathVariable String idOrKey,
                                                           @PathVariable String linkTypeKey,
                                                           @RequestBody XmEntityDbSearchRequest request,
                                                           @ParameterObject Pageable pageable) {
        return respondTargets(idOrKey, linkTypeKey, request, pageable);
    }

    private ResponseEntity<List<LinkDto>> respondTargets(String idOrKey, String linkTypeKey,
                                                         XmEntityDbSearchRequest request, Pageable pageable) {
        Page<LinkDto> page = facade.searchTargets(IdOrKey.of(idOrKey), linkTypeKey, request, pageable, null);
        String url = String.format("/api/_search-db/xm-entities/%s/targets/%s", idOrKey, linkTypeKey);
        HttpHeaders headers = PaginationUtil.generateDbSearchPaginationHttpHeaders(linkParams(request, pageable), page, url);
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }
```

Also fix the GET variant of Task 7 (`searchGet`) to call `request.setRawStringValues(true)` if not already done.

- [ ] **Step 4: Run the test**

Run: `./gradlew test --tests '*XmEntityDbSearchTargetsResourceIntTest*' -x runCategorizedTests`
Expected: PASS. If the permission condition for `Link` from xm-commons contains `returnObject.target...`, the alias rewrite in `PermittedSpecificationRepository` handles it.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/icthh/xm/ms/entity src/test/java/com/icthh/xm/ms/entity/web/rest/XmEntityDbSearchTargetsResourceIntTest.java
git commit -m "Add DB search of links by source entity filtered by target fields"
```

---

### Task 10: JPQL templates configuration service

**Files:**
- Create: `src/main/java/com/icthh/xm/ms/entity/service/search/db/template/JpqlTemplateType.java`
- Create: `src/main/java/com/icthh/xm/ms/entity/service/search/db/template/JpqlTemplate.java`
- Create: `src/main/java/com/icthh/xm/ms/entity/service/search/db/template/XmEntityJpqlTemplatesService.java`
- Modify: `src/main/java/com/icthh/xm/ms/entity/config/ApplicationProperties.java`
- Modify: `src/main/resources/config/application.yml` (after `specification-templates-path-pattern`)
- Modify: `src/test/resources/config/application.yml` (after line 188 `specification-templates-path-pattern`)
- Create: `src/test/resources/config/templates/jpql-templates-dbsearch.yml`
- Test: `src/test/java/com/icthh/xm/ms/entity/service/search/db/template/XmEntityJpqlTemplatesServiceUnitTest.java`

**Interfaces:**
- Produces:
  ```java
  public enum JpqlTemplateType { ENTITY, RAW }
  @Data public class JpqlTemplate {
      private String key; private JpqlTemplateType type = JpqlTemplateType.ENTITY; private String query;
      private String countQuery; private Map<String, String> params = new HashMap<>();   // name -> number|string|boolean|instant|list
      public Set<String> paramNames();            // ":name" tokens in query (and countQuery)
  }
  @Service public class XmEntityJpqlTemplatesService implements RefreshableConfiguration {
      public JpqlTemplate getTemplate(String key);                 // throws EntityNotFoundException
      public Map<String, Object> coerce(JpqlTemplate template, Map<String, ?> rawParams); // applies template.params types to String values
  }
  // ApplicationProperties
  private String jpqlTemplatesPathPattern;        // /config/tenants/{tenantName}/entity/jpql-templates.yml
  private String jpqlTemplatesFolderPathPattern;  // /config/tenants/{tenantName}/entity/jpql-templates/*.yml
  ```
- YAML shape: map keyed by template key (like `search-templates.yml`).

- [ ] **Step 1: Properties and fixture**

`ApplicationProperties.java`, after `specificationTemplatesName`:

```java
    private String jpqlTemplatesPathPattern;
    private String jpqlTemplatesFolderPathPattern;
```

`src/main/resources/config/application.yml`, after `specification-templates-path-pattern`:

```yaml
    jpql-templates-path-pattern: /config/tenants/{tenantName}/entity/jpql-templates.yml
    jpql-templates-folder-path-pattern: /config/tenants/{tenantName}/entity/jpql-templates/*.yml
```

`src/test/resources/config/application.yml`, same two lines after its `specification-templates-path-pattern`.

`src/test/resources/config/templates/jpql-templates-dbsearch.yml`:

```yaml
ACTIVE_BY_ORDER:
  query: "entity.typeKey = :typeKey and entity.stateKey = :stateKey and json_query(entity.data, '$.orderNo') = to_json_b(:orderNo)"
  params:
    orderNo: number
MY_ORDERS:
  query: "entity.typeKey = 'ORDER' and entity.createdBy = :subjectUserKey"
ORDERS_SUMMARY:
  type: RAW
  query: >
    select e.id as id, e.name as name, json_query(e.data, '$.orderNo') as orderNo
    from XmEntity e where e.typeKey = :typeKey order by e.name asc
  countQuery: "select count(e) from XmEntity e where e.typeKey = :typeKey"
ORDER_ENTITIES_RAW:
  type: RAW
  query: "select e from XmEntity e where e.typeKey = :typeKey order by e.name asc"
MIXED_RAW:
  type: RAW
  query: "select e, e.name from XmEntity e where e.typeKey = :typeKey order by e.name asc"
```

- [ ] **Step 2: Write the failing unit test**

```java
package com.icthh.xm.ms.entity.service.search.db.template;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.exceptions.EntityNotFoundException;
import com.icthh.xm.commons.tenant.TenantContext;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.ms.entity.AbstractJupiterUnitTest;
import com.icthh.xm.ms.entity.config.ApplicationProperties;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class XmEntityJpqlTemplatesServiceUnitTest extends AbstractJupiterUnitTest {

    private static final String FILE_KEY = "/config/tenants/TEST/entity/jpql-templates.yml";
    private static final String FOLDER_KEY_A = "/config/tenants/TEST/entity/jpql-templates/a.yml";

    private XmEntityJpqlTemplatesService service;

    @BeforeEach
    public void setUp() {
        ApplicationProperties props = new ApplicationProperties();
        props.setJpqlTemplatesPathPattern("/config/tenants/{tenantName}/entity/jpql-templates.yml");
        props.setJpqlTemplatesFolderPathPattern("/config/tenants/{tenantName}/entity/jpql-templates/*.yml");
        TenantContextHolder holder = mock(TenantContextHolder.class);
        TenantContext ctx = mock(TenantContext.class);
        when(holder.getContext()).thenReturn(ctx);
        when(ctx.getTenantKey()).thenReturn(Optional.of(TenantKey.valueOf("TEST")));
        service = new XmEntityJpqlTemplatesService(props, holder);
    }

    @Test
    public void listensToFileAndFolderPatternsOnly() {
        assertThat(service.isListeningConfiguration(FILE_KEY)).isTrue();
        assertThat(service.isListeningConfiguration(FOLDER_KEY_A)).isTrue();
        assertThat(service.isListeningConfiguration("/config/tenants/TEST/entity/search-templates.yml")).isFalse();
    }

    @Test
    public void mergesFileAndFolderAndParsesTypes() {
        service.onRefresh(FILE_KEY, "A:\n  query: \"entity.name = :name\"\n");
        service.onRefresh(FOLDER_KEY_A, "B:\n  type: RAW\n  query: \"select e from XmEntity e where e.id = :id\"\n  countQuery: \"select count(e) from XmEntity e where e.id = :id\"\n  params:\n    id: number\n");

        JpqlTemplate a = service.getTemplate("A");
        assertThat(a.getType()).isEqualTo(JpqlTemplateType.ENTITY);
        assertThat(a.paramNames()).containsExactly("name");

        JpqlTemplate b = service.getTemplate("B");
        assertThat(b.getType()).isEqualTo(JpqlTemplateType.RAW);
        assertThat(b.getCountQuery()).contains("count(e)");
        assertThat(b.paramNames()).containsExactly("id");
    }

    @Test
    public void removingConfigDropsTemplates() {
        service.onRefresh(FILE_KEY, "A:\n  query: \"entity.name = :name\"\n");
        service.onRefresh(FILE_KEY, null);
        assertThatThrownBy(() -> service.getTemplate("A")).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    public void coercesDeclaredParamTypesAndKeepsOthers() {
        JpqlTemplate t = new JpqlTemplate();
        t.setParams(Map.of("n", "number", "f", "number", "b", "boolean", "d", "instant", "l", "list"));

        Map<String, Object> coerced = service.coerce(t, Map.of(
            "n", "5", "f", "2.5", "b", "true", "d", "2026-01-01T00:00:00Z", "l", "a,b", "s", "text", "already", 7));

        assertThat(coerced).containsEntry("n", 5L).containsEntry("f", 2.5d).containsEntry("b", true)
            .containsEntry("d", Instant.parse("2026-01-01T00:00:00Z")).containsEntry("l", List.of("a", "b"))
            .containsEntry("s", "text").containsEntry("already", 7);
    }

    @Test
    public void coerceRejectsBadValue() {
        JpqlTemplate t = new JpqlTemplate();
        t.setParams(Map.of("n", "number"));
        assertThatThrownBy(() -> service.coerce(t, Map.of("n", "abc"))).isInstanceOf(BusinessException.class).hasMessageContaining("n");
    }
}
```

If `ApplicationProperties` has no setters (check for `@Data`/`@Setter`), construct it via reflection helper `org.springframework.test.util.ReflectionTestUtils.setField(props, "jpqlTemplatesPathPattern", ...)`.

- [ ] **Step 3: Implement**

`JpqlTemplateType.java`:

```java
package com.icthh.xm.ms.entity.service.search.db.template;

public enum JpqlTemplateType { ENTITY, RAW }
```

`JpqlTemplate.java`:

```java
package com.icthh.xm.ms.entity.service.search.db.template;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Data;

@Data
public class JpqlTemplate {

    private static final Pattern PARAM = Pattern.compile("(?<![:\\w]):([A-Za-z_][A-Za-z0-9_]*)");

    private String key;
    private JpqlTemplateType type = JpqlTemplateType.ENTITY;
    private String query;
    private String countQuery;
    /** param name → number | string | boolean | instant | list (used to coerce GET string values). */
    private Map<String, String> params = new HashMap<>();

    public Set<String> paramNames() {
        Set<String> names = new LinkedHashSet<>();
        collect(query, names);
        collect(countQuery, names);
        return names;
    }

    private static void collect(String text, Set<String> names) {
        if (text == null) {
            return;
        }
        Matcher m = PARAM.matcher(text);
        while (m.find()) {
            names.add(m.group(1));
        }
    }
}
```

`XmEntityJpqlTemplatesService.java`:

```java
package com.icthh.xm.ms.entity.service.search.db.template;

import static com.icthh.xm.commons.exceptions.ErrorConstants.ERR_VALIDATION;

import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.exceptions.EntityNotFoundException;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.tenant.YamlMapperUtils;
import com.icthh.xm.ms.entity.config.ApplicationProperties;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/** Tenant config: {@code entity/jpql-templates.yml} plus {@code entity/jpql-templates/*.yml}, merged per tenant. */
@Slf4j
@Service
@RequiredArgsConstructor
public class XmEntityJpqlTemplatesService implements RefreshableConfiguration {

    private static final String TENANT_NAME = "tenantName";
    private static final ObjectMapper MAPPER = YamlMapperUtils.yamlDefaultMapper();

    private final AntPathMatcher matcher = new AntPathMatcher();
    /** tenant → config path → templates in that file. */
    private final Map<String, Map<String, Map<String, JpqlTemplate>>> templatesByTenant = new ConcurrentHashMap<>();
    private final ApplicationProperties applicationProperties;
    private final TenantContextHolder tenantContextHolder;

    public JpqlTemplate getTemplate(String key) {
        String tenant = TenantContextUtils.getRequiredTenantKeyValue(tenantContextHolder);
        Map<String, JpqlTemplate> merged = new LinkedHashMap<>();
        templatesByTenant.getOrDefault(tenant, Map.of()).values().forEach(file -> file.forEach((k, t) -> {
            if (merged.put(k, t) != null) {
                log.warn("JPQL template {} defined more than once for tenant {}, last file wins", k, tenant);
            }
        }));
        JpqlTemplate template = merged.get(key);
        if (template == null || StringUtils.isBlank(template.getQuery())) {
            throw new EntityNotFoundException("JPQL template not found: " + key);
        }
        return template;
    }

    public Map<String, Object> coerce(JpqlTemplate template, Map<String, ?> rawParams) {
        Map<String, Object> result = new HashMap<>();
        Map<String, String> types = template.getParams() == null ? Map.of() : template.getParams();
        rawParams.forEach((name, value) -> result.put(name, coerceValue(name, types.get(name), value)));
        return result;
    }

    private static Object coerceValue(String name, String type, Object value) {
        if (type == null || !(value instanceof String s)) {
            return value;
        }
        try {
            return switch (type) {
                case "number" -> s.contains(".") ? (Object) Double.valueOf(s) : (Object) Long.valueOf(s);
                case "boolean" -> Boolean.valueOf(s);
                case "instant" -> Instant.parse(s);
                case "list" -> Arrays.stream(s.split(",")).map(String::trim).toList();
                default -> s;
            };
        } catch (NumberFormatException | DateTimeParseException e) {
            throw new BusinessException(ERR_VALIDATION, "Invalid value for template param " + name + ": " + value);
        }
    }

    @Override
    public void onRefresh(String updatedKey, String config) {
        try {
            String tenant = matcher.extractUriTemplateVariables(patternFor(updatedKey), updatedKey).get(TENANT_NAME);
            Map<String, Map<String, JpqlTemplate>> files = templatesByTenant.computeIfAbsent(tenant, t -> new ConcurrentHashMap<>());
            if (StringUtils.isBlank(config)) {
                files.remove(updatedKey);
                log.info("JPQL templates {} removed for tenant {}", updatedKey, tenant);
            } else {
                Map<String, JpqlTemplate> parsed = MAPPER.readValue(config, new TypeReference<Map<String, JpqlTemplate>>() {});
                parsed.forEach((k, t) -> t.setKey(k));
                files.put(updatedKey, parsed);
                log.info("JPQL templates {} updated for tenant {}: {}", updatedKey, tenant, parsed.keySet());
            }
        } catch (Exception e) {
            log.error("Error reading JPQL templates from {}", updatedKey, e);
        }
    }

    @Override
    public boolean isListeningConfiguration(String updatedKey) {
        return matcher.match(applicationProperties.getJpqlTemplatesPathPattern(), updatedKey)
            || matcher.match(applicationProperties.getJpqlTemplatesFolderPathPattern(), updatedKey);
    }

    @Override
    public void onInit(String key, String config) {
        if (isListeningConfiguration(key)) {
            onRefresh(key, config);
        }
    }

    private String patternFor(String key) {
        return matcher.match(applicationProperties.getJpqlTemplatesPathPattern(), key)
            ? applicationProperties.getJpqlTemplatesPathPattern()
            : applicationProperties.getJpqlTemplatesFolderPathPattern();
    }
}
```

- [ ] **Step 4: Run the test**

Run: `./gradlew test --tests '*XmEntityJpqlTemplatesServiceUnitTest*' -x runCategorizedTests`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/icthh/xm/ms/entity/service/search/db/template src/main/java/com/icthh/xm/ms/entity/config/ApplicationProperties.java \
        src/main/resources/config/application.yml src/test/resources/config/application.yml \
        src/test/resources/config/templates/jpql-templates-dbsearch.yml \
        src/test/java/com/icthh/xm/ms/entity/service/search/db/template/XmEntityJpqlTemplatesServiceUnitTest.java
git commit -m "Add refreshable JPQL templates configuration service"
```

---

### Task 11: JPQL template execution, ENTITY type, endpoint

**Files:**
- Create: `src/main/java/com/icthh/xm/ms/entity/service/search/db/template/JpqlTemplateExecutor.java`
- Modify: `src/main/java/com/icthh/xm/ms/entity/service/search/db/XmEntityDbSearchService.java`
- Modify: `src/main/java/com/icthh/xm/ms/entity/web/rest/facade/XmEntityDbSearchFacade.java`
- Modify: `src/main/java/com/icthh/xm/ms/entity/web/rest/XmEntityDbSearchResource.java`
- Test: `src/test/java/com/icthh/xm/ms/entity/web/rest/XmEntityDbSearchTemplateResourceIntTest.java`

**Interfaces:**
- Produces:
  ```java
  @Component public class JpqlTemplateExecutor {
      public static final Set<String> SUBJECT_PARAMS = Set.of("subjectUserKey", "subjectLogin", "subjectTenant");
      /** Validates required params, adds subject params, returns the bound param map. Throws BusinessException on missing. */
      public Map<String, Object> bindParams(JpqlTemplate template, Map<String, Object> supplied);
  }
  // service
  @FindWithPermission("XMENTITY.SEARCH.DB")
  Page<XmEntity> searchByEntityTemplate(JpqlTemplate template, Map<String, Object> params, Pageable pageable, String privilegeKey);
  // facade
  Page<XmEntityDto> searchByEntityTemplate(String templateKey, Map<String, Object> rawParams, Pageable pageable, String privilegeKey);
  // resource
  GET|POST /api/_search-db/xm-entities/template/{templateKey}   privilege XMENTITY.SEARCH.DB.TEMPLATE
  ```
  In this task the resource handles only ENTITY templates; RAW handling is added in Task 12. The resource returns `ResponseEntity<List<?>>` so both shapes fit.
- Consumes: `XmEntityJpqlTemplatesService.getTemplate/coerce` (Task 10), `PermittedSpecificationRepository.findAll(Class, whereFragment, params, spec, orders, pageable, privilegeKey)` (Task 2), `SortTranslator` (Task 5), `XmAuthenticationContextHolder.getContext().getUserKey()/getLogin()` (`Optional<String>`), `TenantContextUtils.getRequiredTenantKeyValue(tenantContextHolder)`.

- [ ] **Step 1: Write the failing test**

```java
package com.icthh.xm.ms.entity.web.rest;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.exceptions.EntityNotFoundException;
import com.icthh.xm.ms.entity.AbstractPostgresIntTest;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import com.icthh.xm.ms.entity.service.dto.XmEntityDto;
import com.icthh.xm.ms.entity.service.search.db.template.XmEntityJpqlTemplatesService;
import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;

@Transactional
@WithMockUser(authorities = "SUPER-ADMIN")
public class XmEntityDbSearchTemplateResourceIntTest extends AbstractPostgresIntTest {

    @Autowired private XmEntityDbSearchResource resource;
    @Autowired private XmEntityRepository repository;
    @Autowired private XmEntityJpqlTemplatesService templatesService;

    private XmEntity active1;
    private XmEntity active2;

    @SneakyThrows
    @BeforeEach
    public void seed() {
        pushDbSearchSpec();
        String yml = IOUtils.toString(new ClassPathResource("config/templates/jpql-templates-dbsearch.yml").getInputStream(), UTF_8);
        templatesService.onRefresh(applicationProperties.getJpqlTemplatesPathPattern().replace("{tenantName}", TENANT), yml);

        active1 = repository.save(newEntity("ORDER", "B order", Map.of("orderNo", 1)).stateKey("ACTIVE"));
        active2 = repository.save(newEntity("ORDER", "A order", Map.of("orderNo", 1)).stateKey("ACTIVE"));
        repository.save(newEntity("ORDER", "closed", Map.of("orderNo", 1)).stateKey("CLOSED"));
        repository.save(newEntity("ORDER", "other no", Map.of("orderNo", 2)).stateKey("ACTIVE"));
    }

    @SuppressWarnings("unchecked")
    private List<XmEntityDto> entities(Object body) {
        return (List<XmEntityDto>) body;
    }

    @Test
    public void postEntityTemplateBindsTypedParamsAndSorts() {
        var response = resource.searchByTemplatePost("ACTIVE_BY_ORDER",
            Map.of("typeKey", "ORDER", "stateKey", "ACTIVE", "orderNo", 1),
            PageRequest.of(0, 10, Sort.by("name")));

        assertThat(entities(response.getBody())).extracting(XmEntityDto::getId).containsExactly(active2.getId(), active1.getId());
        assertThat(response.getHeaders().getFirst("X-Total-Count")).isEqualTo("2");
    }

    @Test
    public void getEntityTemplateCoercesStringParams() {
        var params = new LinkedMultiValueMap<String, String>();
        params.add("typeKey", "ORDER");
        params.add("stateKey", "ACTIVE");
        params.add("orderNo", "1");
        params.add("page", "0");
        params.add("size", "1");

        var response = resource.searchByTemplateGet("ACTIVE_BY_ORDER", params, PageRequest.of(0, 1, Sort.by("name")));

        assertThat(entities(response.getBody())).extracting(XmEntityDto::getId).containsExactly(active2.getId());
        assertThat(response.getHeaders().getFirst("X-Total-Count")).isEqualTo("2");
    }

    @Test
    public void missingParamIsRejected() {
        assertThatThrownBy(() -> resource.searchByTemplatePost("ACTIVE_BY_ORDER", Map.of("typeKey", "ORDER"), PageRequest.of(0, 10)))
            .isInstanceOf(BusinessException.class).hasMessageContaining("stateKey");
    }

    @Test
    public void unknownTemplateIs404() {
        assertThatThrownBy(() -> resource.searchByTemplatePost("NOPE", Map.of(), PageRequest.of(0, 10)))
            .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    public void badSortIsRejected() {
        assertThatThrownBy(() -> resource.searchByTemplatePost("ACTIVE_BY_ORDER",
            Map.of("typeKey", "ORDER", "stateKey", "ACTIVE", "orderNo", 1), PageRequest.of(0, 10, Sort.by("evil"))))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    @WithMockUser(username = "u1", authorities = "SUPER-ADMIN")
    public void subjectUserKeyIsBoundAutomatically() {
        XmEntity mine = repository.save(newEntity("ORDER", "mine", Map.of()).createdBy(currentUserKey()));
        var response = resource.searchByTemplatePost("MY_ORDERS", Map.of(), PageRequest.of(0, 10));
        assertThat(entities(response.getBody())).extracting(XmEntityDto::getId).contains(mine.getId());
    }

    private String currentUserKey() {
        return xmAuthenticationContextHolder.getContext().getUserKey().orElse("u1");
    }
}
```

`XmEntity.stateKey(String)` / `createdBy(String)` fluent setters: verify they exist on `XmEntity` (the class uses fluent methods like `typeKey(...)`); if `stateKey(...)` is missing, use `setStateKey` on the saved instance instead.

- [ ] **Step 2: Run to see it fail**

Run: `./gradlew test --tests '*XmEntityDbSearchTemplateResourceIntTest*' -x runCategorizedTests`
Expected: compilation failure.

- [ ] **Step 3: Implement executor param binding**

```java
package com.icthh.xm.ms.entity.service.search.db.template;

import static com.icthh.xm.commons.exceptions.ErrorConstants.ERR_VALIDATION;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpqlTemplateExecutor {

    public static final String SUBJECT_USER_KEY = "subjectUserKey";
    public static final String SUBJECT_LOGIN = "subjectLogin";
    public static final String SUBJECT_TENANT = "subjectTenant";
    public static final Set<String> SUBJECT_PARAMS = Set.of(SUBJECT_USER_KEY, SUBJECT_LOGIN, SUBJECT_TENANT);

    private final XmAuthenticationContextHolder authContextHolder;
    private final TenantContextHolder tenantContextHolder;

    /** Returns exactly the params the template references: supplied values plus subject params. */
    public Map<String, Object> bindParams(JpqlTemplate template, Map<String, Object> supplied) {
        Map<String, Object> bound = new HashMap<>();
        for (String name : template.paramNames()) {
            if (supplied.containsKey(name)) {
                bound.put(name, supplied.get(name));
            } else if (SUBJECT_PARAMS.contains(name)) {
                bound.put(name, subjectValue(name));
            } else {
                throw new BusinessException(ERR_VALIDATION, "Template param is required: " + name);
            }
        }
        return bound;
    }

    private Object subjectValue(String name) {
        return switch (name) {
            case SUBJECT_USER_KEY -> authContextHolder.getContext().getUserKey().orElse(null);
            case SUBJECT_LOGIN -> authContextHolder.getContext().getLogin().orElse(null);
            case SUBJECT_TENANT -> TenantContextUtils.getRequiredTenantKeyValue(tenantContextHolder);
            default -> null;
        };
    }
}
```

- [ ] **Step 4: Service, facade, resource**

Service (add deps `XmEntityJpqlTemplatesService jpqlTemplatesService`, `JpqlTemplateExecutor templateExecutor`):

```java
    @Transactional(readOnly = true)
    @LogicExtensionPoint(value = "SearchDbByEntityTemplate", resolver = JpqlTemplateKeyResolver.class)
    @FindWithPermission(ROW_PRIVILEGE)
    @PrivilegeDescription("Privilege to search xm entities in DB by an ENTITY JPQL template")
    public Page<XmEntity> searchByEntityTemplate(JpqlTemplate template, Map<String, Object> params,
                                                 Pageable pageable, String privilegeKey) {
        if (template.getType() != JpqlTemplateType.ENTITY) {
            throw new BusinessException(ERR_VALIDATION, "Template is not of type ENTITY: " + template.getKey());
        }
        Map<String, Object> bound = templateExecutor.bindParams(template, params);
        return permittedSpecificationRepository.findAll(XmEntity.class, template.getQuery(), bound, null,
            sortTranslator.toOrderProvider(pageable.getSort()), pageable, privilegeKey);
    }
```

Facade (add dep `XmEntityJpqlTemplatesService jpqlTemplatesService`):

```java
    public JpqlTemplate template(String templateKey) {
        return jpqlTemplatesService.getTemplate(templateKey);
    }

    public Page<XmEntityDto> searchByEntityTemplate(JpqlTemplate template, Map<String, Object> rawParams,
                                                    Pageable pageable, String privilegeKey) {
        Map<String, Object> params = jpqlTemplatesService.coerce(template, rawParams);
        return xmEntityDbSearchService.searchByEntityTemplate(template, params, pageable, privilegeKey).map(xmEntityMapper::toDto);
    }
```

Resource:

```java
    private static final Set<String> PAGE_PARAMS = Set.of("page", "size", "sort");

    @GetMapping(value = "/_search-db/xm-entities/template/{templateKey}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasPermission({'templateKey': #templateKey, 'params': #params}, 'XMENTITY.SEARCH.DB.TEMPLATE')")
    @PrivilegeDescription("Privilege to search xm entities in DB by a JPQL template (GET)")
    public ResponseEntity<List<?>> searchByTemplateGet(@PathVariable String templateKey,
                                                       @RequestParam MultiValueMap<String, String> params,
                                                       @ParameterObject Pageable pageable) {
        Map<String, Object> templateParams = new LinkedHashMap<>();
        params.forEach((k, v) -> {
            if (!PAGE_PARAMS.contains(k) && v != null && !v.isEmpty()) {
                templateParams.put(k, v.get(0));
            }
        });
        return respondTemplate(templateKey, templateParams, pageable);
    }

    @PostMapping(value = "/_search-db/xm-entities/template/{templateKey}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasPermission({'templateKey': #templateKey, 'params': #params}, 'XMENTITY.SEARCH.DB.TEMPLATE')")
    @PrivilegeDescription("Privilege to search xm entities in DB by a JPQL template (POST)")
    public ResponseEntity<List<?>> searchByTemplatePost(@PathVariable String templateKey,
                                                        @RequestBody(required = false) Map<String, Object> params,
                                                        @ParameterObject Pageable pageable) {
        return respondTemplate(templateKey, params == null ? Map.of() : params, pageable);
    }

    private ResponseEntity<List<?>> respondTemplate(String templateKey, Map<String, Object> params, Pageable pageable) {
        JpqlTemplate template = facade.template(templateKey);
        String url = "/api/_search-db/xm-entities/template/" + templateKey;
        Map<String, Object> linkParams = new LinkedHashMap<>(params);
        if (pageable.getSort().isSorted()) {
            linkParams.put("sort", pageable.getSort().stream()
                .map(o -> o.getProperty() + "," + o.getDirection().name().toLowerCase()).toList());
        }
        Page<XmEntityDto> page = facade.searchByEntityTemplate(template, params, pageable, null);
        HttpHeaders headers = PaginationUtil.generateDbSearchPaginationHttpHeaders(linkParams, page, url);
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }
```

- [ ] **Step 5: Run the test**

Run: `./gradlew test --tests '*XmEntityDbSearchTemplateResourceIntTest*' -x runCategorizedTests`
Expected: PASS. If Hibernate rejects `to_json_b(:orderNo)` with an unknown parameter type, change the fixture template to `to_json_b(cast(:orderNo as Long))`, and note this in the spec's template rules.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/icthh/xm/ms/entity src/test/java/com/icthh/xm/ms/entity/web/rest/XmEntityDbSearchTemplateResourceIntTest.java
git commit -m "Add ENTITY JPQL template search with row-level permissions"
```

---

### Task 12: RAW JPQL templates returning rows

**Files:**
- Modify: `src/main/java/com/icthh/xm/ms/entity/service/search/db/template/JpqlTemplateExecutor.java`
- Modify: `src/main/java/com/icthh/xm/ms/entity/web/rest/facade/XmEntityDbSearchFacade.java`
- Modify: `src/main/java/com/icthh/xm/ms/entity/web/rest/XmEntityDbSearchResource.java`
- Test: `src/test/java/com/icthh/xm/ms/entity/web/rest/XmEntityDbSearchRawTemplateResourceIntTest.java`

**Interfaces:**
- Produces:
  ```java
  // executor
  public record RawResult(List<Map<String, Object>> rows, Long total /* null when no countQuery */) {}
  public RawResult executeRaw(JpqlTemplate template, Map<String, Object> params, Pageable pageable,
                              Function<Object, Object> entityToDto);
  // facade
  public RawResult searchByRawTemplate(JpqlTemplate template, Map<String, Object> rawParams, Pageable pageable);
  ```
  Row keys: selection alias, else `col<index>`. Entity values mapped through `entityToDto` (XmEntity → XmEntityDto, Link → LinkDto, others returned as-is).
- Consumes: `EntityManager.createQuery(String, Tuple.class)`, Task 10 and 11 classes.

- [ ] **Step 1: Write the failing test**

```java
package com.icthh.xm.ms.entity.web.rest;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.ms.entity.AbstractPostgresIntTest;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import com.icthh.xm.ms.entity.service.dto.XmEntityDto;
import com.icthh.xm.ms.entity.service.search.db.template.XmEntityJpqlTemplatesService;
import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@WithMockUser(authorities = "SUPER-ADMIN")
public class XmEntityDbSearchRawTemplateResourceIntTest extends AbstractPostgresIntTest {

    @Autowired private XmEntityDbSearchResource resource;
    @Autowired private XmEntityRepository repository;
    @Autowired private XmEntityJpqlTemplatesService templatesService;

    private XmEntity a;
    private XmEntity b;

    @SneakyThrows
    @BeforeEach
    public void seed() {
        pushDbSearchSpec();
        String yml = IOUtils.toString(new ClassPathResource("config/templates/jpql-templates-dbsearch.yml").getInputStream(), UTF_8);
        templatesService.onRefresh(applicationProperties.getJpqlTemplatesPathPattern().replace("{tenantName}", TENANT), yml);
        a = repository.save(newEntity("RAW_T", "A", Map.of("orderNo", 1)));
        b = repository.save(newEntity("RAW_T", "B", Map.of("orderNo", 2)));
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> rows(Object body) {
        return (List<Map<String, Object>>) body;
    }

    @Test
    public void scalarProjectionUsesAliasesAndCountHeaders() {
        var response = resource.searchByTemplatePost("ORDERS_SUMMARY", Map.of("typeKey", "RAW_T"), PageRequest.of(0, 1));

        List<Map<String, Object>> rows = rows(response.getBody());
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0)).containsEntry("id", a.getId()).containsEntry("name", "A").containsKey("orderNo");
        assertThat(response.getHeaders().getFirst("X-Total-Count")).isEqualTo("2");
        assertThat(response.getHeaders().getFirst("Link")).contains("rel=\"next\"");
    }

    @Test
    public void entitySelectionIsMappedToDtoAndNoCountHeaderWithoutCountQuery() {
        var response = resource.searchByTemplatePost("ORDER_ENTITIES_RAW", Map.of("typeKey", "RAW_T"), PageRequest.of(0, 10));

        List<Map<String, Object>> rows = rows(response.getBody());
        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).get("col0")).isInstanceOf(XmEntityDto.class);
        assertThat(((XmEntityDto) rows.get(0).get("col0")).getId()).isEqualTo(a.getId());
        assertThat(response.getHeaders().getFirst("X-Total-Count")).isNull();
    }

    @Test
    public void mixedSelectionUsesPositionalKeys() {
        var rows = rows(resource.searchByTemplatePost("MIXED_RAW", Map.of("typeKey", "RAW_T"), PageRequest.of(0, 10)).getBody());
        assertThat(rows.get(1).get("col0")).isInstanceOf(XmEntityDto.class);
        assertThat(rows.get(1).get("col1")).isEqualTo("B");
    }

    @Test
    public void sortParamIsRejectedForRaw() {
        assertThatThrownBy(() -> resource.searchByTemplatePost("ORDERS_SUMMARY", Map.of("typeKey", "RAW_T"), PageRequest.of(0, 10, Sort.by("name"))))
            .isInstanceOf(BusinessException.class).hasMessageContaining("sort");
    }
}
```

- [ ] **Step 2: Run to see it fail**

Run: `./gradlew test --tests '*XmEntityDbSearchRawTemplateResourceIntTest*' -x runCategorizedTests`
Expected: compilation failure.

- [ ] **Step 3: Implement**

`JpqlTemplateExecutor`, add dependency `EntityManager em` and:

```java
    public record RawResult(List<Map<String, Object>> rows, Long total) {
    }

    public RawResult executeRaw(JpqlTemplate template, Map<String, Object> params, Pageable pageable,
                                Function<Object, Object> entityToDto) {
        if (pageable != null && pageable.getSort().isSorted()) {
            throw new BusinessException(ERR_VALIDATION, "sort is not supported for RAW templates, use order by in the template");
        }
        Map<String, Object> bound = bindParams(template, params);

        TypedQuery<Tuple> query = em.createQuery(template.getQuery(), Tuple.class);
        bound.forEach((name, value) -> {
            if (referenced(query, name)) {
                query.setParameter(name, value);
            }
        });
        if (pageable != null && pageable.isPaged()) {
            query.setFirstResult((int) pageable.getOffset());
            query.setMaxResults(pageable.getPageSize());
        }
        List<Map<String, Object>> rows = query.getResultList().stream().map(t -> toRow(t, entityToDto)).toList();

        Long total = null;
        if (StringUtils.isNotBlank(template.getCountQuery())) {
            TypedQuery<Long> count = em.createQuery(template.getCountQuery(), Long.class);
            bound.forEach((name, value) -> {
                if (referenced(count, name)) {
                    count.setParameter(name, value);
                }
            });
            total = count.getSingleResult();
        }
        return new RawResult(rows, total);
    }

    private static boolean referenced(TypedQuery<?> query, String name) {
        return query.getParameters().stream().anyMatch(p -> name.equals(p.getName()));
    }

    private static Map<String, Object> toRow(Tuple tuple, Function<Object, Object> entityToDto) {
        Map<String, Object> row = new LinkedHashMap<>();
        List<TupleElement<?>> elements = tuple.getElements();
        for (int i = 0; i < elements.size(); i++) {
            String alias = elements.get(i).getAlias();
            String key = StringUtils.isBlank(alias) ? "col" + i : alias;
            row.put(key, entityToDto.apply(tuple.get(i)));
        }
        return row;
    }
```

Imports: `jakarta.persistence.EntityManager`, `jakarta.persistence.Tuple`, `jakarta.persistence.TupleElement`, `jakarta.persistence.TypedQuery`, `java.util.LinkedHashMap`, `java.util.List`, `java.util.function.Function`, `org.apache.commons.lang3.StringUtils`, `org.springframework.data.domain.Pageable`.

Service (`XmEntityDbSearchService`), LEP-covered like the other methods, no `@FindWithPermission` (RAW templates have no row-level wrapping):

```java
    @Transactional(readOnly = true)
    @LogicExtensionPoint(value = "SearchDbByRawTemplate", resolver = JpqlTemplateKeyResolver.class)
    public JpqlTemplateExecutor.RawResult searchByRawTemplate(JpqlTemplate template, Map<String, Object> params,
                                                              Pageable pageable, Function<Object, Object> entityToDto) {
        if (template.getType() != JpqlTemplateType.RAW) {
            throw new BusinessException(ERR_VALIDATION, "Template is not of type RAW: " + template.getKey());
        }
        return templateExecutor.executeRaw(template, params, pageable, entityToDto);
    }
```

Facade:

```java
    public JpqlTemplateExecutor.RawResult searchByRawTemplate(JpqlTemplate template, Map<String, Object> rawParams, Pageable pageable) {
        Map<String, Object> params = jpqlTemplatesService.coerce(template, rawParams);
        return xmEntityDbSearchService.searchByRawTemplate(template, params, pageable, value -> {
            if (value instanceof XmEntity xmEntity) {
                return xmEntityMapper.toDto(xmEntity);
            }
            if (value instanceof Link link) {
                return linkMapper.toDto(link);
            }
            return value;
        });
    }
```

(`XmEntity`, `Link` imports in the facade; the facade does not depend on the executor directly).

Resource `respondTemplate`, replace the body after `linkParams` computation:

```java
        if (template.getType() == JpqlTemplateType.RAW) {
            JpqlTemplateExecutor.RawResult result = facade.searchByRawTemplate(template, params, pageable);
            if (result.total() == null) {
                return ResponseEntity.ok(result.rows());
            }
            Page<Map<String, Object>> page = new PageImpl<>(result.rows(), pageable, result.total());
            HttpHeaders headers = PaginationUtil.generateDbSearchPaginationHttpHeaders(linkParams, page, url);
            return new ResponseEntity<>(result.rows(), headers, HttpStatus.OK);
        }
        Page<XmEntityDto> page = facade.searchByEntityTemplate(template, params, pageable, null);
        HttpHeaders headers = PaginationUtil.generateDbSearchPaginationHttpHeaders(linkParams, page, url);
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
```

Hibernate may return an entity tuple element with a `null` alias for `select e from ...`; the `col0` fallback covers it. If Hibernate assigns an auto alias for the entity (e.g. `e`), adjust the test expectation to `rows.get(0).values().iterator().next()` instead of `get("col0")`.

- [ ] **Step 4: Run the tests**

Run: `./gradlew test --tests '*XmEntityDbSearch*TemplateResourceIntTest*' -x runCategorizedTests`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/icthh/xm/ms/entity src/test/java/com/icthh/xm/ms/entity/web/rest/XmEntityDbSearchRawTemplateResourceIntTest.java
git commit -m "Add RAW JPQL templates returning projection rows"
```

---

### Task 13: Reindex of search_text for existing rows

**Files:**
- Create: `src/main/java/com/icthh/xm/ms/entity/service/search/db/XmEntitySearchTextReindexService.java`
- Modify: `src/main/java/com/icthh/xm/ms/entity/web/rest/XmEntityDbSearchResource.java`
- Test: `src/test/java/com/icthh/xm/ms/entity/service/search/db/XmEntitySearchTextReindexServiceIntTest.java`

**Interfaces:**
- Produces:
  ```java
  @Service public class XmEntitySearchTextReindexService {
      public static final int BATCH_SIZE = 500;
      /** typeKey null → all types with fullTextSearch: true. Returns processed count. Each batch is its own transaction. */
      public long reindex(String typeKey);
  }
  POST /api/_search-db/xm-entities/reindex?typeKey=   privilege XMENTITY.SEARCH.DB.REINDEX   → {"processed": n}
  ```
- Consumes: `XmEntityRepository.findAll(Specification<XmEntity>, Pageable)` and `saveAll`, `XmEntitySpecService.getTypeSpecs()`-like listing (find the method that returns all `TypeSpec`s for the current tenant: `grep -n "public .*List<TypeSpec>\|public .*Collection<TypeSpec>\|public .*Map<String, TypeSpec>" src/main/java/com/icthh/xm/ms/entity/service/XmEntitySpecService.java`), `TransactionTemplate`, listener from Task 6 (saving recomputes `searchText`).

- [ ] **Step 1: Write the failing test**

```java
package com.icthh.xm.ms.entity.service.search.db;

import static org.assertj.core.api.Assertions.assertThat;

import com.icthh.xm.ms.entity.AbstractPostgresIntTest;
import com.icthh.xm.ms.entity.domain.XmEntity;
import jakarta.persistence.EntityManager;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

public class XmEntitySearchTextReindexServiceIntTest extends AbstractPostgresIntTest {

    @Autowired private XmEntitySearchTextReindexService reindexService;
    @Autowired private TransactionTemplate tx;
    @Autowired private EntityManager em;

    private Long id;

    @BeforeEach
    public void seedWithoutSearchText() {
        pushDbSearchSpec();
        id = tx.execute(status -> {
            XmEntity e = newEntity("ORDER", "Legacy", Map.of("orderNo", 7));
            em.persist(e);
            em.flush();
            em.createNativeQuery("update xm_entity set search_text = null where id = :id").setParameter("id", e.getId()).executeUpdate();
            return e.getId();
        });
    }

    @AfterEach
    public void cleanup() {
        tx.executeWithoutResult(s -> em.createNativeQuery("delete from xm_entity where id = :id").setParameter("id", id).executeUpdate());
    }

    private String searchText() {
        return tx.execute(s -> (String) em.createNativeQuery("select search_text from xm_entity where id = :id").setParameter("id", id).getSingleResult());
    }

    @Test
    public void reindexFillsMissingSearchText() {
        assertThat(searchText()).isNull();

        long processed = reindexService.reindex("ORDER");

        assertThat(processed).isGreaterThanOrEqualTo(1);
        assertThat(searchText()).isEqualTo("Legacy\n7");
    }
}
```

- [ ] **Step 2: Run to see it fail**

Run: `./gradlew test --tests '*XmEntitySearchTextReindexServiceIntTest*' -x runCategorizedTests`
Expected: compilation failure.

- [ ] **Step 3: Implement**

```java
package com.icthh.xm.ms.entity.service.search.db;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.XmEntity_;
import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import com.icthh.xm.ms.entity.lep.keyresolver.TypeKeyResolver;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import com.icthh.xm.ms.entity.service.XmEntitySpecService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
@LepService(group = "service.entity.dbsearch")
@RequiredArgsConstructor
public class XmEntitySearchTextReindexService {

    public static final int BATCH_SIZE = 500;

    private final XmEntityRepository xmEntityRepository;
    private final XmEntitySpecService xmEntitySpecService;
    private final SearchTextBuilder searchTextBuilder;
    private final TransactionTemplate transactionTemplate;

    @LogicExtensionPoint(value = "ReindexSearchText", resolver = TypeKeyResolver.class)
    public long reindex(String typeKey) {
        List<String> typeKeys = typeKey != null ? List.of(typeKey) : enabledTypeKeys();
        long processed = 0;
        for (String key : typeKeys) {
            processed += reindexType(key);
        }
        return processed;
    }

    private long reindexType(String typeKey) {
        Specification<XmEntity> spec = (root, query, cb) -> cb.or(
            cb.equal(root.get(XmEntity_.typeKey), typeKey),
            cb.like(root.get(XmEntity_.typeKey), typeKey + ".%"));
        long processed = 0;
        int pageNumber = 0;
        while (true) {
            int current = pageNumber;
            Integer handled = transactionTemplate.execute(status -> {
                Page<XmEntity> page = xmEntityRepository.findAll(spec, PageRequest.of(current, BATCH_SIZE, Sort.by(XmEntity_.ID)));
                page.getContent().forEach(entity -> {
                    TypeSpec typeSpec = xmEntitySpecService.getTypeSpecByKeyWithoutFunctionFilter(entity.getTypeKey()).orElse(null);
                    entity.setSearchText(searchTextBuilder.build(typeSpec, entity));
                });
                xmEntityRepository.saveAll(page.getContent());
                return page.getNumberOfElements();
            });
            processed += handled == null ? 0 : handled;
            if (handled == null || handled < BATCH_SIZE) {
                break;
            }
            pageNumber++;
        }
        log.info("Reindexed search_text for {} entities of type {}", processed, typeKey);
        return processed;
    }

    private List<String> enabledTypeKeys() {
        return xmEntitySpecService.findAllTypes().stream()   // replace with the real "all TypeSpecs of tenant" method found via grep
            .filter(t -> Boolean.TRUE.equals(t.getFullTextSearch()))
            .map(TypeSpec::getKey)
            .toList();
    }
}
```

`reindex` is called through the Spring proxy from the resource, so the LEP interception applies (`TypeKeyResolver` returns no segment when `typeKey` is null, which selects the default LEP script).

Setting `searchText` explicitly before `saveAll` is defensive: `@PreUpdate` only fires for dirty entities, so an entity whose computed text equals the stored one is a no-op, and one whose stored text is null gets updated because the field changed.

Resource:

```java
    @PostMapping(value = "/_search-db/xm-entities/reindex", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasPermission({'typeKey': #typeKey}, 'XMENTITY.SEARCH.DB.REINDEX')")
    @PrivilegeDescription("Privilege to rebuild search_text of xm entities for DB full text search")
    public ResponseEntity<Map<String, Long>> reindex(@RequestParam(required = false) String typeKey) {
        return ResponseEntity.ok(Map.of("processed", reindexService.reindex(typeKey)));
    }
```

(add `XmEntitySearchTextReindexService reindexService` to the resource's constructor deps).

- [ ] **Step 4: Run the test**

Run: `./gradlew test --tests '*XmEntitySearchTextReindexServiceIntTest*' -x runCategorizedTests`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/icthh/xm/ms/entity src/test/java/com/icthh/xm/ms/entity/service/search/db/XmEntitySearchTextReindexServiceIntTest.java
git commit -m "Add search_text reindex endpoint for existing entities"
```

---

### Task 14: Privileges file, build wiring, full verification

**Files:**
- Modify: `src/test/resources/config/privileges/permissions.yml`
- Modify: `build.gradle` (only if new tests are excluded by the current `exclude` rules)
- Modify: `docs/superpowers/specs/2026-09-09-db-search-and-filtering-design.md` (record the deviations listed at the top of this plan)

- [ ] **Step 1: Add privilege keys to the test permissions file**

Find the `entity:` section and the role that holds `XMENTITY.SEARCH.QUERY` (`grep -n "XMENTITY.SEARCH.QUERY" src/test/resources/config/privileges/permissions.yml`). Next to it add entries with the same shape (`disabled: false`, `deleted: false`, `envCondition: null`, `resourceCondition: null`, `reactionStrategy: null`) for:

```
XMENTITY.SEARCH.DB
XMENTITY.SEARCH.DB.QUERY
XMENTITY.SEARCH.DB.TO_LINK
XMENTITY.SEARCH.DB.TEMPLATE
XMENTITY.SEARCH.DB.REINDEX
LINK.SEARCH.DB
LINK.SEARCH.DB.TARGETS
```

If a test compares the generated privileges against `expected-*.yml` files (`grep -rn "expected-privileges\|expected-permissions" src/test/java | head`), run that test and update the expected files with the new keys it reports.

- [ ] **Step 2: Check the test task picks up the new tests**

Run: `./gradlew test -x runCategorizedTests --tests 'com.icthh.xm.ms.entity.*' 2>&1 | tail -40`
Expected: all `*UnitTest`/`*IntTest` incl. the new Postgres ones pass. If Docker is unavailable on CI and the Postgres tests fail there, do NOT exclude them; report it and stop.

- [ ] **Step 3: Record deviations in the spec**

Append a section `## Implementation notes (2026-09-09)` to the spec with the four deviations from the plan header (JsonbCriteriaBuilder untouched, `contains` via cast, `:subjectRoleKey` dropped, RAW scalars unparsed) plus anything discovered during Tasks 2, 6, 11 (bean container fallback, `to_json_b` cast). Also list the LEP keys and resolvers: `SearchDb` (request typeKey), `SearchDbToLink` (entityTypeKey, linkTypeKey), `SearchDbTargets` (linkTypeKey), `SearchDbByEntityTemplate` / `SearchDbByRawTemplate` (template key), `ReindexSearchText` (typeKey).

- [ ] **Step 4: Full build**

Run: `./gradlew clean test`
Expected: BUILD SUCCESSFUL. Paste the summary line of the test report into the final message.

- [ ] **Step 5: Commit**

```bash
git add src/test/resources/config/privileges/permissions.yml docs/superpowers/specs/2026-09-09-db-search-and-filtering-design.md
git commit -m "Register DB search privileges and record implementation notes"
```
