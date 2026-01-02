package com.example.invoices.model

import java.math.BigDecimal
import java.time.LocalDate

data class Invoice(
    val invoiceNumber: String,
    val invoiceDate: LocalDate,
    val company: Company,
    val customer: Customer,
    val items: List<LineItem>,
    val taxRate: Int,
    val paymentTermsDays: Int,
    val paid: Boolean = false
) {
    val subtotal: BigDecimal
        get() = items.fold(BigDecimal.ZERO) { acc, item ->
            acc + (item.unitPrice * item.quantity.toBigDecimal())
        }

    val taxAmount: BigDecimal
        get() = subtotal * (taxRate.toBigDecimal() / BigDecimal(100))

    val total: BigDecimal
        get() = subtotal + taxAmount
}
