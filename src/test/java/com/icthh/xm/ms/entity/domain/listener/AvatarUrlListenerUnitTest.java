package com.icthh.xm.ms.entity.domain.listener;

import static com.icthh.xm.ms.entity.config.Constants.DEFAULT_AVATAR_URL_PREFIX;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.icthh.xm.ms.entity.AbstractSpringBootTest;
import com.icthh.xm.ms.entity.domain.XmEntity;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;

@Slf4j
public class AvatarUrlListenerUnitTest extends AbstractSpringBootTest {

    private AvatarUrlListener target;

    @Before
    public void setup() {
        target = new AvatarUrlListener();
    }

    @Test
    public void testPrePersistEmpty() {
        XmEntity entity = new XmEntity().avatarUrl(null);

        target.prePersist(entity);

        assertNull(entity.getAvatarUrl());
        assertNull(entity.getAvatarUrlRelative());
    }

    @Test
    public void testPrePersistWrongFormat() {
        XmEntity entity = new XmEntity().avatarUrl("hello.jpg");

        target.prePersist(entity);

        assertEquals("hello.jpg", entity.getAvatarUrl());
        assertEquals("hello.jpg", entity.getAvatarUrlRelative());
    }

    @Test
    public void testPrePersistWrongFormatOther() {
        XmEntity entity = new XmEntity().avatarUrl("http://s3.com/hello.jpg");

        target.prePersist(entity);

        assertEquals("http://s3.com/hello.jpg", entity.getAvatarUrl());
        assertEquals("http://s3.com/hello.jpg", entity.getAvatarUrlRelative());
    }

    @Test
    public void testPrePersistSuccess() {
        XmEntity entity = new XmEntity().avatarUrl("http://hello.rgw.icthh.test/hello.jpg");

        target.prePersist(entity);

        assertEquals("hello.jpg", entity.getAvatarUrlRelative());
        assertEquals("http://hello.rgw.icthh.test/hello.jpg", entity.getAvatarUrl());
    }

    @Test
    public void testPrePersistSuccessNginx() {
        XmEntity entity = new XmEntity().avatarUrl("https://hello-rgw.icthh.test/hello.jpg");

        target.prePersist(entity);

        assertEquals("hello.jpg", entity.getAvatarUrlRelative());
        assertEquals("https://hello-rgw.icthh.test/hello.jpg", entity.getAvatarUrl());
    }

    @Test
    public void testPostLoadEmpty() {
        XmEntity entity = new XmEntity().avatarUrl(null);

        target.postLoad(entity);

        assertNull(entity.getAvatarUrl());
        assertNull(entity.getAvatarUrlRelative());
    }

    @Test
    public void testPostLoadWrongFormat() {
        XmEntity entity = new XmEntity().avatarUrl("http://s3.hello.amazonaws.com/hello/hello.jpg");

        target.postLoad(entity);

        assertEquals("http://s3.hello.amazonaws.com/hello/hello.jpg", entity.getAvatarUrl());
    }

    @Test
    public void testPostLoadSuccess() {
        XmEntity entity = new XmEntity().avatarUrl("hello.jpg");

        target.postLoad(entity);

        assertEquals(DEFAULT_AVATAR_URL_PREFIX + "/hello.jpg", entity.getAvatarUrl());
    }

}
