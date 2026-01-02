package com.example.invoices.controller;

import com.example.invoices.model.*;
import com.example.invoices.service.InvoiceService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable String id) {
        Invoice invoice = createSampleInvoice(id);
        byte[] bytes = invoiceService.generate(invoice);

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"invoice-" + id + ".docx\"")
            .contentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            ))
            .body(bytes);
    }

    @GetMapping("/{id}/download/paid")
    public ResponseEntity<byte[]> downloadPaid(@PathVariable String id) {
        Invoice invoice = createSampleInvoice(id).withPaid(true);
        byte[] bytes = invoiceService.generate(invoice);

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"invoice-" + id + "-paid.docx\"")
            .contentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            ))
            .body(bytes);
    }

    private Invoice createSampleInvoice(String id) {
        return new Invoice(
            "INV-" + id,
            LocalDate.now(),
            new Company("Acme Corp", "123 Business Ave, New York, NY", "First National Bank", "1234567890"),
            new Customer("John Smith", "john@example.com"),
            List.of(
                new LineItem("Web Development", 40, new BigDecimal("150.00")),
                new LineItem("UI Design", 20, new BigDecimal("125.00"))
            ),
            10,
            30
        );
    }
}
