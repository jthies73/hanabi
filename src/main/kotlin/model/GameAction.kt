package model

sealed interface GameAction {
    val actorPlayerId: Int
}

data class PlayCard(
    override val actorPlayerId: Int,
    val cardIndex: Int
) : GameAction

data class DiscardCard(
    override val actorPlayerId: Int,
    val cardIndex: Int
) : GameAction

sealed interface GiveHint : GameAction {
    val targetPlayerId: Int
}

data class HintColor(
    override val actorPlayerId: Int,
    override val targetPlayerId: Int,
    val color: Color
) : GiveHint

data class HintRank(
    override val actorPlayerId: Int,
    override val targetPlayerId: Int,
    val rank: Rank
) : GiveHint
