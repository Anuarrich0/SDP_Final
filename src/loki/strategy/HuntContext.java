package loki.strategy;

import loki.builder.Level;
import loki.game.Player;

public record HuntContext(Level.Cell prey, Player.Direction preyDirection, Level.Cell leader, boolean decoy) {
}
