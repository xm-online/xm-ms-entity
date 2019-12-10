package com.icthh.xm.ms.entity.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(basePackages = "com.icthh.xm.ms.entity")
@SuppressWarnings("squid:S2094") // suppress empty class warning
public class FeignConfiguration {

}
