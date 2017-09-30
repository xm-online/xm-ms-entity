import org.slf4j.LoggerFactory

def log = LoggerFactory.getLogger(getClass())
log.info("LEP AFTER: ChangeState for idOrKey [{}], next state [{}]",
    lepContext.inArgs.idOrKey,
    lepContext.inArgs.nextStateKey)
