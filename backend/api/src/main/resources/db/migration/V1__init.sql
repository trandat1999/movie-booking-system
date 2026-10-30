-- Hạ tầng chung. Bảng nghiệp vụ được thêm từ V2 trở đi theo từng giai đoạn (PLAN.md mục 7).

-- Khóa phân tán cho job định kỳ (ShedLock)
CREATE TABLE shedlock (
    name       VARCHAR(64)  NOT NULL PRIMARY KEY,
    lock_until TIMESTAMPTZ  NOT NULL,
    locked_at  TIMESTAMPTZ  NOT NULL,
    locked_by  VARCHAR(255) NOT NULL
);
