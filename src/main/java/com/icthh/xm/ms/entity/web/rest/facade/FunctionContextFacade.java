package com.icthh.xm.ms.entity.web.rest.facade;

import com.icthh.xm.ms.entity.domain.FunctionContext;
import com.icthh.xm.ms.entity.service.FunctionContextService;
import com.icthh.xm.ms.entity.service.dto.FunctionContextDto;
import com.icthh.xm.ms.entity.service.mapper.FunctionContextMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FunctionContextFacade {

    private final FunctionContextService functionContextService;
    private final FunctionContextMapper functionContextMapper;

    public FunctionContextDto save(FunctionContextDto dto) {
        FunctionContext entity = functionContextMapper.toEntity(dto);
        FunctionContext saved = functionContextService.save(entity);
        return functionContextMapper.toDto(saved);
    }

    public List<FunctionContextDto> findAll(String privilegeKey) {
        return functionContextService.findAll(privilegeKey).stream()
            .map(functionContextMapper::toDto)
            .toList();
    }

    public FunctionContextDto findOne(Long id) {
        return functionContextMapper.toDto(functionContextService.findOne(id));
    }

    public void delete(Long id) {
        functionContextService.delete(id);
    }
}
