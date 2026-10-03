package com.charles.calendrierfoyer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.charles.calendrierfoyer.domain.Event;
import com.charles.calendrierfoyer.domain.RecurrenceFrequency;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class RecurrenceServiceTest {

    private final RecurrenceService service = new RecurrenceService();

    private static Event event(LocalDate start, RecurrenceFrequency frequency, int interval) {
        Event e = new Event();
        e.setTitle("Test");
        e.setAllDay(true);
        e.setStartDate(start);
        e.setRecurrenceFrequency(frequency);
        e.setRecurrenceInterval(interval);
        return e;
    }

    @Test
    void evenementPonctuelVisibleUniquementDansSaPlage() {
        Event e = event(LocalDate.of(2026, 3, 10), RecurrenceFrequency.NONE, 1);

        assertThat(service.occurrenceDates(e, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31)))
                .containsExactly(LocalDate.of(2026, 3, 10));
        assertThat(service.occurrenceDates(e, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30)))
                .isEmpty();
    }

    @Test
    void quotidienAvecIntervalleEtDateDeFin() {
        Event e = event(LocalDate.of(2026, 3, 1), RecurrenceFrequency.DAILY, 2);
        e.setRecurrenceEndDate(LocalDate.of(2026, 3, 7));

        assertThat(service.occurrenceDates(e, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31)))
                .containsExactly(
                        LocalDate.of(2026, 3, 1),
                        LocalDate.of(2026, 3, 3),
                        LocalDate.of(2026, 3, 5),
                        LocalDate.of(2026, 3, 7));
    }

    @Test
    void hebdomadaireSurPlusieursJours() {
        // 2 mars 2026 = lundi
        Event e = event(LocalDate.of(2026, 3, 2), RecurrenceFrequency.WEEKLY, 1);
        e.setRecurrenceDaysOfWeek("MON,WED");

        assertThat(service.occurrenceDates(e, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 11)))
                .containsExactly(
                        LocalDate.of(2026, 3, 2),
                        LocalDate.of(2026, 3, 4),
                        LocalDate.of(2026, 3, 9),
                        LocalDate.of(2026, 3, 11));
    }

    @Test
    void hebdomadaireSansJoursUtiliseLeJourDeDepart() {
        Event e = event(LocalDate.of(2026, 3, 4), RecurrenceFrequency.WEEKLY, 2);

        assertThat(service.occurrenceDates(e, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31)))
                .containsExactly(LocalDate.of(2026, 3, 4), LocalDate.of(2026, 3, 18));
    }

    @Test
    void hebdomadaireRefuseUnJourInvalide() {
        Event e = event(LocalDate.of(2026, 3, 2), RecurrenceFrequency.WEEKLY, 1);
        e.setRecurrenceDaysOfWeek("XYZ");

        assertThatThrownBy(() -> service.occurrenceDates(e, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void mensuelSeCaleSurLeDernierJourDuMois() {
        Event e = event(LocalDate.of(2026, 1, 31), RecurrenceFrequency.MONTHLY, 1);

        assertThat(service.occurrenceDates(e, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 4, 30)))
                .containsExactly(LocalDate.of(2026, 2, 28), LocalDate.of(2026, 3, 31), LocalDate.of(2026, 4, 30));
    }

    @Test
    void annuel() {
        Event e = event(LocalDate.of(2020, 6, 15), RecurrenceFrequency.YEARLY, 1);

        assertThat(service.occurrenceDates(e, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)))
                .containsExactly(LocalDate.of(2026, 6, 15));
    }

    @Test
    void serieTermineeAvantLaPlageNeDonneRien() {
        Event e = event(LocalDate.of(2026, 1, 1), RecurrenceFrequency.DAILY, 1);
        e.setRecurrenceEndDate(LocalDate.of(2026, 1, 10));

        assertThat(service.occurrenceDates(e, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28)))
                .isEmpty();
    }

    @Test
    void uneSeriePeriodiqueDonneDesPeriodesQuiChevauchentLaPlage() {
        // Vacances du 30 mars au 2 avril, chaque annee
        Event e = event(LocalDate.of(2025, 3, 30), RecurrenceFrequency.YEARLY, 1);
        e.setEndDate(LocalDate.of(2025, 4, 2));

        assertThat(service.occurrenceSpans(e, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30)))
                .containsExactly(new RecurrenceService.Span(LocalDate.of(2026, 3, 30), LocalDate.of(2026, 4, 2)));
        assertThat(RecurrenceService.extraDays(e)).isEqualTo(3);
    }

    @Test
    void unEvenementSurUnJourDonneUnePeriodeDUnJour() {
        Event e = event(LocalDate.of(2026, 3, 10), RecurrenceFrequency.NONE, 1);

        assertThat(service.occurrenceSpans(e, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31)))
                .containsExactly(new RecurrenceService.Span(LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 10)));
    }
}
