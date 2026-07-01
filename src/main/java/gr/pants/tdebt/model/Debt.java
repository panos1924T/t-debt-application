package gr.pants.tdebt.model;

import gr.pants.tdebt.core.DebtStatus;
import gr.pants.tdebt.core.DebtType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Table(name = "debts")
public class Debt extends AbstractEntity{

    @Column(nullable = false, unique = true)
    private String debtorName;

    @Enumerated(EnumType.STRING)
    @Column(name = "debt_type", nullable = false)
    private DebtType type;

    @Column(precision = 19, scale = 4)
    private BigDecimal balance;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "debt_status", nullable = false)
    private DebtStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;
}
