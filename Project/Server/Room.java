package Project.Server;

import java.util.concurrent.ConcurrentHashMap;
import Project.Common.FlipPayload;
import Project.Common.RollPayload;

import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import Project.Common.LoggerUtil;
import Project.Common.Payload;




public class Room implements AutoCloseable{
    private String name;// unique name of the Room
    protected volatile boolean isRunning = false;
    private ConcurrentHashMap<Long, ServerThread> clientsInRoom = new ConcurrentHashMap<Long, ServerThread>();
    private Random random = new Random();
    private String targetUsername;

    public final static String LOBBY = "lobby";

    private void info(String message) {
        LoggerUtil.INSTANCE.info(String.format("Room[%s]: %s", name, message));
    }
    //arc73 7/22/24
    private static final String BOLD_REGEX = "\\*([^*]+)\\*";
    private static final String ITALICS_REGEX = "-([^\\-]+)-";
    private static final String UNDERLINE_REGEX = "_([^_]+)_";
    private static final String COLOR_REGEX = "\\[([rgbsilver]*)\\s([a-zA-Z]+)\\s([rgbsilver]*)\\]";

    public Room(String name) {
        this.name = name;
        isRunning = true;
        info("created");
    }

    public String getName() {
        return this.name;
    }

    
    //arc73 7/22/24
    private String processTextEffects(String message) { 
        message = message.replaceAll("#r(.+?)r#", "<span style='color:red;'>$1</span>"); // #r[text]r# for red text
        message = message.replaceAll("#g(.+?)g#", "<span style='color:green;'>$1</span>"); // #g[text]g# for green text
        message = message.replaceAll("#b(.+?)b#", "<span style='color:blue;'>$1</span>"); // #b[text]b# for blue text       
        message = message.replaceAll("\\*\\*(.+?)\\*\\*", "<b>$1</b>"); // Double asterisk for bold text
        message = message.replaceAll("\\*(.+?)\\*", "<i>$1</i>"); // Single asterisk for italic text
        message = message.replaceAll("_(.+?)_", "<u>$1</u>"); // Underscore for underlined text                      
        return message; // returns processed message
    }

    private String processBold(String message) {
        System.out.println("Source Message (Bold): " + message);
        // Print the original message for debugging


        // Define the regular expression pattern for detecting bold formatting
        Pattern pattern = Pattern.compile(BOLD_REGEX);
        // Create a matcher object to find matches in the message
        Matcher matcher = pattern.matcher(message);


        // Iterate through all matches in the message
        while (matcher.find()) {
            // Create bold-formatted text using the matched content
            String boldText = "<b>" + matcher.group(1) + "</b>";
            // Replace the original matched content with the formatted text
            message = message.replace(matcher.group(0), boldText);
        }




        // Print the formatted message for debugging
        System.out.println("Formatted Message (Bold): " + message);
        return message;
    }

    private String processItalics(String message) { 
        // Print the original message for debugging
        System.out.println("Source Message (Italics): " + message);
        // Define the regular expression pattern for detecting italics formatting
        Pattern pattern = Pattern.compile(ITALICS_REGEX);
        // Create a matcher object to find matches in the message
        Matcher matcher = pattern.matcher(message);


        // Iterate through all matches in the message
        while (matcher.find()) {
            // Create italics-formatted text using the matched content
            String italicText = "<i>" + matcher.group(1) + "</i>";
            // Replace the original matched content with the formatted text
            message = message.replace(matcher.group(0), italicText);
        }
        // Print the formatted message for debugging
        System.out.println("Formatted Message (Italics): " + message);
        return message;
    } 


    private String processUnderline(String message) { 
        System.out.println("Source Message (Underline): " + message);
        Pattern pattern = Pattern.compile(UNDERLINE_REGEX);
        Matcher matcher = pattern.matcher(message);


        while (matcher.find()) {
            String underlineText = "<u>" + matcher.group(1) + "</u>";
            message = message.replace(matcher.group(0), underlineText);
        }


        System.out.println("Formatted Message (Underline): " + message);
        return message;
    }


