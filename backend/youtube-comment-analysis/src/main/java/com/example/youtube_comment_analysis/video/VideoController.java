package com.example.youtube_comment_analysis.video;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/video")
public class VideoController {
	
	private final VideoService videoService;
	
	@Value("${app.youtube.fetch-count:1000}")
    private int fetchCount;

	@GetMapping("/{videoId}")
	public ResponseEntity<?> getVideoData(@PathVariable("videoId") String videoId){
		return ResponseEntity.ok(videoService.getVideoData(videoId, fetchCount));
	}
}
