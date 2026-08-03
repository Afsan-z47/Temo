package com.projectenigma.model;

import java.util.List;

public record TurnResult(BattleOutcome outcome, List<String> messages, boolean actionAccepted) {
}
