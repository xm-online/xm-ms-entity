package com.icthh.xm.ms.entity.config.health;

import com.icthh.xm.ms.entity.config.ApplicationProperties;
import java.util.concurrent.ExecutionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeClusterOptions;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health.Builder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaAdmin;

@Slf4j
@RequiredArgsConstructor
@Configuration
@ConditionalOnProperty(name = "application.kafkaHealthCheck.enabled", havingValue = "true")
public class KafkaHealthIndicator extends AbstractHealthIndicator {

    private final KafkaAdmin admin;
    private final ApplicationProperties applicationProperties;

    @Override
    protected void doHealthCheck(Builder builder) {
        DescribeClusterOptions describeClusterOptions = new DescribeClusterOptions()
            .timeoutMs(applicationProperties.getKafkaHealthCheck().getConnectionTimeout());

        AdminClient adminClient = AdminClient.create(admin.getConfig());
        DescribeClusterResult describeCluster = adminClient.describeCluster(describeClusterOptions);
        try {
            builder.up()
                .withDetail("clusterId", describeCluster.clusterId().get())
                .withDetail("nodeCount", describeCluster.nodes().get().size())
                .build();
            log.info("Run kafka health check. Result: OK");
        } catch (InterruptedException | ExecutionException e) {
            builder.down()
                .withException(e)
                .build();
        }
    }
}
