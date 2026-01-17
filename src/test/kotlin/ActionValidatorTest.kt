import engine.*
import model.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class ActionValidatorTest {
    @Test
    fun testValidPlay() {
        val validator = ActionValidator()
        val state = createTestState()
        val action = PlayCard(0, 0)
        
        val result = validator.validate(action, state)
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun testInvalidPlayIndex() {
        val validator = ActionValidator()
        val state = createTestState()
        val action = PlayCard(0, 10) // Invalid index
        
        val result = validator.validate(action, state)
        assertTrue(result is ValidationResult.Error)
    }

    @Test
    fun testValidDiscard() {
        val validator = ActionValidator()
        val state = createTestState()
        val action = DiscardCard(0, 0)
        
        val result = validator.validate(action, state)
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun testCannotDiscardAtMaxTokens() {
        val validator = ActionValidator()
        val state = createTestState().copy(clueTokens = 8)
        val action = DiscardCard(0, 0)
        
        val result = validator.validate(action, state)
        assertTrue(result is ValidationResult.Error)
    }

    @Test
    fun testValidHint() {
        val validator = ActionValidator()
        val state = createTestState()
        val action = HintColor(0, 1, Color.RED)
        
        val result = validator.validate(action, state)
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun testCannotHintWithoutTokens() {
        val validator = ActionValidator()
        val state = createTestState().copy(clueTokens = 0)
        val action = HintColor(0, 1, Color.RED)
        
        val result = validator.validate(action, state)
        assertTrue(result is ValidationResult.Error)
    }

    @Test
    fun testCannotHintSelf() {
        val validator = ActionValidator()
        val state = createTestState()
        val action = HintColor(0, 0, Color.RED)
        
        val result = validator.validate(action, state)
        assertTrue(result is ValidationResult.Error)
    }

    @Test
    fun testCannotGiveEmptyHint() {
        val validator = ActionValidator()
        val state = createTestState()
        // Player 1 has no blue cards
        val action = HintColor(0, 1, Color.BLUE)
        
        val result = validator.validate(action, state)
        assertTrue(result is ValidationResult.Error)
    }

    private fun createTestState(): FullGameState {
        return FullGameState(
            deck = ArrayDeque(listOf(CardIdentity(Color.RED, Rank(1)))),
            hands = mapOf(
                0 to listOf(CardIdentity(Color.RED, Rank(1)), CardIdentity(Color.GREEN, Rank(2))),
                1 to listOf(CardIdentity(Color.RED, Rank(2)), CardIdentity(Color.YELLOW, Rank(3)))
            ),
            discardPile = emptyList(),
            board = Color.values().associateWith { null },
            clueTokens = 5,
            fuseTokens = 0,
            currentPlayerIndex = 0,
            turnsRemaining = null,
            gameOver = false
        )
    }
}
