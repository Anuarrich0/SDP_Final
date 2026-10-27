public class Main {
    public static void main(String[] args) {
        Loki loki = new Loki("Classic Loki", 50);
        Mission mission = new Mission("Stabilize the timeline", "New York");

        System.out.println("TVA Mission Control");
        System.out.println("Loki: " + loki.getName());
        System.out.println("Magic power: " + loki.getMagicPower());
        System.out.println("Mission: " + mission.getTitle());
        System.out.println("Location: " + mission.getLocation());
    }
}
