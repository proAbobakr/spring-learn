package com.locationapp.service.service;

import com.locationapp.service.event.CommentEvent;
import com.locationapp.service.event.LocationEvent;
import com.locationapp.service.event.RatingEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Event Publisher Service
 *
 * Publishes domain events to Kafka topics for asynchronous processing.
 * Similar to broadcasting intents in Android.
 */
@Service
@Slf4j
public class EventPublisher {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    private static final String LOCATION_EVENTS_TOPIC = "location-events";
    private static final String RATING_EVENTS_TOPIC = "rating-events";
    private static final String COMMENT_EVENTS_TOPIC = "comment-events";

    /**
     * Publish location event
     */
    @Async
    public void publishLocationEvent(LocationEvent event) {
        try {
            CompletableFuture<SendResult<String, Object>> future =
                    kafkaTemplate.send(LOCATION_EVENTS_TOPIC, event.getLocationId().toString(), event);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Location event published: {} for location: {}",
                            event.getEventType(), event.getLocationId());
                } else {
                    log.error("Failed to publish location event: {}", ex.getMessage());
                }
            });
        } catch (Exception e) {
            log.error("Error publishing location event: {}", e.getMessage());
        }
    }

    /**
     * Publish rating event
     */
    @Async
    public void publishRatingEvent(RatingEvent event) {
        try {
            CompletableFuture<SendResult<String, Object>> future =
                    kafkaTemplate.send(RATING_EVENTS_TOPIC, event.getRatingId().toString(), event);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Rating event published: {} for rating: {}",
                            event.getEventType(), event.getRatingId());
                } else {
                    log.error("Failed to publish rating event: {}", ex.getMessage());
                }
            });
        } catch (Exception e) {
            log.error("Error publishing rating event: {}", e.getMessage());
        }
    }

    /**
     * Publish comment event
     */
    @Async
    public void publishCommentEvent(CommentEvent event) {
        try {
            CompletableFuture<SendResult<String, Object>> future =
                    kafkaTemplate.send(COMMENT_EVENTS_TOPIC, event.getCommentId().toString(), event);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Comment event published: {} for comment: {}",
                            event.getEventType(), event.getCommentId());
                } else {
                    log.error("Failed to publish comment event: {}", ex.getMessage());
                }
            });
        } catch (Exception e) {
            log.error("Error publishing comment event: {}", e.getMessage());
        }
    }
}
