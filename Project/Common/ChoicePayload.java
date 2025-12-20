package Project.Common;

/*
 * UCID: agl8
 * Date: 2025-12-19
 * Summary: Private confirmation payload sent to a player for their last accepted choice (used for cooldown UI).
 */
public class ChoicePayload extends Payload {
    private String choice;

    public ChoicePayload() {
        setPayloadType(PayloadType.CHOICE);
    }

    public String getChoice() {
        return choice;
    }

    public void setChoice(String choice) {
        this.choice = choice;
    }

    @Override
    public String toString() {
        return "ChoicePayload{type=" + getPayloadType() +
                ", choice=" + choice +
                ", clientId=" + getClientId() +
                "}";
    }
}
