package ikhwan.hanif.deteksiobjek;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import androidx.appcompat.app.AppCompatActivity;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;

import ikhwan.hanif.deteksiobjek.ml.SsdMobilenetV11Metadata1;

public class ImageActivity extends AppCompatActivity {
    //USBserial
    private static final String ACTION_USB_PERMISSION = "ikhwan.hanif.deteksiobjek.USB_PERMISSION";
    private UsbSerialPort usbSerialPort;

    //image
    private List<String> labels;
    private Set<String> targetObjects = new HashSet<>(Arrays.asList(""));  // Set ban đầu
    private Paint paint;
    private ImageProcessor imageProcessor;
    private Bitmap bitmap;
    private ImageView imageView;
    private CameraDevice cameraDevice;
    private Handler handler;
    private CameraManager cameraManager;
    private TextureView textureView;
    private SsdMobilenetV11Metadata1 model;
    private boolean isUsingFrontCamera = false;

    private boolean isSend = false;
    private boolean isUsbReceiverRegistered = false; // Đảm bảo chỉ đăng ký receiver 1 lần

    private String targetObjectName = ""; // Lưu tên vật thể người dùng nhập vào
    private float objectX = 0.0f; // Tọa độ x của vật thể
    private float objectY = 0.0f; // Tọa độ y của vật thể

    // Các TextView cho mỗi vật thể
    private TextView[] textViews = new TextView[4];
    private int objectCount = 0;  // Đếm số lượng đối tượng hiển thị

    private static final String TAG = "ObjectMapping";
    private String globalString = "*none:x1,y1,s1;none:x2,y2,s2;none:x3,y3,s3;none:x4,y4,s4#";

    private List<String> dataBuffer = new ArrayList<>();  // Bộ đệm dữ liệu

