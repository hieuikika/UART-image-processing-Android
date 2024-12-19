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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

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

public class AxsesActivity extends AppCompatActivity {

    private List<String> labels;
    private Set<String> targetObjects = new HashSet<>(Arrays.asList("chai", "bàn phím"));  // Set ban đầu
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

    private boolean isFlashOn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_axses);
        getPermission();

        try {
            labels = FileUtil.loadLabels(this, "labels.txt");
            model = SsdMobilenetV11Metadata1.newInstance(this);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Gagal Inisialisasi Model", Toast.LENGTH_SHORT).show();
            finish();
        }
        Button btnFlashlight = findViewById(R.id.btn_flashlight);
        btnFlashlight.setOnClickListener(view -> toggleFlashlight());

        imageProcessor = new ImageProcessor.Builder().add(new ResizeOp(300, 300, ResizeOp.ResizeMethod.BILINEAR)).build();

        HandlerThread handlerThread = new HandlerThread("videoThread");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());

        imageView = findViewById(R.id.imageView);
        textureView = findViewById(R.id.textureView);
        paint = new Paint();

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

                // Vẽ các đối tượng nhận diện
                for (int index = 0; index < scores.length; index++) {
                    if (scores[index] > 0.5) {
                        String detectedObject = labels.get((int) classes[index]).toLowerCase(); // Chuyển sang chữ thường

                        // Kiểm tra nếu đối tượng được nhận diện nằm trong danh sách mục tiêu
                        if (targetObjects.contains(detectedObject.trim())) {
                            int x = index * 4;
                            paint.setColor(Color.RED); // Màu đỏ cho khung hình
                            paint.setStyle(Paint.Style.STROKE);
                            canvas.drawRect(
                                    new RectF(
                                            locations[x + 1] * w,
                                            locations[x] * h,
                                            locations[x + 3] * w,
                                            locations[x + 2] * h
                                    ), paint
                            );
                            paint.setStyle(Paint.Style.FILL);
                            canvas.drawText(
                                    detectedObject + " " + scores[index],
                                    locations[x + 1] * w,
                                    locations[x] * h,
                                    paint
                            );
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
// Sau khi xử lý dữ liệu nhập vào từ EditText
            String input = editTextInput.getText().toString().trim().toLowerCase();  // Lấy dữ liệu và chuyển thành chữ thường

            if (!input.isEmpty()) {
                // Tách dữ liệu nhập vào thành các từ (tách bằng dấu phẩy hoặc khoảng trắng)
                String[] objects = input.trim().split("\\s*,\\s*");  // Sử dụng regex để tách chính xác hơn

                // Cập nhật trực tiếp vào targetObjects, thay thế toàn bộ nội dung
                targetObjects.clear();  // Xóa tất cả phần tử hiện tại trong targetObjects
                for (String obj : objects) {
                    targetObjects.add(obj.trim().toLowerCase());  // Thêm các từ mới vào targetObjects
                }

                // In lại targetObjects ra Logcat để kiểm tra
                Log.d("DataX", "Cập nhật targetObjects: " + targetObjects.toString());

                // Xóa nội dung nhập vào sau khi nhấn enter
                editTextInput.setText("");
                Log.d("DataX", "Cập nhật targetObjects thành công.");
            }


            return true; // Trả về true để tránh bàn phím tự động ẩn
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        model.close();
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
                            Toast.makeText(AxsesActivity.this, "Không thể cấu hình camera", Toast.LENGTH_SHORT).show();
                        }
                    }, handler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
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
                        cameraDevice.createCaptureSession(Collections.singletonList(surface),
                                new CameraCaptureSession.StateCallback() {
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
                                    public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                                        Toast.makeText(AxsesActivity.this, "Gagal Membuka Kamera", Toast.LENGTH_SHORT).show();
                                    }
                                },
                                handler
                        );
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice cameraDevice) {
                    Toast.makeText(AxsesActivity.this, "Camera disconnected", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(@NonNull CameraDevice cameraDevice, int i) {
                    Toast.makeText(AxsesActivity.this, "Error Membuka Kamera", Toast.LENGTH_SHORT).show();
                }
            }, handler);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getCameraId(int facing) {
        try {
            for (String cameraId : cameraManager.getCameraIdList()) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
                Integer lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (lensFacing != null && lensFacing == facing) {
                    return cameraId;
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return null;
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

    private void getPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.CAMERA}, 1001);
        }
    }
}
