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
                list.clear();
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
        // graphic
        private JSplitPane bodySplit;
        private JScrollPane contentsScrollPane;
        private JPanel detailPane = null;
        private JScrollPane detailScrollPane;
        private JList<String> jlist = null;
        private JButton newFolderButton;
        private JButton uploadFileButton;
        private JButton pasteButton;
        // data
        private int firstFile;
        String selectedMoveFile;
        boolean copying;

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

            if (jlist != null) {

                newFolderButton = new JButton("New folder");
                newFolderButton.addActionListener(new ActionTapListener(path, "mkdir"));
                add(newFolderButton);

                uploadFileButton = new JButton("Upload file");
                uploadFileButton.addActionListener(new ActionTapListener(path, "upload"));
                add(uploadFileButton);

                if (selectedMoveFile != null) {
                    pasteButton = new JButton(copying ? "Copy here" : "Move here");
                    pasteButton.addActionListener(new ActionTapListener("", "paste"));
                    add(pasteButton);
                }
            }

            revalidate();
        }

        public void updateJlist(String[] fileList) {
            jlist = new JList<String>(fileList);
            jlist.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            jlist.setLayoutOrientation(JList.VERTICAL);
            jlist.setVisibleRowCount(-1);
            jlist.addListSelectionListener(new MySelectionListener());
            jlist.addMouseListener(new MyMouseAdapter());
        }

        class MySelectionListener implements ListSelectionListener {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                detailPane = new JPanel();
                detailPane.setLayout(new BoxLayout(detailPane, BoxLayout.Y_AXIS));
                if (jlist.getSelectedValue() == "..") {
                    var parentLabel = new JLabel("parent directory");
                    detailPane.add(parentLabel);
                    var navigateButton = new JButton("Navigate");
                    navigateButton.addActionListener(new ParentNavigateTapListener());
                    detailPane.add(navigateButton);
                } else {
                    var nameLabel = new JLabel(jlist.getSelectedValue());
                    detailPane.add(nameLabel);
                    if (jlist.getSelectedIndex() < firstFile) {
                        attemptCommand(new String[] { "list", jlist.getSelectedValue() });
                        var elementsLabel = new JLabel("elements: " + String.valueOf(list.size()));
                        detailPane.add(elementsLabel);
                        var navigateButton = new JButton("Navigate");
                        navigateButton.addActionListener(new NavigateTapListener(jlist.getSelectedValue()));
                        detailPane.add(navigateButton);
                    } else {
                        attemptCommand(new String[] { "size", jlist.getSelectedValue() });
                        var sizeLabel = new JLabel("size: " + list.get(0));
                        detailPane.add(sizeLabel);
                        String actions[] = {
                                "download",
                                "move",
                                "copy",
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
                }
                updateView();
            }
        }

        class MyMouseAdapter extends MouseAdapter {
            @Override
            public void mouseClicked(MouseEvent evt) {
                final var l = (JList<String>) evt.getSource();
                if (evt.getClickCount() == 2 && evt.getButton() == MouseEvent.BUTTON1) {
                    var value = l.getSelectedValue();
                    // directory
                    if (l.getSelectedIndex() < firstFile) {
                        if (value == "..") {
                            var p = path.split("/");
                            path = "";
                            for (var i = 0; i < p.length - 1; i++) {
                                path += p[i] + "/";
                            }
                        } else {
                            path = value;
                        }
                        attemptCommand(new String[] { "list", path });
                        String fileList[] = getFileList();
                        updateJlist(fileList);
                        detailPane = null;
                        updateView();
                    }
                    // file
                    else {
                        var tmp = inputNameDialog("Save file as:", "downloaded.txt");
                        attemptCommand(new String[] { "read", "local-files/" + tmp, l.getSelectedValue() });
                    }

                }
            };
        }

        class NavigateTapListener implements ActionListener {
            String name;

            public NavigateTapListener(String name) {
                this.name = name;
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                path = name;
                attemptCommand(new String[] { "list", path });
                String fileList[] = getFileList();
                updateJlist(fileList);
                detailPane = null;
                updateView();
            };
        }

        class ParentNavigateTapListener implements ActionListener {
            @Override
            public void actionPerformed(ActionEvent e) {
                var p = path.split("/");
                path = "";
                for (var i = 0; i < p.length - 1; i++) {
                    path += p[i] + "/";
                }
                attemptCommand(new String[] { "list", path });
                String fileList[] = getFileList();
                updateJlist(fileList);
                detailPane = null;
                updateView();
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
                var tmp = "";
                switch (action) {
                    case "copy":
                        selectedMoveFile = name;
                        copying = true;
                        break;
                    case "move":
                        selectedMoveFile = name;
                        copying = false;
                        break;
                    case "download":
                        tmp = inputNameDialog("Save file as:", "downloaded.txt");
                        attemptCommand(new String[] { "read", "local-files/" + tmp, name });
                        break;
                    case "replace":
                        tmp = selectLocalFileDialog();
                        attemptCommand(new String[] { "write", tmp, name });
                        break;
                    case "append":
                        tmp = selectLocalFileDialog();
                        attemptCommand(new String[] { "append", tmp, name });
                        break;
                    case "delete":
                        attemptCommand(new String[] { "delete", name });
                        break;
                    case "mkdir":
                        tmp = inputNameDialog("New folder name:", "folder");
                        attemptCommand(new String[] { "mkdir", path + tmp });
                        break;
                    case "upload":
                        tmp = selectLocalFileDialog();
                        var newFile = inputNameDialog("New file name:", "uploaded.txt");
                        attemptCommand(new String[] { "create", tmp, name + newFile });
                        break;
                    case "paste":
                        if (copying) {
                            tmp = inputNameDialog("Copy name:", "copy.txt");
                            attemptCommand(new String[] { "copy", selectedMoveFile, path + tmp });
                            selectedMoveFile = null;
                        } else {
                            tmp = inputNameDialog("Moved file name", "moved.txt");
                            attemptCommand(new String[] { "move", selectedMoveFile, path + tmp });
                            selectedMoveFile = null;
                        }
                        break;
                    default:
                        break;
                }
                attemptCommand(new String[] { "list", path });
                String fileList[] = getFileList();
                updateJlist(fileList);
                detailPane = null;
                updateView();
            };
        }
    }

    public boolean attemptCommand(String[] args) {
        // // debug
        // String msg = "Command ";
        // for (String s : args) {
        //     msg += s + ", ";
        // }
        // System.out.println(msg);

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

    public String selectLocalFileDialog() {
        var fc = new JFileChooser(System.getProperty("user.dir") + "/local-files");
        fc.showOpenDialog(frame);
        var path = fc.getSelectedFile().toPath().toString();
        var file = path.split("client/")[1];
        return file;
    }

    public String inputNameDialog(String msg, String name) {
        String ret = (String) JOptionPane.showInputDialog(frame, msg, name);
        return ret;
    }
    public static void main(String[] args) throws Exception {
        new Interface("", "localhost", 5050);
    }
}
