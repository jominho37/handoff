# ☕ Cafe Handoff (카페 핸드오프)

카페 핸드오프 매니저를 위한 RAG 기반 매뉴얼 안내 챗봇 서비스입니다.

---

## 🖥️ 배포 사이트
> **[배포 링크 주소 입력]** (예: Railway 배포 URL)

---

## 🎯 Git Commit Convention
커밋 메시지는 아래의 이모지와 형식을 준수하여 작성합니다.

| 이모지 | 타입 | 설명 |
| :--- | :--- | :--- |
| 🎉 | **Start** | 프로젝트 시작 (`:tada:`) |
| ✨ | **Feat** | 새로운 기능 추가 및 구현 (`:sparkles:`) |
| 🐛 | **Fix** | 버그 수정 및 해결 (`:bug:`) |
| 🎨 | **Design** | UI 디자인 변경 / CSS 수정 (`:art:`) |
| ♻️ | **Refactor** | 코드 구조 개선 / 리팩토링 (`:recycle:`) |
| 🔧 | **Settings** | 환경설정 및 설정 파일 수정 (`:wrench:`) |
| 🗃️ | **Comment** | 필요한 주석 추가 및 변경 (`:card_file_box:`) |
| ➕ | **Dependency** | 라이브러리 및 플러그인 추가 (`:heavy_plus_sign:`) |
| 📝 | **Docs** | 문서 수정 및 추가 (`:memo:`) |
| 🔀 | **Merge** | 브랜치 병합 (`:twisted_rightwards_arrows:`) |
| 🚀 | **Deploy** | 배포 관련 작업 (`:rocket:`) |
| 🚚 | **Rename** | 파일/폴더명 수정 또는 이동 (`:truck:`) |
| 🔥 | **Remove** | 파일 또는 코드 삭제 (`:fire:`) |
| ⏪️ | **Revert** | 이전 버전으로 롤백 (`:rewind:`) |

### 📝 커밋 메시지 형식
> **형식:** `이모지 타입: 작업 내용 요약`
> - 예: `✨ Feat: 로그인 기능 구현`
> - 예: `🐛 Fix: 로그인 오류 해결`

---

## 🌿 Branch Convention (GitHub Flow)

*   **main** : 상용 배포 브랜치. 항상 배포 가능한 상태를 유지합니다.
*   **develop** : 다음 출시 버전을 개발하는 통합 브랜치.
*   **feature/{이름}/{description}** : 새로운 기능을 개발하는 개인 브랜치.
    *   예: `feature/minho/add-login-page`

---

## 🔀 Workflow (작업 흐름)

1.  `develop` 브랜치에서 본인의 `feature` 브랜치를 생성합니다.
2.  작업 완료 후 컨벤션에 맞춰 커밋합니다.
3.  GitHub에서 **Pull Request(PR)**를 생성하여 팀원들의 리뷰를 받습니다.
4.  리뷰 완료 후 `develop` 브랜치로 병합(Merge)합니다.
5.  배포 시점에 `develop` 브랜치를 `main` 브랜치로 병합하여 배포합니다.

### 💡 작업 예시

**1. 새로운 기능 개발 브랜치 생성**
```bash
git checkout -b feature/minho/기능명
