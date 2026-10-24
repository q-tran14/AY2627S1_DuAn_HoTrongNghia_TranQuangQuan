# Bản Java SUM-Safe: kết quả ngày 21/10/2026

**Cập nhật 23/10/2026:** đã bổ sung initialize/appendBatch/mine giữ lịch sử trong RAM và chính sách ngưỡng động. Xem [INCREMENTAL_API.md](INCREMENTAL_API.md) cho mô tả và bằng chứng mới. Bảng pilot ngày 21/10 dưới đây là số đo của phiên bản trước, chưa phải benchmark của phiên bản incremental hiện tại.

SUM-Safe là tên làm việc cho bản cài đặt thử nghiệm riêng, không phải tên một thuật toán đã công bố. Giữ ý tưởng lưu utility theo TID và giao BitSet của SUM; thay đường duyệt bằng DFS theo thứ tự duy nhất và cận remaining utility. Class SUM gốc không thay đổi.

## Chạy demo trực tiếp

Chạy tại thư mục midterm, dùng CMD hoặc PowerShell:

```powershell
.\run.cmd demo
```

Demo dùng cùng input, minUtility=10, maxR=10:

```text
1 2 3:12:4 4 4
```

Kết quả đã chạy:

- SUM chính thức: 0 HUI, 0 phép ghép.
- SUM-Safe: `1 2 3 #UTIL: 12`, khớp vét cạn.
- Demo dùng source SPMF đã đặt trong vendor/spmf, không cần tải mạng hoặc tmp.
- Ở thư mục gốc, dùng `.\midterm\run.cmd demo`.

Kiểm tra bản Java mới:

```powershell
.\run.cmd check
```

Chạy dữ liệu mẫu:

```powershell
.\run.cmd run safe datasets/DB_Utility.txt results/local/safe-example.txt 30 10
```

Thêm tham số cuối `false` vào lệnh run safe để tắt quyết định cắt nhánh bằng RU. Cả hai chế độ vẫn dùng cùng thứ tự, cấu trúc dữ liệu, lọc TWU, relevance và vẫn tính cận trên; đây là thí nghiệm tách tác động của quyết định cắt, chưa đo toàn bộ chi phí lưu/tính RU. Lệnh `run.cmd original-check` tiếp tục kiểm tra bản gốc và vẫn báo các sai khác đã biết.

## File và thay đổi

| File | Vai trò |
|---|---|
| src/algorithms/sum/AlgoSUMSafe.java | Mining kernel mới, map utility/remaining theo TID, BitSet, DFS |
| src/experiments/RunSUMSafe.java | Chạy một cấu hình, in số HUI, join, node, nhánh cắt và thời gian từng pha |
| src/experiments/CheckSUMSafe.java | Kiểm tra Java với oracle vét cạn, bật/tắt RU, dữ liệu ngẫu nhiên |
| run.cmd | Build và chọn bản gốc, chính thức hoặc Safe |
| src/experiments/StudyCLI.java | Demo đối chiếu gốc và Safe |
| scripts/pilot_sum_safe.py | Pilot giới hạn kích thước, các JVM độc lập, oracle kiểm tra kết quả |

Đây là bản viết riêng, không phải sửa một câu if trong class cũ. Dùng map long để tránh giới hạn int đối với utility; phát hiện overflow bằng Math.addExact. Bộ đọc kiểm tra item trùng, utility âm và TU không khớp, không âm thầm tiếp tục với dữ liệu lỗi. Reinduction được biểu diễn bằng lastSeen, chưa có thực nghiệm tách riêng đóng góp lastSeen.

## Đặc tả đã cài đặt

- Utility không âm; minUtility > 0; maxR >= 0.
- Utility của itemset cộng trên toàn bộ giao dịch chứa itemset.
- Tại cuối dữ liệu, item i active nếu `N-lastSeen(i) <= maxR`, tương đương RC>=0 của source hiện tại.
- Mỗi item trong itemset phải active. Không yêu cầu lần đồng xuất hiện của cả itemset nằm trong cửa sổ.
- Giữ singleton có TWU >= ngưỡng, sau đó sắp theo ID tăng dần. Thứ tự này khác thứ tự utility của SUM gốc và được chọn cố định để lập luận đường sinh rõ ràng.
- Singleton chỉ được xuất sau khi đọc xong, áp dụng cùng điều kiện ngưỡng/relevance như các tập lớn.
- runAlgorithm là lối chạy một lần, khởi tạo lại phiên. API initialize/appendBatch/mine mới giữ lịch sử giữa các batch trong cùng object và có policy fixed/paper; không đọc lại file cũ. Ngưỡng động và các lựa chọn đặc tả được giải thích ở INCREMENTAL_API.md. Chưa có checkpoint qua lần đóng/mở tiến trình.

## Vì sao không bỏ sót theo đặc tả này?

