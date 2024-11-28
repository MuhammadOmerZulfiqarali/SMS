package com.example.sms

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class MessageAdapter(
    private val listOfChat: MutableList<Message>,  // MutableList to allow list modifications
    private val currentUserId: String,
    private val chatPartnerId: String
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val dateFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_USER_CHAT_ME -> {
                Log.d(TAG, "Inflating sent message layout")
                SentMessageViewHolder(layoutInflater.inflate(R.layout.item_chat_me, parent, false))
            }
            VIEW_TYPE_USER_CHAT_OTHER -> {
                Log.d(TAG, "Inflating received message layout")
                ReceivedMessageViewHolder(layoutInflater.inflate(R.layout.item_chat_other, parent, false))
            }
            else -> {
                Log.w(TAG, "Unknown view type. Defaulting to sent message layout")
                SentMessageViewHolder(layoutInflater.inflate(R.layout.item_chat_me, parent, false))
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = listOfChat[position]
        when (holder) {
            is SentMessageViewHolder -> holder.bind(message)
            is ReceivedMessageViewHolder -> holder.bind(message)
            else -> Log.e(TAG, "Unknown ViewHolder type for position $position")
        }
    }

    override fun getItemViewType(position: Int): Int {
        val message = listOfChat[position]

        // Ensure that currentUserId is correctly set for the current user session
        return when {
            message.senderId == currentUserId -> {
                Log.d(TAG, "Using USER_CHAT_ME for message sent by current user")
                VIEW_TYPE_USER_CHAT_ME // Message sent by the current user
            }
            message.receiverId == chatPartnerId -> {
                Log.d(TAG, "Using USER_CHAT_OTHER for message received by current user")
                VIEW_TYPE_USER_CHAT_OTHER // Message received by the current user
            }
            else -> {
                Log.w(TAG, "Unexpected match; defaulting to USER_CHAT_ME")
                VIEW_TYPE_USER_CHAT_ME // Default fallback (optional)
            }
        }
    }


    override fun getItemCount(): Int = listOfChat.size

    inner class SentMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textMessage: TextView = itemView.findViewById(R.id.text_message_me)
        private val textTimestamp: TextView = itemView.findViewById(R.id.text_timestamp_me)

        fun bind(message: Message) {
            textMessage.text = message.text
            textTimestamp.text = dateFormatter.format(Date(message.timestamp))
            Log.d(TAG, "Bound sent message: ${message.text}")
        }
    }

    inner class ReceivedMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textMessage: TextView = itemView.findViewById(R.id.text_message_other)
        private val textTimestamp: TextView = itemView.findViewById(R.id.text_timestamp_other)

        fun bind(message: Message) {
            textMessage.text = message.text
            textTimestamp.text = dateFormatter.format(Date(message.timestamp))
            Log.d(TAG, "Bound received message: ${message.text}")
        }
    }

    companion object {
        private const val VIEW_TYPE_USER_CHAT_ME = 2
        private const val VIEW_TYPE_USER_CHAT_OTHER = 1
        private const val TAG = "MessageAdapter"
    }
}
