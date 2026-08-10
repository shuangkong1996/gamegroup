package com.gamegroup.service.resume;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 生成面向 openhtmltopdf 的 HTML（避免 flex/grid，改用 table 布局）。
 */
@Component
public class ResumeHtmlRenderer {

    private static final List<String> DEFAULT_SECTION_ORDER = List.of(
            "summary", "experience", "projects", "education", "skills"
    );

    public String render(String templateId, JsonNode content) {
        JsonNode safe = content == null || content.isNull() ? null : content;
        if ("sidebar".equalsIgnoreCase(templateId)) {
            return wrapDocument(renderSidebar(safe));
        }
        return wrapDocument(renderClassic(safe));
    }

    private String wrapDocument(String body) {
        return """
                <!DOCTYPE html>
                <html lang="zh-CN">
                <head>
                  <meta charset="UTF-8" />
                  <style>
                    * { box-sizing: border-box; }
                    html, body { margin: 0; padding: 0; }
                    body {
                      font-family: ResumeCJK, 'PingFang SC', 'Microsoft YaHei', sans-serif;
                      color: #111827;
                      background: #ffffff;
                    }
                    @page { size: A4; margin: 0; }
                    table { border-collapse: collapse; }
                    ul { margin: 0.35rem 0 0; padding-left: 1.1rem; }
                    li { margin-top: 0.15rem; }
                    p { margin: 0; }
                    h1, h2 { margin: 0; }
                  </style>
                </head>
                <body>%s</body>
                </html>
                """.formatted(body);
    }

