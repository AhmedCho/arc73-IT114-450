package Project.Client;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import Project.Client.Interfaces.ICardControls;
import Project.Client.Interfaces.IConnectionEvents;
import Project.Client.Interfaces.IMessageEvents;
import Project.Client.Interfaces.IRoomEvents;
import Project.Client.Views.ChatPanel;
import Project.Client.Views.ConnectionPanel;
import Project.Client.Views.Menu;
import Project.Client.Views.RoomsPanel;
import Project.Client.Views.UserDetailsPanel;
import Project.Client.Views.UserListPanel;
import Project.Common.LoggerUtil;


/**
 * ClientUI is the main application window that manages different screens and
 * handles client events.
 */
public class ClientUI extends JFrame implements IConnectionEvents, IMessageEvents, IRoomEvents, ICardControls {
    private CardLayout card = new CardLayout(); // Layout manager to switch between different screens
    private Container container; // Container to hold different panels
    private JPanel cardContainer;
    private String originalTitle;
    private JPanel currentCardPanel;
    private CardView currentCard = CardView.CONNECT;
    private JMenuBar menu;
    private ConnectionPanel connectionPanel;
    private UserDetailsPanel userDetailsPanel;
    private ChatPanel chatPanel;
    private RoomsPanel roomsPanel;
    private JLabel roomLabel = new JLabel();
    private UserListPanel userListPanel;

    {
        // Note: Moved from Client as this file is the entry point now
        // statically initialize the client-side LoggerUtil
        LoggerUtil.LoggerConfig config = new LoggerUtil.LoggerConfig();
        config.setFileSizeLimit(2048 * 1024); // 2MB
        config.setFileCount(1);
        config.setLogLocation("client.log");
        // Set the logger configuration
        LoggerUtil.INSTANCE.setConfig(config);
    }


    /**
     * Constructor to create the main application window.
     *
     * @param title The title of the window.
     */
    public ClientUI(String title) {
        super(title); // Call the parent's constructor to set the frame title
        originalTitle = title;
        container = getContentPane();
        cardContainer = new JPanel();
        cardContainer.setLayout(card);
        container.add(roomLabel, BorderLayout.NORTH);
        container.add(cardContainer, BorderLayout.CENTER);
        //arc73 7/29/24
        userListPanel = new UserListPanel();
        Client.INSTANCE.setUserListPanel(userListPanel);
        add(userListPanel, BorderLayout.EAST);
        cardContainer.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                cardContainer.setPreferredSize(e.getComponent().getSize());
                cardContainer.revalidate();
                cardContainer.repaint();
            }


