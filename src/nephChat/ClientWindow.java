package nephChat;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;

public class ClientWindow extends JFrame implements Runnable {
    private static final long serialVersionUID = 1L;

    private JPanel mContentPane;
    private JTextField txtMessage;
    private JTextArea mHistory;
    private DefaultCaret mCaret;
    private Thread run, listen;
    private Client client;

    private boolean running = false;
    private JMenuBar menuBar;
    private JMenu mFile;
    private JMenuItem mOnlineUsers;
    private JMenuItem mExit;

    private OnlineUsers users;

    public ClientWindow(String name, String address, int port) {
        setTitle("tcp Chat Client");
        client = new Client(name, address, port);
        boolean connect = client.openConnection(address);
        if (!connect) {
            System.err.println("Connection failed!");
            console("Connection failed!");
        }
        createWindow();
        console("Attempting a connection to " + address + ":" + port + ", user: " + name);
        String connection = "/c/" + name + "/e/";
        client.send(connection.getBytes());
        users = new OnlineUsers();
        running = true;
        run = new Thread(this, "Running");
        run.start();
    }

    private void createWindow() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(880, 550);
        setLocationRelativeTo(null);

        menuBar = new JMenuBar();
        setJMenuBar(menuBar);

        mFile = new JMenu("File");
        menuBar.add(mFile);

        mOnlineUsers = new JMenuItem("Online Users");
        mOnlineUsers.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                users.setVisible(true);
            }
        });
        mFile.add(mOnlineUsers);

        mExit = new JMenuItem("Exit");
        mFile.add(mExit);
        mContentPane = new JPanel();
        mContentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(mContentPane);

        GridBagLayout mGBLContentPane = new GridBagLayout();
        mGBLContentPane.columnWidths = new int[] { 28, 815, 30, 7 }; // SUM = 880
        mGBLContentPane.rowHeights = new int[] { 25, 485, 40 }; // SUM = 550
        mContentPane.setLayout(mGBLContentPane);

        mHistory = new JTextArea();
        mHistory.setEditable(false);
        JScrollPane mScroll = new JScrollPane(mHistory);
        mCaret = (DefaultCaret) mHistory.getCaret();
        mCaret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        GridBagConstraints mScrollConstraints = new GridBagConstraints();
        mScrollConstraints.insets = new Insets(0, 0, 5, 5);
        mScrollConstraints.fill = GridBagConstraints.BOTH;
        mScrollConstraints.gridx = 0;
        mScrollConstraints.gridy = 0;
        mScrollConstraints.gridwidth = 3;
        mScrollConstraints.gridheight = 2;
        mScrollConstraints.weightx = 1;
        mScrollConstraints.weighty = 1;
        mScrollConstraints.insets = new Insets(0, 5, 0, 0);
        mContentPane.add(mScroll, mScrollConstraints);

        txtMessage = new JTextField();
        txtMessage.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    send(txtMessage.getText(), true);
                }
            }
        });
        GridBagConstraints gbc_txtMessage = new GridBagConstraints();
        gbc_txtMessage.insets = new Insets(0, 0, 0, 5);
        gbc_txtMessage.fill = GridBagConstraints.HORIZONTAL;
        gbc_txtMessage.gridx = 0;
        gbc_txtMessage.gridy = 2;
        gbc_txtMessage.gridwidth = 2;
        gbc_txtMessage.weightx = 1;
        gbc_txtMessage.weighty = 0;
        mContentPane.add(txtMessage, gbc_txtMessage);
        txtMessage.setColumns(10);

        JButton btnSend = new JButton("Send");
        btnSend.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                send(txtMessage.getText(), true);
            }
        });
        GridBagConstraints gbc_btnSend = new GridBagConstraints();
        gbc_btnSend.insets = new Insets(0, 0, 0, 5);
        gbc_btnSend.gridx = 2;
        gbc_btnSend.gridy = 2;
        gbc_btnSend.weightx = 0;
        gbc_btnSend.weighty = 0;
        mContentPane.add(btnSend, gbc_btnSend);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                String disconnect = "/d/" + client.getID() + "/e/";
                send(disconnect, false);
                running = false;
                client.close();
            }
        });

        setVisible(true);

        txtMessage.requestFocusInWindow();
    }

    public void run() {
        listen();
    }

    private void send(String message, boolean text) {
        if (message.equals("")) return;
        if (text) {
            message = client.getName() + ": " + message;
            message = "/m/" + message + "/e/";
            txtMessage.setText("");
        }
        client.send(message.getBytes());
    }

    public void listen() {
        listen = new Thread("Listen") {
            public void run() {
                while (running) {
                    String message = client.receive();
                    if (message.startsWith("/c/")) {
                        client.setID(Integer.parseInt(message.split("/c/|/e/")[1]));
                        console("Successfully connected to server! ID: " + client.getID());
                    } else if (message.startsWith("/m/")) {
                        String text = message.substring(3);
                        text = text.split("/e/")[0];
                        console(text);
                    } else if (message.startsWith("/i/")) {
                        String text = "/i/" + client.getID() + "/e/";
                        send(text, false);
                    } else if (message.startsWith("/u/")) {
                        String[] u = message.split("/u/|/n/|/e/");
                        users.update(Arrays.copyOfRange(u, 1, u.length - 1));
                    }
                }
            }
        };
        listen.start();
    }

    public void console(String message) {
        mHistory.append(message + "\n");
        mHistory.setCaretPosition(mHistory.getDocument().getLength());
    }
}
