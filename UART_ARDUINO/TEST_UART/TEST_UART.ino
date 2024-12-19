const int ledPin = 12;  // LED pin
unsigned long timeLedOff = 0;  // Biến lưu thời gian khi LED được tắt
bool isLedOff = false;  // Biến kiểm tra trạng thái LED (tắt hay không)

void setup() {
  Serial.begin(115200);  // Mở cổng serial
  pinMode(ledPin, OUTPUT);
}

void loop() {
  if (Serial.available() > 0) {  // Kiểm tra xem có dữ liệu gửi đến
    String input = Serial.readString();  // Đọc dữ liệu 
    input.trim();  // Loại bỏ các ký tự thừa

    // Kiểm tra nếu dữ liệu nhập vào là "sáng" hoặc "tắt"
    if (input.equalsIgnoreCase("sáng")) {
      digitalWrite(ledPin, HIGH);  // Bật đèn LED (HIGH)
      Serial.println("LED bật");   // Gửi thông tin về trạng thái LED
      isLedOff = false;  // Đánh dấu là LED đang bật
    } else if (input.equalsIgnoreCase("tắt")) {
      digitalWrite(ledPin, LOW);  // Tắt đèn LED (LOW)
      Serial.println("LED tắt");  // Gửi thông tin về trạng thái LED
      timeLedOff = millis();  // Lưu lại thời điểm LED tắt
      isLedOff = true;  // Đánh dấu là LED đã tắt
    }
  }

  // Nếu LED đã tắt, mỗi 500ms sẽ gửi thông báo về trạng thái LED tắt
  if (isLedOff) {
    unsigned long elapsedTime = millis();  // Tính thời gian kể từ khi LED tắt
      delay(10);
      Serial.print("LED đang bị tắt ở khoảng ");  // In ra thông báo
      Serial.print(elapsedTime );  // In ra thời gian đã trôi qua (tính bằng giây)
      Serial.println(" mili giây kể từ khi chạy");
      
    }
      if (!isLedOff) {
    unsigned long elapsedTime2 = millis();  // Tính thời gian kể từ khi LED tắt
      delay(10);
      Serial.println("LED đang bật ở khoảng ");  // In ra thông báo
      Serial.print(elapsedTime2 );  // In ra thời gian đã trôi qua (tính bằng giây)
      Serial.println(" mili giây kể từ khi chạy/.,\';[]-0987654321`qứaersfvsssssfhhhhhhhhhhhhhhhhhhhhhhhhhhhss;ioooooooooooooo");
      
    }
  }
