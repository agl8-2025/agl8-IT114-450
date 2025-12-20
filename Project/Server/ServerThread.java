package Project.Server;

import java.net.Socket;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import Project.Common.TextFX.Color;
import Project.Common.TimerPayload;
import Project.Common.TimerType;
import Project.Common.ChoicePayload;
import Project.Common.ConnectionPayload;
import Project.Common.Constants;
import Project.Common.GameSettingPayload;
import Project.Common.HostPayload;
import Project.Common.LoggerUtil;
import Project.Common.Payload;
import Project.Common.PayloadType;
import Project.Common.Phase;
import Project.Common.PointsPayload;
import Project.Common.ReadyPayload;
import Project.Common.RoomAction;
import Project.Common.RoomResultPayload;
import Project.Common.StatusPayload;
import Project.Common.TextFX;
import Project.Common.User;

/*
 * UCID: agl8
 * Date: 2025-12-19
 * Summary: Server-side thread wrapper. Receives payloads and routes to Room/GameRoom handle methods.
 *          Adds send/sync helpers for host, settings, away, elimination, and choice confirmation.
 */
public class ServerThread extends BaseServerThread {
    private Consumer<ServerThread> onInitializationComplete; // callback to inform when this object is ready

    /**
     * A wrapper method so we don't need to keep typing out the long/complex sysout
     * line inside
     * 
     * @param message
     */
    @Override
    protected void info(String message) {
        LoggerUtil.INSTANCE
                .info(TextFX.colorize(String.format("Thread[%s]: %s", this.getClientId(), message), Color.CYAN));
    }

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
        this.onInitializationComplete = onInitializationComplete;
    }

    // agl8 - 12/19/2025
    // exposes the user object for GameRoom access
    public User getUser() {
        return this.user;
    }

    // agl8 - 12/19/2025
    // sends payload to client
    public void sendPayload(Payload p) {
        sendToClient(p);
    }

    // Start Send*() Methods
    /**
     * Syncs a specific client's points
     * 
     * @param clientId
     * @param points
     * @return
     */
    public boolean sendPlayerPoints(long clientId, int points) {
        PointsPayload rp = new PointsPayload();
        rp.setPoints(points);
        rp.setClientId(clientId);
        return sendToClient(rp);
    }

    public boolean sendGameEvent(String str) {
        return sendMessage(Constants.GAME_EVENT_CHANNEL, str);
    }

    /**
     * Syncs the current time of a specific TimerType
     * 
     * @param timerType
     * @param time
     * @return
     */
    public boolean sendCurrentTime(TimerType timerType, int time) {
        TimerPayload tp = new TimerPayload();
        tp.setTime(time);
        tp.setTimerType(timerType);
        return sendToClient(tp);
    }

    // agl8 - 12/19/2025
    // Milestone 3: send current timer for round
    public void sendCurrentTimer(TimerType type, int secondsRemaining) {
        TimerPayload tp = new TimerPayload();
        tp.setPayloadType(PayloadType.TIME);
        tp.setTimerType(type);
        tp.setTime(secondsRemaining);
        sendToClient(tp);
    }

    public boolean sendResetTurnStatus() {
        ReadyPayload rp = new ReadyPayload();
        rp.setPayloadType(PayloadType.RESET_TURN);
        return sendToClient(rp);
    }

    public boolean sendTurnStatus(long clientId, boolean didTakeTurn) {
        return sendTurnStatus(clientId, didTakeTurn, false);
    }

    public boolean sendTurnStatus(long clientId, boolean didTakeTurn, boolean quiet) {
        ReadyPayload rp = new ReadyPayload();
        rp.setPayloadType(quiet ? PayloadType.SYNC_TURN : PayloadType.TURN);
        rp.setClientId(clientId);
        rp.setReady(didTakeTurn);
        return sendToClient(rp);
    }

    public boolean sendCurrentPhase(Phase phase) {
        Payload p = new Payload();
        p.setPayloadType(PayloadType.PHASE);
        p.setMessage(phase.name());
        return sendToClient(p);
    }

    public boolean sendResetReady() {
        ReadyPayload rp = new ReadyPayload();
        rp.setPayloadType(PayloadType.RESET_READY);
        return sendToClient(rp);
    }

    public boolean sendReadyStatus(long clientId, boolean isReady) {
        return sendReadyStatus(clientId, isReady, false);
    }

    public boolean sendReadyStatus(long clientId, boolean isReady, boolean quiet) {
        ReadyPayload rp = new ReadyPayload();
        rp.setClientId(clientId);
        rp.setReady(isReady);
        if (quiet) {
            rp.setPayloadType(PayloadType.SYNC_READY);
        }
        return sendToClient(rp);
    }

    public boolean sendRooms(List<String> rooms) {
        RoomResultPayload rrp = new RoomResultPayload();
        rrp.setRooms(rooms);
        return sendToClient(rrp);
    }

    // agl8 - 12/19/2025
    // Milestone 3: send host info to client
    public void sendHost(long hostClientId, boolean isSync) {
        HostPayload hp = new HostPayload();
        hp.setPayloadType(isSync ? PayloadType.SYNC_HOST : PayloadType.HOST);
        hp.setHostClientId(hostClientId);
        sendToClient(hp);
    }

    // agl8 - 12/19/2025
    // Milestone 3: send game settings to client
    public void sendGameSettings(boolean enableRps5, boolean enableCooldown, boolean isSync) {
        GameSettingPayload gp = new GameSettingPayload();
        gp.setPayloadType(isSync ? PayloadType.SYNC_GAME_SETTING : PayloadType.GAME_SETTING);
        gp.setEnableRps5(enableRps5);
        gp.setEnableCooldown(enableCooldown);
        sendToClient(gp);
    }

    // agl8 - 12/19/2025
    // Milestone 3: send away status to client
    public void sendAwayStatus(long clientId, boolean isAway, boolean isQuiet, boolean isSync) {
        StatusPayload sp = new StatusPayload();
        sp.setPayloadType(isSync ? PayloadType.SYNC_AWAY : PayloadType.AWAY);
        sp.setClientId(clientId);
        sp.setStatus(isAway);
        sp.setQuiet(isQuiet);
        sendToClient(sp);
    }

    // agl8 - 12/19/2025
    // Milestone 3: send elimination status to client
    public void sendEliminatedStatus(long clientId, boolean isEliminated, boolean isSync) {
        StatusPayload sp = new StatusPayload();
        sp.setPayloadType(isSync ? PayloadType.SYNC_ELIMINATED : PayloadType.ELIMINATED);
        sp.setClientId(clientId);
        sp.setStatus(isEliminated);
        sp.setQuiet(true);
        sendToClient(sp);
    }

    // agl8 - 12/19/2025
    // Milestone 3: send choice acknowledgment (for cooldown UI)
    public void sendChoiceAck(long clientId, String choice) {
        ChoicePayload cp = new ChoicePayload();
        cp.setPayloadType(PayloadType.CHOICE);
        cp.setClientId(clientId);
        cp.setChoice(choice);
        sendToClient(cp);
    }

    protected boolean sendDisconnect(long clientId) {
        Payload payload = new Payload();
        payload.setClientId(clientId);
        payload.setPayloadType(PayloadType.DISCONNECT);
        return sendToClient(payload);
    }

    protected boolean sendResetUserList() {
        return sendClientInfo(Constants.DEFAULT_CLIENT_ID, null, null, RoomAction.JOIN);
    }

    protected boolean sendClientInfo(long clientId, String clientName, String roomName, RoomAction action) {
        return sendClientInfo(clientId, clientName, roomName, action, false);
    }

    protected boolean sendClientInfo(long clientId, String clientName, String roomName, RoomAction action,
            boolean isSync) {
        ConnectionPayload payload = new ConnectionPayload();
        switch (action) {
            case JOIN:
                payload.setPayloadType(PayloadType.ROOM_JOIN);
                break;
            case LEAVE:
                payload.setPayloadType(PayloadType.ROOM_LEAVE);
                break;
            default:
                break;
        }
        if (isSync) {
            payload.setPayloadType(PayloadType.SYNC_CLIENT);
        }
        payload.setClientId(clientId);
        payload.setClientName(clientName);
        payload.setMessage(roomName);
        return sendToClient(payload);
    }

    protected boolean sendClientId() {
        ConnectionPayload payload = new ConnectionPayload();
        payload.setPayloadType(PayloadType.CLIENT_ID);
        payload.setClientId(getClientId());
        payload.setClientName(getClientName());
        return sendToClient(payload);
    }

    protected boolean sendMessage(long clientId, String message) {
        Payload payload = new Payload();
        payload.setPayloadType(PayloadType.MESSAGE);
        payload.setMessage(message);
        payload.setClientId(clientId);
        return sendToClient(payload);
    }

    // End Send*() Methods
    @Override
    protected void processPayload(Payload incoming) {

        switch (incoming.getPayloadType()) {
            case CLIENT_CONNECT:
                setClientName(((ConnectionPayload) incoming).getClientName().trim());
                break;
            case DISCONNECT:
                currentRoom.handleDisconnect(this);
                break;
            case MESSAGE:
                currentRoom.handleMessage(this, incoming.getMessage());
                break;
            case REVERSE:
                currentRoom.handleReverseText(this, incoming.getMessage());
                break;
            case ROOM_CREATE:
                currentRoom.handleCreateRoom(this, incoming.getMessage());
                break;
            case ROOM_JOIN:
                currentRoom.handleJoinRoom(this, incoming.getMessage());
                break;
            case ROOM_LEAVE:
                currentRoom.handleJoinRoom(this, Room.LOBBY);
                break;
            case ROOM_LIST:
                currentRoom.handleListRooms(this, incoming.getMessage());
                break;
            case READY:
                try {
                    ((GameRoom) currentRoom).handleReady(this);
                } catch (Exception e) {
                    sendMessage(Constants.DEFAULT_CLIENT_ID, "You must be in a GameRoom to do the ready check");
                }
                break;
            case TURN:
                try {
                    ((GameRoom) currentRoom).handleTurnAction(this, incoming.getMessage());
                } catch (Exception e) {
                    sendMessage(Constants.DEFAULT_CLIENT_ID, "You must be in a GameRoom to do a turn");
                }
                break;
            case PICK:
                try {
                    ((GameRoom) currentRoom).handlePick(this, incoming.getMessage());
                }
                catch (Exception e) {
                    sendMessage(Constants.DEFAULT_CLIENT_ID, "You must be in a Gameroom to pick.");
                }
                break;
            // agl8 - 12/19/2025
            // Milestone 3: routes GAME_SETTING payload to GameRoom
            case GAME_SETTING:
                try {
                    if (incoming instanceof GameSettingPayload gp) {
                        ((GameRoom) currentRoom).handleGameSetting(this, gp.isEnableRps5(), gp.isEnableCooldown());
                    }
                }
                catch (Exception e) {
                    sendMessage(Constants.DEFAULT_CLIENT_ID, "You must be in a GameRoom to change settings.");
                }
                break;
            // agl8 - 12/19/2025
            // Milestone 3: routes AWAY payload to GameRoom
            case AWAY:
                try {
                    ((GameRoom) currentRoom).handleAway(this);
                }
                catch (Exception e) {
                    sendMessage(Constants.DEFAULT_CLIENT_ID, "You must be in a GameRoom to toggle away.");
                }
                break;
            default:
                LoggerUtil.INSTANCE.warning(TextFX.colorize("Unknown payload type received", Color.RED));
                break;
        }
    }

    // limited user data exposer
    protected boolean isReady() {
        return this.user.isReady();
    }

    protected void setReady(boolean isReady) {
        this.user.setReady(isReady);
    }

    protected boolean didTakeTurn() {
        return this.user.didTakeTurn();
    }

    protected void setTookTurn(boolean tookTurn) {
        this.user.setTookTurn(tookTurn);
    }

    protected int getPoints() {
        return this.user.getPoints();
    }

    protected void setPoints(int points) {
        this.user.setPoints(points);
    }

    protected void changePoints(int points) {
        this.user.setPoints(this.user.getPoints() + points);
    }

    @Override
    protected void onInitialized() {
        onInitializationComplete.accept(this);
    }
}