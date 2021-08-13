package com.icthh.xm.ms.entity.config.jsonb;

import io.github.jhipster.domain.util.FixedPostgreSQL95Dialect;

public class CustomPostgreSQL95Dialect extends FixedPostgreSQL95Dialect {

    public CustomPostgreSQL95Dialect() {
        super();
        // register custom jsonb functions here
    }

}
