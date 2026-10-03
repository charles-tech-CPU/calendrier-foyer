CREATE TABLE event (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(160) NOT NULL,
    description VARCHAR(2000),
    location VARCHAR(160),
    color VARCHAR(20),
    all_day BOOLEAN NOT NULL DEFAULT FALSE,
    start_date DATE NOT NULL,
    start_time TIME,
    end_time TIME,
    recurrence_frequency VARCHAR(10) NOT NULL DEFAULT 'NONE',
    recurrence_interval INTEGER NOT NULL DEFAULT 1,
    recurrence_days_of_week VARCHAR(30),
    recurrence_end_date DATE,
    CONSTRAINT chk_event_all_day_or_time CHECK (all_day = TRUE OR start_time IS NOT NULL),
    CONSTRAINT chk_event_time_order CHECK (start_time IS NULL OR end_time IS NULL OR end_time > start_time),
    CONSTRAINT chk_event_recurrence_interval CHECK (recurrence_interval >= 1),
    CONSTRAINT chk_event_recurrence_frequency CHECK (recurrence_frequency IN ('NONE','DAILY','WEEKLY','MONTHLY','YEARLY'))
);
CREATE INDEX idx_event_start_date ON event (start_date);

-- Quelques evenements de depart, a titre d'exemple (modifiables/supprimables ensuite).
INSERT INTO event (title, description, all_day, start_date, start_time, end_time, recurrence_frequency, recurrence_interval, color)
VALUES ('Bienvenue dans ton calendrier', 'Cree, modifie ou supprime cet evenement depuis l''appli.', TRUE, CURRENT_DATE, NULL, NULL, 'NONE', 1, '#4a90d9');
