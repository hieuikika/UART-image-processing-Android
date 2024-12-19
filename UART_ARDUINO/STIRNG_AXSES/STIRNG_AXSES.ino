String inputString = "";  // Biến để lưu trữ chuỗi nhận từ UART
bool stringComplete = false;  // Cờ xác nhận chuỗi đã hoàn tất
const int ledPin = 13;  // LED pin
// Các biến lưu tọa độ và thông tin của từng vật
String x1, y1, s1;  // Tọa độ cho "chai"
String x2, y2, s2;  // Tọa độ cho "bàn phím"
String x3, y3, s3;  // Tọa độ cho "chuột"
String x4, y4, s4;  // Tọa độ cho "chuột"

void setup() {
  Serial.begin(115200);  // Mở Serial với baud rate 115200
  inputString.reserve(200);  // Dự trữ bộ nhớ cho chuỗi dài
   pinMode(ledPin, OUTPUT);
}

void loop() {
  if (stringComplete) {
    // Sau khi nhận đủ chuỗi, xử lý nó
    processData(inputString);
    inputString = "";  // Xóa chuỗi để chuẩn bị nhận dữ liệu mới
    stringComplete = false;
  }
}

// Hàm đọc dữ liệu từ Serial
void serialEvent() {
  while (Serial.available()) {
    char inChar = (char)Serial.read();
    inputString += inChar;  // Thêm kí tự vào chuỗi
    if (inChar == '#') {  // Khi nhận được ký tự #, đánh dấu chuỗi đã hoàn tất
      stringComplete = true;
    }
  }
}

// Hàm xử lý chuỗi
void processData(String data) {
  // Tìm dấu "*" và "#", bỏ qua phần đầu và phần cuối chuỗi
  int startIdx = data.indexOf('*');
  int endIdx = data.indexOf('#');
  if (startIdx != -1 && endIdx != -1 && endIdx > startIdx) {
    data = data.substring(startIdx + 1, endIdx);  // Cắt phần giữa
  }

  // Tách chuỗi theo dấu ";"
  int startIndex = 0;
  int endIndex = data.indexOf(';');

  while (endIndex > 0) {
    String item = data.substring(startIndex, endIndex);  // Lấy phần tử
    parseItem(item);  // Phân tách thông tin và lưu vào các biến

    startIndex = endIndex + 1;  // Cập nhật chỉ số bắt đầu
    endIndex = data.indexOf(';', startIndex);  // Tìm dấu ";" tiếp theo
  }

  // Xử lý phần tử cuối cùng (nếu có)
  if (startIndex < data.length()) {
    parseItem(data.substring(startIndex));
  }
}

void parseItem(String item) {
  int nameEndIdx = item.indexOf(':');
  if (nameEndIdx == -1) return;  // Kiểm tra xem có dấu ":" không
  String name = item.substring(0, nameEndIdx);  
  String values = item.substring(nameEndIdx + 1);
  int xEndIdx = values.indexOf(',');
  int yEndIdx = values.indexOf(',', xEndIdx + 1);

  if (xEndIdx == -1 || yEndIdx == -1) return;  // Đảm bảo chuỗi định dạng đúng

  String x = values.substring(0, xEndIdx);
  String y = values.substring(xEndIdx + 1, yEndIdx);
  String s = values.substring(yEndIdx + 1);

  if (name == "chai") {
    x1 = x; y1 = y; s1 = s;  
    Serial.println("Thông tin chai:");
    Serial.print("x = "); Serial.println(x1);
    Serial.print("y = "); Serial.println(y1);
    Serial.print("s = "); Serial.println(s1);

    // Chuyển x1 thành số nguyên
    int x1_int = x1.toInt();
    

    // So sánh giá trị và điều khiển LED
    if (x1_int > 500 && x1_int < 550) {
      Serial.println("LED bật");
      digitalWrite(ledPin, HIGH);
    } else {
      Serial.println("LED tắt");
      digitalWrite(ledPin, LOW);
    }
  }

//    if (name == "chai") {
//    x2 = x; y2 = y; s2 = s;  
//    Serial.println("Thông tin chai:");
//    Serial.print("x = "); Serial.println(x2);
//    Serial.print("y = "); Serial.println(y2);
//    Serial.print("s = "); Serial.println(s2);
//
//    // Chuyển x1 thành số nguyên
//    int x2_int = x2.toInt();
//    
//
//    // So sánh giá trị và điều khiển LED
//    if (x2_int > 500 && x2_int < 550) {
//      Serial.println("LED bật");
//      digitalWrite(ledPin, HIGH);
//    } else {
//      Serial.println("LED tắt");
//      digitalWrite(ledPin, LOW);
//    }
//  }
}
