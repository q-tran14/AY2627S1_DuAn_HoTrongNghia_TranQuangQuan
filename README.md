# Khai phá tập mục lợi ích cao – chương trình giữa kỳ

Đề tài: Research and Development of Solutions for Mining High-Utility Itemsets from Incremental Datasets.

**Thư mục đang làm và chuẩn bị nộp: [midterm](midterm/README.md).**

Mở thư mục `midterm` bằng File Explorer, nhập `cmd` vào thanh địa chỉ rồi nhấn Enter. Cửa sổ CMD sẽ mở ngay tại thư mục đó. Chạy:

```bat
run.cmd demo
run.cmd check
run.cmd incremental-demo
run.cmd pack
```

Nếu dùng PowerShell, mở terminal tại thư mục `midterm` rồi chạy:

```powershell
.\run.cmd demo
.\run.cmd check
.\run.cmd incremental-demo
.\run.cmd pack
```

Có thể đặt hoặc giải nén dự án ở bất kỳ ổ đĩa nào. run.cmd tự xác định thư mục chứa nó; các đường dẫn dữ liệu tương đối được tính từ thư mục midterm.

Cần JDK 25. Lệnh .cmd gọi Java trực tiếp, không cần Set-ExecutionPolicy. ZIP tạo tại midterm/dist/SUM_midterm.zip; chỉ gửi gói này khi đã bổ sung báo cáo theo yêu cầu thầy.

| Thư mục | Vai trò |
|---|---|
| midterm/ | Code, source gốc, dataset dùng, tài liệu và bằng chứng; chạy độc lập |
| reference/ | Thuật toán HUIM/dataset khác và ghi chú lịch sử, để tham khảo tiếp |
| archive/ | Bản khôi phục cấu trúc cũ trước khi dọn; không gửi trong gói nộp |

Build, .class, tmp và bảng/biểu đồ cũ không thuộc kết quả kiểm chứng hiện tại đã được dọn khỏi cây đang làm. Các bảng/biểu đồ và script cũ được giữ trong bản khôi phục; không dùng làm số liệu mới của SUM-Safe.

Ghi chú báo cáo: [REPORT_GUIDE.md](midterm/docs/REPORT_GUIDE.md). Thay đổi chương trình và kết quả: [SUM_SAFE_JAVA.md](midterm/docs/SUM_SAFE_JAVA.md).

Đã có API giữ trạng thái qua batch và policy ngưỡng động trong một phiên RAM. Hướng dẫn chạy: [INCREMENTAL_API.md](midterm/docs/INCREMENTAL_API.md).
