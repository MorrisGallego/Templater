package me.vjgallego.pdfgenerator.controller;

import com.github.jknack.handlebars.Handlebars;
import me.vjgallego.pdfgenerator.configuration.ApplicationPaths;
import me.vjgallego.pdfgenerator.service.GeneratorService;
import net.lingala.zip4j.ZipFile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("generator")
public class GeneratorController {
    private final GeneratorService generator;
    private final Handlebars interpreter;
    private final ApplicationPaths paths;

    public GeneratorController(GeneratorService generator, ApplicationPaths paths) {
        this.generator = generator;
        this.paths = paths;
        this.interpreter = new Handlebars();
    }

    @PostMapping(
            value = "{template}",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public @ResponseBody ResponseEntity<byte[]> post(
            @PathVariable String template,
            @RequestParam(value = "fileNamePattern", required = false) String fileNamePattern,
            @RequestBody List<Map<String, String>> parameters
    ){
        try {
            if(parameters.size() == 1){
                // Build file name pattern
                var filename = (fileNamePattern != null && !fileNamePattern.isBlank()
                        ? interpreter.compileInline(fileNamePattern).apply(parameters.getFirst())
                        : UUID.randomUUID().toString()) + ".pdf";

                // Generate the PDF
                var file = generator.generate(template, parameters.getFirst());

                // Generate and return the pdf
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                        .contentType(MediaType.APPLICATION_PDF)
                        .contentLength(file.length)
                        .body(file);
            } else {
                // Generate a PDF for each parameter map
                var files = parameters.stream()
                        .map(params -> new FileWithMetadata(generator.generate(template, params), params))
                        .map(fileWithMetadata -> {
                            var params = fileWithMetadata.metadata;
                            var bytes = fileWithMetadata.file;

                            try {
                                var filename = (fileNamePattern != null
                                        ? interpreter.compileInline(fileNamePattern).apply(params)
                                        : UUID.randomUUID().toString()) + ".pdf";
                                var file = Files.createFile(Path.of(paths.path(), template, filename)).toFile();

                                try(var writer = new PrintStream(file)) {
                                    writer.write(bytes);
                                }

                                return file;
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }).toList();

                byte[] bytes;

                // Create a zip file and add the generated PDFs
                try(var zipFile = new ZipFile(Path.of(paths.path(), template, UUID.randomUUID().toString()).toString())) {
                    // Add files to zip
                    zipFile.addFiles(files);

                    // Read bytes from zip file
                    bytes = Files.readAllBytes(zipFile.getFile().toPath());

                    // Clean up temporary files
                    Files.delete(zipFile.getFile().toPath());
                }

                // Clean up temporary files
                for(var file: files)
                    Files.delete(file.toPath());

                // Return the zip file
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + UUID.randomUUID() + ".zip")
                        .contentType(MediaType.parseMediaType("application/zip"))
                        .contentLength(bytes.length)
                        .body(bytes);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private record FileWithMetadata(byte[] file, Map<String, String> metadata){}
}
