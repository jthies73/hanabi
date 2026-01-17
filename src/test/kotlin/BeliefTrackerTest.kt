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
        
        // Simulate a hint that matches cards at indices 1 and 3
        val hint = HintColor(1, 0, Color.RED)
        val hintedIndices = listOf(1, 3)
        tracker.updateFromAction(hint, publicState, hintedIndices)
        
        val knowledge = tracker.getKnowledge()
        
        // Cards 1 and 3 should be marked as clued and restricted to RED
        assertTrue(knowledge[1].isClued, "Card 1 should be clued")
        assertEquals(1, knowledge[1].possibleColors.size, "Card 1 should only have RED as possible color")
        assertTrue(knowledge[1].possibleColors.contains(Color.RED), "Card 1 should have RED")
        
        assertTrue(knowledge[3].isClued, "Card 3 should be clued")
        assertEquals(1, knowledge[3].possibleColors.size, "Card 3 should only have RED as possible color")
        assertTrue(knowledge[3].possibleColors.contains(Color.RED), "Card 3 should have RED")
        
        // Cards 0, 2, 4 should NOT have RED as a possibility (negative inference)
        assertFalse(knowledge[0].isClued, "Card 0 should not be clued")
        assertFalse(knowledge[0].possibleColors.contains(Color.RED), "Card 0 should not have RED")
        
        assertFalse(knowledge[2].isClued, "Card 2 should not be clued")
        assertFalse(knowledge[2].possibleColors.contains(Color.RED), "Card 2 should not have RED")
        
        assertFalse(knowledge[4].isClued, "Card 4 should not be clued")
        assertFalse(knowledge[4].possibleColors.contains(Color.RED), "Card 4 should not have RED")
    }

    @Test
    fun testRankHintRestriction() {
        val tracker = BeliefTracker(0, 5)
        val publicState = createTestPublicState()
        
        // Simulate a hint that matches cards at indices 0 and 2
        val hint = HintRank(1, 0, Rank(3))
        val hintedIndices = listOf(0, 2)
        tracker.updateFromAction(hint, publicState, hintedIndices)
        
        val knowledge = tracker.getKnowledge()
        
        // Cards 0 and 2 should be marked as clued and restricted to rank 3
        assertTrue(knowledge[0].isClued, "Card 0 should be clued")
        assertEquals(1, knowledge[0].possibleRanks.size, "Card 0 should only have rank 3 as possible")
        assertTrue(knowledge[0].possibleRanks.contains(Rank(3)), "Card 0 should have rank 3")
        
        assertTrue(knowledge[2].isClued, "Card 2 should be clued")
        assertEquals(1, knowledge[2].possibleRanks.size, "Card 2 should only have rank 3 as possible")
        assertTrue(knowledge[2].possibleRanks.contains(Rank(3)), "Card 2 should have rank 3")
        
        // Cards 1, 3, 4 should NOT have rank 3 as a possibility (negative inference)
        assertFalse(knowledge[1].isClued, "Card 1 should not be clued")
        assertFalse(knowledge[1].possibleRanks.contains(Rank(3)), "Card 1 should not have rank 3")
        
        assertFalse(knowledge[3].isClued, "Card 3 should not be clued")
        assertFalse(knowledge[3].possibleRanks.contains(Rank(3)), "Card 3 should not have rank 3")
        
        assertFalse(knowledge[4].isClued, "Card 4 should not be clued")
        assertFalse(knowledge[4].possibleRanks.contains(Rank(3)), "Card 4 should not have rank 3")
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
