package gr.pants.tdebt.mapper;

import gr.pants.tdebt.dto.transactions_dto.TransactionInsertDTO;
import gr.pants.tdebt.dto.transactions_dto.TransactionReadOnlyDTO;
import gr.pants.tdebt.model.Transaction;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {

    public TransactionReadOnlyDTO toReadOnlyDTO(Transaction transaction) {
        return new TransactionReadOnlyDTO(
                transaction.getUuid().toString(),
                transaction.getDate(),
                transaction.getAmount(),
                transaction.getAction().toString(),
                transaction.getNote(),
                transaction.getCreatedAt()
        );
    }

    public Transaction toEntity(TransactionInsertDTO insertDTO) {
        Transaction transaction = new Transaction();
        transaction.setDate(insertDTO.date());
        transaction.setAmount(insertDTO.amount());
        transaction.setAction(insertDTO.action());
        transaction.setNote(insertDTO.note());

        return transaction;
    }
}
