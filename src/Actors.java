public class Actors {
//    private final String id;
    private final String name;
    private final int birthYear;

    public Actors(String name, int birthYear) {
        this.name = name;
        this.birthYear = birthYear;
    }

    public String getName() {
        return name;
    }

    public int getBirthYear() {
        return birthYear;
    }

    public String toString() {
        return "Name:" + getName() + ", " +
                "Birth Year:" + getBirthYear() + "\n";
    }
}
