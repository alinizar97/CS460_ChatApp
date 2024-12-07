package com.example.chatappcs460;

/**
 * Represents a message in a chat conversation.
 * Contains the sender's ID, the message text, and a timestamp.
 */
public class Message {

    private String senderId;
    private String message;
    private long timestamp;

    /**
     * Default constructor for Firestore deserialization.
     */
    public Message() {
        // No-argument constructor required for Firestore
    }

    /**
     * Constructor to create a message.
     *
     * @param senderId  The ID of the user who sent the message.
     * @param message   The content of the message.
     * @param timestamp The timestamp when the message was sent.
     */
    public Message(String senderId, String message, long timestamp) {
        this.senderId = senderId;
        this.message = message;
        this.timestamp = timestamp;
    }

    /**
     * @return The sender ID of the message.
     */
    public String getSenderId() {
        return senderId;
    }

    /**
     * @return The message content.
     */
    public String getMessage() {
        return message;
    }

    /**
     * @return The timestamp of the message.
     */
    public long getTimestamp() {
        return timestamp;
    }
}
