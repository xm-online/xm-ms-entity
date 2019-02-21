package com.icthh.xm.ms.entity;

import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Abstract test for extension for any WebMvc test.
 * Marks test with junit @Category
 */
@RunWith(SpringRunner.class)
@Category(AbstractWebMvcTest.class)
public abstract class AbstractWebMvcTest {
}
