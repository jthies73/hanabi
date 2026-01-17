package ui

import engine.FullGameState
import engine.PublicGameState
import model.*

class ConsoleUI {
    fun displayWelcome() {
        println("╔══════════════════════════════════════════════════════════════╗")
        println("║                         HANABI                                ║")
        println("╚══════════════════════════════════════════════════════════════╝")
    }

    fun displayGameState(state: FullGameState, humanPlayerId: Int) {
        println()
        println("╔══════════════════════════════════════════════════════════════╗")
        println("║                         HANABI                                ║")
        println("╠══════════════════════════════════════════════════════════════╣")
        
        // Display board
        val boardStr = Color.values().joinToString(" ") { color ->
            val rank = state.board[color]
            val colorCode = getColorCode(color)
            val rankStr = if (rank != null) rank.value.toString() else "-"
            "$colorCode[$rankStr]\u001B[0m"
        }
        println("║ Board:  $boardStr                             ")
        
        // Display tokens and deck
        val clueStr = "${state.clueTokens}/8"
        val fuseStr = "${state.fuseTokens}/3"
        val deckStr = state.deck.size.toString()
        println("║ Clues: $clueStr    Fuses: $fuseStr    Deck: $deckStr${" ".repeat(maxOf(0, 25 - deckStr.length))}")
        println("╠══════════════════════════════════════════════════════════════╣")
        
        // Display discard pile
        val discards = state.discardPile.takeLast(10).joinToString(", ") { 
            "${getColorInitial(it.color)}${it.rank.value}"
        }
        println("║ Discards: ${discards}${" ".repeat(maxOf(0, 49 - discards.length))}║")
        println("╠══════════════════════════════════════════════════════════════╣")
        
        // Display other players' hands
        for ((playerId, hand) in state.hands) {
            if (playerId != humanPlayerId) {
                val playerName = getPlayerName(playerId, humanPlayerId)
                val handStr = hand.joinToString(" ") { card ->
                    val colorCode = getColorCode(card.color)
                    "$colorCode[${getColorInitial(card.color)}${card.rank.value}]\u001B[0m"
                }
                println("║ $playerName: $handStr${" ".repeat(maxOf(0, 42 - hand.size * 5))}║")
            }
        }
        
        // Display human hand (hidden)
        val humanHand = state.hands[humanPlayerId]
        if (humanHand != null) {
            val handStr = humanHand.indices.joinToString(" ") { "[??]" }
            println("║ You:  $handStr  (indices 0-${humanHand.size - 1})${" ".repeat(maxOf(0, 31 - humanHand.size * 5))}║")
        }
        
        println("╚══════════════════════════════════════════════════════════════╝")
    }

    fun displayPublicGameState(state: PublicGameState) {
        println()
        println("╔══════════════════════════════════════════════════════════════╗")
        println("║                      GAME STATE                               ║")
        println("╠══════════════════════════════════════════════════════════════╣")
        
        // Display board
        val boardStr = Color.values().joinToString(" ") { color ->
            val rank = state.board[color]
            val rankStr = if (rank != null) rank.value.toString() else "-"
            "${getColorInitial(color)}[$rankStr]"
        }
        println("║ Board:  $boardStr                                    ║")
        
        // Display tokens and deck
        println("║ Clues: ${state.clueTokens}/8    Fuses: ${state.fuseTokens}/3    Deck: ${state.deckSize}                    ║")
        println("╚══════════════════════════════════════════════════════════════╝")
    }

    fun displayCommands() {
        println()
        println("Commands:")
        println("  play <index>         - Play card at index")
        println("  discard <index>      - Discard card at index")
        println("  hint <player> <clue> - Give hint (e.g., 'hint Bot1 red' or 'hint Bot2 3')")
        println("  query <player>       - Ask bot what they know about their cards")
        println("  help                 - Show commands")
        println("  quit                 - Exit game")
    }

    fun displayBotAction(playerId: Int, humanPlayerId: Int, action: GameAction) {
        val playerName = getPlayerName(playerId, humanPlayerId)
        val actionStr = when (action) {
            is PlayCard -> "plays card at index ${action.cardIndex}"
            is DiscardCard -> "discards card at index ${action.cardIndex}"
            is HintColor -> {
                val targetName = getPlayerName(action.targetPlayerId, humanPlayerId)
                "gives $targetName a ${action.color.name.lowercase()} hint"
            }
            is HintRank -> {
                val targetName = getPlayerName(action.targetPlayerId, humanPlayerId)
                "gives $targetName a ${action.rank.value} hint"
            }
        }
        println("$playerName $actionStr")
    }

    fun displayPlayResult(card: CardIdentity, success: Boolean) {
        val colorCode = getColorCode(card.color)
        if (success) {
            println("$colorCode✓ Successfully played ${card.color} ${card.rank.value}!\u001B[0m")
        } else {
            println("$colorCode✗ Failed to play ${card.color} ${card.rank.value} - added to discard pile and gained a fuse!\u001B[0m")
        }
    }

    fun displayDiscardResult(card: CardIdentity) {
        val colorCode = getColorCode(card.color)
        println("${colorCode}Discarded ${card.color} ${card.rank.value} - gained a clue token\u001B[0m")
    }

    fun displayHintResult(action: GiveHint, matchingIndices: List<Int>) {
        val hintType = when (action) {
            is HintColor -> action.color.name.lowercase()
            is HintRank -> action.rank.value.toString()
        }
        println("Hint given: $hintType - matching cards at indices: ${matchingIndices.joinToString(", ")}")
    }

    fun displayBotKnowledge(botName: String, knowledge: String) {
        println()
        println("╔══════════════════════════════════════════════════════════════╗")
        println("║ $botName's Knowledge${" ".repeat(maxOf(0, 48 - botName.length))}║")
        println("╠══════════════════════════════════════════════════════════════╣")
        knowledge.lines().forEach { line ->
            if (line.isNotEmpty()) {
                println("║ ${line}${" ".repeat(maxOf(0, 60 - line.length))}║")
            }
        }
        println("╚══════════════════════════════════════════════════════════════╝")
    }

    fun displayGameOver(score: Int, fuseTokens: Int) {
        println()
        println("╔══════════════════════════════════════════════════════════════╗")
        println("║                      GAME OVER                                ║")
        println("╠══════════════════════════════════════════════════════════════╣")
        if (fuseTokens >= 3) {
            println("║ EXPLOSION! The game ended with 3 fuses.                      ║")
            println("║ Final Score: 0                                                ║")
        } else if (score == 25) {
            println("║ PERFECT SCORE! All fireworks completed!                      ║")
            println("║ Final Score: 25                                               ║")
        } else {
            println("║ Final Score: $score${" ".repeat(maxOf(0, 47 - score.toString().length))}║")
        }
        println("╚══════════════════════════════════════════════════════════════╝")
    }

    fun displayError(message: String) {
        println("\u001B[31m✗ Error: $message\u001B[0m")
    }

    fun displayMessage(message: String) {
        println(message)
    }

    private fun getColorCode(color: Color): String {
        return when (color) {
            Color.RED -> "\u001B[31m"      // Red
            Color.YELLOW -> "\u001B[33m"   // Yellow
            Color.GREEN -> "\u001B[32m"    // Green
            Color.BLUE -> "\u001B[34m"     // Blue
            Color.WHITE -> "\u001B[37m"    // White
        }
    }

    private fun getColorInitial(color: Color): String {
        return color.name[0].toString()
    }

    private fun getPlayerName(playerId: Int, humanPlayerId: Int): String {
        return if (playerId == humanPlayerId) {
            "You"
        } else {
            "Bot$playerId"
        }
    }
}
