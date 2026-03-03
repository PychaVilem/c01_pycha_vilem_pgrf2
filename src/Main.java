
import controller.Controller3D;
import view.Window;

public class Main {
    public static void main(String[] args) {
        Window window = new Window(800, 600);
        new Controller3D(window.getPanel());
    }
}