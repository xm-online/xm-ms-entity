package com.icthh.xm.lep.lifecycle

import com.icthh.xm.commons.exceptions.EntityNotFoundException
import com.icthh.xm.lep.AbstractGroovyUnitTest
import com.icthh.xm.ms.entity.domain.XmEntity
import com.icthh.xm.ms.entity.domain.ext.IdOrKey
import com.icthh.xm.ms.entity.service.XmEntityService
import com.icthh.xm.ms.entity.service.XmTenantLifecycleService
import org.junit.Before
import org.junit.Test

import static com.icthh.xm.lep.LepTestConstants.LEP_DEFAULT_PATH
import static org.mockito.Mockito.*

class ChangeStateUnitTest extends AbstractGroovyUnitTest {

    String scriptName = LEP_DEFAULT_PATH + '/lifecycle/ChangeState.groovy'

    GroovyShell groovyShell
    Binding binding
    def lepContext

    @Before
    void setUp() {
        groovyShell = new GroovyShell()
        binding = new Binding()
        lepContext = [
            inArgs:[
                idOrKey: null,
                nextStateKey: null,
                context: [:]
            ],
            services:[
                xmEntity: null,
                xmTenantLifeCycle: null
            ],
            commons:[
            ]
        ]
    }

    @Test
    void testErrorOnNotFound() {
        def msg = shouldFail(EntityNotFoundException) {
            lepContext.inArgs.idOrKey = IdOrKey.of(1)
            lepContext.services.xmEntity = [
                findOne : { IdOrKey key -> null }] as XmEntityService

            //set context
            groovyShell.setVariable('lepContext', lepContext)
            groovyShell.evaluate(new File(scriptName))
        }
        assertEquals 'Entity not found', msg
    }

    @Test
    void testChangeState() {
        lepContext.inArgs.idOrKey = IdOrKey.of(1)
        lepContext.inArgs.nextStateKey = "STATE1"
        lepContext.services.xmEntity = [
            findOne : { IdOrKey key -> newXmEntity(IdOrKey.of(1), "ENY_TYPE", [:]) },
            save: { XmEntity entity -> entity}
        ] as XmEntityService

        //set context
        groovyShell.setVariable('lepContext', lepContext)
        XmEntity value = groovyShell.evaluate(new File(scriptName))

        XmEntity ent = newXmEntity(IdOrKey.of(1), "ENY_TYPE", [:])
        ent.setStateKey("STATE1")

        assertEquals  ent, value
        assertEquals ent.stateKey, value.stateKey
    }

    @Test
    void testTenantChangeState() {
        lepContext.inArgs.idOrKey = IdOrKey.of(1)
        lepContext.inArgs.nextStateKey = "STATE1"
        lepContext.inArgs.context = [:]
        lepContext.services.xmEntity = [
            findOne : { IdOrKey key -> newXmEntity(IdOrKey.of(1), "RESOURCE.XM-TENANT", [:]) },
            save: { XmEntity entity -> entity}
        ] as XmEntityService

        XmTenantLifecycleService xmTenantLifeCycle = mock(XmTenantLifecycleService.class)
        doNothing().when(xmTenantLifeCycle).changeState(any(), any(), any())

        lepContext.services.xmTenantLifeCycle = xmTenantLifeCycle

        //set context
        groovyShell.setVariable('lepContext', lepContext)
        XmEntity value = groovyShell.evaluate(new File(scriptName))

        XmEntity ent = newXmEntity(IdOrKey.of(1), "RESOURCE.XM-TENANT", [:])
        ent.setStateKey("NEW")

        assertEquals  ent, value
        assertEquals ent.stateKey, value.stateKey
    }

    private newXmEntity(IdOrKey id, String typeKey, Map data) {
        XmEntity e = new XmEntity()
        e.setId(id.getId())
        e.setKey(id.getKey())
        e.setStateKey("NEW")
        e.setTypeKey(typeKey)
        e.setData(data)
        return e
    }

}
