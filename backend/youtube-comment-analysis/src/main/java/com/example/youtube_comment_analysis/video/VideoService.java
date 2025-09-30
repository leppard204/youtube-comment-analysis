package com.example.youtube_comment_analysis.video;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import com.example.youtube_comment_analysis.AiSender;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.netty.handler.timeout.TimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class VideoService {
	
	private final WebClient yt;
	private final AiSender aiSender;
	
	public VideoService(@Qualifier("youtubeWebClient") WebClient yt, AiSender aiSender) {
        this.yt = yt;
        this.aiSender = aiSender;
    }
	
	@Value("${youtube.api.key}")
	private String apikey;
	
	private final ObjectMapper mapper = new ObjectMapper();
	
	public VideoResponse getVideoData(String videoId, int limit) {
		try {
			//영상 데이터
			String videoJson = yt.get()
                    .uri(b->b.path("/videos")
                    		.queryParam("part", "snippet,statistics")
                    		.queryParam("id", videoId)
                    		.queryParam("key", apikey)
                    		.build())
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, res ->
                    res.bodyToMono(String.class)
                       .map(body -> new RuntimeException("클라이언트 오류 (API Key, 권한 등): " + body)))
                    .onStatus(HttpStatusCode::is5xxServerError, res ->
                    res.bodyToMono(String.class)
                       .map(body -> new RuntimeException("유튜브 서버 오류: " + body)))
                    .bodyToMono(String.class)
                    .block();
			
			JsonNode vroot = mapper.readTree(videoJson);
			JsonNode items = vroot.path("items");
			
			if (!items.isArray() || items.size() == 0) {
                throw new IllegalArgumentException("영상 없음 : " + videoId);
            }
			
			JsonNode v0 = items.get(0);
            JsonNode snippet = v0.path("snippet");
            JsonNode stats = v0.path("statistics");
            
            VideoResponse resp = new VideoResponse();
            
            resp.setVideoId(videoId);
            resp.setTitle(snippet.path("title").asText(null));
            resp.setChannelId(snippet.path("channelId").asText(null));
            resp.setChannelTitle(snippet.path("channelTitle").asText(null));
            resp.setPublishedAt(snippet.path("publishedAt").asText(null));
            resp.setViewCount(stats.path("viewCount").isMissingNode() ? null : stats.path("viewCount").asLong());
            resp.setLikeCount(stats.path("likeCount").isMissingNode() ? null : stats.path("likeCount").asLong());
            resp.setCommentCount(stats.path("commentCount").isMissingNode() ? null : stats.path("commentCount").asLong());
            
            //댓글 데이터
            List<CommentDto> comments = new ArrayList<>();
            String pageToken = null;
            int remain = Math.max(0, limit);
            
            while (remain > 0) {
                int pageSize = Math.min(100, remain);  // 100건 씩 돌면서 갖고옴
                final String token = pageToken;
                String ctJson = yt.get()
                        .uri(b->b.path("/commentThreads")
                        		.queryParam("part", "snippet,replies")
                        		.queryParam("textFormat", "plainText")
                                .queryParam("order", "time")
                                .queryParam("maxResults", pageSize)
                                .queryParam("videoId", videoId)
                                .queryParam("key", apikey)
                                .queryParamIfPresent("pageToken", Optional.ofNullable(token))
                                .build())
                        .retrieve()
                        .onStatus(HttpStatusCode::is4xxClientError, res ->
                        res.bodyToMono(String.class)
                           .map(body -> new RuntimeException("댓글 조회 오류: " + body)))
                        .onStatus(HttpStatusCode::is5xxServerError, res ->
                        res.bodyToMono(String.class)
                           .map(body -> new RuntimeException("댓글 서버 오류: " + body)))
                        .bodyToMono(String.class)
                        .block();

                JsonNode croot = mapper.readTree(ctJson);
                JsonNode citems = croot.path("items");

                if (citems.isArray()) {
                    for (JsonNode it : citems) {
                        JsonNode top = it.path("snippet").path("topLevelComment");
                        String commentId = top.path("id").asText();
                        JsonNode cs = top.path("snippet");
                        String author = cs.path("authorDisplayName").asText(null);
                        String text = cs.path("textDisplay").asText(null);
                        long likeCount = cs.path("likeCount").asLong(0);
                        String publishedAt = cs.path("publishedAt").asText(null);

                        comments.add(new CommentDto(
                                commentId,
                                author,
                                text,
                                likeCount,
                                publishedAt
                        ));
                    }
                }

                pageToken = croot.path("nextPageToken").isMissingNode() ? null : croot.path("nextPageToken").asText(null);
                remain -= pageSize;

                if (pageToken == null) 
                	break; 
            }
            var commentList = comments.stream()
            		.map(c->new AiSender.CommentLite(c.getCommentId(), c.getText()))
            		.toList();
            var sendResult = aiSender.send(commentList);
            log.info("FastAPI sendOnly result: success={}, clientError={}, otherError={}",
                    sendResult.success(), sendResult.clientError(), sendResult.otherError());
            
            resp.setComments(comments);
            return resp;
		}
		catch (WebClientRequestException e) {
	        throw new RuntimeException("네트워크 오류: " + e.getMessage(), e);
		}
		catch (TimeoutException e) {
	        throw new RuntimeException("YouTube API 응답 지연", e);
	    }
		catch(Exception e){
			throw new RuntimeException("Failed to fetch YouTube data: " + e.getMessage(), e);
		}
	}
}
