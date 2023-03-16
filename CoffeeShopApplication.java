import javax.swing.JFrame;

public class CoffeeShopApplication {
    private JFrame frame;

    public CoffeeShopApplication() {
        initialize();
    }

    private void initialize() {
        frame = new JFrame();
        frame.setBounds(100, 100, 800, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(null);
        frame.setVisible(true); // set JFrame visible
    }


    public JFrame getFrame() {
        return frame;
    }

    // other methods, etc.
}
