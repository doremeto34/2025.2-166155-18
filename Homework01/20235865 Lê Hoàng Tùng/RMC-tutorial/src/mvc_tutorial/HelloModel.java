package mvc_tutorial;

public class HelloModel {
    public String greet(String name) {
        name = (name == null) ? "" : name.trim();
        return name.isEmpty() ? "Hello!" : "Hello " + name + "!";
    }
}