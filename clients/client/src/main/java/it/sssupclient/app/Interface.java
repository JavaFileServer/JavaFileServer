package it.sssupclient.app;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

public class Interface {
    // server connection parameters
    String host;
    int port;
    String username;
    // connection status
    boolean connected = false;
    // current data
    String path = "";
    ArrayList<String> list = new ArrayList<String>();
    // graphic components
    JFrame frame;
    JButton connectionButton;
    JLabel connectionStatusLabel;
    InterfaceBody body;
    JTextField usernameField;
    JTextField hostField;
    JTextField portField;
    // ListModel<String> contentsList;

    static String defalutUser = "unknown";
    static String disconnectedStatus = "Connect to server to start browsing files...";

    public Interface(String username, String host, int port) throws Exception {
        this.username = username;
        this.host = host;
        this.port = port;
        // init frame
        createWindow();
    }

    public void createWindow() {
        frame = new JFrame("Java File Server");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 600);

        frame.getContentPane().add(BorderLayout.SOUTH, createBottomRow());
        frame.getContentPane().add(BorderLayout.NORTH, createTopRow());
        body = new InterfaceBody();
        frame.getContentPane().add(BorderLayout.CENTER, body);
        frame.setVisible(true);
    }

    public JPanel createBottomRow() {
        var bottomRow = new JPanel();
        var usernameLabel = new JLabel("username:");
        usernameField = new JTextField(username, 10);
        var hostLabel = new JLabel("host:");
        hostField = new JTextField(host, 15);
        var portLabel = new JLabel("port");
        portField = new JTextField(String.valueOf(port), 5);
        connectionButton = new JButton("connect");
        connectionButton.addActionListener(new ConnectionTapListener());

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
        connectionStatusLabel = new JLabel(disconnectedStatus);
        topRow.add(connectionStatusLabel);
        return topRow;
    }

    public void updateConnection(boolean newConnected, boolean failed) {
        connected = newConnected;
        connectionButton.setText(connected ? "disconnect" : "connect");
        connectionStatusLabel.setText(
                connected ? "Connected to " + host + "/" + String.valueOf(port) + "/" + path
                        : failed ? "Connection failed " : disconnectedStatus);
    }

    class ConnectionTapListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent ae) {
            // attempt connection
            if (!connected) {
                String[] initArgs = { "list" };
                boolean success;
                username = usernameField.getText();
                host = hostField.getText();
                port = Integer.valueOf(portField.getText());
                try {
                    success = App.execute(initArgs, list, username, host, port);
                } catch (Exception e) {
                    connectionStatusLabel.setText("Connection failed");
                    return;
                }
                if (!success) {
                    connectionStatusLabel.setText("Connection failed");
                    return;
                }
                updateConnection(success, false);
                frame.getContentPane().remove(body);
                body = new InterfaceBody(list);
                frame.getContentPane().add(body);
                frame.revalidate();
            }
            // disconnect
            else {
                connectionInterrupted();
            }
        };
    }

    public boolean rootFolder() {
        return path == "";
    }

    class InterfaceBody extends JPanel {
        private JSplitPane bodySplit;
        private JScrollPane contentsScrollPane;
        private JPanel detailPane = null;
        private JScrollPane detailScrollPane;
        private JList<String> jlist = null;
        private int firstFile;

        public InterfaceBody() {
            updateView();
        }

        public InterfaceBody(ArrayList<String> list) {
            // contents
            String fileList[] = getFileList();
            updateJlist(fileList);
            updateView();
        }

        public String[] getFileList() {
            // file list
            int fileListLength = list.size() + (rootFolder() ? 0 : 1);
            String fileList[] = new String[fileListLength];
            int i = 0;

            // parent dir
            if (!rootFolder()) {
                fileList[i] = "..";
                i++;
            }
            // directories
            for (int l = 0; l < list.size(); l++) {
                if (list.get(l).endsWith("/")) {
                    fileList[i] = list.get(l);
                    i++;
                }
            }
            firstFile = i;
            // files
            for (int l = 0; l < list.size(); l++) {
                if (!list.get(l).endsWith("/")) {
                    fileList[i] = list.get(l);
                    i++;
                }
            }
            return fileList;
        }

        // after update on detailPane or jlist
        public void updateView() {
            removeAll();

            // contents
            contentsScrollPane = (jlist == null) ? new JScrollPane() : new JScrollPane(jlist);
            contentsScrollPane.setPreferredSize(new Dimension(600, 450));

            // selected file details
            detailScrollPane = (detailPane == null) ? new JScrollPane() : new JScrollPane(detailPane);
            detailScrollPane.setPreferredSize(new Dimension(300, 450));

            bodySplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, contentsScrollPane, detailScrollPane);
            bodySplit.setSize(1000, 450);
            add(bodySplit);
            revalidate();
        }

        public void updateJlist(String[] fileList) {
            jlist = new JList<String>(fileList);
            jlist.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            jlist.setLayoutOrientation(JList.VERTICAL);
            jlist.setVisibleRowCount(-1);
            jlist.addListSelectionListener(new MySelectionListener());
        }

        class MySelectionListener implements ListSelectionListener {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                detailPane = new JPanel();
                detailPane.setLayout(new BoxLayout(detailPane, BoxLayout.Y_AXIS));
                var nameLabel = new JLabel(jlist.getSelectedValue());
                detailPane.add(nameLabel);

                if (jlist.getSelectedIndex() < firstFile) {
                    attemptCommand(new String[] { "list", jlist.getSelectedValue() });
                    var elementsLabel = new JLabel("elements: " + String.valueOf(list.size()));
                    detailPane.add(elementsLabel);
                    var navigateButton = new JButton("Navigate");
                    navigateButton.addActionListener(new DirTapListener(jlist.getSelectedValue()));
                    detailPane.add(navigateButton);
                } else {
                    attemptCommand(new String[] { "size", jlist.getSelectedValue() });
                    var sizeLabel = new JLabel("size: " + list.get(0));
                    detailPane.add(sizeLabel);
                    String actions[] = {
                            "copy",
                            "cut",
                            "download",
                            "replace",
                            "append",
                            "delete"
                    };
                    for (String action : actions) {
                        var tmpButton = new JButton(action);
                        tmpButton.addActionListener(new ActionTapListener(jlist.getSelectedValue(), action));
                        detailPane.add(tmpButton);
                    }
                }
                updateView();
            }
        }

        class DirTapListener implements ActionListener {
            String name;

            public DirTapListener(String name) {
                this.name = name;
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                if (name == "..") {
                    var p = path.split("/");
                    path = "";
                    for (var i = 0; i < p.length - 1; i++) {
                        path += p[i] + "/";
                    }
                } else {
                    path = path + name;
                }
                attemptCommand(new String[] { "list", path });
                String fileList[] = getFileList();
                updateJlist(fileList);
                detailPane = null;
                updateView();
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

        class ActionTapListener implements ActionListener {
            String name;
            String action;

            public ActionTapListener(String name, String action) {
                this.name = name;
                this.action = action;
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                // TODO select file show buttons
            };
        }
    }

    public boolean attemptCommand(String[] args) {
        list.clear();
        boolean success;
        try {
            success = App.execute(args, list, username, host, port);
        } catch (Exception e) {
            connectionInterrupted();
            return false;
        }
        if (!success) {
            connectionInterrupted();
            return false;
        }
        return success;
    }

    public void connectionInterrupted() {
        updateConnection(false, true);
        list.clear();
        path = "";
        frame.getContentPane().remove(body);
        body = new InterfaceBody();
        frame.getContentPane().add(body);
        frame.revalidate();
    }

}
