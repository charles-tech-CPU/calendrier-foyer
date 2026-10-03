# Calendrier Foyer

Calendrier unique et partagé du foyer, pensé pour être consulté et modifié au
doigt sur une tablette murale : vue mensuelle, évènements ponctuels ou
récurrents (quotidien/hebdomadaire/mensuel/annuel), gros boutons et cases
tactiles. Backend Java / Spring Boot, frontend Vue 3, base PostgreSQL.

Cinquième projet **indépendant** de `lol-results`, `foot-results`,
`tennis-results` et `budget-foyer` : repo séparé, base séparée, ports
différents (backend 8084, frontend 5177) pour pouvoir faire tourner les cinq
en parallèle.

## 1. Prérequis

Identiques aux quatre autres projets : Java 21 (JDK), Maven, Node.js 20+/npm,
PostgreSQL 14+, Git.

## 2. Principe du projet

- Un seul calendrier, partagé par tout le foyer (pas de calendriers séparés
  par personne dans cette v1 - vous avez choisi cette option pour démarrer
  simple).
- Un **évènement** (`Event`) est soit **ponctuel** (une seule date), soit
  **récurrent** : quotidien, hebdomadaire (avec les jours de la semaine de
  ton choix, ex: lundi/mercredi/vendredi), mensuel (même jour du mois, ex:
  "le 1er de chaque mois" - si ce jour n'existe pas dans un mois plus court,
  l'appli se cale automatiquement sur le dernier jour du mois, ex: le 31
  janvier devient le 28 février) ou annuel.
- **Les occurrences futures d'un évènement récurrent ne sont jamais stockées
  en base** : elles sont recalculées à la volée à chaque fois que tu ouvres
  un mois, exactement comme le classement tennis ou les totaux du budget ne
  sont jamais stockés non plus - toujours le même principe dans tous ces
  projets : ne jamais dupliquer une information qui peut être recalculée de
  façon fiable à partir de la règle d'origine.
- Un évènement peut avoir une **couleur** libre, et une **répétition** avec
  un intervalle (ex: "toutes les 2 semaines") et, optionnellement, une date
  de fin (sinon la série ne s'arrête jamais).

### Simplifications volontaires (v1)

- **Modifier ou supprimer un évènement récurrent agit sur toute la série**,
  pas seulement sur une occurrence précise (pas de "cette occurrence
  seulement" vs "toute la série" comme dans Google Calendar). Si un jour tu
  as besoin d'annuler une seule occurrence sans toucher au reste (ex: la
  garderie fermée exceptionnellement un jour donné), demande à Claude Code
  d'ajouter une table d'exceptions - c'est un ajout raisonnable mais qui
  mérite d'être fait volontairement plutôt que d'alourdir cette v1.
- **Pas de calendriers séparés par membre du foyer** (un seul calendrier
  partagé, comme choisi) - une couleur par évènement permet déjà de s'y
  retrouver visuellement.
- **Pas de synchronisation avec Google Calendar ou un autre calendrier
  externe** - cette appli est autonome, comme choisi, ce qui évite toute la
  complexité d'authentification OAuth. Si l'envie vient plus tard, c'est un
  projet à part entière à cadrer séparément.
- **Un évènement non journée-entière ne peut pas s'étaler sur plusieurs
  jours** dans cette v1 (ex: pas de "vacances du 10 au 15 juillet" en un
  seul évènement avec horaires) - pour un évènement multi-jours, utilise
  "journée entière" ou crée un évènement par jour.

## 3. Base de données

```bash
psql -U postgres
```

```sql
CREATE USER calendrier_user WITH PASSWORD 'calendrier_password';
CREATE DATABASE calendrier_foyer OWNER calendrier_user;
\q
```

## 4. Git

```bash
cd calendrier-foyer
git init
git add .
git commit -m "Scaffold initial : backend Spring Boot, frontend Vue 3, calendrier avec recurrence calculee a la volee"
```

## 5. Backend (Spring Boot)

```bash
cd backend
mvn spring-boot:run
```

Flyway applique automatiquement `V1__init.sql` (schéma + un évènement de
bienvenue à titre d'exemple). L'API démarre sur `http://localhost:8084`.

| Méthode | URL | Description |
|---|---|---|
| GET | `/api/events?from=AAAA-MM-JJ&to=AAAA-MM-JJ` | Occurrences (ponctuelles + récurrentes calculées) visibles sur la plage |
| GET | `/api/events/{id}` | Définition complète d'un évènement/série (pour l'écran d'édition) |
| POST | `/api/events` | Créer un évènement |
| PUT | `/api/events/{id}` | Modifier un évènement (toute la série si récurrent) |
| DELETE | `/api/events/{id}` | Supprimer un évènement (toute la série si récurrent) |

⚠️ Comme pour les quatre autres projets, **je n'ai pas pu compiler ce
backend** dans mon bac à sable (Maven Central bloqué). J'ai en revanche
**validé l'intégralité du schéma SQL sur une vraie base PostgreSQL**
(création de la table, toutes les contraintes testées une par une :
évènement non journée-entière sans heure rejeté, heure de fin avant heure de
début rejetée, fréquence de récurrence invalide rejetée), et surtout **j'ai
rejoué l'algorithme de calcul des récurrences en Python** (miroir exact de
`RecurrenceService.java`) sur des cas réalistes insérés en base - garderie
quotidienne du lundi au vendredi, rendez-vous mensuel avec date de fin,
anniversaire annuel, évènement ponctuel passé vs à venir - et vérifié que la
requête SQL de présélection (`EventRepository.findRelevantForRange`) et le
calcul des occurrences produisent exactement les dates attendues pour un
mois donné. Le premier `mvn spring-boot:run` chez toi reste le vrai test
pour la compilation Java elle-même - Claude Code, avec un vrai compilateur
sous la main, corrigera vite une éventuelle erreur de syntaxe.

## 6. Frontend (Vue 3)

```bash
cd frontend
npm install
npm run dev
```

Démarre sur `http://localhost:5177`, appelle l'API sur `http://localhost:8084`
(CORS déjà configuré). Écran unique, pensé pour le tactile :

- **Vue mensuelle** en grille (grosses cases, minimum 48px de cible tactile
  partout) avec navigation mois précédent/suivant, et les évènements du jour
  affichés en pastilles colorées.
- **Tape sur un jour** pour voir la liste complète de ses évènements, avec un
  bouton "+ Ajouter un évènement" toujours visible.
- **Tape sur un évènement** pour le modifier ou le supprimer (toute la
  série s'il est récurrent).
- Le formulaire de création/édition propose journée entière ou horaires
  précis, lieu, description, couleur, et la récurrence (aucune / quotidienne
  / hebdomadaire avec jours au choix / mensuelle / annuelle) avec intervalle
  et date de fin optionnelle.

## 7. Pistes d'évolution (hors v1)

- Exceptions ponctuelles sur une série récurrente (annuler ou déplacer une
  seule occurrence sans toucher au reste).
- Calendriers séparés par membre du foyer, avec filtre d'affichage.
- Synchronisation avec Google Calendar (ou un autre calendrier externe) via
  API, pour voir les évènements aussi sur les téléphones.
- Évènements multi-jours avec horaires.
- Rappels/notifications avant un évènement.

## Structure du repo

```
calendrier-foyer/
├── backend/    Spring Boot (Java 21, Maven, PostgreSQL, Flyway)
└── frontend/   Vue 3 + Vite (interface tactile, vue mensuelle)
```
