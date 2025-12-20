package Project.Client.Interfaces;

// agl8 12-19-2025
// UI callback for receiving game setting changes
public interface IGameSettingEvents extends IClientEvents {
    void onGameSettingsUpdate(boolean enableRps5, boolean enableCooldown);
}
