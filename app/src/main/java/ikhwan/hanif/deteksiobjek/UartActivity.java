package ikhwan.hanif.deteksiobjek;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class UartActivity extends AppCompatActivity {
    private static final String ACTION_USB_PERMISSION = "ikhwan.hanif.deteksiobjek.USB_PERMISSION";
    private UsbSerialPort usbSerialPort;
    private EditText editTextMessage;
    private TextView textViewData; // TextView để hiển thị dữ liệu
    private BroadcastReceiver usbReceiver;
    private boolean isUsbReceiverRegistered = false;

    private List<String> dataBuffer = new ArrayList<>();  // Bộ đệm dữ liệu

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_uart);





        Button btnOn = findViewById(R.id.btnOn);
        Button btnOff = findViewById(R.id.btnOff);
        editTextMessage = findViewById(R.id.editTextMessage);
        textViewData = findViewById(R.id.textViewData); // Khởi tạo TextView

        UsbManager usbManager = (UsbManager) getSystemService(USB_SERVICE);
        List<UsbSerialDriver> drivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager);

        if (drivers.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy thiết bị USB", Toast.LENGTH_SHORT).show();
            return;
        }

        // Chọn driver đầu tiên
        UsbSerialDriver driver = drivers.get(0);
        UsbDevice device = driver.getDevice();

        // Tạo BroadcastReceiver chỉ khi cần thiết
        usbReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (ACTION_USB_PERMISSION.equals(action)) {
                    synchronized (this) {
                        UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                            if (device != null) {
                                Log.d("USB", "Quyền được cấp: " + device);
                                openUsbPort(usbManager, driver);
                            }
                        } else {
                            Log.d("USB", "Quyền bị từ chối");
                            Toast.makeText(context, "Quyền USB bị từ chối", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
        };

        // Kiểm tra quyền USB
        if (!usbManager.hasPermission(device)) {
            PendingIntent permissionIntent = PendingIntent.getBroadcast(
                    this,
                    0,
                    new Intent(ACTION_USB_PERMISSION),
                    PendingIntent.FLAG_IMMUTABLE
            );
            usbManager.requestPermission(device, permissionIntent);

            // Đăng ký BroadcastReceiver nếu chưa đăng ký
            if (!isUsbReceiverRegistered) {
                IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
                registerReceiver(usbReceiver, filter);
                isUsbReceiverRegistered = true;
            }
        } else {
            openUsbPort(usbManager, driver);
        }

        btnOn.setOnClickListener(v -> sendCommand("sáng"));
        btnOff.setOnClickListener(v -> sendCommand("tắt"));

        // Lắng nghe sự kiện khi người dùng nhấn Enter trong EditText
        editTextMessage.setOnEditorActionListener((TextView v, int actionId, KeyEvent event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                String message = editTextMessage.getText().toString().trim();

                if (!message.isEmpty()) {
                    Log.d("UARTT", "Chuỗi đã nhập: " + message);
                    sendCommand(message);
                }
                return true;
            }
            return false;
        });
    }

    private void openUsbPort(UsbManager usbManager, UsbSerialDriver driver) {
        try {
            usbSerialPort = driver.getPorts().get(0); // Sử dụng cổng đầu tiên
            usbSerialPort.open(usbManager.openDevice(driver.getDevice()));
            usbSerialPort.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
            Toast.makeText(this, "Mở cổng USB thành công", Toast.LENGTH_SHORT).show();

            // Tạo một thread để đọc dữ liệu từ Arduino
            ScrollView scrollView = findViewById(R.id.scrollView); // Tìm ScrollView từ XML
            TextView textViewData = findViewById(R.id.textViewData); // Tìm TextView từ XML
            if (scrollView != null && textViewData != null) {
// Giới hạn tốc độ cập nhật, ví dụ mỗi 100ms (hoặc nhiều hơn)
                new Thread(() -> {
                    while (true) {
                        try {
                            byte[] buffer = new byte[256];  // Tăng kích thước buffer
                            int numBytesRead = usbSerialPort.read(buffer, 1000);
                            if (numBytesRead > 0) {
                                String receivedData = new String(buffer, 0, numBytesRead);
                                dataBuffer.add(receivedData);  // Thêm dữ liệu vào bộ đệm

                                // Nếu bộ đệm đủ lớn, cập nhật giao diện
                                if (dataBuffer.size() >= 20) {  // Ví dụ, chỉ cập nhật khi có ít nhất 10 dòng
                                    StringBuilder sb = new StringBuilder();
                                    for (String data : dataBuffer) {
                                        sb.append(data).append("\n");
                                    }

                                    // Cập nhật UI
                                    runOnUiThread(() -> {
                                        textViewData.setText(sb.toString());
                                        scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
                                    });

                                    // Dọn dẹp bộ đệm
                                    dataBuffer.clear();
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();

            }
        } catch (IOException e) {
            Toast.makeText(this, "Không thể mở cổng USB", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void sendCommand(String command) {
        if (usbSerialPort != null) {
            try {
                usbSerialPort.write((command + "\n").getBytes(), 1000);
                Toast.makeText(this, "Gửi: " + command, Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                Toast.makeText(this, "Lỗi gửi dữ liệu", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (isUsbReceiverRegistered) {
                unregisterReceiver(usbReceiver);
                isUsbReceiverRegistered = false;
            }
            if (usbSerialPort != null) {
                usbSerialPort.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
