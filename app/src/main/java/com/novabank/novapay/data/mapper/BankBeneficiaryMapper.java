package com.novabank.novapay.data.mapper;

import com.google.firebase.firestore.DocumentSnapshot;
import com.novabank.novapay.domain.model.bank.Bank;
import com.novabank.novapay.domain.model.bank.Beneficiary;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Mapper cho collections banks/{bankCode} và users/{uid}/beneficiaries/{id}.
 */
public final class BankBeneficiaryMapper {

    private BankBeneficiaryMapper() {
    }

    public static Bank bankFromDocument(DocumentSnapshot snapshot) {
        if (snapshot == null || !snapshot.exists()) {
            return null;
        }
        return bankFromMap(snapshot.getId(), snapshot.getData());
    }

    public static Bank bankFromMap(String bankCode, Map<String, Object> data) {
        if (bankCode == null || data == null) {
            return null;
        }
        return new Bank(
                bankCode,
                FirestoreMapUtils.toStringOrEmpty(data.get("name")),
                FirestoreMapUtils.toStringOrEmpty(data.get("logoUrl")),
                FirestoreMapUtils.toStringOrEmpty(data.get("napasCode"))
        );
    }

    public static Beneficiary beneficiaryFromDocument(DocumentSnapshot snapshot) {
        if (snapshot == null || !snapshot.exists()) {
            return null;
        }
        return beneficiaryFromMap(snapshot.getId(), snapshot.getData());
    }

    public static Beneficiary beneficiaryFromMap(String id, Map<String, Object> data) {
        if (id == null || data == null) {
            return null;
        }
        String name = FirestoreMapUtils.toStringOrEmpty(data.get("name"));
        String accountNumber = FirestoreMapUtils.toStringOrEmpty(data.get("accountNumber"));
        if (name.isEmpty() || accountNumber.isEmpty()) {
            return null;
        }
        return new Beneficiary(
                id,
                name,
                accountNumber,
                FirestoreMapUtils.toStringOrEmpty(data.get("bankCode")),
                FirestoreMapUtils.toStringOrEmpty(data.get("alias"))
        );
    }

    public static Map<String, Object> beneficiaryToMap(Beneficiary beneficiary) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", beneficiary.getName());
        map.put("accountNumber", beneficiary.getAccountNumber());
        map.put("bankCode", beneficiary.getBankCode());
        map.put("alias", beneficiary.getAlias());
        return map;
    }
}
