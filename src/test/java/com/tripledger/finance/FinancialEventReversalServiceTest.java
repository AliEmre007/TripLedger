package com.tripledger.finance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tripledger.audit.AuditService;
import com.tripledger.authorization.AuthorizationService;
import com.tripledger.authorization.Permission;
import com.tripledger.common.api.ApiException;
import com.tripledger.identity.ActorContext;
import com.tripledger.identity.UserRole;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FinancialEventReversalServiceTest {

    private static final UUID ORGANISATION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID EVENT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID BOOKING_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final Instant NOW = Instant.parse("2026-07-14T06:00:00Z");

    @Mock
    private FinancialEventRepository financialEventRepository;

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private AuditService auditService;

    private final List<FinancialEvent> savedEvents = new ArrayList<>();
    private FinancialEvent original;

    @BeforeEach
    void setUp() {
        original = originalEvent();
        when(financialEventRepository.findByOrganisationIdAndId(ORGANISATION_ID, EVENT_ID))
                .thenReturn(Optional.of(original));
        when(financialEventRepository.existsByOrganisationIdAndReversesEventId(ORGANISATION_ID, EVENT_ID))
                .thenReturn(false);
    }

    @Test
    void createsFullReversalWithoutChangingOriginalEvent() {
        when(financialEventRepository.save(any(FinancialEvent.class))).thenAnswer(invocation -> {
            FinancialEvent event = invocation.getArgument(0);
            savedEvents.add(event);
            return event;
        });

        FinancialEventReversalService.FinancialEventReversalResult result = service().reverse(
                actor(),
                EVENT_ID,
                new FinancialEventReversalService.ReverseFinancialEventCommand(
                        "Gateway corrected duplicate payment.",
                        Instant.parse("2026-07-15T09:00:00Z"),
                        null
                )
        );

        assertThat(savedEvents).hasSize(1);
        FinancialEvent reversal = savedEvents.getFirst();
        assertThat(reversal.eventType()).isEqualTo(FinancialEventType.REVERSAL);
        assertThat(reversal.direction()).isEqualTo(FinancialEventDirection.REVERSAL);
        assertThat(reversal.reversesEventId()).isEqualTo(EVENT_ID);
        assertThat(reversal.amount()).isEqualByComparingTo(original.amount());
        assertThat(reversal.currency()).isEqualTo(original.currency());
        assertThat(reversal.bookingId()).isEqualTo(BOOKING_ID);
        assertThat(reversal.adjustmentReason()).isEqualTo("Gateway corrected duplicate payment.");
        assertThat(result.reversal().reversesEventId()).isEqualTo(EVENT_ID);
        assertThat(result.replacementEvent()).isNull();
        assertThat(original.reversesEventId()).isNull();
        verify(authorizationService).require(actor(), Permission.FINANCIAL_ACTION_WITH_MFA);
        verify(auditService).recordSuccess(
                actor(),
                "FINANCIAL_EVENT_REVERSED",
                AuditService.TARGET_BOOKING,
                BOOKING_ID,
                "financial_event:" + EVENT_ID,
                "financial_event:" + reversal.id(),
                "Gateway corrected duplicate payment."
        );
    }

    @Test
    void canCreateReplacementEventWithReversal() {
        when(financialEventRepository.save(any(FinancialEvent.class))).thenAnswer(invocation -> {
            FinancialEvent event = invocation.getArgument(0);
            savedEvents.add(event);
            return event;
        });

        FinancialEventReversalService.FinancialEventReversalResult result = service().reverse(
                actor(),
                EVENT_ID,
                new FinancialEventReversalService.ReverseFinancialEventCommand(
                        "Corrected amount from gateway.",
                        Instant.parse("2026-07-15T09:00:00Z"),
                        new FinancialEventReversalService.ReplacementFinancialEventCommand(
                                FinancialEventType.CUSTOMER_PAYMENT,
                                new BigDecimal("900.00"),
                                "eur",
                                Instant.parse("2026-07-15T09:05:00Z"),
                                "PAY-1001-CORRECTED"
                        )
                )
        );

        assertThat(savedEvents).hasSize(2);
        FinancialEvent replacement = savedEvents.get(1);
        assertThat(replacement.eventType()).isEqualTo(FinancialEventType.CUSTOMER_PAYMENT);
        assertThat(replacement.direction()).isEqualTo(FinancialEventDirection.INCREASE_RECEIVED);
        assertThat(replacement.reversesEventId()).isNull();
        assertThat(replacement.bookingId()).isEqualTo(BOOKING_ID);
        assertThat(replacement.amount()).isEqualByComparingTo(new BigDecimal("900.00"));
        assertThat(replacement.currency()).isEqualTo("EUR");
        assertThat(result.replacementEvent().externalReference()).isEqualTo("PAY-1001-CORRECTED");
    }

    @Test
    void rejectsSecondReversalForSameOriginalEvent() {
        when(financialEventRepository.existsByOrganisationIdAndReversesEventId(ORGANISATION_ID, EVENT_ID))
                .thenReturn(true);

        assertThatThrownBy(() -> service().reverse(
                actor(),
                EVENT_ID,
                new FinancialEventReversalService.ReverseFinancialEventCommand("Duplicate correction.", null, null)
        ))
                .isInstanceOf(ApiException.class)
                .hasMessage("Financial event already has a reversal.");
    }

    private FinancialEventReversalService service() {
        return new FinancialEventReversalService(
                financialEventRepository,
                authorizationService,
                auditService,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    private FinancialEvent originalEvent() {
        return new FinancialEvent(
                EVENT_ID,
                ORGANISATION_ID,
                UUID.randomUUID(),
                BOOKING_ID,
                FinancialEventType.CUSTOMER_PAYMENT,
                FinancialEventDirection.INCREASE_RECEIVED,
                new BigDecimal("950.00"),
                "EUR",
                Instant.parse("2026-07-02T10:15:00Z"),
                "PAY-1001",
                null,
                null,
                USER_ID,
                NOW
        );
    }

    private ActorContext actor() {
        return new ActorContext(
                USER_ID,
                ORGANISATION_ID,
                "Finance User",
                UserRole.FINANCE,
                true,
                "corr-123"
        );
    }
}
