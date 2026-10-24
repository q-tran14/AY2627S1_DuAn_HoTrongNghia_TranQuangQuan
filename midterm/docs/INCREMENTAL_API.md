# Cập nhật giữ trạng thái và ngưỡng động của SUM-Safe

Cập nhật ngày 23/10/2026. Bổ sung vào bản Java trong midterm; source SPMF gốc vẫn được giữ nguyên để đối chiếu.

## Chạy ngay bằng CMD

Mở CMD tại thư mục có run.cmd:

```bat
run.cmd incremental-demo
run.cmd check
```

PowerShell dùng `.\run.cmd` thay cho `run.cmd`. Không cần Set-ExecutionPolicy.

Demo dùng ba file trong datasets/incremental, được thêm lần lượt vào **cùng một object Java**. Hàng trước không được đọc lại để cập nhật; utility theo TID, tổng utility, TWU và lần xuất hiện cuối được giữ trong RAM.

| Batch mới | Dữ liệu mới | Utility tích lũy của {1,2,3} | Ngưỡng dùng | Minimum singleton sau batch |
|---|---|---:|---:|---:|
| 1 | 1 2 3:12:4 4 4 | 12 | 10 | 4 |
| 2 | 1 2 3:6:2 2 2 | 18 | 14 = 10+4 | 6 |
| 3 | 1 2 3 4:16:2 2 2 10 | 24 | 20 = 14+6 | 8 |

maxR=1, cả ba item 1/2/3 vẫn active ở mỗi bước. Item 4 mới ở batch 3 có utility 10, nhưng min trên toàn bộ singleton là 8. Kết quả mỗi batch có {1,2,3} với utility 12,18,24 tương ứng; demo đối chiếu oracle ở từng bước.

Mỗi output là **ảnh chụp tập HUI tại thời điểm hiện tại**, không nối danh sách HUI cũ vào kết quả mới. Khi threshold hoặc relevance thay đổi, HUI cũ có thể biến mất.

Output:

```text
results/local/incremental-demo/
  batch-01.txt
  batch-02.txt
  batch-03.txt
  batches.csv
```

batches.csv ghi input, số giao dịch mới/tích lũy, ngưỡng thực dùng, minimum singleton, số HUI/join và thời gian từng pha. Thời gian demo không dùng để kết luận tăng tốc.

## Chạy dữ liệu của bạn

Giữ ngưỡng cố định:

```bat
run.cmd incremental fixed results/local/fixed-session 10 1 datasets/incremental/batch-01.txt datasets/incremental/batch-02.txt datasets/incremental/batch-03.txt
```

Tăng ngưỡng theo quy tắc paper:

```bat
run.cmd incremental paper results/local/paper-session 10 1 datasets/incremental/batch-01.txt datasets/incremental/batch-02.txt datasets/incremental/batch-03.txt
```

Tham số: policy, thư mục output, ngưỡng ban đầu, maxR, danh sách batch. **Mỗi file phải chỉ chứa phần giao dịch mới**. Nếu đưa lại file toàn prefix, các giao dịch cũ sẽ bị tính thêm lần nữa. Script không tự đoán dữ liệu trùng.

Mỗi lệnh mở một phiên mới; các batch trong cùng lệnh chia sẻ trạng thái. Chạy lệnh khác hoặc đóng tiến trình thì trạng thái RAM không được lưu tự động ra đĩa. Giữ state qua batch không đồng nghĩa đã có checkpoint để mở lại phiên sau.

## Tùy chọn cắt nhánh khi chạy incremental

Thêm `--pruning false` trước danh sách batch để tắt RU; `--pruning true` để bật. Mặc định là `true`, nên các lệnh cũ vẫn hoạt động.

```bat
run.cmd incremental fixed results/local/no-prune 10 1 --pruning false datasets/incremental/batch-01.txt datasets/incremental/batch-02.txt
```

Tùy chọn chỉ thay đổi quyết định cắt nhánh. Output itemset và utility phải giữ nguyên ở cùng dữ liệu, ngưỡng và maxR. CSV mới ghi thêm cột `pruning`; các bản ghi lịch sử được giữ nguyên theo phiên bản đã tạo chúng.

## API Java

```java
AlgoSUMSafe miner = new AlgoSUMSafe();
miner.initialize(10, 1, true, AlgoSUMSafe.ThresholdPolicy.PAPER_MIN_ALL_SINGLETONS);
miner.appendBatch("datasets/incremental/batch-01.txt");
miner.mine("results/local/session/batch1.txt");
miner.appendBatch("datasets/incremental/batch-02.txt");
miner.mine("results/local/session/batch2.txt");
```

- initialize: tạo phiên mới, xóa trạng thái trước nếu có.
- appendBatch: đọc, kiểm tra và thêm riêng batch mới; TID tăng toàn cục qua các batch.
- mine: dùng trạng thái đã giữ để lọc, tạo cấu trúc khai phá và xuất snapshot HUI.
- getCurrentMinUtility: ngưỡng hiện tại; getMinimumSingletonUtility: minimum tổng utility singleton; getBatchCount và transactionCount: số batch không rỗng/giao dịch đã thêm.
- runAlgorithm cũ vẫn tương thích: khởi tạo phiên fixed, thêm một file và mine. Nó là cách chạy một lần, không giữ phiên giữa các lần gọi.

