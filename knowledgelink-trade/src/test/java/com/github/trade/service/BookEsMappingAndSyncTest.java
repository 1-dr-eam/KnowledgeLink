package com.github.trade.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

@SpringBootTest
public class BookEsMappingAndSyncTest {
    @Autowired
    private ElasticsearchClient elasticsearchClient;

    @Autowired
    private BookEsService bookEsService;

    @Test
    public void createInitialMapping() throws IOException {
        String indexName = "book_info";
        BooleanResponse existsResponse = elasticsearchClient.indices().exists(e -> e.index(indexName));
        if (existsResponse.value()) {
            elasticsearchClient.indices().delete(d -> d.index(indexName));
        }
        elasticsearchClient.indices().create(c -> c.index(indexName)
                .mappings(m -> m
                        .properties("id", p -> p.long_(i -> i))
                        .properties("sellerId", p -> p.long_(i -> i))
                        .properties("name", p -> p.text(t -> t.analyzer("ik_max_word").searchAnalyzer("ik_smart")))
                        .properties("author", p -> p.text(t -> t.analyzer("ik_max_word").searchAnalyzer("ik_smart")))
                        .properties("publisher", p -> p.text(t -> t.analyzer("ik_max_word").searchAnalyzer("ik_smart")))
                        .properties("version", p -> p.keyword(k -> k))
                        .properties("price", p -> p.double_(d -> d))
                        .properties("type", p -> p.keyword(k -> k))
                        .properties("classify", p -> p.keyword(k -> k))
                        .properties("subClassify", p -> p.keyword(k -> k))
                        .properties("note", p -> p.boolean_(b -> b))
                        .properties("description", p -> p.text(t -> t.analyzer("ik_max_word").searchAnalyzer("ik_smart")))
                        .properties("status", p -> p.integer(i -> i))
                        .properties("avatar", p -> p.keyword(k -> k))
                        .properties("createTime", p -> p.date(d -> d.format("strict_date_optional_time||epoch_millis")))
                        .properties("updateTime", p -> p.date(d -> d.format("strict_date_optional_time||epoch_millis")))));
        BooleanResponse createdResponse = elasticsearchClient.indices().exists(e -> e.index(indexName));
        Assertions.assertTrue(createdResponse.value());
    }

    @Test
    public void syncAllBooksFromDatabaseToEs() throws IOException {
        bookEsService.initIndexAndSync();
    }

    @Test
    public void deleteTestMapping() throws IOException {
        elasticsearchClient.indices().delete(d -> d.index("book_info_mapping_test"));
    }

    @Test
    public void deleteMapping() throws IOException {
        elasticsearchClient.indices().delete(d -> d.index("book_info"));
    }


}
