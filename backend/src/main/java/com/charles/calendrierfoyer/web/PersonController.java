package com.charles.calendrierfoyer.web;

import com.charles.calendrierfoyer.domain.PersonPhoto;
import com.charles.calendrierfoyer.dto.PersonCreateDto;
import com.charles.calendrierfoyer.dto.PersonDto;
import com.charles.calendrierfoyer.service.PersonService;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/persons")
public class PersonController {

    private final PersonService personService;

    public PersonController(PersonService personService) {
        this.personService = personService;
    }

    @GetMapping
    public List<PersonDto> findAll() {
        return personService.findAll();
    }

    @GetMapping("/{id}")
    public PersonDto findById(@PathVariable Long id) {
        return personService.findById(id);
    }

    @PostMapping
    public PersonDto create(@Valid @RequestBody PersonCreateDto dto) {
        return personService.create(dto);
    }

    @PutMapping("/{id}")
    public PersonDto update(@PathVariable Long id, @Valid @RequestBody PersonCreateDto dto) {
        return personService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        personService.delete(id);
    }

    @GetMapping("/{id}/photo")
    public ResponseEntity<byte[]> photo(@PathVariable Long id) {
        PersonPhoto photo = personService.getPhoto(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(photo.getContentType()))
                .cacheControl(CacheControl.noCache())
                .body(photo.getData());
    }

    @PutMapping(path = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public void uploadPhoto(@PathVariable Long id, @RequestParam("file") MultipartFile file) throws IOException {
        personService.savePhoto(id, file.getContentType(), file.getBytes());
    }

    @DeleteMapping("/{id}/photo")
    public void deletePhoto(@PathVariable Long id) {
        personService.deletePhoto(id);
    }
}
