import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

// 1. 環境変数の読み込み
String clientId = System.getenv("LW_CLIENT_ID");
String clientSecret = System.getenv("LW_CLIENT_SECRET");
String serviceAccount = System.getenv("LW_SERVICE_ACCOUNT");
String botId = System.getenv("LW_BOT_ID");
String targetUser = System.getenv("LW_TARGET_USER");
String keyPath = System.getenv("LW_PRIVATE_KEY_PATH");

// 2. 秘密鍵ファイルの読み込みと整形（※ドットを行末に配置）
String privateKeyString = Files.readString(Path.of(keyPath));
String cleanKey = privateKeyString.
    replace("-----BEGIN PRIVATE KEY-----", "").
    replace("-----END PRIVATE KEY-----", "").
    replaceAll("\\s+", "");

// 3. JWTの生成
String header = "{\"alg\":\"RS256\",\"typ\":\"JWT\"}";
long iat = System.currentTimeMillis() / 1000;
long exp = iat + 3600;
String payload = String.format("{\"iss\":\"%s\",\"sub\":\"%s\",\"iat\":%d,\"exp\":%d}", clientId, serviceAccount, iat, exp);

String encodedHeader = Base64.getUrlEncoder().withoutPadding().encodeToString(header.getBytes(StandardCharsets.UTF_8));
String encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
String dataToSign = encodedHeader + "." + encodedPayload;

PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(cleanKey));
PrivateKey privateKey = KeyFactory.getInstance("RSA").generatePrivate(keySpec);

Signature signature = Signature.getInstance("SHA256withRSA");
signature.initSign(privateKey);
signature.update(dataToSign.getBytes(StandardCharsets.UTF_8));
String encodedSignature = Base64.getUrlEncoder().withoutPadding().encodeToString(signature.sign());

String jwt = dataToSign + "." + encodedSignature;
System.out.println("JWT generated.");

// 4. アクセストークンの取得（※プラスを行末に配置）
String tokenUrl = "https://auth.worksmobile.com/oauth2/v2.0/token";
String tokenBody = "assertion=" + URLEncoder.encode(jwt, StandardCharsets.UTF_8) +
    "&grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer" +
    "&client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8) +
    "&client_secret=" + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8) +
    "&scope=bot";

// （※ドットを行末に配置）
HttpRequest tokenReq = HttpRequest.newBuilder().
    uri(URI.create(tokenUrl)).
    header("Content-Type", "application/x-www-form-urlencoded").
    POST(HttpRequest.BodyPublishers.ofString(tokenBody)).
    build();

HttpResponse<String> tokenRes = HttpClient.newHttpClient().send(tokenReq, HttpResponse.BodyHandlers.ofString());
String accessToken = tokenRes.body().split("\"access_token\":\"")[1].split("\"")[0];
System.out.println("Access Token obtained.");

// 5. メッセージの送信
String msgUrl = "https://www.worksapis.com/v1.0/bots/" + botId + "/users/" + targetUser + "/messages";
String jsonBody = "{\"content\": {\"type\": \"text\", \"text\": \"🤖 環境変数からのテスト通知です！\"}}";

HttpRequest msgReq = HttpRequest.newBuilder().
    uri(URI.create(msgUrl)).
    header("Authorization", "Bearer " + accessToken).
    header("Content-Type", "application/json").
    POST(HttpRequest.BodyPublishers.ofString(jsonBody)).
    build();

HttpResponse<String> msgRes = HttpClient.newHttpClient().send(msgReq, HttpResponse.BodyHandlers.ofString());
System.out.println("Response Status: " + msgRes.statusCode());
System.out.println("Response Body: " + msgRes.body());
