import model.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class DeckTest {
    @Test
    fun testCardDistribution() {
        // Create a full deck
        val cards = mutableListOf<CardIdentity>()
        for (color in Color.values()) {
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

        // Verify total count
        assertEquals(50, cards.size, "Deck should have exactly 50 cards")

        // Verify count per suit
        for (color in Color.values()) {
            val suitCards = cards.filter { it.color == color }
            assertEquals(10, suitCards.size, "Each suit should have 10 cards")

            // Verify rank distribution per suit
            assertEquals(3, suitCards.count { it.rank.value == 1 }, "Should have 3 ones per suit")
            assertEquals(2, suitCards.count { it.rank.value == 2 }, "Should have 2 twos per suit")
            assertEquals(2, suitCards.count { it.rank.value == 3 }, "Should have 2 threes per suit")
            assertEquals(2, suitCards.count { it.rank.value == 4 }, "Should have 2 fours per suit")
            assertEquals(1, suitCards.count { it.rank.value == 5 }, "Should have 1 five per suit")
        }
    }

    @Test
    fun testRankValidation() {
        // Valid ranks
        assertDoesNotThrow { Rank(1) }
        assertDoesNotThrow { Rank(2) }
        assertDoesNotThrow { Rank(3) }
        assertDoesNotThrow { Rank(4) }
        assertDoesNotThrow { Rank(5) }

        // Invalid ranks
        assertThrows(IllegalArgumentException::class.java) { Rank(0) }
        assertThrows(IllegalArgumentException::class.java) { Rank(6) }
    }

    @Test
    fun testRankNext() {
        assertEquals(Rank(2), Rank(1).next())
        assertEquals(Rank(3), Rank(2).next())
        assertEquals(Rank(4), Rank(3).next())
        assertEquals(Rank(5), Rank(4).next())
        assertNull(Rank(5).next())
    }
}
