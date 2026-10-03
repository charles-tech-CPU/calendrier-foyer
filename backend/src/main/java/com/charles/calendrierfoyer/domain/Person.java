package com.charles.calendrierfoyer.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Un membre du foyer. Sa couleur (unique) est reprise par tous les evenements
 * qui le concernent, et son anniversaire est affiche automatiquement chaque
 * annee (calcule a la volee par EventService, jamais stocke).
 */
@Entity
@Table(name = "person")
@Getter
@Setter
@NoArgsConstructor
public class Person {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false, length = 80)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 80)
    private String lastName;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    /** Couleur hexadecimale en minuscules (ex: "#e91e63"), unique dans le foyer. */
    @Column(nullable = false, length = 20, unique = true)
    private String color;
}
