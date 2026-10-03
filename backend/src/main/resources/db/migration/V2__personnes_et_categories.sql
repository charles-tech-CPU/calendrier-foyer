-- Membres du foyer : chacun a sa couleur (unique), et sa date de naissance sert a
-- afficher automatiquement son anniversaire chaque annee (calcule a la volee,
-- jamais stocke comme evenement).
CREATE TABLE person (
    id BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(80) NOT NULL,
    last_name VARCHAR(80) NOT NULL,
    birth_date DATE NOT NULL,
    color VARCHAR(20) NOT NULL,
    CONSTRAINT uq_person_color UNIQUE (color)
);

-- Photo optionnelle, dans une table a part pour ne jamais la charger avec la
-- personne (listes, evenements...) : elle n'est lue que par GET /api/persons/{id}/photo.
CREATE TABLE person_photo (
    person_id BIGINT PRIMARY KEY REFERENCES person (id) ON DELETE CASCADE,
    content_type VARCHAR(50) NOT NULL,
    data BYTEA NOT NULL
);

-- Un evenement peut concerner une personne (il prend alors sa couleur) et a une categorie (icone).
ALTER TABLE event ADD COLUMN person_id BIGINT REFERENCES person (id) ON DELETE SET NULL;
ALTER TABLE event ADD COLUMN category VARCHAR(20) NOT NULL DEFAULT 'OTHER';
ALTER TABLE event ADD CONSTRAINT chk_event_category CHECK (category IN (
    'OTHER', 'BIRTHDAY', 'GARDE', 'MEDICAL', 'WORK', 'SCHOOL', 'SPORT', 'PARTY', 'MEAL',
    'TRAVEL', 'ADMIN', 'HOME', 'SHOPPING', 'TRASH', 'CAR', 'ANIMAL', 'BEAUTY', 'MEETING'));
CREATE INDEX idx_event_person ON event (person_id);
