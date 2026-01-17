# Hanabi Card Game - Console Implementation

A complete console-based implementation of the cooperative card game Hanabi in Kotlin, featuring a single human player and SmartBot AI opponents with intelligent decision-making and belief tracking.

## Features

- **Complete Hanabi Rules**: Implements the standard 50-card deck with proper token economy
- **SmartBot AI**: Intelligent bots that use:
  - Chop convention (rightmost unclued card)
  - Save clues for critical cards
  - Play clues for playable cards
  - Belief tracking with positive and negative inference
- **Interactive Console UI**: Colorized output with clear game state display
- **Query System**: Ask bots what they know about their cards at any time
- **Coroutine-based Input**: Non-blocking input handling
- **Comprehensive Testing**: Unit tests for all major components

## Building and Running

### Prerequisites
- JDK 17 or higher
- Gradle 9.2.1 or higher (or use included wrapper)

### Build
```bash
gradle build
```

### Run
```bash
gradle run
```

Or use the distribution:
```bash
gradle distZip
unzip build/distributions/hanabi-1.0-SNAPSHOT.zip
./hanabi-1.0-SNAPSHOT/bin/hanabi
```

### Test
```bash
gradle test
```

## How to Play

### Game Setup
1. Enter the number of players (2-5)
2. You are always Player 0; others are SmartBots

### Commands
- `play <index>` - Play a card from your hand (e.g., `play 0`)
- `discard <index>` - Discard a card to gain a clue token (e.g., `discard 3`)
- `hint <player> <clue>` - Give a hint to another player
  - Color hints: `hint Bot1 red`, `hint Bot2 blue`
  - Rank hints: `hint Bot1 1`, `hint Bot2 5`
- `query <player>` - See what a bot knows about their cards (e.g., `query Bot1`)
- `help` - Show command list
- `quit` - Exit the game

### Rules Summary

**Objective**: Cooperatively build five firework stacks (one per color) from 1 to 5.

**Deck Composition**:
- 5 colors: Red, Yellow, Green, Blue, White
- Per color: Three 1s, Two 2s, Two 3s, Two 4s, One 5
- Total: 50 cards

**Hand Size**:
- 2-3 players: 5 cards each
- 4-5 players: 4 cards each

**Tokens**:
- **Clue Tokens** (Blue): Start with 8, max 8
  - Spend 1 to give a hint
  - Gain 1 by discarding
  - Gain 1 by successfully playing a 5
- **Fuse Tokens** (Red): Start with 0, max 3
  - Gain 1 for each misplayed card
  - 3 fuses = instant game over (score 0)

**Actions** (choose one per turn):
1. **Play a card**: Attempt to add it to a firework stack
2. **Discard a card**: Remove it from hand, gain a clue token (if not at max)
3. **Give a hint**: Point out ALL cards of a specific color OR rank in a teammate's hand

**Winning**:
- Perfect score (25): Complete all five firework stacks
- Good game: Any score above 15
- Explosion (0): Three fuse tokens

**End Conditions**:
1. All five stacks reach 5 (perfect game)
2. Three fuse tokens (explosion)
3. Deck runs out: Each player gets one final turn

## Architecture

### Package Structure
```
src/main/kotlin/
├── Main.kt                    # Game loop with coroutine-based input
├── model/                     # Domain model
│   ├── Color.kt              # Card colors enum
│   ├── Rank.kt               # Card ranks (1-5)
│   ├── CardIdentity.kt       # Card representation
│   └── GameAction.kt         # Action sealed hierarchy
├── engine/                    # Game engine
│   ├── GameEngine.kt         # Core game logic
│   ├── FullGameState.kt      # Complete state (god view)
│   ├── PublicGameState.kt    # Visible state for players
│   └── ActionValidator.kt    # Action validation
├── knowledge/                 # Belief tracking
│   ├── CardKnowledge.kt      # Per-card knowledge
│   └── BeliefTracker.kt      # Inference engine
├── ai/                        # Artificial intelligence
│   └── SmartBot.kt           # Convention-aware AI
└── ui/                        # User interface
    └── ConsoleUI.kt          # Console display

src/test/kotlin/
├── DeckTest.kt               # Deck composition tests
├── ActionValidatorTest.kt   # Validation logic tests
├── BeliefTrackerTest.kt     # Knowledge inference tests
└── GameEngineTest.kt         # Game mechanics tests
```

