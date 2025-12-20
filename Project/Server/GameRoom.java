package Project.Server;

import Project.Common.*;

import java.util.*;

// agl8 12-19-2025
// RPS game room with host, RPS-5, cooldown, away, spectator, and elimination logic
public class GameRoom extends BaseGameRoom {

    // session state
    private long hostClientId = -1;

    private boolean enableRps5 = false;
    private boolean enableCooldown = false;

    private String phase = "READY"; // "READY" or "IN_PROGRESS"

    // per round data
    private final Map<Long, String> currentChoices = new HashMap<>();
    private final Map<Long, Boolean> didPick = new HashMap<>();

    // cooldown tracking across rounds
    private final Map<Long, String> lastChoice = new HashMap<>();

    // round timer
    private int roundSeconds = 20;

    public GameRoom(String name) {
        super(name);
    }

    @Override
    protected void onClientAdded(ServerThread client) {
        // sync GameRoom state to new client
        syncCurrentPhase(client);
        syncReadyStatus(client);
        if (currentPhase != Phase.READY) {
            syncTurnStatus(client);
            syncPlayerPoints(client);
        }

        // Host assignment: first client in the room becomes host
        if (hostClientId == -1) {
            hostClientId = client.getClientId();
        }

        // Sync host + settings to the joining client
        client.sendHost(hostClientId, true);
        client.sendGameSettings(enableRps5, enableCooldown, true);

        // Sync away + eliminated status to joining client
        for (ServerThread st : clientsInRoom.values()) {
            User u = st.getUser();
            client.sendAwayStatus(st.getClientId(), u.isAway(), true, true);
            client.sendEliminatedStatus(st.getClientId(), u.isEliminated(), true);
        }

        // If game already started and they join (or they didn't ready), treat them as spectator (ready=false)
        if (phase.equals("IN_PROGRESS")) {
            sendGameEventToAll(client.getUser().getDisplayName() + " joined as a spectator.");
        }
    }

    @Override
    protected void onClientRemoved(ServerThread sp) {
        LoggerUtil.INSTANCE.info("Player Removed, remaining: " + clientsInRoom.size());
        if (clientsInRoom.isEmpty()) {
            resetReadyTimer();
            stopRoundTimer();
            onSessionEnd();
        }
    }

    @Override
    protected void handleReady(ServerThread sender) {
        super.handleReady(sender);
        // After any ready change, if phase is READY, allow new settings sync
    }

    public void handleGameSetting(ServerThread sender, boolean rps5, boolean cooldown) {
        // Only host can change, and only during READY phase
        if (sender.getClientId() != hostClientId) {
            sender.sendGameEvent("Only the host can change game settings.");
            return;
        }
        if (!phase.equals("READY")) {
            sender.sendGameEvent("Settings can only be changed during Ready Check.");
            return;
        }

        enableRps5 = rps5;
        enableCooldown = cooldown;

        // Sync to all clients
        for (ServerThread st : clientsInRoom.values()) {
            st.sendGameSettings(enableRps5, enableCooldown, true);
        }
        sendGameEventToAll("Host updated settings: RPS-5=" + enableRps5 + ", Cooldown=" + enableCooldown);
    }

    public void handleAway(ServerThread sender) {
        User u = sender.getUser();
        u.setAway(!u.isAway());

        // Sync away status to everyone
        for (ServerThread st : clientsInRoom.values()) {
            st.sendAwayStatus(sender.getClientId(), u.isAway(), false, true);
        }

        // Game event message required by worksheet
        String msg = u.getDisplayName() + (u.isAway() ? " is away." : " is no longer away.");
        sendGameEventToAll(msg);
    }

    public void handleTurnAction(ServerThread sender, String raw) {
        // Spectator rule: not ready during IN_PROGRESS => spectator (ignored)
        if (phase.equals("IN_PROGRESS") && !sender.isReady()) {
            sender.sendGameEvent("You are a spectator (not ready) and cannot play this round.");
            return;
        }

        // Away rule: away users are ignored from round flow but remain in game
        if (sender.getUser().isAway()) {
            sender.sendGameEvent("You are marked away and are skipped until you return.");
            return;
        }

        // Eliminated rule: eliminated users become spectators
        if (sender.getUser().isEliminated()) {
            sender.sendGameEvent("You are eliminated and are now a spectator.");
            return;
        }

        if (raw == null) return;
        raw = raw.trim();

        if (raw.startsWith("/pick")) {
            String[] parts = raw.split("\\s+");
            if (parts.length < 2) {
                sender.sendGameEvent("Usage: /pick <r|p|s" + (enableRps5 ? "|l|k" : "") + ">");
                return;
            }
            handlePick(sender, parts[1].toLowerCase());
        }
    }

