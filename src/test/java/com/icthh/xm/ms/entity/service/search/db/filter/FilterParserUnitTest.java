package com.icthh.xm.ms.entity.service.search.db.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.ms.entity.AbstractJupiterUnitTest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class FilterParserUnitTest extends AbstractJupiterUnitTest {

    private final FilterParser parser = new FilterParser();

    @Test
    public void parsesBodyWithTypedValues() {
        List<FilterCondition> conditions = parser.parseBody(Map.of(
            "data.order.in", List.of(1, 2, 3),
            "data.subObject.position.eq", 5,
            "name.contains", "abc",
            "removed.eq", true));

        assertThat(conditions).containsExactlyInAnyOrder(
            new FilterCondition("data.order", FilterOperator.IN, List.of(1, 2, 3)),
            new FilterCondition("data.subObject.position", FilterOperator.EQ, List.of(5)),
            new FilterCondition("name", FilterOperator.CONTAINS, List.of("abc")),
            new FilterCondition("removed", FilterOperator.EQ, List.of(true)));
    }

    @Test
    public void parsesQueryParamsAndInfersTypes() {
        List<FilterCondition> conditions = parser.parseQueryParams(Map.of(
            "data.order.in", List.of("1,2,3"),
            "data.price.gt", List.of("10.5"),
            "data.flag.specified", List.of("true"),
            "stateKey.eq", List.of("ACTIVE"),
            "page", List.of("0"), "size", List.of("20"), "sort", List.of("name,asc"),
            "typeKey", List.of("ORDER"), "query", List.of("x"), "includeSubTypes", List.of("false")));

        assertThat(conditions).containsExactlyInAnyOrder(
            new FilterCondition("data.order", FilterOperator.IN, List.of(1L, 2L, 3L)),
            new FilterCondition("data.price", FilterOperator.GT, List.of(10.5d)),
            new FilterCondition("data.flag", FilterOperator.SPECIFIED, List.of(true)),
            new FilterCondition("stateKey", FilterOperator.EQ, List.of("ACTIVE")));
    }

    @Test
    public void dataJsonPathIsDerivedFromField() {
        FilterCondition c = new FilterCondition("data.subObject.position", FilterOperator.EQ, List.of(5));
        assertThat(c.isDataField()).isTrue();
        assertThat(c.dataJsonPath()).isEqualTo("$.subObject.position");
        assertThat(new FilterCondition("name", FilterOperator.EQ, List.of("a")).isDataField()).isFalse();
    }

    @Test
    public void rejectsUnknownOperator() {
        assertThatThrownBy(() -> parser.parseBody(Map.of("name.like", "a")))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("name.like");
    }

    @Test
    public void rejectsKeyWithoutOperator() {
        assertThatThrownBy(() -> parser.parseBody(Map.of("name", "a")))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("name");
    }

    @Test
    public void rejectsScalarForInOperatorInBody() {
        assertThatThrownBy(() -> parser.parseBody(Map.of("name.in", "a")))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("name.in");
    }

    @Test
    public void rejectsMalformedDataPath() {
        for (String key : List.of("data..orderNo.eq", "data.order-no.eq", "data.a b.eq")) {
            assertThatThrownBy(() -> parser.parseBody(Map.of(key, 1)))
                .isInstanceOf(BusinessException.class);
        }
    }

    @Test
    public void rejectsNullScalarValue() {
        java.util.Map<String, Object> filter = new java.util.HashMap<>();
        filter.put("name.eq", null);
        assertThatThrownBy(() -> parser.parseBody(filter))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("name.eq");
    }

    @Test
    public void rejectsNonBooleanForSpecified() {
        assertThatThrownBy(() -> parser.parseQueryParams(Map.of("name.specified", List.of("yes"))))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("name.specified");
    }

    @Test
    public void emptyOrNullFilterGivesEmptyList() {
        assertThat(parser.parseBody(null)).isEmpty();
        assertThat(parser.parseBody(Map.of())).isEmpty();
        assertThat(parser.parseQueryParams(Map.of("page", List.of("1")))).isEmpty();
    }
}
