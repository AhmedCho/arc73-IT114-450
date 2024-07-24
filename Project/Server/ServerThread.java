package Project.Server;
import java.io.IOException;
import java.net.Socket;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import Project.Common.PayloadType;
import Project.Common.RoomResultsPayload;
import Project.Common.Payload;
import Project.Common.RollPayload;
import Project.Common.FlipPayload;
import Project.Common.ConnectionPayload;
import Project.Common.LoggerUtil;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

/**
 * A server-side representation of a single client.
 * This class is more about the data and abstracted communication
 */
public class ServerThread extends BaseServerThread {
    public static final long DEFAULT_CLIENT_ID = -1;
    private Room currentRoom;
    private long clientId;
    private String clientName;
    private Consumer<ServerThread> onInitializationComplete; // callback to inform when this object is ready
    private Set<String> muteList = new HashSet<>();

    /**
     * Wraps the Socket connection and takes a Server reference and a callback
     * 
     * @param myClient
     * @param server
     * @param onInitializationComplete method to inform listener that this object is
     *                                 ready
     */
    protected ServerThread(Socket myClient, Consumer<ServerThread> onInitializationComplete) {
        Objects.requireNonNull(myClient, "Client socket cannot be null");
        Objects.requireNonNull(onInitializationComplete, "callback cannot be null");
        info("ServerThread created");
        // get communication channels to single client
        this.client = myClient;
        this.clientId = ServerThread.DEFAULT_CLIENT_ID;// this is updated later by the server
        this.onInitializationComplete = onInitializationComplete;
        loadMuteList();
    }

    public void setClientName(String name) {
        if (name == null) {
            throw new NullPointerException("Client name can't be null");
        }
        this.clientName = name;
        onInitialized();
    }

    public void setClientId(long clientId) {
        this.clientId = clientId;
        saveClientId();
    }

    public String getClientName() {
        return clientName;
    }

    public long getClientId() {
        return this.clientId;
    }

    protected Room getCurrentRoom() {
        return this.currentRoom;
    }

    protected void setCurrentRoom(Room room) {
        if (room == null) {
            throw new NullPointerException("Room argument can't be null");
        }
        currentRoom = room;
    }

    @Override
    protected void onInitialized() {
        loadMuteList();
        onInitializationComplete.accept(this); // Notify server that initialization is complete
    }

    @Override
    protected void info(String message) {
        LoggerUtil.INSTANCE.info(String.format("ServerThread[%s(%s)]: %s", getClientName(), getClientId(), message));
    }

    @Override
    protected void cleanup() {
        currentRoom = null;
        super.cleanup();
    }

    @Override
    protected void disconnect() {
        saveMuteList();
        // sendDisconnect(clientId, clientName);
        super.disconnect();
    }

