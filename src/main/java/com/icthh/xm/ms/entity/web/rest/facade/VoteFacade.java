package com.icthh.xm.ms.entity.web.rest.facade;

import com.icthh.xm.ms.entity.domain.Vote;
import com.icthh.xm.ms.entity.service.VoteService;
import com.icthh.xm.ms.entity.service.dto.VoteDto;
import com.icthh.xm.ms.entity.service.mapper.VoteMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VoteFacade {

    private final VoteService voteService;
    private final VoteMapper voteMapper;

    public VoteDto save(VoteDto dto) {
        Vote entity = voteMapper.toEntity(dto);
        Vote saved = voteService.save(entity);
        return voteMapper.toDto(saved);
    }

    public Page<VoteDto> findAll(Pageable pageable, String privilegeKey) {
        return voteService.findAll(pageable, privilegeKey).map(voteMapper::toDto);
    }

    public VoteDto findOne(Long id) {
        return voteMapper.toDto(voteService.findOne(id));
    }

    public void delete(Long id) {
        voteService.delete(id);
    }
}
