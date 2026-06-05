// Exporting as package
package gamemechanics;

/* 
    Event types for the football simulation
    Enum was used to simplify event type handling and reduce erros from String types 
*/
public enum EventType {
    KICKOFF,
    SHOT,
    BIG_CHANCE,
    GOAL,
    SAVE,
    MISS,
    FOUL,
    YELLOW_CARD,
    RED_CARD,
    PENALTY,
    HALF_TIME,
    FULL_TIME,
    COMMENTARY
}
