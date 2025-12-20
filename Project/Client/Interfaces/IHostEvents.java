package Project.Client.Interfaces;

// agl8 12-19-2025
// UI callback for receiving host updates
public interface IHostEvents extends IClientEvents {
    void onHostUpdate(long hostClientId);
}
