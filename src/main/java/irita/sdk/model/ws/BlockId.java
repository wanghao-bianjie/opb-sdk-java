package irita.sdk.model.ws;

public class BlockId {

    private String hash;
    private Parts parts;

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getHash() {
        return hash;
    }

    public void setParts(Parts parts) {
        this.parts = parts;
    }

    public Parts getParts() {
        return parts;
    }

}