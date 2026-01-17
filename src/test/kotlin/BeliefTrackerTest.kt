import knowledge.BeliefTracker
import knowledge.CardKnowledge
import engine.PublicGameState
import model.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class BeliefTrackerTest {
    @Test
    fun testInitialKnowledge() {
        val tracker = BeliefTracker(0, 5)
        val knowledge = tracker.getKnowledge()
        
        assertEquals(5, knowledge.size)
        
        for (k in knowledge) {
            assertEquals(Color.values().size, k.possibleColors.size, "Should have all colors possible")
            assertEquals(5, k.possibleRanks.size, "Should have all ranks possible")
            assertFalse(k.isClued, "Should not be clued initially")
        }
    }

    @Test
    fun testColorHintRestriction() {
        val tracker = BeliefTracker(0, 5)
        val publicState = createTestPublicState()
        
        val hint = HintColor(1, 0, Color.RED)
        tracker.updateFromAction(hint, publicState)
        
        val knowledge = tracker.getKnowledge()
        for (k in knowledge) {
            assertTrue(k.isClued, "All cards should be marked as clued after hint")
            assertEquals(1, k.possibleColors.size, "Should only have RED as possible color")
            assertTrue(k.possibleColors.contains(Color.RED), "RED should be the possible color")
        }
    }

    @Test
    fun testRankHintRestriction() {
        val tracker = BeliefTracker(0, 5)
        val publicState = createTestPublicState()
        
        val hint = HintRank(1, 0, Rank(3))
        tracker.updateFromAction(hint, publicState)
        
        val knowledge = tracker.getKnowledge()
        for (k in knowledge) {
            assertTrue(k.isClued, "All cards should be marked as clued after hint")
            assertEquals(1, k.possibleRanks.size, "Should only have rank 3 as possible")
            assertTrue(k.possibleRanks.contains(Rank(3)), "Rank 3 should be the possible rank")
        }
    }

    @Test
    fun testCardRemovalOnPlay() {
        val tracker = BeliefTracker(0, 5)
        val publicState = createTestPublicState()
        
        val play = PlayCard(0, 2)
        tracker.updateFromAction(play, publicState)
        
        // After playing card at index 2, we should still have 5 cards (a new one is added)
        val knowledge = tracker.getKnowledge()
        assertEquals(5, knowledge.size)
    }

    @Test
    fun testCardRemovalOnDiscard() {
        val tracker = BeliefTracker(0, 5)
        val publicState = createTestPublicState()
        
        val discard = DiscardCard(0, 0)
        tracker.updateFromAction(discard, publicState)
        
        val knowledge = tracker.getKnowledge()
        assertEquals(5, knowledge.size)
    }

    @Test
    fun testCertaintyCheck() {
        val certain = CardKnowledge(
            possibleColors = setOf(Color.RED),
            possibleRanks = setOf(Rank(3))
        )
        assertTrue(certain.isCertain())
        assertEquals(Color.RED, certain.getCertainColor())
        assertEquals(Rank(3), certain.getCertainRank())
        
        val uncertain = CardKnowledge(
            possibleColors = setOf(Color.RED, Color.BLUE),
            possibleRanks = setOf(Rank(3))
        )
        assertFalse(uncertain.isCertain())
        assertNull(uncertain.getCertainColor())
        assertEquals(Rank(3), uncertain.getCertainRank())
    }

    private fun createTestPublicState(): PublicGameState {
        return PublicGameState(
            myPlayerId = 0,
            myHandSize = 5,
            otherHands = mapOf(
                1 to listOf(
                    CardIdentity(Color.RED, Rank(1)),
                    CardIdentity(Color.BLUE, Rank(2))
                )
            ),
            discardPile = emptyList(),
            board = Color.values().associateWith { null },
            clueTokens = 8,
            fuseTokens = 0,
            deckSize = 30,
            actionHistory = emptyList()
        )
    }
}
