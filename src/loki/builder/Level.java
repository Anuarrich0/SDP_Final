package loki.builder;

import java.util.ArrayDeque;
import java.util.List;

/** Builder constructs and validates a grid; coordinates use LibGDX's bottom-left origin. */
public final class Level {
    public static final int TILE = 48;

    public record Cell(int x, int y) {
        public float worldX() {
            return (x + .5f) * TILE;
        }

        public float worldY() {
            return (y + .5f) * TILE;
        }
    }

    private final List<String> rows;
    private final Cell spawn, exit, terminal, door;
    private final int tunnelRow;
    private final List<Cell> charges, guards, powers, dots;

    private Level(List<String> rows) {
        this.rows = List.copyOf(rows);
        List<Cell> starts = find('S');
        List<Cell> exits = find('P');
        List<Cell> terminals = find('T');
        List<Cell> doors = find('D');
        if (doors.size() != 1) {
            throw new IllegalArgumentException("Need one house door");
        }
        door = doors.get(0);
        tunnelRow = height() / 2;
        if (wall(0, tunnelRow) || wall(width() - 1, tunnelRow)) {
            throw new IllegalArgumentException("Need two tunnel openings");
        }
        charges = find('C');
        guards = find('G');
        if (guards.size() != 3 || guards.stream().anyMatch(c -> !house(c))) {
            throw new IllegalArgumentException("Need three guards inside the house");
        }
        powers = find('O');
        dots = find('.');
        if (starts.size() != 1 || exits.size() != 1 || terminals.size() != 1 || charges.isEmpty()) {
            throw new IllegalArgumentException("Need one spawn, one portal, one terminal and at least one charge");
        }
        spawn = starts.get(0);
        exit = exits.get(0);
        terminal = terminals.get(0);
        boolean[][] reachable = new boolean[height()][width()];
        ArrayDeque<Cell> queue = new ArrayDeque<Cell>();
        queue.add(spawn);
        reachable[spawn.y()][spawn.x()] = true;
        while (!queue.isEmpty()) {
            Cell cell = queue.remove();
            for (Cell neighbor : neighbors(cell)) {
                if (canEnter(neighbor, false) && !reachable[neighbor.y()][neighbor.x()]) {
                    reachable[neighbor.y()][neighbor.x()] = true;
                    queue.add(neighbor);
                }
            }
        }
        if (!reachable[exit.y()][exit.x()] || !reachable[terminal.y()][terminal.x()]
                || charges.stream().anyMatch(cell -> !reachable[cell.y()][cell.x()])
                || powers.stream().anyMatch(cell -> !reachable[cell.y()][cell.x()])
                || dots.stream().anyMatch(cell -> !reachable[cell.y()][cell.x()])) {
            throw new IllegalArgumentException("Unreachable map objective");
        }
        if (!reachable[houseExit().y()][houseExit().x()] || !canEnter(door, true)
                || guards.stream().anyMatch(cell -> findPath(cell, houseExit(), true).isEmpty())) {
            throw new IllegalArgumentException("House has no exit");
        }
    }

    private List<Cell> find(char symbol) {
        java.util.ArrayList<Cell> result = new java.util.ArrayList<Cell>();
        for (int y = 0; y < height(); y++) {
            for (int x = 0; x < width(); x++) {
                if (rows.get(height() - 1 - y).charAt(x) == symbol) {
                    result.add(new Cell(x, y));
                }
            }
        }
        return List.copyOf(result);
    }

    public int width() {
        return rows.get(0).length();
    }

    public int height() {
        return rows.size();
    }

    public Cell spawn() {
        return spawn;
    }

    public Cell exit() {
        return exit;
    }

    public Cell terminal() {
        return terminal;
    }

    public List<Cell> charges() {
        return charges;
    }

    public List<Cell> guards() {
        return guards;
    }

    public List<Cell> powers() {
        return powers;
    }

    public List<Cell> dots() {
        return dots;
    }

    public Cell door() {
        return door;
    }

