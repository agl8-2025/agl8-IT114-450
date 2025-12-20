package Project.Client.Interfaces;

// agl8 12-19-2025
// UI callback for receiving away status updates
public interface IAwayEvents extends IClientEvents {
    void onAwayStatusUpdated(long clientId, boolean isAway, boolean isQuiet);
}
