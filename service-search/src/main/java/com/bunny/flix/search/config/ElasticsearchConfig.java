package com.bunny.flix.search.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;

/**
 * Elasticsearch 配置类（ES 8.x Java API Client）
 * uris 格式支持带协议头，如 http://host1:9201,http://host2:9201
 */
@Configuration
public class ElasticsearchConfig {

    /** ES节点地址，多个逗号分隔 */
    @Value("${spring.elasticsearch.uris}")
    private String uris;

    /** 创建 Elasticsearch 8.x 客户端 */
    @Bean
    public ElasticsearchClient elasticsearchClient() {
        String[] hosts = uris.split(",");
        HttpHost[] httpHosts = new HttpHost[hosts.length];
        for (int i = 0; i < hosts.length; i++) {
            String raw = hosts[i].trim();
            // 兼容带协议头(http://)与不带协议头的写法
            URI uri = URI.create(raw.contains("://") ? raw : "http://" + raw);
            String scheme = uri.getScheme() == null ? "http" : uri.getScheme();
            int port = uri.getPort() == -1 ? 9200 : uri.getPort();
            httpHosts[i] = new HttpHost(uri.getHost(), port, scheme);
        }
        RestClient restClient = RestClient.builder(httpHosts).build();
        ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        return new ElasticsearchClient(transport);
    }
}
