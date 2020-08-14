package com.icthh.xm.ms.entity.service.dto;

import com.icthh.xm.ms.entity.domain.Rating;
import com.icthh.xm.ms.entity.domain.Vote;
import com.icthh.xm.ms.entity.domain.XmEntity;
import java.time.Instant;
import java.util.Optional;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class VoteExportDto {

    private Long id;
    private String userKey;
    private Double value;
    private String message;
    private Instant entryDate;
    private Long ratingId;
    private Long entityId;

    public VoteExportDto(Vote vote) {

        if (vote != null) {
            this.id = vote.getId();
            this.userKey = vote.getUserKey();
            this.value = vote.getValue();
            this.message = vote.getMessage();
            this.entryDate = vote.getEntryDate();
            this.ratingId = Optional.ofNullable(vote.getRating()).map(Rating::getId).orElse(null);
            this.entityId = Optional.ofNullable(vote.getXmEntity()).map(XmEntity::getId).orElse(null);
        }
    }

    public Vote toVote(Rating rating, XmEntity entity) {
        Vote vote = new Vote();
        vote.setUserKey(this.getUserKey());
        vote.setValue(this.getValue());
        vote.setMessage(this.getMessage());
        vote.setEntryDate(this.getEntryDate());
        vote.setRating(rating);
        vote.setXmEntity(entity);
        return vote;
    }
}
