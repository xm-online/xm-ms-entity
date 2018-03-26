import com.icthh.xm.ms.entity.domain.XmEntity
import org.slf4j.Logger
import org.slf4j.LoggerFactory


Logger log = LoggerFactory.getLogger(getClass())

XmEntity xmEntity = lepContext.inArgs.xmEntity

xmEntity.getData().runGeneralScript = (xmEntity.getData().runGeneralScript ?: 0) + 1

XmEntity result = lepContext.lep.proceed(lepContext.lep.getMethodArgValues())

log.info("Run general script");

return result
