package com.example.invoices.service;

import com.docstencil.core.api.OfficeTemplate;
import com.example.invoices.model.Invoice;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class InvoiceService {

    private final OfficeTemplate template =
        OfficeTemplate.fromResource("templates/invoice.docx");

    public byte[] generate(Invoice invoice) {
        return template.render(Map.of("invoice", invoice)).toByteArray();
    }
}
