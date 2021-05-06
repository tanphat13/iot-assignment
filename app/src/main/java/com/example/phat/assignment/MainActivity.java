package com.example.phat.assignment;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements SerialInputOutputManager.Listener
 {
    MQTTHelper mqttHelper;
    final String TAG = "MAIN_TAG";
    private static final String ACTION_USB_PERMISSION = "com.android.recipes.USB_PERMISSION";
    private static final String INTENT_ACTION_GRANT_USB = BuildConfig.APPLICATION_ID + ".GRANT_USB";
    String buffer  = "";

    UsbSerialPort port;

     private void startMQTT() {
         mqttHelper = new MQTTHelper(getApplicationContext());
         mqttHelper.setCallback(new MqttCallbackExtended() {
             @Override
             public void connectComplete(boolean b, String s) {

             }

             @Override
             public void connectionLost(Throwable throwable) {

             }

             @Override
             public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
                 port.write("LED".getBytes(), 1000);
             }

             @Override
             public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

             }
         });

     }

     protected void sendMQTTMessage(String data) {
         MqttMessage msg = new MqttMessage();
         msg.setId(1234);
         msg.setQos(0);
         msg.setRetained(true);

         byte[] b = data.getBytes(Charset.forName("UTF-8"));
         msg.setPayload(b);

         Log.d("ABC", "Publish :" + msg);
         try {
             mqttHelper.mqttAndroidClient.publish("tuannguyenngoc/feeds/test1", msg);
         } catch (MqttException e) {
             Log.d("MQTT","Publishing Error");
         }
     }

    private void openUART(){
        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);

        if (availableDrivers.isEmpty()) {
            Log.d(TAG, "UART is not available");

        }else {
            Log.d(TAG, "UART is available");

            UsbSerialDriver driver = availableDrivers.get(0);
            UsbDeviceConnection connection = manager.openDevice(driver.getDevice());
            if (connection == null) {

                PendingIntent usbPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(INTENT_ACTION_GRANT_USB), 0);
                manager.requestPermission(driver.getDevice(), usbPermissionIntent);

                manager.requestPermission(driver.getDevice(), PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0));


                return;
            } else {

                port = driver.getPorts().get(0);
                try {
                    port.open(connection);
                    port.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);

                    SerialInputOutputManager usbIoManager = new SerialInputOutputManager(port, this);
                    Executors.newSingleThreadExecutor().submit(usbIoManager);
                    Log.d(TAG, "UART is openned");

                } catch (Exception e) {
                    Log.d(TAG, "There is error");
                }
            }
        }
    }

    @Override
     public void onNewData(byte[] data) {
         buffer += new String(data);
         Log.d("Received data", buffer);
         sendMQTTMessage(buffer);
     }

     @Override
     public void onRunError(Exception e) {

     }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        openUART();
        startMQTT();
    }
}
