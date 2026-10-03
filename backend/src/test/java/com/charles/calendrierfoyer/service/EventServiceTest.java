package com.charles.calendrierfoyer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.charles.calendrierfoyer.domain.Event;
import com.charles.calendrierfoyer.domain.RecurrenceFrequency;
import com.charles.calendrierfoyer.dto.EventCreateDto;
import com.charles.calendrierfoyer.dto.EventDto;
import com.charles.calendrierfoyer.dto.EventOccurrenceDto;
import com.charles.calendrierfoyer.repository.EventRepository;
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

    private EventService eventService;

    @BeforeEach
    void setUp() {
        // Vrai RecurrenceService : le calcul des occurrences fait partie de ce qu'on verifie
        eventService = new EventService(eventRepository, new RecurrenceService());
    }

    private static EventCreateDto allDayDto(String title, RecurrenceFrequency frequency, Integer interval) {
        return new EventCreateDto(
                title, "desc", "maison", "#4a90d9", true, MARCH_1, null, null, frequency, interval, "MON", MARCH_31);
    }

    private static EventCreateDto timedDto(LocalTime start, LocalTime end) {
        return new EventCreateDto("Dentiste", null, null, null, false, MARCH_1, start, end, null, null, null, null);
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
        when(eventRepository.findRelevantForRange(MARCH_1, LocalDate.of(2026, 3, 8)))
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
}
