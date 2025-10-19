package com.ariete.advancednotes.adapters

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.ariete.advancednotes.R
import com.ariete.advancednotes.databinding.ItemNoteContainerBinding
import com.bumptech.glide.Glide
import com.ariete.advancednotes.model.Note

class NoteAdapter
constructor(
    private val context: Context,
) : RecyclerView.Adapter<NoteAdapter.NoteViewHolder>() {

    private companion object {
        const val textNoteMaxLength = 128
    }

    class NoteViewHolder(val itemBinding: ItemNoteContainerBinding) :
        RecyclerView.ViewHolder(itemBinding.root)

    /**
        * DiffUtil.ItemCallback - class,
        * responsible for calculating the difference between two lists.
        * -------------------------------------------------------------
        * DiffUtil.ItemCallback - класс,
        * ответственный за вычисление разницы между двумя списками.
     */
    private val differCallback =
        object : DiffUtil.ItemCallback<Note>() {
            override fun areItemsTheSame(
                oldItem: Note,
                newItem: Note
            ): Boolean {
                /**
                    * We compare identificators of elements.
                    * --------------------------------------
                    * Сравниваем идентификаторы элементов.
                */
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(
                oldItem: Note,
                newItem: Note
            ): Boolean {
                /**
                    * We compare contents of elements.
                    * --------------------------------
                    * Сравниваем содержимое элементов.
                */
                return oldItem == newItem
            }
        }

    /**
        * AsyncListDiffer - it is a class,
        * which support a list
        * with auto updating
        * after changing.
        * --------------------------------
        * AsyncListDiffer - это класс,
        * который поддерживает список
        * с автоматическим обновлением
        * после изменения.
     */
    val differ = AsyncListDiffer(
        this,
        differCallback
    )

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): NoteViewHolder {
        return NoteViewHolder(
            ItemNoteContainerBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(
        holder: NoteViewHolder,
        position: Int
    ) {
        val currentNote = differ.currentList[position]

        holder.itemBinding.textTitle.text = currentNote.noteTitle
        holder.itemBinding.dateTime.text = currentNote.dateTime

        currentNote.imagePath?.let {
            Glide.with(context)
                .load(it)
                .into(holder.itemBinding.imageNote)

            holder.itemBinding.imageNote.visibility = View.VISIBLE
        } ?: run {
            if (currentNote.noteBody.isNotEmpty()) {
                check_text_note_length(currentNote.noteBody).let {
                    holder.itemBinding.noteText.text = it

                    holder.itemBinding.noteText.visibility = View.VISIBLE
                }
            }
        }

        holder.itemBinding
            .indicator
            .setBackgroundColor(currentNote.colorOfCircuit!!)

        holder.itemView.setOnClickListener { mView ->
            val bundle = Bundle()

            bundle.putString("message", "change after save")
            bundle.putParcelable("note", currentNote)

            mView.findNavController().navigate(
                R.id.action_homeFragment_to_noteFragment,
                bundle
            )
        }
    }

    private fun `check_text_note_length`(text: String): String {
        return if (text.length <= textNoteMaxLength) {
            text
        } else {
            text.substring(0, textNoteMaxLength) + "..."
        }
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }
}