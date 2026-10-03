package com.charles.calendrierfoyer.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Categorie d'evenement creee par le foyer : un nom et une icone (un emoji),
 * affichee en grand dans le calendrier pour etre comprise sans savoir lire.
 */
@Entity
@Table(name = "category")
@Getter
@Setter
@NoArgsConstructor
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Unique, sans tenir compte des majuscules. */
    @Column(nullable = false, length = 60)
    private String name;

    /** Emoji (eventuellement compose de plusieurs caracteres, ex: drapeau ou famille). */
    @Column(nullable = false, length = 64)
    private String icon;

    /** Ordre d'affichage : les nouvelles categories s'ajoutent a la fin. */
    @Column(nullable = false)
    private int position;
}
