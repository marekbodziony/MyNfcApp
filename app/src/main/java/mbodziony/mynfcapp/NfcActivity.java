package mbodziony.mynfcapp;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.Toast;

public class NfcActivity extends AppCompatActivity {

    private EditText text;
    private NfcAdapter mNfcAdapter;
    private PendingIntent nfcPendingIntent;
    private IntentFilter[] mNdefExchangeFilters;

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

    }


    @Override
    protected void onNewIntent (Intent intent){
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())){
            NdefMessage[] msgs = getNdefMessages(intent);
            byte [] payload = msgs[0].getRecords()[0].getPayload();
            text.setText(new String(payload));
        }
    }


    @Override
    public void onResume(){
        super.onResume();
        // if NDEF message received from other device
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())){
            NdefMessage[] ndefMessages = getNdefMessages(getIntent());
            byte [] payload = ndefMessages[0].getRecords()[0].getPayload();
            text.setText(new String(payload));
            setIntent(new Intent()); // Consume this intent.
        }
        mNfcAdapter.enableForegroundDispatch(this,nfcPendingIntent,mNdefExchangeFilters,null);
        mNfcAdapter.setNdefPushMessage(convertTextToNdefMessage(),this);
    }

    @Override
    public void onPause(){
        super.onPause();
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
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())){
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            if (rawMsgs != null){
                msgs = new NdefMessage[rawMsgs.length];
                for (int i = 0; i < rawMsgs.length; i ++){
                    msgs[i] = (NdefMessage)rawMsgs[i];
                }
            }
        }
        else{
            Toast.makeText(this,"No NDEF message!",Toast.LENGTH_LONG).show();
        }
        return msgs;
    }


}
