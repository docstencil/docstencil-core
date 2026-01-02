package com.example.invoices.model;

public record Company(
    String name,
    String address,
    String bankName,
    String accountNumber
) {}
