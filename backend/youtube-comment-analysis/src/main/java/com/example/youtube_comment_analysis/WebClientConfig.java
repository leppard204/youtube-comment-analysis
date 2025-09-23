package com.example.youtube_comment_analysis;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import reactor.netty.http.client.HttpClient;

@Configuration
public class WebClientConfig {
	
    @Bean
    public WebClient webClient(WebClient.Builder builder) {
    	HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000) // TCP 연결 타임아웃 (5초)
                .responseTimeout(Duration.ofSeconds(5))              // 응답 타임아웃 (5초)
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(5))   // 읽기 타임아웃 (5초)
                            .addHandlerLast(new WriteTimeoutHandler(5))) // 쓰기 타임아웃 (5초)
                .keepAlive(true); 
    	
    	return builder
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl("https://www.googleapis.com/youtube/v3") // 기본 URL 지정 (선택)
                .defaultHeader("Accept", "application/json")      // 기본 헤더 (선택)
                .build();
    }
}
