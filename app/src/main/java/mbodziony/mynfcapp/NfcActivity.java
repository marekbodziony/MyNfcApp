package mbodziony.mynfcapp;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.nfc.tech.NfcA;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;

public class NfcActivity extends AppCompatActivity {

    private EditText text;
    private Button wtiteToTagBtn;

    private boolean mWriteMode = false;
    private boolean mResume = false;

    private NfcAdapter mNfcAdapter;

    private PendingIntent nfcPendingIntent;
    private IntentFilter[] mNdefExchangeFilters;
    private IntentFilter[] mWriteTagFilters;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nfc);

        text = (EditText) findViewById(R.id.text);
        text.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) { }
            @Override
            public void afterTextChanged(Editable editable) {
                mNfcAdapter.setNdefPushMessage(convertTextToNdefMessage(),NfcActivity.this);
            }
        });

        wtiteToTagBtn = (Button)findViewById(R.id.write_to_tag);
        wtiteToTagBtn.setOnClickListener(new View.OnClickListener() {
            // write to tag as long as the dialog is shown
            @Override
            public void onClick(View view) {
                disableNdefExchange();
                enableTagWrite();
                new AlertDialog.Builder(NfcActivity.this).setTitle("Touch TAG to write")
                        .setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialogInterface) {
                                disableTagWrite();
                                enableNdefExchange();
                            }
                }).create().show();
            }
        });

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

        // Handle all of received NFC intents in this activity.
        nfcPendingIntent = PendingIntent.getActivity(this,0,new Intent(this,getClass()).setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),0);

        // Intent filters for reading a text from exchanging over p2p.
        IntentFilter ndefFilter = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        try{
            ndefFilter.addDataType("text/plain");      // set payload type which interests us in NDEF message
            mNdefExchangeFilters = new IntentFilter[]{ ndefFilter };
        } catch (IntentFilter.MalformedMimeTypeException e) {
            e.printStackTrace();
        }

        // Intent filters for writing to a tag
        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        mWriteTagFilters = new IntentFilter[]{tagDetected};

    }


    @Override
    protected void onNewIntent (Intent intent){
        // NDEF exchange mode
        if (!mWriteMode && NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())){
            NdefMessage[] msgs = getNdefMessages(intent);
            byte [] payload = msgs[0].getRecords()[0].getPayload();
            text.setText(new String(payload));
        }
        //Tag writing mode
        if (mWriteMode && NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())){
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            writeTag(convertTextToNdefMessage(),tag);
        }
    }


    @Override
    public void onResume(){
        super.onResume();
        mResume = true;
        // if NDEF message received from other device
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())){
            NdefMessage[] ndefMessages = getNdefMessages(getIntent());
            byte [] payload = ndefMessages[0].getRecords()[0].getPayload();
            text.setText(new String(payload));
            setIntent(new Intent()); // Consume this intent.
        }
        enableNdefExchange();
    }

    @Override
    public void onPause(){
        super.onPause();
        mResume = false;
        mNfcAdapter.disableForegroundDispatch(this);
    }

    // method convert text String into NDEF message
    private NdefMessage convertTextToNdefMessage() {
        byte[] textByte = text.getText().toString().getBytes();
        NdefRecord ndefRecord = new NdefRecord(NdefRecord.TNF_WELL_KNOWN,NdefRecord.RTD_TEXT, new byte[0],textByte);
//        Toast.makeText(this,"New NDEF message created",Toast.LENGTH_SHORT).show();
        return new NdefMessage(new NdefRecord[]{ndefRecord, NdefRecord.createApplicationRecord("mbodziony.mynfcapp")});
    }

    // method to get NDEF messages from Intent
    private NdefMessage[] getNdefMessages(Intent intent) {
        // Parse the intent
        NdefMessage[] msgs = null;
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action) || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())){
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            if (rawMsgs != null){
                msgs = new NdefMessage[rawMsgs.length];
                for (int i = 0; i < rawMsgs.length; i ++){
                    msgs[i] = (NdefMessage)rawMsgs[i];
                }
            }
            else {
                // unknown tag
                byte[] empty = new byte[]{};
                NdefRecord record = new NdefRecord(NdefRecord.TNF_UNKNOWN,empty,empty,empty);
                NdefMessage msg = new NdefMessage(new NdefRecord[]{record});
                msgs = new NdefMessage[]{msg};
            }
        }
        else{
            Toast.makeText(this,"Unknown intent!",Toast.LENGTH_LONG).show();
            finish();
        }
        return msgs;
    }

    // enable NDEF exchange between Android devices
    private void enableNdefExchange(){
        mNfcAdapter.enableForegroundDispatch(NfcActivity.this,nfcPendingIntent,mNdefExchangeFilters,null);
        mNfcAdapter.setNdefPushMessage(convertTextToNdefMessage(),NfcActivity.this);
    }
    // disable NDEF exchange between Android devices
    private void disableNdefExchange(){
        mNfcAdapter.disableForegroundDispatch(NfcActivity.this);
    }
    // enable tag writing
    private void enableTagWrite(){
        mNfcAdapter.disableForegroundDispatch(NfcActivity.this);
        mWriteMode = true;
        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        mWriteTagFilters = new IntentFilter[]{tagDetected};
        mNfcAdapter.enableForegroundDispatch(NfcActivity.this,nfcPendingIntent,mWriteTagFilters,null);
    }
    // disable tag writing
    private void disableTagWrite(){
        mWriteMode = false;
        mNfcAdapter.disableForegroundDispatch(NfcActivity.this);
    }

    // write to tag
    private boolean writeTag(NdefMessage ndefMsg,Tag tag){
        int size = ndefMsg.getByteArrayLength();
        try{
            Ndef ndef = Ndef.get(tag);
            if (ndef != null){
                ndef.connect();
                if (!ndef.isWritable()){
                    Toast.makeText(this,"tag is read-only!",Toast.LENGTH_SHORT).show();
                    return false;
                }
                if (ndef.getMaxSize() < size){
                    Toast.makeText(this,"Tag capacity is " + ndef.getMaxSize() + " bytes, message is " + size + " bytes.",Toast.LENGTH_SHORT).show();
                    return false;
                }
                ndef.writeNdefMessage(ndefMsg);
                Toast.makeText(this,"Wrote message to pre-formatted tag.",Toast.LENGTH_SHORT).show();
                return true;
            }
            else{
                NdefFormatable format = NdefFormatable.get(tag);
                if (format != null){
                    try{
                        format.connect();
                        format.format(ndefMsg);
                        Toast.makeText(this,"Formatted tag and wrote message.",Toast.LENGTH_SHORT).show();
                        return true;
                    }
                    catch (IOException e){
                        Toast.makeText(this,"Failed to format tag.",Toast.LENGTH_SHORT).show();
                        Log.d("NFC",e.getMessage());
                    }
                }
                else {
                    Toast.makeText(this,"Tag doesn't suppoprt NDEF!",Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
        }
        catch (Exception e){
            Toast.makeText(this,"Failed to write tag!",Toast.LENGTH_SHORT).show();
            Log.d("NFC",e.getMessage());
        }
        return false;
    }

}
