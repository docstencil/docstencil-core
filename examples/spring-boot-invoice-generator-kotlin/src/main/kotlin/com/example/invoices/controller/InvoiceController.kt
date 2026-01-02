package com.example.invoices.controller

import com.example.invoices.model.*
import com.example.invoices.service.InvoiceService
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.time.LocalDate

@RestController
@RequestMapping("/api/invoices")
class InvoiceController(private val invoiceService: InvoiceService) {

    @GetMapping("/{id}/download")
    fun download(@PathVariable id: String): ResponseEntity<ByteArray> {
        val invoice = createSampleInvoice(id)
        val bytes = invoiceService.generate(invoice)

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"invoice-$id.docx\"")
            .contentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            ))
            .body(bytes)
    }

    @GetMapping("/{id}/download/paid")
    fun downloadPaid(@PathVariable id: String): ResponseEntity<ByteArray> {
        val invoice = createSampleInvoice(id).copy(paid = true)
        val bytes = invoiceService.generate(invoice)

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"invoice-$id-paid.docx\"")
            .contentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            ))
            .body(bytes)
    }

    private fun createSampleInvoice(id: String): Invoice {
        return Invoice(
            invoiceNumber = "INV-$id",
            invoiceDate = LocalDate.now(),
            company = Company("Acme Corp", "123 Business Ave, New York, NY", "First National Bank", "1234567890"),
            customer = Customer("John Smith", "john@example.com"),
            items = listOf(
                LineItem("Web Development", 40, BigDecimal("150.00")),
                LineItem("UI Design", 20, BigDecimal("125.00"))
            ),
            taxRate = 10,
            paymentTermsDays = 30
        )
    }
}
