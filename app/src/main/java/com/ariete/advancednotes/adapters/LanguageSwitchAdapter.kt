package com.ariete.advancednotes.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ariete.advancednotes.R

data class LanguageCard(
    val title: String,
    val hint: String,
    val code: String,
    var isChecked: Boolean
)

interface OnLanguageSelectedListener {
    fun onLanguageSelected(selectedLanguage: LanguageCard)
}

class LanguageSwitchAdapter(
    private val language_cards: MutableList<LanguageCard>,
    private val listener: OnLanguageSelectedListener
) : RecyclerView.Adapter<LanguageSwitchAdapter.LanguageSwitchViewHolder>() {

    inner class LanguageSwitchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val lang_name: TextView = itemView.findViewById(R.id.languageCardText)
        val hint_text: TextView = itemView.findViewById(R.id.languageCardTextHint)
        val isChecked: RadioButton = itemView.findViewById(R.id.languageSwitchButton)
    }

    private var selectedPosition = language_cards.indexOfFirst { it.isChecked }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): LanguageSwitchViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(
                R.layout.layout_language_card,
                parent,
                false
            )
        return LanguageSwitchViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: LanguageSwitchViewHolder,
        @SuppressLint("RecyclerView") position: Int
    ) {
        val currentLanguageCard = language_cards[position]

        holder.lang_name.text = currentLanguageCard.title
        holder.hint_text.text = currentLanguageCard.hint
        holder.isChecked.isChecked = position == selectedPosition

        holder.itemView.setOnClickListener {
            if (position != selectedPosition) {
                val previousSelectedPosition = selectedPosition
                selectedPosition = position

                notifyItemChanged(previousSelectedPosition)
                notifyItemChanged(selectedPosition)

                listener.onLanguageSelected(currentLanguageCard)
            }
        }

        holder.isChecked.isClickable = false
        holder.isChecked.isFocusable = false
    }

    override fun getItemCount(): Int = language_cards.size
}