    @Override
    public void handleMessage(ServerThread sender, String message) {
        // If session is in progress and sender is not ready, they are spectator: cannot send messages
        if (phase.equals("IN_PROGRESS") && !sender.isReady()) {
            sender.sendGameEvent("Spectators cannot send chat messages.");
            return;
        }
        super.handleMessage(sender, message);
    }

    @Override
    protected void onSessionStart() {
        LoggerUtil.INSTANCE.info("onSessionStart() start");
        phase = "IN_PROGRESS";
        changePhase(Phase.IN_PROGRESS);

        sendGameEventToAll("Session started. Round beginning...");
        startRound();
        LoggerUtil.INSTANCE.info("onSessionStart() end");
    }

    private void startRound() {
        // reset round state
        currentChoices.clear();
        didPick.clear();

        // reset pick indicators on UI using DEFAULT_CLIENT_ID pattern (-1)
        syncTurnReset();

        // start timer for round
        startRoundTimer();

        sendGameEventToAll("Round started. Pick now!");
    }

    private void endRound() {
        stopRoundTimer();

        // Players who didn't pick are eliminated (but stay as spectators)
        for (ServerThread st : clientsInRoom.values()) {
            if (!isEligiblePlayer(st)) continue;

            boolean picked = didPick.getOrDefault(st.getClientId(), false);
            if (!picked) {
                st.getUser().setEliminated(true);
                syncEliminatedToAll(st.getClientId(), true);
                sendGameEventToAll(st.getUser().getDisplayName() + " did not pick and is eliminated.");
            }
        }

        resolveBattlesClockwise();

        int alive = countAlivePlayers();
        if (alive <= 1) {
            endSessionWithWinnerOrTie();
        } else {
            startRound();
        }
    }

    private void endSessionWithWinnerOrTie() {
        int alive = countAlivePlayers();
        if (alive == 1) {
            ServerThread winner = getLastAlivePlayer();
            sendGameEventToAll("Game over! Winner: " + winner.getUser().getDisplayName());
        } else {
            sendGameEventToAll("Game over! No players remaining (tie).");
        }

        // scoreboard sorted by points desc
        List<ServerThread> all = new ArrayList<>(clientsInRoom.values());
        all.sort((a, b) -> Integer.compare(b.getUser().getPoints(), a.getUser().getPoints()));

        StringBuilder sb = new StringBuilder("Final Scoreboard:\n");
        for (ServerThread st : all) {
            sb.append(st.getUser().getDisplayName())
              .append(" (#").append(st.getClientId()).append(") - ")
              .append(st.getUser().getPoints()).append(" pts\n");
        }
        sendGameEventToAll(sb.toString());

        // reset session state without disconnecting (per requirement)
        resetSession();
    }

    private void resetSession() {
        phase = "READY";
        changePhase(Phase.READY);

        // reset each user server-side
        for (ServerThread st : clientsInRoom.values()) {
            st.getUser().reset();
            lastChoice.remove(st.getClientId());
            syncPointsToAll(st.getClientId(), 0);
            syncEliminatedToAll(st.getClientId(), false);
            syncAwayToAll(st.getClientId(), false, true);
        }

        // require new ready check
        resetReadyStatus();
        sendGameEventToAll("Session reset. Ready check required to start again.");
    }

