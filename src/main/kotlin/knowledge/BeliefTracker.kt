package knowledge

import engine.PublicGameState
import model.*

class BeliefTracker(private val myPlayerId: Int, initialHandSize: Int) {
    private var handKnowledge = List(initialHandSize) { CardKnowledge() }

    fun updateFromAction(action: GameAction, publicState: PublicGameState, hintedIndices: List<Int>? = null) {
        when (action) {
            is HintColor -> {
                if (action.targetPlayerId == myPlayerId) {
                    processColorHint(action.color, publicState, hintedIndices)
                }
            }
            is HintRank -> {
                if (action.targetPlayerId == myPlayerId) {
                    processRankHint(action.rank, publicState, hintedIndices)
                }
            }
            is PlayCard -> {
                if (action.actorPlayerId == myPlayerId) {
                    removeCardKnowledge(action.cardIndex)
                }
            }
            is DiscardCard -> {
                if (action.actorPlayerId == myPlayerId) {
                    removeCardKnowledge(action.cardIndex)
                }
            }
        }

        // Update possibilities based on visible cards
        updateFromVisibleCards(publicState)
    }

    private fun processColorHint(color: Color, publicState: PublicGameState, hintedIndices: List<Int>?) {
        val newKnowledge = handKnowledge.toMutableList()
        
        if (hintedIndices != null) {
            // We have explicit information about which cards were hinted
            for (i in handKnowledge.indices) {
                if (i in hintedIndices) {
                    // Positive hint: this card IS this color
                    newKnowledge[i] = handKnowledge[i].copy(
                        possibleColors = setOf(color),
                        isClued = true,
                        positiveColorClues = handKnowledge[i].positiveColorClues + color
                    )
                } else {
                    // Negative hint: this card is NOT this color
                    val newColors = handKnowledge[i].possibleColors - color
                    if (newColors.isNotEmpty()) {
                        newKnowledge[i] = handKnowledge[i].copy(
                            possibleColors = newColors
                        )
                    }
                }
            }
        } else {
            // Fallback: assume all cards that could be this color were hinted
            for (i in handKnowledge.indices) {
                if (handKnowledge[i].possibleColors.contains(color)) {
                    newKnowledge[i] = handKnowledge[i].copy(
                        possibleColors = setOf(color),
                        isClued = true,
                        positiveColorClues = handKnowledge[i].positiveColorClues + color
                    )
                }
            }
        }
        
        handKnowledge = newKnowledge
    }

    private fun processRankHint(rank: Rank, publicState: PublicGameState, hintedIndices: List<Int>?) {
        val newKnowledge = handKnowledge.toMutableList()
        
        if (hintedIndices != null) {
            // We have explicit information about which cards were hinted
            for (i in handKnowledge.indices) {
                if (i in hintedIndices) {
                    // Positive hint: this card IS this rank
                    newKnowledge[i] = handKnowledge[i].copy(
                        possibleRanks = setOf(rank),
                        isClued = true,
                        positiveRankClues = handKnowledge[i].positiveRankClues + rank
                    )
                } else {
                    // Negative hint: this card is NOT this rank
                    val newRanks = handKnowledge[i].possibleRanks - rank
                    if (newRanks.isNotEmpty()) {
                        newKnowledge[i] = handKnowledge[i].copy(
                            possibleRanks = newRanks
                        )
                    }
                }
            }
        } else {
            // Fallback: assume all cards that could be this rank were hinted
            for (i in handKnowledge.indices) {
                if (handKnowledge[i].possibleRanks.contains(rank)) {
                    newKnowledge[i] = handKnowledge[i].copy(
                        possibleRanks = setOf(rank),
                        isClued = true,
                        positiveRankClues = handKnowledge[i].positiveRankClues + rank
                    )
                }
            }
        }
        
        handKnowledge = newKnowledge
    }

    private fun removeCardKnowledge(cardIndex: Int) {
        if (cardIndex in handKnowledge.indices) {
            handKnowledge = handKnowledge.filterIndexed { i, _ -> i != cardIndex } + CardKnowledge()
        }
    }

    private fun updateFromVisibleCards(publicState: PublicGameState) {
        // Count what cards are visible (in other hands, discard, and board)
        val visibleCards = mutableMapOf<Pair<Color, Rank>, Int>()
        
        // Count cards in other hands
        for (hand in publicState.otherHands.values) {
            for (card in hand) {
                val key = card.color to card.rank
                visibleCards[key] = visibleCards.getOrDefault(key, 0) + 1
            }
        }
        
        // Count cards in discard pile
        for (card in publicState.discardPile) {
            val key = card.color to card.rank
            visibleCards[key] = visibleCards.getOrDefault(key, 0) + 1
        }
        
        // Count cards on board
        for ((color, rank) in publicState.board) {
            if (rank != null) {
                for (r in 1..rank.value) {
                    val key = color to Rank(r)
                    visibleCards[key] = visibleCards.getOrDefault(key, 0) + 1
                }
            }
        }
        
        // Update knowledge based on what's impossible
        val newKnowledge = handKnowledge.map { knowledge ->
            var updated = knowledge
            
            // Remove colors/ranks that are fully accounted for
            val possibleColors = knowledge.possibleColors.filter { color ->
                knowledge.possibleRanks.any { rank ->
                    val key = color to rank
                    val visible = visibleCards.getOrDefault(key, 0)
                    val total = getCardCount(rank)
                    visible < total
                }
            }.toSet()
            
            val possibleRanks = knowledge.possibleRanks.filter { rank ->
                knowledge.possibleColors.any { color ->
                    val key = color to rank
                    val visible = visibleCards.getOrDefault(key, 0)
                    val total = getCardCount(rank)
                    visible < total
                }
            }.toSet()
            
            if (possibleColors.isNotEmpty() && possibleRanks.isNotEmpty()) {
                updated = updated.copy(
                    possibleColors = possibleColors,
                    possibleRanks = possibleRanks
                )
            }
            
            updated
        }
        
        handKnowledge = newKnowledge
    }

    private fun getCardCount(rank: Rank): Int {
        return when (rank.value) {
            1 -> 3
            2, 3, 4 -> 2
            5 -> 1
            else -> 0
        }
    }

    fun getKnowledge(): List<CardKnowledge> = handKnowledge

    fun getKnowledgeAt(index: Int): CardKnowledge? = handKnowledge.getOrNull(index)

    fun updateHandSize(newSize: Int) {
        if (newSize < handKnowledge.size) {
            handKnowledge = handKnowledge.take(newSize)
        } else if (newSize > handKnowledge.size) {
            handKnowledge = handKnowledge + List(newSize - handKnowledge.size) { CardKnowledge() }
        }
    }
}
