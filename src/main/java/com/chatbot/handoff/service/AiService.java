package com.chatbot.handoff.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import com.chatbot.handoff.service.SemanticCacheService.CacheResult;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AiService {

    private static final String SYSTEM_PROMPT = """
            너는 '카페 핸드오프'의 AI 매니저야.
            아래 [Context]는 카페 운영 매뉴얼에서 발췌한 내용이고, 이것이 네가 참고할 유일한 사실이야.

            규칙:
            1. 반드시 [Context]에 있는 정보만으로 답변해.
            2. [Context]에 관련 내용이 없으면 절대 지어내지 말고, 반드시 다음 문장만 출력해:
               "죄송합니다. 해당 내용은 매뉴얼에 없습니다. 점장님께 확인해 주세요!"
            3. 친절하고 간결하게 답변해.
            """;

    private final ChatClient chatClient;
    private final VectorService vectorService;
    private final SemanticCacheService semanticCacheService;

    public String generateAnswer(String userQuery) {
        CacheResult cacheResult = semanticCacheService.lookup(userQuery);
        if (cacheResult.isHit()) {
            return cacheResult.answer();
        }

        List<Document> docs = vectorService.search(userQuery);
        String context = docs.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n"));

        String answer = chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(String.format("[Context]\n%s\n\n[질문]\n%s", context, userQuery))
                .call()
                .content();

        semanticCacheService.cacheAnswer(cacheResult.embedding(), answer);
        return answer;
    }
}