    public void handlePick(ServerThread sender, String choice) {
        // Away users cannot pick
        if (sender.getUser().isAway()) {
            sender.sendGameEvent("You are marked as away. Toggle away status to participate.");
            return;
        }

        if (!isValidChoice(choice)) {
            sender.sendGameEvent("Invalid choice. Use r/p/s" + (enableRps5 ? "/l/k" : ""));
            return;
        }

        // cooldown: same option can't be picked twice in a row by same player
        if (enableCooldown) {
            String last = lastChoice.get(sender.getClientId());
            if (last != null && last.equals(choice)) {
                sender.sendGameEvent("Cooldown: you can't pick the same option twice in a row.");
                return;
            }
        }

        currentChoices.put(sender.getClientId(), choice);
        didPick.put(sender.getClientId(), true);

        // sync picked indicator (TURN true)
        syncTurnPicked(sender.getClientId(), true);

        // confirm choice privately to sender (used by client cooldown UI)
        sender.sendChoiceAck(sender.getClientId(), choice);

        // record last choice after acceptance
        lastChoice.put(sender.getClientId(), choice);

        sendGameEventToAll(sender.getUser().getDisplayName() + " picked their choice.");

        // if all eligible players picked, end round early
        if (allEligiblePicked()) {
            sendGameEventToAll("All players picked. Ending round early...");
            endRound();
        }
    }

    private void resolveBattlesClockwise() {
        List<ServerThread> players = getEligiblePlayersInOrder();
        if (players.size() < 2) return;

        // Each player attacks next player clockwise: A->B, B->C, C->A
        for (int i = 0; i < players.size(); i++) {
            ServerThread attacker = players.get(i);
            ServerThread defender = players.get((i + 1) % players.size());

            if (!isEligiblePlayer(attacker) || !isEligiblePlayer(defender)) continue;

            String a = currentChoices.get(attacker.getClientId());
            String d = currentChoices.get(defender.getClientId());

            if (a == null || d == null) continue;

            int result = compareRps(a, d);
            if (result == 0) {
                sendGameEventToAll(attacker.getUser().getDisplayName() + " (" + a + ") tied " +
                        defender.getUser().getDisplayName() + " (" + d + ")");
                continue;
            }

            ServerThread winner = (result > 0) ? attacker : defender;
            ServerThread loser = (winner == attacker) ? defender : attacker;

            // Points for winner
            winner.changePoints(1);
            syncPointsToAll(winner.getClientId(), winner.getPoints());

            sendGameEventToAll(attacker.getUser().getDisplayName() + " (" + a + ") vs " +
                    defender.getUser().getDisplayName() + " (" + d + ") => Winner: " +
                    winner.getUser().getDisplayName());

            // Eliminate loser (stays as spectator)
            loser.getUser().setEliminated(true);
            syncEliminatedToAll(loser.getClientId(), true);
            sendGameEventToAll(loser.getUser().getDisplayName() + " was eliminated.");
        }
    }

    // Full RPS-5 compare: r,p,s,l,k (Spock = k)
    // returns 1 if A beats B, -1 if A loses to B, 0 tie
    private int compareRps(String a, String b) {
        if (a.equals(b)) return 0;

        // Rock beats Scissors & Lizard
        if (a.equals("r") && (b.equals("s") || b.equals("l"))) return 1;
        // Paper beats Rock & Spock
        if (a.equals("p") && (b.equals("r") || b.equals("k"))) return 1;
        // Scissors beats Paper & Lizard
        if (a.equals("s") && (b.equals("p") || b.equals("l"))) return 1;
        // Lizard beats Spock & Paper
        if (a.equals("l") && (b.equals("k") || b.equals("p"))) return 1;
        // Spock beats Scissors & Rock
        if (a.equals("k") && (b.equals("s") || b.equals("r"))) return 1;

        return -1;
    }

    private boolean isEligiblePlayer(ServerThread st) {
        if (st == null) return false;
        User u = st.getUser();
        if (u == null) return false;
        if (u.isEliminated()) return false;
        if (u.isAway()) return false;
        // spectator = not ready during in-progress
        if (phase.equals("IN_PROGRESS") && !st.isReady()) return false;
        return true;
    }

    private boolean isValidChoice(String c) {
        if (c == null) return false;
        if (c.equals("r") || c.equals("p") || c.equals("s")) return true;
        if (!enableRps5) return false;
        return c.equals("l") || c.equals("k");
    }

    private boolean allEligiblePicked() {
        for (ServerThread st : clientsInRoom.values()) {
            if (!isEligiblePlayer(st)) continue;
            if (!didPick.getOrDefault(st.getClientId(), false)) return false;
        }
        return true;
    }

