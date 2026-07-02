package gr.pants.tdebt.repository;

import gr.pants.tdebt.model.Debt;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DebtRepository extends JpaRepository<Debt, Long> {
}
