package com.example.app1;

import static android.content.ContentValues.TAG;
import static java.lang.Math.abs;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TableLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;
import com.ftdi.j2xx.D2xxManager;
import com.ftdi.j2xx.FT_Device;
import android.hardware.usb.UsbManager;
import android.widget.Toast;

//import com.palmsens.methodscriptexample.R;



public class MainActivity extends AppCompatActivity {
    String drug="acamol1";
    String mode="static";

    private static List<Double> mCurrentReadings = new ArrayList<>();                               //Collection of current readings
    private static List<Double> mVoltageReadings = new ArrayList<>();                               //Collection of potential readings
    private static D2xxManager ftD2xxManager = null;
    private final Handler mHandler = new Handler();
    private FT_Device ftDevice;

    private static final String SCRIPT_FILE_NAME ="";
    private boolean mThreadIsStopped = true;
    private static final int BAUD_RATE = 230400;                                                    //Baud Rate for EmStat Pico
    private static final int LATENCY_TIMER = 16;                                                    //Read time out for the device in milli seconds.
    private static final int PACKAGE_DATA_VALUE_LENGTH = 8;                                         //Length of the data value in a package
    private static final int OFFSET_VALUE = 0x8000000;                                              //Offset value to receive positive values
    private int mNDataPointsReceived = 0;

    /**
     * The SI unit of the prefixes and their corresponding factors
     */
    private static final Map<Character, Double> SI_PREFIX_FACTORS = new HashMap<Character, Double>() {{  //The SI unit of the prefixes and their corresponding factors
        put('a', 1e-18);
        put('f', 1e-15);
        put('p', 1e-12);
        put('n', 1e-9);
        put('u', 1e-6);
        put('m', 1e-3);
        put('i', 1.0);
        put(' ', 1.0);
        put('k', 1e3);
        put('M', 1e6);
        put('G', 1e9);
        put('T', 1e12);
        put('P', 1e15);
        put('E', 1e18);
    }};


    /**
     * The beginning characters of the measurement response packages to help parsing the response
     */
    private enum Reply {
        REPLY_BEGIN_VERSION('t'),
        REPLY_BEGIN_RESPONSE('e'),
        REPLY_MEASURING('M'),
        REPLY_BEGIN_PACKETS('P'),
        REPLY_END_MEAS_LOOP('*'),
        REPLY_EMPTY_LINE('\n'),
        REPLY_ABORTED('Z');

        private final char mReply;

        Reply(char reply) {
            this.mReply = reply;
        }

        public static Reply getReply(char reply) {
            for (Reply replyChar : values()) {
                if (replyChar.mReply == reply) {
                    return replyChar;
                }
            }
            return null;
        }
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        setTitle("Check My Drink");
    }

    public void onClickTestWhiteWine(View view) {
        runTest("white_wine");

    }

    public void onClickTestVodka(View view) {
        runTest("vodka");

    }

    public void onClickTestRum(View view) {
        runTest("rum");

    }

    public void onClickTestJin(View view) {
        runTest("jin");

    }

    public void onClickTestWhiskey(View view) {
        runTest("whiskey");

    }

    public void onClickTestRedWine(View view) {
        runTest("red_wine");
    }

    public void launchContacts(View view) {
        Log.d("warning","contacting");
//        view.setVisibility(View.GONE);
        Intent intent = new Intent(this, ContactsActivity.class);
        startActivity(intent);
    }

    public void runTest(String drink ) {
        boolean result=false;

        if (Objects.equals(mode, "static")) {
            Log.d("success","working from preexisting experiment files");
            result = compare_currents(mode, drink);

        } else {
//            double[] currents = {0.000001, 0.001, 0.5, 0.05};
            try {
                Log.d("Debug","dynamic mode, connect and run test");
                //    currents = runTest.run()
                result = compare_currents(mode, drink);
            } catch (Exception e) {
                Log.d("Failure","can not connect in order to perform measure");
            }
        }


        findViewById(R.id.red_wine).setVisibility(View.GONE);
        findViewById(R.id.white_wine).setVisibility(View.GONE);
        findViewById(R.id.whiskey).setVisibility(View.GONE);
        findViewById(R.id.vodka).setVisibility(View.GONE);
        findViewById(R.id.jin).setVisibility(View.GONE);
        findViewById(R.id.rum).setVisibility(View.GONE);
        TableLayout tbl =findViewById(R.id.tableLayout);
        // Create layout parameters (here using LinearLayout parameters)
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );

        // Optionally, set margins or gravity on the parameters
        params.setMargins(16, 16, 16, 16);

