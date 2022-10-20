package me.vjgallego.pdfgenerator.service;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.TagType;
import com.github.jknack.handlebars.cache.HighConcurrencyTemplateCache;
import com.github.jknack.handlebars.io.FileTemplateLoader;
import me.vjgallego.pdfgenerator.configuration.ApplicationPaths;
import me.vjgallego.pdfgenerator.model.Metadata;
import net.lingala.zip4j.ZipFile;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class TemplateService {
    private final Handlebars templateEngine;
    private final ApplicationPaths paths;

    public TemplateService(ApplicationPaths paths) {
        this.paths = paths;
        templateEngine = new Handlebars()
                .with(new FileTemplateLoader(paths.path(), FileSystems.getDefault().getSeparator()+"index.hbs"))
                .with(new HighConcurrencyTemplateCache());
    }

    public File compile(String template, Map<String, String> parameters) {
        try {
            // Create a temporary file to store the compiled html
            var tempFile = Files.createTempFile(Path.of(paths.path(), template), "", ".html").toFile();

            // Compile the template and write to the temporal file
            try(var out = new PrintWriter(tempFile)){
                out.print(templateEngine.compile(template).apply(parameters));
                out.flush();
            }

            // Return the compiled file
            return tempFile;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void deploy(MultipartFile file) {
        try {
            // Extract template name from uploaded file
            var template = Path.of(Objects.requireNonNull(file.getOriginalFilename())).getFileName().toString().replace(".zip", "");

            // Construct the zip location
            var zipLocation = Path.of(paths.path(), template+".zip");

            // Construct the template location
            var templateLocation = Path.of(paths.path(), template);

            // If a template with the same name already exists, throw error
            if(list().contains(template)){
                throw new IOException("Template already exists!");
            }

            // Save uploaded template to templates directory
            file.transferTo(zipLocation);

            // Unzip template
            try(var zip = new ZipFile(zipLocation.toFile())) {
                zip.extractAll(templateLocation.toString());
            }

            // If no index.hbs exists, delete template and throw error
            if(Arrays.stream(Objects.requireNonNull(templateLocation.toFile().list())).noneMatch(filepath -> filepath.endsWith("index.hbs"))){
                Files.delete(templateLocation);
                throw new IOException(template+"/index.hbs file not found!");
            }

            // Delete the zip file
            Files.delete(zipLocation);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized void remove(String template) {
        try {
            if(!Files.exists(Path.of(paths.path(), template))) {
                throw new IOException("Template "+template+" not found!");
            }

            FileSystemUtils.deleteRecursively(Path.of(paths.path(), template));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Metadata metadata(String template) {
        try {
            return new Metadata().setVariables(templateEngine.compile(template).collect(TagType.VAR));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<String> list() {
        try(var templates= Files.list(Path.of(paths.path())).filter(path -> path.toFile().isDirectory())) {
            return templates.map(Path::getFileName).map(Path::toString).collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
