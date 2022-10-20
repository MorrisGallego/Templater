package me.vjgallego.pdfgenerator.service;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.ScreenshotType;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

@Service
public class GeneratorService {
    private final Browser browser;
    private final TemplateService templates;

    public GeneratorService(TemplateService templates) {
        this.templates = templates;
        this.browser = Playwright.create().chromium().launch();
    }

    public byte[] generate(String template, Map<String, String> parameters) {
        try {
            // Open a new browser tab
            var page = browser.newPage();

            // Compile the template to a temporal html file
            var file = templates.compile(template, parameters);

            // Define options for the PDF
            var options = new Page.PdfOptions()
                    .setFormat("A4")
                    .setDisplayHeaderFooter(false)
                    .setPrintBackground(false)
                    .setScale(1.0)
                    .setPreferCSSPageSize(true);

            // Navigate to temporal html file
            page.navigate(file.getAbsolutePath());

            // Print tab to PDF
            var pdf = page.pdf(options);

            // Close tab
            page.close();
            // Delete temporal file
            Files.delete(file.toPath());

            return pdf;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] preview(String template) {
        try {
            // Open a new browser tab
            var page = browser.newPage();

            // Build a map with the default parameters
            var defaultParameters = new HashMap<String, String>();
            for(var parameter: templates.metadata(template).variables())
                defaultParameters.put(parameter, "{{"+parameter+"}}");

            // Compile the template to a temporal html file
            var file = templates.compile(template, defaultParameters);

            // Navigate to temporal html file
            page.navigate(file.getAbsolutePath());

            // Build screenshot options
            var options = new Locator.ScreenshotOptions()
                    .setType(ScreenshotType.JPEG)
                    .setOmitBackground(true);

            // Print tab body to JPG
            var image = page.locator("body").screenshot(options);

            // Close tab
            page.close();

            // Delete temporal files
            Files.delete(file.toPath());

            return image;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
