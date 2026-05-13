package com.chatbot.handoff.runner;

import java.io.File;
import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ManualDataLoader {

    private static final String VECTOR_STORE_FILE = "vector_store.json";

    private final SimpleVectorStore vectorStore;

    @Value("file:cafe_manual.txt")
    private Resource cafeManualResource;

    @EventListener(ApplicationReadyEvent.class)
    public void loadManualData() {
        File cacheFile = new File(VECTOR_STORE_FILE);

        if (cacheFile.exists()) {
            vectorStore.load(cacheFile);
            log.info("캐시 파일에서 벡터 스토어 로드 완료: {}", cacheFile.getAbsolutePath());
            return;
        }

        log.info("캐시 파일 없음 — 임베딩 생성 시작");
        TextReader textReader = new TextReader(cafeManualResource);
        List<Document> documents = textReader.get();

        TokenTextSplitter splitter = new TokenTextSplitter();
        List<Document> chunks = splitter.apply(documents);

        vectorStore.add(chunks);
        vectorStore.save(cacheFile);
        log.info("cafe_manual.txt 로딩 완료 — {}개 청크 저장 및 캐시됨", chunks.size());
    }
}
