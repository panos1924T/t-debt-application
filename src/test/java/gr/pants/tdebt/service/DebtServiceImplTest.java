package gr.pants.tdebt.service;

import gr.pants.tdebt.core.enums.DebtStatus;
import gr.pants.tdebt.core.enums.DebtType;
import gr.pants.tdebt.core.exceptions.DebtHasTransactionsException;
import gr.pants.tdebt.core.exceptions.EntityNotFoundException;
import gr.pants.tdebt.core.exceptions.InvalidArgumentException;
import gr.pants.tdebt.core.filters.DebtFilters;
import gr.pants.tdebt.dto.debt_dto.DebtInsertDTO;
import gr.pants.tdebt.dto.debt_dto.DebtReadOnlyDTO;
import gr.pants.tdebt.dto.debt_dto.DebtUpdateDTO;
import gr.pants.tdebt.mapper.DebtMapper;
import gr.pants.tdebt.model.Debt;
import gr.pants.tdebt.model.User;
import gr.pants.tdebt.repository.DebtRepository;
import gr.pants.tdebt.repository.TransactionRepository;
import gr.pants.tdebt.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DebtServiceImplTest {

    @Mock
    private DebtRepository debtRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private DebtMapper debtMapper;

    @InjectMocks
    private DebtServiceImpl debtService;

    private UUID debtUuid;
    private UUID userUuid;

    @BeforeEach
    void setUp() {
        debtUuid = UUID.randomUUID();
        userUuid = UUID.randomUUID();
    }

    // ---- Factory helper: builds a Debt fixture with sensible defaults,
    // ---- overridden per-test only where the scenario needs it.
    private Debt debtFixture(DebtStatus status) {
        Debt debt = new Debt();
        debt.setUuid(debtUuid);
        debt.setStatus(status);
        debt.setBalance(BigDecimal.valueOf(200));
        debt.setDebtorName("Existing Debtor");
        return debt;
    }

    @Nested
    @DisplayName("saveDebt")
    class SaveDebtTests {

        @Test
        @DisplayName("should create debt with OPEN status and ZERO balance when user exists")
        void saveDebt_shouldCreateDebtWithOpenStatusAndZeroBalance_whenUserExists() {
            // GIVEN
            DebtInsertDTO dto = new DebtInsertDTO("Nikos Papas", DebtType.I_OWE, "Loan for car repair");
            User user = new User();

            // This mirrors what the REAL debtMapper.toEntity(dto) would produce -
            // debtorName/type/description set, but NOT user/status/balance, since
            // those three are the service's own responsibility, not the mapper's.
            Debt mapped = new Debt();
            mapped.setDebtorName(dto.debtorName());
            mapped.setType(dto.debtType());
            mapped.setDescription(dto.description());

            DebtReadOnlyDTO expected = mock(DebtReadOnlyDTO.class);

            when(userRepository.findUserByUuidAndDeletedFalse(userUuid)).thenReturn(Optional.of(user));
            when(debtMapper.toEntity(dto)).thenReturn(mapped);
            when(debtRepository.save(any(Debt.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(debtMapper.toReadOnlyDTO(any(Debt.class))).thenReturn(expected);

            // WHEN
            DebtReadOnlyDTO result = debtService.saveDebt(dto, userUuid);

            // THEN: the three fields the SERVICE itself is responsible for setting.
            assertThat(mapped.getUser()).isEqualTo(user);
            assertThat(mapped.getStatus()).isEqualTo(DebtStatus.OPEN);
            assertThat(mapped.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result).isEqualTo(expected);
            verify(debtRepository).save(mapped);
        }

        @Test
        @DisplayName("should throw EntityNotFoundException when user does not exist")
        void saveDebt_shouldThrowEntityNotFoundException_whenUserNotFound() {
            // GIVEN
            DebtInsertDTO dto = new DebtInsertDTO("Nikos Papas", DebtType.I_OWE, "Loan for car repair");

            when(userRepository.findUserByUuidAndDeletedFalse(userUuid)).thenReturn(Optional.empty());

            // WHEN / THEN
            assertThatThrownBy(() -> debtService.saveDebt(dto, userUuid))
                    .isInstanceOf(EntityNotFoundException.class);

            // Nothing should be mapped or saved once the user lookup fails.
            verifyNoInteractions(debtMapper);
            verifyNoInteractions(debtRepository);
        }
    }

    @Nested
    @DisplayName("updateDebt")
    class UpdateDebtTests {

        @Test
        @DisplayName("should update debtorName and description when debt is open")
        void updateDebt_shouldUpdateDebtorNameAndDescription_whenDebtIsOpen() {
            // GIVEN
            Debt debt = debtFixture(DebtStatus.OPEN);
            DebtUpdateDTO dto = new DebtUpdateDTO("New Name", "New description");
            DebtReadOnlyDTO expected = mock(DebtReadOnlyDTO.class);

            when(debtRepository.findByUuidAndUser_UuidAndDeletedFalse(debtUuid, userUuid))
                    .thenReturn(Optional.of(debt));
            when(debtRepository.save(debt)).thenReturn(debt);
            when(debtMapper.toReadOnlyDTO(debt)).thenReturn(expected);

            // WHEN
            DebtReadOnlyDTO result = debtService.updateDebt(dto, debtUuid, userUuid);

            // THEN
            assertThat(debt.getDebtorName()).isEqualTo("New Name");
            assertThat(debt.getDescription()).isEqualTo("New description");
            assertThat(result).isEqualTo(expected);
            verify(debtRepository).save(debt);
        }

        @Test
        @DisplayName("should throw InvalidArgumentException when debt is archived")
        void updateDebt_shouldThrowInvalidArgumentException_whenDebtIsArchived() {
            // GIVEN
            Debt debt = debtFixture(DebtStatus.ARCHIVED);
            DebtUpdateDTO dto = new DebtUpdateDTO("New Name", "New description");

            when(debtRepository.findByUuidAndUser_UuidAndDeletedFalse(debtUuid, userUuid))
                    .thenReturn(Optional.of(debt));

            // WHEN / THEN
            assertThatThrownBy(() -> debtService.updateDebt(dto, debtUuid, userUuid))
                    .isInstanceOf(InvalidArgumentException.class)
                    .hasMessageContaining("archived");

            verify(debtRepository, never()).save(any());
            verifyNoInteractions(debtMapper);
        }

        @Test
        @DisplayName("should throw EntityNotFoundException when debt does not exist or is not owned by user")
        void updateDebt_shouldThrowEntityNotFoundException_whenDebtNotFound() {
            // GIVEN
            DebtUpdateDTO dto = new DebtUpdateDTO("New Name", "New description");

            when(debtRepository.findByUuidAndUser_UuidAndDeletedFalse(debtUuid, userUuid))
                    .thenReturn(Optional.empty());

            // WHEN / THEN
            assertThatThrownBy(() -> debtService.updateDebt(dto, debtUuid, userUuid))
                    .isInstanceOf(EntityNotFoundException.class);

            verifyNoInteractions(debtMapper);
        }
    }

    @Nested
    @DisplayName("deleteDebt")
    class DeleteDebtTests {

        @Test
        @DisplayName("should soft delete debt when it has no existing transactions")
        void deleteDebt_shouldSoftDeleteDebt_whenNoTransactionsExist() {
            // GIVEN
            Debt debt = debtFixture(DebtStatus.OPEN);
            DebtReadOnlyDTO expected = mock(DebtReadOnlyDTO.class);

            when(debtRepository.findByUuidAndUser_UuidAndDeletedFalse(debtUuid, userUuid))
                    .thenReturn(Optional.of(debt));
            when(transactionRepository.existsByDebt_Uuid(debtUuid)).thenReturn(false);
            when(debtRepository.save(debt)).thenReturn(debt);
            when(debtMapper.toReadOnlyDTO(debt)).thenReturn(expected);

            // WHEN
            DebtReadOnlyDTO result = debtService.deleteDebt(debtUuid, userUuid);

            // THEN: the entity's own soft-delete flag flipped to true.
            assertThat(debt.isDeleted()).isTrue();
            assertThat(result).isEqualTo(expected);
            verify(debtRepository).save(debt);
        }

        @Test
        @DisplayName("should throw DebtHasTransactionsException when debt has existing transactions")
        void deleteDebt_shouldThrowDebtHasTransactionsException_whenTransactionsExist() {
            // GIVEN
            Debt debt = debtFixture(DebtStatus.OPEN);

            when(debtRepository.findByUuidAndUser_UuidAndDeletedFalse(debtUuid, userUuid))
                    .thenReturn(Optional.of(debt));
            when(transactionRepository.existsByDebt_Uuid(debtUuid)).thenReturn(true);

            // WHEN / THEN
            assertThatThrownBy(() -> debtService.deleteDebt(debtUuid, userUuid))
                    .isInstanceOf(DebtHasTransactionsException.class);

            verify(debtRepository, never()).save(any());
            verifyNoInteractions(debtMapper);
        }

        @Test
        @DisplayName("should throw EntityNotFoundException when debt does not exist or is not owned by user")
        void deleteDebt_shouldThrowEntityNotFoundException_whenDebtNotFound() {
            // GIVEN
            when(debtRepository.findByUuidAndUser_UuidAndDeletedFalse(debtUuid, userUuid))
                    .thenReturn(Optional.empty());

            // WHEN / THEN
            assertThatThrownBy(() -> debtService.deleteDebt(debtUuid, userUuid))
                    .isInstanceOf(EntityNotFoundException.class);

            verifyNoInteractions(transactionRepository);
        }
    }

    @Nested
    @DisplayName("toggleStatus")
    class ToggleStatusTests {

        @Test
        @DisplayName("should archive an open debt")
        void toggleStatus_shouldArchiveDebt_whenCurrentStatusIsOpen() {
            // GIVEN
            Debt debt = debtFixture(DebtStatus.OPEN);
            DebtReadOnlyDTO expected = mock(DebtReadOnlyDTO.class);

            when(debtRepository.findByUuidAndUser_UuidAndDeletedFalse(debtUuid, userUuid))
                    .thenReturn(Optional.of(debt));
            when(debtRepository.save(debt)).thenReturn(debt);
            when(debtMapper.toReadOnlyDTO(debt)).thenReturn(expected);

            // WHEN
            DebtReadOnlyDTO result = debtService.toggleStatus(debtUuid, userUuid);

            // THEN
            assertThat(debt.getStatus()).isEqualTo(DebtStatus.ARCHIVED);
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("should reopen an archived debt")
        void toggleStatus_shouldReopenDebt_whenCurrentStatusIsArchived() {
            // GIVEN
            Debt debt = debtFixture(DebtStatus.ARCHIVED);
            DebtReadOnlyDTO expected = mock(DebtReadOnlyDTO.class);

            when(debtRepository.findByUuidAndUser_UuidAndDeletedFalse(debtUuid, userUuid))
                    .thenReturn(Optional.of(debt));
            when(debtRepository.save(debt)).thenReturn(debt);
            when(debtMapper.toReadOnlyDTO(debt)).thenReturn(expected);

            // WHEN
            DebtReadOnlyDTO result = debtService.toggleStatus(debtUuid, userUuid);

            // THEN
            assertThat(debt.getStatus()).isEqualTo(DebtStatus.OPEN);
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("should throw EntityNotFoundException when debt does not exist or is not owned by user")
        void toggleStatus_shouldThrowEntityNotFoundException_whenDebtNotFound() {
            // GIVEN
            when(debtRepository.findByUuidAndUser_UuidAndDeletedFalse(debtUuid, userUuid))
                    .thenReturn(Optional.empty());

            // WHEN / THEN
            assertThatThrownBy(() -> debtService.toggleStatus(debtUuid, userUuid))
                    .isInstanceOf(EntityNotFoundException.class);

            verify(debtRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getDebtByUuid")
    class GetDebtByUuidTests {

        @Test
        @DisplayName("should return debt when owned by user")
        void getDebtByUuid_shouldReturnDebt_whenOwnedByUser() {
            // GIVEN
            Debt debt = debtFixture(DebtStatus.OPEN);
            DebtReadOnlyDTO expected = mock(DebtReadOnlyDTO.class);

            when(debtRepository.findByUuidAndUser_UuidAndDeletedFalse(debtUuid, userUuid))
                    .thenReturn(Optional.of(debt));
            when(debtMapper.toReadOnlyDTO(debt)).thenReturn(expected);

            // WHEN
            DebtReadOnlyDTO result = debtService.getDebtByUuid(debtUuid, userUuid);

            // THEN
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("should throw EntityNotFoundException when debt does not exist or is not owned by user")
        void getDebtByUuid_shouldThrowEntityNotFoundException_whenNotFoundOrNotOwned() {
            // GIVEN
            when(debtRepository.findByUuidAndUser_UuidAndDeletedFalse(debtUuid, userUuid))
                    .thenReturn(Optional.empty());

            // WHEN / THEN
            assertThatThrownBy(() -> debtService.getDebtByUuid(debtUuid, userUuid))
                    .isInstanceOf(EntityNotFoundException.class);

            verifyNoInteractions(debtMapper);
        }
    }

    @Nested
    @DisplayName("getFilteredPaginatedDebts")
    class GetFilteredPaginatedDebtsTests {

        @Test
        @DisplayName("should return mapped page of the user's debts")
        @SuppressWarnings("unchecked")
        void getFilteredPaginatedDebts_shouldReturnMappedPage() {
            // GIVEN: unlike transaction pagination, there is no parent-ownership
            // lookup here - scoping by userUuid happens inside DebtSpecification.build().
            DebtFilters filters = mock(DebtFilters.class);
            Pageable pageable = PageRequest.of(0, 10);

            Debt d1 = debtFixture(DebtStatus.OPEN);
            Page<Debt> debtPage = new PageImpl<>(List.of(d1), pageable, 1);

            when(debtRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(debtPage);
            when(debtMapper.toReadOnlyDTO(any(Debt.class))).thenReturn(mock(DebtReadOnlyDTO.class));

            // WHEN
            Page<DebtReadOnlyDTO> result = debtService.getFilteredPaginatedDebts(userUuid, filters, pageable);

            // THEN
            assertThat(result.getTotalElements()).isEqualTo(1);
            verify(debtRepository).findAll(any(Specification.class), eq(pageable));
        }
    }
}