    // arc73 7/29/24 - handle received message from the Client
    @Override
    protected void processPayload(Payload payload) {
        try {
            switch (payload.getPayloadType()) {
                case CLIENT_CONNECT:
                    ConnectionPayload cp = (ConnectionPayload) payload;
                    setClientName(cp.getClientName());
                    break;
                case MESSAGE:
                if (payload.isPrivate()) {
                    currentRoom.handlePrivateMessage(this, payload);
                } else {
                    currentRoom.sendMessage(this, payload.getMessage());
                }
                    break;
                case ROOM_CREATE:
                    currentRoom.handleCreateRoom(this, payload.getMessage());
                    break;
                case ROOM_JOIN:
                    currentRoom.handleJoinRoom(this, payload.getMessage());
                    break;
                case ROOM_LIST:
                    currentRoom.handleListRooms(this, payload.getMessage());
                    break;
                case DISCONNECT:
                    currentRoom.disconnect(this);
                    break;
                case ROLL: // Case for 'ROLL' payload type                                                         
                    if (payload instanceof RollPayload) {
                        RollPayload rollPayload = (RollPayload) payload;
                        System.out.println("Received RollPayload"); // Message to the console indicating the RollPayload was received
                        currentRoom.handleRoll(this, rollPayload); // Handles the roll command in the same room the client sent the payload from
                    }
                    break;
                case FLIP: // Case for 'FLIP' payload type
                    if (payload instanceof FlipPayload) {
                        FlipPayload flipPayload = (FlipPayload) payload;
                        System.out.println("Received FlipPayload: " + flipPayload); // Message to the console indicating the FlipPayload was received
                        currentRoom.handleFlip(this, flipPayload); // Handles the roll command in the same room the client sent the payload from
                    }
                    break;
                case MUTE:
                    currentRoom.handleMute(this, payload);
                    break;
                case UNMUTE:
                    currentRoom.handleUnmute(this, payload);
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            LoggerUtil.INSTANCE.severe("Could not process Payload: " + payload,e);
        
        }
    }
    //arc73 7/29/24
    private void saveClientId() {
        File file = new File(getClientName() + "_clientId.txt");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(Long.toString(clientId));
        } catch (IOException e) {
            LoggerUtil.INSTANCE.severe("Encountered issue attempting to save client ID for client: " + getClientName(), e);
        }
    }
    //arc73 7/29/24
    //Load mute list method
    private void loadMuteList() {
        File file = new File(getClientName() + "_mutelist.txt");
        if (file.exists()) { //Checks if file exists
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) { //Read each line of the file and add it to the mute list
                    muteList.add(line);
                }
                //Log mute list has been successfully loaded
                LoggerUtil.INSTANCE.info("Mute list loaded for client: " + getClientName());
                //Log error message if an IOException occurs while loading the file
            } catch (IOException e) {
                LoggerUtil.INSTANCE.severe("Encountered issue attempting to load mute list for client: " + getClientName(), e);
            }
        }
    }
    //Save mute list method
    private void saveMuteList() {
        File file = new File(getClientName() + "_mutelist.txt");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            for (String mutedUsername : muteList) { //Write each muted username in the muteList to the file
                writer.write(mutedUsername);
                writer.newLine();
            }
            // Log that the mute list has been successfully saved
            LoggerUtil.INSTANCE.info("Mute list saved for client: " + getClientName());
            // Log an error message if an IOException occurs while saving the file
        } catch (IOException e) {
            LoggerUtil.INSTANCE.severe("Encountered issue attempting to save mute list for client: " + getClientName(), e);
        }
    }
    
    

    

    //arc73 7/29/24
    public void addToMuteList(String username) {
        muteList.add(username);
        saveMuteList();
    }

    public void removeFromMuteList(String username) {
        muteList.remove(username);
        saveMuteList();
    }

    public boolean isMuted(String username) {
        return muteList.contains(username);
    }

    public Set<String> getMuteList() {
        return muteList;
    }

    // send methods to pass data back to the Client

    public boolean sendRooms(List<String> rooms) {
        RoomResultsPayload rrp = new RoomResultsPayload();
        rrp.setRooms(rooms);
        return send(rrp);
    }

    public boolean sendClientSync(long clientId, String clientName) {
        ConnectionPayload cp = new ConnectionPayload();
        cp.setClientId(clientId);
        cp.setClientName(clientName);
        cp.setConnect(true);
        cp.setPayloadType(PayloadType.SYNC_CLIENT);
        return send(cp);
    }

    /**
     * Overload of sendMessage used for server-side generated messages
     * 
     * @param message
     * @return @see {@link #send(Payload)}
     */
    public boolean sendMessage(String message) {
        return sendMessage(ServerThread.DEFAULT_CLIENT_ID, message, false);
    }

    /**
     * Sends a message with the author/source identifier
     * 
     * @param senderId
     * @param message
     * @return @see {@link #send(Payload)}
     */
    public boolean sendMessage(long senderId, String message, boolean isPrivate) {
        Payload p = new Payload();
        p.setClientId(senderId);
        p.setMessage(message);
        p.setPayloadType(PayloadType.MESSAGE);
        p.setPrivate(isPrivate);
        return send(p);
    }

    /**
     * Tells the client information about a client joining/leaving a room
     * 
     * @param clientId   their unique identifier
     * @param clientName their name
     * @param room       the room
     * @param isJoin     true for join, false for leaivng
     * @return success of sending the payload
     */
    public boolean sendRoomAction(long clientId, String clientName, String room, boolean isJoin) {
        ConnectionPayload cp = new ConnectionPayload();
        cp.setPayloadType(PayloadType.ROOM_JOIN);
        cp.setConnect(isJoin); // <-- determine if join or leave
        cp.setMessage(room);
        cp.setClientId(clientId);
        cp.setClientName(clientName);
        return send(cp);
    }

    /**
     * Tells the client information about a disconnect (similar to leaving a room)
     * 
     * @param clientId   their unique identifier
     * @param clientName their name
     * @return success of sending the payload
     */
    public boolean sendDisconnect(long clientId, String clientName) {
        ConnectionPayload cp = new ConnectionPayload();
        cp.setPayloadType(PayloadType.DISCONNECT);
        cp.setConnect(false);
        cp.setClientId(clientId);
        cp.setClientName(clientName);
        return send(cp);
    }

    /**
     * Sends (and sets) this client their id (typically when they first connect)
     * 
     * @param clientId
     * @return success of sending the payload
     */
    public boolean sendClientId(long clientId) {
        this.clientId = clientId;
        ConnectionPayload cp = new ConnectionPayload();
        cp.setPayloadType(PayloadType.CLIENT_ID);
        cp.setConnect(true);
        cp.setClientId(clientId);
        cp.setClientName(clientName);
        return send(cp);
    }

    // end send methods
}
