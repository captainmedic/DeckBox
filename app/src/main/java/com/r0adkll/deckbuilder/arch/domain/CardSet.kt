package com.r0adkll.deckbuilder.arch.domain


import paperparcel.PaperParcel
import paperparcel.PaperParcelable


@PaperParcel
data class CardSet(
        val code: String,
        val ptcgoCode: String,
        val name: String,
        val series: String,
        val totalCards: Int,
        val standardLegal: Boolean,
        val expandedLegal: Boolean,
        val releaseDate: String,
        val symbolUrl: String
) : PaperParcelable {
    companion object {
        @JvmField val CREATOR = PaperParcelCardSet.CREATOR
    }
}