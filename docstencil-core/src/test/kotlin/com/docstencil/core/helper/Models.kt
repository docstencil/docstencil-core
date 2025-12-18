package com.docstencil.core.helper

import java.time.LocalDate

data class Person(var firstName: String, var lastName: String)

data class Company(var name: String, var employees: MutableList<com.docstencil.core.helper.Person>)

data class Item(var name: String, var children: MutableList<Item> = mutableListOf())

data class Chain(var name: String, var child: Chain? = null)

data class Section(
    var title: String,
    var paragraphs: List<String>,
    var tables: List<List<List<String>>>,
    var children: List<Section>,
)

data class InvoiceAddress(
    val street: String,
    val streetNumber: String,
    val zipCode: String,
    val city: String,
    val country: String,
)

data class InvoicePerson(
    val name: String,
    val address: InvoiceAddress,
    val uid: String,
    val companyRegistrationNumber: String,
    val iban: String,
)

data class InvoicePosition(
    val name: String,
    val hours: Int,
    val rate: Int,
)

data class Invoice(
    val id: String,
    val serviceName: String,
    val freelancer: InvoicePerson,
    val customer: InvoicePerson,
    val positions: List<InvoicePosition>,
    val date: LocalDate,
    val serviceFrom: LocalDate,
    val serviceTo: LocalDate,
    val vatPercent: Int,
)
