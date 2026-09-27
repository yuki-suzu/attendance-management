# Attendance Management Service

HRMOS API から取得した勤怠データや従業員マスタをローカルデータベースと同期し、勤怠状況の分析・集計を行うコアエンジンです。未打刻（遅刻・打刻忘れ）や月次サマリの異常を検知し、Kafka
を通じて通知サービスへイベントを発行します。

## ⚙️ テックスタック

* **Language**: Java 25
* **Framework**: Spring Boot 3
* **Database**: PostgreSQL 16
* **O/R Mapper (更新系)**: Spring Data JDBC
* **SQL Builder (参照系・一部更新)**: jOOQ
* **DB Migration**: Flyway
* **Distributed Lock**: ShedLock (バッチの多重実行制御)
* **Messaging**: Spring Kafka (Producer)
* **Object Mapper**: MapStruct, Jackson

## 🚀 アプリケーションの仕様・責務

1. **外部データ同期**: HRMOS API から従業員、部門、勤務区分、打刻履歴、勤怠実績を同期。
2. **出勤状況判定**: HRMOS の実績とマスタ設定を比較し、「未出勤」「遅刻 / 打刻忘れ」などの状態を動的に判定。
3. **月次サマリ集計**: 各従業員の勤怠実績（予定休・当欠・半休・遅延）を集計し、差分のみを通知。
4. **イベント発行 (Event-Driven)**: 抽出した業務イベントを Kafka へ非同期に Publish。
5. **多重実行防止・履歴管理**: ShedLock を用いたスケジュールロック制御と、`t_checked_employee`
   を用いた当日中の重複通知防止。

## 🕒 スケジュール (Cron ジョブ)

以下のバッチ処理が `@Scheduled` アノテーションにより自動実行されます。設定値は環境変数 (`.env` /
`application.yaml`) で制御可能です。

| クラス名                                | 処理内容                  | プロパティ設定キー                        |
|:------------------------------------|:----------------------|:---------------------------------|
| `UnstampedAlertScheduler`           | 未打刻アラートバッチ（午前）        | `app.batch.alert-cron1`          |
| `UnstampedAlertScheduler`           | 未打刻アラートバッチ（午後）        | `app.batch.alert-cron2`          |
| `SyncMasterScheduler`               | HRMOS マスタ同期（拠点・部門・社員） | `app.batch.sync-master-cron`     |
| `MonthlyAttendanceSummaryScheduler` | 月次勤怠サマリの自動集計・通知       | `app.batch.monthly-summary-cron` |
| `TableDataCleanupScheduler`         | 不要な履歴レコードの物理削除（パージ）   | `app.batch.cleanup-cron`         |

*※ `SyncMasterScheduler` はシステム起動時 (`ApplicationReadyEvent`) にも初期同期を実行します。*

## 🌐 エンドポイント一覧 (REST API)

手動実行や外部システムとの連携用に以下の API を提供します。（ベースパス: `/api/v1`）

### 運用保守・バッチ手動実行 API

| メソッド | エンドポイント                                        | 説明                  |
|:-----|:-----------------------------------------------|:--------------------|
| POST | `/admin/maintenance/cleanup/checked-employees` | チェック済み履歴のパージ実行      |
| POST | `/admin/maintenance/cleanup/monthly-summaries` | 古い月次サマリのパージ実行       |
| POST | `/admin/employees/sync`                        | 従業員マスタ同期の手動トリガー     |
| POST | `/sync/master`                                 | 部門・勤務区分マスタ同期の手動トリガー |
| POST | `/attendances/alerts/unstamped`                | 未打刻アラート処理の手動トリガー    |
| POST | `/attendances/summary/monthly`                 | 月次サマリ集計処理の手動トリガー    |

### データ取得 API

| メソッド | エンドポイント                              | 説明               |
|:-----|:-------------------------------------|:-----------------|
| GET  | `/attendance`                        | 特定日の全従業員出勤状況取得   |
| GET  | `/attendances/unstamped`             | 特定日の未打刻者リスト取得    |
| GET  | `/stamp-logs/daily`                  | 特定日の全従業員打刻履歴取得   |
| GET  | `/stamp-logs/users/{employeeNumber}` | 特定ユーザーの期間内打刻履歴取得 |

## 📨 Kafka トピック (Producer)

以下のトピックに対し、メッセージを発行します。

* `notification-topic`: システムエラー時のバッチ障害通知（緊急通報）。
* `unstamped-alert-topic`: 管理者向け未打刻サマリアラート。
* `unstamped-direct-topic`: 未打刻者本人向け個別DMリマインド（※ `app.kafka.direct-reminder-enabled` が
  `true` の場合のみ発火）。
* `attendance-irregularity-topic`: 差分が検知された勤怠不良者・月次サマリレポート。
