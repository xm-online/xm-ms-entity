import com.icthh.xm.commons.exceptions.BusinessException
import com.icthh.xm.ms.entity.domain.XmEntity
import org.slf4j.Logger
import org.slf4j.LoggerFactory

Logger log = LoggerFactory.getLogger(getClass())

XmEntity xmEntity = lepContext.inArgs.xmEntity

XmEntity result = lepContext.lep.proceed(lepContext.lep.getMethodArgValues())

throw new BusinessException("");

return result
