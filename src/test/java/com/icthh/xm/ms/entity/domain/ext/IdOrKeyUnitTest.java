package com.icthh.xm.ms.entity.domain.ext;
import com.icthh.xm.ms.entity.AbstractUnitTest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import static org.assertj.core.api.Assertions.*;

public class IdOrKeyUnitTest extends AbstractUnitTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void of() {
        assertThat(IdOrKey.of(0L)).isEqualTo(IdOrKey.of(Long.valueOf(0)));
        assertThat(IdOrKey.of(0L)).isNotEqualTo(IdOrKey.of(Long.valueOf(1)));
        assertThat(IdOrKey.of(123123L)).isEqualTo(IdOrKey.of(Long.valueOf(123123)));
        assertThat(IdOrKey.of(123123123123123L)).isEqualTo(IdOrKey.of(Long.valueOf(123123123123123L)));
        assertThat(IdOrKey.of(123123123123123L)).isEqualTo(IdOrKey.of("123123123123123"));
    }

    @Test
    public void ofKey() {
        assertThat(IdOrKey.ofKey("123")).isEqualTo(IdOrKey.ofKey("123"));
        assertThat(IdOrKey.ofKey("qwe")).isEqualTo(IdOrKey.ofKey("qwe"));
        assertThat(IdOrKey.ofKey("qwe")).isEqualTo(IdOrKey.of("qwe"));
        assertThat(IdOrKey.ofKey("q")).isNotEqualTo(IdOrKey.ofKey("123"));
    }

    @Test
    public void of1() {
        assertThat(IdOrKey.of("qwe")).isEqualTo(IdOrKey.of("qwe"));
        assertThat(IdOrKey.of("qwe")).isNotEqualTo(IdOrKey.of("zxc"));
        assertThat(IdOrKey.of("qwe")).isEqualTo(IdOrKey.ofKey("qwe"));
        assertThat(IdOrKey.of("123")).isEqualTo(IdOrKey.of(123L));
    }

    @Test
    public void isKey() {
        assertThat(IdOrKey.SELF.isKey()).isTrue();
        assertThat(IdOrKey.of(1L).isKey()).isFalse();
        assertThat(IdOrKey.of("yt").isKey()).isTrue();
        assertThat(IdOrKey.of("125").isKey()).isFalse();
        assertThat(IdOrKey.ofKey("1").isKey()).isTrue();
    }

    @Test
    public void isId() {
        assertThat(IdOrKey.SELF.isId()).isFalse();
        assertThat(IdOrKey.of(1L).isId()).isTrue();
        assertThat(IdOrKey.of("yt").isId()).isFalse();
        assertThat(IdOrKey.of("125").isId()).isTrue();
        assertThat(IdOrKey.ofKey("1").isId()).isFalse();
    }

    @Test
    public void isSelf() {
        assertThat(IdOrKey.SELF).isEqualTo(IdOrKey.SELF);
        assertThat(IdOrKey.SELF).isEqualTo(IdOrKey.of("self"));
        assertThat(IdOrKey.SELF).isEqualTo(IdOrKey.ofKey("self"));
        assertThat(IdOrKey.SELF).isNotEqualTo(IdOrKey.of(1L));
        assertThat(IdOrKey.SELF).isNotEqualTo(IdOrKey.of("sdf"));
        assertThat(IdOrKey.SELF).isNotEqualTo(IdOrKey.ofKey("asd"));
        assertThat(IdOrKey.SELF.isSelf()).isTrue();
        assertThat(IdOrKey.of("1").isSelf()).isFalse();
        assertThat(IdOrKey.of(12L).isSelf()).isFalse();
        assertThat(IdOrKey.of("qwe").isSelf()).isFalse();
        assertThat(IdOrKey.ofKey("qwe").isSelf()).isFalse();
        assertThat(IdOrKey.of("self").isSelf()).isTrue();
        assertThat(IdOrKey.ofKey("self").isSelf()).isTrue();
    }

    @Test
    public void getId() {
        assertThat(IdOrKey.SELF.getId()).isNull();
        assertThat(IdOrKey.of(123L).getId()).isEqualTo(Long.valueOf(123L));
        assertThat(IdOrKey.of("123").getId()).isEqualTo(Long.valueOf(123L));
        assertThat(IdOrKey.ofKey("123").getId()).isNull();
        assertThat(IdOrKey.of("123z").getId()).isNull();
        assertThat(IdOrKey.ofKey("123z").getId()).isNull();
    }

    @Test
    public void getKey() {
        assertThat(IdOrKey.SELF.getKey()).isEqualTo("self");
        assertThat(IdOrKey.of(123L).getKey()).isNull();
        assertThat(IdOrKey.of("123").getKey()).isNull();
        assertThat(IdOrKey.of("123z").getKey()).isEqualTo("123z");
        assertThat(IdOrKey.ofKey("123").getKey()).isEqualTo("123");
        assertThat(IdOrKey.ofKey("123z").getKey()).isEqualTo("123z");
    }

    @Test
    public void errorOfKeyNull() {
        //TODO may be IllegalArgumentException should be raised????
        exception.expect(NullPointerException.class);
        IdOrKey.ofKey(null);
    }

    @Test
    public void errorOfNullString() {
        //TODO may be IllegalArgumentException  should be raised????
        exception.expect(NullPointerException.class);
        IdOrKey.of((String) null);
    }

    @Test
    public void errorOfNullNumber() {
        //TODO may be IllegalArgumentException  should be raised????
        exception.expect(NullPointerException.class);
        IdOrKey.of((Long) null);
    }

}
