# Tổ chức lại ngày 23/10/2026

Thư mục đang làm và chuẩn bị nộp là **SUM-Safe/**. Hãy sửa code, chạy lệnh và viết tài liệu chương trình tại đây.

| Trước đây ở gốc repository | Bản đang dùng |
|---|---|
| algorithms/sum/AlgoSUMSafe.java | SUM-Safe/src/algorithms/sum/AlgoSUMSafe.java |
| algorithms/sum/AlgoScentedUtilityMiner.java, ResidueMap.java | SUM-Safe/src/algorithms/sum/; chỉ dùng cho kiểm tra lịch sử local |
| experiments/CheckSUMSafe.java, CheckSumCorrectness.java, RunSUMSafe.java | SUM-Safe/src/experiments/ |
| tmp/audit/official/... | SUM-Safe/vendor/spmf/...; source chính thức dùng demo |
| scripts/run-sum.ps1, demo-counterexample.ps1 | SUM-Safe/run.cmd và src/experiments/StudyCLI.java |
| scripts/pilot_sum_safe.py | SUM-Safe/scripts/pilot_sum_safe.py |
| results/local/... | SUM-Safe/results/local/...; tự sinh khi chạy |
| Kết quả đã chọn làm bằng chứng | SUM-Safe/evidence/; được đóng gói |

Các thuật toán HUIM khác và datasets còn lại nằm ở reference/ ngoài bộ nộp. Chúng có thể dùng để mở rộng nghiên cứu sau này. reference được giữ cục bộ và loại khỏi Git; nó không phải một bản build hoàn chỉnh hay bộ kết quả được xác nhận cho SUM-Safe.

Các script cũ, lịch sử terminal, bảng/biểu đồ cũ chưa tái kiểm chứng được loại khỏi cấu trúc đang làm và ZIP nộp. Bản khôi phục của cấu trúc trước khi dọn được lưu ngoài gói nộp tại archive/. File .class và cache/build/tmp cũ là dữ liệu sinh lại được nên được xóa khỏi cây làm việc.

ZIP nộp giữa kỳ được tạo bởi `run.cmd pack`, chỉ chứa code, nguồn gốc/giấy phép, dữ liệu dùng, tài liệu và bằng chứng được chọn. Không nén cả repository hoặc thư mục reference/archive để gửi thầy.

Ngày 24/10/2026: đổi tên thư mục chương trình từ midterm thành SUM-Safe. Các log và manifest lịch sử được giữ nguyên; tên đường dẫn cũ trong chúng phản ánh thời điểm chạy.
