package com.charles.calendrierfoyer.service;

import com.charles.calendrierfoyer.domain.Event;
import com.charles.calendrierfoyer.domain.RecurrenceFrequency;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.stereotype.Service;

/**
 * Calcule les dates d'occurrence d'un evenement sur une plage [from, to],
 * sans jamais rien stocker : les occurrences futures d'un evenement recurrent
 * ne sont que le resultat d'un calcul, refait a chaque demande (meme principe
 * que RankingService dans tennis-results ou SummaryService dans budget-foyer).
 * Un garde-fou (safetyCap) empeche toute boucle infinie meme en cas de
 * parametre invalide (ex: intervalle a 0 corrige a 1 en amont par EventService).
 */
@Service
public class RecurrenceService {

    private static final int SAFETY_CAP = 3660; // large marge (~10 ans de pas quotidiens)

    /** Une periode [start, end] (start == end pour un evenement sur un seul jour). */
    public record Span(LocalDate start, LocalDate end) {}

    /** Duree d'une periode en jours supplementaires (0 = un seul jour). */
    public static long extraDays(Event e) {
        return e.getEndDate() == null ? 0 : ChronoUnit.DAYS.between(e.getStartDate(), e.getEndDate());
    }

    /**
     * Toutes les periodes de l'evenement qui chevauchent [from, to] : chaque
     * occurrence commence a une date calculee par occurrenceDates et dure
     * autant de jours que la periode d'origine (startDate..endDate).
     */
    public List<Span> occurrenceSpans(Event e, LocalDate from, LocalDate to) {
        long extra = extraDays(e);
        return occurrenceDates(e, from.minusDays(extra), to).stream()
                .map(start -> new Span(start, start.plusDays(extra)))
                .toList();
    }

    public List<LocalDate> occurrenceDates(Event e, LocalDate from, LocalDate to) {
        if (e.getRecurrenceFrequency() == RecurrenceFrequency.NONE) {
            LocalDate d = e.getStartDate();
            return (!d.isBefore(from) && !d.isAfter(to)) ? List.of(d) : List.of();
        }

        LocalDate seriesEnd = e.getRecurrenceEndDate();
        LocalDate rangeEnd = (seriesEnd != null && seriesEnd.isBefore(to)) ? seriesEnd : to;
        if (rangeEnd.isBefore(e.getStartDate())) {
            return List.of();
        }

        int interval = Math.max(1, e.getRecurrenceInterval());
        List<LocalDate> dates = new ArrayList<>();

        switch (e.getRecurrenceFrequency()) {
            case DAILY -> {
                LocalDate cursor = e.getStartDate();
                int i = 0;
                while (!cursor.isAfter(rangeEnd) && i < SAFETY_CAP) {
                    if (!cursor.isBefore(from)) dates.add(cursor);
                    cursor = cursor.plusDays(interval);
                    i++;
                }
            }
            case WEEKLY -> {
                Set<DayOfWeek> days = parseDaysOfWeek(
                        e.getRecurrenceDaysOfWeek(), e.getStartDate().getDayOfWeek());
                LocalDate weekStart = e.getStartDate().with(DayOfWeek.MONDAY);
                int i = 0;
                while (!weekStart.isAfter(rangeEnd) && i < SAFETY_CAP) {
                    for (DayOfWeek d : days) {
                        LocalDate occ = weekStart.with(d);
                        if (!occ.isBefore(e.getStartDate()) && !occ.isBefore(from) && !occ.isAfter(rangeEnd)) {
                            dates.add(occ);
                        }
                    }
                    weekStart = weekStart.plusWeeks(interval);
                    i++;
                }
            }
            case MONTHLY -> {
                int dayOfMonth = e.getStartDate().getDayOfMonth();
                int i = 0;
                LocalDate cursor;
                do {
                    cursor = addMonthsClamped(e.getStartDate(), i * interval, dayOfMonth);
                    if (!cursor.isAfter(rangeEnd) && !cursor.isBefore(from)) {
                        dates.add(cursor);
                    }
                    i++;
                } while (!cursor.isAfter(rangeEnd) && i < SAFETY_CAP);
            }
            case YEARLY -> {
                LocalDate cursor = e.getStartDate();
                int i = 0;
                while (!cursor.isAfter(rangeEnd) && i < SAFETY_CAP) {
                    if (!cursor.isBefore(from)) dates.add(cursor);
                    cursor = cursor.plusYears(interval);
                    i++;
                }
            }
            default -> {}
        }

        Collections.sort(dates);
        return dates;
    }

    private LocalDate addMonthsClamped(LocalDate original, int monthsToAdd, int desiredDay) {
        YearMonth ym = YearMonth.from(original).plusMonths(monthsToAdd);
        int day = Math.min(desiredDay, ym.lengthOfMonth());
        return ym.atDay(day);
    }

    private Set<DayOfWeek> parseDaysOfWeek(String csv, DayOfWeek fallback) {
        if (csv == null || csv.isBlank()) {
            return EnumSet.of(fallback);
        }
        Set<DayOfWeek> result = EnumSet.noneOf(DayOfWeek.class);
        for (String token : csv.split(",")) {
            result.add(mapDay(token.trim().toUpperCase(Locale.ROOT)));
        }
        return result.isEmpty() ? EnumSet.of(fallback) : result;
    }

    private DayOfWeek mapDay(String code) {
        return switch (code) {
            case "MON" -> DayOfWeek.MONDAY;
            case "TUE" -> DayOfWeek.TUESDAY;
            case "WED" -> DayOfWeek.WEDNESDAY;
            case "THU" -> DayOfWeek.THURSDAY;
            case "FRI" -> DayOfWeek.FRIDAY;
            case "SAT" -> DayOfWeek.SATURDAY;
            case "SUN" -> DayOfWeek.SUNDAY;
            default -> throw new IllegalArgumentException("Jour de semaine invalide: " + code);
        };
    }
}
