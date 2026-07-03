package gr.pants.tdebt.mapper;

import gr.pants.tdebt.dto.debt_dto.DebtInsertDTO;
import gr.pants.tdebt.dto.debt_dto.DebtReadOnlyDTO;
import gr.pants.tdebt.model.Debt;
import org.springframework.stereotype.Component;

@Component
public class DebtMapper {

    public DebtReadOnlyDTO toReadOnlyDTO(Debt debt) {
        return new DebtReadOnlyDTO(
                debt.getUuid().toString(),
                debt.getDebtorName(),
                debt.getType(),
                debt.getBalance(),
                debt.getDescription(),
                debt.getStatus(),
                debt.getCreatedAt(),
                debt.getUpdatedAt()
        );
    }

    public Debt toEntity(DebtInsertDTO insertDTO) {
        Debt debt = new Debt();
        debt.setDebtorName(insertDTO.debtorName());
        debt.setType(insertDTO.debtType());
        debt.setBalance(insertDTO.balance());
        debt.setDescription(insertDTO.description());

        return debt;
    }
}
