package loki;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Facade: one entry point for game rules, without graphics or keyboard dependencies. */
public final class GameSession {
    public enum State { PLAYING, WON, LOST }
    public record Snapshot(int health,int charges,int required,State state) { }
    private final Level level;
    private final Player player;
    private final List<Guard> guards;
    private final List<Level.Cell> charges;
    private final List<GameListener> listeners=new ArrayList<>();
    private final GuardStrategy patrol=new PatrolStrategy(), chase=new ChaseStrategy();
    private int health=100;
    private float hurtCooldown;
    private Illusion illusion;
    private float illusionCooldown;
    private float dashTime;
    private float dashCooldown;
    private float directionX = 1;
    private float directionY;
    private State state=State.PLAYING;
    public GameSession(Level level,LokiVariant hero) {
        this.level=Objects.requireNonNull(level); player=new Player(hero,level.spawn());
        guards=level.guards().stream().map(spawn -> new Guard(level, spawn)).toList();
        charges=new ArrayList<>(level.charges());
    }
    public Level level() { return level; }
    public Player player() { return player; }
    public List<Guard> guards() { return guards; }
    public List<Level.Cell> charges() { return List.copyOf(charges); }
    public Snapshot snapshot() { return new Snapshot(health,level.charges().size()-charges.size(),level.charges().size(),state); }
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

    // SPACE starts a short dash in the last movement direction.
    public boolean dash() {
        if (state != State.PLAYING || dashCooldown > 0) {
            return false;
        }
        dashTime = 0.18f;
        dashCooldown = 2;
        return true;
    }

    public void update(float dt,float dx,float dy) {
        if(!Float.isFinite(dt)||dt<=0||dt>.05f||!Float.isFinite(dx)||!Float.isFinite(dy))
            throw new IllegalArgumentException("Invalid simulation input");
        if(state!=State.PLAYING) return;
        Snapshot before=snapshot();
        float length = (float) Math.hypot(dx, dy);
        if (length > 0 && dashTime == 0) {
            directionX = dx / length;
            directionY = dy / length;
        }

        float dashStep = Math.min(dt, dashTime);
        if (dashStep > 0) {
            player.move(level, directionX * 500 * dashStep, directionY * 500 * dashStep);
        }
        float walkStep = dt - dashStep;
        if (length > 0 && walkStep > 0) {
            player.move(level, dx / length * player.speed() * walkStep,
                    dy / length * player.speed() * walkStep);
        }
        dashTime = Math.max(0, dashTime - dt);
        dashCooldown = Math.max(0, dashCooldown - dt);
        charges.removeIf(c -> Math.hypot(player.x()-c.worldX(),player.y()-c.worldY())<26);
        hurtCooldown=Math.max(0,hurtCooldown-dt);
        illusionCooldown = Math.max(0, illusionCooldown - dt);
        if (illusion != null) {
            illusion.update(dt);
            if (illusion.getTimeLeft() == 0) illusion = null;
        }
        for(Guard guard:guards) {
            if (illusion != null && guard.canSee(level, illusion.x(), illusion.y())) {
                guard.remember(illusion.x(), illusion.y());
            } else if (guard.canSee(level, player.x(), player.y())) {
                guard.remember(player.x(), player.y());
            } else {
                guard.forget(dt);
            }
            guard.setStrategy(guard.isSearching() ? chase : patrol);
            guard.update(level, guard.getLastSeenX(), guard.getLastSeenY(), dt);
            if(hurtCooldown==0 && Math.hypot(player.x()-guard.x(),player.y()-guard.y())<27) {
                health=Math.max(0,health-player.variant().getFailurePenalty()*2); hurtCooldown=1;
            }
        }
        if(health==0) state=State.LOST;
        else if(charges.isEmpty() && Math.hypot(player.x()-level.exit().worldX(),player.y()-level.exit().worldY())<26)
            state=State.WON;
        if(!before.equals(snapshot())) for(GameListener listener:List.copyOf(listeners)) listener.onChanged(snapshot());
    }
}
