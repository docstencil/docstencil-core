package com.example.invoices.model

import java.math.BigDecimal

data class LineItem(
    val description: String,
    val quantity: Int,
    val unitPrice: BigDecimal
)
