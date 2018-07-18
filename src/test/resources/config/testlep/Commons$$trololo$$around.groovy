
import org.slf4j.Logger
import org.slf4j.LoggerFactory

Logger log = LoggerFactory.getLogger(getClass())

return [
    myCommonFunction: { funcArg ->
        "RESULT ${lepContext.inArgs.args} | ${funcArg}"
    }
]
