package com.ariete.advancednotes.model

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Entity(tableName = "notes")

@Parcelize
/**
    * The @Parcelize annotation is needed to automatically create the Parcelable class.
    * ---------------------------------------------------------------------------------
    * Аннотация @Parcelize нужна для автоматического создания Parcelable класса.
*/
data class Note(
    /**
        * Data class automatically creates the following functions:
        * equals() and hashCode(),
        * toString(),
        * component() functions for each of the parameters,
        * copy().
        * ---------------------------------------------------------
        * Data class автоматически создает следующие функции:
        * equals() and hashCode(),
        * toString(),
        * component() функции для каждого из параметров,
        * copy().
    */
    @PrimaryKey(autoGenerate = true)
    val id: Int,

    @ColumnInfo(name = "dateTime")
    var dateTime: String? = null,

    @ColumnInfo(name = "noteTitle")
    var noteTitle: String = "",

    @ColumnInfo(name = "noteBody")
    var noteBody: String = "",

    @ColumnInfo(name = "colorOfCircuit")
    var colorOfCircuit: Int? = null,

    @ColumnInfo(name = "imagePath")
    var imagePath: String? = null,

    @ColumnInfo(name = "webLink")
    var webLink: String? = null,
) : Parcelable