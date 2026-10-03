package com.charles.calendrierfoyer.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.charles.calendrierfoyer.domain.PersonPhoto;
import com.charles.calendrierfoyer.dto.PersonCreateDto;
import com.charles.calendrierfoyer.service.PersonService;
import java.io.IOException;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class PersonControllerTest {

    @Mock
    private PersonService personService;

    @InjectMocks
    private PersonController controller;

    @Test
    void chaqueRouteDelegueAuService() throws IOException {
        PersonCreateDto dto = new PersonCreateDto("Caitlyn", "Graire", LocalDate.of(2018, 3, 15), "#e91e63");
        byte[] data = {1, 2, 3};

        controller.findAll();
        controller.findById(1L);
        controller.create(dto);
        controller.update(1L, dto);
        controller.delete(1L);
        controller.uploadPhoto(1L, new MockMultipartFile("file", "photo.jpg", "image/jpeg", data));
        controller.deletePhoto(1L);

        verify(personService).findAll();
        verify(personService).findById(1L);
        verify(personService).create(dto);
        verify(personService).update(1L, dto);
        verify(personService).delete(1L);
        verify(personService).savePhoto(1L, "image/jpeg", data);
        verify(personService).deletePhoto(1L);
    }

    @Test
    void laPhotoEstRenvoyeeAvecSonTypeEtSansCache() {
        PersonPhoto photo = new PersonPhoto();
        photo.setContentType("image/png");
        photo.setData(new byte[] {9});
        when(personService.getPhoto(1L)).thenReturn(photo);

        var response = controller.photo(1L);

        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.IMAGE_PNG);
        assertThat(response.getHeaders().getCacheControl()).isEqualTo("no-cache");
        assertThat(response.getBody()).containsExactly(9);
    }
}
