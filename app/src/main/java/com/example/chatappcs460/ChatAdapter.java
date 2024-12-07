package com.example.chatappcs460;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Adapter class for managing chat messages in a RecyclerView.
 * Handles the display of sent and received messages.
 */
public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.MessageViewHolder> {

    private final List<Message> messageList;
    private final String currentUserId;

    /**
     * Constructor for ChatAdapter.
     *
     * @param messageList   The list of messages to display.
     * @param currentUserId The ID of the current user (used to distinguish sent and received messages).
     */
    public ChatAdapter(List<Message> messageList, String currentUserId) {
        this.messageList = messageList;
        this.currentUserId = currentUserId;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message, parent, false); // Inflates the custom message layout.
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messageList.get(position);

        // Check if the message was sent by the current user
        if (message.getSenderId().equals(currentUserId)) {
            holder.tvMessageSent.setText(message.getMessage());
            holder.tvMessageSent.setVisibility(View.VISIBLE);
            holder.tvMessageReceived.setVisibility(View.GONE);
        } else {
            holder.tvMessageReceived.setText(message.getMessage());
            holder.tvMessageReceived.setVisibility(View.VISIBLE);
            holder.tvMessageSent.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size(); // Returns the number of messages in the list.
    }

    /**
     * ViewHolder class for holding the UI elements of a chat message.
     */
    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessageSent, tvMessageReceived;

        /**
         * Constructor for MessageViewHolder.
         *
         * @param itemView The view for the message item.
         */
        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessageSent = itemView.findViewById(R.id.tvMessageSent); // Sent message TextView.
            tvMessageReceived = itemView.findViewById(R.id.tvMessageReceived); // Received message TextView.
        }
    }
}
