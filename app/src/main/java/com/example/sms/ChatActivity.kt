package com.example.sms

import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class ChatActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var messageList: MutableList<Message>
    private lateinit var databaseReference: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var editMessage: EditText
    private lateinit var buttonSend: ImageButton
    private val dateFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())

    private lateinit var currentUserId: String
    private lateinit var chatPartnerId: String
    private lateinit var currentUsername: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        // Initialize Firebase Authentication
        auth = FirebaseAuth.getInstance()
        currentUserId = auth.currentUser?.uid ?: ""
        currentUsername = intent.getStringExtra("username") ?: "Unknown"
        chatPartnerId = intent.getStringExtra("chatPartnerId") ?: "RECEIVER_USER_ID"

        databaseReference = FirebaseDatabase.getInstance().getReference("chats")

        recyclerView = findViewById(R.id.recyclerView)
        editMessage = findViewById(R.id.edit_message)
        buttonSend = findViewById(R.id.button_send)

        recyclerView.layoutManager = LinearLayoutManager(this)
        messageList = mutableListOf()
        messageAdapter = MessageAdapter(messageList, currentUserId, chatPartnerId)
        recyclerView.adapter = messageAdapter

        loadMessages()

        buttonSend.setOnClickListener {
            val messageText = editMessage.text.toString().trim()
            if (messageText.isNotEmpty()) {
                sendMessage(messageText)
            }
        }
    }

    private fun sendMessage(text: String) {
        val messageId = databaseReference.push().key ?: return
        val timestamp = System.currentTimeMillis()

        val message = Message(
            id = messageId,
            senderId = currentUserId,
            receiverId = chatPartnerId,
            text = text,
            timestamp = timestamp
        )

        val updates = hashMapOf<String, Any>(
            "/$currentUserId/$chatPartnerId/$messageId" to message,
            "/$chatPartnerId/$currentUserId/$messageId" to message
        )

        databaseReference.updateChildren(updates)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("ChatActivity", "Message sent successfully.")
                } else {
                    Log.e("ChatActivity", "Failed to send message: ${task.exception}")
                }
            }

        editMessage.text.clear()
        recyclerView.scrollToPosition(messageList.size - 1)
    }

    private fun loadMessages() {
        val chatPath = databaseReference.child(currentUserId).child(chatPartnerId)
        chatPath.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val message = snapshot.getValue(Message::class.java)
                message?.let {
                    if (!messageList.contains(it)) {
                        messageList.add(it)
                        messageAdapter.notifyItemInserted(messageList.size - 1)
                        recyclerView.scrollToPosition(messageList.size - 1)
                    }
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                // Optional: handle if a message is edited
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                // Optional: handle if a message is deleted
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}

            override fun onCancelled(error: DatabaseError) {
                Log.e("ChatActivity", "Failed to load messages: ${error.message}")
            }
        })
    }
}
