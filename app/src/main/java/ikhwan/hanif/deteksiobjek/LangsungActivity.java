package ikhwan.hanif.deteksiobjek;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
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
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
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
import java.util.Collections;


import ikhwan.hanif.deteksiobjek.ml.SsdMobilenetV11Metadata1;

public class LangsungActivity extends AppCompatActivity {

    private List<String> labels;
    private List<Integer> colors = Arrays.asList(
            Color.BLUE, Color.GREEN, Color.RED, Color.CYAN, Color.GRAY, Color.BLACK,
            Color.DKGRAY, Color.MAGENTA, Color.YELLOW, Color.RED
    );
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
        setContentView(R.layout.activity_langsung);
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
        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int width, int height) {
                openCamera();
            }

            @Override
            public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surfaceTexture, int width, int height) {
            }

            @Override
            public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surfaceTexture) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surfaceTexture) {
                bitmap = textureView.getBitmap();
                if (bitmap == null) return;

                // Chuyển bitmap thành TensorImage
                TensorImage image = TensorImage.fromBitmap(bitmap);
                image = imageProcessor.process(image);

                // Lấy kích thước màn hình hiện tại
                int w = textureView.getWidth();
                int h = textureView.getHeight();

                // Nếu màn hình xoay ngang, xoay ảnh
                if (w > h) {  // Nếu chế độ màn hình ngang
                    image = rotateImage(image, 90); // Xoay ảnh 90 độ
                }

                // Thực hiện nhận diện đối tượng
                SsdMobilenetV11Metadata1.Outputs outputs = model.process(image);
                float[] locations = outputs.getLocationsAsTensorBuffer().getFloatArray();
                float[] classes = outputs.getClassesAsTensorBuffer().getFloatArray();
                float[] scores = outputs.getScoresAsTensorBuffer().getFloatArray();

                Bitmap mutable = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                Canvas canvas = new Canvas(mutable);

                paint.setTextSize(h / 15f);
                paint.setStrokeWidth(h / 85f);

                // Vẽ các đối tượng nhận diện
                for (int index = 0; index < scores.length; index++) {
                    if (scores[index] > 0.5) {
                        int x = index * 4;
                        paint.setColor(colors.get(index));
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
                                labels.get((int) classes[index]) + " " + scores[index],
                                locations[x + 1] * w,
                                locations[x] * h,
                                paint
                        );
                    }
                }

                // Hiển thị kết quả lên ImageView
                imageView.setImageBitmap(mutable);
            }


        });

        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        paint = new Paint();
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
                            Toast.makeText(LangsungActivity.this, "Không thể cấu hình camera", Toast.LENGTH_SHORT).show();
                        }
                    }, handler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private TensorImage rotateImage(TensorImage image, int degree) {
        // Lấy bitmap từ TensorImage
        Bitmap bitmap = image.getBitmap();

        // Tạo một đối tượng Matrix để xoay hình ảnh
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);  // Xoay hình ảnh theo góc

        // Xoay hình ảnh và tạo bitmap mới
        Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

        // Trả về hình ảnh đã xoay dưới dạng TensorImage
        return TensorImage.fromBitmap(rotatedBitmap);
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

                                            session.setRepeatingRequest(captureRequestBuilder.build(), null, null);
                                        } catch (CameraAccessException e) {
                                            e.printStackTrace();
                                        }
                                    }

                                    @Override
                                    public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                                        Toast.makeText(LangsungActivity.this, "Gagal Membuka Kamera", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(LangsungActivity.this, "Camera disconnected", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(@NonNull CameraDevice cameraDevice, int i) {
                    Toast.makeText(LangsungActivity.this, "Error Membuka Kamera", Toast.LENGTH_SHORT).show();
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
                Integer cameraFacing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (cameraFacing != null && cameraFacing == facing) {
                    return cameraId;
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        throw new RuntimeException("Camera not found");
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
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.CAMERA
        ) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.CAMERA}, 101);
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Perizinan kamera dibutuhkan", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
}
