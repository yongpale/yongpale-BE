# 가족 정보 API

로그인 회원당 가구 프로필 하나를 등록한다. 모든 요청에는 카카오 로그인으로 발급받은 서비스 JWT가 필요하다. 회원 ID는 JWT에서 가져오며 요청 본문이나 경로로 받지 않는다.

```http
Authorization: Bearer <accessToken>
Content-Type: application/json
```

| 메서드 | 경로 | 기능 | 성공 상태 |
| --- | --- | --- | --- |
| POST | `/api/v1/households` | 내 가족 정보 최초 등록 | 201 |
| GET | `/api/v1/households` | 내 가족 정보 조회 | 200 |
| PUT | `/api/v1/households` | 내 가족 정보 전체 수정 | 200 |

## 등록·수정 요청

POST와 PUT은 같은 형식의 본문을 사용한다.

```json
{
  "region": {
    "guName": "기흥구",
    "dongName": "구갈동"
  },
  "situations": ["DUAL_INCOME", "MULTI_CHILD"],
  "children": [
    {"nickname": "홍길동", "grade": "초2"},
    {"nickname": "홍길순", "grade": "유치7세"}
  ]
}
```

- `region.guName`: 거주 구. 필수, 최대 20자.
- `region.dongName`: 거주 읍·면·동. 필수, 최대 30자. 예: `포곡읍`, `백암면`, `구갈동`.
- 지역은 `regions` 테이블에 등록된 구·읍면동 조합으로 조회한다. 임의의 지역을 새로 생성하지 않는다. 서버 시작 시 [공식 행정읍면동 CSV](regions.md)에서 누락 지역을 자동 적재한다. `GET /api/v1/regions?guName=기흥구`로 선택 목록을 조회할 수 있다. 기존 지역의 ID·좌표·인구는 유지한다.
- `situations`: 필수 배열. `DUAL_INCOME`(맞벌이), `SINGLE_PARENT`(한부모), `MULTI_CHILD`(다자녀)를 복수 선택한다. 해당 사항이 없으면 빈 배열 `[]`을 보낸다. 자녀 수를 근거로 다자녀 여부를 자동 변경하지 않는다.
- `children`: 한 명 이상의 자녀. 배열의 첫 번째가 첫째, 두 번째가 둘째다. 학년이나 닉네임으로 재정렬하지 않는다.
- `nickname`: 필수, 최대 30자. 앞뒤 공백은 제거한다.
- `grade`: 필수. `유치5세`, `유치6세`, `유치7세`, `초1`, `초2`, `초3`, `초4`, `초5`, `초6` 중 하나. DB의 `grade_order`는 순서대로 1~9이며, `초2`는 5다. 출생 순서와 별개다.

PUT은 부분 수정이 아니다. 지역·가정 상황·자녀를 모두 전달한다. 요청에서 빠진 자녀는 삭제되고 전체 목록을 새로 저장하므로 자녀 ID는 변경된다. 자녀의 추가·삭제·순서 변경도 이 목록으로 반영한다. 가구 ID는 유지된다. 이번 기능은 자녀 ID를 외부 참조로 사용하는 API를 제공하지 않는다.

## 응답 예시

POST, GET, PUT 모두 같은 응답 형식을 사용한다. ID 값은 예시다.

```json
{
  "id": 1,
  "region": {
    "id": 1,
    "guName": "기흥구",
    "dongName": "구갈동",
    "regionType": "DONG"
  },
  "situations": ["DUAL_INCOME", "MULTI_CHILD"],
  "children": [
    {"id": 1, "birthOrder": 1, "nickname": "홍길동", "grade": "초2"},
    {"id": 2, "birthOrder": 2, "nickname": "홍길순", "grade": "유치7세"}
  ]
}
```

POST 성공 시 `Location: /api/v1/households`도 반환한다. 자녀는 IDENTITY 식별자를 입력 순서대로 발급받고 ID 오름차순으로 조회해 순서를 유지한다. 추가 DB 컬럼이나 스키마 변경은 없다.

## 오류와 일관성

| 상태 | 의미 |
| --- | --- |
| 400 | 필수 항목 누락, 빈 자녀 목록, 잘못된 학년·가정 상황, 등록되지 않은 지역 |
| 401 | JWT 누락·위조·만료, 폐기된 로그인, 존재하지 않는 회원 또는 탈퇴·비활성 회원 |
| 404 | 본인 가구가 없는 상태에서 GET·PUT 요청 |
| 409 | 이미 가구가 있는 회원이 POST로 중복 등록 |

조회·수정은 항상 로그인한 회원의 가구만 대상으로 한다. 다른 회원의 가구 ID를 전달하는 기능은 없다. 등록·수정 시 회원 행에 쓰기 잠금을 잡아 동시 등록 중복 및 수정 충돌을 방지한다. 가구·자녀 변경은 하나의 트랜잭션으로 반영하며 실패하면 전체 롤백한다.

## 확인

Swagger UI의 Households 항목에서 요청을 실행할 수 있다. Authorize에 서비스 Access Token을 입력한다.

`./gradlew test`로 실제 JWT와 테스트용 H2 DB를 사용하는 API 통합 테스트를 실행한다. 회원별 접근 제한, 중복·동시 등록, 전체 수정, 자녀 순서, 학년 코드, 입력 검증을 확인한다. 테스트 DB와 업무 DB는 분리된다.
