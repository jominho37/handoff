# 카페 핸드오프 AI 챗봇 개발 기록

## 프로젝트 개요

카페 운영 매뉴얼(`cafe_manual.txt`)을 기반으로 직원들의 질문에 답변하는 RAG(Retrieval Augmented Generation) 챗봇.
Spring Boot 4.0.6 + Spring AI + Google Gemini API로 구성.

### 기술 스택

| 구분 | 기술 |
|------|------|
| Framework | Spring Boot 4.0.6 |
| AI Framework | Spring AI 1.1.0-M1-PLATFORM-2 |
| Chat Model | Gemini 2.5 Flash |
| Embedding Model | Gemini Embedding 2 |
| Vector Store | SimpleVectorStore (파일 캐싱) |
| Build Tool | Gradle |
| Java | 17 |

### 아키텍처

```
사용자 질문
    │
    ▼
ChatController (/api/chat)
    │
    ▼
AiService
    ├── VectorService.search(query)  ← 유사 문서 3개 검색
    │       └── SimpleVectorStore (vector_store.json 캐시)
    │               └── Gemini Embedding 2 (임베딩 생성)
    │
    └── ChatClient.prompt()  ← 검색된 문서 + 질문으로 답변 생성
            └── Gemini 2.5 Flash
```

### 주요 파일 구조

```
src/main/java/com/chatbot/handoff/
├── config/AiConfig.java          # 빈 설정 (ConnectionDetails, EmbeddingModel, VectorStore, ChatClient)
├── controller/ChatController.java # REST API 엔드포인트
├── service/AiService.java         # RAG 로직 (검색 + 답변 생성)
├── service/VectorService.java     # 벡터 유사도 검색
├── runner/ManualDataLoader.java   # 서버 시작 시 매뉴얼 로딩 + 캐싱
└── model/
    ├── ChatRequest.java
    └── ChatResponse.java
```

---

## 시행착오 기록

### 1. NoClassDefFoundError: GoogleGenAiEmbeddingConnectionDetails

**증상**: 서버 시작 시 `java.lang.NoClassDefFoundError: org/springframework/ai/google/genai/GoogleGenAiEmbeddingConnectionDetails`

**원인**: `spring-ai-autoconfigure-model-google-genai` 모듈이 `spring-ai-google-genai-embedding`에 대해 `optional` 의존성을 갖고 있었음. Gradle은 optional 의존성을 자동으로 가져오지 않기 때문에 `GoogleGenAiEmbeddingConnectionDetails` 클래스가 클래스패스에 없었음.

**해결**: `build.gradle`에 누락된 아티팩트를 명시적으로 추가.

```gradle
implementation 'org.springframework.ai:spring-ai-google-genai-embedding:1.1.0-M1-PLATFORM-2'
```

**교훈**: Spring AI의 Google GenAI 모듈은 BOM에 포함되지 않은 실험적 모듈이라 스타터도 없고, 개별 아티팩트를 직접 관리해야 한다.

---

### 2. Google GenAI project-id must be set!

**증상**: `spring.ai.google.genai.project-id`를 YAML에 설정했는데도 임베딩 자동설정에서 project-id를 찾지 못함.

**원인**: Chat과 Embedding의 프로퍼티 prefix가 서로 달랐음.

| 구분 | prefix |
|------|--------|
| Chat | `spring.ai.google.genai` |
| Embedding | `spring.ai.google.genai.embedding` |

최상위에 `project-id`를 넣으면 Chat만 읽고, Embedding은 자기 prefix 하위에서 별도로 찾음.

**해결**: `application.yaml`의 `embedding` 섹션에 `api-key`와 `project-id`를 별도로 추가.

```yaml
embedding:
  api-key: ${GOOGLE_AI_API_KEY}
  project-id: handoff-project
```

---

### 3. 임베딩 모델명이 YAML 설정을 무시하고 text-embedding-004로 고정

**증상**: YAML에 `gemini-embedding-2`를 지정했는데 실제로는 `text-embedding-004`가 사용되어 404 에러 발생.

**원인**: `GoogleGenAiTextEmbeddingOptions.DEFAULT_MODEL_NAME`이 `TEXT_EMBEDDING_004`로 하드코딩되어 있고, 자동설정이 YAML의 model 값을 Options 객체에 제대로 바인딩하지 못함.

**해결**: `AiConfig.java`에서 `GoogleGenAiTextEmbeddingModel` 빈을 직접 생성하여 모델명을 코드 레벨에서 강제 지정.

```java
@Bean
public GoogleGenAiTextEmbeddingModel googleGenAiTextEmbeddingModel(
        GoogleGenAiEmbeddingConnectionDetails connectionDetails) {
    GoogleGenAiTextEmbeddingOptions options = GoogleGenAiTextEmbeddingOptions.builder()
            .model("gemini-embedding-2")
            .build();
    return new GoogleGenAiTextEmbeddingModel(connectionDetails, options);
}
```

---

### 4. API 버전 v1 vs v1beta — 404 에러

**증상**: 모델명을 올바르게 설정해도 계속 404 에러 발생.

**원인**: 두 가지 문제가 동시에 존재.

| 문제 | 상세 |
|------|------|
| API 버전 | Google GenAI SDK 기본값은 `v1beta`인데, `v1`으로 변경 시 임베딩 엔드포인트가 존재하지 않아 404 |
| 모델명 prefix | SDK가 내부적으로 `models/`를 붙이므로, 코드에서 `models/gemini-embedding-2`로 지정하면 `models/models/gemini-embedding-2`가 되어 404 |

