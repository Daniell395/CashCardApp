package com.example.cashcard;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

interface CashCardRepository extends CrudRepository <CashCard,Long>,
        PagingAndSortingRepository<CashCard, Long> {


    @RestController
    @RequestMapping("/cashcards")
    class CashCardController {
        private final CashCardRepository cashCardRepository;

        private CashCardController(CashCardRepository cashCardRepository) {
            this.cashCardRepository = cashCardRepository;
        }
    }
}
