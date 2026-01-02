package com.example.docstencildemo;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping("/welcome")
    public ResponseEntity<byte[]> generateWelcome(
            @RequestParam String name,
            @RequestParam String company) {
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"welcome.docx\"")
            .contentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
            .body(documentService.generateWelcomeLetter(name, company));
    }
}
