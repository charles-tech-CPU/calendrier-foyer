package com.charles.calendrierfoyer.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Photo (optionnelle) d'une personne, stockee a part pour ne la charger qu'a la demande. */
@Entity
@Table(name = "person_photo")
@Getter
@Setter
@NoArgsConstructor
public class PersonPhoto {

    @Id
    @Column(name = "person_id")
    private Long personId;

    @Column(name = "content_type", nullable = false, length = 50)
    private String contentType;

    @Column(nullable = false)
    private byte[] data;
}
