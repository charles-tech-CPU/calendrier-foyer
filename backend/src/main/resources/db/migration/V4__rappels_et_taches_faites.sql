-- Rappel : nombre de minutes avant le debut de l'evenement ou une notification
-- s'affiche dans l'appli (0 = a l'heure pile, NULL = pas de rappel). Pour un
-- evenement "journee entiere", le decompte part de 8h le jour meme.
ALTER TABLE event ADD COLUMN reminder_minutes INTEGER;
ALTER TABLE event ADD CONSTRAINT chk_event_reminder CHECK (reminder_minutes IS NULL OR reminder_minutes >= 0);

-- Occurrences cochees "c'est fait" (taches : ranger sa chambre, faire son lit...).
-- Seules les occurrences cochees sont stockees : une ligne = (evenement, date).
CREATE TABLE event_done (
    event_id BIGINT NOT NULL REFERENCES event (id) ON DELETE CASCADE,
    occurrence_date DATE NOT NULL,
    PRIMARY KEY (event_id, occurrence_date)
);
CREATE INDEX idx_event_done_date ON event_done (occurrence_date);
