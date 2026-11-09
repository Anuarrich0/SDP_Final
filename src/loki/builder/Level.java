package loki.builder;
import java.util.ArrayDeque;
import java.util.List;

/** Builder constructs and validates a grid; coordinates use LibGDX's bottom-left origin. */
public final class Level {
    public static final int TILE = 48;
    public record Cell(int x, int y) {
        public float worldX() { return (x + .5f) * TILE; }
        public float worldY() { return (y + .5f) * TILE; }
    }
    private final List<String> rows;
    private final Cell spawn, exit;
    private final List<Cell> charges, guards;

    private Level(List<String> rows) {
        this.rows = List.copyOf(rows);
        var starts = find('S'); var exits = find('P');
        charges = find('C'); guards = find('G');
        if (starts.size() != 1 || exits.size() != 1 || charges.isEmpty())
            throw new IllegalArgumentException("Need one spawn, one portal and at least one charge");
        spawn = starts.get(0); exit = exits.get(0);
        boolean[][] seen = new boolean[height()][width()];
        var queue = new ArrayDeque<Cell>(); queue.add(spawn); seen[spawn.y()][spawn.x()] = true;
        while (!queue.isEmpty()) {
            Cell c = queue.remove();
            for (Cell n : List.of(new Cell(c.x()+1,c.y()), new Cell(c.x()-1,c.y()),
                    new Cell(c.x(),c.y()+1), new Cell(c.x(),c.y()-1))) {
                if (!wall(n.x(),n.y()) && !seen[n.y()][n.x()]) {
                    seen[n.y()][n.x()] = true; queue.add(n);
                }
            }
        }
        if (!seen[exit.y()][exit.x()] || charges.stream().anyMatch(c -> !seen[c.y()][c.x()])
                || guards.stream().anyMatch(c -> !seen[c.y()][c.x()]))
            throw new IllegalArgumentException("Unreachable map objective");
    }
    private List<Cell> find(char symbol) {
        var result = new java.util.ArrayList<Cell>();
        for (int y=0; y<height(); y++) for (int x=0; x<width(); x++)
            if (rows.get(height()-1-y).charAt(x) == symbol) result.add(new Cell(x,y));
        return List.copyOf(result);
    }
    public int width() { return rows.get(0).length(); }
    public int height() { return rows.size(); }
    public Cell spawn() { return spawn; }
    public Cell exit() { return exit; }
    public List<Cell> charges() { return charges; }
    public List<Cell> guards() { return guards; }
    public boolean wall(int x, int y) {
        return x<0 || y<0 || x>=width() || y>=height() || rows.get(height()-1-y).charAt(x)=='#';
    }
    public boolean canStand(float x, float y, float radius) {
        int left=(int)Math.floor((x-radius)/TILE), right=(int)Math.floor((x+radius)/TILE);
        int bottom=(int)Math.floor((y-radius)/TILE), top=(int)Math.floor((y+radius)/TILE);
        for(int cy=bottom;cy<=top;cy++) for(int cx=left;cx<=right;cx++) if(wall(cx,cy)) return false;
        return true;
    }
    public List<Cell> findPath(Cell start, Cell target) {
        if (wall(start.x(), start.y()) || wall(target.x(), target.y())) return List.of();
        Cell[][] previous = new Cell[height()][width()];
        ArrayDeque<Cell> queue = new ArrayDeque<>();
        queue.add(start);
        previous[start.y()][start.x()] = start;
        int[][] directions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        // ponytail: BFS is enough for the small map; use A* if maps become much larger.
        while (!queue.isEmpty()) {
            Cell current = queue.remove();
            if (current.equals(target)) {
                java.util.ArrayList<Cell> result = new java.util.ArrayList<>();
                while (!current.equals(start)) {
                    result.add(current);
                    current = previous[current.y()][current.x()];
                }
                java.util.Collections.reverse(result);
                return List.copyOf(result);
            }
            for (int[] direction : directions) {
                int x = current.x() + direction[0];
                int y = current.y() + direction[1];
                if (!wall(x, y) && previous[y][x] == null) {
                    previous[y][x] = current;
                    queue.add(new Cell(x, y));
                }
            }
        }
        return List.of();
    }

    public boolean canSee(float startX, float startY, float endX, float endY) {
        if (!Float.isFinite(startX) || !Float.isFinite(startY)
                || !Float.isFinite(endX) || !Float.isFinite(endY)) return false;
        int x = (int) Math.floor(startX / TILE), y = (int) Math.floor(startY / TILE);
        int endCellX = (int) Math.floor(endX / TILE), endCellY = (int) Math.floor(endY / TILE);
        if (wall(x, y) || wall(endCellX, endCellY)) return false;
        double dx = endX - startX, dy = endY - startY;
        int stepX = Double.compare(dx, 0), stepY = Double.compare(dy, 0);
        double deltaX = dx == 0 ? Double.POSITIVE_INFINITY : TILE / Math.abs(dx);
        double deltaY = dy == 0 ? Double.POSITIVE_INFINITY : TILE / Math.abs(dy);
        double nextX = dx == 0 ? Double.POSITIVE_INFINITY
                : ((stepX > 0 ? (x + 1) * TILE : x * TILE) - startX) / dx;
        double nextY = dy == 0 ? Double.POSITIVE_INFINITY
                : ((stepY > 0 ? (y + 1) * TILE : y * TILE) - startY) / dy;
        while (x != endCellX || y != endCellY) {
            if (Math.abs(nextX - nextY) < 1e-10) {
                // A closed corner must not allow vision through two touching walls.
                if (wall(x + stepX, y) || wall(x, y + stepY)) return false;
                x += stepX; y += stepY;
                nextX += deltaX; nextY += deltaY;
            } else if (nextX < nextY) {
                x += stepX; nextX += deltaX;
            } else {
                y += stepY; nextY += deltaY;
            }
            if (wall(x, y)) return false;
        }
        return true;
    }

    public static final class Builder {
        private List<String> rows = List.of();
        public Builder rows(String... rows) { this.rows=List.of(rows); return this; }
        public Level build() {
            if(rows.size()<3 || rows.get(0).length()<3 || rows.stream().anyMatch(r ->
                    r.length()!=rows.get(0).length() || !r.matches("[#.SPCG]+")))
                throw new IllegalArgumentException("Invalid map rows");
            return new Level(rows);
        }
    }
    public static Level training() {
        return new Builder().rows(
            "####################",
            "#S.................#",
            "#....###....###..C.#",
            "#....#........#....#",
            "#....#..G.....#....#",
            "#.............#....#",
            "#..C....###........#",
            "#.......#.....###..#",
            "#..###..#..........#",
            "#.......#....C...P.#",
            "#..................#",
            "####################").build();
    }
}
