package Project;

public class RollPayload extends Payload {
    private int numDice;
    private int numSides;

    public RollPayload() {
        setPayloadType(PayloadType.ROLL);              
    }

    public int getNumDice() {
        return numDice;
    }

    public void setNumDice(int numDice) {
        this.numDice = numDice;
    }

    public int getNumSides() {
        return numSides;
    }

    public void setNumSides(int numSides) {
        this.numSides = numSides;
    }

    @Override
    public String toString() {
        return String.format("RollPayload [numDice=%d, numSides=%d]", numDice, numSides);
    }
}