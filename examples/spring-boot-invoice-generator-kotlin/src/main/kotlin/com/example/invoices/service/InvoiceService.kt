package com.example.invoices.service

import com.docstencil.core.api.OfficeTemplate
import com.example.invoices.model.Invoice
import org.springframework.stereotype.Service

@Service
class InvoiceService {

    private val template = OfficeTemplate.fromResource("templates/invoice.docx")

    fun generate(invoice: Invoice): ByteArray {
        return template.render(mapOf("invoice" to invoice)).toByteArray()
    }
}
