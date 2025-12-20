package Project.Client.Interfaces;

// agl8 12-19-2025
// UI callback for receiving elimination status updates
public interface IEliminationEvents extends IClientEvents {
    void onEliminationUpdated(long clientId, boolean isEliminated);
}
