package com.example.docstencildemo

import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/documents")
class DocumentController(private val documentService: DocumentService) {

    @GetMapping("/welcome")
    fun generateWelcome(
        @RequestParam name: String,
        @RequestParam company: String
    ): ResponseEntity<ByteArray> {
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"welcome.docx\"")
            .contentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
            .body(documentService.generateWelcomeLetter(name, company))
    }
}
