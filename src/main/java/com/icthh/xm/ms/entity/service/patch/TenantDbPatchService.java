package com.icthh.xm.ms.entity.service.patch;

import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.ms.entity.service.patch.model.XmTenantPatch;
import com.icthh.xm.ms.entity.service.patch.model.XmTenantPatchType;
import com.icthh.xm.ms.entity.service.patch.model.XmTenantPatchValidationException;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ResourceAccessor;
import liquibase.sdk.resource.MockResourceAccessor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import javax.sql.DataSource;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.io.InputStream;
import java.io.StringWriter;
import java.sql.Connection;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.stream;
import static java.util.Comparator.comparing;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang.text.StrSubstitutor.replace;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Slf4j
@Component
public class TenantDbPatchService implements RefreshableConfiguration {

    private final static String PATCH_PATH = "/config/tenants/{tenantName}/entity/dbPatches/{filename}.yml";
    public static final String XM_ENTITY_TABLE_NAME = "xm_entity";

    private final Map<String, Map<String, TenantDbPatch>> configuration = new ConcurrentHashMap<>();
    private final AntPathMatcher matcher = new AntPathMatcher();
    private final MultiTenantConnectionProvider multiTenantConnectionProvider;
    private final String liquibaseTemplate;
    private final ObjectMapper objectMapper;

    @SneakyThrows
    public TenantDbPatchService(MultiTenantConnectionProvider multiTenantConnectionProvider) {
        this.multiTenantConnectionProvider = multiTenantConnectionProvider;
        InputStream liquibaseTemplateResource = getClass().getClassLoader().getResourceAsStream("config/liquibase-patch-template.xml");
        this.liquibaseTemplate = IOUtils.toString(liquibaseTemplateResource, UTF_8);

        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        objectMapper.setVisibility(PropertyAccessor.ALL, NONE);
        objectMapper.setVisibility(PropertyAccessor.FIELD, ANY);
        stream(XmTenantPatchType.values())
                .map(it -> new NamedType(it.getImplementationClass(), it.name()))
                .forEach(objectMapper::registerSubtypes);
        this.objectMapper = objectMapper;
    }

    public void onRefresh(final String updatedKey, final String config) {
        try {
            Map<String, String> templateVariables = this.matcher.extractUriTemplateVariables(PATCH_PATH, updatedKey);
            String tenant = templateVariables.get("tenantName");

            if (isBlank(config)) {
                var patches = this.configuration.get(tenant);
                if (MapUtils.isNotEmpty(patches)) {
                    patches.remove(updatedKey);
                }
                return;
            }

            var patches = this.configuration.computeIfAbsent(tenant, it -> new ConcurrentHashMap<>());
            String filename = templateVariables.get("filename");
            patches.put(updatedKey, new TenantDbPatch(filename, config));

            log.info("Configuration was updated for tenant [{}] by key [{}]", tenant, updatedKey);
        } catch (Exception e) {
            log.error("Error read configuration from path " + updatedKey, e);
        }
    }

    public boolean isListeningConfiguration(final String updatedKey) {
        return this.matcher.match(PATCH_PATH, updatedKey);
    }

    @Override
    @SneakyThrows
    public void refreshFinished(Collection<String> paths) {
        Set<String> tenants = paths.stream().map(this::toTenantKey).collect(Collectors.toSet());
        tenants.forEach(this::applyPath);
    }

    private void applyPath(String tenant) {
        StopWatch stopWatch = StopWatch.createStarted();
        log.info("Start apply patch for tenant {}", tenant);
        try (Connection connection = multiTenantConnectionProvider.getConnection(tenant)) {
            Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
            database.setDefaultSchemaName(tenant.toLowerCase());

            var patchesMap = this.configuration.getOrDefault(tenant, Map.of());
            List<TenantDbPatch> patches = patchesMap.values().stream().sorted(comparing(TenantDbPatch::getFilename)).collect(toList());
            LinkedHashMap<String, String> content = toLiquibaseContent(tenant, patches);

            log.info("Start apply liquibase change sets for tenant {} | {}ms", tenant, stopWatch.getTime(MILLISECONDS));

            for (TenantDbPatch patch : patches) {
                log.info("Start apply {} | {}ms", patch.filename, stopWatch.getTime(MILLISECONDS));
                ResourceAccessor resourceAccessor = new MockResourceAccessor(content);
                Liquibase liquibase = new Liquibase(patch.filename + ".xml", resourceAccessor, database);
                liquibase.update(new Contexts(tenant), new LabelExpression());
                log.info("{} applied finished | {}ms", patch.filename, stopWatch.getTime(MILLISECONDS));
            }

            log.info("Patch applied for tenant {}, {}ms", tenant, stopWatch.getTime(MILLISECONDS));
        } catch (Exception e) {
            log.error("Error during apply tenant db patch", e);
        }
    }

    protected LinkedHashMap<String, String> toLiquibaseContent(String tenant, List<TenantDbPatch> patches) {
        LinkedHashMap<String, String> content = new LinkedHashMap<>();
        for (TenantDbPatch patch : patches) {
            XmTenantPatch xmTenantPatch = readPatch(patch.patchContent);
            validate(xmTenantPatch, patch.filename);
            String patchContent = xmTenantPatch.liquibaseRepresentation(tenant);
            String liquibase = replace(liquibaseTemplate, Map.of("patchContent", patchContent));
            log.info("Patch file {} transformed to {}", patch.filename, liquibase);
            content.put(patch.filename + ".xml", liquibase);
        }
        return content;
    }

    protected void validate(XmTenantPatch xmTenantPatch, String filename) {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            var violations = validator.validate(xmTenantPatch);
            if (!violations.isEmpty()) {
                log.error("Error patch validation {} | {}", filename, violations);
                throw new XmTenantPatchValidationException(violations);
            }
        }
    }

    private String toTenantKey(String it) {
        return matcher.extractUriTemplateVariables(PATCH_PATH, it).get("tenantName");
    }

    @SneakyThrows
    public XmTenantPatch readPatch(String patch) {
        return objectMapper.readValue(patch, XmTenantPatch.class);
    }

    @Getter
    @RequiredArgsConstructor
    public static class TenantDbPatch {
        private final String filename;
        private final String patchContent;
    }

}
