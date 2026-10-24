# Kiểm chứng phần incremental ngày 23/10/2026

- 500 chuỗi ngẫu nhiên + 4 chuỗi cố định, seed 20261023.
- 7.036 snapshot, gồm fixed/paper và RU bật/tắt, khớp oracle vét cạn và cold-start tại cùng threshold.
- Ngưỡng kỳ vọng tính độc lập bằng cách cộng minimum tổng utility singleton toàn prefix trước đó.
- Xóa input sau append; mine và những lần cập nhật sau vẫn chạy từ history giữ trong RAM.
- Kiểm tra batch rỗng, metadata, item mới, hết hiệu lực/tái xuất hiện, mine lặp, pending update, append malformed không làm đổi state, output chặn ghi đè input, overflow và reset.
- Demo ba batch dùng ngưỡng 10,14,20; bộ ba có utility 12,18,24. Mỗi output khớp oracle.
- Bộ batch cũ 9 ca + 2.759 prefix vẫn PASS sau khi refactor.

Không có kết luận tăng tốc incremental hoặc checkpoint qua tiến trình từ các kiểm tra này. Policy paper hiện thực minimum của tất cả singleton đã thấy; ngữ nghĩa này được công bố trong docs/INCREMENTAL_API.md.
