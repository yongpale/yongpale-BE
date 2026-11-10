-- MariaDB 개발용 지역 예시. 실제 지역 기준 데이터 적재를 대체하지 않는다.
-- 기존 지역이 있으면 좌표·인구 등은 그대로 유지한다.
INSERT INTO regions (gu_name, dong_name, region_type)
VALUES ('기흥구', '구갈동', 'DONG')
ON DUPLICATE KEY UPDATE region_id = region_id;
