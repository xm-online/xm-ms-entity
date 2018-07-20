
import org.slf4j.Logger
import org.slf4j.LoggerFactory

Logger log = LoggerFactory.getLogger(getClass())

log.info('COMMONS {}', lepContext.commons)

return [result: lepContext.commons.xm.path.to.commons.trololo(1, 2, 5).myCommonFunction("COMMON_ARGUMENT")]
