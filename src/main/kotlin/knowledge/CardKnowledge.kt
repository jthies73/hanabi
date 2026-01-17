package knowledge

import model.Color
import model.Rank

data class CardKnowledge(
    val possibleColors: Set<Color> = Color.values().toSet(),
    val possibleRanks: Set<Rank> = (1..5).map { Rank(it) }.toSet(),
    val isClued: Boolean = false,
    val positiveColorClues: Set<Color> = emptySet(),
    val positiveRankClues: Set<Rank> = emptySet()
) {
    fun isCertain(): Boolean = possibleColors.size == 1 && possibleRanks.size == 1
    
    fun getCertainColor(): Color? = if (possibleColors.size == 1) possibleColors.first() else null
    
    fun getCertainRank(): Rank? = if (possibleRanks.size == 1) possibleRanks.first() else null
}
