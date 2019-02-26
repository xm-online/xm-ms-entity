package com.icthh.xm.lep

import com.icthh.xm.ms.entity.AbstractUnitTest
import org.junit.experimental.categories.Category

/**
 * Abstract test for extension for any Groovy Unit test.
 * Marks test with junit @Category the same as java unit tests
 */
@Category(AbstractUnitTest.class)
abstract class AbstractGroovyUnitTest extends GroovyTestCase {
}
