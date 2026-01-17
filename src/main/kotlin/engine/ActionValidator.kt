package engine

import model.*

class ActionValidator {
    fun validate(action: GameAction, state: FullGameState): ValidationResult {
        val hand = state.hands[action.actorPlayerId] ?: return ValidationResult.Error("Player not found")
        
        return when (action) {
            is PlayCard -> validatePlay(action, hand, state)
            is DiscardCard -> validateDiscard(action, hand, state)
            is GiveHint -> validateHint(action, state)
        }
    }

    private fun validatePlay(action: PlayCard, hand: List<CardIdentity>, state: FullGameState): ValidationResult {
        if (action.cardIndex !in hand.indices) {
            return ValidationResult.Error("Invalid card index")
        }
        return ValidationResult.Valid
    }

    private fun validateDiscard(action: DiscardCard, hand: List<CardIdentity>, state: FullGameState): ValidationResult {
        if (action.cardIndex !in hand.indices) {
            return ValidationResult.Error("Invalid card index")
        }
        if (state.clueTokens >= 8) {
            return ValidationResult.Error("Cannot discard when at max clue tokens")
        }
        return ValidationResult.Valid
    }

    private fun validateHint(action: GiveHint, state: FullGameState): ValidationResult {
        if (state.clueTokens <= 0) {
            return ValidationResult.Error("No clue tokens available")
        }
        if (action.targetPlayerId == action.actorPlayerId) {
            return ValidationResult.Error("Cannot hint yourself")
        }
        val targetHand = state.hands[action.targetPlayerId] 
            ?: return ValidationResult.Error("Target player not found")
        
        // Check for empty hint
        val matchingCards = when (action) {
            is HintColor -> targetHand.count { it.color == action.color }
            is HintRank -> targetHand.count { it.rank == action.rank }
        }
        
        if (matchingCards == 0) {
            return ValidationResult.Error("Cannot give empty hint")
        }
        
        return ValidationResult.Valid
    }
}

sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Error(val message: String) : ValidationResult()
}
