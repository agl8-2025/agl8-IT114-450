package Project.Common;

/*
 * UCID: agl8
 * Date: 2025-12-19
 * Summary: Extends User state for Milestone 3: away and eliminated flags (synced server->client).
 */
public class User {
    private long clientId = Constants.DEFAULT_CLIENT_ID;
    private String clientName;
    private boolean isReady = false;
    private boolean tookTurn = false;
    private int points = 0;

    // Milestone 3 flags
    private boolean away = false;
    private boolean eliminated = false;

    /**
     * @return the points
     */
    public int getPoints() {
        return points;
    }

    /**
     * @param points the points to set
     */
    public void setPoints(int points) {
        this.points = points;
    }

    /**
     * @return the clientId
     */
    public long getClientId() {
        return clientId;
    }

    /**
     * @param clientId the clientId to set
     */
    public void setClientId(long clientId) {
        this.clientId = clientId;
    }

    /**
     * @return the username
     */
    public String getClientName() {
        return clientName;
    }

    /**
     * @param username the username to set
     */
    public void setClientName(String username) {
        this.clientName = username;
    }

    public String getDisplayName() {
        return String.format("%s#%s", this.clientName, this.clientId);
    }

    public boolean isReady() {
        return isReady;
    }

    public void setReady(boolean isReady) {
        this.isReady = isReady;
    }

    public void reset() {
        // Note: clientId and clientName should NOT be reset - they persist across game sessions
        this.isReady = false;
        this.tookTurn = false;
        this.points = 0;
        this.away = false;
        this.eliminated = false;
    }

    /**
     * @return the tookTurn
     */
    public boolean didTakeTurn() {
        return tookTurn;
    }

    /**
     * @param tookTurn the tookTurn to set
     */
    public void setTookTurn(boolean tookTurn) {
        this.tookTurn = tookTurn;
    }

    // agl8 - 12/19/2025
    // away status getter/setter
    public boolean isAway() {
        return away;
    }

    public void setAway(boolean isAway) {
        this.away = isAway;
    }

    // agl8 - 12/19/2025
    // eliminated status getter/setter
    public boolean isEliminated() {
        return eliminated;
    }

    public void setEliminated(boolean isEliminated) {
        this.eliminated = isEliminated;
    }
}
