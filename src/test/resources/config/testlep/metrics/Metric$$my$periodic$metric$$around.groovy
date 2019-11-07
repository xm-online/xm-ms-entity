def context = lepContext.metricsContext.getMetricsContext()
def countPeriod = context.countPeriod ?: 0
context.countPeriod = countPeriod + 1
return context.countPeriod
