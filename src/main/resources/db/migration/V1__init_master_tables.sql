-- =========================================================================
-- [共通関数] 更新日時(updated_at)の自動更新トリガー用関数
-- ※PostgreSQLはMySQLの `ON UPDATE CURRENT_TIMESTAMP` がないため関数を定義します
-- =========================================================================
CREATE OR REPLACE FUNCTION update_timestamp()
RETURNS TRIGGER AS $$BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
RETURN NEW;
END;$$ LANGUAGE plpgsql;


-- =========================================================================
-- 1. 部門マスタ TBL (m_department)
-- =========================================================================
CREATE TABLE m_department (
    id INTEGER NOT NULL,
    name VARCHAR(100) NOT NULL,
    sequence INTEGER,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

COMMENT ON TABLE m_department IS '部門マスタ TBL';
COMMENT ON COLUMN m_department.id IS '部門ID (HRMOSの部門ID)';
COMMENT ON COLUMN m_department.name IS '部門名';
COMMENT ON COLUMN m_department.sequence IS '並び順';
COMMENT ON COLUMN m_department.created_at IS 'システム作成日時';
COMMENT ON COLUMN m_department.updated_at IS 'システム更新日時';

-- 自動更新トリガーの設定
CREATE TRIGGER trigger_m_department_updated_at
    BEFORE UPDATE ON m_department
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();


-- =========================================================================
-- 2. 勤務区分マスタ TBL (m_segment)
-- =========================================================================
CREATE TABLE m_segment (
    id INTEGER NOT NULL,
    title VARCHAR(100) NOT NULL,
    display_title VARCHAR(100),
    status INTEGER NOT NULL,
    start_at TIME,
    end_at TIME,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

COMMENT ON TABLE m_segment IS '勤務区分マスタ TBL';
COMMENT ON COLUMN m_segment.id IS '勤務区分ID (HRMOSの勤務区分ID)';
COMMENT ON COLUMN m_segment.title IS '勤務区分名 (例: 出勤)';
COMMENT ON COLUMN m_segment.display_title IS '表示名';
COMMENT ON COLUMN m_segment.status IS '状態 (1: 勤務, 2: 休日・休暇, 3: その他)';
COMMENT ON COLUMN m_segment.start_at IS '基準となる勤務開始時間 (例: 09:00:00)';
COMMENT ON COLUMN m_segment.end_at IS '基準となる勤務終了時間';

CREATE TRIGGER trigger_m_segment_updated_at
    BEFORE UPDATE ON m_segment
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();


-- =========================================================================
-- 3. 従業員マスタ TBL (m_employee)
-- ※HRMOSのIDをそのまま使うため、あえて外部キー制約はつけていません（洗い替え時のエラー防止）
-- =========================================================================
CREATE TABLE m_employee (
    id INTEGER NOT NULL,
    employee_number VARCHAR(50),
    last_name VARCHAR(100) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    email VARCHAR(255),
    department_id INTEGER,
    default_segment_id INTEGER,
    employment_id INTEGER,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

COMMENT ON TABLE m_employee IS '従業員マスタ TBL';
COMMENT ON COLUMN m_employee.id IS '社員ID (HRMOSの内部ID)';
COMMENT ON COLUMN m_employee.employee_number IS '社員番号';
COMMENT ON COLUMN m_employee.last_name IS '姓';
COMMENT ON COLUMN m_employee.first_name IS '名';
COMMENT ON COLUMN m_employee.email IS 'メールアドレス';
COMMENT ON COLUMN m_employee.department_id IS '部門ID (m_departmentと連携)';
COMMENT ON COLUMN m_employee.default_segment_id IS '初期表示の勤務区分ID (m_segmentと連携)';
COMMENT ON COLUMN m_employee.employment_id IS '雇用形態ID';

CREATE TRIGGER trigger_m_employee_updated_at
    BEFORE UPDATE ON m_employee
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();
