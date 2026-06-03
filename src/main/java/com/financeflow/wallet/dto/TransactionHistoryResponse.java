package com.financeflow.wallet.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class TransactionHistoryResponse {
    List<TransactionHistoryItem> items;
    int page;
    int size;
    long totalElements;
    int totalPages;
}
