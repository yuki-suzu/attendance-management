import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

// 1. 環境変数の読み込み
String companyUrl = System.getenv("HRMOS_COMPANY_URL");
String secretKey = System.getenv("HRMOS_SECRET_KEY");

System.out.println("🚀 1. HRMOSから認証Tokenを取得します...");
String tokenUrl = "https://ieyasu.co/api/" + companyUrl + "/v1/authentication/token";

// （※ jshell対策でドットを行末に配置）
HttpRequest tokenReq = HttpRequest.newBuilder().
    uri(URI.create(tokenUrl)).
    header("Authorization", "Basic " + secretKey).
    header("Content-Type", "application/json").
    GET().
    build();

HttpClient client = HttpClient.newHttpClient();
HttpResponse<String> tokenRes = client.send(tokenReq, HttpResponse.BodyHandlers.ofString());

if (tokenRes.statusCode() != 200) {
    System.err.println("❌ Token取得失敗: " + tokenRes.statusCode());
    System.err.println(tokenRes.body());
} else {
    // JSONから無理やり token の値を抽出（外部ライブラリ不使用）
    String token = tokenRes.body().split("\"token\":\"")[1].split("\"")[0];
    System.out.println("✅ Token取得成功: " + token.substring(0, 5) + "...");

    System.out.println("\n🚀 2. ユーザー一覧（最大5件）を取得します...");
    String usersUrl = "https://ieyasu.co/api/" + companyUrl + "/v1/users?limit=5";

    // HRMOSのAPIは Bearer ではなく Token というプレフィックスを使います
    HttpRequest usersReq = HttpRequest.newBuilder().
        uri(URI.create(usersUrl)).
        header("Authorization", "Token " + token).
        header("Content-Type", "application/json").
        GET().
        build();

    HttpResponse<String> usersRes = client.send(usersReq, HttpResponse.BodyHandlers.ofString());

    if (usersRes.statusCode() == 200) {
        System.out.println("✅ ユーザー一覧取得成功！(Status: 200)");
        System.out.println("=========================================");
        System.out.println(usersRes.body());
        System.out.println("=========================================");
    } else {
        System.err.println("❌ ユーザー一覧取得失敗: " + usersRes.statusCode());
        System.err.println(usersRes.body());
    }
}
