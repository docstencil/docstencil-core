package com.example.docstencildemo

import com.docstencil.core.api.OfficeTemplate
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class DocumentService {

    private val template = OfficeTemplate.fromResource("templates/welcome-letter.docx")

    fun generateWelcomeLetter(name: String, company: String): ByteArray {
        return template.render(mapOf(
            "name" to name,
            "company" to company,
            "date" to LocalDate.now()
        )).toByteArray()
    }
}
