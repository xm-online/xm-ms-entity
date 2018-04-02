package com.icthh.xm.ms.entity.web.rest;

import com.codahale.metrics.annotation.Timed;
import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.exceptions.ErrorConstants;
import com.icthh.xm.ms.entity.domain.Vote;
import com.icthh.xm.ms.entity.service.VoteService;
import com.icthh.xm.ms.entity.web.rest.util.HeaderUtil;
import com.icthh.xm.ms.entity.web.rest.util.PaginationUtil;
import com.icthh.xm.ms.entity.web.rest.util.RespContentUtil;
import io.swagger.annotations.ApiParam;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import javax.validation.Valid;

/**
 * REST controller for managing Vote.
 */
@RestController
@RequestMapping("/api")
public class VoteResource {

    private static final String ENTITY_NAME = "vote";

    private final VoteResource voteResource;
    private final VoteService voteService;

    public VoteResource(
                    @Lazy VoteResource voteResource,
                    VoteService voteService) {
        this.voteResource = voteResource;
        this.voteService = voteService;
    }

    /**
     * POST  /votes : Create a new vote.
     *
     * @param vote the vote to create
     * @return the ResponseEntity with status 201 (Created) and with body the new vote, or with status 400 (Bad Request) if the vote has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/votes")
    @Timed
    @PreAuthorize("hasPermission({'vote': #vote}, 'VOTE.CREATE')")
    public ResponseEntity<Vote> createVote(@Valid @RequestBody Vote vote) throws URISyntaxException {
        if (vote.getId() != null) {
            throw new BusinessException(ErrorConstants.ERR_BUSINESS_IDEXISTS,
                                        "A new vote cannot already have an ID");
        }
        Vote result = voteService.save(vote);
        return ResponseEntity.created(new URI("/api/votes/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * PUT  /votes : Updates an existing vote.
     *
     * @param vote the vote to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated vote,
     * or with status 400 (Bad Request) if the vote is not valid,
     * or with status 500 (Internal Server Error) if the vote couldn't be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping("/votes")
    @Timed
    @PreAuthorize("hasPermission({'id': #vote.id, 'newVote': #vote}, 'vote', 'VOTE.UPDATE')")
    public ResponseEntity<Vote> updateVote(@Valid @RequestBody Vote vote) throws URISyntaxException {
        if (vote.getId() == null) {
            //in order to call method with permissions check
            return this.voteResource.createVote(vote);
        }
        Vote result = voteService.save(vote);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(ENTITY_NAME, vote.getId().toString()))
            .body(result);
    }

    /**
     * GET  /votes : get all the votes.
     *
     * @param pageable the pagination information
     * @return the ResponseEntity with status 200 (OK) and the list of votes in body
     */
    @GetMapping("/votes")
    @Timed
    public ResponseEntity<List<Vote>> getAllVotes(@ApiParam Pageable pageable) {
        Page<Vote> page = voteService.findAll(pageable, null);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/votes");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    /**
     * GET  /votes/:id : get the "id" vote.
     *
     * @param id the id of the vote to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the vote, or with status 404 (Not Found)
     */
    @GetMapping("/votes/{id}")
    @Timed
    @PostAuthorize("hasPermission({'returnObject': returnObject.body}, 'VOTE.GET_LIST.ITEM')")
    public ResponseEntity<Vote> getVote(@PathVariable Long id) {
        Vote vote = voteService.findOne(id);
        return RespContentUtil.wrapOrNotFound(Optional.ofNullable(vote));
    }

    /**
     * DELETE  /votes/:id : delete the "id" vote.
     *
     * @param id the id of the vote to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/votes/{id}")
    @Timed
    @PreAuthorize("hasPermission({'id': #id}, 'vote', 'VOTE.DELETE')")
    public ResponseEntity<Void> deleteVote(@PathVariable Long id) {
        voteService.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, id.toString())).build();
    }

    /**
     * SEARCH  /_search/votes?query=:query : search for the vote corresponding
     * to the query.
     *
     * @param query the query of the vote search
     * @param pageable the pagination information
     * @return the result of the search
     */
    @GetMapping("/_search/votes")
    @Timed
    public ResponseEntity<List<Vote>> searchVotes(@RequestParam String query, @ApiParam Pageable pageable) {
        Page<Vote> page = voteService.search(query, pageable, null);
        HttpHeaders headers = PaginationUtil.generateSearchPaginationHttpHeaders(query, page, "/api/_search/votes");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

}
