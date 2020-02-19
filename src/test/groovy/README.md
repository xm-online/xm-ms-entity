# Preconditions

directory `src/test/groovy/com/icthh/xm/lep/tenant` is reserved for xm-entity tenant's LEP unit tests symlinks
directory `src/test/resources/lep/custom` is reserved for tenant's LEP scripts symlinks

# Automatic test scanning

Run class com.icthh.xm.ms.entity.LepTestLinkScanner

# Manual environment setup

## Setup environment for tenant
```
export XM_REPOSITORY_HOME=<path_to_ms_config>/xm-ms-config-repository
export tenant=<tenant>
export tenant_upper=`echo $tenant | tr a-z A-Z`

export LEP_TEST_HOME=./src/test/groovy/com/icthh/xm/lep/tenant
export LEP_SCRIPT_HOME=./src/test/resources/lep/custom
```

## 1. Create symlink to Unit tests:

```
mkdir -p $LEP_TEST_HOME
ln -s $XM_REPOSITORY_HOME/config/tenants/$tenant_upper/entity/test/ $LEP_TEST_HOME/$tenant
```

## 2. Create symlink to entity LEPs:
```
mkdir -p $LEP_SCRIPT_HOME
ln -s $XM_REPOSITORY_HOME/config/tenants/$tenant_upper/entity/lep $LEP_SCRIPT_HOME/$tenant
```
## 3. Create tes as extension from AbstractLepFunctionTest

## 4. Test LEP scripts in entity service context from IDE or gradle:

Run single test:
```
./gradlew test --tests <MyTestName>
```

## 5. It is possible to add multiple tenants for parallel testing.

# Test creation

Every Spring configuration related to LEP groovy tests should be annotated with `@Profile('leptest')`
to prevent interfering with backend java tests.
