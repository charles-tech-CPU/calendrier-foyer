package com.charles.calendrierfoyer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.charles.calendrierfoyer.domain.Category;
import com.charles.calendrierfoyer.domain.Event;
import com.charles.calendrierfoyer.domain.EventDone;
import com.charles.calendrierfoyer.domain.Person;
import com.charles.calendrierfoyer.domain.RecurrenceFrequency;
import com.charles.calendrierfoyer.dto.EventCreateDto;
import com.charles.calendrierfoyer.dto.EventDto;
import com.charles.calendrierfoyer.dto.EventOccurrenceDto;
import com.charles.calendrierfoyer.repository.CategoryRepository;
import com.charles.calendrierfoyer.repository.EventDoneRepository;
import com.charles.calendrierfoyer.repository.EventRepository;
import com.charles.calendrierfoyer.repository.PersonRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    private static final LocalDate MARCH_1 = LocalDate.of(2026, 3, 1);
    private static final LocalDate MARCH_31 = LocalDate.of(2026, 3, 31);

    @Mock
    private EventRepository eventRepository;

    @Mock
    private PersonRepository personRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private EventDoneRepository eventDoneRepository;

    private EventService eventService;

    @BeforeEach
    void setUp() {
        // Vrai RecurrenceService : le calcul des occurrences fait partie de ce qu'on verifie
        eventService = new EventService(
                eventRepository, personRepository, categoryRepository, eventDoneRepository, new RecurrenceService());
    }

    private static EventCreateDto allDayDto(String title, RecurrenceFrequency frequency, Integer interval) {
        return new EventCreateDto(
                title, "desc", "maison", "#4a90d9", true, MARCH_1, null, null, frequency, interval, "MON", MARCH_31,
                null, null, null, null);
    }

    private static EventCreateDto timedDto(LocalTime start, LocalTime end) {
        return new EventCreateDto(
                "Dentiste",
                null,
                null,
                null,
                false,
                MARCH_1,
                start,
                end,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    private static Event event(Long id, String title, LocalDate start, LocalTime startTime, RecurrenceFrequency f) {
        Event e = new Event();
        e.setId(id);
        e.setTitle(title);
        e.setStartDate(start);
        e.setAllDay(startTime == null);
        e.setStartTime(startTime);
        e.setRecurrenceFrequency(f);
        return e;
    }

    @Test
    void lesOccurrencesSontDerouleesEtTrieesParDatePuisHeure() {
        Event ponctuel = event(1L, "Dentiste", LocalDate.of(2026, 3, 2), LocalTime.of(9, 0), RecurrenceFrequency.NONE);
        Event journee = event(2L, "Anniversaire", LocalDate.of(2026, 3, 2), null, RecurrenceFrequency.NONE);
        Event hebdo = event(3L, "Piscine", MARCH_1, LocalTime.of(18, 0), RecurrenceFrequency.WEEKLY);
        when(eventRepository.findRelevantForRange(eq(MARCH_1), eq(LocalDate.of(2026, 3, 8)), any()))
                .thenReturn(List.of(hebdo, ponctuel, journee));

        List<EventOccurrenceDto> occurrences = eventService.occurrencesInRange(MARCH_1, LocalDate.of(2026, 3, 8));

        assertThat(occurrences)
                .extracting(EventOccurrenceDto::title)
                .containsExactly("Piscine", "Anniversaire", "Dentiste", "Piscine");
        assertThat(occurrences).extracting(EventOccurrenceDto::recurring).containsExactly(true, false, false, true);
    }

    @Test
    void unePlageInverseeEstRefusee() {
        assertThatThrownBy(() -> eventService.occurrencesInRange(MARCH_31, MARCH_1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void trouverUnEvenement() {
        when(eventRepository.findById(1L))
                .thenReturn(Optional.of(event(1L, "Dentiste", MARCH_1, null, RecurrenceFrequency.NONE)));

        assertThat(eventService.findById(1L).title()).isEqualTo("Dentiste");
    }

    @Test
    void trouverUnEvenementInconnuLeveUneErreur() {
        when(eventRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.findById(1L)).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void creerUnEvenementHebdomadaire() {
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        EventDto dto = eventService.create(allDayDto("Piscine", RecurrenceFrequency.WEEKLY, 2));

        assertThat(dto.title()).isEqualTo("Piscine");
        assertThat(dto.recurrenceFrequency()).isEqualTo(RecurrenceFrequency.WEEKLY);
        assertThat(dto.recurrenceInterval()).isEqualTo(2);
        assertThat(dto.recurrenceDaysOfWeek()).isEqualTo("MON");
        assertThat(dto.recurrenceEndDate()).isEqualTo(MARCH_31);
        assertThat(dto.startTime()).isNull();
    }

    @Test
    void sansRecurrenceLesChampsDeRecurrenceSontRemisAZero() {
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        EventDto dto = eventService.create(allDayDto("Fete", null, 5));

        assertThat(dto.recurrenceFrequency()).isEqualTo(RecurrenceFrequency.NONE);
        assertThat(dto.recurrenceInterval()).isEqualTo(1);
        assertThat(dto.recurrenceDaysOfWeek()).isNull();
        assertThat(dto.recurrenceEndDate()).isNull();
    }

    @Test
    void unIntervalleInvalideEstRameneA1() {
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        EventDto dto = eventService.create(allDayDto("Courses", RecurrenceFrequency.MONTHLY, 0));

        assertThat(dto.recurrenceInterval()).isEqualTo(1);
        assertThat(dto.recurrenceDaysOfWeek()).isNull();
    }

    @Test
    void creerUnEvenementAvecHoraires() {
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        EventDto dto = eventService.create(timedDto(LocalTime.of(9, 0), LocalTime.of(10, 0)));

        assertThat(dto.startTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(dto.endTime()).isEqualTo(LocalTime.of(10, 0));
    }

    @Test
    void uneHeureDeDebutEstRequiseHorsJourneeEntiere() {
        EventCreateDto dto = timedDto(null, null);

        assertThatThrownBy(() -> eventService.create(dto)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void lHeureDeFinDoitEtreApresLHeureDeDebut() {
        EventCreateDto dto = timedDto(LocalTime.of(10, 0), LocalTime.of(9, 0));

        assertThatThrownBy(() -> eventService.create(dto)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void modifierUnEvenement() {
        Event existing = event(1L, "Ancien", MARCH_1, null, RecurrenceFrequency.NONE);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(eventRepository.save(existing)).thenReturn(existing);

        EventDto dto = eventService.update(1L, allDayDto("Nouveau", RecurrenceFrequency.YEARLY, 1));

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.title()).isEqualTo("Nouveau");
        assertThat(dto.recurrenceFrequency()).isEqualTo(RecurrenceFrequency.YEARLY);
    }

    @Test
    void supprimerUnEvenement() {
        eventService.delete(1L);

        verify(eventRepository).deleteById(1L);
    }

    private static Person caitlyn() {
        Person p = new Person();
        p.setId(7L);
        p.setFirstName("Caitlyn");
        p.setLastName("Graire");
        p.setBirthDate(LocalDate.of(2018, 3, 15));
        p.setColor("#e91e63");
        return p;
    }

    @Test
    void unEvenementDUnePersonnePrendSaCouleur() {
        Event e = event(1L, "Danse", LocalDate.of(2026, 3, 4), null, RecurrenceFrequency.NONE);
        e.setColor("#000000");
        e.setCategory(sport());
        e.setPerson(caitlyn());
        when(eventRepository.findRelevantForRange(eq(MARCH_1), eq(MARCH_31), any()))
                .thenReturn(List.of(e));

        EventOccurrenceDto occ =
                eventService.occurrencesInRange(MARCH_1, MARCH_31).get(0);

        assertThat(occ.color()).isEqualTo("#e91e63");
        assertThat(occ.personId()).isEqualTo(7L);
        assertThat(occ.categoryId()).isEqualTo(3L);
        assertThat(occ.icon()).isEqualTo("⚽");
    }

    @Test
    void lAnniversaireDUnePersonneEstAjouteAutomatiquement() {
        when(personRepository.findAll()).thenReturn(List.of(caitlyn()));

        List<EventOccurrenceDto> occurrences = eventService.occurrencesInRange(MARCH_1, MARCH_31);

        assertThat(occurrences).hasSize(1);
        EventOccurrenceDto birthday = occurrences.get(0);
        assertThat(birthday.date()).isEqualTo(LocalDate.of(2026, 3, 15));
        assertThat(birthday.title()).isEqualTo("Anniversaire de Caitlyn (8 ans)");
        assertThat(birthday.eventId()).isNull();
        assertThat(birthday.categoryId()).isNull();
        assertThat(birthday.icon()).isEqualTo(EventService.BIRTHDAY_ICON);
        assertThat(birthday.color()).isEqualTo("#e91e63");
    }

    @Test
    void pasDAnniversaireHorsDeLaPlageNiAvantLaNaissance() {
        when(personRepository.findAll()).thenReturn(List.of(caitlyn()));

        assertThat(eventService.occurrencesInRange(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30)))
                .isEmpty();
        assertThat(eventService.occurrencesInRange(LocalDate.of(2018, 1, 1), LocalDate.of(2018, 12, 31)))
                .isEmpty();
    }

    @Test
    void unAnniversaireDu29FevrierTombeLe28LesAnneesNonBissextiles() {
        Person p = caitlyn();
        p.setBirthDate(LocalDate.of(2016, 2, 29));
        when(personRepository.findAll()).thenReturn(List.of(p));

        assertThat(eventService.occurrencesInRange(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28)))
                .extracting(EventOccurrenceDto::date)
                .containsExactly(LocalDate.of(2026, 2, 28));
    }

    private static Category sport() {
        Category c = new Category();
        c.setId(3L);
        c.setName("Sport");
        c.setIcon("⚽");
        return c;
    }

    @Test
    void creerUnEvenementPourUnePersonne() {
        when(personRepository.findById(7L)).thenReturn(Optional.of(caitlyn()));
        when(categoryRepository.findById(3L)).thenReturn(Optional.of(sport()));
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));
        EventCreateDto dto = new EventCreateDto(
                "Dentiste", null, null, null, true, MARCH_1, null, null, null, null, null, null, 3L, 7L, null, null);

        EventDto created = eventService.create(dto);

        assertThat(created.personId()).isEqualTo(7L);
        assertThat(created.categoryId()).isEqualTo(3L);
    }

    @Test
    void unEvenementPeutNAvoirAucuneCategorie() {
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(eventService.create(timedDto(LocalTime.of(9, 0), null)).categoryId())
                .isNull();
    }

    @Test
    void uneCategorieInconnueEstRefusee() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());
        EventCreateDto dto = new EventCreateDto(
                "X", null, null, null, true, MARCH_1, null, null, null, null, null, null, 99L, null, null, null);

        assertThatThrownBy(() -> eventService.create(dto)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void unePersonneInconnueEstRefusee() {
        when(personRepository.findById(99L)).thenReturn(Optional.empty());
        EventCreateDto dto = new EventCreateDto(
                "X", null, null, null, true, MARCH_1, null, null, null, null, null, null, null, 99L, null, null);

        assertThatThrownBy(() -> eventService.create(dto)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void lesOccurrencesCocheesSontMarqueesFaites() {
        Event e = event(1L, "Faire le lit", MARCH_1, null, RecurrenceFrequency.DAILY);
        e.setReminderMinutes(15);
        when(eventRepository.findRelevantForRange(eq(MARCH_1), eq(LocalDate.of(2026, 3, 2)), any()))
                .thenReturn(List.of(e));
        when(eventDoneRepository.findByOccurrenceDateBetween(MARCH_1, LocalDate.of(2026, 3, 2)))
                .thenReturn(List.of(new EventDone(1L, MARCH_1)));

        List<EventOccurrenceDto> occurrences = eventService.occurrencesInRange(MARCH_1, LocalDate.of(2026, 3, 2));

        assertThat(occurrences).extracting(EventOccurrenceDto::done).containsExactly(true, false);
        assertThat(occurrences.get(0).reminderMinutes()).isEqualTo(15);
    }

    @Test
    void cocherEtDecocherUneOccurrence() {
        Event e = event(1L, "Faire le lit", MARCH_1, null, RecurrenceFrequency.DAILY);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(e));
        EventDone.Key key = new EventDone.Key(1L, MARCH_31);
        when(eventDoneRepository.existsById(key)).thenReturn(true);

        eventService.setDone(1L, MARCH_31, true);
        eventService.setDone(1L, MARCH_31, false);

        verify(eventDoneRepository).save(any(EventDone.class));
        verify(eventDoneRepository).deleteById(key);
    }

    @Test
    void decocherUneOccurrenceNonCocheeNeFaitRien() {
        Event e = event(1L, "Faire le lit", MARCH_1, null, RecurrenceFrequency.DAILY);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(e));
        when(eventDoneRepository.existsById(any())).thenReturn(false);

        eventService.setDone(1L, MARCH_31, false);

        verify(eventDoneRepository, never()).deleteById(any());
    }

    @Test
    void onNePeutPasCocherUnJourOuLEvenementNALieuPas() {
        Event e = event(1L, "Dentiste", MARCH_1, null, RecurrenceFrequency.NONE);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(e));

        assertThatThrownBy(() -> eventService.setDone(1L, MARCH_31, true)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void unRappelNegatifEstRefuse() {
        EventCreateDto dto = new EventCreateDto(
                "X", null, null, null, true, MARCH_1, null, null, null, null, null, null, null, null, -5, null);

        assertThatThrownBy(() -> eventService.create(dto)).isInstanceOf(IllegalArgumentException.class);
    }

    private static EventCreateDto periodDto(LocalDate start, LocalDate end, boolean allDay) {
        return new EventCreateDto(
                "Vacances",
                null,
                null,
                null,
                allDay,
                start,
                allDay ? null : LocalTime.of(9, 0),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                0,
                end);
    }

    @Test
    void unePeriodeApparaitChaqueJourAvecSesBornesEtUnSeulRappel() {
        Event e = event(1L, "Vacances", LocalDate.of(2026, 2, 27), null, RecurrenceFrequency.NONE);
        e.setEndDate(LocalDate.of(2026, 3, 3));
        e.setReminderMinutes(0);
        when(eventRepository.findRelevantForRange(eq(MARCH_1), eq(MARCH_31), any()))
                .thenReturn(List.of(e));

        List<EventOccurrenceDto> occurrences = eventService.occurrencesInRange(MARCH_1, MARCH_31);

        // Commencee en fevrier : seuls les jours de mars sont renvoyes
        assertThat(occurrences)
                .extracting(EventOccurrenceDto::date)
                .containsExactly(MARCH_1, LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 3));
        assertThat(occurrences)
                .allMatch(o -> o.spanStart().equals(LocalDate.of(2026, 2, 27))
                        && o.spanEnd().equals(LocalDate.of(2026, 3, 3)));
        // Le rappel n'est que sur le premier jour (27 fevrier, hors plage ici)
        assertThat(occurrences).allMatch(o -> o.reminderMinutes() == null);
    }

    @Test
    void lePremierJourDUnePeriodePorteLeRappel() {
        Event e = event(1L, "Vacances", MARCH_1, null, RecurrenceFrequency.NONE);
        e.setEndDate(LocalDate.of(2026, 3, 2));
        e.setReminderMinutes(1440);
        when(eventRepository.findRelevantForRange(eq(MARCH_1), eq(MARCH_31), any()))
                .thenReturn(List.of(e));

        assertThat(eventService.occurrencesInRange(MARCH_1, MARCH_31))
                .extracting(EventOccurrenceDto::reminderMinutes)
                .containsExactly(1440, null);
    }

    @Test
    void creerUnePeriode() {
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        EventDto dto = eventService.create(periodDto(MARCH_1, MARCH_31, true));

        assertThat(dto.endDate()).isEqualTo(MARCH_31);
    }

    @Test
    void uneFinIdentiqueAuDebutDonneUnSeulJour() {
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(eventService.create(periodDto(MARCH_1, MARCH_1, true)).endDate())
                .isNull();
    }

    @Test
    void lesPeriodesInvalidesSontRefusees() {
        EventCreateDto finAvantDebut = periodDto(MARCH_31, MARCH_1, true);
        EventCreateDto avecHoraire = periodDto(MARCH_1, MARCH_31, false);
        EventCreateDto tropLongue = periodDto(MARCH_1, MARCH_1.plusYears(2), true);

        assertThatThrownBy(() -> eventService.create(finAvantDebut)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> eventService.create(avecHoraire)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> eventService.create(tropLongue)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void onPeutCocherUnJourAuMilieuDUnePeriode() {
        Event e = event(1L, "Vacances", MARCH_1, null, RecurrenceFrequency.NONE);
        e.setEndDate(MARCH_31);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(e));

        eventService.setDone(1L, LocalDate.of(2026, 3, 15), true);

        verify(eventDoneRepository).save(any(EventDone.class));
    }
}
