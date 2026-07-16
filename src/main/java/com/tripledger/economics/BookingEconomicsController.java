package com.tripledger.economics;

import com.tripledger.identity.ActorContext;
import com.tripledger.identity.ActorContextResolver;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bookings")
public class BookingEconomicsController {

    private final ActorContextResolver actorContextResolver;
    private final BookingEconomicsService bookingEconomicsService;
    private final BookingEconomicsExplanationService bookingEconomicsExplanationService;

    public BookingEconomicsController(ActorContextResolver actorContextResolver,
                                      BookingEconomicsService bookingEconomicsService,
                                      BookingEconomicsExplanationService bookingEconomicsExplanationService) {
        this.actorContextResolver = actorContextResolver;
        this.bookingEconomicsService = bookingEconomicsService;
        this.bookingEconomicsExplanationService = bookingEconomicsExplanationService;
    }

    @GetMapping("/{bookingId}/economics")
    public BookingEconomicsDetail calculate(@PathVariable UUID bookingId, HttpServletRequest servletRequest) {
        ActorContext actor = actorContextResolver.resolve(servletRequest);
        return bookingEconomicsService.calculate(actor, bookingId);
    }

    @GetMapping("/{bookingId}/economics/explanation")
    public BookingEconomicsExplanationDetail explain(@PathVariable UUID bookingId,
                                                     HttpServletRequest servletRequest) {
        ActorContext actor = actorContextResolver.resolve(servletRequest);
        return bookingEconomicsExplanationService.explain(actor, bookingId);
    }
}
