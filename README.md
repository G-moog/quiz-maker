# quiz-maker 백엔드

Spring Boot 4 + Java 17 + MySQL(Railway). 앱(프런트)은 `quiz-maker-front` 저장소.
`master` 에 push 하면 Railway 가 곧바로 배포한다.

## 시험

```bash
./mvnw test -Dtest=QuizServiceTest,ExcelServiceTest,NoteServiceTest,NoteControllerTest
```

그냥 `./mvnw test` 는 DB 가 있어야 하는 `contextLoads` 때문에 실패한다.

## 운영 DB 변경과 배포 순서

이 서버는 `spring.jpa.hibernate.ddl-auto=validate` 이다. 표를 스스로 만들지 않고,
시작할 때 엔티티와 운영 DB 표가 맞는지만 검사한다(마이그레이션 도구 없음).
그래서 새 표·칸이 필요한 변경은 SQL 을 `db/` 에 두고 **사람이 운영 DB 에 먼저 실행**한다.

### 2026-10-20 질문 메모 (`question_notes`)

1. **운영 DB 에 SQL 실행** — `db/20261020_question_notes.sql`
   - **`users.id` 타입 확인은 건너뛰지 말고 반드시 실행한다.** 운영 표는 사람이 만든 것이라
     `users.id` 가 `bigint` 인지 `bigint unsigned` 인지 모른다(`int` 라면 지금도 서버가 뜨지 않으므로 둘 중 하나).
     `bigint unsigned` 이면 SQL 의 `user_id` 줄을 `BIGINT UNSIGNED` 로 바꿔 실행한다.
     그대로 실행하면 `ERROR 3780 … are incompatible` 로 표가 만들어지지 않는다. 엔진(InnoDB)도 같이 확인한다.
   - `CREATE TABLE` 을 실행한 뒤 `SHOW CREATE TABLE question_notes;` 로 칸 11개·색인 2개·외래키 1개를 확인한다.
   - **`CREATE TABLE` 이 오류로 끝났거나 `SHOW CREATE TABLE` 이 표를 보여 주지 않으면 2번(백엔드 push)으로 넘어가지 않는다.**
   - 이 표는 옛 백엔드에는 아무 영향이 없으므로 먼저 만들어 두어도 안전하다.
2. **백엔드 push** — Railway 배포 로그에 `Started QuizMakerApplication` 이 찍히는지,
   로그인 토큰으로 `GET /api/notes` 가 `200 []` 인지 확인한다.
   메모를 하나 써 보면 `createdAt` 이 `…Z` 로 끝나는 UTC 시각(한국 시각 - 9시간)이어야 한다.
3. **앱(quiz-maker-front) push**

> **경고 — 순서를 바꾸면 앱 전체가 멈춘다.**
> 표 없이 백엔드를 먼저 배포하면 시작할 때 Hibernate 검사가
> `Schema validation: missing table [question_notes]` 로 실패해 서버가 뜨지 않는다
> (Railway 로그에서는 `missing table [question_notes]` 로 찾으면 된다).
> 메모뿐 아니라 로그인·문제 풀기까지 모두 안 된다.
> 그때는 곧바로 1번 SQL 을 실행하고 Railway 에서 다시 배포(Redeploy)한다.
>
> 앱을 백엔드보다 먼저 올리면 메모 기능만 오류가 나고(아직 `/api/notes` 가 없음) 나머지는 그대로다.

되돌리기: 백엔드를 이전 커밋으로 되돌리는 것만으로 충분하다(표가 남아 있어도 옛 백엔드는 상관없다).
표까지 지우려면 **백엔드를 되돌린 뒤에** SQL 파일 끝의 `DROP TABLE` 을 실행한다(메모가 모두 지워진다).

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
