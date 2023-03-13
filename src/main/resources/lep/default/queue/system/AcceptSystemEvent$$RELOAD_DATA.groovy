import org.slf4j.LoggerFactory

def log = LoggerFactory.getLogger(getClass())
String prefix = "#### "

def data = lepContext.inArgs.event.data

log.info("${prefix} =============RELOAD DATA FROM ENTITY===========>>>>>>>>>> {}", data)
