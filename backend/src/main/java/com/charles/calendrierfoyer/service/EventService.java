package com.charles.calendrierfoyer.service;

import com.charles.calendrierfoyer.domain.Event;
import com.charles.calendrierfoyer.domain.RecurrenceFrequency;
import com.charles.calendrierfoyer.dto.EventCreateDto;
import com.charles.calendrierfoyer.dto.EventDto;
import com.charles.calendrierfoyer.dto.EventOccurrenceDto;
import com.charles.calendrierfoyer.repository.EventRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final RecurrenceService recurrenceService;

    public EventService(EventRepository eventRepository, RecurrenceService recurrenceService) {
        this.eventRepository = eventRepository;
        this.recurrenceService = recurrenceService;
    }

    /** Toutes les occurrences (ponctuelles + recurrentes deroulees) visibles sur [from, to]. */
    public List<EventOccurrenceDto> occurrencesInRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("La date de debut doit preceder la date de fin.");
        }
        List<Event> candidates = eventRepository.findRelevantForRange(from, to);
        return candidates.stream()
                .flatMap(e -> recurrenceService.occurrenceDates(e, from, to).stream()
                        .map(date -> new EventOccurrenceDto(
                                e.getId(), e.getTitle(), e.getDescription(), e.getLocation(), e.getColor(),
                                e.isAllDay(), date, e.getStartTime(), e.getEndTime(),
                                e.getRecurrenceFrequency() != RecurrenceFrequency.NONE)))
                .sorted(Comparator.comparing(EventOccurrenceDto::date)
                        .thenComparing(o -> o.startTime() == null ? java.time.LocalTime.MIN : o.startTime()))
                .toList();
    }

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

    private Event getOrThrow(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Evenement introuvable: " + id));
    }

    private void apply(Event e, EventCreateDto dto) {
        boolean allDay = Boolean.TRUE.equals(dto.allDay());
        if (!allDay && dto.startTime() == null) {
            throw new IllegalArgumentException("Une heure de debut est requise pour un evenement qui n'est pas journee entiere.");
        }
        if (dto.startTime() != null && dto.endTime() != null && !dto.endTime().isAfter(dto.startTime())) {
            throw new IllegalArgumentException("L'heure de fin doit etre apres l'heure de debut.");
        }
        RecurrenceFrequency frequency = dto.recurrenceFrequency() == null ? RecurrenceFrequency.NONE : dto.recurrenceFrequency();
        int interval = (dto.recurrenceInterval() == null || dto.recurrenceInterval() < 1) ? 1 : dto.recurrenceInterval();

        e.setTitle(dto.title());
        e.setDescription(dto.description());
        e.setLocation(dto.location());
        e.setColor(dto.color());
        e.setAllDay(allDay);
        e.setStartDate(dto.startDate());
        e.setStartTime(allDay ? null : dto.startTime());
        e.setEndTime(allDay ? null : dto.endTime());
        e.setRecurrenceFrequency(frequency);
        e.setRecurrenceInterval(frequency == RecurrenceFrequency.NONE ? 1 : interval);
        e.setRecurrenceDaysOfWeek(frequency == RecurrenceFrequency.WEEKLY ? dto.recurrenceDaysOfWeek() : null);
        e.setRecurrenceEndDate(frequency == RecurrenceFrequency.NONE ? null : dto.recurrenceEndDate());
    }

    private EventDto toDto(Event e) {
        return new EventDto(e.getId(), e.getTitle(), e.getDescription(), e.getLocation(), e.getColor(),
                e.isAllDay(), e.getStartDate(), e.getStartTime(), e.getEndTime(),
                e.getRecurrenceFrequency(), e.getRecurrenceInterval(), e.getRecurrenceDaysOfWeek(), e.getRecurrenceEndDate());
    }
}
