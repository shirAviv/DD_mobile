package com.example.app1;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;
import android.Manifest;

public class ContactsActivity extends AppCompatActivity {

    private static final int PERMISSIONS_REQUEST_READ_CONTACTS = 100;
    private AutoCompleteTextView contactsAutoComplete;
    private Button sendButton, callButton;
    private String selectedPhoneNumber; // Store selected contact number

    private List<Contact> contactsList = new ArrayList<>();
    // Simple Contact class holding name and phone number.
    private static class Contact {
        String name;
        String phoneNumber;

        Contact(String name, String phoneNumber) {
            this.name = name;
            this.phoneNumber = phoneNumber;
        }

        // When used in the adapter, this is what will be displayed.
        @NonNull
        @Override
        public String toString() {
            return name;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_contatcts);
        contactsAutoComplete = findViewById(R.id.autoCompleteContacts);
        sendButton = findViewById(R.id.sendButton);
        callButton = findViewById(R.id.callButton);

        sendButton.setVisibility(View.GONE);
        callButton.setVisibility(View.GONE);

        // Check if the READ_CONTACTS permission is granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted; request it
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_CONTACTS},
                    PERMISSIONS_REQUEST_READ_CONTACTS);
        } else {
            // Permission is granted; load contacts
            loadContacts();
        }
        // Button Click Listeners
        sendButton.setOnClickListener(v -> sendSms(selectedPhoneNumber));
        callButton.setOnClickListener(v -> callContact(selectedPhoneNumber));
    }

    private void loadContacts() {
        contactsList.clear();
        Cursor cursor = null;

        try {
            // Query the contacts database for display names
            cursor = getContentResolver().query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    new String[]{
                            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                            ContactsContract.CommonDataKinds.Phone.NUMBER
                    },
                    null,
                    null,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            );

            if (cursor != null) {
                while (cursor.moveToNext()) {
                    // Get the display name
                    String name = cursor.getString(
                            cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                    String phoneNumber = cursor.getString(
                            cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER));

                    contactsList.add(new Contact(name, phoneNumber));
                }
            }

            // Create an ArrayAdapter using a simple drop-down layout and the list of names
            ArrayAdapter<Contact> adapter = new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_dropdown_item_1line,
                    contactsList
            );

            // Set the adapter for the AutoCompleteTextView
            contactsAutoComplete.setAdapter(adapter);
            contactsAutoComplete.setThreshold(1); // Show suggestions after 1 character

            // When a user selects a contact, launch the dialer with the phone number.
            contactsAutoComplete.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, android.view.View view, int position, long id) {
                    Contact selectedContact = (Contact) parent.getItemAtPosition(position);
                    selectedPhoneNumber = selectedContact.phoneNumber;
                    // Show buttons when a contact is selected
                    sendButton.setVisibility(View.VISIBLE);
                    callButton.setVisibility(View.VISIBLE);

//                    call(phone);
//                    sendSms(phone);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error loading contacts", Toast.LENGTH_SHORT).show();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    // opens the call app with the selected phone number
    private void callContact(String phoneNumber) {
        Intent dialIntent = new Intent(Intent.ACTION_DIAL);
        dialIntent.setData(Uri.parse("tel:" + phoneNumber));
        try {
            startActivity(dialIntent);
        } catch (Exception e) {
            Toast.makeText(this, "Call failed", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    // Opens the SMS app with the selected phone number.
    private void sendSms(String phoneNumber) {
        Intent smsIntent = new Intent(Intent.ACTION_VIEW);
        smsIntent.setData(Uri.parse("smsto:" + phoneNumber)); // "smsto:" opens SMS app
        smsIntent.putExtra("sms_body", "I need help"); // Prefilled text

        try {
            startActivity(smsIntent);
        } catch (Exception e) {
            Toast.makeText(this, "SMS sending failed", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }


    // Handle the result of the runtime permission request
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_READ_CONTACTS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted; load contacts
                loadContacts();
            } else {
                // Permission denied; notify the user
                Toast.makeText(this, "Permission required to load contacts", Toast.LENGTH_SHORT).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

}