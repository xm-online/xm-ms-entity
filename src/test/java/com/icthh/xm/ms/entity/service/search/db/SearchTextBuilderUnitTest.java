package com.icthh.xm.ms.entity.service.search.db;

import static org.assertj.core.api.Assertions.assertThat;

import com.icthh.xm.ms.entity.AbstractJupiterUnitTest;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class SearchTextBuilderUnitTest extends AbstractJupiterUnitTest {

    private final SearchTextBuilder builder = new SearchTextBuilder();

    private static XmEntity entity(Map<String, Object> data) {
        return new XmEntity().name("Alpha order").description("Big <b>one</b>").data(new HashMap<>(data));
    }

    @Test
    public void returnsNullWhenFlagOffOrSpecMissing() {
        assertThat(builder.build(null, entity(Map.of()))).isNull();
        assertThat(builder.build(TypeSpec.builder().key("T").build(), entity(Map.of()))).isNull();
        assertThat(builder.build(TypeSpec.builder().key("T").fullTextSearch(false).build(), entity(Map.of()))).isNull();
    }

    @Test
    public void joinsNameDescriptionAndConfiguredDataFields() {
        TypeSpec spec = TypeSpec.builder().key("T").fullTextSearch(true)
            .fullTextSearchDataFields(List.of("data.orderNo", "customer.city", "data.tags", "data.customer", "data.missing"))
            .build();
        XmEntity e = entity(Map.of(
            "orderNo", 42,
            "customer", Map.of("city", "Kyiv"),
            "tags", List.of("vip", "urgent")));

        String text = builder.build(spec, e);

        // lists and objects are rendered with String.valueOf; missing paths are skipped
        assertThat(text).isEqualTo("Alpha order\nBig <b>one</b>\n42\nKyiv\n[vip, urgent]\n{city=Kyiv}");
    }

    @Test
    public void dataFieldsAreSpelExpressions() {
        TypeSpec spec = TypeSpec.builder().key("T").fullTextSearch(true)
            .fullTextSearchDataFields(List.of(
                "data.lines[0].sku",          // index into a list of objects
                "data.customer?.city",        // safe navigation: customer missing → skipped
                "data.total * 2"))            // arithmetic
            .build();
        XmEntity e = entity(Map.of(
            "lines", List.of(Map.of("sku", "SKU-1"), Map.of("sku", "SKU-2")),
            "total", 21,
            "tags", List.of("a", "b")));

        assertThat(builder.build(spec, e)).isEqualTo("Alpha order\nBig <b>one</b>\nSKU-1\n42");
    }

    @Test
    public void nameAndDescriptionOnlyWhenNoDataFields() {
        TypeSpec spec = TypeSpec.builder().key("T").fullTextSearch(true).build();
        assertThat(builder.build(spec, entity(Map.of("x", 1)))).isEqualTo("Alpha order\nBig <b>one</b>");
        assertThat(builder.build(spec, new XmEntity().name("only"))).isEqualTo("only");
    }
}
