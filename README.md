# Phát Hiện Đối Tượng Trên Android

<img width="100" alt="icon-image-detection" src="app/src/main/res/drawable/icon.png">

## video demo  
<img width="200" alt="sc-image-detection-1" src="https://drive.google.com/file/d/1I_TOUaBGab6d6XzOKA1KlsAntyYz1El2/view?usp=sharing">  


## Mô Tả  
Phát Hiện Đối Tượng Trên Android là một ứng dụng đơn giản cho phép người dùng phát hiện các đối tượng bằng camera trên thiết bị Android. Ứng dụng này sẽ giúp nhận diện vật thể và sau đó gửi tọa độ về arduino nhằm định vị vật thể. 

## Tính Năng Chính  
- Phát hiện đối tượng thời gian thực bằng camera trên thiết bị.  
- Phát hiện đối tượng từ hình ảnh do người dùng chọn.  
- Giao diện đơn giản và thân thiện với người dùng.  
- Tích hợp Firebase ML Kit để phát hiện đối tượng.
- Tích hợp chức năng truyền tọa độ về vi điều khiển.
- Tích hợp chức năng truyền giá trị String UART về vi điều khiển.
- Tích hợp chức năng định vị tối đa 4 vật thể cùng lúc.   

## Cách Sử Dụng truyền tọa độ UART ARDUINO
1. Mở ứng dụng trên thiết bị Android của bạn.  
2. Kết nối arduino với điện thoại thông qua cổng chuyển đổi type-c to usb (rất quan trọng)
3. Ấn vào chữ UART khi vào hãy cho phép tất cả
4. Quay trở lại và vào UART-Image sau đó hãy nhập tên vậy thể ví dụ: 'chai ' sau đó ấn ok hoặc nhập 4 vật thể cùng lúc như 'chai,chuột,bàn phím,voi' sau đó nhấn ok
5. Ấn On sẽ gửi giá trị tọa độ qua UART đến arduino,và ngược lại

## Công Nghệ Sử Dụng  
- Ngôn ngữ lập trình: Java  
- Android SDK  
- Firebase ML Kit  

## Hướng Dẫn Cài Đặt  
1. Clone repository này về thiết bị của bạn.  
2. Mở dự án bằng Android Studio.  
3. Chạy dự án trên thiết bị Android hoặc trình giả lập.  

## Đóng Góp  
Chúng tôi luôn hoan nghênh các đóng góp và đề xuất cải tiến. Nếu bạn muốn đóng góp cho dự án này, hãy tạo một pull request.  


