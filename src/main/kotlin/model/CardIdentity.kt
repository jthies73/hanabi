package model

import java.util.UUID

data class CardIdentity(
    val color: Color,
    val rank: Rank,
    val id: UUID = UUID.randomUUID()
)
