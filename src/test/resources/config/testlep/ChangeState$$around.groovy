import com.icthh.xm.commons.exceptions.EntityNotFoundException
import com.icthh.xm.ms.entity.domain.XmEntity
import com.icthh.xm.ms.entity.domain.ext.IdOrKey
import com.icthh.xm.ms.entity.service.XmEntityService
import com.icthh.xm.ms.entity.service.XmTenantLifecycleService
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

if ("TEST_LIFECYCLE" == entity.getTypeKey()) {
    entity.getData().updateState = (entity.getData().updateState ?: 0) + 1;
    entity.setStateKey(nextStateKey)
}

if ("TEST_LIFECYCLE_TYPE_KEY.SUB.CHILD" == entity.getTypeKey()) {
    entity.getData().called = entity.getData().called + ' root';
    entity.setStateKey(nextStateKey)
}

return xmEntityService.save(entity)
