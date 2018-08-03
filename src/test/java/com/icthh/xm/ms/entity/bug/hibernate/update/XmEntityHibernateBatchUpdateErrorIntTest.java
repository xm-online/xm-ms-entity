package com.icthh.xm.ms.entity.bug.hibernate.update;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_AUTH_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.icthh.xm.commons.lep.XmLepScriptConfigServerResourceLoader;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.ms.entity.EntityApp;
import com.icthh.xm.ms.entity.config.LepConfiguration;
import com.icthh.xm.ms.entity.config.SecurityBeanOverrideConfiguration;
import com.icthh.xm.ms.entity.config.TestConfigConstants;
import com.icthh.xm.ms.entity.config.tenant.WebappTenantOverrideConfiguration;
import com.icthh.xm.ms.entity.domain.Link;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.service.LinkService;
import com.icthh.xm.ms.entity.service.XmEntityService;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.jpa.domain.Specifications;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Slf4j
@RunWith(SpringRunner.class)
@WithMockUser(authorities = {"SUPER-ADMIN"})
@SpringBootTest(classes = {
    EntityApp.class,
    SecurityBeanOverrideConfiguration.class,
    WebappTenantOverrideConfiguration.class,
    XmEntityHibernateBatchUpdateErrorConfigOverride.class,
    LepConfiguration.class
})
public class XmEntityHibernateBatchUpdateErrorIntTest {

    @Autowired
    private XmEntityService xmEntityServiceImpl;

    @Autowired
    private EntityManager em;

    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Autowired
    private XmAuthenticationContextHolder authContextHolder;

    @Autowired
    private LepManager lepManager;

    @Autowired
    private XmLepScriptConfigServerResourceLoader leps;

    @Autowired
    private LinkService linkService;

    @BeforeTransaction
    public void beforeTransaction() {
        TenantContextUtils.setTenant(tenantContextHolder, TestConfigConstants.TENANT_AEBUGHBU);
    }

    @Before
    public void setup() {
        lepManager.beginThreadContext(ctx -> {
            ctx.setValue(THREAD_CONTEXT_KEY_TENANT_CONTEXT, tenantContextHolder.getContext());
            ctx.setValue(THREAD_CONTEXT_KEY_AUTH_CONTEXT, authContextHolder.getContext());
        });

        buildTestData();
    }

    private String productKey;

    private void buildTestData() {
        XmEntity car = xmEntityServiceImpl.save(buildAeCar());
        em.flush();

        XmEntity product = buildProduct();
        product = xmEntityServiceImpl.save(product);
        productKey = product.getKey();
        em.flush();

        Link link = linkProductToCar(product, car);
        link = linkService.save(link);
        em.flush();
    }

    @After
    @Override
    public void finalize() {
        lepManager.endThreadContext();
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
    }

    @Test
    @Transactional
    public void updateProductStateWithLinkedCarAndGetException() {
        initLepScripts();

        // only state key is used from XmEntity
        XmEntity rentProduct = new XmEntity().stateKey("RENT_STARTED");
        xmEntityServiceImpl.updateState(productKey, rentProduct);
    }

    private void initLepScripts() {
        String pattern = "/config/tenants/" + TestConfigConstants.TENANT_AEBUGHBU + "/entity/lep/";

        String[] testLeps = new String[]{
            "lifecycle/ChangeState$$around.groovy",
            "service/entity/Save$$around.groovy",
        };

        for (String lep : testLeps) {
            leps.onRefresh(
                pattern + lep,
                loadFile("com/icthh/xm/ms/entity/bug/hibernate/update/" + lep)
            );
        }
    }

    @SneakyThrows
    private static String loadFile(String path) {
        InputStream cfgInputStream = new ClassPathResource(path).getInputStream();
        return IOUtils.toString(cfgInputStream, UTF_8);
    }

    private XmEntity buildProduct() {
        XmEntity product = new XmEntity();
        product.setTypeKey("PRODUCT.AEPRODUCT.CAR-RENT.PER-MIN-RENT");
        product.setStateKey("CHECKUP_STARTED");
        product.setKey("product-car-share-0");
        product.setName("AEProduct.CarRent.#0");
        return product;
    }

    private XmEntity buildAeCar() {
        XmEntity car = new XmEntity();
        car.setTypeKey("RESOURCE.CAR.AE");
        car.setStateKey("RESERVED");
        car.setKey("resource-car-ae-0");
        car.setName("Car.AE.#0");
        car.getData().put("vin", "win-win");
        car.getData().put("registrationNumber", "AA1111AA");
        return car;
    }

    private Link linkProductToCar(XmEntity product, XmEntity car) {
        Link link = new Link();
        link.setTypeKey("LINK.PRODUCT.AEPRODUCT.RESOURCE.CAR");

        product.addTargets(link);
        car.addSources(link);

        return link;
    }

}
