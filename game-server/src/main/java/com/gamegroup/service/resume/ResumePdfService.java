package com.gamegroup.service.resume;

import com.gamegroup.common.exception.BusinessException;
import com.gamegroup.common.response.ErrorCode;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Slf4j
@Service
public class ResumePdfService {

    private static final String FONT_FAMILY = "ResumeCJK";

    private static final List<String> SYSTEM_FONT_CANDIDATES = List.of(
            // Linux common CJK fonts
            "/usr/share/fonts/truetype/wqy/wqy-microhei.ttc",
            "/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc",
            "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc",
            "/usr/share/fonts/opentype/noto/NotoSansCJKsc-Regular.otf",
            "/usr/share/fonts/google-noto-cjk/NotoSansCJK-Regular.ttc",
            "/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc",
            "/usr/share/fonts/truetype/arphic/uming.ttc",
            "/usr/share/fonts/truetype/droid/DroidSansFallbackFull.ttf",
            // macOS
            "/System/Library/Fonts/PingFang.ttc",
            "/System/Library/Fonts/STHeiti Light.ttc",
            "/Library/Fonts/Arial Unicode.ttf",
            // Windows (偶尔本机调试)
            "C:/Windows/Fonts/msyh.ttc",
            "C:/Windows/Fonts/simhei.ttf",
            "C:/Windows/Fonts/simsun.ttc"
    );

    public byte[] htmlToPdf(String html) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.useDefaultPageSize(210, 297, PdfRendererBuilder.PageSizeUnits.MM);
            registerCjkFont(builder);
            builder.withHtmlContent(html, null);
            builder.toStream(output);
            builder.run();
            return output.toByteArray();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("OpenHTMLToPDF generation failed", e);
            throw new BusinessException(ErrorCode.RESUME_PDF_FAILED);
        }
    }

    private void registerCjkFont(PdfRendererBuilder builder) {
        for (String classpathPath : List.of("fonts/resume-cjk.ttf", "fonts/resume-cjk.ttc", "fonts/resume-cjk.otf")) {
            ClassPathResource classpathFont = new ClassPathResource(classpathPath);
            if (!classpathFont.exists()) {
                continue;
            }
            builder.useFont(() -> openClasspath(classpathFont), FONT_FAMILY);
            log.info("Resume PDF using classpath font {}", classpathPath);
            return;
        }

        for (String candidate : SYSTEM_FONT_CANDIDATES) {
            Path path = Path.of(candidate);
            if (!Files.isRegularFile(path)) {
                continue;
            }
            File file = path.toFile();
            builder.useFont(file, FONT_FAMILY);
            log.info("Resume PDF using system font {}", candidate);
            return;
        }

        log.warn("No CJK font found for resume PDF. Chinese glyphs may be missing. "
                + "See game-server/src/main/resources/fonts/README.md");
    }

    private InputStream openClasspath(ClassPathResource resource) {
        try {
            return resource.getInputStream();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.RESUME_PDF_FAILED);
        }
    }
}
