package gr.pants.tdebt.repository;

import gr.pants.tdebt.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
}
