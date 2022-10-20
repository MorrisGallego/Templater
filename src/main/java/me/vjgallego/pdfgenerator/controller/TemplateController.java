package me.vjgallego.pdfgenerator.controller;

import me.vjgallego.pdfgenerator.model.Metadata;
import me.vjgallego.pdfgenerator.service.GeneratorService;
import me.vjgallego.pdfgenerator.service.TemplateService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("templates")
public class TemplateController {
    private final TemplateService templateEngine;
    private final GeneratorService generator;

    public TemplateController(TemplateService templateEngine, GeneratorService generator) {
        this.templateEngine = templateEngine;
        this.generator = generator;
    }

    @GetMapping(
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public List<String> list(){
        return templateEngine.list();
    }

    @GetMapping(
            value = "{template}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Metadata metadata(@PathVariable String template){
        return templateEngine.metadata(template);
    }

    @GetMapping(
            value = "{template}/preview",
            produces = MediaType.IMAGE_JPEG_VALUE
    )
    public byte[] preview(@PathVariable String template){
        return generator.preview(template);
    }

    @PostMapping(
            consumes = { MediaType.MULTIPART_FORM_DATA_VALUE }
    )
    public ResponseEntity<?> post(@RequestParam("file") MultipartFile file) {
        try{
            templateEngine.deploy(file);

            return ResponseEntity.ok().build();
        } catch(Exception e){
            return ResponseEntity.badRequest().body(e);
        }
    }

    @DeleteMapping(value = "{template}")
    public ResponseEntity<?> delete(@PathVariable String template) {
        try{
            templateEngine.remove(template);

            return ResponseEntity.ok().build();
        } catch(Exception e){
            return ResponseEntity.badRequest().body(e);
        }

    }
}
