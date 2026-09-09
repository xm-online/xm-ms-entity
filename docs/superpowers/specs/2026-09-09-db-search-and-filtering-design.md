# DB-backed search, filtering and JPQL templates for XmEntity

Date: 2026-09-09. Branch: `feature/entity_api`. Source requirements: `filtering.txt`.

## 1. Problem

`/_search-with-typekey/xm-entities`, `/_search-with-typekey-and-template/xm-entities` and
`/xm-entities/{entityTypeKey}/{idOrKey}/links/{linkTypeKey}/search` are Elasticsearch-only.
With `application.elastic-enabled: false` (the repo default) they throw
`UnsupportedOperationException`. Installations without Elasticsearch need equivalent
functionality backed by the relational DB (Postgres primary, Oracle fallback).

## 2. Decisions taken during brainstorming

| Topic | Decision |
|---|---|
| Full text storage | Nullable column `xm_entity.search_text`, filled by a JPA listener only for types with `fullTextSearch: true`. No side table. |
| Match semantics (Postgres) | Substring, case-insensitive: `ILIKE '%q%'` on `search_text` with a partial GIN `gin_trgm_ops` index (`pg_trgm`). Oracle: `lower(search_text) like lower('%q%')`, no index. |
| Execution mechanism | JPA Criteria `Specification`s built with the existing `JsonbCriteriaBuilder` / xm-commons `CustomExpression`; row-level permissions merged by parsing the xm-commons permission JPQL with Hibernate 7 `HibernateCriteriaBuilder.createQuery(hql, Class)`. |
| JPQL templates | WHERE-fragment only, alias `returnObject`, XmEntity results only, named params bound via `setParameter`. Resource-level privilege carries `templateKey` and params so tenants can restrict per template. |
| Privileges | New family `XMENTITY.SEARCH.DB.*`. Do not reuse `XMENTITY.SEARCH`. |
| typeKey matching | Default includes dotted subtypes (`ORDER`, `ORDER.*`). `includeSubTypes=false` matches the exact typeKey only. |
| Tests | No H2 emulation of json functions. All new integration tests run on Postgres via Testcontainers (`pg-test` profile). |
| Backfill | Admin endpoint `POST /_search-db/xm-entities/reindex` recomputes `search_text` for existing rows. |

## 3. API

All endpoints live in a new `XmEntityDbSearchResource` under `/api`, delegate through
`XmEntityDbSearchFacade` to `XmEntityDbSearchService`. Every list endpoint returns the DTO list
in the body and `X-Total-Count` + RFC 5988 `Link` headers, like the existing search endpoints.
`page`, `size`, `sort` are always query parameters, for GET and POST alike.

### 3.1 Search by typeKey with full text and filters

```
GET  /api/_search-db/xm-entities?typeKey=ORDER&includeSubTypes=true&query=text
        &data.order.in=1,2,3&stateKey.eq=ACTIVE&sort=data.order,desc&page=0&size=20
POST /api/_search-db/xm-entities?page=0&size=20&sort=data.order,desc
{
  "typeKey": "ORDER",
  "includeSubTypes": true,
  "query": "text",
  "filter": {
    "data.order.in": [1, 2, 3],
    "data.subObject.position.eq": 5,
    "name.contains": "abc",
    "startDate.gte": "2026-01-01T00:00:00Z"
  }
}
```

Returns `List<XmEntityDto>`. Resource privilege `XMENTITY.SEARCH.DB.QUERY`, resource map
`{typeKey, query, filter}`. Row-level privilege `@FindWithPermission("XMENTITY.SEARCH.DB")`.

`typeKey` is required. `query` and `filter` are optional. Soft-deleted rows (`removed = true`)
are excluded unless the filter contains `removed.eq=true`.

### 3.2 Filter grammar

Filter key: `<field>.<op>`. In GET requests the key is the query parameter name and `in`/`notIn`
values are comma-separated; in POST the value is the JSON value (array for `in`/`notIn`).

Fields:
- Column fields (whitelist): `id`, `key`, `typeKey`, `stateKey`, `name`, `description`,
  `startDate`, `updateDate`, `endDate`, `createdBy`, `updatedBy`, `removed`.
  Values are converted to the attribute Java type (`Long`, `Instant`, `Boolean`, `String`).
- Data fields: `data.<path>` where path is dot-separated, e.g. `data.subObject.position`.
  Translated to JSON path `$.subObject.position`.

Operators:

| op | column field | data field (Postgres) | data field (Oracle) |
|---|---|---|---|
| `eq`, `notEq` | `=` / `<>` | `json_query(data,path) = to_jsonb(:v)` | `json_value(data,path) = :v` (text) |
| `in`, `notIn` | `in (...)` | jsonb `in (to_jsonb(:v1), ...)` | text `in (...)` |
| `contains` | `lower(col) like lower('%v%')` | `jsonb_extract_path_text(...) ilike '%v%'` | `lower(json_value(...)) like` |
| `specified` | `is [not] null` | `json_query(...) is [not] null` | same |
| `gt`, `gte`, `lt`, `lte` | typed comparison | jsonb comparison (numeric for numbers) | text comparison (documented limitation) |

Value typing for data fields: POST keeps JSON types (number, string, boolean). GET values are
parsed as number, then boolean, then string. Unknown field, unknown op, unparsable value or
non-whitelisted sort property produce `400 ERR_VALIDATION`.

### 3.3 Sorting

`sort=<property>,<asc|desc>` repeated. `property` is a whitelisted column field or
`data.<path>`. `data.<path>` sorts by `json_query(returnObject.data, '$.<path>')` (jsonb order
on Postgres, text order on Oracle). Anything else is `400`.

### 3.4 Search entities for the "link add" dialog

```
GET|POST /api/_search-db/xm-entities/{entityTypeKey}/{idOrKey}/links/{linkTypeKey}
```

Same `query`, `filter`, `includeSubTypes`, paging and sort as 3.1 (no `typeKey`; the target
typeKey comes from `LinkSpec.typeKey`). When `LinkSpec.isUnique == true`, entities already
linked from the source with this link typeKey are excluded, and the source entity itself is
excluded. Returns `List<XmEntityDto>`. Resource privilege `XMENTITY.SEARCH.DB.TO_LINK`,
resource map `{entityTypeKey, idOrKey, linkTypeKey, query, filter}`. Row-level
`XMENTITY.SEARCH.DB`. Unknown entity typeKey / link typeKey: `400`.

### 3.5 Search links of a source entity

```
GET|POST /api/_search-db/xm-entities/{idOrKey}/targets/{linkTypeKey}
```

Returns `List<LinkDto>` (links where `source = entity` and `link.typeKey = linkTypeKey`),
consistent with the branch's `GET /xm-entities/{id}/targets/{typeKey}`. `query` and `filter`
apply to the link's target entity (`target.searchText`, `target.<column>`, `target.data.<path>`);
sort properties are also resolved on the target. Resource privilege
`LINK.SEARCH.DB.TARGETS`, resource map `{idOrKey, linkTypeKey, query, filter}`. Row-level
`@FindWithPermission("LINK.SEARCH.DB")` on `Link`.

### 3.6 JPQL templates

```
GET  /api/_search-db/xm-entities/template/{templateKey}?stateKey=ACTIVE&order=5&page=0&size=20&sort=name,asc
POST /api/_search-db/xm-entities/template/{templateKey}?page=0&size=20
{ "stateKey": "ACTIVE", "order": 5 }
```

Template definition, tenant config `/config/tenants/{tenant}/entity/jpql-templates.yml` and
`/config/tenants/{tenant}/entity/jpql-templates/*.yml` (merged, later files win on key clash,
clash logged as warning):

```yaml
templates:
  - key: ACTIVE_BY_ORDER
    query: "returnObject.stateKey = :stateKey and json_query(returnObject.data, '$.order') = to_json_b(:order)"
    params:            # optional, used to coerce GET string values
      order: number    # number | string | boolean | instant | list
```

Rules:
- Templates are HQL and may use the dialect functions xm-commons registers: `json_query(col, '$.path')`
  exists on both Postgres and Oracle; `to_json_b`, `to_json_b_text`, `jsonb_to_string` are
  Postgres-only, so a template using them fails on Oracle. Template authors own DB portability.
- The template is a JPQL WHERE fragment over alias `returnObject` (XmEntity). The service
  wraps it: `select returnObject from XmEntity returnObject where (<template>) and (<permission>)`.
- Parameters are bound with `setParameter`; there is no string substitution. Every `:name` in
  the template must be supplied, otherwise `400`. Extra supplied params are ignored.
- On GET, values come from query params (excluding `page`, `size`, `sort`) and are coerced by
  the `params` declaration, defaulting to string. On POST, the body is the params map.
- Sort is validated and translated as in 3.3 (property whitelist plus `data.<path>`).
- Unknown template: `404`.
- Resource privilege `XMENTITY.SEARCH.DB.TEMPLATE`, resource map `{templateKey, params}`.
  Row-level `XMENTITY.SEARCH.DB`.

Execution uses xm-commons `PermittedRepository.findByCondition(template, params, pageable,
XmEntity.class, privilegeKey)`; the translated sort is passed as a `Sort` whose property strings
are already valid JPQL expressions.

### 3.7 Reindex

```
POST /api/_search-db/xm-entities/reindex?typeKey=ORDER
```

