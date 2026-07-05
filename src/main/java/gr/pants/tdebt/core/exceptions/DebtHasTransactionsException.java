package gr.pants.tdebt.core.exceptions;

public class DebtHasTransactionsException extends AppGenericException {

    private static final String DEFAULT_CODE = "HasTransactions";

    public DebtHasTransactionsException(String errorCode, String message) {
        super(errorCode + DEFAULT_CODE, message);
    }
}
