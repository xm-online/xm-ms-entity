package com.icthh.xm.ms.entity.service.search.db.dialect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.icthh.xm.ms.entity.AbstractJupiterUnitTest;
import jakarta.persistence.criteria.Path;
import java.math.BigDecimal;
import java.math.BigInteger;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.junit.jupiter.api.Test;

/**
 * Oracle returns JSON values as scalars, so the RETURNING type must follow the compared value; otherwise
 * numeric filters would be compared as text and, for example, 10 would rank before 2.
 */
public class OracleJsonValueStrategyUnitTest extends AbstractJupiterUnitTest {

    private final OracleJsonValueStrategy strategy = new OracleJsonValueStrategy();

    @Test
    public void returningTypeFollowsTheComparedValue() {
        assertThat(OracleJsonValueStrategy.returningType(10L)).isEqualTo(Long.class);
        assertThat(OracleJsonValueStrategy.returningType(10)).isEqualTo(Long.class);
        assertThat(OracleJsonValueStrategy.returningType(BigInteger.TEN)).isEqualTo(Long.class);
        assertThat(OracleJsonValueStrategy.returningType(10.5d)).isEqualTo(Double.class);
        assertThat(OracleJsonValueStrategy.returningType(10.5f)).isEqualTo(Double.class);
        assertThat(OracleJsonValueStrategy.returningType(BigDecimal.ONE)).isEqualTo(BigDecimal.class);
        assertThat(OracleJsonValueStrategy.returningType(true)).isEqualTo(Boolean.class);
        assertThat(OracleJsonValueStrategy.returningType("Kyiv")).isEqualTo(String.class);
        assertThat(OracleJsonValueStrategy.returningType(null)).isEqualTo(String.class);
    }

    @Test
    public void numericOperandAsksForTypedJsonValue() {
        HibernateCriteriaBuilder cb = mock(HibernateCriteriaBuilder.class);
        Path<?> data = mock(Path.class);

        strategy.jsonValue(cb, data, "$.orderNo", 10L);

        verify(cb).jsonValue(data, "$.orderNo", Long.class);
    }

    @Test
    public void stringOperandAndTextExtractionUseString() {
        HibernateCriteriaBuilder cb = mock(HibernateCriteriaBuilder.class);
        Path<?> data = mock(Path.class);

        strategy.jsonValue(cb, data, "$.city", "Kyiv");
        strategy.jsonText(cb, data, "$.city");

        verify(cb, times(2)).jsonValue(data, "$.city", String.class);
    }

    @Test
    public void literalKeepsItsJavaType() {
        HibernateCriteriaBuilder cb = mock(HibernateCriteriaBuilder.class);

        strategy.literal(cb, 10L);

        verify(cb).literal(10L);
    }
}
