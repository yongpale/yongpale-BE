# 토큰 재발급과 로그아웃

카카오 로그인 콜백은 서비스용 Access Token과 Refresh Token을 JSON으로 반환한다. 기존 `tokenType`, `accessToken`, `expiresIn`, `user` 필드를 유지하고 `refreshToken`, `refreshExpiresIn`을 추가했다. 카카오의 토큰이나 OAuth 로그인 과정의 임시 JSESSIONID와는 별개다.

## DB 및 설정

MariaDB에 `auth_sessions`, `auth_refresh_tokens` 테이블이 필요하다. 기존 DB에서 `ddl-auto=validate`를 사용한다면 [추가 테이블 SQL](sql/auth-sessions.sql)을 먼저 적용한 후 새 서버를 시작한다. 개발용 `JPA_DDL_AUTO=update` 환경에서는 JPA가 테이블을 추가한다. 이번 작업에서 사용 중인 DB에 직접 접속해 변경하지 않았다.

- Access Token: `auth.jwt.access-token-ttl`, 기존 기본값 30분.
- Refresh Token: `auth.refresh.ttl`, 미설정 시 14일. 환경 변수 `AUTH_REFRESH_TTL=14d`로 설정 가능.
- Refresh Token 만료는 **최초 로그인 기준 고정 14일**이며 재발급할 때 연장되지 않는다. 로그인 유효기간이 끝나면 다시 카카오 로그인을 한다.
- DB에는 Refresh Token 원문이 아닌 SHA-256 해시를 저장한다. 토큰에는 256비트 난수와 로그인 식별자가 포함된다.
- JWT에 로그인 식별자 `sid`와 토큰 식별자 `jti`가 추가된다. API마다 DB의 로그인 상태와 회원 활성 상태도 확인해 로그아웃·회원 비활성화가 즉시 반영된다.
- **배포 전 발급한 기존 Access Token은 새 서버에서 401이 된다.** 새 로그인으로 토큰 쌍을 받아야 한다.

## 로그인·재발급 응답

```json
{
  "tokenType": "Bearer",
  "accessToken": "eyJ...",
  "expiresIn": 1800,
  "user": {
    "id": 1,
    "nickname": "민재",
    "email": null,
    "termsAgreementRequired": false
  },
  "refreshToken": "로그인UUID.임의의비밀값",
  "refreshExpiresIn": 1209600
}
```

`expiresIn`, `refreshExpiresIn`은 초 단위다. Refresh Token 남은 시간은 재발급할 때 줄어든다. 로그인 최대 유효기간이 지나면 Access Token의 exp가 남아 있어도 인증할 수 없다. 응답에는 `Cache-Control: no-store`를 적용한다. 토큰을 URL, 로그 또는 저장소에 포함하지 않는다. 프론트의 토큰 보관·전달 방식은 기존 JSON 계약을 유지하며 HttpOnly 쿠키 방식은 이 구현에 포함하지 않는다.

## 재발급

```http
POST /api/v1/auth/refresh
Content-Type: application/json

{"refreshToken": "로그인 시 받은 Refresh Token"}
```

성공하면 200과 새로운 Access Token·Refresh Token을 반환한다. Access Token이 만료된 상태에서도 호출할 수 있고 Authorization 헤더는 필요하지 않다. 이 경로는 헤더의 Bearer Token을 사용하지 않으며 본문의 Refresh Token만 검증한다. 실수로 만료된 Bearer 헤더를 함께 보내도 재발급이 가능하다.

재발급 시 기존 Refresh Token을 사용 처리하고 새로운 Refresh Token을 발급한다. **성공 응답의 두 토큰을 함께 교체**해야 한다. 이미 사용한 Refresh Token을 다시 보내면 탈취·재사용으로 판단해 해당 로그인 전체를 폐기한다. 이후 그 로그인에서 발급받은 모든 Access Token과 최신 Refresh Token도 거부된다.

프론트에서는 여러 API가 동시에 401이 되더라도 재발급 요청은 한 번만 실행하고, 나머지는 그 결과를 기다리도록 구현한다. 이미 사용한 Refresh Token으로 재발급을 재시도하지 않는다. 재발급 응답을 잃어 최신 토큰을 알 수 없으면 다시 로그인한다. 재발급 자체가 401을 반환하면 반복 호출하지 않는다.

## 로그아웃

```http
POST /api/v1/auth/logout
Content-Type: application/json

{"refreshToken": "현재 Refresh Token"}
```

성공 응답은 204이며 본문이 없다. Access Token이 없어도 호출할 수 있다. 본문의 토큰 소유를 확인한 뒤 해당 로그인만 폐기하므로 다른 기기의 로그인은 유지한다. 해당 로그인의 기존 Access Token과 Refresh Token은 즉시 거부된다. 이미 로그아웃한 요청을 다시 보내도 204다. 교체 전 토큰 또는 만료된 토큰도 해당 로그인의 종료에 사용할 수 있다. 알 수 없는 토큰은 다른 로그인을 건드리지 않고 204를 반환한다.

프론트는 서버 로그아웃 요청 후 보관 중인 토큰과 사용자 정보를 제거한다. 이 API는 **우리 서비스에서의 로그아웃**이며 카카오 계정의 웹 로그인 세션 종료나 카카오 연결 해제를 수행하지 않는다. 다시 카카오 로그인할 때 카카오가 추가 입력 없이 인증할 수 있다.

## 오류·운영 특성

- 400: 필수 `refreshToken` 누락, 빈 값, 최대 길이 256자 초과 등 요청 형식 오류.
- 401: 재발급 토큰 위조·만료·재사용, 폐기된 로그인, 탈퇴·비활성 회원.
- 보호 API도 폐기·만료된 로그인 또는 비활성 회원이면 401을 반환한다.
- 일반 보호 API는 기존과 같이 `Authorization: Bearer <accessToken>`을 요구한다. Refresh Token을 Bearer로 보내 API를 호출할 수 없다.
- 로그인 상태를 MariaDB에 저장하므로 서버 재시작 후에도 재발급·폐기 상태가 유지된다. 같은 DB와 JWT 서명키를 사용하는 서버끼리 공유한다.
- 로그인 행의 쓰기 잠금으로 재발급·로그아웃을 직렬 처리한다. 동시에 같은 Refresh Token을 재발급하면 하나만 성공하고 후속 재사용 감지로 로그인 전체가 폐기된다.
- 사용 처리한 해시는 재사용 감지를 위해 로그인 만료 전까지 유지해야 한다. 만료된 로그인·토큰 이력의 정기 정리 작업은 포함하지 않는다. 만료 이후 보존 정책을 정하고 토큰 이력, 로그인 순서로 삭제할 수 있다.
- 자동 테스트는 별도 H2 DB의 MariaDB 호환 모드에서 실행하며 실제 카카오 인증 서버를 호출하지 않는다.

설계 참고: [OAuth 보안 권고의 Refresh Token 보호](https://www.rfc-editor.org/rfc/rfc9700.html#section-4.14), [Spring Security JWT 인증](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html).