    private String processColor(String message) { 
        System.out.println("Source Message (Color): " + message);
        Pattern pattern = Pattern.compile(COLOR_REGEX);
        Matcher matcher = pattern.matcher(message);

        while (matcher.find()) {
            String colorText = "<font.color=" + matcher.group(2) + ">" + matcher.group(3) + "</font>";
            message = message.replace(matcher.group(0), colorText);
        }
        

        message = message.replace("[r", "<font color=red>").replace("r]","</font>");
        message = message.replace("[g", "<font color=green>").replace("g]","</font>");
        message = message.replace("[b", "<font color=blue>").replace("b]","</font>");        
           
        System.out.println("Formatted Message (Color): " + message);
        return message;
    } 

    private String formatMessage(String message) { 
        System.out.println("Source Message: " + message);
        message = processBold(message);
        message = processItalics(message);
        message = processUnderline(message);
        message = processColor(message);

       
        System.out.println("Formatted Message: " + message);
        return message;

    }

    public void processTextFormatting(ServerThread sender, String message) {
        String formattedMessage = formatMessage(message);
        sendMessage(sender, formattedMessage);
    }

   

    protected synchronized void addClient(ServerThread client) {
        if (!isRunning) { // block action if Room isn't running
            return;
        }
        if (clientsInRoom.containsKey(client.getClientId())) {
            info("Attempting to add a client that already exists in the room");
            return;
        }
        clientsInRoom.put(client.getClientId(), client);
        client.setCurrentRoom(this);

        // notify clients of someone joining
        sendRoomStatus(client.getClientId(), client.getClientName(), true);
        // sync room state to joiner
        syncRoomList(client);

        info(String.format("%s[%s] joined the Room[%s]", client.getClientName(), client.getClientId(), getName()));

    }

    protected synchronized void removedClient(ServerThread client) {
        if (!isRunning) { // block action if Room isn't running
            return;
        }
        // notify remaining clients of someone leaving
        // happen before removal so leaving client gets the data
        sendRoomStatus(client.getClientId(), client.getClientName(), false);
        clientsInRoom.remove(client.getClientId());
        LoggerUtil.INSTANCE.fine("Clients remaining in Room: " + clientsInRoom.size());

        info(String.format("%s[%s] left the room", client.getClientName(), client.getClientId(), getName()));

        autoCleanup();

    }

    /**
     * Takes a ServerThread and removes them from the Server
     * Adding the synchronized keyword ensures that only one thread can execute
     * these methods at a time,
     * preventing concurrent modification issues and ensuring thread safety
     * 
     * @param client
     */
    protected synchronized void disconnect(ServerThread client) {
        if (!isRunning) { // block action if Room isn't running
            return;
        }
        long id = client.getClientId();
        sendDisconnect(client);
        client.disconnect();
        // removedClient(client); // <-- use this just for normal room leaving
        clientsInRoom.remove(client.getClientId());
        LoggerUtil.INSTANCE.fine("Clients remaining in Room: " + clientsInRoom.size());
        
        // Improved logging with user data
        info(String.format("%s[%s] disconnected", client.getClientName(), id));
        autoCleanup();
    }

    protected synchronized void disconnectAll() {
        info("Disconnect All triggered");
        if (!isRunning) {
            return;
        }
        clientsInRoom.values().removeIf(client -> {
            disconnect(client);
            return true;
        });
        info("Disconnect All finished");
        autoCleanup();
    }

    /**
     * Attempts to close the room to free up resources if it's empty
     */
    private void autoCleanup() {
        if (!Room.LOBBY.equalsIgnoreCase(name) && clientsInRoom.isEmpty()) {
            close();
        }
    }

    public void close() {
        // attempt to gracefully close and migrate clients
        if (!clientsInRoom.isEmpty()) {
            sendMessage(null, "Room is shutting down, migrating to lobby");
            info(String.format("migrating %s clients", name, clientsInRoom.size()));
            clientsInRoom.values().removeIf(client -> {
                Server.INSTANCE.joinRoom(Room.LOBBY, client);
                return true;
            });
        }
        Server.INSTANCE.removeRoom(this);
        isRunning = false;
        clientsInRoom.clear();
        info(String.format("closed", name));
    }

