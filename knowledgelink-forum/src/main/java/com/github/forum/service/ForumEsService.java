package com.github.forum.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.DeleteResponse;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import com.github.forum.dto.SearchDTO;
import com.github.forum.entity.Forum;
import com.github.forum.entity.ForumEsDocument;
import com.github.forum.mapper.ForumPostMapper;
import com.github.forum.vo.ForumBrowseVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 论坛es服务
 *
 * @author ning
 * @date 2026/04/03
 */
@Service
@RequiredArgsConstructor
public class ForumEsService {
    private final ElasticsearchClient elasticsearchClient;
    private final ForumPostMapper forumPostMapper;

    @Value("${forum.es.post-index:forum_post}")
    private String postIndex;

    public void initIndexAndSync() throws IOException {
        BooleanResponse existsResponse = elasticsearchClient.indices().exists(e -> e.index(postIndex));
        if (!existsResponse.value()) {
            elasticsearchClient.indices().create(c -> c.index(postIndex)
                    .mappings(m -> m
                            .properties("id", p -> p.long_(i -> i))
                            .properties("userId", p -> p.long_(i -> i))
                            .properties("title", p -> p.text(t -> t.analyzer("ik_max_word").searchAnalyzer("ik_smart")))
                            .properties("summary", p -> p.text(t -> t.analyzer("ik_max_word").searchAnalyzer("ik_smart")))
                            .properties("content", p -> p.text(t -> t.analyzer("ik_max_word").searchAnalyzer("ik_smart")))
                            .properties("coverAvatar", p -> p.keyword(k -> k))
                            .properties("label", p -> p.keyword(k -> k))
                            .properties("subject", p -> p.keyword(k -> k))
                            .properties("subClassify", p -> p.keyword(k -> k))
                            .properties("type", p -> p.keyword(k -> k))
                            .properties("visibleRange", p -> p.keyword(k -> k))
                            .properties("pageViews", p -> p.integer(i -> i))
                            .properties("likeCount", p -> p.integer(i -> i))
                            .properties("collectCount", p -> p.integer(i -> i))
                            .properties("commentCount", p -> p.integer(i -> i))
                            .properties("createTime", p -> p.date(d -> d.format("strict_date_optional_time||epoch_millis")))
                            .properties("updateTime", p -> p.date(d -> d.format("strict_date_optional_time||epoch_millis")))));
            syncAllForums();
        }
    }

    public void syncAllForums() throws IOException {
        List<Forum> forums = forumPostMapper.selectList(null);
        if (forums == null || forums.isEmpty()) {
            return;
        }
        BulkRequest.Builder bulkBuilder = new BulkRequest.Builder();
        for (Forum forum : forums) {
            ForumEsDocument document = BeanUtil.copyProperties(forum, ForumEsDocument.class);
            bulkBuilder.operations(op -> op.index(idx -> idx
                    .index(postIndex)
                    .id(String.valueOf(document.getId()))
                    .document(document)));
        }
        BulkResponse bulkResponse = elasticsearchClient.bulk(bulkBuilder.build());
        if (bulkResponse.errors()) {
            throw new RuntimeException("ES全量索引构建失败");
        }
    }

    public void saveForum(Forum forum) throws IOException {
        ForumEsDocument document = BeanUtil.copyProperties(forum, ForumEsDocument.class);
        IndexResponse indexResponse = elasticsearchClient.index(i -> i
                .index(postIndex)
                .id(String.valueOf(document.getId()))
                .document(document));
        if (!"created".equals(indexResponse.result().jsonValue()) && !"updated".equals(indexResponse.result().jsonValue())) {
            throw new RuntimeException("ES写入失败");
        }
    }

    public void deleteForum(Long forumId) throws IOException {
        DeleteResponse deleteResponse = elasticsearchClient.delete(d -> d
                .index(postIndex)
                .id(String.valueOf(forumId)));
        if ("failed".equals(deleteResponse.result().jsonValue())) {
            throw new RuntimeException("ES删除失败");
        }
    }

    public List<ForumBrowseVO> searchForums(SearchDTO searchDTO) throws IOException {
        Query query = buildQuery(searchDTO);
        SearchRequest.Builder requestBuilder = new SearchRequest.Builder()
                .index(postIndex)
                .query(query)
                .size(100);
        if (searchDTO != null && searchDTO.getSort() != null) {
            if (searchDTO.getSort() == 0) {
                requestBuilder.sort(s -> s.field(f -> f.field("createTime").order(SortOrder.Desc)));
            } else if (searchDTO.getSort() == 1) {
                requestBuilder.sort(s -> s.field(f -> f.field("likeCount").order(SortOrder.Desc)));
            } else if (searchDTO.getSort() == 2) {
                requestBuilder.sort(s -> s.field(f -> f.field("collectCount").order(SortOrder.Desc)));
            }
        }
        SearchResponse<ForumEsDocument> response = elasticsearchClient.search(requestBuilder.build(), ForumEsDocument.class);
        List<ForumBrowseVO> result = new ArrayList<>();
        response.hits().hits().forEach(hit -> {
            ForumEsDocument source = hit.source();
            if (source != null) {
                ForumBrowseVO forumBrowseVO = BeanUtil.copyProperties(source, ForumBrowseVO.class);
                forumBrowseVO.setUserName(null);
                forumBrowseVO.setUserAvatar(null);
                result.add(forumBrowseVO);
            }
        });
        return result;
    }

    private Query buildQuery(SearchDTO searchDTO) {
        List<Query> mustQueries = new ArrayList<>();
        List<Query> filterQueries = new ArrayList<>();
        if (searchDTO != null && StrUtil.isNotBlank(searchDTO.getKeyword())) {
            mustQueries.add(Query.of(q -> q.multiMatch(mm -> mm
                    .query(searchDTO.getKeyword())
                    .fields("title^4", "summary^2", "content", "label^2", "subject^2", "subClassify^2"))));
        }
        if (searchDTO != null && StrUtil.isNotBlank(searchDTO.getLabel())) {
            filterQueries.add(Query.of(q -> q.term(t -> t.field("label").value(searchDTO.getLabel()))));
        }
        if (searchDTO != null && StrUtil.isNotBlank(searchDTO.getSubject())) {
            filterQueries.add(Query.of(q -> q.term(t -> t.field("subject").value(searchDTO.getSubject()))));
        }
        if (searchDTO != null && StrUtil.isNotBlank(searchDTO.getSubClassify())) {
            filterQueries.add(Query.of(q -> q.term(t -> t.field("subClassify").value(searchDTO.getSubClassify()))));
        }
        filterQueries.add(Query.of(q -> q.term(t -> t.field("visibleRange").value("公开"))));
        return Query.of(q -> q.bool(b -> b.must(mustQueries).filter(filterQueries)));
    }
}
