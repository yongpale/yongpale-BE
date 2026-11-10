import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 돌봄시설 CSV의 주소를 카카오 로컬 API로 좌표(lat, lng)로 바꿔 채운다. 한 번만 실행하는 도구다.
 *
 * 실행 (프로젝트 루트에서, Java 11 이상)
 *   KAKAO_REST_API_KEY=<카카오 REST API 키> java scripts/GeocodeFacilities.java [CSV 경로]
 *   CSV 경로를 생략하면 src/main/resources/data/facilities.csv
 *
 * - 이미 lat, lng가 있는 행은 건너뛴다.
 * - 주소 검색 → (상세 주소를 뗀) 도로명·지번 주소 검색 → 시설명 키워드 검색 순으로 찾는다.
 * - 카카오 응답의 y = 위도(lat), x = 경도(lng). DB 컬럼 scale에 맞춰 소수점 7자리로 저장한다.
 * - 끝까지 못 찾은 시설은 목록으로 출력하고 비워 둔다.
 */
public class GeocodeFacilities {

    private static final String ADDRESS_API = "https://dapi.kakao.com/v2/local/search/address.json?query=";
    private static final String KEYWORD_API = "https://dapi.kakao.com/v2/local/search/keyword.json?query=";
    // "…전대로 99 2층" → "…전대로 99": 공백 뒤 첫 건물번호(또는 지번)까지만 남긴다
    private static final Pattern BASE_ADDRESS = Pattern.compile("^(.+?\\s\\d+(?:-\\d+)?)(?=\\s|$)");
    private static final Pattern X = Pattern.compile("\"x\":\"([0-9.]+)\"");
    private static final Pattern Y = Pattern.compile("\"y\":\"([0-9.]+)\"");
    private static final Pattern ADDRESS_NAME = Pattern.compile("\"address_name\":\"([^\"]*)\"");

    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private static String apiKey;

    public static void main(String[] args) throws Exception {
        apiKey = System.getenv("KAKAO_REST_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("환경변수 KAKAO_REST_API_KEY가 없습니다.");
            System.exit(1);
        }
        Path csv = Path.of(args.length > 0 ? args[0] : "src/main/resources/data/facilities.csv");
        String content = Files.readString(csv, StandardCharsets.UTF_8);
        boolean bom = content.startsWith("﻿");
        List<List<String>> rows = parse(bom ? content.substring(1) : content);

        List<String> header = rows.get(0);
        int name = header.indexOf("name"), gu = header.indexOf("gu"), address = header.indexOf("address");
        int lat = header.indexOf("lat"), lng = header.indexOf("lng");

        int filled = 0, skipped = 0;
        List<String> failed = new ArrayList<>();
        for (List<String> row : rows.subList(1, rows.size())) {
            if (!row.get(lat).isBlank() && !row.get(lng).isBlank()) {
                skipped++;
                continue;
            }
            String[] point = geocode(row.get(name), row.get(gu), row.get(address));
            if (point == null) {
                failed.add(row.get(name) + " | " + row.get(address));
                continue;
            }
            row.set(lat, point[0]);
            row.set(lng, point[1]);
            filled++;
            Thread.sleep(100);
        }

        Files.writeString(csv, (bom ? "﻿" : "") + write(rows), StandardCharsets.UTF_8);
        System.out.printf("좌표 채움 %d곳, 이미 있음 %d곳, 실패 %d곳%n", filled, skipped, failed.size());
        failed.forEach(f -> System.out.println("  실패: " + f));
    }

    /** {위도, 경도} 또는 못 찾으면 null */
    private static String[] geocode(String name, String gu, String address) throws IOException, InterruptedException {
        String full = address.split(",")[0].strip();
        String[] point = search(ADDRESS_API, full, null);
        if (point == null) {
            Matcher base = BASE_ADDRESS.matcher(full);
            if (base.find()) {
                point = search(ADDRESS_API, base.group(1), null);
            }
        }
        if (point == null) {
            // 키워드 검색은 동명이 다른 곳이 나올 수 있어서, 결과 주소에 같은 구가 있을 때만 쓴다
            point = search(KEYWORD_API, "용인 " + name, gu);
        }
        return point;
    }

    private static String[] search(String api, String query, String requiredGu) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(api + URLEncoder.encode(query, StandardCharsets.UTF_8)))
                .header("Authorization", "KakaoAK " + apiKey)
                .timeout(Duration.ofSeconds(10))
                .GET().build();
        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            throw new IllegalStateException("카카오 API 오류 " + response.statusCode() + ": " + response.body());
        }
        String body = response.body();
        int start = body.indexOf("\"documents\":[{");
        if (start < 0) {
            return null;
        }
        String first = body.substring(start);
        if (requiredGu != null) {
            Matcher addressName = ADDRESS_NAME.matcher(first);
            if (!addressName.find() || !addressName.group(1).contains(requiredGu)) {
                return null;
            }
        }
        Matcher x = X.matcher(first), y = Y.matcher(first);
        if (!x.find() || !y.find()) {
            return null;
        }
        return new String[]{round(y.group(1)), round(x.group(1))};
    }

    private static String round(String value) {
        return new java.math.BigDecimal(value).setScale(7, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    // ---- RFC 4180 CSV (따옴표 안의 쉼표·줄바꿈 처리) ----

    private static List<List<String>> parse(String text) {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quoted) {
                if (c == '"' && i + 1 < text.length() && text.charAt(i + 1) == '"') {
                    field.append('"');
                    i++;
                } else if (c == '"') {
                    quoted = false;
                } else {
                    field.append(c);
                }
            } else if (c == '"') {
                quoted = true;
            } else if (c == ',') {
                row.add(field.toString());
                field.setLength(0);
            } else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                    i++;
                }
                row.add(field.toString());
                field.setLength(0);
                rows.add(row);
                row = new ArrayList<>();
            } else {
                field.append(c);
            }
        }
        if (field.length() > 0 || !row.isEmpty()) {
            row.add(field.toString());
            rows.add(row);
        }
        return rows;
    }

    private static String write(List<List<String>> rows) {
        StringBuilder out = new StringBuilder();
        for (List<String> row : rows) {
            for (int i = 0; i < row.size(); i++) {
                if (i > 0) out.append(',');
                String value = row.get(i);
                if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
                    out.append('"').append(value.replace("\"", "\"\"")).append('"');
                } else {
                    out.append(value);
                }
            }
            out.append('\n');
        }
        return out.toString();
    }
}
