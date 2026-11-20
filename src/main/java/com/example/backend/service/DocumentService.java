package com.example.backend.service;

import com.example.backend.exception.BusinessException;
import com.example.backend.model.DocumentInfo;
import com.example.backend.repository.DocumentRepository;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    /** 粗略估算的向量化分块大小（字符数），仅用于给前端展示 chunk 数量。 */
    private static final int CHUNK_SIZE_ESTIMATE = 800;

    private final DocumentRepository documentRepository;
    private final EmbeddingStoreIngestor embeddingStoreIngestor;
    private final EmbeddingStore<TextSegment> embeddingStore;

    public DocumentInfo uploadDocument(String name, String content) {
        if (content == null || content.isBlank()) {
            throw BusinessException.badRequest("文档内容不能为空");
        }
        String id = UUID.randomUUID().toString();

        Metadata metadata = new Metadata();
        metadata.put("document_id", id);
        metadata.put("document_name", name);

        Document document = Document.from(content, metadata);
        embeddingStoreIngestor.ingest(document);

        int chunkCount = (int) Math.max(1, Math.ceil(content.length() / (double) CHUNK_SIZE_ESTIMATE));

        DocumentInfo docInfo = DocumentInfo.builder()
                .id(id)
                .name(name)
                .content(content)
                .chunkCount(chunkCount)
                .createdAt(LocalDateTime.now())
                .build();
        documentRepository.save(docInfo);

        log.info("[Document] uploaded id={}, name={}, chunks≈{}", id, name, chunkCount);
        return docInfo;
    }

    public List<DocumentInfo> listDocuments() {
        return documentRepository.findAll();
    }

    public DocumentInfo requireDocument(String id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("文档不存在"));
    }

    public void deleteDocument(String id) {
        documentRepository.deleteById(id);
        embeddingStore.removeAll(metadataKey("document_id").isEqualTo(id));
        log.info("[Document] deleted id={}", id);
    }
}
