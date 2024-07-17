import com.icthh.xm.ms.entity.domain.spec.TypeSpec

Map<String, TypeSpec> entitySpec = lepContext.inArgs.entitySpec
def typeSpec = new TypeSpec()
typeSpec.setKey("ENTITY.FROM.LEP")
typeSpec.setName(["en": "name"])
entitySpec.put("ENTITY.FROM.LEP", typeSpec)

