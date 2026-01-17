package engine

import model.*

class GameEngine(private val numPlayers: Int) {
    private val validator = ActionValidator()
    private var state: FullGameState
    
    init {
        require(numPlayers in 2..5) { "Number of players must be between 2 and 5" }
        state = initializeGame()
    }

    private fun initializeGame(): FullGameState {
        val deck = createDeck()
        val handSize = if (numPlayers <= 3) 5 else 4
        val hands = mutableMapOf<Int, List<CardIdentity>>()
        
        // Deal cards
        for (playerId in 0 until numPlayers) {
            hands[playerId] = List(handSize) { deck.removeFirst() }
        }
        
        val board = Color.values().associateWith { null as Rank? }
        
        return FullGameState(
            deck = deck,
            hands = hands,
            discardPile = emptyList(),
            board = board,
            clueTokens = 8,
            fuseTokens = 0,
            currentPlayerIndex = 0,
            turnsRemaining = null,
            gameOver = false,
            actionHistory = emptyList()
        )
    }

    private fun createDeck(): ArrayDeque<CardIdentity> {
        val cards = mutableListOf<CardIdentity>()
        for (color in Color.values()) {
            // 3x 1s, 2x 2s, 2x 3s, 2x 4s, 1x 5
            cards.add(CardIdentity(color, Rank(1)))
            cards.add(CardIdentity(color, Rank(1)))
            cards.add(CardIdentity(color, Rank(1)))
            cards.add(CardIdentity(color, Rank(2)))
            cards.add(CardIdentity(color, Rank(2)))
            cards.add(CardIdentity(color, Rank(3)))
            cards.add(CardIdentity(color, Rank(3)))
            cards.add(CardIdentity(color, Rank(4)))
            cards.add(CardIdentity(color, Rank(4)))
            cards.add(CardIdentity(color, Rank(5)))
        }
        cards.shuffle()
        return ArrayDeque(cards)
    }

    fun getPublicState(forPlayerId: Int): PublicGameState {
        val otherHands = state.hands.filterKeys { it != forPlayerId }
        return PublicGameState(
            myPlayerId = forPlayerId,
            myHandSize = state.hands[forPlayerId]?.size ?: 0,
            otherHands = otherHands,
            discardPile = state.discardPile,
            board = state.board,
            clueTokens = state.clueTokens,
            fuseTokens = state.fuseTokens,
            deckSize = state.deck.size,
            actionHistory = state.actionHistory
        )
    }

    fun getCurrentState(): FullGameState = state

    fun executeAction(action: GameAction): ActionResult {
        if (state.gameOver) {
            return ActionResult.Error("Game is over")
        }

        val validation = validator.validate(action, state)
        if (validation is ValidationResult.Error) {
            return ActionResult.Error(validation.message)
        }

        state = when (action) {
            is PlayCard -> executePlay(action)
            is DiscardCard -> executeDiscard(action)
            is GiveHint -> executeHint(action)
        }

        // Update action history
        state = state.copy(actionHistory = state.actionHistory + action)

        // Check for game end conditions
        checkGameEnd()

        return ActionResult.Success(state.gameOver)
    }

    private fun executePlay(action: PlayCard): FullGameState {
        val hand = state.hands[action.actorPlayerId]!!
        val card = hand[action.cardIndex]
        val newHands = state.hands.toMutableMap()
        newHands[action.actorPlayerId] = hand.filterIndexed { i, _ -> i != action.cardIndex }

        val expectedRank = state.board[card.color]?.next() ?: Rank(1)
        
        return if (card.rank == expectedRank) {
            // Valid play
            val newBoard = state.board.toMutableMap()
            newBoard[card.color] = card.rank
            
            // Draw replacement card
            val newDeck = state.deck.toMutableList().let { ArrayDeque(it) }
            if (newDeck.isNotEmpty()) {
                newHands[action.actorPlayerId] = newHands[action.actorPlayerId]!! + newDeck.removeFirst()
            }
            
            // Check if played a 5 - restore clue token
            val newClueTokens = if (card.rank.value == 5 && state.clueTokens < 8) {
                state.clueTokens + 1
            } else {
                state.clueTokens
            }
            
            state.copy(
                deck = newDeck,
                hands = newHands,
                board = newBoard,
                clueTokens = newClueTokens,
                currentPlayerIndex = (state.currentPlayerIndex + 1) % numPlayers
            )
        } else {
            // Invalid play - add to discard, increment fuse
            val newDeck = state.deck.toMutableList().let { ArrayDeque(it) }
            if (newDeck.isNotEmpty()) {
                newHands[action.actorPlayerId] = newHands[action.actorPlayerId]!! + newDeck.removeFirst()
            }
            
            state.copy(
                deck = newDeck,
                hands = newHands,
                discardPile = state.discardPile + card,
                fuseTokens = state.fuseTokens + 1,
                currentPlayerIndex = (state.currentPlayerIndex + 1) % numPlayers
            )
        }
    }

    private fun executeDiscard(action: DiscardCard): FullGameState {
        val hand = state.hands[action.actorPlayerId]!!
        val card = hand[action.cardIndex]
        val newHands = state.hands.toMutableMap()
        newHands[action.actorPlayerId] = hand.filterIndexed { i, _ -> i != action.cardIndex }

        // Draw replacement card
        val newDeck = state.deck.toMutableList().let { ArrayDeque(it) }
        if (newDeck.isNotEmpty()) {
            newHands[action.actorPlayerId] = newHands[action.actorPlayerId]!! + newDeck.removeFirst()
        }

        return state.copy(
            deck = newDeck,
            hands = newHands,
            discardPile = state.discardPile + card,
            clueTokens = minOf(state.clueTokens + 1, 8),
            currentPlayerIndex = (state.currentPlayerIndex + 1) % numPlayers
        )
    }

    private fun executeHint(action: GiveHint): FullGameState {
        return state.copy(
            clueTokens = state.clueTokens - 1,
            currentPlayerIndex = (state.currentPlayerIndex + 1) % numPlayers
        )
    }

    private fun checkGameEnd() {
        // Check for explosion (3 fuses)
        if (state.fuseTokens >= 3) {
            state = state.copy(gameOver = true)
            return
        }

        // Check for perfect score
        val allFivesPlayed = state.board.values.all { it?.value == 5 }
        if (allFivesPlayed) {
            state = state.copy(gameOver = true)
            return
        }

        // Check for deck exhaustion
        if (state.deck.isEmpty() && state.turnsRemaining == null) {
            state = state.copy(turnsRemaining = numPlayers)
        } else {
            val turnsLeft = state.turnsRemaining
            if (turnsLeft != null) {
                val remaining = turnsLeft - 1
                if (remaining == 0) {
                    state = state.copy(gameOver = true)
                } else {
                    state = state.copy(turnsRemaining = remaining)
                }
            }
        }
    }

    fun getScore(): Int {
        if (state.fuseTokens >= 3) return 0
        return state.board.values.sumOf { it?.value ?: 0 }
    }

    fun getCurrentPlayer(): Int = state.currentPlayerIndex

    fun isGameOver(): Boolean = state.gameOver

    fun getNumPlayers(): Int = numPlayers
}

sealed class ActionResult {
    data class Success(val gameOver: Boolean) : ActionResult()
    data class Error(val message: String) : ActionResult()
}
