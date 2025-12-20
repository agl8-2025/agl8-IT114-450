package Project.Common;

/*
 * UCID: agl8
 * Date: 2025-12-19
 * Summary: Payload to sync game settings (RPS-5 toggle, cooldown toggle).
 */
public class GameSettingPayload extends Payload {
    private boolean enableRps5;
    private boolean enableCooldown;

    public GameSettingPayload() {
        setPayloadType(PayloadType.GAME_SETTING);
    }

    public boolean isEnableRps5() {
        return enableRps5;
    }

    public void setEnableRps5(boolean enableRps5) {
        this.enableRps5 = enableRps5;
    }

    public boolean isEnableCooldown() {
        return enableCooldown;
    }

    public void setEnableCooldown(boolean enableCooldown) {
        this.enableCooldown = enableCooldown;
    }

    @Override
    public String toString() {
        return "GameSettingPayload{type=" + getPayloadType() +
                ", enableRps5=" + enableRps5 +
                ", enableCooldown=" + enableCooldown +
                ", clientId=" + getClientId() +
                "}";
    }
}
