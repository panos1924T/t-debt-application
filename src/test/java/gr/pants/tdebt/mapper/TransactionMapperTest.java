package gr.pants.tdebt.mapper;

import gr.pants.tdebt.core.enums.TransactionAction;
import gr.pants.tdebt.dto.transactions_dto.TransactionInsertDTO;
import gr.pants.tdebt.dto.transactions_dto.TransactionReadOnlyDTO;
import gr.pants.tdebt.model.Debt;
import gr.pants.tdebt.model.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// No @ExtendWith(MockitoExtension.class), no @Mock anywhere in this class.
// TransactionMapper has zero dependencies (no repository, no other bean) - so
// there is nothing to fake. We just call it directly with real objects.
class TransactionMapperTest {

    private final TransactionMapper transactionMapper = new TransactionMapper();

    private Debt debt;

    @BeforeEach
    void setUp() {
        debt = new Debt();
        debt.setDebtorName("Maria Papadopoulou");
    }

    @Nested
    @DisplayName("toReadOnlyDTO")
    class ToReadOnlyDTOTests {

        @Test
        @DisplayName("should map all fields correctly when transaction is NOT a correction")
        void toReadOnlyDTO_shouldMapAllFields_whenNotACorrection() {
            // GIVEN
            UUID transactionUuid = UUID.randomUUID();
            Instant createdAt = Instant.parse("2026-07-01T10:15:30Z");

            Transaction transaction = new Transaction();
            transaction.setUuid(transactionUuid);
            transaction.setDate(LocalDate.of(2026, 7, 1));
            transaction.setAmount(BigDecimal.valueOf(150));
            transaction.setAction(TransactionAction.INCREASE);
            transaction.setNote("Initial loan");
            transaction.setDebt(debt);
            transaction.setCreatedAt(createdAt);
            // correctedTransaction is deliberately left null

            // WHEN
            TransactionReadOnlyDTO dto = transactionMapper.toReadOnlyDTO(transaction);

            // THEN
            assertThat(dto.uuid()).isEqualTo(transactionUuid.toString());
            assertThat(dto.date()).isEqualTo(LocalDate.of(2026, 7, 1));
            assertThat(dto.amount()).isEqualByComparingTo(BigDecimal.valueOf(150));
            assertThat(dto.action()).isEqualTo("INCREASE");
            assertThat(dto.note()).isEqualTo("Initial loan");
            assertThat(dto.debtorName()).isEqualTo("Maria Papadopoulou");
            assertThat(dto.createdAt()).isEqualTo(createdAt);
            // The one branch this mapper actually has to get right:
            assertThat(dto.correctedTransactionUuid()).isNull();
        }

        @Test
        @DisplayName("should include correctedTransactionUuid when transaction IS a correction")
        void toReadOnlyDTO_shouldIncludeCorrectedTransactionUuid_whenIsACorrection() {
            // GIVEN
            UUID originalUuid = UUID.randomUUID();
            Transaction original = new Transaction();
            original.setUuid(originalUuid);

            UUID correctionUuid = UUID.randomUUID();
            Transaction correction = new Transaction();
            correction.setUuid(correctionUuid);
            correction.setDate(LocalDate.of(2026, 7, 3));
            correction.setAmount(BigDecimal.valueOf(50));
            correction.setAction(TransactionAction.INCREASE);
            correction.setNote("Correction note");
            correction.setDebt(debt);
            correction.setCorrectedTransaction(original);

            // WHEN
            TransactionReadOnlyDTO dto = transactionMapper.toReadOnlyDTO(correction);

            // THEN: this is the one conditional line in the mapper -
            // it must resolve to the ORIGINAL's uuid, not the correction's own.
            assertThat(dto.correctedTransactionUuid()).isEqualTo(originalUuid.toString());
            assertThat(dto.uuid()).isEqualTo(correctionUuid.toString());
        }
    }

    @Nested
    @DisplayName("toEntity")
    class ToEntityTests {

        @Test
        @DisplayName("should map all InsertDTO fields onto a new Transaction")
        void toEntity_shouldMapAllFields() {
            // GIVEN
            TransactionInsertDTO dto = new TransactionInsertDTO(
                    LocalDate.of(2026, 7, 1),
                    BigDecimal.valueOf(100),
                    TransactionAction.DECREASE,
                    "test note"
            );

            // WHEN
            Transaction transaction = transactionMapper.toEntity(dto);

            // THEN
            assertThat(transaction.getDate()).isEqualTo(dto.date());
            assertThat(transaction.getAmount()).isEqualByComparingTo(dto.amount());
            assertThat(transaction.getAction()).isEqualTo(dto.action());
            assertThat(transaction.getNote()).isEqualTo(dto.note());
            // toEntity() must NOT set debt or corrections - that's the service's job.
            assertThat(transaction.getDebt()).isNull();
            assertThat(transaction.getCorrectedTransaction()).isNull();
        }
    }
}
