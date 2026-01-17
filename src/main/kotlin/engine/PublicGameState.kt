package engine

import model.CardIdentity
import model.Color
import model.GameAction
import model.Rank

data class PublicGameState(
    val myPlayerId: Int,
    val myHandSize: Int,
    val otherHands: Map<Int, List<CardIdentity>>,
    val discardPile: List<CardIdentity>,
    val board: Map<Color, Rank?>,
    val clueTokens: Int,
    val fuseTokens: Int,
    val deckSize: Int,
    val actionHistory: List<GameAction>
)
