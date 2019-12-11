import com.icthh.xm.ms.entity.service.XmEntityService
import org.slf4j.LoggerFactory

def log = LoggerFactory.getLogger(getClass())


XmEntityService xmEntityService = lepContext.services.xmEntity

def entity = lepContext.lep.proceed(lepContext.lep.getMethodArgValues())
entity.getData().updateByEntity = (entity.getData().updateByEntity ?: 0) + 1;
return xmEntityService.save(entity)
