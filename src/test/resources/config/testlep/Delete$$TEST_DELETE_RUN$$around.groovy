import com.icthh.xm.ms.entity.domain.XmEntity
import com.icthh.xm.ms.entity.repository.XmEntityRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory


Logger log = LoggerFactory.getLogger(getClass())

XmEntity xmEntity = lepContext.inArgs.xmEntity

xmEntity.getData().runDeleteRun = (xmEntity.getData().runDeleteRun ?: 0) + 1


log.info("Run save script");

return xmEntity
