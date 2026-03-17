package com.github.trade.service;

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
import com.github.trade.dto.BookDTO;
import com.github.trade.dto.BookSearchDTO;
import com.github.trade.entity.Book;
import com.github.trade.entity.BookEsDocument;
import com.github.trade.mapper.BookMapper;
import com.github.trade.util.BookConversionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 书籍ES索引操作层
 *
 * @author ning
 * @date 2026/03/16
 */
@Service
@RequiredArgsConstructor
public class BookEsService {
    private final ElasticsearchClient elasticsearchClient;
    private final BookMapper bookMapper;
    private final BookConversionUtil bookConversionUtil;

    @Value("${trade.es.book-index:book_info}")
    private String bookIndex;

    public void initIndexAndSync() throws IOException {
        BooleanResponse existsResponse = elasticsearchClient.indices().exists(e -> e.index(bookIndex));
        if (!existsResponse.value()) {
            elasticsearchClient.indices().create(c -> c.index(bookIndex)
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
            syncAllBooks();
        }
    }

    public void syncAllBooks() throws IOException {
        List<Book> books = bookMapper.selectList(null);
        if (books == null || books.isEmpty()) {
            return;
        }
        BulkRequest.Builder bulkBuilder = new BulkRequest.Builder();
        for (Book book : books) {
            BookEsDocument document = BeanUtil.copyProperties(book, BookEsDocument.class);
            bulkBuilder.operations(op -> op.index(idx -> idx
                    .index(bookIndex)
                    .id(String.valueOf(document.getId()))
                    .document(document)));
        }
        BulkResponse bulkResponse = elasticsearchClient.bulk(bulkBuilder.build());
        if (bulkResponse.errors()) {
            throw new RuntimeException("ES全量索引构建失败");
        }
    }

    public void saveBook(Book book) throws IOException {
        BookEsDocument document = BeanUtil.copyProperties(book, BookEsDocument.class);
        IndexResponse indexResponse = elasticsearchClient.index(i -> i
                .index(bookIndex)
                .id(String.valueOf(document.getId()))
                .document(document));
        if (!"created".equals(indexResponse.result().jsonValue()) && !"updated".equals(indexResponse.result().jsonValue())) {
            throw new RuntimeException("ES写入失败");
        }
    }

    public void deleteBook(Integer id) throws IOException {
        DeleteResponse deleteResponse = elasticsearchClient.delete(d -> d
                .index(bookIndex)
                .id(String.valueOf(id)));
        if ("failed".equals(deleteResponse.result().jsonValue())) {
            throw new RuntimeException("ES删除失败");
        }
    }

    public List<BookDTO> searchBooks(BookSearchDTO bookSearchDTO) throws IOException {
        Query query = buildQuery(bookSearchDTO);
        SearchRequest.Builder requestBuilder = new SearchRequest.Builder()
                .index(bookIndex)
                .query(query)
                .size(100);
        if (bookSearchDTO.getSort() != null) {
            if (bookSearchDTO.getSort() == 1) {
                requestBuilder.sort(s -> s.field(f -> f.field("price").order(SortOrder.Asc)));
            } else if (bookSearchDTO.getSort() == 2) {
                requestBuilder.sort(s -> s.field(f -> f.field("price").order(SortOrder.Desc)));
            }
        }
        SearchResponse<BookEsDocument> response = elasticsearchClient.search(requestBuilder.build(), BookEsDocument.class);
        List<BookDTO> result = new ArrayList<>();
        response.hits().hits().forEach(hit -> {
            BookEsDocument source = hit.source();
            if (source != null) {
                Book book = BeanUtil.copyProperties(source, Book.class);
                BookDTO bookDTO = bookConversionUtil.toBookDTO(book);
                result.add(bookDTO);
            }
        });
        return result;
    }

    private Query buildQuery(BookSearchDTO bookSearchDTO) {
        List<Query> mustQueries = new ArrayList<>();
        List<Query> filterQueries = new ArrayList<>();
        if (bookSearchDTO != null && StrUtil.isNotBlank(bookSearchDTO.getSearchKeyword())) {
            mustQueries.add(Query.of(q -> q.multiMatch(mm -> mm
                    .query(bookSearchDTO.getSearchKeyword())
                    .fields("name^4", "author^2", "publisher^2", "classify^2", "subClassify^2", "description"))));
        }
        if (bookSearchDTO != null && bookSearchDTO.getNote() != null) {
            filterQueries.add(Query.of(q -> q.term(t -> t.field("note").value(bookSearchDTO.getNote()))));
        }
        if (bookSearchDTO != null && bookSearchDTO.getType() != null) {
            String typeText = bookSearchDTO.getType();
            if (StrUtil.isNotBlank(typeText)) {
                filterQueries.add(Query.of(q -> q.term(t -> t.field("type").value(typeText))));
            }
        }
        if (mustQueries.isEmpty() && filterQueries.isEmpty()) {
            return Query.of(q -> q.matchAll(m -> m));
        }
        return Query.of(q -> q.bool(b -> b.must(mustQueries).filter(filterQueries)));
    }
}
