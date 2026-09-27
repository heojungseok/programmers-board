# programmers-board

회원·게시판·댓글 API입니다. **읽기는 누구나, 쓰기는 로그인한 회원만, 수정·삭제는 작성자만** 할 수 있도록 설계했습니다.

## 목차

1. [실행 방법](#1-실행-방법)
2. [API](#2-api)
3. [호출 예시](#3-호출-예시)
4. [설계 설명](#4-설계-설명)

## 1. 실행 방법

Java 25와 Docker가 필요합니다.

1. JWT 서명 키를 만들어 `.env`에 넣습니다. 형식은 `.env.example`에 있습니다.

   ```bash
   printf 'JWT_SECRET=%s\n' "$(openssl rand -base64 32)" > .env
   ```

   `jwt.secret`에는 기본값을 두지 않았습니다. `.env`가 없으면 기동에 실패합니다. **서명 키가 빈 값으로 올라가는 것보다 뜨지 않는 편이 안전하다고 봤습니다.**

2. PostgreSQL 컨테이너를 띄웁니다. PostgreSQL 17이 `127.0.0.1:5432`에 올라가고, 앱이 쓸 `board-db`와 테스트가 쓸 `board_test_db`가 함께 만들어집니다. 계정은 `sa`이고 비밀번호는 없습니다.

   ```bash
   docker compose up -d
   ```

3. 앱을 실행합니다. `8890` 포트에서 뜨고, 첫 기동 때 Hibernate가 `member`·`post`·`comment` 테이블을 만듭니다.

   ```bash
   ./gradlew bootRun
   ```

이미 쓰는 PostgreSQL에 붙이려면 `board-db`와 `board_test_db`를 만든 뒤 접속 정보를 `src/main/resources/application.yml`에서 바꿉니다.

### 테스트

```bash
./gradlew test
```

테스트는 `board_test_db`를 쓰고 실행마다 스키마를 다시 만듭니다. 앱이 쓰는 `board-db`는 건드리지 않습니다.

전체 36개이고 모두 실제 HTTP 요청을 보내는 통합 테스트입니다. 글 14개, 댓글 9개, 회원·인증 7개, 오류 응답 5개, 앱 기동 1개입니다.

## 2. API

토큰이 필요한 요청은 `Authorization: Bearer <accessToken>` 헤더를 붙입니다.

| 기능 | 메서드·경로 | 인증 | 성공 응답 |
|---|---|---|---|
| 회원 가입 | `POST /api/members` | - | `201` |
| 로그인 | `POST /api/auth/login` | - | `200` 토큰 |
| 글 목록 | `GET /api/posts?page=0&size=10` | - | `200` 페이지 |
| 글 상세 | `GET /api/posts/{postId}` | - | `200` |
| 글 작성 | `POST /api/posts` | 필요 | `201` + `Location` |
| 글 수정 | `PUT /api/posts/{postId}` | 작성자 | `200` |
| 글 삭제 | `DELETE /api/posts/{postId}` | 작성자 | `204` |
| 댓글 작성 | `POST /api/posts/{postId}/comments` | 필요 | `201` + `Location` |
| 댓글 목록 | `GET /api/posts/{postId}/comments` | - | `200` 전체 |
| 댓글 수정 | `PUT /api/comments/{commentId}` | 작성자 | `200` |
| 댓글 삭제 | `DELETE /api/comments/{commentId}` | 작성자 | `204` |

글 목록은 최신순이고 기본 페이지 크기는 20입니다. 댓글 목록은 페이지를 나누지 않고 오래된 순으로 전체를 반환합니다. 한 글의 댓글 수가 많지 않다고 보았고, 늘어나면 페이지 나누기가 필요합니다.

### 공통 응답 형식

성공과 실패가 같은 골격을 씁니다. 값이 없는 필드는 응답에서 빠집니다.

```json
{ "code": "SUCCESS", "message": "상세 조회 완료", "data": { "id": 1, "title": "첫 글" } }
```

```json
{ "code": "POST_NOT_FOUND", "message": "글을 찾을 수 없습니다." }
```

검증 실패는 어느 필드가 왜 거절됐는지를 `errors`에 담습니다.

```json
{
  "code": "INVALID_INPUT",
  "message": "입력이 올바르지 않습니다.",
  "errors": [ { "field": "title", "reason": "공백일 수 없습니다" } ]
}
```

클라이언트는 `code`로 원인을 구분합니다. HTTP 상태만으로는 갈리지 않는 경우가 있기 때문입니다. `404` 하나에 글 없음과 댓글 없음과 없는 주소가 함께 들어옵니다.

| 상태 | code | 발생 상황 |
|---|---|---|
| 400 | `INVALID_INPUT` | 검증 실패, 깨진 JSON 본문, 경로 변수 타입 불일치 |
| 401 | `AUTH_REQUIRED` | 토큰 없이 인증이 필요한 요청 |
| 401 | `TOKEN_EXPIRED` | 만료된 토큰 |
| 401 | `LOGIN_FAILED` | 로그인 실패. 없는 이메일과 틀린 비밀번호를 구분하지 않습니다 |
| 403 | `ACCESS_DENIED` | 남의 글·댓글을 수정하거나 삭제 |
| 404 | `POST_NOT_FOUND` | 없거나 삭제된 글 |
| 404 | `COMMENT_NOT_FOUND` | 없거나 삭제된 댓글 |
| 404 | `RESOURCE_NOT_FOUND` | 없는 주소 |
| 405 | `METHOD_NOT_ALLOWED` | 지원하지 않는 HTTP 메서드 |
| 409 | `EMAIL_DUPLICATED` | 이미 가입된 이메일 |
| 415 | `UNSUPPORTED_MEDIA_TYPE` | `application/json`이 아닌 본문 |
| 500 | `INTERNAL_ERROR` | 처리하지 못한 예외. 원인은 서버 로그에만 남기고 응답에는 담지 않습니다 |

삭제는 `204 No Content`로 답합니다. **이 응답만 공통 형식을 쓰지 않습니다.** 규격상 본문을 가질 수 없기 때문입니다.

## 3. 호출 예시

가입부터 댓글까지 이어서 호출한 결과입니다. 응답은 실제로 받은 것입니다.

**회원 가입**

```bash
curl -i -X POST localhost:8890/api/members -H 'Content-Type: application/json' \
  -d '{"email":"demo@board.com","password":"password123","nickname":"데모"}'
```

```
HTTP/1.1 201
{"code":"SUCCESS","message":"회원 가입 성공","data":{"id":1,"email":"demo@board.com","nickname":"데모"}}
```

**로그인** — 이후 요청에 쓸 토큰을 받습니다.

```bash
TOKEN=$(curl -s -X POST localhost:8890/api/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"demo@board.com","password":"password123"}' \
  | python3 -c 'import sys,json;print(json.load(sys.stdin)["data"]["accessToken"])')
```

```
{"code":"SUCCESS","message":"로그인 성공","data":{"accessToken":"eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIi..."}}
```

**글 작성** — `Location`에 방금 만든 글의 주소가 담깁니다.

```bash
curl -i -X POST localhost:8890/api/posts -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' -d '{"title":"첫 글","content":"본문입니다"}'
```

```
HTTP/1.1 201
Location: /api/posts/1
{"code":"SUCCESS","message":"글 생성 완료","data":{"id":1,"title":"첫 글","content":"본문입니다"}}
```

**댓글 작성**

```bash
curl -i -X POST localhost:8890/api/posts/1/comments -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' -d '{"content":"첫 댓글"}'
```

```
HTTP/1.1 201
Location: /api/comments/1
{"code":"SUCCESS","message":"댓글 작성 완료","data":{"id":1,"content":"첫 댓글","nickname":"데모","createdAt":"2026-09-27T13:08:27.670924Z"}}
```

**글 목록** — 작성자 닉네임과 댓글 수가 함께 나옵니다. 로그인 없이 조회됩니다.

```bash
curl -s 'localhost:8890/api/posts?page=0&size=10'
```

```json
{
  "code": "SUCCESS",
  "message": "목록 조회 완료",
  "data": {
    "content": [
      { "id": 1, "title": "첫 글", "nickname": "데모", "commentCount": 1, "createdAt": "2026-09-27T13:08:27.613448Z" }
    ],
    "page": 0, "size": 10, "totalElements": 1, "totalPages": 1
  }
}
```

**글 상세와 댓글 목록** — 둘 다 로그인 없이 조회됩니다.

```bash
curl -s localhost:8890/api/posts/1 && curl -s localhost:8890/api/posts/1/comments
```

```
{"code":"SUCCESS","message":"상세 조회 완료","data":{"id":1,"title":"첫 글","content":"본문입니다","nickname":"데모"}}
{"code":"SUCCESS","message":"댓글 목록 조회 완료","data":[{"id":1,"content":"첫 댓글","nickname":"데모","createdAt":"2026-09-27T13:08:27.670924Z"}]}
```

### 401 — 로그인 없이 쓰기

```bash
curl -i -X POST localhost:8890/api/posts -H 'Content-Type: application/json' \
  -d '{"title":"t","content":"c"}'
```

```
HTTP/1.1 401
{"code":"AUTH_REQUIRED","message":"로그인이 필요합니다."}
```

만료된 토큰을 보내면 같은 401이지만 `code`가 `TOKEN_EXPIRED`로 달라집니다. 클라이언트가 재로그인을 유도할 수 있습니다.

### 403 — 남의 글 수정

다른 회원으로 가입해 토큰을 받고, 1번 글을 수정해 봅니다.

```bash
curl -s -X POST localhost:8890/api/members -H 'Content-Type: application/json' \
  -d '{"email":"other@board.com","password":"password123","nickname":"타인"}' > /dev/null
OTHER=$(curl -s -X POST localhost:8890/api/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"other@board.com","password":"password123"}' \
  | python3 -c 'import sys,json;print(json.load(sys.stdin)["data"]["accessToken"])')
curl -i -X PUT localhost:8890/api/posts/1 -H "Authorization: Bearer $OTHER" \
  -H 'Content-Type: application/json' -d '{"title":"몰래 수정","content":"몰래"}'
```

```
HTTP/1.1 403
{"code":"ACCESS_DENIED","message":"접근이 제한됐습니다."}
```

없는 글에 같은 요청을 보내면 `404`입니다. **대상이 없으면 404를 먼저 내고, 있을 때만 작성자를 비교해 403을 냅니다.** 순서를 뒤집으면 없는 글에도 403이 나가 그 글이 존재한다는 사실이 새어 나갑니다.

## 4. 설계 설명

### 4.1 로그인 방식

**JWT를 선택했습니다.** 로그인하면 서명된 토큰을 돌려주고, 이후 요청은 `Authorization` 헤더의 토큰으로 신원을 확인합니다. 토큰에는 회원 id와 이메일만 담았습니다. 서명은 위조를 막아 주지만 내용을 감춰 주지는 않으므로, **토큰에 넣은 값은 모두 공개된다고 보고 비밀번호와 권한은 넣지 않았습니다.**

| 방식 | 장점 | 과제에서 고르지 않은 이유 |
|---|---|---|
| JWT + 직접 만든 검증 필터 (선택) | 서버가 상태를 갖지 않아 확장이 쉽습니다 | - |
| 세션 | 서버가 세션을 지우면 즉시 무효화됩니다 | 서버가 상태를 가지므로 인스턴스가 늘면 세션 저장소가 필요해집니다 |

인증이 없는 요청과 권한이 없는 요청은 **서로 다른 곳에서 응답이 만들어집니다.**

| 상황 | 응답을 만드는 곳 | 결과 |
|---|---|---|
| 토큰이 없거나 만료 | `JwtAuthenticationEntryPoint` (보안 필터 단계) | `401` |
| 남의 글·댓글을 수정·삭제 | `GlobalExceptionHandler` (컨트롤러 단계) | `403` |

보안 필터는 컨트롤러보다 앞에 있어서 `@RestControllerAdvice`가 닿지 않습니다. 그래서 401은 필터 쪽에서 직접 써야 합니다. 두 경로가 같은 모양을 내도록 `ErrorCode`와 응답을 쓰는 코드를 한 곳에 두고 양쪽이 함께 씁니다.

권한 판정을 Security 설정이 아니라 서비스에서 하는 이유가 있습니다. Security의 주소별 규칙은 **요청만 보고** 판단하는데, "이 글의 작성자인가"는 데이터를 봐야 알 수 있습니다. 서비스는 이미 그 글을 조회한 상태라 조회가 한 번 더 늘지 않습니다.

한계는 두 가지입니다. **리프레시 토큰이 없어 만료되면 다시 로그인해야 합니다.** 그리고 **서버가 토큰을 즉시 무효화할 수 없습니다** — 로그아웃은 클라이언트가 토큰을 버리는 것이고, 유출된 토큰은 만료까지 유효합니다. 무효화 목록을 두면 막을 수 있지만 요청마다 저장소를 확인해야 해서 상태를 갖지 않는 이점이 사라집니다.

### 4.2 N+1

글 목록에는 작성자 닉네임과 댓글 수가 필요합니다. 글을 엔티티로 읽고 각 글에서 작성자와 댓글을 꺼내면 글 한 건마다 조회가 추가됩니다. 글 10개면 목록 1번에 작성자 10번, 댓글 수 10번으로 **21번**이 됩니다.

**목록은 집계 쿼리 하나로 DTO를 직접 만듭니다.** 작성자는 조인해 닉네임만 꺼내고, 댓글은 `LEFT JOIN`으로 세고, 글 단위로 그룹화합니다.

```sql
select new PostListItemResponse(p.id, p.title, m.nickname, count(c), p.createdAt)
from Post p
    join p.author m
    left join Comment c on c.post = p and c.deletedAt is null
where p.deletedAt is null
group by p.id, p.title, m.nickname, p.createdAt
order by p.createdAt desc
```

엔티티를 만들지 않으므로 지연 로딩이 일어날 통로 자체가 없습니다. `group by`가 있어 전체 개수를 세는 쿼리는 따로 지정했습니다. Spring Data가 자동으로 만드는 개수 쿼리는 그룹이 유지돼 글 수가 아니라 그룹 수를 세기 때문입니다.

| 방식 | 장점 | 과제에서 고르지 않은 이유 |
|---|---|---|
| 집계 쿼리로 DTO 직접 생성 (선택) | 필요한 값만 읽고 엔티티를 만들지 않습니다 | - |
| 엔티티 조회 후 자바에서 변환 | 코드가 단순합니다 | 작성자·댓글 수를 꺼낼 때 글 수만큼 조회가 늘어납니다 |
| 서브쿼리로 댓글 수 | 조인 결과가 부풀지 않아 `group by`가 필요 없습니다 | 세는 대상이 댓글 하나뿐이라 조인으로 충분합니다. 집계가 둘 이상 필요해지면 이쪽이 안전합니다 |

**글이 몇 개든 목록 조회는 SQL 2건입니다.** 목록 1건과 전체 개수 1건입니다. `PostApiTest`가 글 5개를 만들고 목록을 한 번 조회해, Hibernate 통계로 실행된 쿼리 수가 2건을 넘지 않는지 확인합니다.

### 4.3 삭제 처리

**행을 지우지 않고 `deletedAt`에 삭제 시각을 남깁니다.** 복구할 수 있고, 언제 지웠는지 남고, 나중에 삭제된 댓글의 자리를 남겨 둘 수 있습니다.

조회에서 걸러 내는 일은 엔티티에 선언한 `@SQLRestriction("deleted_at is null")`이 합니다. Hibernate가 생성하는 조회 SQL에 조건이 자동으로 추가되기 때문에, 조회 코드는 "삭제"라는 개념을 몰라도 삭제된 글이 `404`가 됩니다. 재삭제도 같은 경로라 `404`입니다.

글을 지울 때는 그 글의 댓글도 함께 표시해야 합니다. 실제로 나가는 SQL은 **댓글이 몇 개든 2건**입니다.

```sql
update post    set deleted_at = ? where id = ?
update comment set deleted_at = ? where post_id = ? and deleted_at is null
```

두 번째는 벌크 UPDATE입니다. 댓글을 하나씩 읽어 지우지 않습니다.

| 방식 | 장점 | 과제에서 고르지 않은 이유 |
|---|---|---|
| soft delete + 벌크 UPDATE (선택) | 복구·이력이 남고, 쿼리 수가 댓글 수와 무관합니다 | - |
| 물리 삭제 | 데이터가 쌓이지 않습니다 | 되돌릴 수 없고 이력이 남지 않습니다 |
| `cascade = REMOVE` | 코드가 한 줄로 끝납니다 | 댓글을 하나씩 조회해 하나씩 삭제하므로 댓글 수만큼 쿼리가 늘어납니다. 물리 삭제라 soft delete와 섞이지 않습니다 |

**`@SQLRestriction`은 Hibernate가 생성하는 조회 SQL에만 적용됩니다.** 직접 작성한 JPQL과 벌크 UPDATE는 예외이므로, 삭제 조건을 쿼리에 직접 명시해야 합니다.

| 쿼리 | 직접 명시한 조건 | 빠뜨렸을 때 |
|---|---|---|
| 글 목록 집계 | `p.deletedAt is null`, `c.deletedAt is null` | 삭제된 글이 목록에 나오고, 삭제된 댓글이 댓글 수에 포함됩니다 |
| 댓글 벌크 삭제 | `c.deletedAt is null` | 이미 삭제된 댓글의 삭제 시각을 덮어씁니다 |

두 경우 모두 **예외도 오류 로그도 없이 결과만 틀리기 때문에** 실행 중에는 드러나지 않습니다. 삭제된 글이 목록에서 제외되는지와 글을 삭제하면 그 댓글도 수정할 수 없는지를 테스트로 고정했습니다.

남는 한계는 데이터가 계속 쌓이는 것입니다. 지운 글과 댓글이 테이블에 남아 있어, 양이 늘면 보관 기간을 정해 실제로 지우거나 별도 테이블로 옮기는 일이 필요해집니다.
