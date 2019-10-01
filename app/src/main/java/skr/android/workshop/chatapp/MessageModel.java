package skr.android.workshop.chatapp;

public class MessageModel {

    private String text;
    private String name;
    private String photoUrl;
    //  private String userId;

    public MessageModel( ) {

    }

    public MessageModel(String text, String name, String photoUrl ) {
        this.text = text;
        this.name = name;
        this.photoUrl = photoUrl;
        //  this.userId = userId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    // public String getUserId(){return userId;}

    // public void setUserId(String userId){ this.userId = userId;}

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

}

