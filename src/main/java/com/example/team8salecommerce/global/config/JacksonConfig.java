package com.example.team8salecommerce.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Jackson ObjectMapper 설정
 *ㅍ
 * Redis Pub/Sub 메시지 직렬화 등 애플리케이션 내부에서
 * ObjectMapper를 주입받아 사용할 수 있도록 Bean으로 등록한다.
 */
@Configuration
public class JacksonConfig {

	/**
	 * Java LocalDateTime 같은 날짜 타입도 JSON으로 처리할 수 있는 ObjectMapper를 등록한다.
	 *
	 * @return 애플리케이션 공용 ObjectMapper
	 */
	@Bean
	public ObjectMapper objectMapper() {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.registerModule(new JavaTimeModule());
		objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		return objectMapper;
	}
}
