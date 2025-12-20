package Project.Common;

/*
 * UCID: agl8
 * Date: 2025-12-19
 * Summary: Generic boolean-status payload (used for AWAY and ELIMINATED).
 */
public class StatusPayload extends Payload {
    private boolean status;
    private boolean quiet;

    public StatusPayload() {
        // caller sets payload type to AWAY/ELIMINATED or SYNC variants
    }

    public boolean getStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public boolean isQuiet() {
        return quiet;
    }

    public void setQuiet(boolean quiet) {
        this.quiet = quiet;
    }

    @Override
    public String toString() {
        return "StatusPayload{type=" + getPayloadType() +
                ", status=" + status +
                ", quiet=" + quiet +
                ", clientId=" + getClientId() +
                "}";
    }
}
