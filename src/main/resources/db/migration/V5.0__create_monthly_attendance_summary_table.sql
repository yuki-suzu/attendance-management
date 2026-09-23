-- =========================================================================
-- 月次勤怠サマリ TBL (t_monthly_attendance_summary)
-- =========================================================================
CREATE TABLE t_monthly_attendance_summary
(
    proc_month      VARCHAR(7)   NOT NULL,
    employee_number VARCHAR(50)  NOT NULL,
    segment_id      INTEGER      NOT NULL,
    segment_title   VARCHAR(100) NOT NULL,
    count           INTEGER      NOT NULL DEFAULT 0,
    msg_send        CHAR(1)      NOT NULL DEFAULT '0',
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (proc_month, employee_number, segment_id, segment_title)
);

COMMENT ON TABLE t_monthly_attendance_summary IS '月次勤怠サマリ TBL';
COMMENT ON COLUMN t_monthly_attendance_summary.proc_month IS '処理対象年月 (例: 2026-09)';
COMMENT ON COLUMN t_monthly_attendance_summary.employee_number IS '社員番号 (例: A0001)';
COMMENT ON COLUMN t_monthly_attendance_summary.segment_id IS '勤務区分ID (m_segment.id)';
COMMENT ON COLUMN t_monthly_attendance_summary.segment_title IS '勤務区分名 (マスタ変更対応用)';
COMMENT ON COLUMN t_monthly_attendance_summary.count IS '当月累積日数';
COMMENT ON COLUMN t_monthly_attendance_summary.msg_send IS '通知済みフラグ (0:未送信, 1:送信済)';
COMMENT ON COLUMN t_monthly_attendance_summary.created_at IS 'システム作成日時';
COMMENT ON COLUMN t_monthly_attendance_summary.updated_at IS 'システム更新日時';

-- 自動更新トリガーの設定 (V3で定義された update_timestamp() を利用)
-- [jooq ignore start]
CREATE TRIGGER trigger_t_monthly_attendance_summary_updated_at
    BEFORE UPDATE
    ON t_monthly_attendance_summary
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();
-- [jooq ignore stop]