    private boolean isFlashOn = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);
        getPermission();
        UsbManager usbManager = (UsbManager) getSystemService(USB_SERVICE);
        List<UsbSerialDriver> drivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager);

        if (drivers.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy thiết bị USB", Toast.LENGTH_SHORT).show();
            return;
        }
        // Chọn driver đầu tiên
        UsbSerialDriver driver = drivers.get(0);
        UsbDevice device = driver.getDevice();
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

        Button btnFlashlight = findViewById(R.id.btn_flashlight);
        btnFlashlight.setOnClickListener(view -> toggleFlashlight());


        try {
            labels = FileUtil.loadLabels(this, "labels.txt");
            model = SsdMobilenetV11Metadata1.newInstance(this);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Gagal Inisialisasi Model", Toast.LENGTH_SHORT).show();
            finish();
        }

        imageProcessor = new ImageProcessor.Builder().add(new ResizeOp(300, 300, ResizeOp.ResizeMethod.BILINEAR)).build();

        HandlerThread handlerThread = new HandlerThread("videoThread");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());

        imageView = findViewById(R.id.imageView);
        textureView = findViewById(R.id.textureView);
        paint = new Paint();

        // Khởi tạo các TextView
        textViews[0] = findViewById(R.id.onebot);
        textViews[1] = findViewById(R.id.twobot);
        textViews[2] = findViewById(R.id.threebot);
        textViews[3] = findViewById(R.id.fourbot);
        //khởi tạo button
        Button onSend = findViewById(R.id.OnSend);
        Button offSend = findViewById(R.id.OffSend);
        onSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isSend = true;
            }
        });

        offSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isSend = false;
            }
        });

        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int width, int height) {
                openCamera();
            }

            @Override
            public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surfaceTexture, int width, int height) {}

            @Override
            public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surfaceTexture) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surfaceTexture) {
                bitmap = textureView.getBitmap();
                if (bitmap == null) return;

                TensorImage image = TensorImage.fromBitmap(bitmap);
                image = imageProcessor.process(image);

                SsdMobilenetV11Metadata1.Outputs outputs = model.process(image);
                float[] locations = outputs.getLocationsAsTensorBuffer().getFloatArray();
                float[] classes = outputs.getClassesAsTensorBuffer().getFloatArray();
                float[] scores = outputs.getScoresAsTensorBuffer().getFloatArray();

                Bitmap mutable = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                Canvas canvas = new Canvas(mutable);

                int h = mutable.getHeight();
                int w = mutable.getWidth();
                paint.setTextSize(h / 15f);
                paint.setStrokeWidth(h / 85f);

                objectCount = 0;  // Reset số lượng vật thể mỗi lần vẽ

                // Lưu trữ các tên đối tượng nhận diện và hiển thị chúng trong TextView tương ứng
                Set<String> detectedObjects = new HashSet<>();

                for (int index = 0; index < scores.length; index++) {
                    if (scores[index] > 0.5) {
                        String detectedObject = labels.get((int) classes[index]).toLowerCase(); // Chuyển sang chữ thường

                        // Kiểm tra nếu đối tượng được nhận diện nằm trong danh sách mục tiêu
                        if (targetObjects.contains(detectedObject.trim()) && objectCount < 4 && !detectedObjects.contains(detectedObject)) {
                            int x = index * 4;

                            // Lấy tọa độ của đối tượng
                            float left = locations[x + 1] * w;
                            float top = locations[x] * h;
                            float right = locations[x + 3] * w;
                            float bottom = locations[x + 2] * h;

                            objectX = (left + right) / 2;  // Tọa độ x trung tâm
                            objectY = (top + bottom) / 2;  // Tọa độ y trung tâm

                            // Tính diện tích ô vuông (S)
                            float width = right - left;
                            float height = bottom - top;
                            float S = width * height;  // Diện tích ô vuông

                            // Cập nhật TextView với tên vật thể và tọa độ
                            if (objectCount < targetObjects.size()) {
                                textViews[objectCount].setText(detectedObject + ": x=" + (int) objectX + "; y=" + (int) objectY + "; s=" + (int) S);
                                objectCount++;  // Tăng số lượng đối tượng hiển thị
                                processInput(detectedObject,objectX,objectY,S,objectCount);
                            }

                            // Vẽ khung bao quanh vật thể
                            paint.setColor(Color.RED); // Màu đỏ cho khung hình
                            paint.setStyle(Paint.Style.STROKE);
                            canvas.drawRect(new RectF(left, top, right, bottom), paint);

                            // Vẽ tên vật thể và độ tin cậy
                            paint.setStyle(Paint.Style.FILL);
                            canvas.drawText(
                                    detectedObject + " " + scores[index],
                                    left,
                                    top,
                                    paint
                            );

                            detectedObjects.add(detectedObject);  // Thêm vật thể vào danh sách đã nhận diện
                        }
                    }
                }

                imageView.setImageBitmap(mutable);
            }

        });

        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        // Xử lý nhập tên vật thể từ EditText
        EditText editTextInput = findViewById(R.id.editTextInput);
        editTextInput.setOnEditorActionListener((v, actionId, event) -> {
            String input = editTextInput.getText().toString().trim().toLowerCase();  // Lấy dữ liệu và chuyển thành chữ thường

            if (!input.isEmpty()) {
                String[] objects = input.trim().split("\\s*,\\s*");  // Tách các từ bằng dấu phẩy hoặc khoảng trắng

                targetObjects.clear();  // Xóa tất cả phần tử hiện tại trong targetObjects
                for (String obj : objects) {
                    targetObjects.add(obj.trim().toLowerCase());  // Thêm các từ mới vào targetObjects
                }
                targetObjectName = input;
                Log.d("DataX", "Cập nhật targetObjects: " + targetObjects.toString());

                editTextInput.setText("");  // Xóa nội dung nhập vào sau khi nhấn enter

                Log.d("DataX", "Cập nhật targetObjects thành công.");
            }

            return true; // Trả về true để tránh bàn phím tự động ẩn
        });
    }

    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            Log.d("USB", "Quyền được cấp: " + device);
                            UsbManager usbManager = (UsbManager) getSystemService(USB_SERVICE);
                            List<UsbSerialDriver> drivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager);
                            if (!drivers.isEmpty()) {
                                openUsbPort(usbManager, drivers.get(0));
                            }
                        }
                    } else {
                        Log.d("USB", "Quyền bị từ chối");
                        Toast.makeText(context, "Quyền USB bị từ chối", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    };
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
                    long lastUpdateTime = System.currentTimeMillis(); // Thời điểm cập nhật cuối cùng

                    while (true) {
                        try {
                            byte[] buffer = new byte[256];
                            int numBytesRead = usbSerialPort.read(buffer, 100);
                            if (numBytesRead > 0) {
                                String receivedData = new String(buffer, 0, numBytesRead);
                                dataBuffer.add(receivedData);
                            }

                            long currentTime = System.currentTimeMillis();
                            if (currentTime - lastUpdateTime >= 50) { // Cập nhật UI mỗi 100ms
                                lastUpdateTime = currentTime;

                                StringBuilder sb = new StringBuilder();
                                for (String data : dataBuffer) {
                                    sb.append(data).append("\n");
                                }

                                runOnUiThread(() -> {
                                    textViewData.setText(sb.toString());
                                    scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
                                });

                                dataBuffer.clear();
                            }
                            Thread.sleep(10); // Nghỉ để giảm tải CPU
                        } catch (IOException | InterruptedException e) {
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
                Toast.makeText(this, "Gửi: " + command, Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                Toast.makeText(this, "Lỗi gửi dữ liệu", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }
    }

    private void processInput(String detectedObject, float objectX, float objectY, float S, int objectCount) {
        // Tách chuỗi `globalString` thành từng phần
        String[] segments = globalString.substring(1, globalString.length() - 1).split(";"); // Bỏ ký tự '*' và '#' rồi tách theo ';'

        // Tạo chuỗi mới cho đối tượng hiện tại (bỏ số thứ tự)
        StringBuilder updatedSegment = new StringBuilder();
        updatedSegment.append(detectedObject.trim()) // Loại bỏ khoảng trắng trước và sau tên đối tượng
                .append(":")
                .append((int) objectX).append(",") // Lấy tọa độ x
                .append((int) objectY).append(",") // Lấy tọa độ y
                .append((int) S); // Lấy diện tích

        // Cập nhật chuỗi tương ứng trong `segments`
        segments[objectCount - 1] = updatedSegment.toString();

        // Gộp lại thành chuỗi `globalString`
        StringBuilder updatedString = new StringBuilder("*");
        for (int i = 0; i < segments.length; i++) {
            updatedString.append(segments[i]);
            if (i < segments.length - 1) {
                updatedString.append(";");
            }
        }
        updatedString.append("#");

        // Gán giá trị mới cho chuỗi toàn cục
        globalString = updatedString.toString();
        if(isSend){
            sendCommand(globalString); // Gửi chuỗi qua UART
        }

        // In ra logcat để kiểm tra
        Log.d("DataG", "Cập nhật targetObjects thành công. " + globalString);
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

    private void toggleFlashlight() {
        if (cameraDevice == null) {
            Toast.makeText(this, "Camera chưa được khởi tạo", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Xây dựng yêu cầu chụp với đèn flash
            CaptureRequest.Builder captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
            Surface surface = new Surface(surfaceTexture);

            captureRequestBuilder.addTarget(surface);
            if (isFlashOn) {
                captureRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);
                isFlashOn = false;
            } else {
                captureRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH);
                isFlashOn = true;
            }

            // Gửi yêu cầu chụp tới camera
            cameraDevice.createCaptureSession(Collections.singletonList(surface),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            try {
                                session.setRepeatingRequest(captureRequestBuilder.build(), null, handler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            Toast.makeText(ImageActivity.this, "Không thể cấu hình camera", Toast.LENGTH_SHORT).show();
                        }
                    }, handler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void onSwitchCameraButtonClick(View view) {
        isUsingFrontCamera = !isUsingFrontCamera;
        closeCamera();
        openCamera();
    }

    private void closeCamera() {
        if (cameraDevice != null) {
            cameraDevice.close();
        }
    }
    @SuppressLint("MissingPermission")
    private void openCamera() {
        String cameraId = isUsingFrontCamera
                ? getCameraId(CameraCharacteristics.LENS_FACING_FRONT)
                : getCameraId(CameraCharacteristics.LENS_FACING_BACK);

        try {
            cameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    cameraDevice = camera;
                    SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
                    Surface surface = new Surface(surfaceTexture);

                    try {
                        cameraDevice.createCaptureSession(Collections.singletonList(surface), new CameraCaptureSession.StateCallback() {
                            @Override
                            public void onConfigured(@NonNull CameraCaptureSession session) {
                                try {
                                    CaptureRequest.Builder captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                                    captureRequestBuilder.addTarget(surface);
                                    session.setRepeatingRequest(captureRequestBuilder.build(), null, handler);
                                } catch (CameraAccessException e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onConfigureFailed(@NonNull CameraCaptureSession session) {}

                        }, handler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    cameraDevice.close();
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    Log.e("Camera", "Camera error: " + error);
                }
            }, handler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private String getCameraId(int lensFacing) {
        try {
            for (String cameraId : cameraManager.getCameraIdList()) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
                int facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing == lensFacing) {
                    return cameraId;
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void getPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.CAMERA}, 1);
        }
    }

}
