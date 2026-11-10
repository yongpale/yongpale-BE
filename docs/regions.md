# 용인시 행정읍면동 기준 데이터

제공받은 `경기도 용인시_행정구역 정보(행정읍면동).csv`를 UTF-8로 변환해 `src/main/resources/data/regions.csv`에 포함했다. 원본 파일은 변경하지 않았다.

- 출처: [공공데이터포털 — 경기도 용인시 행정구역 정보](https://www.data.go.kr/data/15136530/fileData.do)
- 파일의 데이터 기준일: 2026-09-07
- 원본 인코딩: CP949
- 지역 수: 39개 (기흥구 15개, 처인구 13개, 수지구 11개)
- 원본 컬럼: `행정읍면동`, `행정읍면동명`, `데이터기준일자`

`행정읍면동`의 앞부분을 구 이름으로, `행정읍면동명`을 읍·면·동 이름으로 사용한다. 이름의 끝에 따라 `region_type`을 `EUP`, `MYEON`, `DONG`으로 구분한다. 파일에는 좌표·아동 인구가 없으므로 해당 값은 채우지 않는다. `데이터기준일자`를 인구 통계의 기준일인 `population_base_date`에 넣지 않는다.

## DB 적재

MariaDB 접속 설정과 기존 테이블이 준비된 상태에서 서버를 시작하거나 재시작하면 `RegionCsvLoader`가 자동으로 실행된다. 원본 파일이 다운로드 폴더에 계속 남아 있을 필요는 없다.

```text
행정구역 기준 데이터 39개 반영 완료 (기존 지역 유지)
```

`(gu_name, dong_name)` 유니크 제약을 기준으로 MariaDB의 `INSERT ... ON DUPLICATE KEY UPDATE`를 사용한다. 중복 행은 기존 ID를 그대로 유지하는 처리를 하므로 기존 가구·시설의 지역 참조와 좌표·인구 데이터는 유지된다. 구갈동 한 개만 들어 있는 DB에도 나머지 지역을 추가하며 여러 번 실행해도 중복 행이 생기지 않는다. 기존 지역의 이름·유형을 수정하거나 파일에서 빠진 지역을 삭제하지 않는다.

CSV 전체의 헤더·주소·유형·중복·기준일을 검증한 후 하나의 트랜잭션으로 적재한다. 검증이나 DB 저장이 실패하면 시작 로그에서 오류를 확인한다. 이 로더는 기본 활성화되어 있으며 필요하면 아래 설정으로 끌 수 있다.

```yaml
app:
  data:
    regions:
      enabled: false
```

적재 후 확인 SQL:

```sql
SELECT gu_name, COUNT(*) AS region_count
FROM regions
GROUP BY gu_name;

SELECT region_id, gu_name, dong_name, region_type
FROM regions
WHERE gu_name = '기흥구' AND dong_name = '상하동';
```

기존 DB에 별도로 등록한 지역이 더 있으면 전체 행 수는 39개보다 많을 수 있다. 이번 작업에서는 사용 중인 DB에 직접 접속해 적재하지 않는다. 새 코드로 서버를 재시작할 때 적재된다.

## 프론트 지역 선택 API

JWT가 필요하다.

```http
GET /api/v1/regions
Authorization: Bearer <accessToken>
```

구별 목록:

```http
GET /api/v1/regions?guName=기흥구
Authorization: Bearer <accessToken>
```

URL에서 한글 쿼리 값은 프론트의 URLSearchParams 등으로 인코딩한다. 결과는 구 이름·읍면동 이름 오름차순이며, 구 이름이 없거나 공백이면 전체 목록을 반환한다. 일치하는 구가 없으면 빈 배열을 반환한다. 응답 예시의 ID는 예시 값이다.

```json
[
  {"id": 1, "guName": "기흥구", "dongName": "구갈동", "regionType": "DONG"},
  {"id": 2, "guName": "기흥구", "dongName": "구성동", "regionType": "DONG"}
]
```

프론트는 목록에서 선택한 `guName`, `dongName`을 `/api/v1/households` 요청의 `region`에 전달한다. 행정동 이름을 그대로 사용하며 법정동 이름과 혼용하지 않는다.

## 업데이트 및 검증

새 공식 파일을 사용할 때는 컬럼 이름을 유지하고 UTF-8로 변환해 `regions.csv`를 교체한다. 새 지역은 다음 시작 시 추가된다. 행정구역 개편으로 기존 지역 이름이 바뀌거나 통폐합되는 경우에는 가구 참조 이전을 별도 검토한다.

`./gradlew test`에서 원본 39개 파싱, 읍·면·동 구분, 잘못된 CSV 거부, 초기 적재, 재실행 시 ID·인구·좌표·가구 연결 유지, 구별 조회와 JWT 보호, 실제 상하동 가족 등록 요청을 검증한다. 기본 통합 테스트 DB는 H2의 MariaDB 호환 모드다.
