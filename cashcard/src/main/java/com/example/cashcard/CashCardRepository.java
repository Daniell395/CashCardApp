package com.example.cashcard;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

interface CashCardRepository extends CrudRepository <CashCard,Long>,
        PagingAndSortingRepository<CashCard, Long> {

    CashCard findByIdAndOwner(Long id, String owner);
    Page<CashCard> findByOwner(String owner, PageRequest pageRequest);

    boolean existsByIdAndOwner(Long id, String name);

    @RestController
    @RequestMapping("/cashcards")
    class CashCardController {
        private final CashCardRepository cashCardRepository;

        private CashCardController(CashCardRepository cashCardRepository) {
            this.cashCardRepository = cashCardRepository;
        }
    }
}
