/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.springai.chat.config;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * DashScope API 自动配置类
 * 用于创建 DashScopeApi Bean，支持知识库检索功能
 * 
 * @author yanwenjie
 */
@Slf4j
@Configuration
public class BailianAutoconfiguration {

	@Value("${spring.ai.dashscope.api-key:}")
	private String apiKey;

	/**
	 * 创建 DashScopeApi Bean
	 * 从配置文件读取 API Key，如果未配置则不创建 Bean（应用仍可正常启动）
	 * 
	 * @return DashScopeApi 实例，如果 API Key 未配置则返回 null
	 */
	@Bean
	@ConditionalOnProperty(name = "spring.ai.dashscope.api-key")
	public DashScopeApi dashScopeApi() {
		if (apiKey == null || apiKey.trim().isEmpty()) {
			log.warn("DashScope API Key 未配置，DashScopeApi Bean 将不会被创建");
			return null;
		}
		
		try {
			DashScopeApi api = DashScopeApi.builder()
					.apiKey(apiKey)
					.build();
			log.info("DashScopeApi Bean 创建成功 - API Key 已配置");
			return api;
		} catch (Exception e) {
			log.error("创建 DashScopeApi Bean 失败", e);
			return null;
		}
	}

}
