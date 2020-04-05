# Preconditions

directory `src/test/groovy/com/icthh/xm/lep/tenant` is reserved for xm-entity tenant's LEP unit tests symlinks
directory `src/test/resources/lep/custom` is reserved for tenant's LEP scripts symlinks

# Automatic test environment setup

Run lep lest link scanner:

```
java com.icthh.xm.ms.entity.LepTestLinkScanner <path/to/xm-config-repository>
```

It will create links to config repository for all tenants where for entity microservice `test` folder exists.

# Manual environment setup

**NOTE! it is preferable to use test setup approach describer above to avoid mistakes.** 

## Setup environment for tenant
```
export XM_REPOSITORY_HOME=<path_to_ms_config>/xm-ms-config-repository
export tenant=<tenant>
export tenant_upper=`echo $tenant | tr a-z A-Z`

export LEP_TEST_HOME=./src/test/lep
export LEP_SCRIPT_HOME=./src/main/lep
```

## 1. Create symlink to Unit tests:

```
mkdir -p $LEP_TEST_HOME
ln -s $XM_REPOSITORY_HOME/config/tenants/$tenant_upper/entity/test/ $LEP_TEST_HOME/$tenant_upper/entity/test
```

## 2. Create symlink to entity LEPs:
```
mkdir -p $LEP_SCRIPT_HOME
ln -s $XM_REPOSITORY_HOME/config/tenants/$tenant_upper/entity/lep $LEP_SCRIPT_HOME/$tenant_upper/entity/lep
```
## 3. Create tes as extension from AbstractGroovyUnitTest or AbstractLepFunctionTest

## 4. Test LEP scripts in entity service context from IDE or gradle:

Run single test:
```
./gradlew runCategorizedTests --tests <MyTestName>
```

Run all LEP tests related to ms entity:
```
./gradlew runCategorizedTests --tests *.entity.test.*
```

## 5. It is possible to add multiple tenants for parallel testing.

# Test creation guidelines

1. Read javadoc for `com.icthh.xm.lep.AbstractGroovyUnitTest`
1. Every Spring configuration related to LEP groovy tests should be annotated with `@Profile('leptest')`
to prevent interfering with backend java tests.
1. Typically LEP Unit test has next structure:
```

package XM.entity.test.service.entity

import com.icthh.xm.commons.exceptions.BusinessException
import com.icthh.xm.lep.AbstractGroovyUnitTest
import com.icthh.xm.ms.entity.domain.Link
import com.icthh.xm.ms.entity.domain.XmEntity
import org.junit.Before
import org.junit.Test
import org.mockito.MockitoAnnotations

import static groovy.test.GroovyAssert.shouldFail
import static org.mockito.ArgumentMatchers.any
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.verifyZeroInteractions
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.when
import static org.mockito.Mockito.withSettings

class SaveAgreementUnitTest extends AbstractGroovyUnitTest {

    String scriptName = resolveScriptName('/service/entity/Save$$AGREEMENT$$around.groovy')

    Closure linkEntities

    @Override
    @Before
    void setUp() {
        super.setUp()
        linkEntities = mock(Closure.class, withSettings().useConstructor(this))
    }

    @Override
    Object setLepContext() {
        MockitoAnnotations.initMocks(this)
        [
                lep    : [
                        proceed           : { buildAgreement() },
                        getMethodArgValues: { '' }
                ],
                commons: [
                        linkUtils: {
                            [
                                    linkEntities: linkEntities
                            ]
                        }
                ]
        ]
    }

    @Test
    void execFunc_SAVE_success() {
        when(linkEntities.call(any(), any(), any(), any())).thenReturn([])

        XmEntity agreement = evaluateScript(scriptName)

        assert agreement
        assert agreement.id == 1L
        assert agreement.sources

        verify(linkEntities).call('PARTY.AGREEMENT',
                'AGREEMENT.PARTY',
                buildParty(),
                buildEmptyLinksEntity())

    }

    @Test
    void execFunc_SAVE_orderLinksNotFoundException() {
        lepContext.lep.proceed = { buildEmptyLinksEntity() }

        def e = shouldFail {
            evaluateScript(scriptName)
        }

        verifyZeroInteractions(linkEntities)

        assert e instanceof BusinessException
        assert e.code == 'error.agreement.no.links.found'
        assert e.message == 'The are no links with state = ORDER.AGREEMENT'
    }

    @Test
    void execFunc_SAVE_partyLinksNotFoundException() {
        lepContext.lep.proceed = { buildAgreementNoParties() }

        def e = shouldFail {
            evaluateScript(scriptName)
        }

        verifyZeroInteractions(linkEntities)

        assert e instanceof BusinessException
        assert e.code == 'error.order.no.links.found'
        assert e.message == 'The are no links with state = ORDER.PARTY'
    }

    private static XmEntity buildEmptyLinksEntity() {
        [id: 1L] as XmEntity
    }


    private static XmEntity buildParty() {
        [id: 100L] as XmEntity
    }

    private static Set<Link> buildOrderLinks() {
        [
                [
                        typeKey: 'ORDER.AGREEMENT',
                        source : buildEmptyLinksEntity()
                ] as Link,
                [
                        typeKey: 'ORDER.OTHER_LINK'
                ] as Link
        ] as Set
    }

    private static List<Link> buildPartyLinks() {
        [
                [
                        typeKey: 'ORDER.PARTY',
                        target : buildParty()
                ] as Link
        ]
    }

    private static XmEntity buildAgreement() {
        XmEntity agreement = buildEmptyLinksEntity()

        def links = buildOrderLinks()

        links.find { it -> it.typeKey == 'ORDER.AGREEMENT' }
                .source
                .targets =
                buildPartyLinks()
        agreement.sources = links
        agreement
    }

    private static XmEntity buildAgreementNoParties() {
        XmEntity agreement = buildEmptyLinksEntity()
        agreement.sources = buildOrderLinks()
        agreement
    }
}
```
