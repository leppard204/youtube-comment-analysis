package com.example.youtube_comment_analysis.video;

import java.util.List;

import lombok.Data;

@Data
public class VideoResponse {
	
    private String videoId;
    private String title;
    private String channelId;
    private String channelTitle;
    private String publishedAt;
    private Long viewCount;
    private Long likeCount;
    private Long commentCount;
    private List<CommentDto> comments;
}
