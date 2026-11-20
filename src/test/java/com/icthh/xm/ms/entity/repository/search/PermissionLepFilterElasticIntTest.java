package com.icthh.xm.ms.entity.repository.search;

import com.icthh.xm.commons.lep.XmLepScriptConfigServerResourceLoader;
import com.icthh.xm.commons.permission.access.subject.Subject;
import com.icthh.xm.commons.permission.service.PermissionLepConditionService;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.ms.entity.AbstractSpringBootTest;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.repository.search.translator.SpelToElasticTranslator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;

import java.util.Map;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_AUTH_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Walks the whole query path exactly as {@code MultiRoleStrategy.createCondition} does - resolve
 * {@code #lep.filter(...)} against a real Groovy script, then translate the condition into
 * Elasticsearch syntax - so it shows the fragment a tenant script returns really does reach the
 * query, which neither a commons unit test nor a mocked repository test can demonstrate.
 */
public class PermissionLepFilterElasticIntTest extends AbstractSpringBootTest {

    private static final String SEGMENT = "elastic-search-test";

    private static final String PRIVILEGE = "XMENTITY.SEARCH";

    private static final String ROLE = "ROLE_B2B_EXPERT";

    private static final String SCRIPT_PATH =
        "/config/tenants/RESINTTEST/entity/lep/service/permission/Filter$$" + SEGMENT + "$$tenant.groovy";

    private static final Subject SUBJECT = new Subject("jsmith", "user-key-1", ROLE);

    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Autowired
    private XmAuthenticationContextHolder authContextHolder;

    @Autowired
    private LepManager lepManager;

    @Autowired
    private XmLepScriptConfigServerResourceLoader leps;

    @Autowired
    private PermissionLepConditionService permissionLepConditionService;

    @Autowired
    private SpelToElasticTranslator spelToElasticTranslator;

    @Before
    public void setup() {
        TenantContextUtils.setTenant(tenantContextHolder, "RESINTTEST");
        lepManager.beginThreadContext(ctx -> {
            ctx.setValue(THREAD_CONTEXT_KEY_TENANT_CONTEXT, tenantContextHolder.getContext());
            ctx.setValue(THREAD_CONTEXT_KEY_AUTH_CONTEXT, authContextHolder.getContext());
        });
    }

    @After
    public void tearDown() {
        leps.onRefresh(SCRIPT_PATH, null);
        lepManager.endThreadContext();
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
    }

    private String permittedQuery(String condition, Map<String, Object> contextParams) {
        String resolved = permissionLepConditionService.applyFilter(condition, PRIVILEGE, ROLE, SUBJECT,
            contextParams);
        return spelToElasticTranslator.translate(resolved, SUBJECT);
    }

    @Test
    public void scriptFragmentReachesTheElasticsearchQuery() {
        // the subject travels inside contextParams - filter() itself takes no subject argument
        leps.onRefresh(SCRIPT_PATH,
            "return 'data.owner:\"' + lepContext.inArgs.contextParams.subject.userKey + '\"'");

        String query = permittedQuery("#returnObject.typeKey == \"LEAD\" && #lep.filter('" + SEGMENT + "')",
            Map.of("query", "typeKey:LEAD", "entityClass", XmEntity.class));

        assertThat(query).isEqualTo("typeKey : \"LEAD\"  AND  (data.owner:\"user-key-1\")");
    }

    @Test
    public void scriptSeesTheSearchContextAndItsOwnParams() {
        leps.onRefresh(SCRIPT_PATH, "def args = lepContext.inArgs\n"
            + "return 'q:\"' + args.contextParams.query + '\" AND t:\"' + args.params.typeKey + '\"'");

        String query = permittedQuery("#lep.filter('" + SEGMENT + "', {typeKey: 'LEAD'})",
            Map.of("query", "typeKey:LEAD", "entityClass", XmEntity.class));

        assertThat(query).isEqualTo("(q:\"typeKey:LEAD\" AND t:\"LEAD\")");
    }

    @Test
    public void subjectParamsAreEvaluatedLiveBeforeTheScriptRuns() {
        leps.onRefresh(SCRIPT_PATH, "return 'owner:\"' + lepContext.inArgs.params.userKey + '\"'");

        String query = permittedQuery("#lep.filter('" + SEGMENT + "', {userKey: #subject.userKey})", Map.of());

        assertThat(query).isEqualTo("(owner:\"user-key-1\")");
    }

    @Test
    public void missingScriptDeniesInsteadOfWideningTheQuery() {
        assertThatThrownBy(() -> permittedQuery("#lep.filter('no-such-segment')", Map.of()))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    public void conditionWithoutMarkerIsUnaffected() {
        String query = permittedQuery("#returnObject.createdBy == #subject.userKey", Map.of());

        assertThat(query).isEqualTo("createdBy : \"user-key-1\"");
    }
}
