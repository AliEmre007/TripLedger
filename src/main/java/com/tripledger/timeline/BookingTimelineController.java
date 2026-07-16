package com.tripledger.timeline;

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
public class BookingTimelineController {

    private final ActorContextResolver actorContextResolver;
    private final BookingTimelineService bookingTimelineService;

    public BookingTimelineController(ActorContextResolver actorContextResolver,
                                     BookingTimelineService bookingTimelineService) {
        this.actorContextResolver = actorContextResolver;
        this.bookingTimelineService = bookingTimelineService;
    }

    @GetMapping("/{bookingId}/timeline")
    public BookingTimeline get(@PathVariable UUID bookingId, HttpServletRequest servletRequest) {
        ActorContext actor = actorContextResolver.resolve(servletRequest);
        return bookingTimelineService.get(actor, bookingId);
    }
}
