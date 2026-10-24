# Kiểm tra tổ chức và đóng gói ngày 23/10/2026

- Native CMD: run.cmd demo chạy source SPMF trong vendor, tái hiện phản ví dụ và Safe khớp oracle.
- run.cmd check: 9 ca cố định + 2.759 prefix từ 500 database; RU bật/tắt đều khớp, 5 input sai bị từ chối. Xem safe-checks.txt.
- PowerShell với ExecutionPolicy Restricted vẫn gọi .cmd và chạy demo thành công; không dùng .ps1 hoặc Bypass trong đường chạy mới.
- ZIP đã được kiểm tra CRC và SHA-256 từng file theo MANIFEST, giải nén sang thư mục có dấu cách và build/chạy demo từ đó.
- original-check vẫn exit 1 vì 3 sai khác đã biết; không đổi tiêu chí để biến bản gốc thành PASS.
- Pilot đã chạy lại sau khi chuyển folder, 12 JVM đều khớp oracle. Kết quả mới nằm trong results/local/safe-pilot và không thay số liệu pilot ngày 21/10 trong tài liệu lịch sử.

Các kiểm tra trên xác nhận bộ chương trình đóng gói hoạt động. Chưa phải benchmark dữ liệu đầy đủ, chứng minh tăng tốc tổng quát hay báo cáo giữa kỳ đã được duyệt.
