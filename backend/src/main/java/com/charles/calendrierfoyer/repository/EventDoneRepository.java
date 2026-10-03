package com.charles.calendrierfoyer.repository;

import com.charles.calendrierfoyer.domain.EventDone;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventDoneRepository extends JpaRepository<EventDone, EventDone.Key> {

    List<EventDone> findByOccurrenceDateBetween(LocalDate from, LocalDate to);
}
