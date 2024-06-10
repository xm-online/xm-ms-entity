package com.icthh.xm.lep

import com.icthh.xm.commons.lep.spring.lepservice.LepServiceFactory
import com.icthh.xm.commons.security.XmAuthenticationContext
import com.icthh.xm.ms.entity.lep.LepContext
import com.icthh.xm.ms.entity.lep.helpers.LepContextConstructor
import com.icthh.xm.ms.entity.repository.XmEntityRepository
import com.icthh.xm.ms.entity.service.XmEntityService
import groovy.transform.ToString
import org.junit.Test
import org.springframework.web.client.RestTemplate

import static org.mockito.ArgumentMatchers.eq
import static org.mockito.Mockito.mock
import static org.junit.Assert.*
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.verifyNoMoreInteractions

class LepContextTransformationUnitTest {


    public static final String FINAL_STRING_VALUE = "some value inited in constructor"

    @Test
    public void test() {
        def mockLepContext = new LepContext()
        mockLepContext.services = new LepContext.LepServices();
        mockLepContext.templates = new LepContext.LepTemplates();
        mockLepContext.repositories = new LepContext.LepRepositories();
        mockLepContext.lepServices = mock(LepServiceFactory.class)

        mockLepContext.repositories.xmEntity = mock(XmEntityRepository.class);
        mockLepContext.services.xmEntity = mock(XmEntityService.class);
        mockLepContext.templates.plainRest = mock(RestTemplate.class);
        mockLepContext.templates.rest = mock(RestTemplate.class);
        mockLepContext.authContext = mock(XmAuthenticationContext.class);

        def t = new TestLepService(mockLepContext);
        assertSame(mockLepContext.repositories.xmEntity, t.repo)
        assertSame(mockLepContext.templates.rest, t.rest)
        assertSame(mockLepContext.templates.plainRest, t.plainRest)
        assertSame(mockLepContext, t.lepContext)

        assertSame(mockLepContext.services.xmEntity, t.anotherService.entityService)
        assertSame(mockLepContext.authContext, t.anotherService.auth)
        assertEquals(FINAL_STRING_VALUE, t.anotherService.valueInitedInConstructor)

        verify(mockLepContext.lepServices).getInstance((Class<AnotherTestLepServiceCreateByFactory>)eq(AnotherTestLepServiceCreateByFactory.class))
        verifyNoMoreInteractions(mockLepContext.lepServices)
    }

    @ToString
    @LepContextConstructor
    public static class TestLepService {
        final RestTemplate rest
        final RestTemplate plainRest
        final XmEntityRepository repo
        final AnotherTestLepService anotherService
        final LepContext lepContext
    }

    @ToString
    @LepContextConstructor(useLepFactory = true)
    public static class AnotherTestLepService {

        static final String CONSTANT = "some constant value";

        final XmEntityService entityService
        final XmAuthenticationContext auth
        final String valueInitedInConstructor
        final AnotherTestLepServiceCreateByFactory anotherTestLepServiceCreateByFactory

        AnotherTestLepService(def lepContext) {
            valueInitedInConstructor = FINAL_STRING_VALUE
        }
    }

    @ToString
    @LepContextConstructor
    public static class AnotherTestLepServiceCreateByFactory {
        final LepContext lepContext
    }

}
