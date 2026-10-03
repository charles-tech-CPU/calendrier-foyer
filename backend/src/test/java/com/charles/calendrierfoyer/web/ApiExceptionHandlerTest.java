package com.charles.calendrierfoyer.web;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void uneRessourceIntrouvableRenvoie404AvecLeMessage() {
        var response = handler.handleNotFound(new EntityNotFoundException("Evenement introuvable: 5"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("error", "Evenement introuvable: 5");
    }

    @Test
    void uneSaisieInvalideRenvoie400AvecLeMessage() {
        var response = handler.handleBadRequest(new IllegalArgumentException("L'heure de fin doit etre apres."));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("error", "L'heure de fin doit etre apres.");
    }

    @Test
    void unFichierTropLourdRenvoie413() {
        var response = handler.handleTooLarge(new MaxUploadSizeExceededException(5_000_000));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(response.getBody()).containsKey("error");
    }
}
