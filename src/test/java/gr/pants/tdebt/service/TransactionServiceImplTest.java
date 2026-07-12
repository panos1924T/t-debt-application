package gr.pants.tdebt.service;

import gr.pants.tdebt.core.enums.DebtStatus;
import gr.pants.tdebt.core.enums.TransactionAction;
import gr.pants.tdebt.core.exceptions.EntityNotFoundException;
import gr.pants.tdebt.core.exceptions.InvalidArgumentException;
import gr.pants.tdebt.core.filters.TransactionFilters;
import gr.pants.tdebt.dto.transactions_dto.TransactionInsertDTO;
import gr.pants.tdebt.dto.transactions_dto.TransactionReadOnlyDTO;
import gr.pants.tdebt.dto.transactions_dto.TransactionUpdateDTO;
import gr.pants.tdebt.mapper.TransactionMapper;
import gr.pants.tdebt.model.Debt;
import gr.pants.tdebt.model.Transaction;
import gr.pants.tdebt.repository.DebtRepository;
import gr.pants.tdebt.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private DebtRepository debtRepository;

    @Mock
    private TransactionMapper transactionMapper;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    // Shared fixtures, available to this class AND every @Nested class below,
    // since @BeforeEach on the outer class runs before every test, including nested ones.
    private UUID debtUuid;
    private UUID userUuid;
    private Debt debt;

    @BeforeEach
    void setUp() {
        debtUuid = UUID.randomUUID();
        userUuid = UUID.randomUUID();

        debt = new Debt();
        debt.setUuid(debtUuid);
        debt.setStatus(DebtStatus.OPEN);
        debt.setBalance(BigDecimal.valueOf(500));
    }

    // ---- Factory helpers: keep test bodies focused on the scenario, not on
    // ---- boilerplate object construction. Change a field here once, every
    // ---- test that uses it stays correct.

    private TransactionInsertDTO insertDTO(BigDecimal amount, TransactionAction action) {
        return new TransactionInsertDTO(LocalDate.of(2026, 7, 1), amount, action, "note");
    }

    private TransactionUpdateDTO updateDTO(LocalDate date, BigDecimal amount, TransactionAction action, String note) {
        return new TransactionUpdateDTO(date, amount, action, note);
    }

    private Transaction existingTransaction(Long id, UUID uuid, BigDecimal amount, TransactionAction action) {
        Transaction transaction = new Transaction();
        transaction.setId(id);
        transaction.setUuid(uuid);
        transaction.setDate(LocalDate.of(2026, 7, 1));
        transaction.setAmount(amount);
        transaction.setAction(action);
        transaction.setDebt(debt);
        return transaction;
    }

    @Nested
    @DisplayName("saveTransaction")
    class SaveTransactionTests {

        @Test
        @DisplayName("should increase balance when action is INCREASE")
        void saveTransaction_shouldIncreaseBalance_whenActionIsIncrease() {
            // GIVEN
            TransactionInsertDTO dto = insertDTO(BigDecimal.valueOf(100), TransactionAction.INCREASE);
            Transaction mapped = existingTransaction(null, null, dto.amount(), dto.action());
            TransactionReadOnlyDTO expected = mock(TransactionReadOnlyDTO.class);

            when(debtRepository.findByUuidAndUser_UuidAndDeletedFalse(debtUuid, userUuid))
                    .thenReturn(Optional.of(debt));
            when(transactionMapper.toEntity(dto)).thenReturn(mapped);
            when(transactionRepository.save(any(Transaction.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(transactionMapper.toReadOnlyDTO(any(Transaction.class))).thenReturn(expected);

            // WHEN
            TransactionReadOnlyDTO result = transactionService.saveTransaction(debtUuid, dto, userUuid);

            // THEN
            assertThat(debt.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(600));
            assertThat(result).isEqualTo(expected);
            verify(debtRepository).save(debt);
            verify(transactionRepository).save(mapped);
        }

        @Test
        @DisplayName("should decrease balance, even into negative, when action is DECREASE")
        void saveTransaction_shouldAllowNegativeBalance_whenDecreaseExceedsBalance() {
            // GIVEN: balance is 500, we withdraw 700 -> intentionally allowed to go negative
            TransactionInsertDTO dto = insertDTO(BigDecimal.valueOf(700), TransactionAction.DECREASE);
            Transaction mapped = existingTransaction(null, null, dto.amount(), dto.action());

            when(debtRepository.findByUuidAndUser_UuidAndDeletedFalse(debtUuid, userUuid))
                    .thenReturn(Optional.of(debt));
            when(transactionMapper.toEntity(dto)).thenReturn(mapped);
            when(transactionRepository.save(any(Transaction.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(transactionMapper.toReadOnlyDTO(any(Transaction.class))).thenReturn(mock(TransactionReadOnlyDTO.class));

            // WHEN
            transactionService.saveTransaction(debtUuid, dto, userUuid);

            // THEN: no exception, no guard - balance simply goes negative
            assertThat(debt.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(-200));
        }

        @Test
        @DisplayName("should throw InvalidArgumentException when debt is archived")
        void saveTransaction_shouldThrowInvalidArgumentException_whenDebtIsArchived() {
            // GIVEN
            debt.setStatus(DebtStatus.ARCHIVED);
            TransactionInsertDTO dto = insertDTO(BigDecimal.valueOf(100), TransactionAction.INCREASE);

            when(debtRepository.findByUuidAndUser_UuidAndDeletedFalse(debtUuid, userUuid))
                    .thenReturn(Optional.of(debt));

            // WHEN / THEN
            assertThatThrownBy(() -> transactionService.saveTransaction(debtUuid, dto, userUuid))
                    .isInstanceOf(InvalidArgumentException.class)
                    .hasMessageContaining("archived");

            verifyNoInteractions(transactionRepository);
            verify(debtRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw EntityNotFoundException when debt does not exist or is not owned by user")
        void saveTransaction_shouldThrowEntityNotFoundException_whenDebtNotFound() {
            // GIVEN
            TransactionInsertDTO dto = insertDTO(BigDecimal.valueOf(100), TransactionAction.INCREASE);

            when(debtRepository.findByUuidAndUser_UuidAndDeletedFalse(debtUuid, userUuid))
                    .thenReturn(Optional.empty());

            // WHEN / THEN
            assertThatThrownBy(() -> transactionService.saveTransaction(debtUuid, dto, userUuid))
                    .isInstanceOf(EntityNotFoundException.class);

            verifyNoInteractions(transactionRepository);
            verifyNoInteractions(transactionMapper);
        }
    }

    @Nested
    @DisplayName("updateTransaction")
    class UpdateTransactionTests {

        @Test
        @DisplayName("should update metadata only, in place, when delta is zero")
        void updateTransaction_shouldUpdateMetadataOnly_whenDeltaIsZero() {
            // GIVEN: same amount and action as the original -> no financial change
            UUID transUuid = UUID.randomUUID();
            Transaction original = existingTransaction(1L, transUuid, BigDecimal.valueOf(100), TransactionAction.INCREASE);
            TransactionUpdateDTO dto = updateDTO(
                    LocalDate.of(2026, 7, 5), BigDecimal.valueOf(100), TransactionAction.INCREASE, "corrected note only");

            when(debtRepository.findByUuidAndUser_UuidAndDeletedFalse(debtUuid, userUuid))
                    .thenReturn(Optional.of(debt));
            when(transactionRepository.findByUuidAndDebt_Uuid(transUuid, debtUuid))
                    .thenReturn(Optional.of(original));
            when(transactionRepository.save(original)).thenReturn(original);
            when(transactionMapper.toReadOnlyDTO(original)).thenReturn(mock(TransactionReadOnlyDTO.class));

            // WHEN
            transactionService.updateTransaction(debtUuid, transUuid, dto, userUuid);

            // THEN: original record itself was updated and saved...
            assertThat(original.getNote()).isEqualTo("corrected note only");
            assertThat(original.getDate()).isEqualTo(LocalDate.of(2026, 7, 5));
            verify(transactionRepository).save(original);

            // ...and crucially, NO new record was created, and balance was untouched.
            assertThat(debt.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(500));
            verify(debtRepository, never()).save(any());
            // We never even need to ask "has this been corrected before" on the metadata-only path.
            verify(transactionRepository, never()).existsByCorrectedTransaction_Id(any());
        }

        @Test
        @DisplayName("should create a new correction record when delta is non-zero")
        void updateTransaction_shouldCreateCorrection_whenDeltaIsNonZero() {
            // GIVEN: original was 100 INCREASE, updated to 150 INCREASE -> delta = +50
            UUID transUuid = UUID.randomUUID();
            Transaction original = existingTransaction(1L, transUuid, BigDecimal.valueOf(100), TransactionAction.INCREASE);
            TransactionUpdateDTO dto = updateDTO(
                    LocalDate.of(2026, 7, 5), BigDecimal.valueOf(150), TransactionAction.INCREASE, "correction note");

            when(debtRepository.findByUuidAndUser_UuidAndDeletedFalse(debtUuid, userUuid))
                    .thenReturn(Optional.of(debt));
            when(transactionRepository.findByUuidAndDebt_Uuid(transUuid, debtUuid))
                    .thenReturn(Optional.of(original));
            when(transactionRepository.existsByCorrectedTransaction_Id(1L)).thenReturn(false);
            when(transactionRepository.save(any(Transaction.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(transactionMapper.toReadOnlyDTO(any(Transaction.class))).thenReturn(mock(TransactionReadOnlyDTO.class));

            // WHEN
            transactionService.updateTransaction(debtUuid, transUuid, dto, userUuid);

            // THEN: capture exactly what was passed to save(), so we can inspect
            // the correction record's own fields - not just "something was saved".
            ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
            verify(transactionRepository).save(captor.capture());
            Transaction correction = captor.getValue();

            assertThat(correction.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(50));
            assertThat(correction.getAction()).isEqualTo(TransactionAction.INCREASE);
            assertThat(correction.getCorrectedTransaction()).isEqualTo(original);

            // Balance moved by the delta (500 + 50 = 550), not by the full new amount.
            assertThat(debt.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(550));
            verify(debtRepository).save(debt);
        }

        @Test
        @DisplayName("should throw InvalidArgumentException when correcting an already-corrected transaction")
        void updateTransaction_shouldThrowInvalidArgumentException_whenAlreadyCorrected() {
            // GIVEN: original has already been corrected once before (anti-chaining)
            UUID transUuid = UUID.randomUUID();
            Transaction original = existingTransaction(1L, transUuid, BigDecimal.valueOf(100), TransactionAction.INCREASE);
            TransactionUpdateDTO dto = updateDTO(
                    LocalDate.of(2026, 7, 5), BigDecimal.valueOf(200), TransactionAction.INCREASE, "trying to chain");

            when(debtRepository.findByUuidAndUser_UuidAndDeletedFalse(debtUuid, userUuid))
                    .thenReturn(Optional.of(debt));
            when(transactionRepository.findByUuidAndDebt_Uuid(transUuid, debtUuid))
                    .thenReturn(Optional.of(original));
            when(transactionRepository.existsByCorrectedTransaction_Id(1L)).thenReturn(true);

            // WHEN / THEN
            assertThatThrownBy(() -> transactionService.updateTransaction(debtUuid, transUuid, dto, userUuid))
                    .isInstanceOf(InvalidArgumentException.class)
                    .hasMessageContaining("already been corrected");

            // Nothing should be persisted once the anti-chaining guard trips.
            verify(transactionRepository, never()).save(any());
            verify(debtRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw InvalidArgumentException when debt is archived")
        void updateTransaction_shouldThrowInvalidArgumentException_whenDebtIsArchived() {
            // GIVEN
            debt.setStatus(DebtStatus.ARCHIVED);
            UUID transUuid = UUID.randomUUID();
            TransactionUpdateDTO dto = updateDTO(
                    LocalDate.of(2026, 7, 5), BigDecimal.valueOf(100), TransactionAction.INCREASE, "note");

            when(debtRepository.findByUuidAndUser_UuidAndDeletedFalse(debtUuid, userUuid))
                    .thenReturn(Optional.of(debt));

            // WHEN / THEN
            assertThatThrownBy(() -> transactionService.updateTransaction(debtUuid, transUuid, dto, userUuid))
                    .isInstanceOf(InvalidArgumentException.class)
                    .hasMessageContaining("archived");

            // We should fail fast on the archived check, before ever looking up the transaction.
            verifyNoInteractions(transactionRepository);
        }

        @Test
        @DisplayName("should throw EntityNotFoundException when original transaction does not exist on this debt")
        void updateTransaction_shouldThrowEntityNotFoundException_whenOriginalTransactionNotFound() {
            // GIVEN
            UUID transUuid = UUID.randomUUID();
            TransactionUpdateDTO dto = updateDTO(
                    LocalDate.of(2026, 7, 5), BigDecimal.valueOf(100), TransactionAction.INCREASE, "note");

            when(debtRepository.findByUuidAndUser_UuidAndDeletedFalse(debtUuid, userUuid))
                    .thenReturn(Optional.of(debt));
            when(transactionRepository.findByUuidAndDebt_Uuid(transUuid, debtUuid))
                    .thenReturn(Optional.empty());

            // WHEN / THEN
            assertThatThrownBy(() -> transactionService.updateTransaction(debtUuid, transUuid, dto, userUuid))
                    .isInstanceOf(EntityNotFoundException.class);

            verify(transactionRepository, never()).save(any());
            verify(debtRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getTransactionByUuid")
    class GetTransactionByUuidTests {

        @Test
        @DisplayName("should return transaction when it belongs to the requesting user")
        void getTransactionByUuid_shouldReturnTransaction_whenOwnedByUser() {
            // GIVEN
            UUID transUuid = UUID.randomUUID();
            Transaction transaction = existingTransaction(1L, transUuid, BigDecimal.valueOf(100), TransactionAction.INCREASE);
            TransactionReadOnlyDTO expected = mock(TransactionReadOnlyDTO.class);

            when(transactionRepository.findTransactionByUuidAndDebt_User_UuidAndDebt_DeletedFalse(transUuid, userUuid))
                    .thenReturn(Optional.of(transaction));
            when(transactionMapper.toReadOnlyDTO(transaction)).thenReturn(expected);

            // WHEN
            TransactionReadOnlyDTO result = transactionService.getTransactionByUuid(transUuid, userUuid);

            // THEN
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("should throw EntityNotFoundException when transaction does not exist or is not owned by user")
        void getTransactionByUuid_shouldThrowEntityNotFoundException_whenNotFoundOrNotOwned() {
            // GIVEN: this single repository method covers BOTH "doesn't exist" and
            // "belongs to someone else" - from the caller's perspective they must
            // be indistinguishable, to avoid leaking existence of other users' data (IDOR).
            UUID transUuid = UUID.randomUUID();

            when(transactionRepository.findTransactionByUuidAndDebt_User_UuidAndDebt_DeletedFalse(transUuid, userUuid))
                    .thenReturn(Optional.empty());

            // WHEN / THEN
            assertThatThrownBy(() -> transactionService.getTransactionByUuid(transUuid, userUuid))
                    .isInstanceOf(EntityNotFoundException.class);

            verifyNoInteractions(transactionMapper);
        }
    }

    @Nested
    @DisplayName("getPaginatedFilteredDebtTransactions")
    class GetPaginatedFilteredDebtTransactionsTests {

        @Test
        @DisplayName("should return mapped page when debt is owned by user")
        @SuppressWarnings("unchecked")
        void getPaginatedFilteredDebtTransactions_shouldReturnMappedPage_whenDebtIsOwned() {
            // GIVEN
            // TransactionFilters isn't stubbed with real field values here - the
            // service only forwards it into the (already-tested-elsewhere)
            // TransactionSpecification builder, it doesn't branch on it directly.
            TransactionFilters filters = mock(TransactionFilters.class);
            Pageable pageable = PageRequest.of(0, 10);

            Transaction t1 = existingTransaction(1L, UUID.randomUUID(), BigDecimal.valueOf(100), TransactionAction.INCREASE);
            Transaction t2 = existingTransaction(2L, UUID.randomUUID(), BigDecimal.valueOf(50), TransactionAction.DECREASE);
            Page<Transaction> transactionPage = new PageImpl<>(List.of(t1, t2), pageable, 2);

            when(debtRepository.findByUuidAndUser_UuidAndDeletedFalse(debtUuid, userUuid))
                    .thenReturn(Optional.of(debt));
            when(transactionRepository.findAll(any(Specification.class), eq(pageable)))
                    .thenReturn(transactionPage);
            when(transactionMapper.toReadOnlyDTO(any(Transaction.class))).thenReturn(mock(TransactionReadOnlyDTO.class));

            // WHEN
            Page<TransactionReadOnlyDTO> result = transactionService
                    .getPaginatedFilteredDebtTransactions(debtUuid, filters, pageable, userUuid);

            // THEN
            assertThat(result.getTotalElements()).isEqualTo(2);
            verify(transactionRepository).findAll(any(Specification.class), eq(pageable));
        }

        @Test
        @DisplayName("should throw EntityNotFoundException when debt is not owned by user")
        void getPaginatedFilteredDebtTransactions_shouldThrowEntityNotFoundException_whenDebtNotOwned() {
            // GIVEN
            TransactionFilters filters = mock(TransactionFilters.class);
            Pageable pageable = PageRequest.of(0, 10);

            when(debtRepository.findByUuidAndUser_UuidAndDeletedFalse(debtUuid, userUuid))
                    .thenReturn(Optional.empty());

            // WHEN / THEN
            assertThatThrownBy(() -> transactionService
                    .getPaginatedFilteredDebtTransactions(debtUuid, filters, pageable, userUuid))
                    .isInstanceOf(EntityNotFoundException.class);

            // The ownership check must short-circuit before any query is built or run.
            verifyNoInteractions(transactionRepository);
        }
    }

    @Nested
    @DisplayName("getPaginatedTransactions")
    class GetPaginatedTransactionsTests {

        @Test
        @DisplayName("should return mapped page of all of the user's transactions across debts")
        void getPaginatedTransactions_shouldReturnMappedPage() {
            // GIVEN
            Pageable pageable = PageRequest.of(0, 10);
            Transaction t1 = existingTransaction(1L, UUID.randomUUID(), BigDecimal.valueOf(100), TransactionAction.INCREASE);
            Page<Transaction> transactionPage = new PageImpl<>(List.of(t1), pageable, 1);

            when(transactionRepository.findAllByDebtUserUuid(userUuid, pageable)).thenReturn(transactionPage);
            when(transactionMapper.toReadOnlyDTO(any(Transaction.class))).thenReturn(mock(TransactionReadOnlyDTO.class));

            // WHEN
            Page<TransactionReadOnlyDTO> result = transactionService.getPaginatedTransactions(pageable, userUuid);

            // THEN
            assertThat(result.getTotalElements()).isEqualTo(1);
            verify(transactionRepository).findAllByDebtUserUuid(userUuid, pageable);
            // This method never touches DebtRepository at all - it's a global,
            // cross-debt query scoped only by user uuid.
            verifyNoInteractions(debtRepository);
        }
    }
}