    private String renderClassic(JsonNode content) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style=\"width:210mm;min-height:297mm;padding:14mm 16mm;font-size:13px;line-height:1.55;\">");

        sb.append("<table style=\"width:100%;margin-bottom:0.85rem;border-bottom:2px solid #111827;padding-bottom:0.85rem;\"><tr>");
        sb.append("<td style=\"vertical-align:top;width:62%;\">");
        sb.append("<h1 style=\"font-size:28px;font-weight:700;line-height:1.2;\">")
                .append(esc(text(content, "basics", "name"), "姓名")).append("</h1>");
        sb.append("<p style=\"margin-top:0.35rem;color:#374151;font-size:14px;\">")
                .append(esc(text(content, "basics", "title"), "职位意向")).append("</p>");
        sb.append("</td><td style=\"vertical-align:top;text-align:right;color:#4b5563;font-size:12px;line-height:1.7;\">");
        appendContactLine(sb, text(content, "basics", "phone"));
        appendContactLine(sb, text(content, "basics", "email"));
        appendContactLine(sb, text(content, "basics", "location"));
        appendContactLine(sb, text(content, "basics", "website"));
        sb.append("</td></tr></table>");

        for (String section : sectionOrder(content)) {
            switch (section) {
                case "summary" -> appendClassicSummary(sb, content);
                case "experience" -> appendClassicExperience(sb, content);
                case "projects" -> appendClassicProjects(sb, content);
                case "education" -> appendClassicEducation(sb, content);
                case "skills" -> appendClassicSkills(sb, content);
                default -> {
                }
            }
        }
        sb.append("</div>");
        return sb.toString();
    }

    private String renderSidebar(JsonNode content) {
        StringBuilder sb = new StringBuilder();
        sb.append("<table style=\"width:210mm;min-height:297mm;\"><tr>");

        sb.append("<td style=\"width:68mm;vertical-align:top;padding:14mm 10mm;background-color:#1f2937;color:#f9fafb;font-size:12.5px;line-height:1.55;\">");
        sb.append("<h1 style=\"font-size:24px;font-weight:700;line-height:1.25;\">")
                .append(esc(text(content, "basics", "name"), "姓名")).append("</h1>");
        sb.append("<p style=\"margin:0.4rem 0 1.2rem;color:#d1d5db;font-size:13px;\">")
                .append(esc(text(content, "basics", "title"), "职位意向")).append("</p>");

        sb.append("<h2 style=\"margin:0 0 0.45rem;font-size:11px;letter-spacing:0.12em;text-transform:uppercase;color:#93c5fd;\">联系方式</h2>");
        sb.append("<div style=\"margin:0 0 1.2rem;color:#e5e7eb;font-size:11.5px;line-height:1.75;\">");
        appendContactLine(sb, text(content, "basics", "phone"));
        appendContactLine(sb, text(content, "basics", "email"));
        appendContactLine(sb, text(content, "basics", "location"));
        appendContactLine(sb, text(content, "basics", "website"));
        sb.append("</div>");

        List<String> skills = stringList(content, "skills");
        if (!skills.isEmpty()) {
            sb.append("<h2 style=\"margin:0 0 0.45rem;font-size:11px;letter-spacing:0.12em;text-transform:uppercase;color:#93c5fd;\">技能</h2>");
            sb.append("<p style=\"margin:0;color:#f3f4f6;font-size:11.5px;line-height:1.7;\">")
                    .append(esc(String.join(" · ", skills))).append("</p>");
        }
        sb.append("</td>");

        sb.append("<td style=\"vertical-align:top;padding:14mm 12mm;font-size:12.5px;line-height:1.55;\">");
        for (String section : sectionOrder(content)) {
            switch (section) {
                case "summary" -> appendSidebarSummary(sb, content);
                case "experience" -> appendSidebarExperience(sb, content);
                case "projects" -> appendSidebarProjects(sb, content);
                case "education" -> appendSidebarEducation(sb, content);
                case "skills" -> {
                }
                default -> {
                }
            }
        }
        sb.append("</td></tr></table>");
        return sb.toString();
    }

    private void appendClassicSummary(StringBuilder sb, JsonNode content) {
        String summary = text(content, "summary");
        if (summary.isBlank()) {
            return;
        }
        appendClassicSectionTitle(sb, "个人简介");
        sb.append("<p style=\"color:#1f2937;white-space:pre-wrap;\">").append(esc(summary)).append("</p>");
    }

    private void appendClassicExperience(StringBuilder sb, JsonNode content) {
        JsonNode arr = array(content, "experience");
        if (arr == null || arr.isEmpty()) {
            return;
        }
        appendClassicSectionTitle(sb, "工作经历");
        for (JsonNode item : arr) {
            sb.append("<div style=\"margin-top:0.7rem;\">");
            appendRow(sb, esc(text(item, "role"), "职位") + " · " + esc(text(item, "company"), "公司"),
                    esc(period(text(item, "start"), text(item, "end"))));
            appendBullets(sb, item);
            sb.append("</div>");
        }
    }

    private void appendClassicProjects(StringBuilder sb, JsonNode content) {
        JsonNode arr = array(content, "projects");
        if (arr == null || arr.isEmpty()) {
            return;
        }
        appendClassicSectionTitle(sb, "项目经历");
        for (JsonNode item : arr) {
            sb.append("<div style=\"margin-top:0.7rem;\">");
            String heading = esc(text(item, "name"), "项目名称");
            String role = text(item, "role");
            if (!role.isBlank()) {
                heading = heading + " · " + esc(role);
            }
            appendRow(sb, heading, esc(text(item, "link")));
            appendBullets(sb, item);
            sb.append("</div>");
        }
    }

    private void appendClassicEducation(StringBuilder sb, JsonNode content) {
        JsonNode arr = array(content, "education");
        if (arr == null || arr.isEmpty()) {
            return;
        }
        appendClassicSectionTitle(sb, "教育经历");
        for (JsonNode item : arr) {
            sb.append("<div style=\"margin-top:0.7rem;\">");
            appendRow(sb, esc(text(item, "school"), "学校") + " · " + esc(text(item, "degree"), "学历"),
                    esc(period(text(item, "start"), text(item, "end"))));
            String desc = text(item, "description");
            if (!desc.isBlank()) {
                sb.append("<p style=\"margin-top:0.35rem;color:#1f2937;white-space:pre-wrap;\">").append(esc(desc)).append("</p>");
            }
            sb.append("</div>");
        }
    }

    private void appendClassicSkills(StringBuilder sb, JsonNode content) {
        List<String> skills = stringList(content, "skills");
        if (skills.isEmpty()) {
            return;
        }
        appendClassicSectionTitle(sb, "技能");
        sb.append("<p style=\"color:#1f2937;\">").append(esc(String.join(" · ", skills))).append("</p>");
    }

    private void appendClassicSectionTitle(StringBuilder sb, String title) {
        sb.append("<h2 style=\"margin:1rem 0 0.45rem;padding-bottom:0.2rem;border-bottom:1px solid #d1d5db;font-size:13px;font-weight:700;letter-spacing:0.08em;text-transform:uppercase;\">")
                .append(esc(title)).append("</h2>");
    }

    private void appendSidebarSummary(StringBuilder sb, JsonNode content) {
        String summary = text(content, "summary");
        if (summary.isBlank()) {
            return;
        }
        appendSidebarSectionTitle(sb, "个人简介");
        sb.append("<p style=\"color:#1f2937;white-space:pre-wrap;\">").append(esc(summary)).append("</p>");
    }

    private void appendSidebarExperience(StringBuilder sb, JsonNode content) {
        JsonNode arr = array(content, "experience");
        if (arr == null || arr.isEmpty()) {
            return;
        }
        appendSidebarSectionTitle(sb, "工作经历");
        for (JsonNode item : arr) {
            sb.append("<div style=\"margin-top:0.75rem;\">");
            sb.append("<div style=\"font-weight:700;\">")
                    .append(esc(text(item, "role"), "职位")).append(" · ")
                    .append(esc(text(item, "company"), "公司")).append("</div>");
            sb.append("<div style=\"color:#6b7280;font-size:11.5px;margin-top:0.15rem;\">")
                    .append(esc(period(text(item, "start"), text(item, "end")))).append("</div>");
            appendBullets(sb, item);
            sb.append("</div>");
        }
    }

    private void appendSidebarProjects(StringBuilder sb, JsonNode content) {
        JsonNode arr = array(content, "projects");
        if (arr == null || arr.isEmpty()) {
            return;
        }
        appendSidebarSectionTitle(sb, "项目经历");
        for (JsonNode item : arr) {
            sb.append("<div style=\"margin-top:0.75rem;\">");
            sb.append("<div style=\"font-weight:700;\">").append(esc(text(item, "name"), "项目名称"));
            String role = text(item, "role");
            if (!role.isBlank()) {
                sb.append(" · ").append(esc(role));
            }
            sb.append("</div>");
            String link = text(item, "link");
            if (!link.isBlank()) {
                sb.append("<div style=\"color:#6b7280;font-size:11.5px;margin-top:0.15rem;\">").append(esc(link)).append("</div>");
            }
            appendBullets(sb, item);
            sb.append("</div>");
        }
    }

    private void appendSidebarEducation(StringBuilder sb, JsonNode content) {
        JsonNode arr = array(content, "education");
        if (arr == null || arr.isEmpty()) {
            return;
        }
        appendSidebarSectionTitle(sb, "教育经历");
        for (JsonNode item : arr) {
            sb.append("<div style=\"margin-top:0.75rem;\">");
            sb.append("<div style=\"font-weight:700;\">")
                    .append(esc(text(item, "school"), "学校")).append(" · ")
                    .append(esc(text(item, "degree"), "学历")).append("</div>");
            sb.append("<div style=\"color:#6b7280;font-size:11.5px;margin-top:0.15rem;\">")
                    .append(esc(period(text(item, "start"), text(item, "end")))).append("</div>");
            String desc = text(item, "description");
            if (!desc.isBlank()) {
                sb.append("<p style=\"margin-top:0.35rem;color:#1f2937;white-space:pre-wrap;\">").append(esc(desc)).append("</p>");
            }
            sb.append("</div>");
        }
    }

    private void appendSidebarSectionTitle(StringBuilder sb, String title) {
        sb.append("<h2 style=\"margin:0 0 0.5rem;padding-bottom:0.25rem;border-bottom:2px solid #e5e7eb;font-size:13px;font-weight:700;letter-spacing:0.06em;\">")
                .append(esc(title)).append("</h2>");
    }

    private void appendRow(StringBuilder sb, String leftHtml, String rightHtml) {
        sb.append("<table style=\"width:100%;\"><tr>");
        sb.append("<td style=\"vertical-align:baseline;font-weight:700;\">").append(leftHtml).append("</td>");
        if (rightHtml != null && !rightHtml.isBlank()) {
            sb.append("<td style=\"vertical-align:baseline;text-align:right;color:#6b7280;font-size:12px;white-space:nowrap;\">")
                    .append(rightHtml).append("</td>");
        }
        sb.append("</tr></table>");
    }

    private void appendBullets(StringBuilder sb, JsonNode item) {
        List<String> bullets = stringList(item, "bullets");
        if (bullets.isEmpty()) {
            return;
        }
        sb.append("<ul>");
        for (String bullet : bullets) {
            sb.append("<li>").append(esc(bullet)).append("</li>");
        }
        sb.append("</ul>");
    }

    private void appendContactLine(StringBuilder sb, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        sb.append("<div>").append(esc(value)).append("</div>");
    }

    private List<String> sectionOrder(JsonNode content) {
        List<String> order = new ArrayList<>();
        JsonNode arr = array(content, "sectionOrder");
        if (arr != null) {
            for (JsonNode node : arr) {
                if (node != null && node.isTextual()) {
                    String value = node.asText().trim();
                    if (!value.isEmpty() && !order.contains(value)) {
                        order.add(value);
                    }
                }
            }
        }
        for (String section : DEFAULT_SECTION_ORDER) {
            if (!order.contains(section)) {
                order.add(section);
            }
        }
        return order;
    }

    private JsonNode array(JsonNode root, String field) {
        if (root == null || !root.has(field) || !root.get(field).isArray()) {
            return null;
        }
        return root.get(field);
    }

    private List<String> stringList(JsonNode root, String field) {
        List<String> result = new ArrayList<>();
        JsonNode arr = array(root, field);
        if (arr == null) {
            return result;
        }
        for (JsonNode node : arr) {
            if (node != null && node.isTextual()) {
                String value = node.asText().trim();
                if (!value.isEmpty()) {
                    result.add(value);
                }
            }
        }
        return result;
    }

    private String text(JsonNode root, String... path) {
        JsonNode current = root;
        for (String key : path) {
            if (current == null || !current.has(key) || current.get(key).isNull()) {
                return "";
            }
            current = current.get(key);
        }
        if (current == null || current.isNull()) {
            return "";
        }
        if (current.isTextual() || current.isNumber() || current.isBoolean()) {
            return current.asText().trim();
        }
        return "";
    }

    private String period(String start, String end) {
        if (start.isBlank() && end.isBlank()) {
            return "";
        }
        if (end.isBlank()) {
            return start;
        }
        if (start.isBlank()) {
            return end;
        }
        return start + " - " + end;
    }

    private String esc(String value) {
        return HtmlUtils.htmlEscape(value == null ? "" : value);
    }

    private String esc(String value, String fallback) {
        String text = value == null ? "" : value.trim();
        return HtmlUtils.htmlEscape(text.isEmpty() ? fallback : text);
    }
}
