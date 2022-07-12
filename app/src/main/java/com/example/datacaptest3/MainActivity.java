package com.example.datacaptest3;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.datacap.android.dsiEMVAndroid;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.math.BigDecimal;
import java.net.ResponseCache;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Objects;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class MainActivity extends AppCompatActivity {
    private EditText amountInput;
    private EditText tipInput;

    private Button saleButton;
    private Button returnButton;
    private Button voidButton;

    private Button dlParamButton;
    private Button summaryButton;
    private Button closeButton;

    private Button tableAddButton;

    private TextView textViewResponse;

    private TableLayout transactionTable;

    //request config parameters
    private String merchantID = "BANKCTORR2GP";
    private String secureDevice = "EMV_LANE3000_DATACAP_E2E";
    private String pinpadIPAddress = "192.168.0.101";
    private String pinpadPort ="12000";
    private Integer purchaseAmount = 0;
    private Integer tipAmount = 0;
    private String invoiceNo = "0003";

    //batch parameters
    private String batchNo = "";
    private String batchItemCount = "";
    private String netBatchTotal = "";

    //Store transaction data
    private Map transactionData;
    private int orderNumber = 1;

    //External logging parameters
    private String filename = "";
    private String filepath = "";
    private String fileContent = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //set paremeters for external logging
        String dateFormat = new SimpleDateFormat("yyyy-MM-dd").format(Calendar.getInstance().getTime());
        filename = dateFormat + "_log.txt";

        //create instance of dsiEMVAndroid to be used for future requests
        dsiEMVAndroid mDSIEMVAndroid = new dsiEMVAndroid(MainActivity.this);

        transactionTable = (TableLayout) findViewById(R.id.transaction_table);

        //Test button to add dummy entries to the transaction table
        tableAddButton = (Button) findViewById(R.id.table_add_button);
        tableAddButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            }
        });

        amountInput = (EditText) findViewById(R.id.amount_edit_text);
        amountInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                String value= amountInput.getText().toString();
                value = value.replace(".", "");
                try{
                    if(value.equals("")){
                        purchaseAmount = 0;
                    } else {
                        purchaseAmount = Integer.parseInt(value);
                    }
                } catch (NumberFormatException e){
                    purchaseAmount = 0;
                }


            }
        });

        tipInput = (EditText) findViewById(R.id.tip_amount_edit_text);
        tipInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                String value= tipInput.getText().toString();
                value = value.replace(".", "");
                try{
                    tipAmount = Integer.parseInt(value);
                } catch (NumberFormatException e){

                }
                if(value.equals("")){
                    tipAmount = 0;
                }
            }
        });

        textViewResponse = (TextView) findViewById(R.id.textview_response);

        dlParamButton = (Button) findViewById(R.id.param_button);
        dlParamButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                downloadParam(mDSIEMVAndroid);
            }
        });

        summaryButton = (Button) findViewById(R.id.summary_button);
        summaryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                batchSummary(mDSIEMVAndroid);
            }
        });

        closeButton = (Button) findViewById(R.id.batch_close_button);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                batchClose(mDSIEMVAndroid);
            }
        });

        saleButton = (Button) findViewById(R.id.sale_button);
        saleButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view){
                padReset(mDSIEMVAndroid);
                saleRequest(mDSIEMVAndroid);
                padReset(mDSIEMVAndroid);
            }
        });

        voidButton = (Button) findViewById(R.id.void_button);
        voidButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                padReset(mDSIEMVAndroid);
                voidSale(mDSIEMVAndroid);
                padReset(mDSIEMVAndroid);
            }
        });

        returnButton = (Button) findViewById(R.id.return_button);
        returnButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                padReset(mDSIEMVAndroid);
                returnSale(mDSIEMVAndroid);
                padReset(mDSIEMVAndroid);
            }
        });

    }

    //EMVParamDownload
    private void downloadParam(dsiEMVAndroid mDSIEMVAndroid) {
        String request = "<?xml version='1.0'?>" +
                "<TStream>" +
                "<Admin>" +
                "<OperationMode>CERT</OperationMode>" +
                "<MerchantID>BANKCTORR2GP</MerchantID>" +
                "<TerminalID>001</TerminalID>" +
                "<POSPackageID>EMVUSClient:1.26</POSPackageID>" +
                "<OperatorID>Test</OperatorID>" +
                "<UserTrace>Dev1</UserTrace>" +
                "<TranCode>EMVParamDownload</TranCode>" +
                "<SecureDevice>EMV_LANE3000_DATACAP_E2E</SecureDevice>" +
                "<PinPadIpAddress>192.168.0.101</PinPadIpAddress>" +
                "<PinPadIpPort>12000</PinPadIpPort>" +
                "<SequenceNo>0010010010</SequenceNo>" +
                "</Admin>" +
                "</TStream>";

        String response = mDSIEMVAndroid.ProcessTransaction(request);

        textViewResponse.setText(response);
    }

    //Pad rest to run before and after every transaction
    private void padReset(dsiEMVAndroid mDSIEMVAndroid){
        String request = "<?xml version='1.0'?>" +
                "<TStream>" +
                "<Transaction>" +
                "<OperationMode>CERT</OperationMode>" +
                "<MerchantID>" + merchantID +"</MerchantID>" +
                "<TerminalID>001</TerminalID>" +
                "<POSPackageID>EMVUSClient:1.26</POSPackageID>" +
                "<OperatorID>Test</OperatorID>" +
                "<UserTrace>Dev1</UserTrace>" +
                "<TranCode>EMVPadReset</TranCode>" +
                "<SecureDevice>"+ secureDevice +"</SecureDevice>" +
                "<PinPadIpAddress>"+ pinpadIPAddress +"</PinPadIpAddress>" +
                "<PinPadIpPort>"+ pinpadPort +"</PinPadIpPort>" +
                "<SequenceNo>0010010010</SequenceNo>" +
                "</Transaction>" +
                "</TStream>";

        String response = mDSIEMVAndroid.ProcessTransaction(request);
    }

    //EMVSale
    private void saleRequest(dsiEMVAndroid mDSIEMVAndroid) {
        if(purchaseAmount > 0){
            String request = "<?xml version='1.0'?>" +
                    "<TStream>" +
                    "<Transaction>" +
                    "<OperationMode>CERT</OperationMode>" +
                    "<MerchantID>" + merchantID +"</MerchantID>" +
                    "<TerminalID>001</TerminalID>" +
                    "<POSPackageID>EMVUSClient:1.26</POSPackageID>" +
                    "<OperatorID>Test</OperatorID>" +
                    "<UserTrace>Dev1</UserTrace>" +
                    "<CardType>Credit</CardType>" +
                    "<TranCode>EMVSale</TranCode>" +
                    "<CollectData>CardholderName</CollectData>" +
                    "<SecureDevice>"+ secureDevice +"</SecureDevice>" +
                    "<PinPadIpAddress>"+ pinpadIPAddress +"</PinPadIpAddress>" +
                    "<PinPadIpPort>"+ pinpadPort +"</PinPadIpPort>" +
                    "<InvoiceNo>"+ invoiceNo +"</InvoiceNo>" +
                    "<RefNo>"+ invoiceNo +"</RefNo>" +
                    "<Amount>" +
                    "<Purchase>"+ purchaseAmount +"</Purchase>" +
                    "<Gratuity>"+ tipAmount +"</Gratuity>" +
                    "</Amount>" +
                    "<SequenceNo>0010010010</SequenceNo>" +
                    "<PartialAuth>Allow</PartialAuth>" +
                    "<RecordNo>RecordNumberRequested</RecordNo>" +
                    "<Frequency>Recurring</Frequency>" +
                    "</Transaction>" +
                    "</TStream>";

            String response = mDSIEMVAndroid.ProcessTransaction(request);


            if (Objects.equals(batchNo, "")){
                batchSummary(mDSIEMVAndroid);
            }

            textViewResponse.setText(response);
            addResponseToTable(response);

            writeToFile(response);
        } else {
            textViewResponse.setText("please input valid sale amount.");
        }
    }

    //SaleByRecordNo. Not used currently.
    private void saleByRecord(dsiEMVAndroid mDSIEMVAndroid){
        String request = "<?xml version='1.0'?>" +
                "<TStream>" +
                "<Transaction>" +
                "<OperationMode>CERT</OperationMode>" +
                "<MerchantID>" + merchantID +"</MerchantID>" +
                "<TerminalID>001</TerminalID>" +
                "<POSPackageID>EMVUSClient:1.26</POSPackageID>" +
                "<OperatorID>Test</OperatorID>" +
                "<UserTrace>Dev1</UserTrace>" +
                "<CardType>Credit</CardType>" +
                "<TranCode>SaleByRecordNo</TranCode>" +
                "<CollectData>CardholderName</CollectData>" +
                "<SecureDevice>"+ secureDevice +"</SecureDevice>" +
                "<PinPadIpAddress>"+ pinpadIPAddress +"</PinPadIpAddress>" +
                "<PinPadIpPort>"+ pinpadPort +"</PinPadIpPort>" +
                "<InvoiceNo>"+ invoiceNo +"</InvoiceNo>" +
                "<RefNo>"+ invoiceNo +"</RefNo>" +
                "<Amount>" +
                "<Purchase>"+ purchaseAmount +"</Purchase>" +
                "<Gratuity>"+ tipAmount +"</Gratuity>" +
                "</Amount>" +
                "<SequenceNo>0010010010</SequenceNo>" +
                "<PartialAuth>Allow</PartialAuth>" +
                "<RecordNo>RecordNumberRequested</RecordNo>" +
                "<Frequency>Recurring</Frequency>" +
                "</Transaction>" +
                "</TStream>";

        String response = mDSIEMVAndroid.ProcessTransaction(request);

        textViewResponse.setText(response);
    }

    private boolean isExternalStorageAvaiableForRW() {
        String extStorageState = Environment.getExternalStorageState();
        if (extStorageState.equals(Environment.MEDIA_MOUNTED)){
            return true;
        }
        return false;
    }

    //EMVVoidSale
    private void voidSale(dsiEMVAndroid mDSIEMVAndroid){
        String request = "<?xml version='1.0'?>" +
                "<TStream>" +
                "<Transaction>" +
                "<OperationMode>CERT</OperationMode>" +
                "<MerchantID>" + merchantID +"</MerchantID>" +
                "<TerminalID>001</TerminalID>" +
                "<POSPackageID>EMVUSClient:1.26</POSPackageID>" +
                "<OperatorID>Test</OperatorID>" +
                "<UserTrace>Dev1</UserTrace>" +
                "<CardType>Credit</CardType>" +
                "<TranCode>EMVVoidSale</TranCode>" +
                "<CollectData>CardholderName</CollectData>" +
                "<SecureDevice>"+ secureDevice +"</SecureDevice>" +
                "<PinPadIpAddress>"+ pinpadIPAddress +"</PinPadIpAddress>" +
                "<PinPadIpPort>"+ pinpadPort +"</PinPadIpPort>" +
                "<InvoiceNo>"+ invoiceNo +"</InvoiceNo>" +
                "<RefNo>"+ invoiceNo +"</RefNo>" +
                "<AuthCode>036556</AuthCode>" +
                "<Amount>" +
                "<Purchase>"+ purchaseAmount +"</Purchase>" +
                "<Gratuity>"+ tipAmount +"</Gratuity>" +
                "</Amount>" +
                "<SequenceNo>0010010010</SequenceNo>" +
                "<PartialAuth>Allow</PartialAuth>" +
                "<RecordNo>RecordNumberRequested</RecordNo>" +
                "<Frequency>Recurring</Frequency>" +
                "</Transaction>" +
                "</TStream>";

        String response = mDSIEMVAndroid.ProcessTransaction(request);

        addResponseToTable(response);
        writeToFile(response);

        textViewResponse.setText(response);
    }

    //EMVReturn
    private void returnSale(dsiEMVAndroid mDSIEMVAndroid){
        String request = "<?xml version='1.0'?>" +
                "<TStream>" +
                "<Transaction>" +
                "<OperationMode>CERT</OperationMode>" +
                "<MerchantID>" + merchantID +"</MerchantID>" +
                "<TerminalID>001</TerminalID>" +
                "<POSPackageID>EMVUSClient:1.26</POSPackageID>" +
                "<OperatorID>Test</OperatorID>" +
                "<UserTrace>Dev1</UserTrace>" +
                "<CardType>Credit</CardType>" +
                "<TranCode>EMVReturn</TranCode>" +
                "<CollectData>CardholderName</CollectData>" +
                "<SecureDevice>"+ secureDevice +"</SecureDevice>" +
                "<PinPadIpAddress>"+ pinpadIPAddress +"</PinPadIpAddress>" +
                "<PinPadIpPort>"+ pinpadPort +"</PinPadIpPort>" +
                "<InvoiceNo>"+ invoiceNo +"</InvoiceNo>" +
                "<RefNo>"+ invoiceNo +"</RefNo>" +
                "<AuthCode>036556</AuthCode>" +
                "<Amount>" +
                "<Purchase>"+ purchaseAmount +"</Purchase>" +
                "</Amount>" +
                "<SequenceNo>0010010010</SequenceNo>" +
                "<PartialAuth>Allow</PartialAuth>" +
                "<RecordNo>RecordNumberRequested</RecordNo>" +
                "<Frequency>Recurring</Frequency>" +
                "</Transaction>" +
                "</TStream>";

        String response = mDSIEMVAndroid.ProcessTransaction(request);

        addResponseToTable(response);
        writeToFile(response);

        textViewResponse.setText(response);
    }

    //BatchSummary
    private void batchSummary(dsiEMVAndroid mDSIEMVAndroid){
        String request="<?xml version='1.0'?>" +
                "<TStream>" +
                "<Admin>" +
                "<OperationMode>CERT</OperationMode>" +
                "<MerchantID>" + merchantID +"</MerchantID>" +
                "<TerminalID>001</TerminalID>" +
                "<POSPackageID>EMVUSClient:1.26</POSPackageID>" +
                "<OperatorID>Test</OperatorID>" +
                "<UserTrace>Dev1</UserTrace>" +
                "<TranType>Administrative</TranType>" +
                "<TranCode>BatchSummary</TranCode>" +
                "<SecureDevice>"+ secureDevice +"</SecureDevice>" +
                "<SequenceNo>0010010010</SequenceNo>" +
                "</Admin>" +
                "</TStream>";

        String response = mDSIEMVAndroid.ProcessTransaction(request);

        textViewResponse.setText(response);

        updateBatchParameters(response);
    }

    //BatchClose
    private void batchClose(dsiEMVAndroid mDSIEMVAndroid){
        String request="<?xml version='1.0'?>" +
                "<TStream>" +
                "<Admin>" +
                "<OperationMode>CERT</OperationMode>" +
                "<MerchantID>" + merchantID +"</MerchantID>" +
                "<TerminalID>001</TerminalID>" +
                "<POSPackageID>EMVUSClient:1.26</POSPackageID>" +
                "<OperatorID>Test</OperatorID>" +
                "<UserTrace>Dev1</UserTrace>" +
                "<TranType>Administrative</TranType>" +
                "<TranCode>BatchClose</TranCode>" +
                "<SecureDevice>"+ secureDevice +"</SecureDevice>" +
                "<SequenceNo>0010010010</SequenceNo>" +
                "<BatchNo>" + batchNo + "</BatchNo>" +
                "<BatchItemCount>" + batchItemCount + "</BatchItemCount>" +
                "<NetBatchTotal>" + netBatchTotal + "</NetBatchTotal>" +
                "</Admin>" +
                "</TStream>";

        String response = mDSIEMVAndroid.ProcessTransaction(request);
        batchNo = "";

        textViewResponse.setText(response);
    }

    //Add entry to the transaction table, this shouldn't be used on its own?
    private void addToTable(String order_number, String auth_number, String ref_number, String transaction_amount, String transaction_tip,
                            String transaction_total, String batch_number, String transaction_status, String transaction_type){
        TableRow newRow = new TableRow(this);

        TextView OrderNumber = new TextView(this);
        TextView authNumber = new TextView(this);
        TextView refNumber = new TextView(this);
        TextView amount = new TextView(this);
        TextView tip = new TextView(this);
        TextView total = new TextView(this);
        TextView batchNumber = new TextView(this);
        TextView status = new TextView(this);
        TextView type = new TextView(this);

        float scale = getResources().getDisplayMetrics().density;
        int dpAsPixels = (int) (10*scale + 0.5f);

        OrderNumber.setText(order_number);
        OrderNumber.setTextColor(Color.WHITE);
        OrderNumber.setPadding(dpAsPixels,dpAsPixels,dpAsPixels,dpAsPixels);

        authNumber.setText(auth_number);
        authNumber.setTextColor(Color.WHITE);
        authNumber.setPadding(dpAsPixels,dpAsPixels,dpAsPixels,dpAsPixels);

        refNumber.setText(ref_number);
        refNumber.setTextColor(Color.WHITE);
        refNumber.setPadding(dpAsPixels,dpAsPixels,dpAsPixels,dpAsPixels);

        amount.setText(transaction_amount);
        amount.setTextColor(Color.WHITE);
        amount.setPadding(dpAsPixels,dpAsPixels,dpAsPixels,dpAsPixels);

        tip.setText(transaction_tip);
        tip.setTextColor(Color.WHITE);
        tip.setPadding(dpAsPixels,dpAsPixels,dpAsPixels,dpAsPixels);

        total.setText(transaction_total);
        total.setTextColor(Color.WHITE);
        total.setPadding(dpAsPixels,dpAsPixels,dpAsPixels,dpAsPixels);

        batchNumber.setText(batch_number);
        batchNumber.setTextColor(Color.WHITE);
        batchNumber.setPadding(dpAsPixels,dpAsPixels,dpAsPixels,dpAsPixels);

        status.setText(transaction_status);
        status.setTextColor(Color.WHITE);
        status.setPadding(dpAsPixels,dpAsPixels,dpAsPixels,dpAsPixels);

        type.setText(transaction_type);
        type.setTextColor(Color.WHITE);
        type.setPadding(dpAsPixels,dpAsPixels,dpAsPixels,dpAsPixels);

        newRow.addView(OrderNumber);
        newRow.addView(authNumber);
        newRow.addView(refNumber);
        newRow.addView(amount);
        newRow.addView(tip);
        newRow.addView(total);
        newRow.addView(batchNumber);
        newRow.addView(status);
        newRow.addView(type);

        transactionTable.addView(newRow);

        orderNumber += 1;
    }

    //add to Table With values from response.
    private void addResponseToTable(String response){
        if (response != null && !response.isEmpty()){
            try{
                // set up xml doc
                DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                InputSource inputSource = new InputSource(new StringReader(response));
                Document doc = builder.parse(inputSource);

                String cmdStatus = doc.getElementsByTagName("CmdStatus").item(0).getTextContent();

                if(Objects.equals(cmdStatus, "Approved")){
                    //auth #, ref #, Amount, tip, Batch#, Status
                    String auth_number = doc.getElementsByTagName("AuthCode").item(0).getTextContent();
                    String ref_number = doc.getElementsByTagName("RefNo").item(0).getTextContent();
                    String transaction_amount = doc.getElementsByTagName("Purchase").item(0).getTextContent();
                    String transaction_tip = doc.getElementsByTagName("Gratuity").item(0).getTextContent();
                    String transaction_total = doc.getElementsByTagName("Authorize").item(0).getTextContent();
                    String batch_number = batchNo;
                    String tran_code = doc.getElementsByTagName("TranCode").item(0).getTextContent();

                    addToTable(String.valueOf(orderNumber), auth_number, ref_number, transaction_amount, transaction_tip, transaction_total,
                            batch_number, cmdStatus, tran_code);

                    // it failed, print out Datacap error messages
                    System.out.printf("%s %s %s - %s",
                            doc.getElementsByTagName("ResponseOrigin").item(0).getTextContent(), cmdStatus,
                            doc.getElementsByTagName("DSIXReturnCode").item(0).getTextContent(),
                            doc.getElementsByTagName("TextResponse").item(0).getTextContent());
                }
            } catch (ParserConfigurationException | SAXException | IOException | DOMException e) {
                System.out.println(e.toString());
            }
        }
    }

    //Update the batch related variables
    private void updateBatchParameters(String response){
        if (response != null && !response.isEmpty()){
            try{
                // set up xml doc
                DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                InputSource inputSource = new InputSource(new StringReader(response));
                Document doc = builder.parse(inputSource);

                String cmdStatus = doc.getElementsByTagName("CmdStatus").item(0).getTextContent();

                if(Objects.equals(cmdStatus, "Success")){
                    batchNo = doc.getElementsByTagName("BatchNo").item(0).getTextContent();
                    batchItemCount = doc.getElementsByTagName("BatchItemCount").item(0).getTextContent();
                    netBatchTotal = doc.getElementsByTagName("NetBatchTotal").item(0).getTextContent();
                    // it failed, print out Datacap error messages
                    System.out.printf("%s %s %s - %s",
                            doc.getElementsByTagName("ResponseOrigin").item(0).getTextContent(), cmdStatus,
                            doc.getElementsByTagName("DSIXReturnCode").item(0).getTextContent(),
                            doc.getElementsByTagName("TextResponse").item(0).getTextContent());
                }

            } catch (ParserConfigurationException | SAXException | IOException | DOMException e) {
                System.out.println(e.toString());
            }
        }
    }

    //Write to a log file
    private void writeToFile(String response){
        if(isExternalStorageAvaiableForRW()){
            fileContent = response;
            if (!fileContent.equals("")){
                File myExternalFile = new File(getExternalFilesDir(filepath), filename);
                FileOutputStream fos = null;
                try{
                    fos = new FileOutputStream(myExternalFile, true);

                    String dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(Calendar.getInstance().getTime());

                    fos.write("====================\n".getBytes());
                    fos.write((dateFormat + "\n").getBytes());
                    fos.write(fileContent.getBytes());
                    fos.write("====================\n".getBytes());
                } catch (FileNotFoundException e){
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}