Recomputes `search_text` for all entities of `typeKey` (and subtypes) in pages of 500, within
one transaction per page. Returns `{ "processed": <count> }`. Privilege
`XMENTITY.SEARCH.DB.REINDEX`. Without `typeKey`, processes every type that has
`fullTextSearch: true`.

## 4. Entity spec changes

`TypeSpec` gets two fields (schema is generated from the class, so tenant yml validation
picks them up automatically):

```yaml
types:
  - key: ORDER
    fullTextSearch: true
    fullTextSearchDataFields:
      - data.order
      - data.subObject.position
      - data.tags          # list of scalars: values joined with spaces
```

`fullTextSearchDataFields` entries may be written with or without the `data.` prefix.
Object-valued paths are skipped with a debug log. Inheritance: fields follow the existing
`SpecInheritanceProcessor` behaviour for scalar and list fields.

## 5. Components

```
web/rest/XmEntityDbSearchResource            REST, validation of paging, @PreAuthorize
web/rest/facade/XmEntityDbSearchFacade       DTO mapping, header generation
service/search/db/XmEntityDbSearchService    orchestration, @FindWithPermission, @LogicExtensionPoint
service/search/db/filter/FilterParser        "<field>.<op>" -> FilterCondition(field, op, values)
service/search/db/filter/XmEntityFilterSpecificationBuilder
                                             FilterCondition list -> Specification<XmEntity>
                                             (also builds Specification<Link> over target for 3.5)
service/search/db/filter/SortTranslator      Sort -> List<jakarta.persistence.criteria.Order> (Criteria)
                                             Sort -> Sort with JPQL expressions (templates)
service/search/db/dialect/JsonValueStrategy  Postgres / Oracle differences (value wrapping,
                                             text extraction). Chosen from spring.datasource.url,
                                             same rule as xm-commons JsonbExpression/OracleExpression.
config/jsonb/JsonbCriteriaBuilder            existing; refactor to depend on CustomExpression
repository/search/db/PermittedSpecificationRepository
                                             Specification + permission JPQL -> Page<T>
service/search/db/template/XmEntityJpqlTemplatesService
                                             RefreshableConfiguration for jpql-templates(.yml|/*.yml)
domain/listener/XmEntitySearchTextListener   @PrePersist/@PreUpdate -> XmEntity.searchText
service/search/db/SearchTextBuilder          TypeSpec + XmEntity -> String (null when disabled)
```

### 5.1 PermittedSpecificationRepository

```java
<T> Page<T> findAll(Class<T> entityClass, Specification<T> spec, Pageable pageable, String privilegeKey)
<T> long count(Class<T> entityClass, Specification<T> spec, String privilegeKey)
```

1. `condition = permissionCheckService.createCondition(authentication, privilegeKey, new SpelToJpqlTranslator())`
   (the same call xm-commons `PermittedRepository` makes).
2. `hql = "select returnObject from " + entityName + " returnObject" + (condition blank ? "" : " where " + condition)`.
3. `JpaCriteriaQuery<T> cq = ((HibernateCriteriaBuilder) em.getCriteriaBuilder()).createQuery(hql, entityClass)`.
4. `Root<T> root = cq.getRoots().iterator().next()`; `Predicate p = spec.toPredicate(root, cq, cb)`;
   `cq.where(cb.and(cq.getRestriction(), p))` (skip `and` when restriction is null).
5. `cq.orderBy(sortTranslator.toOrders(sort, root, cb))`.
6. Paging via `setFirstResult`/`setMaxResults`; count query built the same way with
   `select count(returnObject) ...`.

Spike first: an integration test on Postgres proving steps 3 and 4 work in Hibernate 7.3.1
(root reuse, metamodel paths, `cb.function` on the parsed root). If it fails, fallback is a
`SpelToPredicateTranslator` covering the subset xm-commons translates textually
(`#returnObject.<path>`, `==`, `&&`, `||`, `#subject.<field>` literals) and the design above
stays unchanged otherwise.

### 5.2 Search text

- New field `XmEntity.searchText` (`@Column(name = "search_text") @JsonIgnore`, not in DTO).
- `XmEntitySearchTextListener` registered in `@EntityListeners`. Preferred wiring: Spring's
  Hibernate `BeanContainer` integration so the listener gets constructor injection of
  `XmEntitySpecService` and `SearchTextBuilder`. If that does not work in this app context,
  use the existing static-setter injection pattern from `XmEntityElasticSearchListener`.
- `SearchTextBuilder.build(TypeSpec, XmEntity)`: `null` when `fullTextSearch` is not `true`;
  otherwise `name`, `description`, then each configured data value (scalars via `toString`,
  lists of scalars joined with space, objects skipped) joined with `\n`. Deterministic order.
