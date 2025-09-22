package com.example.youtube_comment_analysis.video;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.netty.handler.timeout.TimeoutException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VideoService {

	private final WebClient webClient;
	private static final String YT_BASE = "https://www.googleapis.com/youtube/v3";
	
	@Value("${youtube.api.key}")
	private String apikey;
	
	private final ObjectMapper mapper = new ObjectMapper();
	
	public VideoResponse getVideoData(String videoId, int limit) {
		try {
			//영상 데이터
			String videoUrl = YT_BASE + "/videos"
                    + "?part=snippet,statistics"
                    + "&id=" + videoId
                    + "&key=" + apikey;
			
			String videoJson = webClient.get()
                    .uri(videoUrl)
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
                String ctUrl = YT_BASE + "/commentThreads"
                        + "?part=snippet,replies"
                        + "&textFormat=plainText"
                        + "&order=time"
                        + "&maxResults=" + pageSize
                        + "&videoId=" + videoId
                        + (pageToken != null ? "&pageToken=" + pageToken : "")
                        + "&key=" + apikey;

                String ctJson = webClient.get()
                        .uri(ctUrl)
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
