import engine.GameEngine
import model.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class GameEngineTest {
    @Test
    fun testGameInitialization() {
        val game = GameEngine(4)
        val state = game.getCurrentState()
        
        assertEquals(8, state.clueTokens, "Should start with 8 clue tokens")
        assertEquals(0, state.fuseTokens, "Should start with 0 fuse tokens")
        assertFalse(state.gameOver, "Game should not be over at start")
        assertEquals(0, game.getCurrentPlayer(), "First player should be 0")
    }

    @Test
    fun testHandSizes() {
        // 2-3 players: 5 cards
        val game2 = GameEngine(2)
        assertEquals(5, game2.getCurrentState().hands[0]?.size)
        
        val game3 = GameEngine(3)
        assertEquals(5, game3.getCurrentState().hands[0]?.size)
        
        // 4-5 players: 4 cards
        val game4 = GameEngine(4)
        assertEquals(4, game4.getCurrentState().hands[0]?.size)
        
        val game5 = GameEngine(5)
        assertEquals(4, game5.getCurrentState().hands[0]?.size)
    }

    @Test
    fun testValidPlay() {
        val game = GameEngine(2)
        val state = game.getCurrentState()
        
        // Find a 1 in the current player's hand
        val hand = state.hands[0]!!
        val oneIndex = hand.indexOfFirst { it.rank.value == 1 }
        
        if (oneIndex >= 0) {
            val card = hand[oneIndex]
            val result = game.executeAction(PlayCard(0, oneIndex))
            
            assertTrue(result is engine.ActionResult.Success)
            
            val newState = game.getCurrentState()
            assertEquals(card.rank, newState.board[card.color], "Card should be on the board")
        }
    }

    @Test
    fun testInvalidPlay() {
        val game = GameEngine(2)
        val state = game.getCurrentState()
        
        // Try to play a non-1 card
        val hand = state.hands[0]!!
        val nonOneIndex = hand.indexOfFirst { it.rank.value != 1 }
        
        if (nonOneIndex >= 0) {
            val card = hand[nonOneIndex]
            if (card.rank.value != 1) { // Ensure it's not playable
                val fusesBefore = state.fuseTokens
                game.executeAction(PlayCard(0, nonOneIndex))
                
                val newState = game.getCurrentState()
                assertEquals(fusesBefore + 1, newState.fuseTokens, "Should gain a fuse token")
            }
        }
    }

    @Test
    fun testDiscard() {
        val game = GameEngine(2)
        val tokensBefore = game.getCurrentState().clueTokens
        
        game.executeAction(DiscardCard(0, 0))
        
        val tokensAfter = game.getCurrentState().clueTokens
        assertEquals(minOf(tokensBefore + 1, 8), tokensAfter, "Should gain a clue token")
    }

    @Test
    fun testExplosionEndsGame() {
        val game = GameEngine(2)
        
        // Play invalid cards to get 3 fuses
        for (i in 0 until 3) {
            val state = game.getCurrentState()
            if (!state.gameOver) {
                val hand = state.hands[state.currentPlayerIndex]!!
                val nonOneIndex = hand.indexOfFirst { it.rank.value != 1 }
                if (nonOneIndex >= 0) {
                    game.executeAction(PlayCard(state.currentPlayerIndex, nonOneIndex))
                }
            }
        }
        
        assertTrue(game.isGameOver(), "Game should be over after 3 fuses")
        assertEquals(0, game.getScore(), "Score should be 0 after explosion")
    }

    @Test
    fun testPlayerRotation() {
        val game = GameEngine(3)
        
        assertEquals(0, game.getCurrentPlayer())
        
        // Use hints to advance turns and reduce clue tokens
        val state = game.getCurrentState()
        
        // Player 0 gives a hint
        val targetHand1 = state.hands[1]!!
        game.executeAction(HintColor(0, 1, targetHand1[0].color))
        assertEquals(1, game.getCurrentPlayer())
        
        // Player 1 gives a hint
        val state2 = game.getCurrentState()
        val targetHand2 = state2.hands[2]!!
        game.executeAction(HintColor(1, 2, targetHand2[0].color))
        assertEquals(2, game.getCurrentPlayer())
        
        // Player 2 gives a hint
        val state3 = game.getCurrentState()
        val targetHand3 = state3.hands[0]!!
        game.executeAction(HintColor(2, 0, targetHand3[0].color))
        
        assertEquals(0, game.getCurrentPlayer(), "Should wrap around to player 0")
    }
}
