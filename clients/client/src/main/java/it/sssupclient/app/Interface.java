package it.sssupclient.app;
import javax.swing.JFrame;

import it.sssupserver.app.App;

import javax.swing.JButton;

public class Interface {
    String pwd;
    
    public static void main( String[] args ) throws Exception
    {
        JFrame frame = new JFrame("My First GUI");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(300,300);
        JButton button = new JButton("Press");
        frame.getContentPane().add(button); // Adds Button to content pane of frame
        frame.setVisible(true);
        App.execute("append");
    }
}
