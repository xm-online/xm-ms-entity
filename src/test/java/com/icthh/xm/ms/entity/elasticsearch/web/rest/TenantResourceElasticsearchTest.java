package com.icthh.xm.ms.entity.elasticsearch.web.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.icthh.xm.commons.i18n.error.web.ExceptionTranslator;
import com.icthh.xm.commons.gen.api.TenantsApi;
import com.icthh.xm.commons.gen.api.TenantsApiController;
import com.icthh.xm.commons.gen.model.Tenant;
import com.icthh.xm.commons.lep.api.LepEngineSession;
import com.icthh.xm.commons.lep.api.LepManagementService;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.tenantendpoint.TenantManager;
import com.icthh.xm.ms.entity.elasticsearch.AbstractElasticSpringBootTest;
import com.icthh.xm.ms.entity.web.rest.TenantResource;
import com.icthh.xm.ms.entity.web.rest.TestUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;

/**
 * Test class for the TenantResource REST controller.
 *
 * @see TenantResource
 */
public class TenantResourceElasticsearchTest extends AbstractElasticSpringBootTest {

    private static final String TENANT_SUPER = "XM";
    private static final String TENANT_NEW = "SAMARA";
    private static final String API_ENDPOINT = "/api/tenants/";

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private TenantManager tenantManager;

    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Autowired
    private LepManagementService lepManager;

    private MockMvc mockMvc;

    @BeforeTransaction
    public void beforeTransaction() {
        TenantContextUtils.setTenant(tenantContextHolder, TENANT_SUPER);
    }

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        TenantsApi controller = new TenantsApiController(new TenantResource(tenantManager));
        this.mockMvc = MockMvcBuilders
            .standaloneSetup(controller)
            .setControllerAdvice(exceptionTranslator)
            .setMessageConverters(jacksonMessageConverter)
            .build();
    }

    @AfterEach
    public void tearDown() {
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
    }

    @Test
    @Transactional
    public void createDeleteTenant() throws Exception {
        Connection connection = dataSource.getConnection();
        //int countXm = countTables(connection, TENANT_SUPER);

        Tenant tenant = new Tenant();
        tenant.setTenantKey(TENANT_NEW);

        // Create tenant
        tenantContextHolder.getPrivilegedContext().execute(TenantContextUtils.buildTenant(TENANT_SUPER), () -> {
            try (LepEngineSession context = lepManager.beginThreadContext()) {
                mockMvc
                    .perform(post(API_ENDPOINT)
                        .contentType(TestUtil.APPLICATION_JSON_UTF8)
                        .content(TestUtil.convertObjectToJsonBytes(tenant)))
                    .andExpect(status().isOk());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        // Check schema created
        ResultSet schemas = connection.getMetaData().getSchemas();
        boolean found = false;
        while (schemas.next()) {
            if (TENANT_NEW.equals(schemas.getString(1))) {
                found = true;
            }
        }

        assertEquals(true, found);

        // Check tables created
        assertTrue(countTables(connection, TENANT_NEW) > 1);

        // delete tenant
        mockMvc.perform(delete(API_ENDPOINT + "{tenantKey}", TENANT_NEW).accept(TestUtil.APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());

        // Check schema deleted
        ResultSet schemasNew = connection.getMetaData().getSchemas();
        boolean foundDeleted = false;
        while (!foundDeleted && schemasNew.next()) {
            if (TENANT_NEW.equals(schemasNew.getString(1))) {
                foundDeleted = true;
            }
        }
        assertEquals(false, foundDeleted);
    }

    private static int countTables(Connection con, String schema) throws SQLException {
        ResultSet tables = con.getMetaData().getTables(null, schema, null, null);
        int count = 0;
        while (tables.next()) {
            count++;
        }
        return count;
    }

}
