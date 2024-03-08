package com.joshlong.mogul.api.notifications;

import org.springframework.util.Assert;

import java.util.Date;

/**
 * describes an event - any event - that should be communicated to a particular, logged-in, user.
 *
 * @param when     when the message was published. <EM>Not</EM> when (or if) the message has been routed, delivered, or read.
 * @param key      some extra information that, when paired with the category, tells us a thing and tell us to which other thing the message applies; e.g.: a podcast-episode and an ID.
 * @param category a machine-friendly string that is constant, indicating the nature of the notification, e.g.: <code>podcast-episode-completed</code>.
 *                 It is expected that the client would use this category to lookup internationalized messages for each notification
 * @param context  any extra context associated with the notification. <code>null</code> or <code>""</coded> if unneeded.
 *                 if this is a system-wide message that needs to be delivered, for which there is
 *                 no internationalized text, then this could be a human-readable message sent in whatever language is required.
 * @param mogulId  the current authenticated user for whom this context is intended or <code>NULL</code> if this is meant to be a system wide notification
 *                 intended for all users logged into the system
 */
public record NotificationEvent(Long mogulId, String category, String key, Date when, String context) {

    public static String categoryFromClassName(Class<?> clazz) {
        Assert.notNull(clazz, "the class must be non-null");
        var clazzName = clazz.getSimpleName();
        Assert.hasText(clazzName, "the class name must have text");
        var buffer = new StringBuilder();
        for (var c : clazzName.toCharArray()) {
            if (Character.isUpperCase(c)) {
                buffer
                        .append("-")
                        .append(Character.toLowerCase(c));
            }//
            else {
                buffer.append(c);
            }
        }
        return buffer.toString();
    }
}
