package com.icthh.xm.ms.entity.config.amazon;


import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@LepService(group = "service.s3")
public class AmazonS3BucketNameFactory {

    @LogicExtensionPoint("PrepareBucketName")
    public String prepareBucketName(String bucketPrefix, String bucket) {
        String formatted;
        if (StringUtils.isBlank(bucketPrefix)) {
            formatted = prepareString(bucket);
        } else {
            formatted = prepareString(bucketPrefix)+ "-" + prepareString(bucket);
        }
        log.info("Formatted bucket name: {}", formatted);
        return formatted;
    }

    private String prepareString(String str) {
        return str.toLowerCase().replace("_", "-");
    }

}
