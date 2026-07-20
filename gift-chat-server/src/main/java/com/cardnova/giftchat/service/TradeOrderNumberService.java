package com.cardnova.giftchat.service;

import com.cardnova.giftchat.entity.TradeOrderNumberEntity;
import com.cardnova.giftchat.repository.TradeOrderNumberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class TradeOrderNumberService {

    private final TradeOrderNumberRepository repository;

    public TradeOrderNumberService(TradeOrderNumberRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public String nextOrderNo() {
        return nextNumber("CB");
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public String nextNumber(String prefix) {
        if (prefix == null || !prefix.matches("[A-Z]{2,4}")) {
            throw new IllegalArgumentException("Invalid number prefix");
        }
        TradeOrderNumberEntity allocation = new TradeOrderNumberEntity();
        allocation.setAllocatedAt(LocalDateTime.now());
        long sequence = repository.saveAndFlush(allocation).getId();
        return prefix + LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyMMdd"))
            + "-" + String.format("%010d", sequence);
    }
}