    public Cell houseExit() {
        return new Cell(door.x(), door.y() + 1);
    }

    public boolean house(Cell cell) {
        return cell.x() >= door.x() - 3 && cell.x() <= door.x() + 3 && cell.y() >= door.y() - 2 && cell.y() < door.y();
    }

    public boolean suppressed(Cell cell) {
        return symbol(cell) == 'M';
    }

    private char symbol(Cell cell) {
        return wall(cell.x(), cell.y()) ? '#' : rows.get(height() - 1 - cell.y()).charAt(cell.x());
    }

    public boolean canEnter(Cell cell, boolean houseAccess) {
        return !wall(cell.x(), cell.y()) && (houseAccess || (!house(cell) && !cell.equals(door)));
    }

    public boolean tunnel(Cell cell) {
        return cell.y() == tunnelRow && (cell.x() < 3 || cell.x() >= width() - 3);
    }

    public Cell neighbor(Cell from, int dx, int dy) {
        int x = from.x() + dx, y = from.y() + dy;
        if (y == tunnelRow && dy == 0) {
            x = Math.floorMod(x, width());
        }
        return new Cell(x, y);
    }

    public List<Cell> neighbors(Cell cell) {
        return List.of(neighbor(cell, 1, 0), neighbor(cell, -1, 0), neighbor(cell, 0, 1), neighbor(cell, 0, -1));
    }

    public double distance(float x1, float y1, float x2, float y2) {
        float dx = Math.abs(x1 - x2);
        if ((int) (y1 / TILE) == tunnelRow && (int) (y2 / TILE) == tunnelRow) {
            dx = Math.min(dx, width() * TILE - dx);
        }
        return Math.hypot(dx, y1 - y2);
    }

    public int tunnelRow() {
        return tunnelRow;
    }

    public boolean wall(int x, int y) {
        return x < 0 || y < 0 || x >= width() || y >= height() || rows.get(height() - 1 - y).charAt(x) == '#';
    }

    public List<Cell> findPath(Cell start, Cell target) {
        return findPath(start, target, false);
    }

    public List<Cell> findPath(Cell start, Cell target, boolean houseAccess) {
        if (!canEnter(start, houseAccess) || !canEnter(target, houseAccess)) {
            return List.of();
        }
        Cell[][] previous = new Cell[height()][width()];
        ArrayDeque<Cell> queue = new ArrayDeque<>();
        queue.add(start);
        previous[start.y()][start.x()] = start;
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
            for (Cell next : neighbors(current)) {
                if (canEnter(next, houseAccess) && previous[next.y()][next.x()] == null) {
                    previous[next.y()][next.x()] = current;
                    queue.add(next);
                }
            }
        }
        return List.of();
    }

    public static final class Builder {
        private List<String> rows = List.of();

        public Builder rows(String... rows) {
            this.rows = List.of(rows);
            return this;
        }

        public Level build() {
            if (rows.size() < 3 || rows.get(0).length() < 3 || rows.stream()
                    .anyMatch(r -> r.length() != rows.get(0).length() || !r.matches("[#.SPGCTODHM_]+"))) {
                throw new IllegalArgumentException("Invalid map rows");
            }
            return new Level(rows);
        }
    }

    public static Level tva() {
        return new Builder().rows(
                "#########################",
                "#C...O.............O...P#",
                "#.###.###.##.##.###.###.#",
                "#.###.###.##.##.###.###.#",
                "#.###.###.##.##.###.###.#",
                "#....M.............T....#",
                "#.###.###.##.##.###.###.#",
                "#..........._...........#",
                "#.###.#.####D####.#.###.#",
                "_.......#HHHHHHH#..M...._",
                "#.###.#.#HGHGHGH#.#.###.#",
                "#.###.#.#########.#.###.#",
                "#.###.###.##.##.###.###.#",
                "#....M......C...........#",
                "#.###.###.##.##.###.###.#",
                "#.###.###.##.##.###.###.#",
                "#.###.###.##.##.###.###.#",
                "#S...O.............O...C#",
                "#########################").build();
    }
}
