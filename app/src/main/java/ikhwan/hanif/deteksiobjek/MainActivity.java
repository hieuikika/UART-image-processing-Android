package ikhwan.hanif.deteksiobjek;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;

public class MainActivity extends AppCompatActivity {

    Button scrLangsungBtn, scrGambarBtn, scrAxsesBtn, scrUartBtn, scrImageBtn;
    SoundPool soundPool;
    int soundId;
    MediaPlayer mediaPlayer; // Khai báo MediaPlayer để phát âm thanh chào mừng

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Phát âm thanh chào mừng khi mở ứng dụng
        mediaPlayer = MediaPlayer.create(this, R.raw.welcome);
        mediaPlayer.start();

        // Tạo SoundPool để sử dụng âm thanh click
        soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
        soundId = soundPool.load(this, R.raw.click_me, 1); // Thay bằng âm thanh click

        scrLangsungBtn = findViewById(R.id.secaraLangsungBtn);
        scrGambarBtn = findViewById(R.id.secaraGambarBtn);
        scrAxsesBtn = findViewById(R.id.secaraAxsesBtn);
        scrUartBtn = findViewById(R.id.uartBtn);
        scrImageBtn = findViewById(R.id.uartImagebtn);

        scrLangsungBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playSound();
                startActivity(new Intent(MainActivity.this, LangsungActivity.class));
            }
        });

        scrGambarBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playSound();
                startActivity(new Intent(MainActivity.this, GambarActivity.class));
            }
        });

        scrAxsesBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playSound();
                startActivity(new Intent(MainActivity.this, AxsesActivity.class));
            }
        });

        scrUartBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playSound();
                startActivity(new Intent(MainActivity.this, UartActivity.class));
            }
        });

        scrImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playSound();
                startActivity(new Intent(MainActivity.this, ImageActivity.class));
            }
        });
    }

    private void playSound() {
        soundPool.play(soundId, 1, 1, 0, 0, 1); // Phát âm thanh click
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Giải phóng MediaPlayer
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }

        // Giải phóng SoundPool
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
    }
}
