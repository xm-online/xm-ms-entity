package com.icthh.xm.ms.entity.service.impl;

import com.google.common.collect.Maps;
import com.icthh.xm.ms.entity.AbstractUnitTest;
import com.icthh.xm.ms.entity.domain.spec.TagSpec;
import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import com.icthh.xm.ms.entity.domain.spec.UiActionSpec;
import com.icthh.xm.ms.entity.domain.spec.XmEntitySpec;
import com.icthh.xm.ms.entity.service.*;
import org.apache.commons.compress.utils.Lists;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.icthh.xm.ms.entity.domain.spec.UiActionSpec.*;
import static org.assertj.core.api.Assertions.assertThat;

public class SpecVisitorServiceImplUnitTest  extends AbstractUnitTest {

    ISpecVisitor visitor;

    @Before
    public void setUp() {
        visitor = new SpecVisitorServiceImpl();
    }

    @Test
    public void shouldReturnUpdatedSpecsWithUiAction() {
        XmEntitySpec xmSpec = createSpec(5);

        //get xmSpec from dummy spec
        Function<Integer, TypeSpec> getItem = (value) -> xmSpec.getTypes().get(value);
        //get tagSpec[j] item from dummy spec[i]
        BiFunction<Integer, Integer, TagSpec> getTag = (i, j) -> xmSpec.getTypes().get(i).getTags().get(j);

        assertThat(getItem.apply(0).getUiActionSpec()).isNullOrEmpty();
        assertThat(getItem.apply(1).getUiActionSpec()).isNullOrEmpty();

        assertThat(getTag.apply(0, 0).getUiActionSpec()).isNullOrEmpty();
        assertThat(getTag.apply(1, 1).getUiActionSpec()).isNullOrEmpty();

        List<TypeSpec> typeSpecs = visitMeBaby(xmSpec.getTypes(), visitor);

        assertThat(typeSpecs).isNotEmpty();
        assertThat(typeSpecs.get(0).getUiActionSpec()).containsExactlyInAnyOrder(READ);
        assertThat(typeSpecs.get(1).getUiActionSpec()).containsExactlyInAnyOrder(CREATE, READ, UPDATE, DELETE);

        assertThat(typeSpecs.get(0).getTags()).isNotEmpty();
        assertThat(typeSpecs.get(0).getTags().get(0).getUiActionSpec()).containsExactlyInAnyOrder(READ);
        assertThat(typeSpecs.get(1).getTags().get(1).getUiActionSpec()).containsExactlyInAnyOrder(CREATE, READ, UPDATE, DELETE);
    }

    private List<TypeSpec> visitMeBaby(List<TypeSpec> spec, ISpecVisitor visitor) {
        spec.stream().forEach(type -> type.accept(visitor));
        return spec;
    }

    private XmEntitySpec createSpec(int size) {
        XmEntitySpec spec = new XmEntitySpec();
        spec.setTypes(Lists.newArrayList());

        if (size == 0) {
            return spec;
        }

        IntStream.range(0,size).forEach(i -> spec.getTypes().add(newTypeSpec(i)));
        return spec;
    }

    private TypeSpec newTypeSpec(int code) {
        TypeSpec spec = new TypeSpec();
        spec.setKey("key" + code);
        spec.setName(Maps.newHashMap());
        List<TagSpec> tagSpecList = IntStream.range(0, code + 1).mapToObj(this::newTagSpec).collect(Collectors.toList());
        spec.setTags(tagSpecList);
        return spec;
    }

    private TagSpec newTagSpec(int code) {
        TagSpec ts = new TagSpec();
        ts.setKey("tag"+code);
        return ts;
    }

}
