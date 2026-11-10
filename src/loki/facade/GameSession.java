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
    /** The first-level scatter/chase schedule. The last chase has no time limit. */
    public enum Mode { SCATTER, CHASE }
    private static final float[] MODE_SECONDS = {7,20,7,20,5,20,5,Float.POSITIVE_INFINITY};
    public record Snapshot(int health,int charges,int required,boolean terminalOn,boolean exitOpen,State state) { }
    private final Level level;
    private final Player player;
    private final List<Guard> guards;
    private final Guard leader;
    private final List<Level.Cell> charges;
    private final List<GameListener> listeners=new ArrayList<>();
    private final GuardStrategy scatter=new ScatterStrategy(), chase=new ChaseStrategy();
    private Mode mode=Mode.SCATTER;
    private int modeIndex;
    private float modeTimer=MODE_SECONDS[0];
    private final List<Level.Cell> powers, dots;
    private float frightenedTime;
    private float timeStop, timeStopCooldown, releaseTimer;
    private int releaseDots, globalDots;
    private boolean afterHit;
    private int score, combo;
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
        powers=new ArrayList<>(level.powers());
        dots=new ArrayList<>(level.dots());
    }

    /** Assign one hunter type to each G in map reading order. */
    private static List<Guard> createGuards(Level level) {
        GuardType[] types = GuardType.values();
        if (level.guards().size()!=3) throw new IllegalArgumentException("Need three house spawns");
        List<Guard> created=new ArrayList<>();
        created.add(new Guard(level,level.houseExit(),GuardType.B15));
        for (int i=1; i<types.length; i++)
            created.add(new Guard(level,level.guards().get(i-1),types[i]));
        return List.copyOf(created);
    }

    public List<Level.Cell> dots() { return List.copyOf(dots); }
    public List<Level.Cell> powers() { return List.copyOf(powers); }
    public float frightenedTime() { return frightenedTime; }
    public boolean magicBlocked() { return level.suppressed(player.currentCell()); }
    public float timeStop() { return timeStop; }
    public float timeStopCooldown() { return timeStopCooldown; }
    public boolean stopTime() {
        if (state!=State.PLAYING || magicBlocked() || timeStopCooldown>0) return false;
        timeStop=1.5f; timeStopCooldown=12; return true;
    }
    public int score() { return score; }
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
        if (state != State.PLAYING || magicBlocked() || illusionCooldown > 0) return false;
        float duration = 2 + player.variant().getMagicPower() / 40f;
        illusion = new Illusion(player,lastInputDir,duration);
        for (Guard guard:guards) guard.distract(level,player.currentCell());
        illusionCooldown = 7;
        return true;
    }

    // SPACE starts a short dash in the current movement direction.
    public boolean dash() {
        if (state != State.PLAYING || magicBlocked() || dashCooldown > 0) {
            return false;
        }
        boolean started = player.startDash(lastInputDir);
        if (started) dashCooldown = 2;
        return started;
    }

    /** Collect every dot, power sphere and TemPad, then use the terminal to leave. */
    public boolean isExitOpen() { return charges.isEmpty() && terminalActive && dots.isEmpty() && powers.isEmpty(); }

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

        int beforeDots = dots.size();
        dots.removeIf(c -> Math.hypot(player.x()-c.worldX(), player.y()-c.worldY()) < 16);
        int eaten=beforeDots-dots.size();
        score += eaten*10;
        timeStopCooldown=Math.max(0,timeStopCooldown-dt);
        if (magicBlocked()) { timeStop=0; illusion=null; player.cancelDash(); }
        boolean frozen=timeStop>0;
        timeStop=Math.max(0,timeStop-dt);
        dashCooldown = Math.max(0, dashCooldown - dt);
        charges.removeIf(c -> Math.hypot(player.x()-c.worldX(),player.y()-c.worldY())<26);
        hurtCooldown=Math.max(0,hurtCooldown-dt);
        illusionCooldown = Math.max(0, illusionCooldown - dt);
        if (illusion != null) {
            illusion.update(level,dt);
            if (illusion.getTimeLeft() == 0) illusion = null;
        }
        if (!frozen && frightenedTime > 0) {
            frightenedTime = Math.max(0, frightenedTime-dt);
            if (frightenedTime == 0) for (Guard guard : guards) guard.setFrightened(false);
        } else if (!frozen) updateMode(dt);
        if (powers.removeIf(c -> Math.hypot(player.x()-c.worldX(), player.y()-c.worldY()) < 20)) {
            frightenedTime = 6; combo = 0; score += 50;
            for (Guard guard : guards) {
                guard.setFrightened(true); guard.allowReverse();
            }
        }
        releaseDots+=eaten; globalDots+=eaten;
        if (eaten>0) releaseTimer=0;
        if (!frozen) updateHouse(dt);
        HuntContext context = huntContext();
        for(Guard guard:guards) {
            if (!frozen) guard.update(level, context, dt);
            if (!frozen && guard.dangerous() && level.distance(player.x(),player.y(),guard.x(),guard.y())<27) {
                if (guard.isFrightened()) {
                    score += 200 << Math.min(combo++, 3);
                    guard.caught();
                } else if (hurtCooldown == 0) {
                    health=Math.max(0,health-player.variant().getFailurePenalty()*2);
                    hurtCooldown=2;
                    player.setPosition(level.spawn());
                    illusion=null; frightenedTime=0; timeStop=0;
                    afterHit=true; releaseDots=0; globalDots=0; releaseTimer=0;
                    modeIndex=0; mode=Mode.SCATTER; modeTimer=MODE_SECONDS[0];
                    for (Guard other : guards) { other.reset(); other.setStrategy(scatter); }
                    break;
                }
            }
        }
        if(health==0) state=State.LOST;
        else if(isExitOpen() && Math.hypot(player.x()-level.exit().worldX(),player.y()-level.exit().worldY())<26)
            state=State.WON;
        if(!before.equals(snapshot())) for(GameListener listener:List.copyOf(listeners)) listener.onChanged(snapshot());
    }

    private void updateHouse(float dt) {
        releaseTimer+=dt;
        if (guards.stream().anyMatch(g -> g.state()==Guard.State.LEAVING)) return;
        for (Guard guard : guards) {
            if (guard.state()!=Guard.State.WAITING) continue;
            int limit=switch (guard.type()) {
                case B15 -> 0; case D90 -> afterHit ? 7 : 0;
                case C20 -> afterHit ? 17 : 30; case X5 -> afterHit ? 32 : 60;
            };
            if ((afterHit ? globalDots : releaseDots)>=limit || releaseTimer>=4) {
                guard.release(); releaseDots=0; releaseTimer=0;
            }
            break;
        }
    }

    public Mode mode() { return mode; }

    /** Flips SCATTER <-> CHASE on a timer. It only runs while playing, so pause freezes it and R resets it. */
    private void updateMode(float dt) {
        modeTimer -= dt;
        if (modeTimer > 0) return;
        modeIndex = Math.min(modeIndex+1, MODE_SECONDS.length-1);
        mode = modeIndex % 2 == 0 ? Mode.SCATTER : Mode.CHASE;
        modeTimer += MODE_SECONDS[modeIndex];
        GuardStrategy next = mode == Mode.SCATTER ? scatter : chase;
        for (Guard guard : guards) {
            guard.setStrategy(next);
            guard.allowReverse(); // reverse when the mode changes
        }
    }

    /** CHASE always knows where Loki is; a live illusion is an explicit decoy and takes priority. */
    private HuntContext huntContext() {
        Player.Direction facing = player.currentDirection() != Player.Direction.NONE
                ? player.currentDirection() : lastInputDir;
        Level.Cell prey = illusion != null
                ? cellOf(illusion.x(), illusion.y()) : player.currentCell();
        return new HuntContext(prey,illusion!=null ? illusion.direction() : facing,leader.currentCell(),illusion!=null);
    }

    private static Level.Cell cellOf(float x, float y) {
        return new Level.Cell((int) Math.floor(x / Level.TILE), (int) Math.floor(y / Level.TILE));
    }
}