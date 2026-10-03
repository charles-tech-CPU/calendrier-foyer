package com.charles.calendrierfoyer.service;

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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventService {

    /** Icone des anniversaires generes automatiquement a partir des dates de naissance. */
    static final String BIRTHDAY_ICON = "🎂";

    /** Duree maximale d'une periode (ex: vacances), en jours. */
    static final int MAX_PERIOD_DAYS = 366;

    private final EventRepository eventRepository;
    private final PersonRepository personRepository;
    private final CategoryRepository categoryRepository;
    private final EventDoneRepository eventDoneRepository;
    private final RecurrenceService recurrenceService;

    public EventService(
            EventRepository eventRepository,
            PersonRepository personRepository,
            CategoryRepository categoryRepository,
            EventDoneRepository eventDoneRepository,
            RecurrenceService recurrenceService) {
        this.eventRepository = eventRepository;
        this.personRepository = personRepository;
        this.categoryRepository = categoryRepository;
        this.eventDoneRepository = eventDoneRepository;
        this.recurrenceService = recurrenceService;
    }

    /**
     * Toutes les occurrences visibles sur [from, to] : evenements ponctuels,
     * recurrents deroules, et anniversaires des personnes du foyer (deduits de
     * leur date de naissance, jamais stockes).
     */
    @Transactional(readOnly = true)
    public List<EventOccurrenceDto> occurrencesInRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("La date de debut doit preceder la date de fin.");
        }
        Set<EventDone.Key> done = eventDoneRepository.findByOccurrenceDateBetween(from, to).stream()
                .map(d -> new EventDone.Key(d.getEventId(), d.getOccurrenceDate()))
                .collect(Collectors.toSet());
        Stream<EventOccurrenceDto> events =
                eventRepository.findRelevantForRange(from, to, from.minusDays(MAX_PERIOD_DAYS)).stream()
                        .flatMap(e -> recurrenceService.occurrenceSpans(e, from, to).stream()
                                .flatMap(span -> daysOf(span, from, to)
                                        .map(date -> toOccurrence(
                                                e, date, span, done.contains(new EventDone.Key(e.getId(), date))))));
        Stream<EventOccurrenceDto> birthdays =
                personRepository.findAll().stream().flatMap(p -> birthdays(p, from, to).stream());
        // Dans une journee : les periodes d'abord (la plus ancienne en premier, pour
        // qu'elles restent alignees d'un jour a l'autre), puis par heure
        return Stream.concat(birthdays, events)
                .sorted(Comparator.comparing(EventOccurrenceDto::date)
                        .thenComparing(o -> o.spanStart() == null ? LocalDate.MAX : o.spanStart())
                        .thenComparing(o -> o.startTime() == null ? LocalTime.MIN : o.startTime())
                        .thenComparing(o -> o.eventId() == null ? 0L : o.eventId()))
                .toList();
    }

    /** Les jours d'une periode compris dans [from, to]. */
    private static Stream<LocalDate> daysOf(RecurrenceService.Span span, LocalDate from, LocalDate to) {
        LocalDate first = span.start().isBefore(from) ? from : span.start();
        LocalDate last = span.end().isAfter(to) ? to : span.end();
        return first.datesUntil(last.plusDays(1));
    }

    @Transactional(readOnly = true)
    public EventDto findById(Long id) {
        return toDto(getOrThrow(id));
    }

    @Transactional
    public EventDto create(EventCreateDto dto) {
        Event e = new Event();
        apply(e, dto);
        return toDto(eventRepository.save(e));
    }

    @Transactional
    public EventDto update(Long id, EventCreateDto dto) {
        Event e = getOrThrow(id);
        apply(e, dto);
        return toDto(eventRepository.save(e));
    }

    public void delete(Long id) {
        eventRepository.deleteById(id);
    }

    /** Coche (done = true) ou decoche une occurrence precise, qui doit exister a cette date. */
    @Transactional
    public void setDone(Long id, LocalDate date, boolean done) {
        Event e = getOrThrow(id);
        if (recurrenceService.occurrenceSpans(e, date, date).isEmpty()) {
            throw new IllegalArgumentException("Cet evenement n'a pas lieu ce jour-la.");
        }
        EventDone.Key key = new EventDone.Key(id, date);
        if (done) {
            eventDoneRepository.save(new EventDone(id, date));
        } else if (eventDoneRepository.existsById(key)) {
            eventDoneRepository.deleteById(key);
        }
    }

    private Event getOrThrow(Long id) {
        return eventRepository
                .findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Evenement introuvable: " + id));
    }

    /** Un anniversaire par annee couverte par la plage (29 fevrier -> 28 fevrier les annees non bissextiles). */
    private static List<EventOccurrenceDto> birthdays(Person p, LocalDate from, LocalDate to) {
        List<EventOccurrenceDto> result = new ArrayList<>();
        for (int year = Math.max(from.getYear(), p.getBirthDate().getYear() + 1); year <= to.getYear(); year++) {
            LocalDate date = p.getBirthDate().withYear(year);
            if (!date.isBefore(from) && !date.isAfter(to)) {
                int age = year - p.getBirthDate().getYear();
                result.add(new EventOccurrenceDto(
                        null,
                        "Anniversaire de " + p.getFirstName() + " (" + age + " ans)",
                        null,
                        null,
                        p.getColor(),
                        true,
                        date,
                        null,
                        null,
                        true,
                        null,
                        BIRTHDAY_ICON,
                        p.getId(),
                        null,
                        false,
                        null,
                        null));
            }
        }
        return result;
    }

    private static EventOccurrenceDto toOccurrence(Event e, LocalDate date, RecurrenceService.Span span, boolean done) {
        boolean period = span.end().isAfter(span.start());
        Person person = e.getPerson();
        Category category = e.getCategory();
        return new EventOccurrenceDto(
                e.getId(),
                e.getTitle(),
                e.getDescription(),
                e.getLocation(),
                person != null ? person.getColor() : e.getColor(),
                e.isAllDay(),
                date,
                e.getStartTime(),
                e.getEndTime(),
                e.getRecurrenceFrequency() != RecurrenceFrequency.NONE,
                category != null ? category.getId() : null,
                category != null ? category.getIcon() : null,
                person != null ? person.getId() : null,
                // Pour une periode, le rappel ne concerne que le premier jour
                period && !date.equals(span.start()) ? null : e.getReminderMinutes(),
                done,
                period ? span.start() : null,
                period ? span.end() : null);
    }

    private void apply(Event e, EventCreateDto dto) {
        boolean allDay = Boolean.TRUE.equals(dto.allDay());
        if (!allDay && dto.startTime() == null) {
            throw new IllegalArgumentException(
                    "Une heure de debut est requise pour un evenement qui n'est pas journee entiere.");
        }
        if (dto.startTime() != null && dto.endTime() != null && !dto.endTime().isAfter(dto.startTime())) {
            throw new IllegalArgumentException("L'heure de fin doit etre apres l'heure de debut.");
        }
        LocalDate endDate = dto.endDate() == null || dto.endDate().equals(dto.startDate()) ? null : dto.endDate();
        if (endDate != null) {
            if (endDate.isBefore(dto.startDate())) {
                throw new IllegalArgumentException("La date de fin doit etre apres la date de debut.");
            }
            if (!allDay) {
                throw new IllegalArgumentException("Une periode sur plusieurs jours est forcement en journee entiere.");
            }
            if (ChronoUnit.DAYS.between(dto.startDate(), endDate) > MAX_PERIOD_DAYS) {
                throw new IllegalArgumentException("Une periode ne peut pas depasser un an.");
            }
        }
        if (dto.reminderMinutes() != null && dto.reminderMinutes() < 0) {
            throw new IllegalArgumentException("Le rappel ne peut pas etre negatif.");
        }
        RecurrenceFrequency frequency =
                dto.recurrenceFrequency() == null ? RecurrenceFrequency.NONE : dto.recurrenceFrequency();
        int interval =
                (dto.recurrenceInterval() == null || dto.recurrenceInterval() < 1) ? 1 : dto.recurrenceInterval();
        Person person = dto.personId() == null
                ? null
                : personRepository
                        .findById(dto.personId())
                        .orElseThrow(() -> new IllegalArgumentException("Personne introuvable: " + dto.personId()));
        Category category = dto.categoryId() == null
                ? null
                : categoryRepository
                        .findById(dto.categoryId())
                        .orElseThrow(() -> new IllegalArgumentException("Categorie introuvable: " + dto.categoryId()));

        e.setTitle(dto.title());
        e.setDescription(dto.description());
        e.setLocation(dto.location());
        e.setColor(dto.color());
        e.setAllDay(allDay);
        e.setStartDate(dto.startDate());
        e.setEndDate(endDate);
        e.setStartTime(allDay ? null : dto.startTime());
        e.setEndTime(allDay ? null : dto.endTime());
        e.setRecurrenceFrequency(frequency);
        e.setRecurrenceInterval(frequency == RecurrenceFrequency.NONE ? 1 : interval);
        e.setRecurrenceDaysOfWeek(frequency == RecurrenceFrequency.WEEKLY ? dto.recurrenceDaysOfWeek() : null);
        e.setRecurrenceEndDate(frequency == RecurrenceFrequency.NONE ? null : dto.recurrenceEndDate());
        e.setCategory(category);
        e.setPerson(person);
        e.setReminderMinutes(dto.reminderMinutes());
    }

    private static EventDto toDto(Event e) {
        return new EventDto(
                e.getId(),
                e.getTitle(),
                e.getDescription(),
                e.getLocation(),
                e.getColor(),
                e.isAllDay(),
                e.getStartDate(),
                e.getStartTime(),
                e.getEndTime(),
                e.getRecurrenceFrequency(),
                e.getRecurrenceInterval(),
                e.getRecurrenceDaysOfWeek(),
                e.getRecurrenceEndDate(),
                e.getCategory() != null ? e.getCategory().getId() : null,
                e.getPerson() != null ? e.getPerson().getId() : null,
                e.getReminderMinutes(),
                e.getEndDate());
    }
}
