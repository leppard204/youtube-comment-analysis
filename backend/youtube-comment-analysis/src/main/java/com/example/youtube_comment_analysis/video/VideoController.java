package com.example.youtube_comment_analysis.video;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/video")
public class VideoController {

	private final VideoService videoService;

	// dev 쪽에서 추가된 환경설정 기반 기본 fetch 개수 (미설정 시 1000)
	@Value("${app.youtube.fetch-count:1000}")
	private int fetchCount;

	// 단순 조회: limit 미지정 시 fetchCount 사용, 지정 시 100~fetchCount 범위로 정규화
	@GetMapping("/{videoId}")
	public ResponseEntity<?> getVideoData(@PathVariable("videoId") String videoId,
										  @RequestParam(name = "limit", required = false) Integer limit) {

		int requested  = (limit == null ? fetchCount : limit);
		int normalized = Math.min(fetchCount, Math.max(100, requested));

		return ResponseEntity.ok(videoService.getVideoData(videoId, normalized));
	}

	// 사용자 활동 분석 엔드포인트
	@GetMapping("/{videoId}/analysis")
	public ResponseEntity<AnalysisDto> getVideoAnalysis(@PathVariable("videoId") String videoId) {
		return ResponseEntity.ok(videoService.analyzeCommentsActivity(videoId));
	}
}
