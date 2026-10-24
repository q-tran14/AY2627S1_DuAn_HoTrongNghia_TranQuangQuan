# Nội dung slide và kịch bản: kiểm chứng và cải tiến SUM

**Cập nhật 21/10/2026:** đã có bản Java SUM-Safe, kiểm tra RU bật/tắt khớp 9 ca và 2.759 prefix từ 500 database ngẫu nhiên. Demo mới: `.\demo-counterexample.ps1 -Official -CompareSafe`. Pilot 12 JVM trên dữ liệu chiếu khớp oracle nhưng chưa chứng minh tăng tốc. Nội dung bên dưới là kịch bản đề xuất ngày 20/10; các câu “chưa có bản Java” ở slide 7/9 và checklist cuối đã được thay thế bởi trạng thái trong `docs/SUM_SAFE_JAVA.md`. Khi làm deck cuối dùng trạng thái 21/10, vẫn giữ giới hạn chưa có incremental API và chưa có benchmark đầy đủ.

Ngày: 20/10/2026. Bản nội dung để trình bày tiến độ và đề xuất; chưa phải file PowerPoint. Thời lượng mục tiêu 10–12 phút gồm demo. Giữ nguyên tên đề tài đăng ký. Tiêu đề buổi báo cáo: **Kiểm chứng tính đầy đủ và đề xuất cải tiến cơ chế duyệt, cắt nhánh của SUM**.

Thông điệp: đã tìm thấy một phản ví dụ trên bản SPMF công bố; xác định nguyên nhân ở cơ chế mở rộng; đề xuất cận trên an toàn, đã kiểm chứng nguyên mẫu nhưng chưa hoàn tất bản Java và benchmark. Không nói đã hoàn thành toàn bộ giải pháp.

## Chuẩn bị demo để tự kiểm chứng

Bạn đang ở thư mục scripts. Chạy:

```powershell
.\demo-counterexample.ps1 -Official
```

Lệnh này biên dịch source SPMF chính thức đã tải trong phiên audit, không sửa thuật toán; dùng harness riêng để truyền tham số. Không tải mạng khi chạy. Nếu bản source trong `tmp/audit/official` bị xóa, lệnh báo thiếu nguồn, không tự chuyển sang local.

Để chạy bản local:

```powershell
.\demo-counterexample.ps1
```

Để đối chiếu ngưỡng 8 trên bản chính thức:

```powershell
.\demo-counterexample.ps1 -Official -MinUtility 8
```

