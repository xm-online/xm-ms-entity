# DTO/Facade Refactoring - Task Summary

## Goal
Introduce DTOs and Facade layer for REST Resources so the API returns DTOs instead of JPA entities directly.

## What Was Done

### Step 1: DTO Classes Created (13 files)
**Location:** `src/main/java/com/icthh/xm/ms/entity/service/dto/`
- XmEntityDto, AttachmentDto, CalendarDto, CommentDto, ContentDto, EventDto, FunctionContextDto, LinkDto, LocationDto, ProfileDto, RatingDto, TagDto, VoteDto
- Mirror entity fields exactly, keep validation + swagger annotations, no JPA annotations

### Step 2: MapStruct Mappers Created (14 files)
**Location:** `src/main/java/com/icthh/xm/ms/entity/service/mapper/`
- LazyLoadingAwareMapper (base utility with `Hibernate.isInitialized()` checks)
- ContentMapper, TagMapper, LocationMapper, VoteMapper, CommentMapper, FunctionContextMapper, AttachmentMapper, RatingMapper, EventMapper, CalendarMapper, LinkMapper, ProfileMapper, XmEntityMapper
- All lazy collections guarded with `conditionExpression = "java(org.hibernate.Hibernate.isInitialized(...))"`
- Circular refs (XmEntity ↔ Link) handled via `@Named("shallowXmEntityToDto")` in LazyLoadingAwareMapper and `@Named("targetXmEntityToDto")` in LinkMapper

### Step 3: Facade Classes Created (13 files)
**Location:** `src/main/java/com/icthh/xm/ms/entity/web/rest/facade/`
- XmEntityFacade (includes search methods, link operations, state updates, export)
- AttachmentFacade, CalendarFacade, CommentFacade, ContentFacade, EventFacade, FunctionContextFacade, LinkFacade, LocationFacade, ProfileFacade, RatingFacade, TagFacade, VoteFacade

### Step 4: Resource Classes Updated (15 files)
**Location:** `src/main/java/com/icthh/xm/ms/entity/web/rest/`
- All CRUD resources: replaced Service→Facade, Entity→Dto in method signatures
- XmEntitySearchResource: uses XmEntityFacade for search operations
- XmEntitySpecResource: maps generateXmEntity result to XmEntityDto

### Step 5: Swagger Annotations Removed from JPA Entities (13 files)
**Location:** `src/main/java/com/icthh/xm/ms/entity/domain/`
- Removed @ApiModel, @ApiModelProperty, and `import io.swagger.annotations.*` from all entity files

---

## KNOWN ISSUES - Must Fix for Backward Compatibility

### CRITICAL: Jackson Serialization on DTOs

The following Jackson annotations on JPA entities ensure backward-compatible JSON serialization. The DTOs currently do NOT replicate this behavior correctly, which **will break the API contract**.

#### Issue 1: `SimpleLinkSerializer` on Link and XmEntity collections

**Entity code (Link.java):**
```java
@JsonSerialize(using = SimpleLinkSerializer.class)
public class Link implements Serializable { ... }
```

**Entity code (XmEntity.java):**
```java
@JsonSerialize(contentUsing = SimpleLinkSerializer.class)
private Set<Link> targets = new HashSet<>();
```

`SimpleLinkSerializer` is typed to `Link` entity (extends `JsonObjectSerializer<Link>`). It serializes Link with a flattened target object containing only core XmEntity fields (id, key, typeKey, stateKey, name, dates, avatarUrl, description, data, createdBy, removed) and source as just an ID.

**Problem:** `LinkDto` and `XmEntityDto` cannot use `SimpleLinkSerializer` directly because it's typed to `Link` entity, not `LinkDto`. The DTOs currently have NO `@JsonSerialize` annotation.

**Fix needed:** Create a `SimpleLinkDtoSerializer extends JsonObjectSerializer<LinkDto>` that produces the same JSON output, and annotate `LinkDto` with `@JsonSerialize(using = SimpleLinkDtoSerializer.class)` and `XmEntityDto.targets`/`XmEntityDto.sources` with `@JsonSerialize(contentUsing = SimpleLinkDtoSerializer.class)`.

#### Issue 2: `@JsonIdentityInfo` + `@JsonIdentityReference` with `XmEntityObjectIdResolver`

**Entity code (e.g., Attachment.java):**
```java
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id", resolver = XmEntityObjectIdResolver.class)
@JsonIdentityReference(alwaysAsId = true)
private XmEntity xmEntity;
```

`XmEntityObjectIdResolver` resolves an XmEntity ID from the database using `XmEntityRepository`. On deserialization, when the frontend sends `"xmEntity": 123`, the resolver loads the full `XmEntity` JPA entity from DB.

**Problem:** The DTOs use `XmEntityDto xmEntity` with the same annotations pointing to `XmEntityObjectIdResolver`. But `XmEntityObjectIdResolver` returns an `XmEntity` (JPA entity), not an `XmEntityDto`. Jackson will fail or produce wrong type during deserialization.

**Fix needed:** Create `XmEntityDtoObjectIdResolver` that returns an `XmEntityDto` with only `id` set (no DB lookup needed for DTO — the facade will handle the actual entity lookup). Same for `CalendarDtoObjectIdResolver`. Then update the `@JsonIdentityInfo` resolver references on DTO fields.

#### Issue 3: `SimpleXmEntitySerializer`

If any code uses `SimpleXmEntitySerializer` (typed to `XmEntity`), a DTO variant may also be needed.

---

## Remaining Work

### Must Do (Next Iteration)
1. **Create `SimpleLinkDtoSerializer`** — DTO-aware variant of `SimpleLinkSerializer`
2. **Create `XmEntityDtoObjectIdResolver`** — returns `XmEntityDto` with only id set
3. **Create `CalendarDtoObjectIdResolver`** — same pattern for CalendarDto
4. **Add `@JsonSerialize` to `LinkDto` and `XmEntityDto`** fields (targets/sources)
5. **Update `@JsonIdentityInfo` resolver** on all DTO xmEntity/calendar fields
6. **Update tests** — REST layer tests need to use DTOs
7. **Build & verify** — needs Java 21 (local JDK is 25, Gradle 8.9 doesn't support it)

### XmEntityResource Still Uses Domain Entities
- `FunctionContext` — from `XmEntityFunctionServiceFacade.execute()`. Would need a `FunctionContextDto` mapping in that facade too.
- `Profile` — used in `produceEvent()` helper, only accesses `profile.getId()`.

### Test Updates Needed
All REST layer tests (integration tests, MockMvc tests) that create/assert JPA entities in request/response bodies need to be updated to use DTOs instead.

---

## File Inventory

### New Files (~40)
- 13 DTOs in `service/dto/`
- 14 Mappers in `service/mapper/` (including LazyLoadingAwareMapper)
- 13 Facades in `web/rest/facade/`

### Modified Files (~28)
- 15 Resources in `web/rest/`
- 13 Entities in `domain/` (swagger annotation removal)
