# RE:VIEW Backend

**RE:VIEW**는 면접 연습을 기록하고 피드백을 나누는 서비스입니다. 이 저장소는 면접 진행, 답변 처리, 커뮤니티와 사용자 기록을 담당하는 백엔드 API를 담고 있습니다.

## 주요 기능

- 자기소개서와 희망 직무를 바탕으로 면접 질문 생성
- 녹음 답변의 음성 변환 및 질문별 피드백 제공
- 다른 사용자의 답변 평가와 면접 경험 공유
- 면접 기록, 받은 피드백, 프로필 조회 및 관리
- 카카오 로그인

## 기술 구성

| 영역 | 기술 |
| --- | --- |
| 서버 | Java 17, Spring Boot, Spring Security, Spring Data JPA |
| 데이터 | MySQL, Redis |
| 파일·AI | AWS S3, Amazon Bedrock, Apache Tika, faster-whisper |
| 배포 | Docker, GitHub Actions, Amazon ECR·ECS |

## 코드 구조

```text
src/main/java/com/dgu/review/
├── domain/
│   ├── interview/       # 면접과 답변 처리
│   ├── peerfeedback/    # 동료 평가
│   ├── community/       # 커뮤니티
│   ├── myarchive/       # 내 기록과 프로필
│   └── user/            # 사용자
└── global/              # 공통 설정, 인증, 예외 처리
stt_worker/              # 음성 변환 스크립트
```

프런트엔드 코드는 [ReView_FE](https://github.com/DGU-ReView/ReView_FE)에서 확인할 수 있습니다.
