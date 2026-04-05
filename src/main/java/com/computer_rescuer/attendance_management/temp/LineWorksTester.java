package com.computer_rescuer.attendance_management.temp;

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
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LineWorksTester {

  // 1. 環境変数の読み込み
  private static final String CLIENT_ID = System.getenv("LW_CLIENT_ID");
  private static final String CLIENT_SECRET = System.getenv("LW_CLIENT_SECRET");
  private static final String SERVICE_ACCOUNT = System.getenv("LW_SERVICE_ACCOUNT");
  private static final String BOT_ID = System.getenv("LW_BOT_ID");
  private static final String TARGET_USER = System.getenv("LW_TARGET_USER");
  private static final String PRIVATE_KEY_PATH = System.getenv("LW_PRIVATE_KEY_PATH");

  static void main() throws Exception {
    log.info("🚀 1. JWT（署名付きトークン）を生成します...");
    String jwt = generateJwt();
    log.info("✅ JWT生成完了: " + jwt.substring(0, 30) + "...");

    log.info("\n🚀 2. アクセストークンを取得します...");
    String accessToken = getAccessToken(jwt);
    log.info("✅ トークン取得完了: " + accessToken.substring(0, 15) + "...");

    log.info("\n🚀 3. LINE WORKSへメッセージを送信します...");
    sendMessage(accessToken);
    log.info("🎉 全ての処理が完了しました！LINE WORKSを確認してください。");
  }

  /**
   * ① JWT（JSON Web Token）の生成処理
   */
  private static String generateJwt() throws Exception {
    String header = "{\"alg\":\"RS256\",\"typ\":\"JWT\"}";
    long iat = System.currentTimeMillis() / 1000;
    long exp = iat + 3600; // 有効期限は1時間
    String payload = String.format(
        "{\"iss\":\"%s\",\"sub\":\"%s\",\"iat\":%d,\"exp\":%d}",
        CLIENT_ID, SERVICE_ACCOUNT, iat, exp
    );

    String encodedHeader = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(header.getBytes(StandardCharsets.UTF_8));
    String encodedPayload = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    String dataToSign = encodedHeader + "." + encodedPayload;

    // Private Key文字列からヘッダーや改行を除去して復元
    // 2. 秘密鍵ファイルの読み込みと整形
    String privateKeyString = Files.readString(Path.of(PRIVATE_KEY_PATH));
    String cleanKey = privateKeyString
        .replace("-----BEGIN PRIVATE KEY-----", "")
        .replace("-----END PRIVATE KEY-----", "")
        .replaceAll("\\s+", "");

    PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(cleanKey));
    PrivateKey privateKey = KeyFactory.getInstance("RSA").generatePrivate(keySpec);

    // RSA-SHA256 で署名
    Signature signature = Signature.getInstance("SHA256withRSA");
    signature.initSign(privateKey);
    signature.update(dataToSign.getBytes(StandardCharsets.UTF_8));
    String encodedSignature = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(signature.sign());

    return dataToSign + "." + encodedSignature;
  }

  /**
   * ② アクセストークンの取得処理
   */
  private static String getAccessToken(String jwt) throws Exception {
    String url = "https://auth.worksmobile.com/oauth2/v2.0/token";
    String body = "assertion=" + URLEncoder.encode(jwt, StandardCharsets.UTF_8)
        + "&grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer"
        + "&CLIENT_ID=" + URLEncoder.encode(CLIENT_ID, StandardCharsets.UTF_8)
        + "&CLIENT_SECRET=" + URLEncoder.encode(CLIENT_SECRET, StandardCharsets.UTF_8)
        + "&scope=bot";

    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .header("Content-Type", "application/x-www-form-urlencoded")
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .build();

    try (HttpClient httpClient = HttpClient.newHttpClient()) {
      HttpResponse<String> response = httpClient.send(request,
          HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() != 200) {
        throw new RuntimeException("トークン取得失敗: " + response.body());
      }

      // 簡易パース（外部ライブラリなしで access_token の値を抽出）
      String responseBody = response.body();
      return responseBody.split("\"access_token\":\"")[1].split("\"")[0];
    }
  }

  /**
   * ③ メッセージ送信処理
   */
  private static void sendMessage(String accessToken) throws Exception {
    String url =
        "https://www.worksapis.com/v1.0/bots/" + BOT_ID + "/users/" + TARGET_USER + "/messages";

    // 送信するJSONメッセージ
    String jsonBody = """
        {
          "content": {
            "type": "text",
            "text": "🤖 【テスト通知】こんにちは！これはJava 25からの自動通知です。打刻をお忘れなく！"
          }
        }
        """;

    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .header("Authorization", "Bearer " + accessToken)
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
        .build();

    try (HttpClient httpClient = HttpClient.newHttpClient()) {
      HttpResponse<String> response = httpClient.send(request,
          HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() == 201) {
        log.info("✅ 送信成功: 201 Created");
      } else {
        log.error("❌ 送信失敗: {}", response.statusCode());
        log.error(response.body());
      }
    }

  }
}
