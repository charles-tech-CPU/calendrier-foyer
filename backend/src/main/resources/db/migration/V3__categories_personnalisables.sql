-- Les categories (nom + icone) deviennent des donnees modifiables depuis l'appli,
-- au lieu d'une liste figee dans le code. On reprend les categories existantes
-- et on rattache les evenements deja crees a la bonne ligne.
CREATE TABLE category (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(60) NOT NULL,
    icon VARCHAR(64) NOT NULL,
    position INTEGER NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uq_category_name ON category (LOWER(name));

-- Colonne temporaire "code" : sert uniquement a migrer les evenements existants
ALTER TABLE category ADD COLUMN code VARCHAR(20);
INSERT INTO category (code, name, icon, position) VALUES
    ('BIRTHDAY', 'Anniversaire', '🎂', 1),
    ('GARDE', 'Garde', '🛡️', 2),
    ('MEDICAL', 'Médecin', '🩺', 3),
    ('SCHOOL', 'École', '🎒', 4),
    ('SPORT', 'Sport', '⚽', 5),
    ('WORK', 'Travail', '💼', 6),
    ('PARTY', 'Fête', '🎉', 7),
    ('MEETING', 'Rendez-vous', '🤝', 8),
    ('MEAL', 'Repas', '🍽️', 9),
    ('TRAVEL', 'Vacances', '✈️', 10),
    ('SHOPPING', 'Courses', '🛒', 11),
    ('TRASH', 'Poubelles', '🗑️', 12),
    ('HOME', 'Maison', '🏠', 13),
    ('CAR', 'Voiture', '🚗', 14),
    ('ANIMAL', 'Animaux', '🐾', 15),
    ('BEAUTY', 'Coiffeur', '💇', 16),
    ('ADMIN', 'Papiers', '📄', 17),
    -- Taches de la maison
    (NULL, 'Lave-vaisselle', '🧽', 18),
    (NULL, 'Ranger la chambre', '🧸', 19),
    (NULL, 'Faire le lit', '🛏️', 20),
    (NULL, 'Jardin', '🌻', 21),
    (NULL, 'Contrôle technique', '🛠️', 22);

-- Supprimer une categorie ne supprime pas ses evenements : ils redeviennent "sans categorie"
ALTER TABLE event ADD COLUMN category_id BIGINT REFERENCES category (id) ON DELETE SET NULL;
UPDATE event e SET category_id = c.id FROM category c WHERE c.code = e.category;
CREATE INDEX idx_event_category ON event (category_id);

ALTER TABLE event DROP CONSTRAINT chk_event_category;
ALTER TABLE event DROP COLUMN category;
ALTER TABLE category DROP COLUMN code;
