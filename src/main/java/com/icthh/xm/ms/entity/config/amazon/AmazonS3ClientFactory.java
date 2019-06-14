package com.icthh.xm.ms.entity.config.amazon;

import com.amazonaws.services.s3.AmazonS3;

public interface AmazonS3ClientFactory {
    AmazonS3 getAmazonS3();
}
