package com.icthh.xm.ms.entity.repository;

import com.icthh.xm.commons.permission.repository.PermittedRepository;
import com.icthh.xm.commons.permission.service.PermissionCheckService;
import com.icthh.xm.ms.entity.domain.Link;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

/**
 * Permitted repository for Link entity.
 */
@Repository
public class LinkPermittedRepository extends PermittedRepository {

    public LinkPermittedRepository(final PermissionCheckService permissionCheckService) {
        super(permissionCheckService);
    }

    public Page<Link> findAllByTargetIdAndTypeKeyIn(Pageable pageable, Long targetId, Set<String> typeKeys,
                                                    String privilegeKey) {
        String whereCondition = "target.id = :targetId";

        Map<String, Object> conditionParams = new HashMap<>();
        conditionParams.put("targetId", targetId);

        if (CollectionUtils.isNotEmpty(typeKeys)) {
            conditionParams.put("typeKeys", typeKeys);
            whereCondition += " and typeKey in (:typeKeys)";
        }

        return findByCondition(whereCondition, conditionParams, pageable, getType(), privilegeKey);
    }

    private Class<Link> getType() {
        return Link.class;
    }

}
