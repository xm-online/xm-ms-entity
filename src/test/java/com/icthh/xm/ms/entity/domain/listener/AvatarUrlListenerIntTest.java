package com.icthh.xm.ms.entity.domain.listener;

import static com.icthh.xm.ms.entity.config.Constants.DEFAULT_AVATAR_URL_PREFIX;

import com.icthh.xm.ms.entity.config.ApplicationProperties;
import com.icthh.xm.ms.entity.AbstractJupiterSpringBootTest;
import com.icthh.xm.ms.entity.domain.XmEntity;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public class AvatarUrlListenerIntTest extends AbstractJupiterSpringBootTest {

    private AvatarUrlListener target;

    @Autowired
    ApplicationProperties applicationProperties;

    @BeforeEach
    public void setup() {
        target = new AvatarUrlListener();
        target.setApplicationProperties(applicationProperties);
        target.init();
    }

    @Test
    public void testPrePersistEmpty() {
        XmEntity entity = new XmEntity().avatarUrl(null);

        target.prePersist(entity);

        Assertions.assertNull(entity.getAvatarUrl());
        Assertions.assertNull(entity.getAvatarUrlRelative());
    }

    @Test
    public void testPrePersistWrongFormat() {
        XmEntity entity = new XmEntity().avatarUrl("hello.jpg");

        target.prePersist(entity);

        Assertions.assertEquals("hello.jpg", entity.getAvatarUrl());
        Assertions.assertEquals("hello.jpg", entity.getAvatarUrlRelative());
    }

    @Test
    public void testPrePersistWrongFormatOther() {
        XmEntity entity = new XmEntity().avatarUrl("http://s3.com/hello.jpg");

        target.prePersist(entity);

        Assertions.assertEquals("http://s3.com/hello.jpg", entity.getAvatarUrl());
        Assertions.assertEquals("http://s3.com/hello.jpg", entity.getAvatarUrlRelative());
    }

    @Test
    public void testPrePersistSuccess() {
        XmEntity entity = new XmEntity().avatarUrl("http://hello.rgw.icthh.test/hello.jpg");

        target.prePersist(entity);

        Assertions.assertEquals("hello.jpg", entity.getAvatarUrlRelative());
        Assertions.assertEquals("http://hello.rgw.icthh.test/hello.jpg", entity.getAvatarUrl());
    }

    @Test
    public void testPrePersistSuccessNginx() {
        XmEntity entity = new XmEntity().avatarUrl("https://hello-rgw.icthh.test/hello.jpg");

        target.prePersist(entity);

        Assertions.assertEquals("hello.jpg", entity.getAvatarUrlRelative());
        Assertions.assertEquals("https://hello-rgw.icthh.test/hello.jpg", entity.getAvatarUrl());
    }

    @Test
    public void testPostLoadEmpty() {
        XmEntity entity = new XmEntity().avatarUrl(null);

        target.postLoad(entity);

        Assertions.assertNull(entity.getAvatarUrl());
        Assertions.assertNull(entity.getAvatarUrlRelative());
    }

    @Test
    public void testPostLoadWrongFormat() {
        XmEntity entity = new XmEntity().avatarUrl("http://s3.hello.amazonaws.com/hello/hello.jpg");

        target.postLoad(entity);

        Assertions.assertEquals("http://s3.hello.amazonaws.com/hello/hello.jpg", entity.getAvatarUrl());
    }

    @Disabled
    @Test
    public void testPostLoadSuccess() {
        XmEntity entity = new XmEntity().avatarUrl("hello.jpg");

        target.postLoad(entity);

        Assertions.assertEquals(DEFAULT_AVATAR_URL_PREFIX + "/hello.jpg", entity.getAvatarUrl());
    }

    @Test
    public void testPostLoadXmeStoreSuccess() {
        XmEntity entity = new XmEntity().avatarUrl(applicationProperties.getObjectStorage().getDbFilePrefix() + "hello.jpg");

        target.postLoad(entity);

        Assertions.assertEquals(applicationProperties.getObjectStorage().getDbUrlTemplate() + "/" + entity.getAvatarUrlRelative(), entity.getAvatarUrl());
    }

}
