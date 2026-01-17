package ai

import engine.PublicGameState
import knowledge.BeliefTracker
import knowledge.CardKnowledge
import model.*

class SmartBot(private val playerId: Int, initialHandSize: Int) {
    private val beliefTracker = BeliefTracker(playerId, initialHandSize)

    fun updateBeliefs(action: GameAction, publicState: PublicGameState, hintedIndices: List<Int>? = null) {
        beliefTracker.updateFromAction(action, publicState, hintedIndices)
        beliefTracker.updateHandSize(publicState.myHandSize)
    }

    fun getKnowledge(): List<CardKnowledge> = beliefTracker.getKnowledge()

    fun chooseAction(publicState: PublicGameState): GameAction {
        // Decision Priority:
        // 1. Play known-playable cards
        // 2. Play clued cards that are probably playable
        // 3. Give save clue if teammate's chop is critical
        // 4. Give play clue to enable teammate's play
        // 5. Discard chop (rightmost unclued card)

        val knowledge = beliefTracker.getKnowledge()

        // 1. Check for known-playable cards
        for (i in knowledge.indices) {
            if (isKnownPlayable(knowledge[i], publicState.board)) {
                return PlayCard(playerId, i)
            }
        }

        // 2. Check for clued cards that might be playable
        val cluedPlayable = findCluedPlayable(knowledge, publicState.board)
        if (cluedPlayable != null) {
            return PlayCard(playerId, cluedPlayable)
        }

        // 3. Try to give a save clue
        val saveClue = findSaveClue(publicState)
        if (saveClue != null) {
            return saveClue
        }

        // 4. Try to give a play clue
        val playClue = findPlayClue(publicState)
        if (playClue != null) {
            return playClue
        }

        // 5. Discard chop
        val chopIndex = findChopIndex(knowledge)
        return DiscardCard(playerId, chopIndex)
    }

    private fun isKnownPlayable(knowledge: CardKnowledge, board: Map<Color, Rank?>): Boolean {
        if (!knowledge.isCertain()) return false

        val color = knowledge.getCertainColor()!!
        val rank = knowledge.getCertainRank()!!
        val expectedRank = board[color]?.next() ?: Rank(1)

        return rank == expectedRank
    }

    private fun findCluedPlayable(knowledge: List<CardKnowledge>, board: Map<Color, Rank?>): Int? {
        // Look for clued cards that could be playable based on current board state
        for (i in knowledge.indices) {
            val k = knowledge[i]
            if (!k.isClued) continue

            // Check if any possible card identity is playable
            val couldBePlayable = k.possibleColors.any { color ->
                k.possibleRanks.any { rank ->
                    val expectedRank = board[color]?.next() ?: Rank(1)
                    rank == expectedRank
                }
            }

            if (couldBePlayable) {
                // Additional heuristic: prioritize leftmost clued cards
                return i
            }
        }
        return null
    }

    private fun findSaveClue(publicState: PublicGameState): GiveHint? {
        if (publicState.clueTokens <= 0) return null

        // Check each other player's hand for critical cards on their chop
        for ((targetId, hand) in publicState.otherHands) {
            val chopIndex = findChopIndexForHand(hand, publicState)
            if (chopIndex >= 0 && chopIndex < hand.size) {
                val card = hand[chopIndex]

                // Check if this card is critical (last copy needed)
                if (isCriticalCard(card, publicState)) {
                    // Give a clue to save it - prefer color clues
                    return HintColor(playerId, targetId, card.color)
                }
            }
        }
        return null
    }

    private fun findPlayClue(publicState: PublicGameState): GiveHint? {
        if (publicState.clueTokens <= 0) return null

        // Look for cards that are immediately playable in other hands
        for ((targetId, hand) in publicState.otherHands) {
            for ((index, card) in hand.withIndex()) {
                val expectedRank = publicState.board[card.color]?.next() ?: Rank(1)
                if (card.rank == expectedRank) {
                    // This card is playable - give a hint about it
                    // Prefer color hints for lower ranks, rank hints for higher ranks
                    return if (card.rank.value <= 3) {
                        HintColor(playerId, targetId, card.color)
                    } else {
                        HintRank(playerId, targetId, card.rank)
                    }
                }
            }
        }
        return null
    }

    private fun findChopIndex(knowledge: List<CardKnowledge>): Int {
        // Chop is the rightmost unclued card
        for (i in knowledge.indices.reversed()) {
            if (!knowledge[i].isClued) {
                return i
            }
        }
        // If all cards are clued, discard the rightmost one
        return knowledge.size - 1
    }

    private fun findChopIndexForHand(hand: List<CardIdentity>, publicState: PublicGameState): Int {
        // For other players, we don't have their knowledge, so we estimate
        // by looking at what cards were recently hinted
        // Simplification: assume rightmost card is chop
        return hand.size - 1
    }

    private fun isCriticalCard(card: CardIdentity, publicState: PublicGameState): Boolean {
        // A card is critical if it's the last copy of a card that's still needed
        val expectedRank = publicState.board[card.color]?.next() ?: Rank(1)
        
        // If the card is already played or below what's needed, it's not critical
        if (card.rank.value < expectedRank.value) return false

        // Count how many copies of this card are in the discard pile
        val discardedCount = publicState.discardPile.count { 
            it.color == card.color && it.rank == card.rank 
        }

        // Get the total count for this rank
        val totalCount = when (card.rank.value) {
            1 -> 3
            2, 3, 4 -> 2
            5 -> 1
            else -> 0
        }

        // If this is the last copy, it's critical
        return discardedCount >= totalCount - 1
    }

    fun getBeliefState(): String {
        val knowledge = beliefTracker.getKnowledge()
        val sb = StringBuilder()
        
        for (i in knowledge.indices) {
            val k = knowledge[i]
            sb.append("  Card $i: ")
            
            if (k.isCertain()) {
                sb.append("${k.getCertainColor()} ${k.getCertainRank()} (certain)")
            } else {
                val colors = k.possibleColors.joinToString(",") { it.name[0].toString() }
                val ranks = k.possibleRanks.joinToString(",") { it.value.toString() }
                
                if (k.possibleColors.size == 1) {
                    sb.append("Color=${k.possibleColors.first()} (certain)")
                } else if (k.possibleColors.size < Color.values().size) {
                    sb.append("Color={$colors}")
                } else {
                    sb.append("Color={any}")
                }
                
                sb.append(", ")
                
                if (k.possibleRanks.size == 1) {
                    sb.append("Rank=${k.possibleRanks.first().value} (certain)")
                } else if (k.possibleRanks.size < 5) {
                    sb.append("Rank={$ranks}")
                } else {
                    sb.append("Rank={any}")
                }
            }
            
            if (k.isClued) {
                sb.append(" - Clued")
            } else {
                sb.append(" - Unknown")
            }
            
            sb.append("\n")
        }
        
        return sb.toString()
    }
}
