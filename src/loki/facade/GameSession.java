package loki.facade;

import loki.builder.Level;
import loki.factory.LokiVariant;
import loki.game.Guard;
import loki.game.GuardType;
import loki.game.Illusion;
import loki.game.Player;
import loki.observer.GameListener;
import loki.strategy.ChaseStrategy;
import loki.strategy.GuardStrategy;
import loki.strategy.HuntContext;
import loki.strategy.ScatterStrategy;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Facade: one entry point for game rules, without graphics or keyboard dependencies. */
public final class GameSession {
    public enum State { PLAYING, WON, LOST }
    /** The global hunt cycle from the brief: 7 seconds SCATTER, then 20 seconds CHASE, repeat. */
    public enum Mode { SCATTER, CHASE }
    private static final float SCATTER_SECONDS=7, CHASE_SECONDS=20;
    public record Snapshot(int health,int charges,int required,boolean terminalOn,boolean exitOpen,State state) { }
    private final Level level;
    private final Player player;
    private final List<Guard> guards;
    private final Guard leader;
    private final List<Level.Cell> charges;
    private final List<GameListener> listeners=new ArrayList<>();
    private final GuardStrategy scatter=new ScatterStrategy(), chase=new ChaseStrategy();
    private Mode mode=Mode.SCATTER;
    private float modeTimer=SCATTER_SECONDS;
    private int health=100;
    private float hurtCooldown;
    private Illusion illusion;
    private float illusionCooldown;
    private float dashCooldown;
    private Player.Direction lastInputDir = Player.Direction.NONE;
    private boolean terminalActive;
    private State state=State.PLAYING;

    public GameSession(Level level,LokiVariant hero) {
        this.level=Objects.requireNonNull(level); player=new Player(hero,level.spawn());
        guards=createGuards(level);
        leader=guards.stream().filter(guard->guard.type()==GuardType.B15).findFirst().orElseThrow();
        charges=new ArrayList<>(level.charges());
    }

    /** One spawn per hunter. The cell is the G on the training map, not the list index. */
    private static List<Guard> createGuards(Level level) {
        List<Guard> created = List.of(
                guard(level, 8, 7, GuardType.B15),
                guard(level, 16, 8, GuardType.D90),
                guard(level, 3, 4, GuardType.C20),
                guard(level, 12, 1, GuardType.X5));
        if (created.size() != level.guards().size()) {
            throw new IllegalArgumentException("Every G on the map needs exactly one guard type");
        }
        return created;
    }

    private static Guard guard(Level level, int x, int y, GuardType type) {
        Level.Cell cell = new Level.Cell(x, y);
        if (!level.guards().contains(cell)) {
            throw new IllegalArgumentException("No G spawn at " + x + ", " + y);
        }
        return new Guard(level, cell, type);
    }

    public Level level() { return level; }
    public Player player() { return player; }
    public List<Guard> guards() { return guards; }
    public List<Level.Cell> charges() { return List.copyOf(charges); }
    public Snapshot snapshot() { return new Snapshot(health,level.charges().size()-charges.size(),level.charges().size(),terminalActive,isExitOpen(),state); }
    public void addListener(GameListener listener) {
        Objects.requireNonNull(listener);
        if(!listeners.contains(listener)) listeners.add(listener);
        listener.onChanged(snapshot());
    }
    public void removeListener(GameListener listener) { listeners.remove(listener); }
    public float getDashCooldown() { return dashCooldown; }
    public float getIllusionCooldown() { return illusionCooldown; }
    public Illusion getIllusion() { return illusion; }

    public boolean illusion() {
        if (state != State.PLAYING || illusionCooldown > 0) return false;
        float duration = 2 + player.variant().getMagicPower() / 40f;
        illusion = new Illusion(player.x(), player.y(), duration);
        illusionCooldown = 7;
        return true;
    }

    // SPACE starts a short dash in the current movement direction.
    public boolean dash() {
        if (state != State.PLAYING || dashCooldown > 0) {
            return false;
        }
        boolean started = player.startDash(lastInputDir);
        if (started) dashCooldown = 2;
        return started;
    }

    /** The way out opens only after every TemPad is collected and the terminal is switched on. */
    public boolean isExitOpen() { return charges.isEmpty() && terminalActive; }

    /** F: switch on the terminal, but only while standing next to it and holding all the TemPads. */
    public boolean activateTerminal() {
        if (state != State.PLAYING || terminalActive || !charges.isEmpty()) return false;
        Level.Cell terminal = level.terminal();
        if (Math.hypot(player.x()-terminal.worldX(),player.y()-terminal.worldY()) > 50) return false;
        terminalActive = true;
        for (GameListener listener : List.copyOf(listeners)) listener.onChanged(snapshot());
        return true;
    }

    public void update(float dt, Player.Direction inputDir) {
        if(!Float.isFinite(dt)||dt<=0||dt>.05f)
            throw new IllegalArgumentException("Invalid simulation input");
        if(state!=State.PLAYING) return;
        Snapshot before=snapshot();

        if (inputDir != Player.Direction.NONE) {
            lastInputDir = inputDir;
        }

        player.update(level, inputDir, dt);

        dashCooldown = Math.max(0, dashCooldown - dt);
        charges.removeIf(c -> Math.hypot(player.x()-c.worldX(),player.y()-c.worldY())<26);
        hurtCooldown=Math.max(0,hurtCooldown-dt);
        illusionCooldown = Math.max(0, illusionCooldown - dt);
        if (illusion != null) {
            illusion.update(dt);
            if (illusion.getTimeLeft() == 0) illusion = null;
        }
        updateMode(dt);
        HuntContext context = huntContext();
        for(Guard guard:guards) {
            guard.update(level, context, dt);
            if(hurtCooldown==0 && Math.hypot(player.x()-guard.x(),player.y()-guard.y())<27) {
                health=Math.max(0,health-player.variant().getFailurePenalty()*2); hurtCooldown=1;
            }
        }
        if(health==0) state=State.LOST;
        else if(isExitOpen() && Math.hypot(player.x()-level.exit().worldX(),player.y()-level.exit().worldY())<26)
            state=State.WON;
        if(!before.equals(snapshot())) for(GameListener listener:List.copyOf(listeners)) listener.onChanged(snapshot());
    }

    public Mode mode() { return mode; }

    /** Flips SCATTER <-> CHASE on a timer. It only runs while playing, so pause freezes it and R resets it. */
    private void updateMode(float dt) {
        modeTimer -= dt;
        if (modeTimer > 0) return;
        mode = mode == Mode.SCATTER ? Mode.CHASE : Mode.SCATTER;
        modeTimer = mode == Mode.SCATTER ? SCATTER_SECONDS : CHASE_SECONDS;
        GuardStrategy next = mode == Mode.SCATTER ? scatter : chase;
        for (Guard guard : guards) {
            guard.setStrategy(next);
            guard.allowReverse(); // the brief lets hunters turn around when the mode changes
        }
    }

    /** CHASE always knows where Loki is; a live illusion is an explicit decoy and takes priority. */
    private HuntContext huntContext() {
        Player.Direction facing = player.currentDirection() != Player.Direction.NONE
                ? player.currentDirection() : lastInputDir;
        Level.Cell prey = illusion != null
                ? cellOf(illusion.x(), illusion.y()) : player.currentCell();
        return new HuntContext(prey, facing, leader.currentCell());
    }

    private static Level.Cell cellOf(float x, float y) {
        return new Level.Cell((int) Math.floor(x / Level.TILE), (int) Math.floor(y / Level.TILE));
    }
}