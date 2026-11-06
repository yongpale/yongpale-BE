# 카카오 로그인 및 JWT 인증

카카오 로그인만 제공한다. 로그인 성공 시 서비스용 HS256 JWT Access Token을 JSON으로 반환한다. 카카오 Access Token은 서비스 인증에 사용하거나 응답에 포함하지 않는다.

## 실행 준비

1. MariaDB에 `aicare_yongin` 데이터베이스를 만든다. 기본 접속 주소는 `jdbc:mariadb://localhost:3306/aicare_yongin`이다.
2. 프로젝트 루트의 `.env.example`을 참고해 IDE 실행 환경 변수 또는 셸 환경 변수를 설정한다. Spring Boot는 `.env` 파일을 자동으로 읽지 않는다.
3. `KAKAO_CLIENT_ID`는 REST API 키, `KAKAO_CLIENT_SECRET`은 해당 키의 Client Secret이다. 키를 소스 코드에 넣지 않는다.
4. `JWT_SECRET`에는 32바이트 이상의 무작위 비밀값을 설정한다. 예: `openssl rand -hex 32`. 모든 서버 인스턴스가 같은 키를 사용해야 한다.
5. 빈 로컬 개발 DB에서는 `JPA_DDL_AUTO=update`로 최초 테이블을 생성할 수 있다. 기본값 `validate`는 테이블을 생성하거나 변경하지 않는다. 기존 ERD 스키마에는 [SQL](sql/kakao-login.sql)을 검토해 수동 적용한다.
6. `./gradlew bootRun`으로 실행한다.

카카오 개발자 콘솔에 다음 Redirect URI를 정확히 등록한다.

```text
http://localhost:8080/api/v1/auth/kakao/callback-page
```

콘솔에서 카카오 로그인을 활성화하고 필요한 닉네임·이메일 동의항목을 설정한다. 코드에서 추가 scope를 강제하지 않는다. 닉네임이 없으면 `카카오회원`으로 가입하며 이메일이 없어도 로그인할 수 있다. 유효하고 검증된 이메일만 저장한다. OpenID Connect는 현재 구현에서 사용하지 않는다.

HTTPS 배포 시 `KAKAO_REDIRECT_URI`를 배포 주소로 변경하고 콘솔에도 등록한다. `SESSION_COOKIE_SECURE=true`로 설정한다. JWT 키와 카카오 Client Secret은 서버 환경 변수로만 관리한다.

## 브라우저에서 로그인

브라우저 주소창에서 아래 주소를 연다. 프론트 버튼에서도 `fetch` 대신 페이지 이동으로 시작한다.

```text
http://localhost:8080/api/v1/auth/kakao
```

```text
GET /api/v1/auth/kakao
→ /oauth2/authorization/kakao
→ 카카오 로그인·동의
→ /api/v1/auth/kakao/callback-page?code=...&state=...
→ 회원 조회·가입 후 JWT JSON 반환
```

인가 요청의 `state` 검증에는 임시 서버 세션을 사용한다. 동일한 브라우저에서 로그인 시작부터 콜백까지 진행해야 한다. 성공·실패 시 세션을 폐기하며 세션 쿠키로 API에 로그인할 수 없다. 다중 서버 환경에서는 OAuth 요청 세션을 공유하거나 같은 인스턴스로 라우팅해야 한다.

등록된 경로의 `callback-page`는 Spring Security가 처리한다. 현재 HTML 페이지나 프론트 리다이렉트를 반환하지 않는다. 프론트 연동 시 최종 전달 방식을 합의해 변경해야 한다. JWT를 리다이렉트 URL 쿼리에 넣지 않는다.

성공 응답 예시:

```json
{
  "tokenType": "Bearer",
  "accessToken": "eyJ...",
  "expiresIn": 1800,
  "user": {
    "id": 1,
    "nickname": "민재",
    "email": null,
    "termsAgreementRequired": true
  }
}
```

