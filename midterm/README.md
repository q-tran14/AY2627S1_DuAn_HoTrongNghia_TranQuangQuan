# Bộ chương trình giữa kỳ: kiểm chứng SUM và bản thử nghiệm SUM-Safe

Đề tài: **Research and Development of Solutions for Mining High-Utility Itemsets from Incremental Datasets**.

Sinh viên: Hồ Trọng Nghĩa – 520K0163; Trần Quang Quân – 520H0569. Giảng viên: Nguyễn Chí Thiện.

Bộ này tập trung vào kiểm chứng cơ chế khai phá của cài đặt SUM phân phối bởi SPMF và bản Java thử nghiệm SUM-Safe. Đã có mining kernel theo utility không âm, relevance của singleton, cập nhật giữ lịch sử qua các batch trong cùng tiến trình và chính sách tăng ngưỡng theo cách diễn giải công thức mục 4.3 của paper. Cách chọn minimum và giới hạn được ghi trong docs/INCREMENTAL_API.md; chưa có lưu/khôi phục phiên ra đĩa.

## Chạy bằng CMD hoặc PowerShell

Giải nén ZIP trước khi chạy. Cần **JDK 25**, không chỉ JRE. Không cần Maven, Gradle, mạng, PowerShell script hay Set-ExecutionPolicy.

**Cách mở CMD đúng thư mục sau khi giải nén:**

1. Mở thư mục `midterm` bằng File Explorer. Đây là thư mục có file `run.cmd` và `README.md` này.
2. Nhấn vào thanh địa chỉ của File Explorer, nhập `cmd` rồi nhấn Enter.
3. Trong cửa sổ CMD vừa mở, chạy:

```bat
run.cmd demo
run.cmd check
run.cmd incremental-demo
```

Nếu dùng PowerShell, mở terminal tại chính thư mục có file `run.cmd`, rồi chạy:

```powershell
.\run.cmd demo
.\run.cmd check
.\run.cmd incremental-demo
```

Thư mục có thể nằm ở bất kỳ ổ đĩa hoặc vị trí nào. run.cmd tự xác định vị trí của nó và dùng thư mục đó làm gốc cho các đường dẫn tương đối; không cần sửa source theo đường dẫn máy người chạy. Mỗi lần gọi run.cmd, script tạo build/classes nếu cần, biên dịch source rồi thực hiện lệnh đã chọn.

run.cmd tìm JDK qua SUM_JDK, JAVA_HOME, PATH, rồi JDK đi kèm extension Java của VS Code. Nếu không tìm thấy, script dừng và báo lỗi. Khi đó:

- Nếu máy chưa có JDK, cài JDK 25.
- Nếu máy đã có JDK nhưng script không tìm thấy, đặt SUM_JDK bằng đường dẫn thực tế đến thư mục JDK có cả bin/javac.exe và bin/java.exe.

Ví dụ cú pháp trong CMD, **phải thay DUONG_DAN_THUC_TE_DEN_THU_MUC_JDK bằng vị trí JDK trên máy đang chạy**:

```bat
set "SUM_JDK=DUONG_DAN_THUC_TE_DEN_THU_MUC_JDK"
run.cmd demo
```

Thiết lập set chỉ áp dụng trong cửa sổ CMD hiện tại. Vị trí cài JDK và vị trí đặt dự án có thể ở hai ổ đĩa khác nhau. Script biên dịch với --release 25, heap chạy giới hạn 512 MB.

## Các lệnh

| Lệnh | Mục đích |
|---|---|
| run.cmd demo | Source SPMF nguyên gốc → phản ví dụ → SUM-Safe → oracle |
| run.cmd demo 8 | Cùng input, đổi ngưỡng xuống 8 để đối chiếu |
| run.cmd check | Cả bộ kiểm tra batch và cập nhật giữ trạng thái |
| run.cmd incremental-demo | Thêm 3 batch, ngưỡng động 10 → 14 → 20, đối chiếu oracle |
| run.cmd incremental-check | Kiểm tra state, threshold, reinduction và cập nhật lỗi |
| run.cmd incremental fixed results/local/session 10 1 datasets/incremental/batch-01.txt datasets/incremental/batch-02.txt | Thêm lần lượt hai file với ngưỡng cố định |
| run.cmd incremental paper results/local/session-paper 10 1 datasets/incremental/batch-01.txt datasets/incremental/batch-02.txt | Thêm lần lượt hai file với ngưỡng động |
| run.cmd original-check | Tái hiện các sai khác đã biết trên bản local của SUM; exit 1 là dự kiến |
| run.cmd run safe datasets/DB_Utility.txt results/local/safe.txt 30 10 | Chạy bản sửa |
| run.cmd run safe datasets/DB_Utility.txt results/local/no-prune.txt 30 10 false | Bản sửa, tắt quyết định cắt RU |
| run.cmd run original datasets/DB_Utility.txt results/local/original.txt 30 10 | Chạy source SPMF chính thức |
| run.cmd pack | Tạo một file ZIP thông thường: dist/SUM_midterm.zip |

