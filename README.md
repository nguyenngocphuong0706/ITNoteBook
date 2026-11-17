# IT-notebook-app (Nhóm 7)

Dự án môn học: **Phát triển ứng dụng di động**  
Đề tài: **Ứng dụng Sổ tay điện tử tra cứu cho lập trình viên (IT Formula Notebook)**  
Nhóm thực hiện: **Nhóm 7** (4 thành viên)

---

## 👥 Danh sách thành viên Nhóm 7

| STT | Họ và Tên | Mã sinh viên (MSSV) | Vai trò / Phụ trách |
|:---:|:---|:---:|:---|
| 1 | *Thành viên 1* | *MSSV 1* | Trưởng nhóm / Data & Architecture |
| 2 | *Thành viên 2* | *MSSV 2* | UI/UX & Navigation |
| 3 | *Thành viên 3* | *MSSV 3* | AI Service & Networking |
| 4 | *Thành viên 4* | *MSSV 4* | Custom Heatmap & Testing |

---

## 📱 Giới thiệu dự án

Ứng dụng sổ tay điện tử dành cho lập trình viên, hỗ trợ lưu trữ và tra cứu công thức, thuật toán, cấu trúc dữ liệu, câu lệnh SQL, Regex pattern... 
* **Ngôn ngữ:** Java (Java 11)
* **Kiến trúc:** MVVM (Model - View - ViewModel) + Repository Pattern
* **Cơ sở dữ liệu:** Room Database (SQLite) với Data Pre-population (Seeding)
* **Điều hướng:** Android Jetpack Navigation Component (Single Activity)
* **Trí tuệ nhân tạo:** Tích hợp AI hỗ trợ giải thích code (Explain), dịch code sang 10 ngôn ngữ (Translate), sinh câu hỏi phỏng vấn và chấm điểm bài phỏng vấn (Interview & Grading) với cơ chế Streaming SSE
* **Tính năng trực quan:** Custom Heatmap View (GitHub-style) theo dõi lịch sử học tập 365 ngày

---

## 🚀 Hướng dẫn mở và khởi chạy dự án

### 1. Mở dự án trong Android Studio
1. Mở **Android Studio**.
2. Chọn **File -> Open...** (hoặc màn hình Welcome chọn **Open**).
3. Chọn đúng thư mục gốc của dự án:
   `E:\1.Phat_trien_ung_dung_di_dong\Nhom7_ITNoteBook` *(hoặc thư mục chứa file `settings.gradle.kts` và `build.gradle.kts`)*.
4. Đợi Android Studio hoàn tất **Sync Gradle**.

### 2. Cấu hình AI API Key (Tùy chọn)
Nếu muốn sử dụng các tính năng AI (Giải thích code, Dịch code, Phỏng vấn):
1. Mở file `local.properties` tại thư mục gốc dự án.
2. Thêm Groq API Key miễn phí (lấy tại https://console.groq.com):
   ```properties
   GROQ_API_KEY=gsk_your_groq_api_key_here
   ```

### 3. Build & Run
* Chọn cấu hình chạy là **app** trên thanh công cụ.
* Nhấn nút **Run** (biểu tượng tam giác xanh ▶) hoặc tổ hợp phím `Shift + F10` để khởi chạy trên máy ảo hoặc thiết bị thật.

Hoặc build bằng lệnh terminal:
```bash
gradlew.bat assembleDebug
```