- Predicate: `cb.ilike(root.get(XmEntity_.searchText), "%" + escape(q) + "%", '\\')`
  where `escape` escapes `\`, `%`, `_`. Hibernate renders `ilike` on Postgres and
  `lower(x) like lower(y)` on Oracle.

### 5.3 Liquibase

`20260909000000_add_search_text_to_xm_entity.xml`, included in `master.xml`:

1. `addColumn xm_entity.search_text` type `${textType}` (`varchar` on postgresql/h2, `text` on
   oracle; postgres `varchar` without length is unbounded).
2. Postgres only, `runInTransaction="false"`, `failOnError="false"`:
   `CREATE EXTENSION IF NOT EXISTS pg_trgm`.
3. Postgres only, precondition `sqlCheck expectedResult="1" select count(*) from pg_extension where extname='pg_trgm'`
   `onFail="MARK_RAN"`, `runInTransaction="false"`:
   `CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_xm_entity_search_text_trgm ON xm_entity USING gin (search_text gin_trgm_ops) WHERE search_text IS NOT NULL`.

Installs where the extension cannot be created keep working with sequential scans.

### 5.4 Privileges

Resource-level: `XMENTITY.SEARCH.DB.QUERY`, `XMENTITY.SEARCH.DB.TO_LINK`,
`LINK.SEARCH.DB.TARGETS`, `XMENTITY.SEARCH.DB.TEMPLATE`, `XMENTITY.SEARCH.DB.REINDEX`.
Row-level (`@FindWithPermission`): `XMENTITY.SEARCH.DB` on XmEntity, `LINK.SEARCH.DB` on Link.
All carry `@PrivilegeDescription`. Add them to `src/test/resources/config/privileges/permissions.yml`
alongside the new `*.BY_XM_ENTITY*` keys from this branch.

### 5.5 LEP

`XmEntityDbSearchService` methods carry `@LogicExtensionPoint` with `TypeKeyResolver`
(`SearchDb`, `SearchDbToLink`, `SearchDbTargets`) and a plain `SearchDbByTemplate`, mirroring the
Elasticsearch service methods.

## 6. Error handling

| Situation | Response |
|---|---|
| Unknown filter field / op, bad value type, non-whitelisted sort | `400 ERR_VALIDATION` with the offending key |
| Missing `typeKey` (3.1) | `400` |
| Unknown entity typeKey or link typeKey (3.4, 3.5) | `400` |
| Template not found | `404` |
| Template param missing | `400` with param name |
| Permission denied | `403` via existing `hasPermission` handling |

## 7. Testing

All integration tests use the `pg-test` profile with a `PostgreSQLContainer` as in
`JsonbCriteriaBuilderIntTest`. They must run in the default `test` task; if the "pipeline
problem" that excludes `JsonbCriteriaBuilderIntTest` still exists, fix or document it as part
of this work rather than excluding new tests.

- Unit: `FilterParser` (grammar, GET value parsing, errors), `SortTranslator` (whitelist,
  data paths), `SearchTextBuilder` (flag off, scalars, lists, objects, missing paths),
  `XmEntityJpqlTemplatesService` (merge of file and folder, param extraction).
- Integration (Postgres):
  - spike/proof for `PermittedSpecificationRepository` with a tenant `resourceCondition`;
  - 3.1: text query hits name/description/data field, misses when flag off; numeric `in`/`gt`
    on `data.order`; nested `data.subObject.position.eq`; `startDate.gte`; sort by `data.order`
    desc; `includeSubTypes` true/false; `removed` exclusion; headers;
  - 3.4: `isUnique` true excludes linked targets and self; false does not;
  - 3.5: links filtered by target `data.*` and `name.contains`;
  - 3.6: GET with coerced `number` param, POST with JSON types, missing param `400`,
    unknown template `404`, permission `resourceCondition` restricting `templateKey`;
  - 3.7: reindex fills `search_text` for pre-existing rows;
  - liquibase: column exists, index exists when `pg_trgm` available;
  - permission tests per endpoint in the style of `LinkResourcePermissionIntTest`.

## 8. Out of scope / follow-ups

- Existing Elasticsearch endpoints stay unchanged.
- `LinkSpec.max` enforcement (not implemented today).
- xm-commons `PermittedRepository.applyOrder` appends the raw `sort` property to JPQL without
  validation; this is an injection surface on every existing list endpoint and should be fixed
  in xm-commons. This work validates sort on its own endpoints only.
- `PaginationUtil` drops `sort` from generated `Link` headers; new endpoints will inherit this
  unless fixed in the same util.
- Oracle numeric comparison of JSON values is textual; a `json_value(... RETURNING NUMBER)`
  function registration in xm-commons `CustomOracleDialect` would fix it.