Trong PowerShell thêm `.\` trước `run.cmd`. Nếu tên input/output có dấu cách, đặt trong dấu nháy kép. Các tham số run lần lượt là phiên bản, input, output, minUtility, maxR và tùy chọn cắt RU của bản safe.

## Tùy chỉnh dữ liệu và tham số

Các lệnh demo dùng cấu hình có sẵn để minh họa. Dùng `run` hoặc `incremental` khi muốn chọn dữ liệu và tham số; không cần sửa source Java.

| Tham số | Có thể chọn | Ý nghĩa |
|---|---|---|
| `safe` / `original` | Bản nhóm viết / source SUM gốc | Chọn phương pháp chạy một lần |
| `input`, `output` | Đường dẫn tương đối hoặc tuyệt đối | Đổi dataset và nơi ghi tập HUI |
| `minUtility` | Số nguyên dương | Ngưỡng utility tuyệt đối, không phải phần trăm |
| `maxR` | Số nguyên không âm | Số giao dịch vắng mặt tối đa kể từ lần xuất hiện cuối của từng item |
| RU pruning | `true` hoặc `false`, mặc định `true` | Bật/tắt quyết định cắt nhánh bằng remaining utility của SUM-Safe |
| Policy incremental | `fixed` hoặc `paper` | Giữ ngưỡng hoặc tăng theo MIN_ALL_SINGLETONS đã mô tả |
| Danh sách batch | Một hoặc nhiều file, theo thứ tự | Mỗi file chỉ chứa các giao dịch mới |

Chạy một lần, chọn ngưỡng 30, maxR 10, tắt RU; đường dẫn output có dấu cách:

```bat
run.cmd run safe datasets/DB_Utility.txt "results/local/my run.txt" 30 10 false
```

Chạy cập nhật, giữ ngưỡng 10, maxR 1, tắt RU:

```bat
run.cmd incremental fixed results/local/custom-fixed 10 1 --pruning false datasets/incremental/batch-01.txt datasets/incremental/batch-02.txt
```

Chạy cùng dữ liệu với ngưỡng động và RU bật:

```bat
run.cmd incremental paper results/local/custom-paper 10 1 --pruning true datasets/incremental/batch-01.txt datasets/incremental/batch-02.txt
```

`--pruning` phải đặt trước danh sách file batch. Nếu bỏ tùy chọn này, RU được bật như các lệnh cũ. CSV `batches.csv` ghi ngưỡng thực dùng và cột `pruning`. Thay đổi thứ tự hoặc nội dung batch sẽ thay đổi lịch sử và có thể thay đổi kết quả.

Các tham số còn lại dùng **theo vị trí** đúng cú pháp; hiện chưa có các cờ `--input`, `--min-utility`, menu tương tác hoặc file cấu hình JSON. `maxR=0` giữ những item có mặt trong giao dịch cuối. Mỗi lệnh incremental tạo một phiên RAM mới; các batch trong cùng lệnh chia sẻ lịch sử.

Xem trợ giúp bằng `run.cmd help`, `run.cmd --help` hoặc `run.cmd -h`. Trong PowerShell thêm `.\` trước `run.cmd`. Tham số sai được báo bằng `ERROR` và mã thoát 2; `original-check` dùng mã 1 để thể hiện các sai khác đã biết của baseline.

Phân tích số liệu pilot đã lưu (Python 3, không cần cài thư viện ngoài):

```bat
python scripts/analyze_results.py --output-dir results/local/analysis
```

Đây là phân tích lại CSV lịch sử, không phải chạy benchmark mới. `--output-dir` nhận đường dẫn tương đối tính từ midterm hoặc đường dẫn tuyệt đối.

Giới hạn tùy chỉnh hiện tại: heap 512 MB được đặt trong `run.cmd`; seed và số database của bộ kiểm chứng được đặt trong source test. Script pilot hiện cố định prefix 200 giao dịch, top 12 item, ngưỡng 5% và ba lượt đo; các giá trị này chưa được đưa thành option CLI. Kích thước mỗi batch do bạn chuẩn bị trong các file đầu vào.

Ví dụ hai giao dịch giải thích trong báo cáo có thể chạy bằng:

```bat
run.cmd run original datasets/examples/two-transactions.txt results/local/two-original.txt 8 10
run.cmd run safe datasets/examples/two-transactions.txt results/local/two-safe.txt 8 10
```

Cả hai bản trả `1 3 #UTIL: 9`. Đây là ví dụ minh họa cách lập map và ghép itemset; phản ví dụ bỏ sót vẫn dùng file triple-only-high trong lệnh demo.

