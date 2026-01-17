package engine

import model.CardIdentity
import model.Color
import model.GameAction
import model.Rank

data class FullGameState(
    val deck: ArrayDeque<CardIdentity>,
    val hands: Map<Int, List<CardIdentity>>,
    val discardPile: List<CardIdentity>,
    val board: Map<Color, Rank?>,
    val clueTokens: Int,
    val fuseTokens: Int,
    val currentPlayerIndex: Int,
    val turnsRemaining: Int?,
    val gameOver: Boolean,
    val actionHistory: List<GameAction> = emptyList()
)
