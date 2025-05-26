package ru.anb.passwordapp.features.ui.interest

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import ru.anb.passwordapp.R
import ru.anb.passwordapp.features.ui.interest.model.Interest

class InterestsAdapter(
    private val interestList: ArrayList<Interest>
) : RecyclerView.Adapter<InterestsAdapter.InterestsViewHolder>() {

    private var onItemClickListener: OnItemClickListener? = null

    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        onItemClickListener = listener
    }

    inner class InterestsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.interest_imageView)
        val imageClicked: ImageView = itemView.findViewById(R.id.interest_clicked)
        val textView: TextView = itemView.findViewById(R.id.interest_name)

        init {
            itemView.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val interest = interestList[position]
                    // Переключаем состояние выбранности
                    interest.mInterestClicked = !interest.mInterestClicked
                    // Обновляем конкретный элемент
                    notifyItemChanged(position)
                    // Вызываем внешнего слушателя (если нужно)
                    onItemClickListener?.onItemClick(position)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InterestsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.interest_cardview, parent, false)
        return InterestsViewHolder(view)
    }

    override fun onBindViewHolder(holder: InterestsViewHolder, position: Int) {
        val currentInterest = interestList[position]
        Glide.with(holder.itemView.context)
            .load(currentInterest.mImageLink)
            .centerCrop()
            .placeholder(R.drawable.profile_icon)
            .into(holder.imageView)
        holder.textView.text = currentInterest.getLocalizedName()

        if (currentInterest.mInterestClicked) {
            holder.imageClicked.visibility = View.VISIBLE
        } else {
            holder.imageClicked.visibility = View.INVISIBLE
        }
    }

    override fun getItemCount(): Int = interestList.size

    fun getSectionTitle(position: Int): String {
        return interestList[position].mInterestName?.firstOrNull()?.uppercase() ?: "#"
    }
}