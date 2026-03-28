-- 従業員テーブル
CREATE TABLE employee
(
    id            BIGSERIAL PRIMARY KEY,
    employee_name VARCHAR(100) NOT NULL,
    department_id VARCHAR(50)  NOT NULL
);

-- 勤怠テーブル
CREATE TABLE attendance
(
    id            BIGSERIAL PRIMARY KEY,
    employee_id   BIGINT     NOT NULL,
    target_month  VARCHAR(6) NOT NULL,
    working_hours INT        NOT NULL,
    late_flag     BOOLEAN    NOT NULL DEFAULT FALSE,
    FOREIGN KEY (employee_id) REFERENCES employee (id)
);
