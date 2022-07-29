package it.sssupclient.app;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

public class Interface {
    String host;
    int port;
    String username;
    boolean connected = false;
    String defalutUser = "unknown";
    String path;
    JPanel top;
    JPanel bottom;
    JPanel body;
    ArrayList<String> dirs = new ArrayList<String>();
    ArrayList<String> files = new ArrayList<String>();

    public Interface(String username, String host, int port) throws Exception {
        this.username = username;
        this.host = host;
        this.port = port;
        this.path = "/";
        updateList();
    }

    public void updateList() throws Exception {
        String[] initArgs = { "list", path};
        ArrayList<String> list = new ArrayList<String>();
        connected = App.execute(initArgs, list);
        for (String elem : list) {
            if (elem.endsWith("/")) {
                dirs.add(elem);
            } else {
                files.add(elem);
            }
        }
    }

    public String connectionStatus() {
        return connected ? "Connected to " + host + "/" + String.valueOf(port)
                : "Connect to server to start browsing...";
    }

    public JFrame createWindow() {
        JFrame frame = new JFrame("Java File Server");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 600);
        return frame;
    }

    public JPanel createBottomRow() {
        var bottomRow = new JPanel();
        var usernameLabel = new JLabel("username:");
        var usernameField = new JTextField(username, 10);
        var hostLabel = new JLabel("host:");
        var hostField = new JTextField(host, 15);
        var portLabel = new JLabel("port");
        var portField = new JTextField(String.valueOf(port), 5);
        var connectionButton = new JButton(connected ? "disconnect" : "connect");
        connectionButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // if connected disconnect
                if (connected) {
                    connected = false;
                    connectionButton.setText("connect");

                } else {
                    connected = true;
                    connectionButton.setText("disconnect");
                    try {
                        updateList();
                    } catch (Exception e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
                }
            }
        });

        bottomRow.add(usernameLabel);
        bottomRow.add(usernameField);
        bottomRow.add(hostLabel);
        bottomRow.add(hostField);
        bottomRow.add(portLabel);
        bottomRow.add(portField);
        bottomRow.add(connectionButton);

        return bottomRow;
    }

    public JPanel createTopRow() {
        var topRow = new JPanel();
        var connectionStatusLabel = new JLabel(connectionStatus());
        topRow.add(connectionStatusLabel);
        return topRow;
    }

    class DirTapListener implements ActionListener {
        String name;

        public DirTapListener(String name) {
            this.name = name;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            path = path + name;
            try {
                updateList();

            } catch (Exception e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        };
    }

    class ParentDirTapListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            // TODO change path and update list
        };
    }

    class FileTapListener implements ActionListener {
        String name;

        public FileTapListener(String name) {
            this.name = name;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            // TODO select file show buttons
        };
    }

    public JPanel fillBodyList() {
        listModel.clear();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        if (path != "/") {
            var parentButton = new JButton("..");
            parentButton.addActionListener(new ParentDirTapListener());
            body.add(parentButton);
        }
        for (String dir : dirs) {
            var tmpButton = new JButton(dir);
            tmpButton.addActionListener(new DirTapListener(dir));
            body.add(tmpButton);
        }
        for (String file : files) {
            var tmpButton = new JButton(file);
            tmpButton.addActionListener(new FileTapListener(file));
            body.add(tmpButton);
        }
        return body;
    }

    public void show() throws Exception {
        // Window
        JFrame frame = createWindow();

        // Connection Fields
        bottom = createBottomRow();
        frame.getContentPane().add(BorderLayout.SOUTH, bottom);

        // Connection Status
        top = createTopRow();
        frame.getContentPane().add(BorderLayout.NORTH, top);

        // body
        body = new JPanel();
        fillBodyList();
        frame.getContentPane().add(BorderLayout.CENTER, body);

        frame.setVisible(true);
    }
}
