package knowledge

import engine.PublicGameState
import model.*

class BeliefTracker(private val myPlayerId: Int, initialHandSize: Int) {
    private var handKnowledge = List(initialHandSize) { CardKnowledge() }

    fun updateFromAction(action: GameAction, publicState: PublicGameState) {
        when (action) {
            is HintColor -> {
                if (action.targetPlayerId == myPlayerId) {
                    processColorHint(action.color, publicState)
                }
            }
            is HintRank -> {
                if (action.targetPlayerId == myPlayerId) {
                    processRankHint(action.rank, publicState)
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

    private fun processColorHint(color: Color, publicState: PublicGameState) {
        // We need to determine which cards were hinted from the action history
        // For simplicity, we'll mark all cards that match the color
        val newKnowledge = handKnowledge.toMutableList()
        
        for (i in handKnowledge.indices) {
            // Note: In a real implementation, we'd track which specific cards were pointed to
            // For now, we assume the hint points to all matching cards in a sweep
            newKnowledge[i] = handKnowledge[i].copy(
                possibleColors = setOf(color),
                isClued = true,
                positiveColorClues = handKnowledge[i].positiveColorClues + color
            )
        }
        
        handKnowledge = newKnowledge
    }

    private fun processRankHint(rank: Rank, publicState: PublicGameState) {
        val newKnowledge = handKnowledge.toMutableList()
        
        for (i in handKnowledge.indices) {
            newKnowledge[i] = handKnowledge[i].copy(
                possibleRanks = setOf(rank),
                isClued = true,
                positiveRankClues = handKnowledge[i].positiveRankClues + rank
            )
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