## Quy tắc ngưỡng và các lựa chọn được công bố

Paper mục 4.3 đưa công thức `MU_(k+1) = MU_k + TU_min(k)`. Bản này thực hiện:

1. Batch không rỗng đầu tiên dùng ngưỡng ban đầu.
2. Sau mine thành công, lấy minimum của **tổng utility cuối của tất cả singleton đã thấy**, gồm item inactive hoặc chưa qua TWU. Không lấy minimum trong tập HUI đầu ra hoặc giá trị utility trung gian.
3. Khi thêm batch không rỗng tiếp theo, cộng minimum của snapshot trước vào ngưỡng hiện tại.
4. Batch rỗng/metadata-only không làm tăng ngưỡng hoặc làm già counter. Gọi mine lại không tăng ngưỡng.
5. Với policy paper, phải mine thành công sau mỗi batch không rỗng trước khi thêm batch tiếp. Nếu mine lỗi, có thể sửa đường output rồi thử lại.

Đây là lựa chọn đặc tả rõ để hiện thực công thức, không tuyên bố giải quyết mọi chỗ mơ hồ của paper: phần văn bản/ví dụ có số liệu ngưỡng không nhất quán; paper chưa xác định hoàn toàn nhất quán phạm vi map dùng lấy minimum hoặc cách xử lý batch rỗng. Tên enum ghi rõ MIN_ALL_SINGLETONS. Việc chọn minimum của active item thay vì tất cả item là một chính sách khác, cần thực nghiệm riêng.

Relevance tiếp tục theo code SPMF đã kiểm tra: `RC(i,N)=maxR-(N-lastTid(i))`, giữ RC>=0. TWU và utility được tích lũy trên toàn bộ lịch sử, không chỉ cửa sổ. Item hết hiệu lực vẫn giữ lịch sử; khi tái xuất hiện, thông tin cũ được sử dụng đúng theo utility toàn lịch sử.

## Giữ gì và xây lại gì?

**Giữ qua batch:** map singleton -> TID/utility, tổng utility, TWU, lastTid, số giao dịch và trạng thái threshold. Không bỏ vĩnh viễn item chỉ vì nó chưa đạt TWU hoặc đã hết hiệu lực; chúng có thể cần lại sau.

**Xây lại khi mine:** tập item active/đạt TWU, thứ tự ID, remaining utility, các BitSet phục vụ khai phá và map ghép của tiền tố. Việc xây lại này dùng lịch sử trong RAM, không quét lại file cũ. Cách này tránh cache join/cận trên bị lỗi thời khi xuất hiện item mới hoặc ngưỡng thay đổi.

Đây là cập nhật dữ liệu/cấu trúc nền tăng dần, rồi khai phá lại trên cấu trúc đã cập nhật; chưa phải cập nhật chỉ các HUI thay đổi hoặc tái sử dụng mọi map ghép. Chi phí bộ nhớ tăng theo lịch sử TID giữ lại. Chưa có bằng chứng nhanh hơn khai phá lại toàn bộ trên mọi dữ liệu.

## Kiểm chứng

Lệnh `run.cmd check` chạy cả bộ batch cũ và bộ incremental mới. Chỉ chạy phần mới:

```bat
run.cmd incremental-check
```

Bộ mới có 4 chuỗi cố định và 500 chuỗi batch ngẫu nhiên, seed 20261023, cả fixed/paper và RU bật/tắt. Mỗi snapshot:

- Ngưỡng kỳ vọng được tính độc lập từ tổng singleton của toàn prefix.
- Output khớp itemset/utility với vét cạn và cold-start ở đúng ngưỡng đó.
- Input batch bị xóa ngay sau append để phát hiện việc mine phụ thuộc đọc lại file.
- Mine lặp lại không đổi threshold hoặc kết quả.

Ca riêng kiểm tra item mới, hết hiệu lực/tái xuất hiện, RC=0, utility 0, batch rỗng, append lỗi không thay đổi lịch sử, output bị chặn không ghi đè input, overflow ngưỡng/utility, reset phiên. Append validate xong mới commit, tránh thêm nửa batch hợp lệ khi dòng sau lỗi; không cam kết phục hồi khi tiến trình bị tắt hoặc hệ thống hết bộ nhớ.

## Ghi vào báo cáo

“Đã bổ sung phiên cập nhật giữ trạng thái trong bộ nhớ và ngưỡng động theo công thức mục 4.3 với đặc tả minimum trên tất cả singleton. Kết quả sau mỗi batch được đối chiếu oracle. Cấu trúc phục vụ khai phá được xây lại từ lịch sử đã giữ; chưa có checkpoint và chưa đánh giá tăng tốc tổng quát.”
