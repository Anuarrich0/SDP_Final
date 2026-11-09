package loki;
import java.util.Objects;

public final class Player {
    public static final float RADIUS = 13;
    private final LokiVariant variant;
    private float x, y;
    public Player(LokiVariant variant, Level.Cell spawn) {
        this.variant=Objects.requireNonNull(variant); x=spawn.worldX(); y=spawn.worldY();
    }
    public LokiVariant variant() { return variant; }
    public float x() { return x; }
    public float y() { return y; }
    public float speed() { return 150 + variant.getCombatPower()*.4f; }
    public void move(Level level, float dx, float dy) {
        if(!Float.isFinite(dx) || !Float.isFinite(dy)) throw new IllegalArgumentException("Invalid movement");
        int steps=Math.max(1,(int)Math.ceil(Math.max(Math.abs(dx),Math.abs(dy))/5));
        for(int i=0;i<steps;i++) {
            if(level.canStand(x+dx/steps,y,RADIUS)) x+=dx/steps;
            if(level.canStand(x,y+dy/steps,RADIUS)) y+=dy/steps;
        }
    }
}
