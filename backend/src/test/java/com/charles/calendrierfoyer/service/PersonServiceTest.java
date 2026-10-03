package com.charles.calendrierfoyer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.charles.calendrierfoyer.domain.Person;
import com.charles.calendrierfoyer.domain.PersonPhoto;
import com.charles.calendrierfoyer.dto.PersonCreateDto;
import com.charles.calendrierfoyer.dto.PersonDto;
import com.charles.calendrierfoyer.repository.EventRepository;
import com.charles.calendrierfoyer.repository.PersonPhotoRepository;
import com.charles.calendrierfoyer.repository.PersonRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PersonServiceTest {

    private static final LocalDate BIRTH = LocalDate.of(2018, 3, 15);
    private static final byte[] JPEG = {1, 2, 3};

    @Mock
    private PersonRepository personRepository;

    @Mock
    private PersonPhotoRepository photoRepository;

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private PersonService personService;

    private static Person person(Long id, String firstName, String color) {
        Person p = new Person();
        p.setId(id);
        p.setFirstName(firstName);
        p.setLastName("Graire");
        p.setBirthDate(BIRTH);
        p.setColor(color);
        return p;
    }

    @Test
    void lesPersonnesSontTrieesParPrenomAvecIndicationDePhoto() {
        when(personRepository.findAll())
                .thenReturn(List.of(person(1L, "zoe", "#111111"), person(2L, "Caitlyn", "#222222")));
        when(photoRepository.findAllPersonIds()).thenReturn(Set.of(1L));

        List<PersonDto> persons = personService.findAll();

        assertThat(persons).extracting(PersonDto::firstName).containsExactly("Caitlyn", "zoe");
        assertThat(persons).extracting(PersonDto::hasPhoto).containsExactly(false, true);
    }

    @Test
    void trouverUnePersonne() {
        when(personRepository.findById(1L)).thenReturn(Optional.of(person(1L, "Caitlyn", "#e91e63")));
        when(photoRepository.existsById(1L)).thenReturn(true);

        PersonDto dto = personService.findById(1L);

        assertThat(dto.firstName()).isEqualTo("Caitlyn");
        assertThat(dto.hasPhoto()).isTrue();
    }

    @Test
    void trouverUnePersonneInconnueLeveUneErreur() {
        when(personRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> personService.findById(1L)).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void creerUnePersonneNormaliseLaCouleurEtLesNoms() {
        when(personRepository.existsByColor("#e91e63")).thenReturn(false);
        when(personRepository.save(any(Person.class))).thenAnswer(inv -> inv.getArgument(0));

        PersonDto dto = personService.create(new PersonCreateDto(" Caitlyn ", "Graire ", BIRTH, "#E91E63"));

        assertThat(dto.firstName()).isEqualTo("Caitlyn");
        assertThat(dto.lastName()).isEqualTo("Graire");
        assertThat(dto.color()).isEqualTo("#e91e63");
        assertThat(dto.hasPhoto()).isFalse();
    }

    @Test
    void deuxPersonnesNePeuventPasAvoirLaMemeCouleur() {
        when(personRepository.existsByColor("#e91e63")).thenReturn(true);
        PersonCreateDto dto = new PersonCreateDto("Caitlyn", "Graire", BIRTH, "#e91e63");

        assertThatThrownBy(() -> personService.create(dto)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void modifierUnePersonne() {
        Person p = person(1L, "Caitlin", "#e91e63");
        when(personRepository.findById(1L)).thenReturn(Optional.of(p));
        when(personRepository.existsByColorAndIdNot("#2196f3", 1L)).thenReturn(false);
        when(personRepository.save(p)).thenReturn(p);

        PersonDto dto = personService.update(1L, new PersonCreateDto("Caitlyn", "Graire", BIRTH, "#2196f3"));

        assertThat(dto.firstName()).isEqualTo("Caitlyn");
        assertThat(dto.color()).isEqualTo("#2196f3");
    }

    @Test
    void modifierAvecLaCouleurDUnAutreEstRefuse() {
        when(personRepository.findById(1L)).thenReturn(Optional.of(person(1L, "Caitlyn", "#e91e63")));
        when(personRepository.existsByColorAndIdNot("#2196f3", 1L)).thenReturn(true);
        PersonCreateDto dto = new PersonCreateDto("Caitlyn", "Graire", BIRTH, "#2196f3");

        assertThatThrownBy(() -> personService.update(1L, dto)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void supprimerUnePersonneDetacheSesEvenementsEnGardantSaCouleur() {
        Person p = person(1L, "Caitlyn", "#e91e63");
        when(personRepository.findById(1L)).thenReturn(Optional.of(p));

        personService.delete(1L);

        verify(eventRepository).detachPerson(1L, "#e91e63");
        verify(personRepository).delete(p);
    }

    @Test
    void enregistrerUnePhoto() {
        when(personRepository.findById(1L)).thenReturn(Optional.of(person(1L, "Caitlyn", "#e91e63")));
        when(photoRepository.findById(1L)).thenReturn(Optional.empty());

        personService.savePhoto(1L, "IMAGE/JPEG", JPEG);

        ArgumentCaptor<PersonPhoto> saved = ArgumentCaptor.forClass(PersonPhoto.class);
        verify(photoRepository).save(saved.capture());
        assertThat(saved.getValue().getPersonId()).isEqualTo(1L);
        assertThat(saved.getValue().getContentType()).isEqualTo("image/jpeg");
        assertThat(saved.getValue().getData()).isEqualTo(JPEG);
    }

    @Test
    void lesPhotosInvalidesSontRefusees() {
        when(personRepository.findById(1L)).thenReturn(Optional.of(person(1L, "Caitlyn", "#e91e63")));
        byte[] tooBig = new byte[PersonService.MAX_PHOTO_BYTES + 1];

        assertThatThrownBy(() -> personService.savePhoto(1L, "application/pdf", JPEG))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> personService.savePhoto(1L, null, JPEG)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> personService.savePhoto(1L, "image/png", new byte[0]))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> personService.savePhoto(1L, "image/png", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> personService.savePhoto(1L, "image/png", tooBig))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void lirePhoto() {
        PersonPhoto photo = new PersonPhoto();
        when(photoRepository.findById(1L)).thenReturn(Optional.of(photo));

        assertThat(personService.getPhoto(1L)).isSameAs(photo);
    }

    @Test
    void lirePhotoAbsenteLeveUneErreur() {
        when(photoRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> personService.getPhoto(1L)).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void supprimerLaPhotoSeulementSiElleExiste() {
        when(photoRepository.existsById(1L)).thenReturn(true);
        when(photoRepository.existsById(2L)).thenReturn(false);

        personService.deletePhoto(1L);
        personService.deletePhoto(2L);

        verify(photoRepository).deleteById(1L);
        verify(photoRepository, never()).deleteById(2L);
    }
}
