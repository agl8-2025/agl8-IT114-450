package Project.Common;

/*
 * UCID: agl8
 * Date: 2025-12-19
 * Summary: Payload used by server to announce/sync which clientId is the host (session creator).
 */
public class HostPayload extends Payload {
    private long hostClientId;

    public HostPayload() {
        setPayloadType(PayloadType.HOST);
    }

    public long getHostClientId() {
        return hostClientId;
    }

    public void setHostClientId(long hostClientId) {
        this.hostClientId = hostClientId;
    }

    @Override
    public String toString() {
        return "HostPayload{type=" + getPayloadType() +
                ", hostClientId=" + hostClientId +
                ", clientId=" + getClientId() +
                "}";
    }
}
