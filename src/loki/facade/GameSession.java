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

public final class GameSession {
    public enum State {
        PLAYING, WON, LOST
    }

    public enum Mode {
        SCATTER, CHASE
    }

    private static final float[] MODE_SECONDS = { 7, 20, 7, 20, 5, 20, 5, Float.POSITIVE_INFINITY };

    public record Snapshot(int health, int charges, int required, boolean terminalOn, boolean exitOpen, State state) {
    }

    private final Level level;
    private final Player player;
    private final List<Guard> guards;
    private final Guard leader;
    private final List<Level.Cell> charges;
    private final List<GameListener> listeners = new ArrayList<>();
    private final GuardStrategy scatter = new ScatterStrategy(), chase = new ChaseStrategy();
    private Mode mode = Mode.SCATTER;
    private int modeIndex;
    private float modeTimer = MODE_SECONDS[0];
    private final List<Level.Cell> powers, dots;
    private float frightenedTime;
    private float timeStop, timeStopCooldown, releaseTimer;
    private int releaseDots, globalDots;
    private boolean afterHit;
    private int score;
    private int guardReward = 200;
    private int health = 100;
    private float hurtCooldown;
    private Illusion illusion;
    private float illusionCooldown;
    private float dashCooldown;
    private Player.Direction lastInputDir = Player.Direction.NONE;
    private boolean terminalActive;
    private State state = State.PLAYING;

    public GameSession(Level level, LokiVariant hero) {
        this.level = Objects.requireNonNull(level);
        player = new Player(hero, level.spawn());
        guards = createGuards(level);
        leader = guards.get(0);
        charges = new ArrayList<>(level.charges());
        powers = new ArrayList<>(level.powers());
        dots = new ArrayList<>(level.dots());
    }

    private static List<Guard> createGuards(Level level) {
        GuardType[] types = GuardType.values();
        if (level.guards().size() != 3) {
            throw new IllegalArgumentException("Need three house spawns");
        }
        List<Guard> created = new ArrayList<>();
        created.add(new Guard(level, level.houseExit(), GuardType.B15));
        for (int i = 1; i < types.length; i++) {
            created.add(new Guard(level, level.guards().get(i - 1), types[i]));
        }
        return List.copyOf(created);
    }

    public List<Level.Cell> dots() {
        return List.copyOf(dots);
    }

    public List<Level.Cell> powers() {
        return List.copyOf(powers);
    }

    public float frightenedTime() {
        return frightenedTime;
    }

    public boolean magicBlocked() {
        return level.suppressed(player.currentCell());
    }

    public float timeStop() {
        return timeStop;
    }

    public boolean stopTime() {
        if (state != State.PLAYING || magicBlocked() || timeStopCooldown > 0) {
            return false;
        }
        timeStop = 1.5f;
        timeStopCooldown = 12;
        return true;
    }

    public int score() {
        return score;
    }

    public Level level() {
        return level;
    }

    public Player player() {
        return player;
    }

    public List<Guard> guards() {
        return guards;
    }

    public List<Level.Cell> charges() {
        return List.copyOf(charges);
    }

    public Snapshot snapshot() {
        return new Snapshot(health, level.charges().size() - charges.size(), level.charges().size(), terminalActive,
                isExitOpen(), state);
    }

    public void addListener(GameListener listener) {
        Objects.requireNonNull(listener);
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
        listener.onChanged(snapshot());
    }

    public void removeListener(GameListener listener) {
        listeners.remove(listener);
    }

    public Illusion getIllusion() {
        return illusion;
    }

    public boolean illusion() {
        if (state != State.PLAYING || magicBlocked() || illusionCooldown > 0) {
            return false;
        }
        float duration = 2 + player.variant().getMagicPower() / 40f;
        illusion = new Illusion(player, lastInputDir, duration);
        for (Guard guard : guards) {
            guard.distract(level, player.currentCell());
        }
        illusionCooldown = 7;
        return true;
    }

    public boolean dash() {
        if (state != State.PLAYING || magicBlocked() || dashCooldown > 0) {
            return false;
        }
        boolean started = player.startDash(lastInputDir);
        if (started) {
            dashCooldown = 2;
        }
        return started;
    }

    public boolean isExitOpen() {
        return charges.isEmpty() && terminalActive && dots.isEmpty() && powers.isEmpty();
    }

    public boolean activateTerminal() {
        if (state != State.PLAYING || terminalActive || !charges.isEmpty()) {
            return false;
        }
        Level.Cell terminal = level.terminal();
        if (Math.hypot(player.x() - terminal.worldX(), player.y() - terminal.worldY()) > 50) {
            return false;
        }
        terminalActive = true;
        for (GameListener listener : List.copyOf(listeners)) {
            listener.onChanged(snapshot());
        }
        return true;
    }

    public void update(float deltaTime, Player.Direction inputDir) {
        if (!Float.isFinite(deltaTime) || deltaTime <= 0 || deltaTime > .05f) {
            throw new IllegalArgumentException("Invalid simulation input");
        }
        if (state != State.PLAYING) {
            return;
        }
        Snapshot before = snapshot();

        if (inputDir != Player.Direction.NONE) {
            lastInputDir = inputDir;
        }

        player.update(level, inputDir, deltaTime);

        int eaten = collectDots();
        boolean frozen = updateAbilities(deltaTime);
        updateGuardModes(deltaTime, frozen);
        releaseDots += eaten;
        globalDots += eaten;
        if (eaten > 0) {
            releaseTimer = 0;
        }
        if (!frozen) {
            updateHouse(deltaTime);
        }
        updateGuards(deltaTime, frozen);
        if (health == 0) {
            state = State.LOST;
        } else if (isExitOpen()
                && Math.hypot(player.x() - level.exit().worldX(), player.y() - level.exit().worldY()) < 26) {
            state = State.WON;
        }
        if (!before.equals(snapshot())) {
            for (GameListener listener : List.copyOf(listeners)) {
                listener.onChanged(snapshot());
            }
        }
    }