            @Override
            public void componentMoved(ComponentEvent e) {
                // No specific action on move
            }
        });


        setMinimumSize(new Dimension(400, 400));
        setLocationRelativeTo(null); // Center the window
        menu = new Menu(this);
        this.setJMenuBar(menu);


        // Initialize panels
        connectionPanel = new ConnectionPanel(this);
        userDetailsPanel = new UserDetailsPanel(this);
        chatPanel = new ChatPanel(this);
        roomsPanel = new RoomsPanel(this);
       

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                int response = JOptionPane.showConfirmDialog(cardContainer,
                        "Are you sure you want to close this window?", "Close Window?",
                        JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (response == JOptionPane.YES_OPTION) {
                    try {
                        Client.INSTANCE.sendDisconnect();
                    } catch (Exception e) {
                        LoggerUtil.INSTANCE.severe("Error during disconnect: " + e.getMessage());
                    }
                    System.exit(0);
                }
            }
        });

        addExportChatMenuItem();
        pack(); // Resize to fit components
        setVisible(true); // Show the window
    }

    //arc73 7/29/24 - Chat export method
    private void addExportChatMenuItem() {
        JMenu fileMenu = new JMenu("File");     
        JMenuItem exportChatMenuItem = new JMenuItem("Export Chat"); //Creates new button item to export chat history
        exportChatMenuItem.addActionListener(e -> { //ActionListener handles click event of export chat button
            try {
                chatPanel.exportChatHistory();
                //Displays success message when chat history is successfully exported
                JOptionPane.showMessageDialog(this, "The chat history has exported successfully.", "Export Successful", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                //Displays error message when chat history fails to export
                JOptionPane.showMessageDialog(this, "The chat history failed to export.: " + ex.getMessage(), "Export Failed", JOptionPane.ERROR_MESSAGE);
            }
        });
        fileMenu.add(exportChatMenuItem); //Adds export chat menu item to the file menu
        menu.add(fileMenu); //Adds file menu to menu bar
    }


    /**
     * Finds the current visible panel and updates the current card state.
     */
    private void findAndSetCurrentPanel() {
        for (Component c : cardContainer.getComponents()) {
            if (c.isVisible()) {
                currentCardPanel = (JPanel) c;
                currentCard = Enum.valueOf(CardView.class, currentCardPanel.getName());
                // Ensure connection for specific views
                if (Client.INSTANCE.getMyClientId() == ClientData.DEFAULT_CLIENT_ID
                        && currentCard.ordinal() >= CardView.CHAT.ordinal()) {
                    show(CardView.CONNECT.name());
                }
                break;
            }
        }
        LoggerUtil.INSTANCE.fine("Current panel: " + currentCardPanel.getName());
    }


    @Override
    public void next() {
        card.next(cardContainer);
        findAndSetCurrentPanel();
    }


    @Override
    public void previous() {
        card.previous(cardContainer);
        findAndSetCurrentPanel();
    }


    @Override
    public void show(String cardName) {
        card.show(cardContainer, cardName);
        findAndSetCurrentPanel();
    }


    @Override
    public void addPanel(String cardName, JPanel panel) {
        cardContainer.add(panel, cardName);
    }


    @Override
    public void connect() {
        String username = userDetailsPanel.getUsername();
        String host = connectionPanel.getHost();
        int port = connectionPanel.getPort();
        setTitle(originalTitle + " - " + username);
        Client.INSTANCE.connect(host, port, username, this);
    }
   
   


    public static void main(String[] args) {
        // TODO update with your UCID instead of mine
        SwingUtilities.invokeLater(() -> new ClientUI("ARC73-Client"));
    }
    // Interface methods start


    @Override
    public void onClientDisconnect(long clientId, String clientName) {
        if (currentCard.ordinal() >= CardView.CHAT.ordinal()) {
            chatPanel.removeUserListItem(clientId);
            boolean isMe = clientId == Client.INSTANCE.getMyClientId();
            String message = String.format("*%s disconnected*",
                    isMe ? "You" : String.format("%s[%s]", clientName, clientId));
            chatPanel.addText(message);
            if (isMe) {
                LoggerUtil.INSTANCE.info("I disconnected");
                previous();
            }
        }
    }

    public void onMessageReceive(long clientId, String message) {
        if (currentCard.ordinal() >= CardView.CHAT.ordinal()) {
            String clientName = Client.INSTANCE.getClientNameFromId(clientId);
            chatPanel.addText(String.format("%s[%s]: %s", clientName, clientId, message));

            // arc73 7/29/24 - Message handling of mute/unmute
            if (message.contains("You have muted")) {
                // Extract the target username from the message
                String targetUsername = message.split(" ")[3];
                // Retrieve the client ID associated with the target username
                long targetClientId = getClientIdFromName(targetUsername);
                 // Print information about the muted user to the console
                System.out.println("The following user is muted: " + targetUsername + ", ID: " + targetClientId);
                 // Update the user status in the user list panel to 'muted'
                userListPanel.updateUserStatus(targetClientId, "muted");

            } else if (message.contains("You have unmuted")) {
                // Extract the target username from the message
                String targetUsername = message.split(" ")[3];
                // Retrieve the client ID associated with the target username
                long targetClientId = getClientIdFromName(targetUsername);
                // Print information about the unmuted user to the console
                System.out.println("The following user is unmuted: " + targetUsername + ", ID: " + targetClientId);
                // Update the user status in the user list panel to 'active'
                userListPanel.updateUserStatus(targetClientId, "active");
            }
    
            userListPanel.highlightUser(clientId);
        }
    }

    private long getClientIdFromName(String username) {
    for (ClientData client : Client.INSTANCE.getKnownClients().values()) {
        if (client.getClientName().equals(username)) {
            return client.getClientId();
        }
    }
    return -1; // Return an invalid ID if not found
}


    @Override
    public void onReceiveClientId(long id) {
        show(CardView.CHAT.name());
        chatPanel.addText("*You connected*");
    }


    @Override
    public void onResetUserList() {
        chatPanel.clearUserList();
    }


    @Override
    public void onSyncClient(long clientId, String clientName) {
        if (currentCard.ordinal() >= CardView.CHAT.ordinal()) {
            chatPanel.addUserListItem(clientId, String.format("%s (%s)", clientName, clientId));
        }
    }


    @Override
    public void onReceiveRoomList(List<String> rooms, String message) {
        roomsPanel.removeAllRooms();
        if (message != null && !message.isEmpty()) {
            roomsPanel.setMessage(message);
        }
        if (rooms != null) {
            for (String room : rooms) {
                roomsPanel.addRoom(room);
            }
        }
    }


    @Override
    public void onRoomAction(long clientId, String clientName, String roomName, boolean isJoin) {
        LoggerUtil.INSTANCE.info("Current card: " + currentCard.name());
        if (currentCard.ordinal() >= CardView.CHAT.ordinal()) {
            boolean isMe = clientId == Client.INSTANCE.getMyClientId();
            String message = String.format("*%s %s the Room %s*",
                    /* 1st %s */ isMe ? "You" : String.format("%s[%s]", clientName, clientId),
                    /* 2nd %s */ isJoin ? "joined" : "left",
                    /* 3rd %s */ roomName == null ? "" : roomName); // added handling of null after the demo video
            chatPanel.addText(message);
            if (isJoin) {
                roomLabel.setText("Room: " + roomName);
                chatPanel.addUserListItem(clientId, String.format("%s (%s)", clientName, clientId));
            } else {
                chatPanel.removeUserListItem(clientId);
            }


        }
    }


    // Interface methods end
}
