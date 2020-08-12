package com.icthh.xm.ms.entity.service.dto;

import com.icthh.xm.ms.entity.domain.Calendar;
import com.icthh.xm.ms.entity.domain.Comment;
import com.icthh.xm.ms.entity.domain.Event;
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
public class CommentExportDto {

    private Long id;
    private String userKey;
    private String message;
    private Instant entryDate;
    private Long commentId;
    private Long entityId;

    public CommentExportDto(Comment comment) {
        if (comment != null) {
            this.id = comment.getId();
            this.userKey = comment.getUserKey();
            this.message = comment.getMessage();
            this.entryDate = comment.getEntryDate();
            this.commentId = Optional.ofNullable(comment.getComment()).map(Comment::getId).orElse(null);
            this.entityId = Optional.ofNullable(comment.getXmEntity()).map(XmEntity::getId).orElse(null);
        }
    }

    public Comment toComment(XmEntity entity, Comment assigned) {
        Comment comment = new Comment();
        this.id = comment.getId();
        comment.setUserKey(this.getUserKey());
        comment.setMessage(this.getMessage());
        comment.setEntryDate(this.getEntryDate());
        comment.setComment(assigned);
        comment.setXmEntity(entity);
        return comment;
    }

}
