package com.example.cashcard;

import org.springframework.data.annotation.Id;
    record CashCard(@Id Long id, double amount, String owner) {}