    private int collectDots() {
        int beforeDots = dots.size();
        dots.removeIf(c -> Math.hypot(player.x() - c.worldX(), player.y() - c.worldY()) < 16);
        int eaten = beforeDots - dots.size();
        score += eaten * 10;
        return eaten;
    }

    private boolean updateAbilities(float deltaTime) {
        timeStopCooldown = Math.max(0, timeStopCooldown - deltaTime);
        if (magicBlocked()) {
            timeStop = 0;
            illusion = null;
            player.cancelDash();
        }
        boolean frozen = timeStop > 0;
        timeStop = Math.max(0, timeStop - deltaTime);
        dashCooldown = Math.max(0, dashCooldown - deltaTime);
        charges.removeIf(c -> Math.hypot(player.x() - c.worldX(), player.y() - c.worldY()) < 26);
        hurtCooldown = Math.max(0, hurtCooldown - deltaTime);
        illusionCooldown = Math.max(0, illusionCooldown - deltaTime);
        if (illusion != null) {
            illusion.update(level, deltaTime);
            if (illusion.getTimeLeft() == 0) {
                illusion = null;
            }
        }
        return frozen;
    }

    private void updateGuardModes(float deltaTime, boolean frozen) {
        if (!frozen && frightenedTime > 0) {
            frightenedTime = Math.max(0, frightenedTime - deltaTime);
            if (frightenedTime == 0) {
                for (Guard guard : guards) {
                    guard.setFrightened(false);
                }
            }
        } else if (!frozen) {
            updateMode(deltaTime);
        }
        if (powers.removeIf(c -> Math.hypot(player.x() - c.worldX(), player.y() - c.worldY()) < 20)) {
            frightenedTime = 6;
            guardReward = 200;
            score += 50;
            for (Guard guard : guards) {
                guard.setFrightened(true);
                guard.allowReverse();
            }
        }
    }

    private void updateGuards(float deltaTime, boolean frozen) {
        HuntContext context = huntContext();
        for (Guard guard : guards) {
            if (!frozen) {
                guard.update(level, context, deltaTime);
            }
            if (!frozen && guard.dangerous() && level.distance(player.x(), player.y(), guard.x(), guard.y()) < 27) {
                if (guard.isFrightened()) {
                    score += guardReward;
                    guardReward = Math.min(guardReward * 2, 1600);
                    guard.caught();
                } else if (hurtCooldown == 0) {
                    handlePlayerHit();
                    break;
                }
            }
        }
    }

    private void handlePlayerHit() {
        health = Math.max(0, health - player.variant().getHitDamage());
        hurtCooldown = 2;
        player.setPosition(level.spawn());
        illusion = null;
        frightenedTime = 0;
        timeStop = 0;
        afterHit = true;
        releaseDots = 0;
        globalDots = 0;
        releaseTimer = 0;
        modeIndex = 0;
        mode = Mode.SCATTER;
        modeTimer = MODE_SECONDS[0];
        for (Guard other : guards) {
            other.reset();
            other.setStrategy(scatter);
        }
    }

    private void updateHouse(float deltaTime) {
        releaseTimer += deltaTime;
        if (guards.stream().anyMatch(g -> g.state() == Guard.State.LEAVING)) {
            return;
        }
        for (Guard guard : guards) {
            if (guard.state() != Guard.State.WAITING) {
                continue;
            }
            int limit = switch (guard.type()) {
            case B15 -> 0;
            case D90 -> afterHit ? 7 : 0;
            case C20 -> afterHit ? 17 : 30;
            case X5 -> afterHit ? 32 : 60;
            };
            if ((afterHit ? globalDots : releaseDots) >= limit || releaseTimer >= 4) {
                guard.release();
                releaseDots = 0;
                releaseTimer = 0;
            }
            break;
        }
    }

    public Mode mode() {
        return mode;
    }

    private void updateMode(float deltaTime) {
        modeTimer -= deltaTime;
        if (modeTimer > 0) {
            return;
        }
        modeIndex = Math.min(modeIndex + 1, MODE_SECONDS.length - 1);
        mode = modeIndex % 2 == 0 ? Mode.SCATTER : Mode.CHASE;
        modeTimer += MODE_SECONDS[modeIndex];
        GuardStrategy next = mode == Mode.SCATTER ? scatter : chase;
        for (Guard guard : guards) {
            guard.setStrategy(next);
            guard.allowReverse();
        }
    }

    private HuntContext huntContext() {
        Player.Direction facing = player.currentDirection() != Player.Direction.NONE ? player.currentDirection()
                : lastInputDir;
        Level.Cell prey = illusion != null ? cellOf(illusion.x(), illusion.y()) : player.currentCell();
        return new HuntContext(prey, illusion != null ? illusion.direction() : facing, leader.currentCell(),
                illusion != null);
    }

    private static Level.Cell cellOf(float x, float y) {
        return new Level.Cell((int) Math.floor(x / Level.TILE), (int) Math.floor(y / Level.TILE));
    }
}