Nếu ở thư mục gốc dự án, dùng `.\scripts\demo-counterexample.ps1` thay cho `.\demo-counterexample.ps1`. Nếu PowerShell chặn script, dùng thiết lập chỉ trong cửa sổ hiện tại đã áp dụng trước đó:

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
```

Input có thể mở trực tiếp: `datasets/counterexamples/triple-only-high.txt`:

```text
1 2 3:12:4 4 4
```

Demo in toàn bộ 7 tập con và utility tính từ input, sau đó chạy SUM và đọc file output. Không thay thế kết quả SUM bằng kết quả mong đợi. Lệnh demo không báo exception khi tìm thấy sai khác vì mục đích là trình diễn; bộ regression `run-sum.ps1 -Check` vẫn báo thất bại nếu có sai khác.

Kết quả đã tái hiện trên bản chính thức:

| Ngưỡng | HUI theo tính tay | HUI SUM |
|---|---|---|
| 10 | {1,2,3}:12 | Rỗng; join count=0 |
| 8 | Ba cặp utility 8 và bộ ba utility 12 | Khớp cả bốn tập và utility |

Output thô nằm tại `results/local/demo/official-threshold-10.txt` và `official-threshold-8.txt`. Bản chính thức có dòng metadata; “rỗng” nghĩa không có dòng itemset, không phải file 0 byte. Không dùng thời gian chạy vài ms để kết luận hiệu năng.

## Slide 1 — Phạm vi nghiên cứu lần này (35 giây)

**Nội dung trên slide**

- Đề tài: Research and Development of Solutions for Mining High-Utility Itemsets from Incremental Datasets.
- Trọng tâm: kiểm chứng tính đầy đủ của SUM và đề xuất sửa cơ chế duyệt/cắt nhánh.
- Nguồn: paper Sra & Chand và cài đặt phân phối bởi SPMF.

**Lời trình bày**

“Trong lần báo cáo này, chúng em tập trung vào một thành phần của SUM là cách sinh và cắt nhánh các tập mục. Mục tiêu đầu tiên là kiểm tra cài đặt có tìm đủ kết quả theo định nghĩa bài toán hay không. Sau đó, chúng em đề xuất cách điều chỉnh và kế hoạch đánh giá hiệu quả.”

## Slide 2 — Bài toán trên một giao dịch cụ thể (60 giây)

**Hình/bảng trên slide**

Một hóa đơn T1 mua ba mặt hàng A, B, C; mỗi mặt hàng có utility bằng 4. Giả sử utility là lợi nhuận, đã tính bằng số lượng nhân lợi nhuận đơn vị. Tổng utility hóa đơn bằng 12. Ngưỡng tối thiểu bằng 10.

| Giao dịch | A | B | C | Tổng |
|---|---:|---:|---:|---:|
| T1 | 4 | 4 | 4 | 12 |

Chú thích: một dòng = một giao dịch; item = loại hàng; itemset = tập loại hàng. A/B/C tương ứng mã 1/2/3 trong file.

**Lời trình bày**

“Để thống nhất bối cảnh, đây là một hóa đơn mua ba loại hàng. Mỗi số 4 là utility của mặt hàng trong hóa đơn, không phải mã item hay số dòng. Với ngưỡng 10, yêu cầu là tìm tất cả các nhóm hàng có tổng utility đạt ngưỡng. Khi có nhiều giao dịch, chỉ cộng utility tại những giao dịch chứa đủ nhóm hàng đang xét.”

## Slide 3 — Tập con chưa đạt ngưỡng vẫn có thể tạo tập lớn đạt ngưỡng (50 giây)

**Nội dung trên slide**

| Itemset | Utility | Đạt ngưỡng 10? |
|---|---:|---|
| {A}, {B}, {C} | 4 mỗi tập | Không |
| {A,B}, {A,C}, {B,C} | 8 mỗi tập | Không |
| {A,B,C} | 12 | Có |

Kết quả đúng duy nhất: {A,B,C}:12.

**Lời trình bày**

“Ví dụ này cho thấy một điểm quan trọng của HUIM: việc một tập chưa đạt ngưỡng không cho phép kết luận mọi tập lớn hơn chứa nó đều không đạt. Cả ba cặp đều chỉ có utility 8, nhưng bộ ba có utility 12. Vì vậy, chiến lược cắt nhánh phải tính đến phần utility có thể bổ sung.”

## Slide 4 — Thành phần SUM được kiểm tra (60 giây)

**Nội dung trên slide**

Giao dịch → residue map và TID → chọn item theo TWU/reinduction → sắp xếp → ghép và mở rộng → HUI.

- Residue map lưu utility theo giao dịch và tổng utility.
- BitSet intersection xác định giao dịch chứa đồng thời các tập ghép.
- Reinduction chọn item còn hiệu lực.
- Kiểm tra tập trung ở phép ghép và đường mở rộng đệ quy.

**Lời trình bày**

“SUM tổ chức thông tin utility trong residue map và dùng các giao dịch chung để tính utility sau khi ghép. Trong ví dụ đang xét, mỗi item có TWU bằng 12 nên đều vượt bước lọc TWU. Cả ba vừa xuất hiện nên counter đều bằng 10 và không bị loại vì độ mới. Như vậy, nếu bộ ba bị bỏ sót thì cần xem phần khai phá, không thể giải thích bằng reinduction.”

## Slide 5 — Demo trên bản SPMF công bố (90 giây)

**Nội dung trên slide**

- Input: `1 2 3:12:4 4 4`.
- minUtility=10, maxR=10.
- Mong đợi: {1,2,3}:12.
- Quan sát: 0 HUI, 0 join.
- Source tải ngày 20/10/2026; bản gốc không sửa logic.

**Lời trình bày và thao tác**

“Em mở file input để xác nhận đúng một giao dịch, rồi chạy bản chính thức. Phần đầu demo liệt kê mọi tập con để chúng ta kiểm tra độc lập. Phần sau là thống kê và output thật của SUM. Kết quả không chứa bộ ba dù utility 12 đạt ngưỡng 10.”

Chạy `.\demo-counterexample.ps1 -Official`. Chỉ vào EXPECTED, ACTUAL và Missing. Nếu terminal lỗi môi trường, dùng output đã lưu và nói rõ đó là kết quả lần chạy trước, không giả vờ demo đang chạy thành công.

## Slide 6 — Nguyên nhân ở điều kiện mở rộng (75 giây)

**Nội dung trên slide**

Code trong `exploreAndJoinItemsets`:

```java
if (sortedMap.get(key).totUtil >= minUtility - inputMap.totUtil) {
    // computeBitJoin, then recursiveJoinAndEvaluate
} else {
    break;
}
```

Thay số: `4 >= 10 - 4` → sai. Không sinh cặp → không mở rộng tới bộ ba.

**Lời trình bày**

“Điều kiện này cho phép bỏ qua việc tính một cặp không thể đạt ngưỡng. Tuy nhiên, trong cài đặt, đệ quy mở rộng cũng nằm bên trong nhánh điều kiện đó. Khi cặp không được tạo, đường tới bộ ba bị mất. Điểm cần sửa là phân biệt điều kiện xuất một tập với điều kiện dừng mọi mở rộng của tập.”

Đối chiếu phụ nếu còn thời gian: chạy cùng input với ngưỡng 8. Khi `4 >= 8-4` đúng, chương trình tạo cặp và tìm được bộ ba. Đây là bằng chứng bổ trợ về đường chạy, không thay thế phân tích code.

## Slide 7 — Giải pháp đề xuất: duyệt đầy đủ và cắt nhánh an toàn (80 giây)

**Nội dung trên slide**

- Duyệt theo thứ tự A<B<C; mỗi tập có một đường sinh.
- `U(X) >= ngưỡng` → xuất X.
- `UB(X) < ngưỡng` → dừng toàn bộ nhánh.
- UB bao gồm utility hiện tại và phần còn có thể bổ sung.

Ví dụ đường tìm kiếm dự kiến:

```text
{A}: U=4, UB=12 → mở rộng
{A,B}: U=8, UB=12 → mở rộng
{A,B,C}: U=12 → xuất
```

**Lời trình bày**

“Chúng em đề xuất giữ cách tính utility qua các giao dịch chung, nhưng tổ chức lại đường mở rộng theo thứ tự duy nhất. Một tập chưa đủ utility vẫn được mở rộng nếu cận trên cho biết nó còn khả năng đạt ngưỡng. Ở đây, cặp A,B có utility 8 nhưng còn C đóng góp 4, nên chưa được phép cắt nhánh.”

Ghi rõ trên slide: “Đề xuất; nguyên mẫu độc lập đã kiểm chứng, chưa tích hợp hoàn tất Java SUM”. Không trình bày đường này như log của SUM gốc.

## Slide 8 — Vì sao cận trên không bỏ sót HUI? (60 giây)

**Nội dung trên slide**

Với utility không âm:

`UB(X) = Σ[T chứa X] (u(X,T) + RU(X,T))`

RU chỉ cộng các item còn được phép thêm theo thứ tự duyệt.

Mọi mở rộng Y trên nhánh có `U(Y) <= UB(X)`. Vì vậy `UB(X) < ngưỡng` thì cả nhánh không có HUI.

**Lời trình bày**

“Một tập mở rộng chỉ xuất hiện trong các giao dịch đã chứa tiền tố. Trong từng giao dịch đó, phần utility thêm vào không thể vượt tổng utility của các item còn được phép thêm. Do utility không âm, cộng các giá trị này cho ta cận trên an toàn. Cận remaining utility đã có trong HUIM; phần chúng em nghiên cứu là tổ chức lại việc duyệt trên SUM và đánh giá tác động, không nhận đây là nguyên lý mới.”

## Slide 9 — Kết quả đã có và phạm vi chưa hoàn tất (60 giây)

**Nội dung trên slide**

| Đã làm | Chưa hoàn tất |
|---|---|
| Đối chiếu source local và source SPMF | Bản Java tích hợp giải pháp |
| Tái hiện phản ví dụ trên source chính thức | Kiểm thử mở rộng trên bản Java |
| Nguyên mẫu độc lập khớp 9 ca và 500 dữ liệu nhỏ ngẫu nhiên | Benchmark runtime, memory trên dữ liệu lớn |

**Lời trình bày**

“Kết quả hiện tại xác nhận vấn đề và kiểm tra tính khả thi của ý tưởng sửa. 500 ca ngẫu nhiên là kiểm tra nguyên mẫu giải pháp bằng Python với oracle, không phải 500 ca chạy bản Java mới. Chúng em chưa kết luận tăng tốc. Trước khi so tốc độ, các phiên bản phải trả về cùng kết quả đúng.”

## Slide 10 — Phạm vi đóng góp đề xuất (50 giây)

**Nội dung trên slide**

- Kiểm chứng: phản ví dụ, đối chiếu paper–code, bộ kiểm tra độc lập.
- Thuật toán: tổ chức lại duyệt/cắt nhánh, lập luận tính đúng.
- Thực nghiệm: so bản duyệt đúng không cắt với bản có cận trên; khảo sát join, thời gian, bộ nhớ và tham số.
- Bối cảnh incremental: kiểm tra trên từng prefix/batch; tách việc duy trì trạng thái khỏi mining kernel.

**Lời trình bày**

“Chúng em đề xuất lấy cơ chế duyệt và cắt nhánh làm phạm vi chính. Đầu tiên bảo đảm kết quả đầy đủ, sau đó đo tác động của cận trên và các tối ưu thành phần. Đối với dữ liệu tăng dần, chúng em sẽ kiểm tra kết quả ở mỗi lần cập nhật. Chúng em xin ý kiến thầy về phạm vi này trước khi mở rộng thêm các thành phần.”

## Điều kiện trước buổi trình bày theo yêu cầu có code giải pháp

Bộ nội dung hiện tại phù hợp để trình bày phát hiện và xin chốt hướng. Vì yêu cầu của thầy còn có code mới đã áp dụng, trước buổi báo cáo chính cần bổ sung:

1. Bản Java riêng tích hợp duyệt có thứ tự và cận trên; giữ baseline gốc để đối chiếu.
2. Demo cùng input: baseline bỏ sót, bản sửa trả {1,2,3}:12, và cả hai hiển thị rõ tên phiên bản.
3. Kiểm tra toàn bộ bộ ca và dữ liệu ngẫu nhiên trên chính bản Java sửa; không dùng kiểm tra Python thay thế.
4. Một pilot trên dữ liệu thực, so với baseline đúng cùng nhiệm vụ; nếu chưa có, ghi rõ chưa có kết luận hiệu năng.

Không sử dụng bản nội dung này để nói yêu cầu code Java cải tiến đã hoàn thành.

## Câu hỏi thầy có thể đặt và câu trả lời

**“Có phải dữ liệu sai định dạng không?”**
Input đúng ba trường SPMF; item duy nhất trong giao dịch, utility đều dương; 4+4+4=12 bằng TU. Có thể kiểm tra trực tiếp file.

**“Có phải W quá nhỏ nên bị loại?”**
Không trong phản ví dụ này: chỉ một giao dịch, mọi item vừa xuất hiện, RC=10. TWU của mỗi item là 12, cũng đạt ngưỡng 10.

**“Một giao dịch có liên quan đề tài incremental không?”**
Đây là kiểm tra tính đúng của mining kernel trên trường hợp đầu vào hợp lệ, có thể là prefix đầu tiên. Nó cô lập nguyên nhân. Đánh giá cập nhật tăng dần cần làm thêm, không suy ra từ ca này.

**“Vậy bài báo sai hoàn toàn?”**
Không kết luận như vậy. Đã xác minh bản source phân phối hiện tại bỏ sót ở ca cụ thể. Chưa có code thực nghiệm gốc năm 2023 để khẳng định mọi kết quả trong paper sai.

**“Remaining utility có gì mới?”**
Đó là kỹ thuật đã có. Đóng góp dự kiến là kiểm chứng và tổ chức lại cơ chế khai phá của SUM, lập luận đúng, cài đặt và đánh giá từng thành phần. Mức độ đóng góp cần phù hợp phạm vi đề tài được duyệt.

**“Chỉ bỏ if là xong?”**
Chưa đủ để khẳng định đúng. Cần kiểm tra thứ tự mở rộng, tính bao phủ, tránh trùng và điều kiện cắt toàn nhánh. Bản sửa phải qua oracle và ca ngẫu nhiên.

**“Bản sửa chậm hơn thì sao?”**
Không so tốc độ như cùng nhiệm vụ khi bản gốc bỏ sót. Trước hết báo độ đầy đủ, sau đó đo chi phí giữa các bản đều đúng, bật/tắt cận trên. Chưa hứa nhanh hơn.

## Nguồn đưa cuối slide hoặc phụ lục

- Paper chính của Sra & Chand: https://doi.org/10.1007/s41019-023-00229-4.
- Tài liệu SPMF: https://www.philippe-fournier-viger.com/spmf/ScentedMiner_utility.php.
- ZIP nguồn SPMF tải 20/10/2026, SHA-256 và đối chiếu trong `docs/PHAN_TICH_PAPER_SUM_VA_GIAI_PHAP.md`.
- Phản ví dụ và demo: `datasets/counterexamples/triple-only-high.txt`, `scripts/demo-counterexample.ps1`.
- Lỗi số học Fig.5/6, ngưỡng động và singleton đặt ở phụ lục nếu thầy hỏi; không đưa tất cả vào phần chính 10–12 phút.
