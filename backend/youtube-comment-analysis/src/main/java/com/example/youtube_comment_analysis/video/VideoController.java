package com.example.youtube_comment_analysis.video;

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

	@GetMapping("/{videoId}")
	public ResponseEntity<?> getVideoData(@PathVariable("videoId") String videoId, 
			@RequestParam(name = "limit", required = false) Integer limit){
		
		int requested=(limit==null? 100 : limit);
		int normalized = Math.min(1000, Math.max(100, requested));
		
		return ResponseEntity.ok(videoService.getVideoData(videoId, normalized));
	}
}
