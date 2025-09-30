package com.example.youtube_comment_analysis;

import java.util.List;

public record AiSentimentRequest(List<Comment>comments, Trace trace) {
	public record Comment(String id, String text) {}
	public record Trace(String requestId, String analysisETag) {}
}
