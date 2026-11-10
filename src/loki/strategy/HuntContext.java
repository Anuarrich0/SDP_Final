package loki.strategy;

import loki.builder.Level;
import loki.game.Player;

/** Per-frame facts a chase rule needs: where the prey is, how it faces and where the lead hunter is. */
public record HuntContext(Level.Cell prey, Player.Direction preyDirection, Level.Cell leader, boolean decoy) {
    public HuntContext(Level.Cell prey, Player.Direction preyDirection, Level.Cell leader) {
        this(prey, preyDirection, leader, false);
    }
}
