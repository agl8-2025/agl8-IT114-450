// agl8 10-26-25
// Payload class in charge of sending player point updates
package Project.Common;

public class PointsPayload extends Payload {
    private int points;

    public PointsPayload() {
        setPayloadType(PayloadType.POINTS); // IMPORTANT so it doesn't send null type
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    @Override
    public String toString() {
        return super.toString() + String.format(", Points: [%d]", getPoints());
    }
}