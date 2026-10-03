-- Periode sur plusieurs jours (ex: vacances du 20 octobre au 2 novembre) :
-- l'evenement couvre chaque jour de start_date a end_date inclus. NULL = un seul jour.
-- Les jours de la periode ne sont jamais stockes : ils sont deduits a l'affichage.
-- Une periode est toujours en journee entiere (pas d'horaire a cheval sur plusieurs jours).
ALTER TABLE event ADD COLUMN end_date DATE;
ALTER TABLE event ADD CONSTRAINT chk_event_end_date CHECK (end_date IS NULL OR end_date > start_date);
ALTER TABLE event ADD CONSTRAINT chk_event_period_all_day CHECK (end_date IS NULL OR all_day = TRUE);
