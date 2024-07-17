package Project.Common;

import java.io.Serializable;

//arc73 7/22/24
public class Payload implements Serializable {
    private static final long serialVersionUID = 1L;
    private PayloadType payloadType;
    private long clientId;
    private String message;
    private String targetUsername; //Initializes variable for specified username
    private boolean isPrivate;
    

    public PayloadType getPayloadType() {
        return payloadType;
    }



    public void setPayloadType(PayloadType payloadType) {
        this.payloadType = payloadType;
    }



    public long getClientId() {
        return clientId;                                             
    }              



    public void setClientId(long clientId) {
        this.clientId = clientId;
    }



    public String getMessage() {
        return message;
    }



    public void setMessage(String message) {
        this.message = message;
    }



    public String getTargetUsername() { //Getter for target username
        return targetUsername;
    }



    public void setTargetUsername(String targetUsername) { //Setter for target username
        this.targetUsername = targetUsername;
    }



    public boolean isPrivate() { //Getter for isPrivate
        return isPrivate;
    }



    public void setPrivate(boolean isPrivate) { //Setter for isPrivate
        this.isPrivate = isPrivate;
    }




    @Override
    public String toString(){
        return String.format("Payload[%s] Client Id [%s] Message: [%s]", getPayloadType(), getClientId(), getMessage());
    }
}
