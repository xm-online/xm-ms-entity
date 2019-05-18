package com.icthh.xm.ms.entity.domain.listener;

import static org.junit.Assert.assertEquals;

import com.icthh.xm.ms.entity.AbstractSpringBootTest;
import com.icthh.xm.ms.entity.domain.XmEntity;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Ignore;
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

        assertEquals(null, entity.getAvatarUrl());
    }

    @Test
    public void testPrePersistWrongFormat() {
        XmEntity entity = new XmEntity().avatarUrl("hello.jpg");

        target.prePersist(entity);

        assertEquals("hello.jpg", entity.getAvatarUrl());
    }

    @Test
    public void testPrePersistSuccess() {
        XmEntity entity = new XmEntity().avatarUrl("http://hello.rgw.icthh.test/hello.jpg");

        target.prePersist(entity);

        assertEquals("hello.jpg", entity.getAvatarUrlRelative());
    }

    @Test
    public void testPrePersistSuccessNginx() {
        XmEntity entity = new XmEntity().avatarUrl("https://hello-rgw.icthh.test/hello.jpg");

        target.prePersist(entity);

        assertEquals("hello.jpg", entity.getAvatarUrlRelative());
    }

    @Test
    public void testPostLoadEmpty() {
        XmEntity entity = new XmEntity().avatarUrl(null);

        target.postLoad(entity);

        assertEquals(null, entity.getAvatarUrl());
    }

    @Test
    public void testPostLoadWrongFormat() {
        XmEntity entity = new XmEntity().avatarUrl("http://s3.hello.amazonaws.com/hello/hello.jpg");

        target.postLoad(entity);

        assertEquals("http://s3.hello.amazonaws.com/hello/hello.jpg", entity.getAvatarUrl());
    }

    @Ignore
    @Test
    public void testPostLoadSuccess() {
        XmEntity entity = new XmEntity().avatarUrl("hello.jpg");

        target.postLoad(entity);

        assertEquals("http://hello.rgw.icthh.test/hello.jpg", entity.getAvatarUrl());
    }

}
