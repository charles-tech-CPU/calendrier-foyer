package com.charles.calendrierfoyer.repository;

import com.charles.calendrierfoyer.domain.Person;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonRepository extends JpaRepository<Person, Long> {

    boolean existsByColor(String color);

    boolean existsByColorAndIdNot(String color, Long id);
}
