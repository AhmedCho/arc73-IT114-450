package Project.Common;

//  arc73 7/8/24
public class FlipPayload extends Payload {
    public FlipPayload() {
        setPayloadType(PayloadType.FLIP);
    }

    public String toString() {
        return String.format("FlipPayload [Client Id: %d]", getClientId());
    }
}