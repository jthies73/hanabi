import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import ai.SmartBot
import engine.GameEngine
import engine.ActionResult
import model.*
import ui.ConsoleUI
import knowledge.CardKnowledge

suspend fun main() = coroutineScope {
    val ui = ConsoleUI()
    ui.displayWelcome()
    
    print("Enter number of players (2-5): ")
    val numPlayers = readLine()?.toIntOrNull()?.takeIf { it in 2..5 } ?: 4
    
    val humanPlayerId = 0
    val game = GameEngine(numPlayers)
    val bots = mutableMapOf<Int, SmartBot>()
    
    // Create bots for non-human players
    val initialHandSize = if (numPlayers <= 3) 5 else 4
    for (playerId in 1 until numPlayers) {
        bots[playerId] = SmartBot(playerId, initialHandSize)
    }
    
    // Create input channel for query commands
    val inputChannel = Channel<String>(Channel.UNLIMITED)
    
    ui.displayCommands()
    
    // Main game loop
    while (!game.isGameOver()) {
        val currentPlayer = game.getCurrentPlayer()
        val state = game.getCurrentState()
        
        if (currentPlayer == humanPlayerId) {
            // Human player's turn
            ui.displayGameState(state, humanPlayerId)
            println()
            println("Your turn! Enter a command:")
            
            var validAction = false
            while (!validAction) {
                val input = readLine()?.trim() ?: continue
                
                if (input.startsWith("query ")) {
                    handleQuery(input, bots, ui, humanPlayerId)
                    continue
                }
                
                val action = parseHumanAction(input, humanPlayerId, state, ui)
                if (action != null) {
                    val result = game.executeAction(action)
                    when (result) {
                        is ActionResult.Success -> {
                            displayActionResult(action, state, ui)
                            validAction = true
                            
                            // Update bot beliefs
                            for ((botId, bot) in bots) {
                                bot.updateBeliefs(action, game.getPublicState(botId))
                            }
                        }
                        is ActionResult.Error -> {
                            ui.displayError(result.message)
                        }
                    }
                } else {
                    when (input.lowercase()) {
                        "help" -> ui.displayCommands()
                        "quit" -> {
                            println("Thanks for playing!")
                            return@coroutineScope
                        }
                        else -> ui.displayError("Invalid command. Type 'help' for commands.")
                    }
                }
            }
        } else {
            // Bot player's turn
            ui.displayGameState(state, humanPlayerId)
            println()
            
            // Check for query interrupts before bot action
            val queryInput = inputChannel.tryReceive().getOrNull()
            if (queryInput != null && queryInput.startsWith("query ")) {
                handleQuery(queryInput, bots, ui, humanPlayerId)
            }
            
            val bot = bots[currentPlayer]!!
            val publicState = game.getPublicState(currentPlayer)
            val action = bot.chooseAction(publicState)
            
            ui.displayBotAction(currentPlayer, humanPlayerId, action)
            
            val result = game.executeAction(action)
            when (result) {
                is ActionResult.Success -> {
                    displayActionResult(action, state, ui)
                    
                    // Update all bot beliefs
                    for ((botId, botInstance) in bots) {
                        botInstance.updateBeliefs(action, game.getPublicState(botId))
                    }
                }
                is ActionResult.Error -> {
                    ui.displayError("Bot action failed: ${result.message}")
                }
            }
            
            // Small delay to make bot actions visible
            delay(1000)
        }
    }
    
    // Game over
    val finalScore = game.getScore()
    val finalState = game.getCurrentState()
    ui.displayGameState(finalState, humanPlayerId)
    ui.displayGameOver(finalScore, finalState.fuseTokens)
}

fun parseHumanAction(input: String, playerId: Int, state: engine.FullGameState, ui: ConsoleUI): GameAction? {
    val parts = input.trim().split("\\s+".toRegex())
    
    return try {
        when (parts[0].lowercase()) {
            "play" -> {
                if (parts.size < 2) {
                    ui.displayError("Usage: play <index>")
                    return null
                }
                val index = parts[1].toInt()
                PlayCard(playerId, index)
            }
            "discard" -> {
                if (parts.size < 2) {
                    ui.displayError("Usage: discard <index>")
                    return null
                }
                val index = parts[1].toInt()
                DiscardCard(playerId, index)
            }
            "hint" -> {
                if (parts.size < 3) {
                    ui.displayError("Usage: hint <player> <color|rank>")
                    return null
                }
                val targetName = parts[1]
                val targetId = parsePlayerName(targetName, playerId)
                if (targetId == null) {
                    ui.displayError("Invalid player name. Use 'Bot1', 'Bot2', etc.")
                    return null
                }
                
                val clue = parts[2]
                // Try to parse as color first
                val color = try {
                    Color.valueOf(clue.uppercase())
                } catch (e: IllegalArgumentException) {
                    null
                }
                
                if (color != null) {
                    HintColor(playerId, targetId, color)
                } else {
                    // Try to parse as rank
                    val rank = clue.toIntOrNull()
                    if (rank != null && rank in 1..5) {
                        HintRank(playerId, targetId, Rank(rank))
                    } else {
                        ui.displayError("Invalid clue. Use a color (red, yellow, green, blue, white) or rank (1-5)")
                        null
                    }
                }
            }
            else -> null
        }
    } catch (e: Exception) {
        ui.displayError("Invalid command format: ${e.message}")
        null
    }
}

fun parsePlayerName(name: String, humanPlayerId: Int): Int? {
    return when {
        name.lowercase() == "you" && humanPlayerId == 0 -> 0
        name.lowercase().startsWith("bot") -> {
            name.substring(3).toIntOrNull()
        }
        else -> name.toIntOrNull()
    }
}

fun handleQuery(input: String, bots: Map<Int, SmartBot>, ui: ConsoleUI, humanPlayerId: Int) {
    val parts = input.trim().split("\\s+".toRegex())
    if (parts.size < 2) {
        ui.displayError("Usage: query <player>")
        return
    }
    
    val playerName = parts[1]
    val playerId = parsePlayerName(playerName, humanPlayerId)
    
    if (playerId == null) {
        ui.displayError("Invalid player name. Use 'Bot1', 'Bot2', etc.")
        return
    }
    
    if (playerId == humanPlayerId) {
        ui.displayError("You cannot query your own knowledge (you don't know your cards!)")
        return
    }
    
    val bot = bots[playerId]
    if (bot == null) {
        ui.displayError("Player $playerName is not a bot")
        return
    }
    
    val knowledge = bot.getBeliefState()
    val botName = "Bot$playerId"
    ui.displayBotKnowledge(botName, knowledge)
}

fun displayActionResult(action: GameAction, stateBefore: engine.FullGameState, ui: ConsoleUI) {
    when (action) {
        is PlayCard -> {
            val hand = stateBefore.hands[action.actorPlayerId]!!
            val card = hand[action.cardIndex]
            val expectedRank = stateBefore.board[card.color]?.next() ?: Rank(1)
            val success = card.rank == expectedRank
            ui.displayPlayResult(card, success)
        }
        is DiscardCard -> {
            val hand = stateBefore.hands[action.actorPlayerId]!!
            val card = hand[action.cardIndex]
            ui.displayDiscardResult(card)
        }
        is GiveHint -> {
            val targetHand = stateBefore.hands[action.targetPlayerId]!!
            val matchingIndices = when (action) {
                is HintColor -> targetHand.withIndex().filter { it.value.color == action.color }.map { it.index }
                is HintRank -> targetHand.withIndex().filter { it.value.rank == action.rank }.map { it.index }
            }
            ui.displayHintResult(action, matchingIndices)
        }
    }
}

