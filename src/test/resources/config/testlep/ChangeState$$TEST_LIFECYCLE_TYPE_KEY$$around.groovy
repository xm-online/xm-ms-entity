def entity = lepContext.lep.proceed(lepContext.lep.getMethodArgValues())
entity.getData().called = entity.getData().called + ' ${lepName}'
return lepContext.services.xmEntity.save(entity)
