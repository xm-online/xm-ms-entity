package com.icthh.xm.ms.entity.service;

import static org.elasticsearch.index.query.QueryBuilders.queryStringQuery;

import com.icthh.xm.ms.entity.domain.XmFunction;
import com.icthh.xm.ms.entity.repository.XmFunctionRepository;
import com.icthh.xm.ms.entity.repository.search.XmFunctionSearchRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing XmFunctionServiceImpl.
 */
@Slf4j
@RequiredArgsConstructor
@Service("functionService")
@Transactional
public class XmFunctionServiceImpl implements XmFunctionService {

    private final XmFunctionRepository xmFunctionRepository;
    private final XmFunctionSearchRepository xmFunctionSearchRepository;
    private final XmFunctionExecutorService xmFunctionExecutorService;
    private final XmEntitySpecService xmEntitySpecService;


    /**
     * {@inheritDoc}
     */
    @Override
    public XmFunction save(XmFunction xmFunction) {
        log.debug("Request to save XmFunction : {}", xmFunction);
        XmFunction result = xmFunctionRepository.save(xmFunction);
        xmFunctionSearchRepository.save(result);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Transactional(readOnly = true)
    @Override
    public List<XmFunction> findAll() {
        log.debug("Request to get all XmFunctions");
        return xmFunctionRepository.findAll();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public XmFunction findOne(Long id) {
        log.debug("Request to get XmFunction : {}", id);
        return xmFunctionRepository.findOne(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(Long id) {
        log.debug("Request to delete XmFunction : {}", id);
        xmFunctionRepository.delete(id);
        xmFunctionSearchRepository.delete(id);
    }

    /**
     * {@inheritDoc}
     */
    @Transactional(readOnly = true)
    @Override
    public List<XmFunction> search(String query) {
        log.debug("Request to search XmFunctions for query {}", query);
        return StreamSupport
            .stream(xmFunctionSearchRepository.search(queryStringQuery(query)).spliterator(), false)
            .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public XmFunction execute(String functionKey, Map<String, Object> functionContext) {
        if (!xmEntitySpecService.findFunction(functionKey).isPresent()) {
            throw new IllegalArgumentException("Function not found, function key: " + functionKey);
        }

        Map<String, Object> data = xmFunctionExecutorService.executeFunction(null,
                                                                             null,
                                                                             functionKey,
                                                                             functionContext);
        XmFunction functionResult = new XmFunction();
        functionResult.setTypeKey(functionKey);
        functionResult.setKey(functionKey + "-" + UUID.randomUUID().toString());
        functionResult.setData(data);
        functionResult.setStartDate(Instant.now());
        functionResult.setUpdateDate(functionResult.getStartDate());

        return save(functionResult);
    }

}
