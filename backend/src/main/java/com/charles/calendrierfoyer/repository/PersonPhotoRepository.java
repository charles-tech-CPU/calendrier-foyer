package com.charles.calendrierfoyer.repository;

import com.charles.calendrierfoyer.domain.PersonPhoto;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PersonPhotoRepository extends JpaRepository<PersonPhoto, Long> {

    /** Identifiants des personnes qui ont une photo, sans charger les images elles-memes. */
    @Query("SELECT p.personId FROM PersonPhoto p")
    Set<Long> findAllPersonIds();
}
