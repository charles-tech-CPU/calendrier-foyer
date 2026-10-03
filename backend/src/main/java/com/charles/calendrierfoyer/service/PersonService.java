package com.charles.calendrierfoyer.service;

import com.charles.calendrierfoyer.domain.Person;
import com.charles.calendrierfoyer.domain.PersonPhoto;
import com.charles.calendrierfoyer.dto.PersonCreateDto;
import com.charles.calendrierfoyer.dto.PersonDto;
import com.charles.calendrierfoyer.repository.EventRepository;
import com.charles.calendrierfoyer.repository.PersonPhotoRepository;
import com.charles.calendrierfoyer.repository.PersonRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PersonService {

    /** Les photos sont redimensionnees par le frontend avant envoi : 2 Mo est une large marge. */
    static final int MAX_PHOTO_BYTES = 2 * 1024 * 1024;

    private static final Set<String> ALLOWED_PHOTO_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final String COLOR_TAKEN = "Cette couleur est deja utilisee par une autre personne.";

    private final PersonRepository personRepository;
    private final PersonPhotoRepository photoRepository;
    private final EventRepository eventRepository;

    public PersonService(
            PersonRepository personRepository, PersonPhotoRepository photoRepository, EventRepository eventRepository) {
        this.personRepository = personRepository;
        this.photoRepository = photoRepository;
        this.eventRepository = eventRepository;
    }

    public List<PersonDto> findAll() {
        Set<Long> withPhoto = photoRepository.findAllPersonIds();
        return personRepository.findAll().stream()
                .sorted(Comparator.comparing(Person::getFirstName, String.CASE_INSENSITIVE_ORDER))
                .map(p -> toDto(p, withPhoto.contains(p.getId())))
                .toList();
    }

    public PersonDto findById(Long id) {
        return toDto(getOrThrow(id), photoRepository.existsById(id));
    }

    @Transactional
    public PersonDto create(PersonCreateDto dto) {
        String color = normalizeColor(dto.color());
        if (personRepository.existsByColor(color)) {
            throw new IllegalArgumentException(COLOR_TAKEN);
        }
        Person p = new Person();
        apply(p, dto, color);
        return toDto(personRepository.save(p), false);
    }

    @Transactional
    public PersonDto update(Long id, PersonCreateDto dto) {
        Person p = getOrThrow(id);
        String color = normalizeColor(dto.color());
        if (personRepository.existsByColorAndIdNot(color, id)) {
            throw new IllegalArgumentException(COLOR_TAKEN);
        }
        apply(p, dto, color);
        return toDto(personRepository.save(p), photoRepository.existsById(id));
    }

    /** Les evenements de la personne sont conserves (sans personne, avec sa couleur) ; sa photo est supprimee. */
    @Transactional
    public void delete(Long id) {
        Person p = getOrThrow(id);
        eventRepository.detachPerson(id, p.getColor());
        personRepository.delete(p);
    }

    public PersonPhoto getPhoto(Long id) {
        return photoRepository
                .findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Pas de photo pour la personne: " + id));
    }

    @Transactional
    public void savePhoto(Long id, String contentType, byte[] data) {
        getOrThrow(id);
        String type = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        if (!ALLOWED_PHOTO_TYPES.contains(type)) {
            throw new IllegalArgumentException("Format de photo non supporte (JPEG, PNG ou WebP uniquement).");
        }
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("La photo est vide.");
        }
        if (data.length > MAX_PHOTO_BYTES) {
            throw new IllegalArgumentException("La photo est trop lourde (2 Mo maximum).");
        }
        PersonPhoto photo = photoRepository.findById(id).orElseGet(PersonPhoto::new);
        photo.setPersonId(id);
        photo.setContentType(type);
        photo.setData(data);
        photoRepository.save(photo);
    }

    @Transactional
    public void deletePhoto(Long id) {
        if (photoRepository.existsById(id)) {
            photoRepository.deleteById(id);
        }
    }

    Person getOrThrow(Long id) {
        return personRepository
                .findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Personne introuvable: " + id));
    }

    private static String normalizeColor(String color) {
        return color.trim().toLowerCase(Locale.ROOT);
    }

    private static void apply(Person p, PersonCreateDto dto, String color) {
        p.setFirstName(dto.firstName().trim());
        p.setLastName(dto.lastName().trim());
        p.setBirthDate(dto.birthDate());
        p.setColor(color);
    }

    private static PersonDto toDto(Person p, boolean hasPhoto) {
        return new PersonDto(p.getId(), p.getFirstName(), p.getLastName(), p.getBirthDate(), p.getColor(), hasPhoto);
    }
}