응답에는 `Cache-Control: no-store`를 설정한다. 카카오 회원 번호 `kakao_id`로 기존 회원을 찾으며 이메일이 같은 계정을 자동 연결하지 않는다. 탈퇴하거나 `ACTIVE`가 아닌 회원의 로그인을 거부한다. 카카오 인증·동의 취소, 잘못된 state, 토큰 교환 실패 시 `401 KAKAO_LOGIN_FAILED`를 반환한다.

## API 인증

API 요청에 다음 헤더를 포함한다.

```http
Authorization: Bearer <accessToken>
```

| API | 동작 |
| --- | --- |
| `GET /api/v1/auth/kakao` | 브라우저에서 카카오 로그인 시작 |
| `GET /api/v1/auth/kakao/callback-page` | 카카오 콜백 처리, JWT 발급 |
| `GET /api/v1/auth/me` | 로그인 회원 정보와 약관 동의 필요 여부 조회 |
| `POST /api/v1/auth/terms` | 서비스 이용약관·개인정보 수집 동의 저장 |

`me`, `terms`에는 JWT가 필요하다. 토큰이 없거나 서명·발급자·만료 검증에 실패하면 401이다. 해당 회원이 삭제되었거나 비활성 상태이면 `me`, `terms`는 거부한다. 그 외 경로도 기본적으로 JWT 인증이 필요하다.

약관 동의 요청:

```http
POST /api/v1/auth/terms
Authorization: Bearer <accessToken>
Content-Type: application/json

{"termsAgreed": true, "privacyAgreed": true}
```

신규 가입 시 서비스 약관에 동의한 것으로 임의 저장하지 않는다. 최초 로그인 후 `termsAgreementRequired=true`이면 프론트에서 서비스 약관 동의 화면을 보여 주고 위 API를 호출한다. 두 항목 모두 명시적으로 true여야 한다. 재요청해도 최초 동의 시각을 유지한다. 향후 동의가 필요한 업무 API에는 약관 동의 여부 검사를 추가해야 한다. 현재 JWT의 발급 자체는 동의 완료를 의미하지 않는다.

## 현재 범위 및 검증

- Access Token 유효기간은 30분이다. Refresh Token과 재발급 API는 포함하지 않는다. 만료 시 다시 카카오 로그인한다.
- 로그아웃 시 클라이언트가 보관한 Access Token을 제거한다. 서버에서 이미 발급한 토큰을 즉시 폐기하는 기능과 카카오 연결 해제는 포함하지 않는다.
- 프론트 서버 주소가 정해지지 않아 별도 CORS 허용은 설정하지 않았다. 다른 origin에서 API를 호출하려면 해당 origin에 대한 설정이 필요하다.
- 자동 테스트는 외부 카카오 서버를 호출하지 않는다. 실제 REST API 키·Client Secret·MariaDB을 설정한 후 브라우저에서 최초 가입과 재로그인을 확인해야 한다.

공식 문서: [카카오 로그인 설정](https://developers.kakao.com/docs/ko/kakaologin/prerequisite), [카카오 로그인 REST API](https://developers.kakao.com/docs/ko/kakaologin/rest-api), [Spring Security OAuth2 Login](https://docs.spring.io/spring-security/reference/servlet/oauth2/login/advanced.html), [JWT 검증](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html).

## MariaDB 저장 타입

JSON 필드는 `json`, 지역 ID 목록 `Long[]`도 JSON 배열로 저장한다. 시간은 `datetime(6)`에 UTC로 정규화해 저장하며 Java에서는 `OffsetDateTime`을 사용한다. DB에는 원래의 시간대 오프셋을 따로 보존하지 않는다. ERD 원본 파일은 변경하지 않았다.

참고: [MariaDB JSON 타입](https://mariadb.com/docs/server/reference/data-types/string-data-types/json), [Hibernate 시간대 저장 설정](https://docs.hibernate.org/orm/7.4/userguide/html_single/#basic-datetime-time-zone-storage).
