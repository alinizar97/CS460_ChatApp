package com.example.chatappcs460;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles the chat interface, enabling users to send and receive messages.
 * Manages conversations and integrates with Firestore for real-time messaging.
 */
public class ChatActivity extends AppCompatActivity {
    private EditText etMessage;
    private RecyclerView recyclerViewMessages;
    private Button btnLogout, btnAddChatPartner, btnSend;
    private ChatAdapter chatAdapter;
    private List<Message> messageList;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String currentUserId;
    private String activeConversationId = null;

    /**
     * Initializes the chat activity, setting up UI elements and Firebase components.
     *
     * @param savedInstanceState The saved state of the activity.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Initialize UI components
        etMessage = findViewById(R.id.etMessage);
        recyclerViewMessages = findViewById(R.id.recyclerViewMessages);
        btnLogout = findViewById(R.id.btnLogout);
        btnAddChatPartner = findViewById(R.id.btnAddChatPartner);
        btnSend = findViewById(R.id.btnSend);

        // Style buttons
        btnLogout.setBackgroundColor(Color.parseColor("#FFD83D68"));
        btnAddChatPartner.setBackgroundColor(Color.parseColor("#FFD83D68"));
        btnSend.setBackgroundColor(Color.parseColor("#FFD83D68"));

        // Initialize Firebase components
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        currentUserId = auth.getCurrentUser().getUid();

        // Setup RecyclerView
        messageList = new ArrayList<>();
        chatAdapter = new ChatAdapter(messageList, currentUserId);
        recyclerViewMessages.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewMessages.setAdapter(chatAdapter);

        // Load messages for the active conversation
        loadMessages();

        // Logout button functionality
        btnLogout.setOnClickListener(view -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(ChatActivity.this, AuthActivity.class));
            finish();
        });

        // Add chat partner functionality
        btnAddChatPartner.setOnClickListener(view -> openAddChatPartnerDialog());

        // Send message functionality
        btnSend.setOnClickListener(view -> {
            if (activeConversationId != null) {
                sendMessage(activeConversationId);
            } else {
                Toast.makeText(this, "No active conversation. Add a partner first.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Opens a dialog to add a new chat partner.
     */
    private void openAddChatPartnerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Chat Partner");

        final EditText input = new EditText(this);
        input.setHint("Enter email or username");
        builder.setView(input);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String emailOrUsername = input.getText().toString().trim();
            if (!TextUtils.isEmpty(emailOrUsername)) {
                addChatPartnerToFirestore(emailOrUsername);
            } else {
                Toast.makeText(this, "Field cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    /**
     * Searches Firestore for a user by email or username and initiates a conversation.
     *
     * @param emailOrUsername The email or username to search for.
     */
    private void addChatPartnerToFirestore(String emailOrUsername) {
        Log.d("FirestoreDebug", "Searching for user with email/username: " + emailOrUsername);

        db.collection("users")
                .whereEqualTo("email", emailOrUsername)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        String partnerId = task.getResult().getDocuments().get(0).getId();
                        Log.d("FirestoreDebug", "User found by email with ID: " + partnerId);
                        createConversation(partnerId);
                    } else {
                        db.collection("users")
                                .whereEqualTo("username", emailOrUsername)
                                .get()
                                .addOnCompleteListener(usernameTask -> {
                                    if (usernameTask.isSuccessful() && !usernameTask.getResult().isEmpty()) {
                                        String partnerId = usernameTask.getResult().getDocuments().get(0).getId();
                                        Log.d("FirestoreDebug", "User found by username with ID: " + partnerId);
                                        createConversation(partnerId);
                                    } else {
                                        Toast.makeText(this, "No user found with this email/username", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreDebug", "Error finding user: " + e.getMessage());
                    Toast.makeText(this, "Error finding user: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Creates or retrieves an existing conversation between the current user and a partner.
     *
     * @param partnerId The partner's user ID.
     */
    private void createConversation(String partnerId) {
        db.collection("conversations")
                .whereArrayContains("participants", currentUserId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        boolean conversationFound = false;
                        for (DocumentSnapshot document : task.getResult()) {
                            List<String> participants = (List<String>) document.get("participants");
                            if (participants != null && participants.contains(partnerId)) {
                                setActiveConversation(document.getId());
                                conversationFound = true;
                                Toast.makeText(this, "Conversation already exists", Toast.LENGTH_SHORT).show();
                                break;
                            }
                        }

                        if (!conversationFound) {
                            createNewConversation(partnerId);
                        }
                    } else {
                        Toast.makeText(this, "Error fetching conversations: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Creates a new conversation in Firestore.
     *
     * @param partnerId The partner's user ID.
     */
    private void createNewConversation(String partnerId) {
        Map<String, Object> conversationData = new HashMap<>();
        conversationData.put("participants", Arrays.asList(currentUserId, partnerId));

        db.collection("conversations")
                .add(conversationData)
                .addOnSuccessListener(documentReference -> {
                    setActiveConversation(documentReference.getId());
                    Toast.makeText(this, "New conversation created!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to create conversation: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Sets the active conversation ID and loads messages.
     *
     * @param conversationId The ID of the active conversation.
     */
    private void setActiveConversation(String conversationId) {
        this.activeConversationId = conversationId;
        loadMessages();
    }

    /**
     * Sends a message in the active conversation.
     *
     * @param conversationId The ID of the active conversation.
     */
    public void sendMessage(String conversationId) {
        String message = etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(message)) {
            Toast.makeText(this, "Message cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> messageData = new HashMap<>();
        messageData.put("senderId", currentUserId);
        messageData.put("message", message);
        messageData.put("timestamp", System.currentTimeMillis());

        db.collection("conversations").document(conversationId).collection("messages")
                .add(messageData)
                .addOnSuccessListener(documentReference -> {
                    etMessage.setText("");
                    Toast.makeText(this, "Message sent", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to send message", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Loads messages for the active conversation in real-time.
     */
    private void loadMessages() {
        if (activeConversationId == null) {
            Log.d("FirestoreDebug", "No active conversation set");
            return;
        }

        db.collection("conversations").document(activeConversationId).collection("messages")
                .orderBy("timestamp")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(ChatActivity.this, "Error loading messages", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (value != null) {
                        for (DocumentChange dc : value.getDocumentChanges()) {
                            if (dc.getType() == DocumentChange.Type.ADDED) {
                                Message message = dc.getDocument().toObject(Message.class);
                                messageList.add(message);
                                chatAdapter.notifyItemInserted(messageList.size() - 1);
                                recyclerViewMessages.scrollToPosition(messageList.size() - 1);
                            }
                        }
                    }
                });
    }
}
