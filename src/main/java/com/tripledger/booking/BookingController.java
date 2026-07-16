package com.tripledger.booking;

import com.tripledger.identity.ActorContext;
import com.tripledger.identity.ActorContextResolver;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bookings")
public class BookingController {

    private final ActorContextResolver actorContextResolver;
    private final BookingDetailService bookingDetailService;
    private final BookingListService bookingListService;

    public BookingController(ActorContextResolver actorContextResolver,
                             BookingDetailService bookingDetailService,
                             BookingListService bookingListService) {
        this.actorContextResolver = actorContextResolver;
        this.bookingDetailService = bookingDetailService;
        this.bookingListService = bookingListService;
    }

    @GetMapping
    public List<BookingSummary> list(HttpServletRequest servletRequest) {
        ActorContext actor = actorContextResolver.resolve(servletRequest);
        return bookingListService.list(actor);
    }

    @GetMapping("/{bookingId}")
    public BookingDetail get(@PathVariable UUID bookingId, HttpServletRequest servletRequest) {
        ActorContext actor = actorContextResolver.resolve(servletRequest);
        return bookingDetailService.get(actor, bookingId);
    }
}
