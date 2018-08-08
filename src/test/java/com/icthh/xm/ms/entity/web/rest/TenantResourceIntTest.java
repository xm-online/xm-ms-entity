package com.icthh.xm.ms.entity.web.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.icthh.xm.commons.i18n.error.web.ExceptionTranslator;
import com.icthh.xm.commons.gen.api.TenantsApi;
import com.icthh.xm.commons.gen.api.TenantsApiController;
import com.icthh.xm.commons.gen.model.Tenant;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.tenant.spring.config.TenantContextConfiguration;
import com.icthh.xm.ms.entity.EntityApp;
import com.icthh.xm.ms.entity.config.DatabaseConfiguration;
import com.icthh.xm.ms.entity.config.SecurityBeanOverrideConfiguration;
import com.icthh.xm.ms.entity.config.tenant.WebappTenantOverrideConfiguration;
import com.icthh.xm.ms.entity.service.TenantService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.junit4.SpringRunner;
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
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
    EntityApp.class,
    SecurityBeanOverrideConfiguration.class,
    WebappTenantOverrideConfiguration.class,
    TenantContextConfiguration.class,
    DatabaseConfiguration.class
})
public class TenantResourceIntTest {

    private static final String TENANT_SUPER = "XM";
    private static final String TENANT_NEW = "SAMARA";
    private static final String API_ENDPOINT = "/tenants/";

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private TenantContextHolder tenantContextHolder;

    private MockMvc mockMvc;

    @BeforeTransaction
    public void beforeTransaction() {
        TenantContextUtils.setTenant(tenantContextHolder, TENANT_SUPER);
    }

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        TenantsApi controller = new TenantsApiController(new TenantResource(tenantService));
        this.mockMvc = MockMvcBuilders
            .standaloneSetup(controller)
            .setControllerAdvice(exceptionTranslator)
            .setMessageConverters(jacksonMessageConverter)
            .build();
    }

    @After
    @Override
    public void finalize() {
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
        mockMvc
            .perform(post(API_ENDPOINT)
                         .contentType(TestUtil.APPLICATION_JSON_UTF8)
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
