-- =========================================================================
-- チェック済みID TBL (t_checked_employee)
-- =========================================================================
CREATE TABLE t_checked_employee (
    target_date DATE NOT NULL,
    employee_id INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    reason_code char(2),
    PRIMARY KEY (target_date, employee_id)
);

COMMENT ON TABLE t_checked_employee IS 'チェック済みID TBL';
COMMENT ON COLUMN t_checked_employee.target_date IS 'チェック対象日 (例: 2026-04-05)';
COMMENT ON COLUMN t_checked_employee.employee_id IS 'チェック完了した社員ID';
COMMENT ON COLUMN t_checked_employee.created_at IS 'チェック完了日時';
COMMENT ON COLUMN t_checked_employee.reason_code IS '完了事由';