    public void setTargetUsername(String targetUsername) {
        this.targetUsername = targetUsername;
    }
    
    public String getTargetUsername() {
        return this.targetUsername;
    }


  
    // send/sync data to client(s)

    /**
     * Sends to all clients details of a disconnect client
     * @param client
     */
    protected synchronized void sendDisconnect(ServerThread client) {
        info(String.format("sending disconnect status to %s recipients", clientsInRoom.size()));
        clientsInRoom.values().removeIf(clientInRoom -> {
            boolean failedToSend = !clientInRoom.sendDisconnect(client.getClientId(), client.getClientName());
            if (failedToSend) {
                info(String.format("Removing disconnected client[%s] from list", client.getClientId()));
                disconnect(client);
            }
            return failedToSend;
        });
    }

    /**
     * Syncs info of existing users in room with the client
     * 
     * @param client
     */
    protected synchronized void syncRoomList(ServerThread client) {

        clientsInRoom.values().forEach(clientInRoom -> {
            if (clientInRoom.getClientId() != client.getClientId()) {
                client.sendClientSync(clientInRoom.getClientId(), clientInRoom.getClientName());
            }
        });
    }

    /**
     * Syncs room status of one client to all connected clients
     * 
     * @param clientId
     * @param clientName
     * @param isConnect
     */
    protected synchronized void sendRoomStatus(long clientId, String clientName, boolean isConnect) {
        info(String.format("sending room status to %s recipients", clientsInRoom.size()));
        clientsInRoom.values().removeIf(client -> {
            boolean failedToSend = !client.sendRoomAction(clientId, clientName, getName(), isConnect);
            if (failedToSend) {
                info(String.format("Removing disconnected client[%s] from list", client.getClientId()));
                disconnect(client);
            }
            return failedToSend;
        });
    }

    /**
     * Sends a basic String message from the sender to all connectedClients
     * Internally calls processCommand and evaluates as necessary.
     * Note: Clients that fail to receive a message get removed from
     * connectedClients.
     * Adding the synchronized keyword ensures that only one thread can execute
     * these methods at a time,
     * preventing concurrent modification issues and ensuring thread safety
     * 
     * @param message
     * @param sender  ServerThread (client) sending the message or null if it's a
     *                server-generated message
     */
    
    protected synchronized void sendMessage(ServerThread sender, String message) {
        sendMessage(sender, message, false);
    }
    protected synchronized void sendMessage(ServerThread sender, String message, boolean isPrivate) {
        
        if (!isRunning) { // block action if Room isn't running
            return;
        }

        // Note: any desired changes to the message must be done before this section
        long senderId = sender == null ? ServerThread.DEFAULT_CLIENT_ID : sender.getClientId();
        
        final String[] messageToSend = { processTextEffects(message) };
        // loop over clients and send out the message; remove client if message failed
        // to be sent
        // Note: this uses a lambda expression for each item in the values() collection,
        // it's one way we can safely remove items during iteration
        info(String.format("sending message to %s recipients: %s", getName(), clientsInRoom.size(), messageToSend[0]));
        clientsInRoom.values().removeIf(client -> {
            if (client.isMuted(senderId)) {
                info(String.format("Message from %s to %s was skipped due to mute.", sender.getClientName(), client.getClientName()));
                return false;
            }



            boolean failedToSend = !client.sendMessage(senderId, messageToSend[0], isPrivate);
            if (failedToSend) {
                info(String.format("Removing disconnected client[%s] from list", client.getClientId()));
                disconnect(client);
        }
        return failedToSend;
    });
    }

    // end send data to client(s)

