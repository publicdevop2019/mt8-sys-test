package com.hw.helper;

import lombok.Data;

import java.util.Date;

@Data
public class CommentPrivateCard {
    private Long id;
    private String content;
    private String replyTo;
    private Date publishedAt;

}
