import com.icthh.xm.commons.errors.exception.EntityNotFoundException
import com.icthh.xm.ms.entity.domain.XmEntity
import com.icthh.xm.ms.entity.domain.ext.IdOrKey
import com.icthh.xm.ms.entity.service.XmTenantLifecycleService
import com.icthh.xm.ms.entity.service.api.XmEntityService
import org.slf4j.LoggerFactory

def log = LoggerFactory.getLogger(getClass())

// get input parameters
IdOrKey idOrKey = lepContext.inArgs.idOrKey
String nextStateKey = lepContext.inArgs.nextStateKey
Map<String, Object> context = lepContext.inArgs.context
XmEntityService xmEntityService = lepContext.services.xmEntity
XmTenantLifecycleService xmTenantLifeCycleService = lepContext.services.xmTenantLifeCycle

log.info("LEP DEFAULT: ChangeState for idOrKey [{}], next state [{}]", idOrKey, nextStateKey)

// find required xm entity
XmEntity entity = xmEntityService.findOne(idOrKey)
if (!entity) {
    throw new EntityNotFoundException("Entity not found")
}



// TODO why we need this typeKey hard code ??
// mаy be here more appropriate to use LEP XmEntityLifeCycleService#changeState with specific entity type paradigm
if ("RESOURCE.XM-TENANT" == entity.getTypeKey()) {
    xmTenantLifeCycleService.changeState(entity, nextStateKey, context)
} else {
    // change entity state
    entity.setStateKey(nextStateKey)
}

return xmEntityService.save(entity)
