package com.icthh.xm.ms.entity.web.rest;

import com.icthh.xm.commons.errors.ExceptionTranslator;
import com.icthh.xm.commons.gen.api.TenantsApi;
import com.icthh.xm.commons.gen.api.TenantsApiController;
import com.icthh.xm.commons.gen.model.Tenant;
import com.icthh.xm.ms.entity.EntityApp;
import com.icthh.xm.ms.entity.config.SecurityBeanOverrideConfiguration;
import com.icthh.xm.ms.entity.config.tenant.TenantContext;
import com.icthh.xm.ms.entity.config.tenant.WebappTenantOverrideConfiguration;
import com.icthh.xm.ms.entity.service.TenantService;
import com.icthh.xm.ms.entity.service.tenant.TenantDatabaseService;
import com.icthh.xm.ms.entity.service.tenant.TenantElasticService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test class for the TenantResource REST controller.
 *
 * @see TenantResource
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {EntityApp.class, SecurityBeanOverrideConfiguration.class, WebappTenantOverrideConfiguration.class})
public class TenantResourceIntTest {

    private static final String TENANT_SUPER = "XM";
    private static final String TENANT_NEW = "SAMARA";
    private static final String API_ENDPOINT = "/tenants/";

    @Autowired
    private TenantDatabaseService tenantDatabaseService;

    @Autowired
    private TenantElasticService tenantElasticService;

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Autowired
    private DataSource dataSource;

    private MockMvc mockMvc;

    private Tenant tenant;

    private List<String> tenants = new ArrayList<>();

    @Autowired
    private TenantService tenantService;

    @Before
    public void setup() {
        TenantContext.setCurrent(TENANT_NEW);

        MockitoAnnotations.initMocks(this);
        tenant = new Tenant();
        TenantsApi controller = new TenantsApiController(new TenantResource(tenantService));
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(exceptionTranslator).setMessageConverters(jacksonMessageConverter).build();
    }

    @Test
    @Transactional
    public void createDeleteTenant() throws Exception {
        Connection connection = dataSource.getConnection();
        int countXm = countTables(connection, TENANT_SUPER);

        tenant.setTenantKey(TENANT_NEW);

        // Create tenant
        mockMvc.perform(post(API_ENDPOINT).contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(tenant)))
            .andExpect(status().isOk());

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
        assertEquals(countXm, countTables(connection, TENANT_NEW));

        // delete tenant
        mockMvc.perform(delete(API_ENDPOINT + "{tenantKey}", TENANT_NEW).accept(TestUtil.APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());

        // Check schema deleted
        ResultSet schemasNew = connection.getMetaData().getSchemas();
        boolean foundDeleted = false;
        while (schemasNew.next()) {
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

    @After
    public void finalize() {
        TenantContext.setCurrent(TENANT_SUPER);
    }
}