    public void handlePrivateMessage(ServerThread sender, Payload payload) {
        String targetUsername = payload.getTargetUsername();
        String message = payload.getMessage();

        String formattedMessage = processTextEffects(message);

        ServerThread targetClient = clientsInRoom.values().stream()
            .filter(client -> client.getClientName().equalsIgnoreCase(targetUsername))
            .findFirst()
            .orElse(null);

        if (targetClient != null) {
            String privateMessage = String.format("[Private message from %s]: %s", sender.getClientName(), formattedMessage);
            targetClient.sendMessage(sender.getClientId(), privateMessage, true);
            sender.sendMessage(sender.getClientId(), privateMessage, true);
        } else {
            sender.sendMessage(sender.getClientId(), String.format("User %s not found.", targetUsername), true);
        }
    }

    
    //arc73 7/22/24
    //Handle Flip Method                                                                          
    protected synchronized void handleFlip(ServerThread sender, FlipPayload flipPayload) {
        // Determines result of flip, either heads or tails
        String result = random.nextBoolean() ? "heads" : "tails";
        // Writes a message to the console which displays the result of the flip
        String message = String.format("%s flipped a coin and got <b>%s</b>", sender.getClientName(), result);
        // Message sent to clients connected to room
        sendMessage(null, message);
    }
    

    
    //arc73 7/22/24
    //Handle Roll Method
    protected synchronized void handleRoll(ServerThread sender, RollPayload rollPayload) {
        // Gets number of dice from payload
        int Dicenumber = rollPayload.getDicenumber();
        // Gets number of sides of each die from payload
        int Sidesnumber = rollPayload.getSidesnumber();
        // Writes a message to the console indicating the result of the roll
        StringBuilder resultMessage = new StringBuilder(String.format("%s rolled %dd%d and got <b>", sender.getClientName(), Dicenumber, Sidesnumber));
        int total = 0;
        // for-loop iterates through the number of dice specified by the user and the result is appended to the total
        for (int i = 0; i < Dicenumber; i++) {
            // Adds result from each die of the side landed on
            int rollResult = random.nextInt(Sidesnumber) + 1;
            // Roll result added to total
            total += rollResult;
            resultMessage.append(" ").append(rollResult);
        }
        // Total is added to the message which is written to the console
        resultMessage.append(" (total: ").append(total).append(")</b>");
        sendMessage(null, resultMessage.toString());
    }


    // receive data from ServerThread
    
    protected void handleCreateRoom(ServerThread sender, String room) {
        if (Server.INSTANCE.createRoom(room)) {
            Server.INSTANCE.joinRoom(room, sender);
        } else {
            sender.sendMessage(String.format("Room %s already exists", room));
        }
    }

    protected void handleJoinRoom(ServerThread sender, String room) {
        if (!Server.INSTANCE.joinRoom(room, sender)) {
            sender.sendMessage(String.format("Room %s doesn't exist", room));
        }
    }

    protected void handleListRooms(ServerThread sender, String roomQuery){
        sender.sendRooms(Server.INSTANCE.listRooms(roomQuery));
    }

    protected void clientDisconnect(ServerThread sender) {
        disconnect(sender);
    }

    public void handleMute(ServerThread sender, Payload payload) {
        String targetUsername = payload.getTargetUsername();
    
        ServerThread targetClient = clientsInRoom.values().stream()
            .filter(client -> client.getClientName().equalsIgnoreCase(targetUsername))
            .findFirst()
            .orElse(null);
    
        if (targetClient != null) {
            sender.addToMuteList(targetClient.getClientId());
            sender.sendMessage(sender.getClientId(), "You have muted " + targetUsername, false);
        } else {
            sender.sendMessage(sender.getClientId(), "User " + targetUsername + " not found.", false);
        }
    }

    public void handleUnmute(ServerThread sender, Payload payload) {
        String targetUsername = payload.getTargetUsername();
    
        ServerThread targetClient = clientsInRoom.values().stream()
            .filter(client -> client.getClientName().equalsIgnoreCase(targetUsername))
            .findFirst()
            .orElse(null);
    
        if (targetClient != null) {
            sender.removeFromMuteList(targetClient.getClientId());
            sender.sendMessage(sender.getClientId(), "You have unmuted " + targetUsername, false);
        } else {
            sender.sendMessage(sender.getClientId(), "User " + targetUsername + " not found.", false);
        }
    }



    // end receive data from ServerThread
}
