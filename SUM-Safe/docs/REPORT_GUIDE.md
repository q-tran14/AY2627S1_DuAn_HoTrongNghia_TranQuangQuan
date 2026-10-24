# Nội dung đã làm để viết báo cáo giữa kỳ

Cập nhật tổ chức chương trình: 23/10/2026. Đây là ghi chú để viết báo cáo, không phải bản báo cáo đã hoàn tất hoặc đã được giảng viên duyệt.

## Phạm vi đã tìm hiểu

- HUIM: item, transaction, itemset, utility nội bộ/ngoại, utility theo giao dịch và toàn database; HUI khi utility đạt ngưỡng.
- SUM trong paper: residue map, master map, reinduction; duyệt/ghép itemset; bối cảnh cập nhật dữ liệu và tăng ngưỡng.
- Đối chiếu source SPMF: utility theo TID, giao BitSet, lọc TWU/relevance và đường đệ quy.
- Xác minh nguồn source: bản local tương đương logic với bản tải 20/10; khác package/import và metadata output. Bản gốc tải từ SPMF đã được giữ nguyên trong vendor, kèm hash và GPL.

## Vấn đề đã xác minh

Một giao dịch `1 2 3:12:4 4 4`, ngưỡng 10, maxR=10. Tất cả item vừa xuất hiện, TWU=12. Bộ ba utility 12 phải là HUI; singleton 4 và cặp 8 không cần là HUI trước.

Source công bố bỏ sót bộ ba. Điều kiện không sinh cặp đồng thời ngăn đường đệ quy tới bộ ba. Đây là kết luận về cài đặt SPMF đã kiểm tra, không suy ra toàn bộ số liệu paper năm 2023 sai.

Ca singleton: hai giao dịch đóng góp 10 và 5, output ghi 10 thay vì utility cuối 15. Với đặc tả relevance ở thời điểm cuối, singleton hết hiệu lực còn có thể đã được ghi sớm. Các phát hiện phụ về ngưỡng/cập nhật cần trình bày rõ mức độ triển khai, không gọi là đã sửa toàn bộ paper.

## Phần chương trình đã viết

AlgoSUMSafe là bản Java riêng dùng vertical TID/utility map và BitSet, duyệt theo thứ tự ID duy nhất, cắt bằng remaining-utility upper bound. Điều kiện xuất HUI và mở rộng là hai điều kiện riêng. Chỉ xuất sau khi đọc đủ dữ liệu, dùng long và kiểm tra dữ liệu lỗi.

Oracle vét cạn độc lập tính utility trực tiếp trên mọi tập con nhỏ. Check so cả itemset và utility, RU bật/tắt, ca biên, utility vượt int và tái sử dụng object. Demo hiện chạy hoàn toàn bằng .cmd + Java.

Ngày 23/10 bổ sung initialize/appendBatch/mine: giữ map utility theo TID, TWU và lastTid qua các batch trong một object; thêm policy ngưỡng fixed/paper. Mining projections và join được xây lại từ lịch sử RAM, không đọc lại input cũ. Policy paper dùng minimum tổng utility cuối của tất cả singleton; đây là lựa chọn đặc tả được công bố rõ, xem INCREMENTAL_API.md.

## Bằng chứng có thể đưa vào báo cáo

- Demo cùng input: source công bố không có HUI, Safe trả {1,2,3}:12.
- 9 ca cố định + 2.759 prefix từ 500 database ngẫu nhiên, hai chế độ RU đều khớp oracle; 5 input sai bị từ chối.
- API incremental: 7.036 snapshot từ 500 chuỗi ngẫu nhiên và 4 chuỗi cố định, fixed/paper và RU bật/tắt, khớp oracle và cold-start tại cùng ngưỡng. File batch bị xóa sau append để kiểm chứng không đọc lại; có ca rỗng, reinduction, lỗi và overflow.
- Pilot 12 JVM độc lập trên dữ liệu chiếu từ 200 giao dịch đầu và 12 item của Foodmart/Mushroom. Kết quả mỗi lượt khớp oracle; không gọi đây là benchmark nguyên dataset. Foodmart giữ 38 giao dịch sau chiếu, Mushroom giữ 200.
- Lưu ý rõ ngày của bằng chứng. evidence có bản chép của các lần chạy trước và kết quả kiểm tra bộ đóng gói; không trộn thời gian từ các lần khác nhau.

## Giới hạn phải nêu

- Đặc tả utility không âm, relevance của từng singleton RC>=0 tại thời điểm cuối. Không bảo đảm cả itemset vừa đồng xuất hiện.
- State incremental mới chỉ tồn tại trong RAM của cùng object/tiến trình; chưa lưu/khôi phục session ra đĩa. Mining projections vẫn xây lại mỗi query, chưa cập nhật riêng phần HUI thay đổi hoặc tái sử dụng mọi cache join.
- Tính đúng dựa trên bất biến đường duyệt/cận trên cùng kiểm chứng hữu hạn. Chưa chứng minh tăng tốc hoặc tiết kiệm bộ nhớ tổng quát.
- Remaining utility là kỹ thuật đã có trong HUIM. Đóng góp dự kiến là kiểm chứng, tổ chức lại mining kernel và thực nghiệm từng thành phần; cần giảng viên chốt phạm vi.

## Khung viết giữa kỳ

1. Giới thiệu đề tài, mục tiêu và phạm vi giữa kỳ.
2. Cơ sở bài toán và các thành phần SUM đã tìm hiểu, có ví dụ xuyên suốt.
3. Đối chiếu paper–source và cách kiểm chứng; ghi nguồn, phiên bản và giả thiết.
4. Phản ví dụ, kết quả chạy và nguyên nhân trong code.
5. Giải pháp, cấu trúc code, lập luận tính đúng và các thay đổi so baseline.
6. Kiểm thử, pilot, kết quả và giới hạn.
7. Công việc tiếp theo: đánh giá cập nhật trên dữ liệu đầy đủ, chi phí bộ nhớ/lịch sử và cải tiến thành phần theo phạm vi được duyệt.
8. Tài liệu tham khảo và phụ lục lệnh chạy.

Trong báo cáo cuối vẫn cần bao quát các section paper theo yêu cầu thầy. Không dùng số liệu cũ chưa kiểm chứng ở archive làm kết quả của bản Java hiện tại.