    private int countAlivePlayers() {
        int count = 0;
        for (ServerThread st : clientsInRoom.values()) {
            User u = st.getUser();
            // Only count players who are not eliminated AND not away
            if (u != null && !u.isEliminated() && !u.isAway()) count++;
        }
        return count;
    }

    private ServerThread getLastAlivePlayer() {
        for (ServerThread st : clientsInRoom.values()) {
            User u = st.getUser();
            // Only consider players who are not eliminated AND not away
            if (u != null && !u.isEliminated() && !u.isAway()) return st;
        }
        return null;
    }

    private List<ServerThread> getEligiblePlayersInOrder() {
        List<ServerThread> res = new ArrayList<>();
        for (ServerThread st : clientsInRoom.values()) {
            if (isEligiblePlayer(st)) res.add(st);
        }
        return res;
    }

    private void sendGameEventToAll(String msg) {
        for (ServerThread st : clientsInRoom.values()) st.sendGameEvent(msg);
    }

    private void syncPointsToAll(long clientId, int points) {
        for (ServerThread st : clientsInRoom.values()) st.sendPlayerPoints(clientId, points);
    }

    private void syncEliminatedToAll(long clientId, boolean isElim) {
        for (ServerThread st : clientsInRoom.values()) st.sendEliminatedStatus(clientId, isElim, true);
    }

    private void syncAwayToAll(long clientId, boolean isAway, boolean quiet) {
        for (ServerThread st : clientsInRoom.values()) st.sendAwayStatus(clientId, isAway, quiet, true);
    }

    private void syncTurnReset() {
        // Use sendResetTurnStatus which sends RESET_TURN payload
        for (ServerThread st : clientsInRoom.values()) st.sendResetTurnStatus();
    }

    private void syncTurnPicked(long clientId, boolean didPickChoice) {
        // Use sendTurnStatus which sends ReadyPayload (expected by client's processTurn)
        for (ServerThread st : clientsInRoom.values()) st.sendTurnStatus(clientId, didPickChoice);
    }

    // sync helpers from base class adapted
    private void syncTurnStatus(ServerThread incomingClient) {
        for (ServerThread st : clientsInRoom.values()) {
            if (st.getClientId() != incomingClient.getClientId()) {
                incomingClient.sendTurnStatus(st.getClientId(), st.didTakeTurn(), true);
            }
        }
    }

    private void syncPlayerPoints(ServerThread incomingClient) {
        for (ServerThread st : clientsInRoom.values()) {
            if (st.getClientId() != incomingClient.getClientId()) {
                incomingClient.sendPlayerPoints(st.getClientId(), st.getPoints());
            }
        }
    }

    private Timer roundTimer;
    private int currentRoundTime;

    private void startRoundTimer() {
        currentRoundTime = roundSeconds;
        syncRoundTime();

        roundTimer = new Timer();
        roundTimer.scheduleAtFixedRate(new java.util.TimerTask() {
            @Override
            public void run() {
                currentRoundTime--;
                syncRoundTime();

                if (currentRoundTime <= 0) {
                    roundTimer.cancel();
                    sendGameEventToAll("Round timer expired. Ending round...");
                    endRound();
                }
            }
        }, 1000, 1000);
    }

    private void stopRoundTimer() {
        if (roundTimer != null) {
            roundTimer.cancel();
            roundTimer = null;
        }
        // hide timer (baseline: -1 hides)
        for (ServerThread st : clientsInRoom.values()) st.sendCurrentTimer(TimerType.ROUND, -1);
    }

    private void syncRoundTime() {
        for (ServerThread st : clientsInRoom.values()) st.sendCurrentTimer(TimerType.ROUND, currentRoundTime);
    }

    // Required abstract method implementations
    @Override
    protected void onRoundStart() {
        // handled by startRound()
    }

    @Override
    protected void onTurnStart() {
        // not used in RPS - all players pick simultaneously
    }

    @Override
    protected void onTurnEnd() {
        // not used in RPS - all players pick simultaneously
    }

    @Override
    protected void onRoundEnd() {
        // handled by endRound()
    }

    @Override
    protected void onSessionEnd() {
        LoggerUtil.INSTANCE.info("onSessionEnd() start");
        phase = "READY";
        changePhase(Phase.READY);
        resetReadyStatus();
        LoggerUtil.INSTANCE.info("onSessionEnd() end");
    }
}