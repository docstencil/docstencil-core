package com.example.invoices.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class Invoice {
    private final String invoiceNumber;
    private final LocalDate invoiceDate;
    private final Company company;
    private final Customer customer;
    private final List<LineItem> items;
    private final int taxRate;
    private final int paymentTermsDays;
    private final boolean paid;

    public Invoice(
            String invoiceNumber,
            LocalDate invoiceDate,
            Company company,
            Customer customer,
            List<LineItem> items,
            int taxRate,
            int paymentTermsDays,
            boolean paid) {
        this.invoiceNumber = invoiceNumber;
        this.invoiceDate = invoiceDate;
        this.company = company;
        this.customer = customer;
        this.items = items;
        this.taxRate = taxRate;
        this.paymentTermsDays = paymentTermsDays;
        this.paid = paid;
    }

    public Invoice(
            String invoiceNumber,
            LocalDate invoiceDate,
            Company company,
            Customer customer,
            List<LineItem> items,
            int taxRate,
            int paymentTermsDays) {
        this(invoiceNumber, invoiceDate, company, customer, items, taxRate, paymentTermsDays, false);
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public LocalDate getInvoiceDate() {
        return invoiceDate;
    }

    public Company getCompany() {
        return company;
    }

    public Customer getCustomer() {
        return customer;
    }

    public List<LineItem> getItems() {
        return items;
    }

    public int getTaxRate() {
        return taxRate;
    }

    public int getPaymentTermsDays() {
        return paymentTermsDays;
    }

    public boolean isPaid() {
        return paid;
    }

    public BigDecimal getSubtotal() {
        return items.stream()
            .map(item -> item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTaxAmount() {
        return getSubtotal()
            .multiply(BigDecimal.valueOf(taxRate))
            .divide(BigDecimal.valueOf(100));
    }

    public BigDecimal getTotal() {
        return getSubtotal().add(getTaxAmount());
    }

    public Invoice withPaid(boolean paid) {
        return new Invoice(
            this.invoiceNumber,
            this.invoiceDate,
            this.company,
            this.customer,
            this.items,
            this.taxRate,
            this.paymentTermsDays,
            paid
        );
    }
}
