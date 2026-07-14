package com.tripledger.booking;

import com.tripledger.ingestion.SourceRecord;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record BookingItemDetail(
        UUID id,
        String itemExternalId,
        BookingItemServiceType serviceType,
        LocalDate serviceStartDate,
        LocalDate serviceEndDate,
        BigDecimal sellingAmount,
        String sellingCurrency,
        BookingItemState state,
        SourceRecordDetail sourceRecord
) {

    static BookingItemDetail from(BookingItem item, SourceRecord sourceRecord) {
        return new BookingItemDetail(
                item.id(),
                item.itemExternalId(),
                item.serviceType(),
                item.serviceStartDate(),
                item.serviceEndDate(),
                item.sellingAmount(),
                item.sellingCurrency(),
                item.state(),
                SourceRecordDetail.from(sourceRecord)
        );
    }
}
