package com.example.docstencildemo;

import com.docstencil.core.api.OfficeTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;

@Service
public class DocumentService {

    private final OfficeTemplate template =
        OfficeTemplate.fromResource("templates/welcome-letter.docx");

    public byte[] generateWelcomeLetter(String name, String company) {
        return template.render(Map.of(
            "name", name,
            "company", company,
            "date", LocalDate.now()
        )).toByteArray();
    }
}
