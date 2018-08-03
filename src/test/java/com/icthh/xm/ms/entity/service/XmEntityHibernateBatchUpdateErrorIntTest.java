package com.icthh.xm.ms.entity.service;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_AUTH_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.commons.config.client.service.TenantConfigService;
import com.icthh.xm.commons.security.XmAuthenticationContext;
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
import com.icthh.xm.ms.entity.domain.ext.IdOrKey;
import com.icthh.xm.ms.entity.repository.XmEntityPermittedRepository;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import com.icthh.xm.ms.entity.repository.search.XmEntityPermittedSearchRepository;
import com.icthh.xm.ms.entity.repository.search.XmEntitySearchRepository;
import com.icthh.xm.ms.entity.service.impl.StartUpdateDateGenerationStrategy;
import com.icthh.xm.ms.entity.service.impl.XmEntityServiceImpl;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
    LepConfiguration.class
})
public class XmEntityHibernateBatchUpdateErrorIntTest {

    @Autowired
    private XmEntityRepository xmEntityRepository;

    @Autowired
    private XmEntitySearchRepository xmEntitySearchRepository;

    private XmEntityServiceImpl xmEntityServiceImpl;

    @Autowired
    private EntityManager em;

    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Autowired
    private XmEntitySpecService xmEntitySpecService;

    @Autowired
    private XmEntityTemplatesSpecService xmEntityTemplatesSpecService;

    @Autowired
    private LifecycleLepStrategyFactory lifeCycleService;

    @Autowired
    private XmEntityPermittedRepository xmEntityPermittedRepository;

    @Mock
    private XmAuthenticationContextHolder authContextHolder;

    @Mock
    private XmAuthenticationContext xmAuthContext;

    @Autowired
    private LepManager lepManager;

    @Autowired
    private ProfileService profileService;

    @Autowired
    private LinkService linkService;

    @Autowired
    private StorageService storageService;

    @Autowired
    private AttachmentService attachmentService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TenantConfigService tenantConfigService;

    @Autowired
    private XmEntityPermittedSearchRepository xmEntityPermittedSearchRepository;

    @Autowired
    private StartUpdateDateGenerationStrategy startUpdateDateGenerationStrategy;

    @BeforeTransaction
    public void beforeTransaction() {
        TenantContextUtils.setTenant(tenantContextHolder, TestConfigConstants.TENANT_AEBUGHBU);
    }

    @Before
    public void setup() {
        when(authContextHolder.getContext()).thenReturn(xmAuthContext);
        when(xmAuthContext.getUserKey()).thenReturn(Optional.of("qa"));

        XmEntityServiceImpl xmEntityServiceImpl = new XmEntityServiceImpl(xmEntitySpecService,
            xmEntityTemplatesSpecService,
            xmEntityRepository,
            xmEntitySearchRepository,
            lifeCycleService,
            xmEntityPermittedRepository,
            profileService,
            linkService,
            storageService,
            attachmentService,
            xmEntityPermittedSearchRepository,
            startUpdateDateGenerationStrategy,
            authContextHolder,
            objectMapper,
            tenantConfigService);
        xmEntityServiceImpl.setSelf(xmEntityServiceImpl);

        this.xmEntityServiceImpl = xmEntityServiceImpl;

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

    @Test
    @Transactional
    public void test() throws Exception {
        IdOrKey productIdOrKey = IdOrKey.ofKey(productKey);
        XmEntity rentProduct = xmEntityServiceImpl.findAll(
            Specifications.where(
                (root, query, cb) -> productIdOrKey.isId()
                    ? cb.and(cb.equal(root.get("id"), productIdOrKey.getId()))
                    : cb.and(cb.equal(root.get("key"), productIdOrKey.getKey()))
            )
        ).iterator().next();

        XmEntity car = getCarInRent(rentProduct);

        xmEntityServiceImpl.updateState(IdOrKey.of(car.getId()), "IN_USE", Collections.emptyMap());

        rentProduct.getData().put("startRentDate", Instant.now().toString());
    }

    private XmEntity getCarInRent(XmEntity rentProduct) {
        List<XmEntity> foundCars = linkService.findAll(Specifications.where((root, query, cb) -> {
            Join<Object, Object> source = root.join("source", JoinType.INNER);
            Join<Object, Object> target = root.join("target", JoinType.INNER);

            return cb.and(
                cb.equal(source.get("id"), rentProduct.getId()),
                cb.equal(target.get("typeKey"), "RESOURCE.CAR.AE")
            );
        })).stream().map(Link::getTarget).collect(Collectors.toList());

        if (CollectionUtils.isEmpty(foundCars)) {
            throw new IllegalStateException("Car not found for rent ${rentProduct.id}");
        }

        if (foundCars.size() > 1) {
            throw new IllegalStateException("More then one car (" +
                foundCars.stream().map(XmEntity::getId).collect(Collectors.toList()) +
                ") found for rent ${rentProduct.id}");
        }

        return foundCars.iterator().next();
    }

}
