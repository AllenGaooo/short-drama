package com.bunny.flix.search.config;

import com.bunny.flix.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 服务启动时自动创建短剧 ES 索引（已存在则跳过）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IndexInitRunner implements ApplicationRunner {

    private final SearchService searchService;

    @Override
    public void run(ApplicationArguments args) {
        log.info("开始初始化 ES 短剧索引...");
        searchService.createDramaIndex();
    }
}