1. Lọc singleton bằng TWU an toàn với utility không âm: utility mọi tập chứa i không vượt TWU(i).
2. Duyệt theo ID tăng dần: mỗi tập có một biểu diễn tăng duy nhất và một đường sinh.
3. Map của tiền tố X chỉ lưu giao dịch chứa X và utility đúng của X. Ghép với singleton đứng sau X dùng giao BitSet, cộng utility hai tập rời nhau, nên giữ bất biến này.
4. Tại mỗi T chứa X, remaining là tổng utility của các item đã qua lọc và đứng sau phần tử cuối X. Mọi tập mở rộng trên nhánh chỉ lấy item trong phần này.
5. `UB(X)=Σ[u(X,T)+remaining(X,T)]` chặn trên utility mọi mở rộng; vì utility không âm, bỏ các giao dịch không chứa mở rộng không thể làm tăng tổng.
6. Chỉ cắt khi UB<ngưỡng. Xuất khi U>=ngưỡng là điều kiện riêng, không ngăn mở rộng khi U chưa đạt ngưỡng.

Các giả thiết trên không áp dụng trực tiếp cho utility âm. Kiểm tra hữu hạn bên dưới bổ trợ lập luận, không thay thế chứng minh.

## Kết quả kiểm tra Java

Seed 20261021:

- 9 ca cố định: mẫu SPMF, singleton utility cuối, singleton hết hiệu lực, bộ ba, RC=0, tái xuất hiện, dữ liệu rỗng, metadata/utility 0, utility vượt int.
- 500 database ngẫu nhiên, tối đa 7 item và 10 giao dịch; kiểm tra mọi prefix, tổng cộng **2.759 prefix**.
- Mỗi ca chạy RU bật và tắt, so cả itemset và utility với oracle: tổng **5.536 lần chạy miner** khớp.
- Tái sử dụng cùng một object qua các lần chạy để kiểm tra reset trạng thái.
- 5 input sai bị từ chối.
- Báo cáo: `results/local/safe-checks/report.txt`.
- Bộ incremental bổ sung ngày 23/10: 7.036 snapshot ở fixed/paper, RU bật/tắt, từ 500 chuỗi ngẫu nhiên và 4 chuỗi cố định; khớp oracle/cold-start. Kết quả chi tiết tại `results/local/incremental-checks/report.txt`. Lệnh run.cmd check hiện chạy cả hai bộ.

## Pilot: kiểm tra tác động cắt nhánh trên dữ liệu thu gọn

Lấy 200 giao dịch đầu của Foodmart/Mushroom, giữ 12 item phổ biến nhất trong prefix (hòa tần suất chọn ID nhỏ), tính lại TU từ các item được giữ, bỏ giao dịch rỗng. Chọn ngưỡng bằng ceil(5% tổng utility dữ liệu đã chiếu), maxR=100000 để không loại item vì recency. Đây là dữ liệu biến đổi phục vụ pilot, **không phải benchmark đầy đủ trên Foodmart/Mushroom**.

Mỗi cấu hình RU bật/tắt chạy ba lần trong JVM độc lập với -Xmx512m, thứ tự xáo trộn seed cố định. Cả 12 lượt khớp vét cạn trên dữ liệu đã chiếu. Không dùng bản gốc bỏ sót làm baseline hiệu năng.

| Dữ liệu đã chiếu | Giao dịch giữ lại | HUI | Join RU tắt | Join RU bật | Trung vị mine_ms tắt | Trung vị mine_ms bật |
|---|---:|---:|---:|---:|---:|---:|
| Foodmart prefix200/top12 | 38 | 9 | 44 | 40 | 2.897 | 3.013 |
| Mushroom prefix200/top12 | 200 | 4091 | 4083 | 4083 | 44.236 | 42.825 |

Foodmart tránh được 4 phép ghép nhưng không nhanh hơn ở trung vị lần đo này. Mushroom không cắt được nhánh nào ở cấu hình này; sai biệt thời gian không phải bằng chứng RU tăng tốc. Số mẫu ít, thời gian ngắn, JVM mới mỗi lượt không warm-up: chưa rút ra kết luận thống kê về tốc độ.

Thời gian đo bằng nanoTime: load gồm đọc/parse/tính TWU/lastSeen; prepare gồm lọc và tạo vertical maps; mine gồm mở/ghi/đóng output. Không bao gồm khởi động JVM. Chưa đo peak RAM nên không có tuyên bố tiết kiệm bộ nhớ.

Manifest chứa hash nguồn/input, item chọn và tham số; log và CSV nằm trong `results/local/safe-pilot/`. Để chạy lại: build bằng run.cmd help trước, rồi chạy `scripts/pilot_sum_safe.py` bằng Python. Script dùng JDK bundled của VS Code và timeout 30 giây/lượt; đây là pilot cho môi trường máy hiện tại.

## Nội dung có thể trình bày với thầy

“Chúng em đã tái hiện việc bỏ sót trên cài đặt SUM phân phối bởi SPMF và tạo một bản Java riêng dùng đường sinh duy nhất cùng cận trên an toàn. Bản sửa tìm được phản ví dụ và khớp oracle trên bộ kiểm tra hiện có. Đã bổ sung cập nhật lịch sử tăng dần và ngưỡng động, kiểm tra mỗi snapshot với oracle. Cấu trúc phục vụ khai phá vẫn được xây lại từ lịch sử trong RAM. Chưa có bằng chứng tăng tốc tổng quát; hướng tiếp theo là đánh giá trên dữ liệu lớn.”

Không tuyên bố SUM-Safe mới về học thuật, không tuyên bố đã hoàn tất toàn bộ SUM trong paper, không tuyên bố nhanh hơn chỉ từ demo phản ví dụ.
