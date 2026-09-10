package com.gamegroup.service.resume;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.xml.sax.InputSource;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;

import javax.xml.parsers.DocumentBuilderFactory;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThat;

class ResumeHtmlRendererTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ResumeHtmlRenderer renderer = new ResumeHtmlRenderer();
    private final ResumePdfService pdfService = new ResumePdfService();

    @Test
    void should_render_xml_safe_html_when_content_contains_middle_dot_and_special_characters() throws Exception {
        JsonNode content = objectMapper.readTree("""
                {
                  "basics": { "name": "邓睿", "title": "Java · Spring & AI" },
                  "skills": ["Spring Boot · MyBatis", "Docker < K8S"],
                  "sectionOrder": ["skills"]
                }
                """);

        String html = renderer.render("sidebar", content);

        assertThat(html).contains("Java · Spring &amp; AI")
                .doesNotContain("&middot;")
                .contains("Docker &lt; K8S");
        assertThatCode(() -> DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(new InputSource(new StringReader(html))))
                .doesNotThrowAnyException();
    }

    @Test
    void should_generate_pdf_when_content_contains_middle_dot() throws Exception {
        JsonNode content = objectMapper.readTree("""
                {
                  "basics": { "name": "邓睿", "title": "Java · Spring" },
                  "skills": ["Spring Boot · MyBatis"],
                  "sectionOrder": ["skills"]
                }
                """);

        byte[] pdf = pdfService.htmlToPdf(renderer.render("sidebar", content));

        assertThat(new String(pdf, 0, 4, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF");
        try (PDDocument document = PDDocument.load(pdf)) {
            String renderedText = new PDFTextStripper().getText(document);
            assertThat(renderedText).contains("邓睿", "Java", "Spring", "Spring", "Boot", "MyBatis");
        }
    }
}