### Key Design Decisions

1. **Immutable State**: All state transitions use `copy()` to ensure immutability
2. **Sealed Classes**: Action hierarchy uses sealed interfaces for exhaustive `when` handling
3. **Separation of Concerns**: Bots never see their own cards directly (realistic constraint)
4. **Belief Tracking**: Implements both positive (this IS red) and negative (this is NOT red) inference
5. **Coroutines**: Enables non-blocking query system during bot turns

## SmartBot AI

The SmartBot uses a priority-based decision system:

### Decision Priority (highest to lowest):
1. **Play known-playable cards** - 100% certain it will succeed
2. **Play clued cards** - Probably playable based on finesse logic
3. **Give save clue** - Prevent critical cards from being discarded
4. **Give play clue** - Enable teammate to play
5. **Discard chop** - Rightmost unclued card

### Conventions Implemented:
- **Chop**: Rightmost unclued card is the default discard target
- **Save Clues**: Hints touching the chop indicate saving that card
- **Play Clues**: Hints indicating immediately playable cards
- **Critical Cards**: Last copy of a card (e.g., all 5s, last remaining copy)

### Belief Tracking:
- **Positive Inference**: "This card IS red" → possibleColors = {RED}
- **Negative Inference**: "Cards at indices 0,2 are red" → card 1 is NOT red
- **Elimination**: Track visible cards to narrow possibilities
- **Clue Memory**: Remember all positive color/rank clues received

## Example Game Session

```
╔══════════════════════════════════════════════════════════════╗
║                         HANABI                                ║
╚══════════════════════════════════════════════════════════════╝
Enter number of players (2-5): 2

╔══════════════════════════════════════════════════════════════╗
║                         HANABI                                ║
╠══════════════════════════════════════════════════════════════╣
║ Board:  R[-] Y[-] G[-] B[-] W[-]                             ║
║ Clues: 8/8    Fuses: 0/3    Deck: 40                        ║
╠══════════════════════════════════════════════════════════════╣
║ Discards:                                                     ║
╠══════════════════════════════════════════════════════════════╣
║ Bot1: [R1] [B3] [Y2] [W4] [G1]                              ║
║ You:  [??] [??] [??] [??] [??]  (indices 0-4)               ║
╚══════════════════════════════════════════════════════════════╝

Your turn! Enter a command:
> hint Bot1 1
Hint given: 1 - matching cards at indices: 0, 4

Bot1 plays card at index 0
✓ Successfully played RED 1!

Your turn! Enter a command:
> query Bot1

╔══════════════════════════════════════════════════════════════╗
║ Bot1's Knowledge                                             ║
╠══════════════════════════════════════════════════════════════╣
║   Card 0: Color={any}, Rank={2,3,4,5} - Unknown             ║
║   Card 1: Color={any}, Rank={2,3,4,5} - Unknown             ║
║   Card 2: Color={any}, Rank={2,3,4,5} - Unknown             ║
║   Card 3: Color={any}, Rank=1 (certain) - Clued             ║
║   Card 4: Color={any}, Rank={any} - Unknown                 ║
╚══════════════════════════════════════════════════════════════╝
```

## Testing

Run all tests:
```bash
gradle test
```

Test coverage includes:
- **DeckTest**: Validates 50-card deck with correct distribution
- **ActionValidatorTest**: Tests all validation rules
- **BeliefTrackerTest**: Verifies inference logic
- **GameEngineTest**: Tests state transitions and game flow

## Future Enhancements

Potential improvements for the implementation:
- More sophisticated Finesse detection
- Prompt convention support
- Bluff detection
- Rainbow suit variant
- Multiplayer over network
- Replay system
- Statistics tracking

## License

This is an educational implementation for learning Kotlin and game AI.

## Credits

Hanabi was designed by Antoine Bauza and published by Abacus Spiele.
