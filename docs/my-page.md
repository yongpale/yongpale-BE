# 마이페이지 조회

```http
GET /api/v1/auth/me
Authorization: Bearer <accessToken>
```

로그인 회원의 회원 정보, 거주 지역, 자녀 학년, 마이페이지 카드용 개수를 반환한다. 가족 정보를 아직 등록하지 않은 회원도 200으로 응답한다.

```json
{
  "id": 1,
  "name": "민재",
  "nickname": "민재",
  "email": "test@example.com",
  "termsAgreementRequired": false,
  "region": "기흥구 구갈동",
  "childGrades": ["초2", "유치7세"],
  "savedPolicyCount": 0,
  "applicationScheduleCount": 0,
  "matchedBenefitCount": 0
}
```

| 필드 | 타입 | 의미 |
| --- | --- | --- |
| `id` | integer(int64) | 로그인 회원 ID |
| `name` | string | 표시 이름. 현재 저장된 카카오 닉네임을 사용하며 실명 정보는 아님 |
| `nickname` | string | 기존 응답과의 호환을 위한 회원 닉네임 |
| `email` | string 또는 null | 검증된 이메일 |
| `termsAgreementRequired` | boolean | 서비스 필수 약관 동의가 필요한지 여부 |
| `region` | string 또는 null | 가구의 구·읍면동 이름을 공백으로 연결. 예: `기흥구 구갈동` |
| `childGrades` | string[] | 자녀 추가 순서의 학년 목록. 같은 학년도 자녀별로 유지 |
| `savedPolicyCount` | integer(int32) | 정책 저장 개수. 담당 기능 연동 전까지 0 |
| `applicationScheduleCount` | integer(int32) | 신청 일정 개수. 담당 기능 연동 전까지 0 |
| `matchedBenefitCount` | integer(int32) | 맞춤 혜택 개수. 담당 기능 연동 전까지 0 |

**세 집계 값의 현재 0은 미연동 임시값**이다. 정책·일정·혜택 기능의 실제 DB 집계 결과를 뜻하지 않는다. 아직 개발 중인 타 담당 기능을 호출하거나 임의로 집계하지 않는다.

가구가 없으면 `region: null`, `childGrades: []`로 응답한다. 가구가 있고 자녀가 없으면 지역과 빈 학년 배열을 반환한다. 지역·학년은 요청 파라미터가 아니라 로그인 회원에 연결된 가구와 자녀 테이블에서 조회한다. 다른 회원의 가구와 익명 가구는 포함하지 않는다.

학년은 기존 가구 정보의 `grade_order`를 변환한다. 1~3은 유치5세~유치7세, 4~9는 초1~초6이다. 자녀 ID 오름차순으로 읽어 추가 순서를 유지한다.

## 담당 기능 연동 위치

`domain/auth/service/MyPageStatsService.getCounts(Long userId)`가 집계 연동 지점이다. 이 메서드의 세 TODO에서 담당 도메인의 집계 서비스를 호출하고 `MyPageCounts`를 반환하면 된다. 회원 ID는 인증된 본인 ID다. 필드명과 int 타입을 유지하면 프론트 응답 계약을 바꿀 필요가 없다. 실제 조회의 오류를 잡아 무조건 0으로 숨기는 처리는 추가하지 않는다.

`MyPageService`가 회원·가구·자녀 정보와 개수를 조합하고, `/auth/me`에서만 `MyPageResponse`를 사용한다. 로그인·토큰 재발급 응답의 `user`와 약관 동의 API는 기존 `UserResponse` 형식을 유지한다. 이번 변경으로 DB 컬럼이나 테이블을 추가하지 않는다.

현재 지역 기능 브랜치에 가구 조회 Repository와 학년 변환 enum이 없어, 기존 가구 작업 커밋 `10cd699`의 공통 파일 3개를 재사용했다. 가구 등록·수정 Controller/Service는 이번 변경에 포함하지 않는다.

## 검증

마이페이지 API 통합 테스트에서 가족 미등록, 본인 가족 정보 조회, 타 회원 가족 정보 차단, 자녀 순서와 중복 학년 유지, 정수형 임시값, 추후 집계 값 전달, JWT 보호를 검증한다. 기존 로그인·토큰 재발급·로그아웃 테스트도 함께 확인한다.

```shell
./gradlew test --tests '*MyPageApiTests' --tests '*AiCareYongInApplicationTests' --tests '*TokenSessionApiTests' --tests '*JwtServiceTests' --tests '*KakaoUserServiceTests'
```
