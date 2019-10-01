package skr.android.workshop.chatapp;


import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    public static final String ANONYMOUS = "anonymous";
    public static final int DEFAULT_MSG_LENGTH_LIMIT = 1000;

    public static final int RC_SIGN_IN = 1;
    public static final int RC_PHOTO_PICKER = 2;
    private static final String FRIENDLY_CHAT_LENGTH_KEY = "friendly_chat_length";

    private ListView mMessageListView;
    private MessageAdapter mMessageAdapter;
    private ProgressBar mProgressBar;
    private ImageButton mPhotoPickerButton;
    private EditText mMessageEditText;
    private Button mSendButton;

    private String mUsername;

    private String eachPersonDatabaseStorageName;

    //Firebase
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private FirebaseDatabase mFirebasaeDatabase;
    private DatabaseReference mDatabaseReference;
    private ChildEventListener mChildEventListener;
    private FirebaseStorage mFirebaseStorage;
    private StorageReference mStorageReference;
    private FirebaseRemoteConfig mFirebaseRemoteConfig;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mUsername = ANONYMOUS;


        //Initialize Firebase Components
        mFirebasaeDatabase = FirebaseDatabase.getInstance();
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseStorage = FirebaseStorage.getInstance();
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();

        // get References of the database and storage
        mDatabaseReference = mFirebasaeDatabase.getReference().child("messages");
        mStorageReference = mFirebaseStorage.getReference().child("chat_photos");

        //mDatabaseReference = mFirebasaeDatabase.getReference().child(eachPersonDatabaseStorageName);


        // Initialize references to views
        mProgressBar =  findViewById(R.id.progressBar);
        mMessageListView =  findViewById(R.id.messageListView);
        mPhotoPickerButton =  findViewById(R.id.photoPickerButton);
        mMessageEditText =  findViewById(R.id.messageEditText);
        mSendButton =  findViewById(R.id.sendButton);

        // Initialize message ListView and its adapter
        List<MessageModel> friendlyMessages = new ArrayList<>();
        mMessageAdapter = new MessageAdapter(this, R.layout.item_message, friendlyMessages);
        mMessageListView.setAdapter(mMessageAdapter);

        // Initialize progress bar
        mProgressBar.setVisibility(ProgressBar.INVISIBLE);

        // ImagePickerButton shows an image picker to upload a image for a message
        mPhotoPickerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // TODO: Fire an intent to show an image picker
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/jpeg");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY , true);
                startActivityForResult(Intent.createChooser(intent , " Complete action using ") , RC_PHOTO_PICKER);

                mProgressBar.setVisibility(ProgressBar.INVISIBLE);

            }
        });

        // Enable Send button when there's text to send
        mMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() > 0) {
                    mSendButton.setEnabled(true);
                } else {
                    mSendButton.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });


        mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(DEFAULT_MSG_LENGTH_LIMIT)});

        // Send button sends a message and clears the EditText
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // TODO: Send messages on click

                //FriendlyMessage friendlyMessage = new FriendlyMessage(mMessageEditText.getText().toString() , mUsername ,null,eachPersonDatabaseStorageName);
                // mDatabaseReference.child(eachPersonDatabaseStorageName).push().setValue(friendlyMessage);
                /*
                You can create a helper class, where you have a list of variables created, which can hold the datas to be
                        saved in database.

                 */
                MessageModel friendlyMessage = new MessageModel(mMessageEditText.getText().toString() , mUsername ,null);
                mDatabaseReference.push().setValue(friendlyMessage);

                // Clear input box
                mMessageEditText.setText("");
            }
        });

        mAuthStateListener = new FirebaseAuth.AuthStateListener() {

            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {

                // This is to check whether a user's logged in state.

                FirebaseUser user = firebaseAuth.getCurrentUser();

                if ( user != null ){
                    //Signed in
                    //Toast.makeText(MainActivity.this, "You are signed in..\nWelcome to FriendlyChat..", Toast.LENGTH_SHORT).show();
                    ;

                    onSignedInInitialized(user.getDisplayName(),user.getUid());
                }
                else {
                    //Signed out
                    onSignedOutCleanUp();
                    startActivityForResult(  AuthUI.getInstance()
                                    .createSignInIntentBuilder()
                                    .setIsSmartLockEnabled(false)
                                    .setProviders( AuthUI.EMAIL_PROVIDER,
                                            AuthUI.GOOGLE_PROVIDER)
                                    .build() ,
                            RC_SIGN_IN);
                }
            }
        };

        //FirebaseRemoteConfiguration

        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(BuildConfig.DEBUG)
                .build();
        mFirebaseRemoteConfig.setConfigSettings(configSettings);

        Map<String , Object> defaultConfigMap = new HashMap<>();
        defaultConfigMap.put(FRIENDLY_CHAT_LENGTH_KEY , DEFAULT_MSG_LENGTH_LIMIT );

        mFirebaseRemoteConfig.setDefaults(defaultConfigMap);    // Setting default for FirebaseRemoteConfig

        fetchConfig();



    } // end of onCreate

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN ){            //This means that the activity is returned from our login activity

            if ( resultCode == RESULT_OK){

                Toast.makeText(this, "Signed In", Toast.LENGTH_SHORT).show();

            }
            else if ( resultCode == RESULT_CANCELED ){
               /* try {
                    Toast.makeText(this, "Bye Bye....", Toast.LENGTH_SHORT).show();

                    wait(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } */

                Toast.makeText(this, "Not Signed In....", Toast.LENGTH_SHORT).show();

                finish();
            }
        }
        else if ( requestCode == RC_PHOTO_PICKER && resultCode == RESULT_OK){

            Uri selectedImageUri = data.getData();

            // Get a reference to store file in chat_photos
            StorageReference photoRef = mStorageReference.child(eachPersonDatabaseStorageName).child(Objects.requireNonNull(selectedImageUri.getLastPathSegment()));
            // Upload the file to firbase storage
            photoRef.putFile(selectedImageUri).addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>() {

                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                    Uri downloadUrl = taskSnapshot.getUploadSessionUri();
                    assert downloadUrl != null;

                    MessageModel friendlyMessage = new MessageModel(null , mUsername , downloadUrl.toString());
                    //mDatabaseReference.child(eachPersonDatabaseStorageName).push().setValue(friendlyMessage);
                    mDatabaseReference.push().setValue(friendlyMessage);

                }
            });

        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
        fetchConfig();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if ( mAuthStateListener != null) {
            mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
        }
        detachDatabaseReadListener();
        // mMessageAdapter.clear();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int signOut = item.getItemId();
        switch (signOut){
            case R.id.sign_out_menu:

                mFirebaseAuth.signOut();
                AuthUI.getInstance().signOut(this);

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }


    private void onSignedInInitialized(String userName , String Uid) {

        mUsername = userName;
        eachPersonDatabaseStorageName = Uid;
        attachDatabaseReadListener();
    }


    private void onSignedOutCleanUp() {

        mUsername = ANONYMOUS;
        mMessageAdapter.clear();
        detachDatabaseReadListener();
    }

    private void attachDatabaseReadListener(){

        if ( mChildEventListener == null){

            mChildEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {


                    //This function gets triggered for every first time of data and when new data is added to the database
                    MessageModel friendlyMessage = dataSnapshot.getValue(MessageModel.class);
                    mMessageAdapter.add(friendlyMessage);

                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {
               /* FriendlyMessage friendlyMessage = dataSnapshot.getValue(FriendlyMessage.class);
                mMessageAdapter.add(friendlyMessage); */
                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {

                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            };

            mDatabaseReference.addChildEventListener(mChildEventListener);

        }
    }

    private void detachDatabaseReadListener() {

        if (mChildEventListener != null ){
            mDatabaseReference.removeEventListener(mChildEventListener);
            mChildEventListener = null;
        }

    }

    public void fetchConfig(){

        long catchExpression = 3600;
        if (mFirebaseRemoteConfig.getInfo().getConfigSettings().isDeveloperModeEnabled()) {
            catchExpression = 0;
        }

        mFirebaseRemoteConfig.fetch(catchExpression)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        mFirebaseRemoteConfig.activateFetched();  // this activates the parameter
                        //Toast.makeText(MainActivity.this, "Retrived length...", Toast.LENGTH_SHORT).show();
                        applyRetrivedLengthLimit();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(MainActivity.this, "Cannot retrive length", Toast.LENGTH_SHORT).show();
                        applyRetrivedLengthLimit();
                    }
                });

    }

    private void applyRetrivedLengthLimit() {

        Long friendly_msg_length = mFirebaseRemoteConfig.getLong(FRIENDLY_CHAT_LENGTH_KEY);
        mMessageEditText.setFilters(new InputFilter[]{ new InputFilter.LengthFilter(friendly_msg_length.intValue()) } );
        //Toast.makeText(MainActivity.this, "Updated length...", Toast.LENGTH_SHORT).show();

    }

}
