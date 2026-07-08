package gr.pants.tdebt.model;

import gr.pants.tdebt.core.enums.TransactionAction;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Table(name = "transactions")
public class Transaction extends AbstractAuditEntity{

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "debt_id", nullable = false, updatable = false)
    private Debt debt;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionAction action;

    private String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "corrected_transaction_id", updatable = false)
    private Transaction correctedTransaction;
}
