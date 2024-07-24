package Project.Common;


public class RollPayload extends Payload {
    //Number of dice being rolled
    private int Dicenumber;
    //Number of sides on each die
    private int Sidesnumber;
    // Constructor sets payload type to ROLL
    public RollPayload() {
        setPayloadType(PayloadType.ROLL);              
    }
    //Getter - # of Dice
    public int getDiceNumber() {
        return Dicenumber;
    }
    //Setter - # of Dice
    public void setDicenumber(int Dicenumber) {
        this.Dicenumber = Dicenumber;
    }
    //Getter - # of Sides each die
    public int getSidesNumber() {
        return Sidesnumber; 
    }
    //Setter - # of Sides each die
    public void setSidesnumber(int Sidesnumber) {
        this.Sidesnumber = Sidesnumber;
    }
    //String output of RollPayload object
    @Override
    public String toString() {
        return "RollPayload [Dicenumber=" + Dicenumber + ", Sidesnumber=" + Sidesnumber + "]";
    }
}