package com.novabank.novapay.data.dto;

public final class InitiateTransactionRequestDto {

    private final String type;
    private final String fromAccountId;
    private final String toAccountId;
    private final ExternalRecipientDto toExternal;
    private final long amount;
    private final String note;
    private final TransactionMetaDto meta;

    public InitiateTransactionRequestDto(
            String type,
            String fromAccountId,
            String toAccountId,
            ExternalRecipientDto toExternal,
            long amount,
            String note,
            TransactionMetaDto meta
    ) {
        this.type = type;
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.toExternal = toExternal;
        this.amount = amount;
        this.note = note;
        this.meta = meta;
    }

    public String getType() {
        return type;
    }

    public String getFromAccountId() {
        return fromAccountId;
    }

    public String getToAccountId() {
        return toAccountId;
    }

    public ExternalRecipientDto getToExternal() {
        return toExternal;
    }

    public long getAmount() {
        return amount;
    }

    public String getNote() {
        return note;
    }

    public TransactionMetaDto getMeta() {
        return meta;
    }
}
