-- [jooq ignore start]
-- jOOQの自動生成対象外（FlywayでのDB構築時のみ実行される）
CREATE OR REPLACE FUNCTION update_timestamp()
RETURNS TRIGGER AS $$BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
RETURN NEW;
END;$$ language 'plpgsql';


-- 自動更新トリガーの設定
CREATE TRIGGER trigger_m_department_updated_at
    BEFORE UPDATE ON m_department
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE TRIGGER trigger_m_segment_updated_at
    BEFORE UPDATE ON m_segment
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE TRIGGER trigger_m_employee_updated_at
    BEFORE UPDATE ON m_employee
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

-- [jooq ignore stop]