        if (result) {
            Log.d("Warning","No current differences- all good");
            ImageButton imageButtonGood = new ImageButton(this);
            // Set the image resource (replace R.drawable.my_image with your actual image)
            imageButtonGood.setImageResource(R.drawable.green_circle);
            // Optionally, set a content description for accessibility
            imageButtonGood.setContentDescription("All Good");
            // Apply the layout parameters to the ImageButton
            imageButtonGood.setLayoutParams(params);
            // Add the ImageButton to the layout
            tbl.addView(imageButtonGood);

        }
        else {
            Log.d("Warning","differences in current, Bad");
            ImageButton imageButtonBad = new ImageButton(this);
            // Set the image resource (replace R.drawable.my_image with your actual image)
            imageButtonBad.setImageResource(R.drawable.red_circle);
            // Optionally, set a content description for accessibility
            imageButtonBad.setContentDescription("Bad");
            // Apply the layout parameters to the ImageButton
            imageButtonBad.setLayoutParams(params);
            // Add the ImageButton to the layout
            tbl.addView(imageButtonBad);

            ImageButton imageButtonContact = new ImageButton(this);
            // Set the image resource (replace R.drawable.my_image with your actual image)
            imageButtonContact.setImageResource(R.drawable.phone);
            // Optionally, set a content description for accessibility
            imageButtonContact.setContentDescription("Call my contacts");

            (imageButtonContact).setOnClickListener(new View.OnClickListener()
            {
                public void onClick(View v) {
                    launchContacts(v);
                }
            });
            // Apply the layout parameters to the ImageButton
            imageButtonContact.setLayoutParams(params);
            // Add the ImageButton to the layout
            tbl.addView(imageButtonContact);


        }
    }

    boolean compare_currents( String mode, String type) {
        if (Objects.equals(mode, "static")) {
            String file_name = type + "_drop.csv";
            Log.d("debug",file_name);
            List<String[]> clear_list;
            List<String[]> suspect_list = Collections.emptyList();
            try (CSVReader reader = new CSVReader(
                    new InputStreamReader(getAssets().open(file_name), StandardCharsets.UTF_16LE ))) {

                clear_list = reader.readAll();
                file_name=type+"_drop_"+drug+".csv";

                try (CSVReader reader1 = new CSVReader(new InputStreamReader(getAssets().open(file_name), StandardCharsets.UTF_16LE))) {
                    suspect_list = reader1.readAll();
                } catch (IOException | CsvException ex)
                {
                    Log.d("Failue",ex.getMessage());
                    return false;
                }
                int count_above_threshold=0;

                int minSize = Math.min(clear_list.size(), suspect_list.size())-1;
                for (int i = 1; i < minSize; i++) {
                    double v_clear = Double.parseDouble(clear_list.get(i)[0]);
                    double i_clear = Double.parseDouble(clear_list.get(i)[1]);

                    double v_suspect = Double.parseDouble(suspect_list.get(i)[0]);
                    double i_suspect = Double.parseDouble(suspect_list.get(i)[1]);

                    if (v_clear==v_suspect) {
                        double diff=abs(i_clear-i_suspect);
                        if (diff>2.5) {
                            count_above_threshold++;
                        }
                    }

                }
                double ratio=(double)count_above_threshold/(double)minSize;
                Log.d("Debug", String.valueOf(ratio));
                boolean result=ratio < 0.5;
                return result;

            } catch (IOException | CsvException e) {
                Log.d("Failue",e.getMessage());
            }
        } else {
            return(handle_dynamic(type));
        }
        return false;
    }

    /*
    In the Dynamic mode, connect to the emPico device, send the script and wait for results. These are stored
    in mVoltageReadings, mCurrentReadings. Then check, if first file (clear) does not exist, this is first measurement-
    store the results there and finish, no comparison is needed. If first file (clear) exists, assume it is the suspicious
    check- store the results in the second file (suspicious) and call compare_currents with mode 'static',
    so it can compare the results.
     */
    boolean handle_dynamic(String type) {
        String file_name = type + "_drop.csv";
        Log.d("debug",file_name);

        try {
            ftD2xxManager = D2xxManager.getInstance(this);
            if (openDevice()) {
                if (sendScriptFile(type)) {
                    // save voltage and currents to file.
                    // This is the first check - clear
                    if (! isFileExists(file_name)) {
                        writeReadingsToCSV(file_name);
                        return true;
                    }
                    // This is the second check - suspicious
                    else {
                        file_name=type+"_drop_"+drug+".csv";
                        writeReadingsToCSV(file_name);
                        compare_currents("static",type);
                    }
                }
            } else {
                return false;
            }
        } catch (D2xxManager.D2xxException ex) {
            Log.e(TAG, ex.toString());
            AlertDialog.Builder builder = new AlertDialog.Builder(this);                    // Exit the application if D2xxManager instance is null.
            builder.setMessage("Failed to retrieve an instance of D2xxManager. The application is forced to exit.")
                    .setCancelable(false)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            MainActivity.this.finish();
                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();
            return false;
        }
        return true;
    }

    /*
    check that file exists - the clear file.
     */
    public boolean isFileExists(String fileName) {
        AssetManager assetManager = getAssets(); // Get AssetManager instance
        try {
            assetManager.open(fileName); // Try opening the file
            return true; // File exists
        } catch (IOException e) {
            return false; // File does not exist
        }
    }

    /*
    Write the results of the measurement to the file- voltage and currents
     */
    public void writeReadingsToCSV(String fileName) {
        if (mCurrentReadings.size() != mVoltageReadings.size()) {
            throw new IllegalArgumentException("Lists must be of the same size");
        }

        File csvFile = new File(getFilesDir(), fileName); // Save in internal storage

        try (CSVWriter writer = new CSVWriter(new FileWriter(csvFile))) {
            // Write header row
            writer.writeNext(new String[]{"Voltage", "Current"});

            // Write data rows
            for (int i = 0; i < mVoltageReadings.size(); i++) {
                String[] row = {String.valueOf(mVoltageReadings.get(i)),
                        String.valueOf(mCurrentReadings.get(i))};
                writer.writeNext(row);
            }

            System.out.println("CSV file written to: " + csvFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    private boolean discoverDevice() {
        boolean isDeviceFound = false;
        int devCount;
        devCount = ftD2xxManager.createDeviceInfoList(this);

        D2xxManager.FtDeviceInfoListNode[] deviceList = new D2xxManager.FtDeviceInfoListNode[devCount];
        ftD2xxManager.getDeviceInfoList(devCount, deviceList);

        if (devCount > 0) {
            isDeviceFound = true;
        } else {
            Toast.makeText(this, "Discovered no device", Toast.LENGTH_SHORT).show();
        }
        return isDeviceFound;
    }

    private boolean openDevice() {
        if (ftDevice != null) {
            ftDevice.close();
        }

        ftDevice = ftD2xxManager.openByIndex(this, 0);

        boolean isConfigured = false;
        if (ftDevice.isOpen()) {
            if (mThreadIsStopped) {
                SetConfig();                                                                        //Configures the port with necessary parameters
                ftDevice.purge((byte) (D2xxManager.FT_PURGE_TX | D2xxManager.FT_PURGE_RX));         //Purges data from the device's TX/RX buffer
                ftDevice.restartInTask();                                                           //Resumes the driver issuing USB in requests
                new Thread(mLoop).start();                                                          //Start parsing thread
                Log.d(TAG, "Device configured. ");
                isConfigured = true;
            }
        }
        return isConfigured;
    }

    private Runnable mLoop = new Runnable() {
        StringBuilder readLine = new StringBuilder();

        @Override
        public void run() {
            int readSize;
            byte[] rbuf = new byte[1];
            mThreadIsStopped = false;
            try {
                while (!mThreadIsStopped && ftDevice != null) {
                    synchronized (ftDevice) {
                        readSize = ftDevice.getQueueStatus();                    //Retrieves the size of the available data on the device
                        if (readSize > 0) {
                            ftDevice.read(rbuf, 1);                      //Reads one character from the device
                            String rchar = new String(rbuf);
                            readLine.append(rchar);                             //Forms a line of response by appending the character read

                            if (rchar.equals("\n")) {                           //When a new line '\n' is read, the line is sent for processing
                                mHandler.post(new Runnable() {
                                    final String line = readLine.toString();

                                    @Override
                                    public void run() {
                                        processResponse(line);                  //Calls the method to process the measurement package
                                    }
                                });
                                readLine = new StringBuilder();                 //Resets the readLine to store the next measurement package
                            }
                        } // end of if(readSize>0)
                    }// end of synchronized
                }
            } catch (Exception ex) {
                Log.d(TAG, ex.getMessage());
            }
        }
    };

    /**
     * <Summary>
     *      Sets the device configuration properties like Baudrate (230400), databits (8), stop bits (1), parity (None) and bit mode.
     * </Summary>
     */
    private void SetConfig() {
        byte dataBits = D2xxManager.FT_DATA_BITS_8;
        byte stopBits = D2xxManager.FT_STOP_BITS_1;
        byte parity = D2xxManager.FT_PARITY_NONE;

        if (!ftDevice.isOpen()) {
            Log.e(TAG, "SetConfig: device not open");
            return;
        }
        // configures the port, reset to UART mode for 232 devices
        ftDevice.setBitMode((byte) 0, D2xxManager.FT_BITMODE_RESET);
        ftDevice.setBaudRate(BAUD_RATE);
        ftDevice.setLatencyTimer((byte) LATENCY_TIMER);
        ftDevice.setDataCharacteristics(dataBits, stopBits, parity);
    }

    private boolean sendScriptFile(String script_name) {
        boolean isScriptSent = false;
        String line;
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new InputStreamReader(getAssets().open(script_name)));
            while ((line = bufferedReader.readLine()) != null) {
                line += "\n";
                writeToDevice(line);
            }
            isScriptSent = true;
        } catch (FileNotFoundException ex) {
            Log.d(TAG, ex.getMessage());
        } catch (IOException ex) {
            Log.d(TAG, ex.getMessage());
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException ex) {
                    Log.d(TAG, ex.getMessage());
                }
            }
        }
        return isScriptSent;
    }

    private boolean writeToDevice(String script) {
        int bytesWritten;
        boolean isWriteSuccess = false;
        if (ftDevice != null) {
            synchronized (ftDevice) {
                if (!ftDevice.isOpen()) {
                    Log.e(TAG, "onClickWrite : Device is not open");
                    return false;
                }

                byte[] writeByte = script.getBytes();
                bytesWritten = ftDevice.write(writeByte, script.length());                          //Writes to the device
                if (bytesWritten == writeByte.length) {                                             //Verifies if the bytes written equals the total number of bytes sent
                    isWriteSuccess = true;
                }
            }
        }
        return isWriteSuccess;
    }

    /**
     * <Summary>
     *      Processes the measurement packages from the device.
     * </Summary>
     * @param line A complete line of response read from the device.
     */
    private void processResponse(String line) {
                processReceivedPackage(line);   //Calls the method to process the received measurement package
    }

    /**
     * <Summary>
     *      Processes the measurement package from the EmStat Pico and stores the response in RawData.
     * </Summary>
     * @param readLine A measurement package read from the device.
     * @return A boolean to indicate if measurement is complete.
     */
    private void processReceivedPackage(String readLine) {
        Reply beginChar = Reply.getReply(readLine.charAt(0));
        switch (beginChar) {
            case REPLY_EMPTY_LINE:
                break;
            case REPLY_END_MEAS_LOOP:
                break;
            case REPLY_MEASURING:
                break;
            case REPLY_BEGIN_PACKETS:
                mNDataPointsReceived++;                                  //Increments the number of data points if the read line contains the header char 'P
                parsePackageLine(readLine);                              //Parses the line read
                break;
            case REPLY_ABORTED:
            default:
                break;
        }
    }

    /**
     * <summary>
     *      Parses a measurement data package and adds the parsed data values to their corresponding arrays
     * </summary>
     * @param packageLine The measurement data package to be parsed
     */
    private void parsePackageLine(String packageLine) {
        String[] variables;
        String variableIdentifier;
        String dataValue;

        int startingIndex = packageLine.indexOf('P');                            //Identifies the beginning of the measurement data package
        String responsePackageLine = packageLine.substring(startingIndex + 1);   //Removes the beginning character 'P'

        startingIndex = 0;
        Log.d(TAG, String.valueOf(mNDataPointsReceived) + "  " + responsePackageLine);

        variables = responsePackageLine.split(";");                       //The data values are separated by the delimiter ';'
        for (String variable : variables) {
            variableIdentifier = variable.substring(startingIndex, 2);           //The String (2 characters) that identifies the measurement variable
            dataValue = variable.substring(startingIndex + 2, startingIndex + 2 + PACKAGE_DATA_VALUE_LENGTH);
            double dataValueWithPrefix = parseParamValues(dataValue);           //Parses the variable values and returns the actual values with their corresponding SI unit prefixes
            switch (variableIdentifier) {
                case "da":                                                       //Potential reading
                    mVoltageReadings.add(dataValueWithPrefix);                  //Adds the value to the mVoltageReadings array
                    break;
                case "ba":                                                       //Current reading
                    mCurrentReadings.add(dataValueWithPrefix);                  //Adds the value to the mCurrentReadings array
                    break;
            }

        }
    }

    /**
     * <Summary>
     *      Parses the data value package and appends the respective prefixes
     * </Summary>
     * @param paramValueString The data value package to be parsed
     * @return The actual data value (double) after appending the unit prefix
     */
    private double parseParamValues(String paramValueString) {
        if (paramValueString == "     nan")
            return Double.NaN;
        char strUnitPrefix = paramValueString.charAt(7);                         //Identifies the SI unit prefix from the package at position 8
        String strvalue = paramValueString.substring(0, 7);                      //Retrieves the value of the variable from the package
        int value = Integer.parseInt(strvalue, 16);                        //Converts the hex value to int
        double paramValue = value - OFFSET_VALUE;                                //Values offset to receive only positive values
        return (paramValue * SI_PREFIX_FACTORS.get(strUnitPrefix));              //Returns the actual data value after appending the SI unit prefix
    }




}