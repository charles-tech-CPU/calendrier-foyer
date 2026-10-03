package com.charles.calendrierfoyer.dto;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Une occurrence concrete affichee sur le calendrier (une date precise).
 * eventId est nul pour un anniversaire genere automatiquement a partir de la
 * date de naissance d'une personne (il n'y a alors aucun evenement a modifier).
 * color est la couleur effective : celle de la personne si l'evenement en a une.
 * icon est l'icone a afficher (celle de la categorie, nulle si aucune).
 * done indique que cette occurrence precise a ete cochee "c'est fait".
 * spanStart / spanEnd : premier et dernier jour de la periode dont fait partie
 * ce jour (nuls pour un evenement sur un seul jour).
 */
public record EventOccurrenceDto(
        Long eventId,
        String title,
        String description,
        String location,
        String color,
        boolean allDay,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        boolean recurring,
        Long categoryId,
        String icon,
        Long personId,
        Integer reminderMinutes,
        boolean done,
        LocalDate spanStart,
        LocalDate spanEnd) {}
