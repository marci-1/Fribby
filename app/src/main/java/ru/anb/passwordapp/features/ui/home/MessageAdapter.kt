package ru.anb.passwordapp.features.ui.home

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.squareup.picasso.Picasso
import ru.anb.passwordapp.R

class MessageAdapter(private val userMessagesList: List<Messages>) :
    RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    private val mAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private lateinit var userRef: DatabaseReference

    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val senderMessageText: TextView = itemView.findViewById(R.id.sender_message_text)
        val receiverMessageText: TextView = itemView.findViewById(R.id.receiver_message_text)
        val messageSenderPicture: ImageView = itemView.findViewById(R.id.message_sender_image_view)
        val messageReceiverPicture: ImageView = itemView.findViewById(R.id.message_receiver_image_view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.custom_messages_layout, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val messageSenderId = mAuth.currentUser?.uid
        val messages = userMessagesList[position]

        val fromUserId = messages.from
        val fromMessageType = messages.type

        if (fromUserId != null) {
            userRef = FirebaseDatabase.getInstance().getReference("Users").child(fromUserId)
        }

        holder.receiverMessageText.visibility = View.GONE
        holder.senderMessageText.visibility = View.GONE
        holder.messageSenderPicture.visibility = View.GONE
        holder.messageReceiverPicture.visibility = View.GONE

        when (fromMessageType) {
            "text" -> {
                if (fromUserId == messageSenderId) {
                    holder.senderMessageText.apply {
                        visibility = View.VISIBLE
                        setBackgroundResource(R.drawable.sender_messages_layout)
                        setTextColor(Color.BLACK)
                        text = messages.message
                    }
                } else {
                    holder.receiverMessageText.apply {
                        visibility = View.VISIBLE
                        setBackgroundResource(R.drawable.receiver_messages_layout)
                        setTextColor(Color.BLACK)
                        text = messages.message
                    }
                }
            }

            "image" -> {
                val imageUrl = messages.message
                if (fromUserId == messageSenderId) {
                    holder.messageSenderPicture.visibility = View.VISIBLE
                    Picasso.get().load(imageUrl).into(holder.messageSenderPicture)
                } else {
                    holder.messageReceiverPicture.visibility = View.VISIBLE
                    Picasso.get().load(imageUrl).into(holder.messageReceiverPicture)
                }
            }
        }
    }

    override fun getItemCount(): Int = userMessagesList.size
}