package com.icthh.xm.lep.lifecycle

import com.icthh.xm.commons.exceptions.EntityNotFoundException
import com.icthh.xm.lep.AbstractGroovyUnitTest
import com.icthh.xm.ms.entity.domain.XmEntity
import com.icthh.xm.ms.entity.domain.ext.IdOrKey
import com.icthh.xm.ms.entity.service.XmEntityService
import com.icthh.xm.ms.entity.service.XmTenantLifecycleService
import org.junit.Test

import static com.icthh.xm.lep.LepTestConstants.LEP_DEFAULT_PATH
import static org.mockito.Mockito.*

class ChangeStateUnitTest extends AbstractGroovyUnitTest {

    String scriptName = LEP_DEFAULT_PATH + '/lifecycle/ChangeState.groovy'

    @Override
    Object setLepContext() {
        [
            inArgs  : [
                idOrKey     : null,
                nextStateKey: null,
                context     : [:]
            ],
            services: [
                xmEntity         : null,
                xmTenantLifeCycle: null
            ],
            commons : [
            ]
        ]
    }

    private <T> T evaluateScript() {
        evaluateScript(scriptName)
    }

    @Test
    void testErrorOnNotFound() {

        lepContext.inArgs.idOrKey = IdOrKey.of(1)
        lepContext.services.xmEntity = [findOne: { IdOrKey key -> null }] as XmEntityService

        EntityNotFoundException ex = shouldFail(EntityNotFoundException) {
            evaluateScript()
        }
        assert 'Entity not found' == ex.message
    }

    @Test
    void testChangeState() {
        lepContext.inArgs.idOrKey = IdOrKey.of(1)
        lepContext.inArgs.nextStateKey = "STATE1"
        lepContext.services.xmEntity = [
            findOne: { IdOrKey key -> newXmEntity(IdOrKey.of(1), "ENY_TYPE", [:]) },
            save   : { XmEntity entity -> entity }
        ] as XmEntityService

        XmEntity value = evaluateScript()

        XmEntity ent = newXmEntity(IdOrKey.of(1), "ENY_TYPE", [:])
        ent.setStateKey("STATE1")

        assert ent == value
        assert ent.stateKey == value.stateKey
    }

    @Test
    void testTenantChangeState() {
        lepContext.inArgs.idOrKey = IdOrKey.of(1)
        lepContext.inArgs.nextStateKey = "STATE1"
        lepContext.inArgs.context = [:]
        lepContext.services.xmEntity = [
            findOne: { IdOrKey key -> newXmEntity(IdOrKey.of(1), "RESOURCE.XM-TENANT", [:]) },
            save   : { XmEntity entity -> entity }
        ] as XmEntityService

        XmTenantLifecycleService xmTenantLifeCycle = mock(XmTenantLifecycleService.class)
        doNothing().when(xmTenantLifeCycle).changeState(any(XmEntity), anyString(), anyMap())
        lepContext.services.xmTenantLifeCycle = xmTenantLifeCycle

        XmEntity value = evaluateScript()

        verify(xmTenantLifeCycle).changeState([id: 1] as XmEntity, 'STATE1', [:])

        XmEntity expected = newXmEntity(IdOrKey.of(1), "RESOURCE.XM-TENANT", [:])
        expected.setStateKey("NEW")

        assert expected == value
        assert expected.stateKey == value.stateKey
    }

    private static newXmEntity(IdOrKey id, String typeKey, Map data) {
        XmEntity e = new XmEntity()
        e.setId(id.getId())
        e.setKey(id.getKey())
        e.setStateKey("NEW")
        e.setTypeKey(typeKey)
        e.setData(data)
        return e
    }

}
