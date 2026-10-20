# quiz-maker 백엔드

Spring Boot 4 + Java 17 + MySQL(Railway). 앱(프런트)은 `quiz-maker-front` 저장소.
`master` 에 push 하면 Railway 가 곧바로 배포한다.

## 시험

```bash
./mvnw test -Dtest=QuizServiceTest,ExcelServiceTest,NoteServiceTest,NoteControllerTest
```

그냥 `./mvnw test` 는 DB 가 있어야 하는 `contextLoads` 때문에 실패한다.

## 운영 DB 변경과 배포 순서

이 서버는 `spring.jpa.hibernate.ddl-auto=validate` 이다. Hibernate 는 표를 만들지 않고,
시작할 때 엔티티와 운영 DB 표가 맞는지만 검사한다(마이그레이션 도구 없음).
**표가 하나라도 없으면 서버가 아예 뜨지 않는다** — 로그인·문제 풀기까지 모두 멈춘다.

그래서 새 표는 `src/main/resources/db/init/` 의 SQL 로 **서버가 켜질 때 스스로 만든다**
(`spring.sql.init.mode=always`, Hibernate 검사보다 먼저 실행). 이 SQL 은 켜질 때마다 돌므로
`CREATE TABLE IF NOT EXISTS` 처럼 여러 번 돌아도 안전한 문장만 둔다. 있는 표를 바꾸는 일(칸 추가 등)은
여기에 넣지 말고 `db/` 에 SQL 을 두어 사람이 운영 DB 에 먼저 실행한다.

### 2026-10-20 질문 메모 (`question_notes`)

처음(b05178b)에는 사람이 `db/20261020_question_notes.sql` 을 운영 DB 에 먼저 실행하는 방식이었는데,
SQL 없이 백엔드가 먼저 배포되어 `Schema validation: missing table [question_notes]` 로 서버가 뜨지 않았다.
그 뒤로 `db/init/question_notes.sql` 이 표를 만든다. **이제 백엔드 push 만 하면 된다.**

- 외래키(`user_id` → `users.id`)는 걸지 않는다. 운영 `users.id` 가 `bigint unsigned` 이면
  외래키가 `ERROR 3780 … are incompatible` 로 실패해 표가 안 만들어지고 서버가 다시 멈추기 때문이다.
  앱에는 사용자를 지우는 기능이 없고, 메모는 로그인한 사용자 번호로만 저장하고 읽는다.
- 사람이 `db/20261020_question_notes.sql`(외래키 포함)로 먼저 만들어 두었다면 그 표를 그대로 쓴다.
- 확인: Railway 배포 로그에 `Started QuizMakerApplication`, 로그인 토큰으로 `GET /api/notes` 가 `200 []`.
  메모를 하나 써 보면 `createdAt` 이 `…Z` 로 끝나는 UTC 시각(한국 시각 - 9시간)이어야 한다.
- 로컬 MySQL 로 확인한 경우(10/20): 표 없음 → 만들고 뜸 / 표 있음(데이터 유지) → 뜸 /
  외래키 있는 수동 표 → 그대로 두고 뜸 / `users.id` unsigned → 뜸 / 표가 생긴 DB 에 옛 백엔드 → 뜸.

되돌리기: 백엔드를 이전 커밋으로 되돌리는 것만으로 충분하다(표가 남아 있어도 옛 백엔드는 상관없다).
표까지 지우려면 **메모 기능 이전 백엔드(ad05b8b 이하)로 되돌린 뒤에** `DROP TABLE question_notes` 를 실행한다
(메모가 모두 지워진다). 새 백엔드가 떠 있는 채로 지우면 다음 재시작 때 표가 다시 만들어진다.

## 질문 메모 API

문제를 풀다 떠오른 질문을 계정별로 저장한다. 나중에 Claude 가 메모를 읽어 답을 조사하고
노션 공부 페이지로 정리하는 데 쓴다. 모두 `Authorization: Bearer <토큰>` 필요, 내 메모만 보이고 고칠 수 있다.

| 요청 | 설명 |
| --- | --- |
| `GET /api/notes?status=open\|resolved\|all&setId=` | 내 메모 목록, 최신순. `status` 기본 `all`, `setId` 는 선택 |
| `POST /api/notes` `{ setId?, questionId?, blankNo?, context?, content }` | 메모 만들기 → 만든 메모 |
| `PUT /api/notes/{id}` `{ content?, resolved? }` | 본문 고치기·해결 표시 → 고친 메모 |
| `DELETE /api/notes/{id}` | 지우기 → 204 |

응답 항목: `{ id, setId, questionId, setTitle, blankNo, context, content, resolved, resolvedAt, createdAt, updatedAt }`

- `setId` 를 보내면 내 문제집이어야 하고, `setTitle` 은 서버가 그 문제집 제목을 복사해 채운다(요청 값은 쓰지 않음).
- `questionId` 를 보내면 그 문제집의 문항이어야 한다. `questionId` 만 보내면 서버가 문항의 문제집을 찾아 채운다.
- `blankNo` 는 화면에 보이는 빈칸 번호(1부터). `context` 는 1000자, `content` 는 2000자까지(앞뒤 공백은 잘라서 저장).
- `resolved: true` 면 `resolvedAt` = 지금(이미 해결한 메모는 처음 해결한 시각 유지), `false` 면 `null`.
- 시각 셋(`resolvedAt`·`createdAt`·`updatedAt`)은 UTC 로 저장하고 끝에 `Z` 를 붙여 보낸다(예: `2026-10-20T13:40:00.123Z`, 밀리초까지).
  서버(Railway) 시간대 설정과 상관없이 같고, 앱·도구가 기기 시각(한국 시각)으로 바꿔 보여 준다.
  DB 에서 직접 보면 한국 시각보다 9시간 이르다. (문제집 `createdAt` 은 예전 방식 그대로 서버 시각, 시간대 없음)
- 문제집을 지워도 메모는 지우지 않는다. 그 메모의 `setId`·`questionId` 만 `null` 로 비우고 `setTitle`·`blankNo`·`context` 는 남긴다.
  문항을 지우면 `questionId` 만 `null` 이 된다. 따라서 `setId` 가 `null` 인데 `setTitle` 이 있으면 "지운 문제집" 메모다.
- 오류는 400 과 한국어 문장(본문이 그냥 글자): 빈 본문, 너무 긴 값, 모르는 `status`, 남의 문제집·메모(`…권한이 없습니다.`), 없는 문제집·문항·메모.
  형식이 틀린 요청(`?setId=abc`, `/api/notes/abc`, 깨진 JSON, 숫자 칸에 글자·너무 큰 수, 본문 없음)도
  Spring 의 영어 내부 문구 대신 `요청 값 setId 의 형식이 올바르지 않습니다. 숫자여야 합니다.`,
  `요청 본문(JSON)의 형식이 올바르지 않습니다. …` 로 알린다.
