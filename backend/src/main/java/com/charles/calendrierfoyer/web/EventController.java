package com.charles.calendrierfoyer.web;

import com.charles.calendrierfoyer.dto.EventCreateDto;
import com.charles.calendrierfoyer.dto.EventDto;
import com.charles.calendrierfoyer.dto.EventOccurrenceDto;
import com.charles.calendrierfoyer.service.EventService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    public List<EventOccurrenceDto> occurrences(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return eventService.occurrencesInRange(from, to);
    }

    @GetMapping("/{id}")
    public EventDto findById(@PathVariable Long id) {
        return eventService.findById(id);
    }

    @PostMapping
    public EventDto create(@Valid @RequestBody EventCreateDto dto) {
        return eventService.create(dto);
    }

    @PutMapping("/{id}")
    public EventDto update(@PathVariable Long id, @Valid @RequestBody EventCreateDto dto) {
        return eventService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        eventService.delete(id);
    }

    /** Coche "c'est fait" l'occurrence de cet evenement a cette date. */
    @PutMapping("/{id}/done/{date}")
    public void markDone(
            @PathVariable Long id, @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        eventService.setDone(id, date, true);
    }

    @DeleteMapping("/{id}/done/{date}")
    public void unmarkDone(
            @PathVariable Long id, @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        eventService.setDone(id, date, false);
    }
}
