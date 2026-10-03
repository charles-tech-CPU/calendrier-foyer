package com.charles.calendrierfoyer.web;

import static org.mockito.Mockito.verify;

import com.charles.calendrierfoyer.dto.EventCreateDto;
import com.charles.calendrierfoyer.service.EventService;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventControllerTest {

    @Mock
    private EventService eventService;

    @InjectMocks
    private EventController controller;

    @Test
    void chaqueRouteDelegueAuService() {
        LocalDate from = LocalDate.of(2026, 3, 1);
        LocalDate to = LocalDate.of(2026, 3, 31);
        EventCreateDto dto =
                new EventCreateDto("Garderie", null, null, null, true, from, null, null, null, null, null, null);

        controller.occurrences(from, to);
        controller.findById(1L);
        controller.create(dto);
        controller.update(1L, dto);
        controller.delete(1L);

        verify(eventService).occurrencesInRange(from, to);
        verify(eventService).findById(1L);
        verify(eventService).create(dto);
        verify(eventService).update(1L, dto);
        verify(eventService).delete(1L);
    }
}
