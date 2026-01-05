//package com.icthh.xm.ms.entity.web.rest;
//
//import static org.mockito.Mockito.verifyNoInteractions;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
//import com.google.common.collect.ImmutableMap;
//import com.icthh.xm.commons.lep.api.LepManagementService;
//import com.icthh.xm.commons.tenant.TenantContextHolder;
//import com.icthh.xm.commons.tenant.TenantContextUtils;
//import com.icthh.xm.ms.entity.AbstractJupiterSpringBootTest;
//import com.icthh.xm.ms.entity.domain.XmEntity;
//import com.icthh.xm.ms.entity.lep.ElasticIndexManagerService;
//import com.icthh.xm.ms.entity.repository.XmEntityRepository;
//import com.icthh.xm.ms.entity.service.XmEntityService;
//import com.icthh.xm.ms.entity.service.XmEntitySpecService;
//import java.util.Date;
//import java.util.UUID;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.security.test.context.support.WithMockUser;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.test.web.servlet.setup.MockMvcBuilders;
//
//@ActiveProfiles("no-elastic")
//@WithMockUser(authorities = {"SUPER-ADMIN"})
//public class XmEntityNoElasticIntTest extends AbstractJupiterSpringBootTest {
//
//    @Autowired
//    private XmEntityResource xmEntityResource;
//
//    @Autowired
//    private XmEntityService xmEntityService;
//
//    @MockBean
//    private ElasticIndexManagerService elasticIndexManagerService;
//
//    @Autowired
//    private TenantContextHolder tenantContextHolder;
//
//    @Autowired
//    private LepManagementService lepManagementService;
//
//    @Autowired
//    private XmEntitySpecService xmEntitySpecService;
//
//    private MockMvc restXmEntityMockMvc;
//
//    private XmEntity xmEntity;
//
//    @BeforeEach
//    public void setup() {
//        TenantContextUtils.setTenant(tenantContextHolder, "RESINTTEST");
//        lepManagementService.beginThreadContext();
//
//        this.restXmEntityMockMvc = MockMvcBuilders.standaloneSetup(xmEntityResource).build();
//
//        xmEntity =  xmEntityService.save(new XmEntity().typeKey("TEST_NO_PROCESSING_REFS").name("someName").key("somKey"));
//
//    }
//
//    @Test
//    public void testUpdateXmEntityDoesNotCallElasticWhenDisabled() throws Exception {
//        xmEntity.setName("Updated Name");
//
//        restXmEntityMockMvc.perform(put("/api/xm-entities")
//                        .contentType(TestUtil.APPLICATION_JSON_UTF8)
//                        .content(TestUtil.convertObjectToJsonBytes(xmEntity)))
//                .andExpect(status().isOk());
//
//        // Verify that the listener did not call the elastic service
//        verifyNoInteractions(elasticIndexManagerService);
//    }
//
//    private XmEntity createEntity(String typeKey) {
//        XmEntity entity = new XmEntity();
//        entity.setName("Name");
//        entity.setTypeKey(typeKey);
//        entity.setStartDate(new Date().toInstant());
//        entity.setUpdateDate(new Date().toInstant());
//        entity.setKey(UUID.randomUUID().toString());
//        entity.setStateKey("STATE1");
//        entity.setData(ImmutableMap.<String, Object>builder()
//                .put("AAAAAAAAAA", "BBBBBBBBBB").build());
//        return entity;
//    }
//}
