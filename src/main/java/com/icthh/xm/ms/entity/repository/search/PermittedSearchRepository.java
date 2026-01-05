package com.icthh.xm.ms.entity.repository.search;

import com.icthh.xm.commons.search.dto.SearchDto;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PermittedSearchRepository {

    <T> List<T> search(String query, Class<T> entityClass, String privilegeKey);

    <T> Page<T> search(String query, Pageable pageable, Class<T> entityClass, String privilegeKey);

    <T> Page<T> searchForPage(SearchDto searchDto, String privilegeKey);

    <T> Page<T> search(Long scrollTimeInMillis,
                      String query,
                      Pageable pageable,
                      Class<T> entityClass,
                      String privilegeKey);
}
