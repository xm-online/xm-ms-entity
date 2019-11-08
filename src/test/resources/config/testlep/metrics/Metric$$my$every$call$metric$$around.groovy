def context = lepContext.metricsContext.getMetricsContext()
def callCount = context.callCount ?: 0
context.callCount = callCount + 1
return context.callCount
