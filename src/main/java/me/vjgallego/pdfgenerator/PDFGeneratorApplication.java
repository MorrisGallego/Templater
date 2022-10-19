package me.vjgallego.pdfgenerator;

import me.vjgallego.pdfgenerator.configuration.ApplicationPaths;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(ApplicationPaths.class)
public class PDFGeneratorApplication {
    public static void main(String[] args) {
        SpringApplication.run(PDFGeneratorApplication.class, args);
    }
}
