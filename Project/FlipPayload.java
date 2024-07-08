package Project;

public class FlipPayload extends Payload {
    public FlipPayload() {
        setPayloadType(PayloadType.FLIP);
    }

    public String toString() {
        return String.format("FlipPayload [Client Id: %d]", getClientId());
    }
}