Dữ liệu input theo định dạng SPMF: `itemIDs:transactionUtility:itemUtilities`. ID là số nguyên dương, không trùng trong một giao dịch; utility không âm, số lượng utility khớp số item, và transactionUtility bằng tổng itemUtilities.

## Kết quả mong đợi

Demo ngưỡng 10: input `1 2 3:12:4 4 4`, oracle tìm {1,2,3}:12; source gốc bỏ sót; Safe tìm đúng. Demo thoát thành công khi bản sửa khớp oracle, đồng thời vẫn in MISMATCH của bản gốc.

Check: 9 ca cố định + 2.759 prefix từ 500 database, cả RU bật/tắt khớp itemset và utility; 5 input sai bị từ chối. Kiểm tra hữu hạn bổ trợ lập luận đúng, không chứng minh hiệu năng tổng quát.

Check còn kiểm tra riêng API giữ trạng thái: 7.036 snapshot từ 500 chuỗi ngẫu nhiên và 4 chuỗi cố định, xét fixed/paper và RU bật/tắt. Input bị xóa sau append; mine vẫn khớp vét cạn và chạy lại toàn prefix tại cùng ngưỡng. Khi chạy incremental, mỗi file phải chỉ chứa phần dữ liệu mới; các batch trong một lệnh dùng chung state trong RAM.

## Cấu trúc

```text
midterm/
  run.cmd                 Điểm chạy chung cho CMD / PowerShell
  sources.txt             Danh sách source cần biên dịch
  src/                    SUM local, SUM-Safe, oracle, runner và demo
  vendor/spmf/            Ba file source gốc từ SPMF, giữ package/copyright
  vendor/SOURCE.json      URL, ngày tải và SHA-256
  LICENSE_SPMF_GPL3.txt   Giấy phép SPMF
  datasets/               Mẫu, phản ví dụ, Foodmart và Mushroom dùng trong pilot
  docs/                   Ghi chú kỹ thuật và hướng dẫn viết báo cáo
  evidence/               Log / CSV / manifest đã kiểm chứng, có phân biệt ngày chạy
  scripts/                Pilot Python tùy chọn; demo và check không cần Python
  build/                  Tự sinh khi chạy, không đóng gói
  results/local/          Kết quả mỗi lần chạy, không tự đóng gói
  dist/                   ZIP tạo bằng lệnh pack
```

Sau khi có kết quả mới, chỉ chọn bằng chứng muốn nộp và chép vào evidence với ngày, cấu hình tương ứng, rồi chạy pack. Pack đóng gói theo danh sách cho phép; không lấy file cá nhân, .git, build hay log chưa chọn trong results/local. ZIP chứa đầy đủ source chính thức dùng demo, không phụ thuộc tmp hoặc thư mục bên ngoài. Thư mục report là tùy chọn đối với lệnh pack. Báo cáo đang chỉnh trực tiếp ở project LaTeX riêng; trước khi nộp, chép bản nguồn và PDF mới vào report hoặc gửi kèm báo cáo riêng theo yêu cầu thầy. Pack không tự lấy báo cáo từ ngoài midterm.

## Báo cáo và nguồn

- Đọc docs/REPORT_GUIDE.md để phân biệt nội dung paper, cài đặt, đóng góp và giới hạn.
- docs/INCREMENTAL_API.md mô tả API mới, ví dụ ba batch, ngưỡng động và kiểm chứng.
- docs/SUM_SAFE_JAVA.md giải thích code, lập luận, test và pilot. Phần thống kê ngày 21/10 là kết quả trước đây, không gán thành số liệu đo mới.
- Paper: Sra & Chand, DOI https://doi.org/10.1007/s41019-023-00229-4.
- Tài liệu SPMF: https://www.philippe-fournier-viger.com/spmf/ScentedMiner_utility.php.
- Source SPMF có bản quyền và GPL; giữ nguyên header. SUM-Safe là tên thử nghiệm, không tuyên bố là thuật toán mới đã được công bố.

Bộ này chứa chương trình và bằng chứng giữa kỳ. Trước khi nộp cần đồng bộ bản báo cáo hiện tại, nguồn code, tài liệu hướng dẫn và các kết quả được trích dẫn.
