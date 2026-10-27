-- SecureIQ Real-Time Exam Scheduling and Access Control Schema Migration
-- Timezone Standard: Asia/Kolkata (Persisted as UTC/Instant or DATE in PostgreSQL)

-- 1. Add scheduled_end_date and authoritative Instant columns to exams table
ALTER TABLE exams
    ADD COLUMN IF NOT EXISTS scheduled_end_date DATE,
    ADD COLUMN IF NOT EXISTS scheduled_start_instant TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS scheduled_end_instant TIMESTAMP WITH TIME ZONE;

-- 2. Backfill existing exams: default scheduled_end_date to scheduled_date if null
UPDATE exams
SET scheduled_end_date = scheduled_date
WHERE scheduled_end_date IS NULL AND scheduled_date IS NOT NULL;

-- 3. Backfill authoritative instants using Asia/Kolkata (+05:30) offset
UPDATE exams
SET scheduled_start_instant = (scheduled_date || ' ' || start_time)::timestamp AT TIME ZONE 'Asia/Kolkata',
    scheduled_end_instant = (COALESCE(scheduled_end_date, scheduled_date) || ' ' || end_time)::timestamp AT TIME ZONE 'Asia/Kolkata'
WHERE scheduled_start_instant IS NULL AND scheduled_date IS NOT NULL AND start_time IS NOT NULL AND end_time IS NOT NULL;

-- 4. Create index for fast scheduled window queries
CREATE INDEX IF NOT EXISTS idx_exams_scheduled_instants
    ON exams (scheduled_start_instant, scheduled_end_instant);

-- 5. Create index for student attempts lookup by student and session
CREATE INDEX IF NOT EXISTS idx_student_exam_attempts_student_session
    ON student_exam_attempts (student_id, exam_session_id);
