package com.computer_rescuer.attendance_management.infrastructure.config;

import static com.computer_rescuer.attendance_management.shared.DateTimeConstants.JST;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * システム共通の日時操作コンポーネントを定義する構成クラス。
 * <p>
 * アーキテクチャ上の Infrastructure 層に位置し、外部環境依存であるシステム時計およびタイムゾーンを Spring DI コンテナへ提供します。<br>
 * 本クラスで日本標準時（JST）を内包した {@link Clock} Bean を一元管理することで、
 * 各ビジネスロジックでの個別タイムゾーン指定を不要にし、単体テスト時の決定論的な日時シミュレーションを可能にします。
 * </p>
 */
@Configuration
public class DateTimeConfig {

  /**
   * アプリケーション全体で使用する、日本標準時（JST）に基づくシステム時計の Bean を生成します。
   * <p>
   * 呼び出し側の各コンポーネントは、本 Bean をインジェクションして {@code LocalDate.now(clock)} や
   * {@code ZonedDateTime.now(clock)} を呼び出すことで、タイムゾーン定数を直接指定することなく 統一された日本標準時の日時を取得できます。
   * </p>
   *
   * @return JST（Asia/Tokyo）タイムゾーンが設定された {@link Clock} インスタンス
   */
  @Bean
  public Clock clock() {
    return Clock.system(JST);
  }
}
