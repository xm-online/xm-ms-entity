package com.icthh.xm.ms.entity.service.dto;

import com.icthh.xm.ms.entity.domain.Rating;
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
public class RatingExportDto {

    private Long id;
    private String typeKey;
    private Double value;
    private Instant startDate;
    private Instant endDate;
    private Long entityId;


    public RatingExportDto(Rating rating) {

        if (rating != null) {
            this.id = rating.getId();
            this.typeKey = rating.getTypeKey();
            this.value = rating.getValue();
            this.startDate = rating.getStartDate();
            this.endDate = rating.getEndDate();
            this.entityId = Optional.ofNullable(rating.getXmEntity()).map(XmEntity::getId).orElse(null);
        }
    }

    public Rating toRating(XmEntity entity) {
        Rating rating = new Rating();
        rating.setTypeKey(this.getTypeKey());
        rating.setValue(this.getValue());
        rating.setStartDate(this.getStartDate());
        rating.setEndDate(this.getEndDate());
        rating.setXmEntity(entity);
        return rating;
    }
}