**검증**: curl로 직접 확인.

```
v1beta + gemini-embedding-2         → 200 OK
v1     + gemini-embedding-2         → 404
v1beta + models/gemini-embedding-2  → 404 (경로 중복)
```

**해결**:
- API 버전: `v1beta` 유지
- 모델명: `models/` prefix 없이 `gemini-embedding-2`만 지정

```java
Client genAiClient = Client.builder()
        .apiKey(apiKey)
        .httpOptions(HttpOptions.builder()
                .apiVersion("v1beta")
                .build())
        .build();
```

---

### 5. Chat 모델 gemini-1.5-flash 404 에러

**증상**: 임베딩은 성공했으나 채팅 요청 시 `models/gemini-1.5-flash is not found for API version v1beta`.

**원인**: `gemini-1.5-flash`가 deprecated되어 API에서 삭제됨.

**해결**: API로 사용 가능한 모델 목록을 조회하여 `gemini-2.5-flash`로 변경.

```yaml
chat:
  options:
    model: gemini-2.5-flash
```

---

### 6. 무료 티어 할당량(Quota) 소진

**증상**: `429 RESOURCE_EXHAUSTED - Quota exceeded, limit: 0`

**원인**: 서버 시작 시마다 cafe_manual.txt 전체를 청크로 분할하여 임베딩 API를 호출하므로 무료 티어 할당량이 빠르게 소진됨. 또한 특정 모델(gemini-2.0-flash 등)의 일일 할당량이 완전히 소진된 상태.

**해결 (2가지)**:

**(1) 임베딩 결과 파일 캐싱** — `SimpleVectorStore.save()/load()` 활용

```java
File cacheFile = new File("vector_store.json");
if (cacheFile.exists()) {
    vectorStore.load(cacheFile);  // API 호출 0회
    return;
}
// 최초 1회만 임베딩 생성
vectorStore.add(chunks);
vectorStore.save(cacheFile);  // 캐시 저장
```

**(2) 할당량 남은 모델로 전환** — 모델별 할당량이 독립적임을 이용

```bash
gemini-2.0-flash      → 429 (소진)
gemini-2.0-flash-lite → 429 (소진)
gemini-2.5-flash      → 200 (사용 가능!)
```

---

### 7. ApplicationRunner vs @EventListener(ApplicationReadyEvent.class)

**변경 전**: `ApplicationRunner` 인터페이스의 `run()` 메서드로 매뉴얼 로딩.

**변경 후**: `@EventListener(ApplicationReadyEvent.class)` 방식.

**이유**: `ApplicationReadyEvent`는 서버가 완전히 부팅되고 HTTP 요청을 받을 준비가 된 후에 발생하므로, 모든 빈(EmbeddingModel, VectorStore 등)이 초기화된 상태에서 안전하게 실행됨.

---

## 최종 설정 요약

### build.gradle 핵심 의존성

```gradle
// Google GenAI - BOM에 포함되지 않은 실험적 모듈이라 버전 명시 필수
implementation 'org.springframework.ai:spring-ai-google-genai:1.1.0-M1-PLATFORM-2'
implementation 'org.springframework.ai:spring-ai-google-genai-embedding:1.1.0-M1-PLATFORM-2'
implementation 'org.springframework.ai:spring-ai-autoconfigure-model-google-genai:1.1.0-M1-PLATFORM-2'

// BOM이 관리하는 모듈들 (버전 생략)
implementation 'org.springframework.ai:spring-ai-autoconfigure-model-chat-client'
implementation 'org.springframework.ai:spring-ai-vector-store'
implementation 'org.springframework.ai:spring-ai-tika-document-reader'
```

### application.yaml

```yaml
spring:
  ai:
    google:
      genai:
        api-key: ${GOOGLE_AI_API_KEY}     # Chat용
        project-id: handoff-project
        chat:
          options:
            model: gemini-2.5-flash
            temperature: 0.7
        embedding:                         # Embedding은 별도 prefix
          api-key: ${GOOGLE_AI_API_KEY}
          project-id: handoff-project
          options:
            model: gemini-embedding-2
```

### AiConfig.java에서 직접 등록하는 빈 (자동설정 오버라이드)

| 빈 | 이유 |
|----|------|
| `GoogleGenAiEmbeddingConnectionDetails` | API 버전(`v1beta`)과 project-id를 코드에서 직접 제어 |
| `GoogleGenAiTextEmbeddingModel` | 모델명(`gemini-embedding-2`)이 자동설정에서 기본값으로 덮어씌워지는 버그 우회 |
| `SimpleVectorStore` | 파일 캐싱을 위해 구체 타입으로 노출 |
| `ChatClient` | ChatClient.Builder를 통한 표준 생성 |

---

## 운영 참고

### 매뉴얼 갱신 시

`cafe_manual.txt` 내용을 수정한 후 `vector_store.json`을 삭제하고 서버를 재시작하면 임베딩이 재생성됨.

### 무료 티어 할당량 관리

- 분당 한도(RPM) 초과: 1~2분 대기 후 재시도
- 일일 한도(RPD) 소진: 태평양 시간 자정(한국시간 약 오후 4시) 이후 리셋
- 모델별 할당량은 독립적이므로, 한 모델이 소진되면 다른 모델로 전환 가능
