package com.icthh.xm.ms.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class PostgresInfrastructureIntTest extends AbstractPostgresIntTest {

    @Autowired
    private EntityManager em;

    @Test
    public void runsOnPostgresAndLoadsDbSearchSpec() {
        pushDbSearchSpec();

        String version = (String) em.createNativeQuery("select version()").getSingleResult();
        assertThat(version).startsWith("PostgreSQL");

        TypeSpec order = xmEntitySpecService.getTypeSpecByKey("ORDER").orElseThrow();
        assertThat(order.findLinkSpec("ORDER.ITEM")).isPresent();
        assertThat(xmEntitySpecService.getTypeSpecByKey("ORDER.EXPRESS")).isPresent();
    }
}
