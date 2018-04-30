package com.icthh.xm.ms.entity.repository;

import com.icthh.xm.commons.permission.repository.PermittedRepository;
import com.icthh.xm.commons.permission.service.PermissionCheckService;
import com.icthh.xm.ms.entity.domain.XmEntity;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

@Repository
public class XmEntityPermittedRepository extends PermittedRepository {

    public XmEntityPermittedRepository(PermissionCheckService permissionCheckService) {
        super(permissionCheckService);
    }

    /**
     * Find all permitted xm entities by type key in.
     * @param pageable the page info
     * @param typeKeys the type keys
     * @param privilegeKey the privilege key
     * @return permitted xm entities
     */
    public Page<XmEntity> findAllByTypeKeyIn(Pageable pageable, Set<String> typeKeys, String privilegeKey) {
        String whereCondition = "typeKey in (:typeKeys)";

        Map<String, Object> conditionParams = Collections.singletonMap("typeKeys",
            CollectionUtils.isEmpty(typeKeys) ? null : typeKeys);

        return findByCondition(whereCondition, conditionParams, pageable, getType(), privilegeKey);
    }

    public Page<XmEntity> findAllByIdsWithEmbed(Pageable pageable, Set<Long> ids, Set<String> embed, String privilegeKey) {
        String whereCondition = "id in (:ids)";

        Map<String, Object> conditionParams = Collections.singletonMap("ids",
            CollectionUtils.isEmpty(ids) ? null : ids);

        return findByCondition(whereCondition, conditionParams, embed, pageable, getType(), privilegeKey);
    }

    private Class<XmEntity> getType() {
        return XmEntity.class;
    }
}
