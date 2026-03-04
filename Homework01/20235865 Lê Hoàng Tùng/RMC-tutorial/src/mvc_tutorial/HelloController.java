package mvc_tutorial;

public class HelloController {
    private final HelloModel model;

    public HelloController(HelloModel model) {
        this.model = model;
    }

    public String handleGreet(String name) {
        return model.greet(name);
    }
}