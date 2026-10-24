# Kế hoạch hoàn thành đề tài SUM

Ngày lập: 19/10/2026. Cập nhật: 20/10/2026. Hạn nộp: 23:59 ngày 21/11/2026, giờ Việt Nam.
Các ngày gặp thầy bên dưới là mốc đề xuất, chưa phải lịch đã xác nhận. Có thể gặp thầy thứ Ba hoặc thứ Năm; chưa xác định quỹ giờ làm việc cá nhân.

Quyết định ngày 20/10: lập kế hoạch cho một người thực hiện, không phụ thuộc teammate. Xây dựng lại chuỗi kiểm chứng, code cải tiến và thực nghiệm. Code và số liệu cũ được giữ nguyên để tham khảo, chưa được sử dụng làm bằng chứng cho báo cáo mới. Không phải chờ bàn giao code, log hoặc phân công của teammate để bắt đầu.

## 1. Phạm vi đề xuất

Ưu tiên được xác nhận ngày 20/10: chạy được source, hiểu và sửa được source code trước; chỉ xây dựng nội dung slide và kịch bản sau bước này. Xem `docs/CHAY_VA_DOC_SOURCE.md` cho tiến độ chạy và các ca kiểm tra thực tế. Không coi lastSeen là bước sửa đầu tiên khi tính đúng còn sai khác.

Giữ tên đề tài đã đăng ký. Nội dung nghiên cứu tập trung vào hiểu, kiểm chứng và tối ưu thành phần của Scented Utility Miner (SUM), kết hợp thực nghiệm đánh giá miền hoạt động. Dữ liệu tăng dần là bối cảnh của bài toán, không thay thế việc giải thích bài toán HUIM và cơ chế khai phá.

Chưa có hướng cải tiến nào được giảng viên duyệt. Không coi Adaptive-SUM là hướng đã chốt. Không cam kết một tối ưu sẽ nhanh hơn trước khi đo.

Đóng góp dự kiến: (1) đối chiếu paper và cài đặt, (2) tối ưu có kiểm soát một thành phần, (3) thực nghiệm bật/tắt thành phần và phân tích theo đặc điểm dữ liệu. Chưa ưu tiên giao diện, bộ dữ liệu mới hay nhiều thuật toán mới cùng lúc.

## 2. Bắt đầu bài trình bày bằng một ví dụ xuyên suốt

Giả sử utility là lợi nhuận, đã bằng số lượng nhân lợi nhuận mỗi đơn vị:

| Giao dịch | Mặt hàng và utility | Tổng utility |
|---|---|---:|
| T1 | bánh: 6, sữa: 4 | 10 |
| T2 | bánh: 3, trứng: 2 | 5 |
| T3 | bánh: 6, sữa: 8 | 14 |

- Một dòng là một giao dịch mua hàng; item là một loại hàng; itemset là một tập loại hàng, không phải tập dòng.
- X = {bánh, sữa} xuất hiện trong T1 và T3. Utility của X là (6+4)+(6+8)=24. Với ngưỡng 20, X là HUI.
- {bánh} có utility 15 và {sữa} có utility 12, đều dưới ngưỡng 20. Vì vậy không thể loại một itemset chỉ vì các tập con chưa đạt ngưỡng.
- TWU(X) là tổng utility của các giao dịch chứa X. Với utility không âm, TWU là cận trên để cắt nhánh an toàn; phải giải thích giả thiết này.
- Sau khi hiểu HUIM mới thêm giao dịch T4, rồi giải thích độ mới và reinduction. Phân biệt utility tích lũy toàn lịch sử với utility chỉ tính trong cửa sổ: đây không phải cùng một định nghĩa.

Ví dụ tự xây dựng này phục vụ giải thích, không phải kết quả thực nghiệm.

## 3. Các phát hiện ban đầu từ tài liệu và repository

Nguồn paper: `W:/Study/Tài liệu tham khảo chính.pdf`, Sra và Chand, DOI: https://doi.org/10.1007/s41019-023-00229-4.
Nguồn slide: `W:/Study/SUM - Mining from Incremental Datasets.pptx`.
Tài liệu SPMF: https://www.philippe-fournier-viger.com/spmf/ScentedMiner_utility.php.

| Phát hiện | Căn cứ | Hệ quả / việc cần làm |
|---|---|---|
| Paper có ba ý chính: cấu trúc residue/master map, reinduction, tăng ngưỡng động | Mục 4.1–4.3 | Báo cáo phải giải thích cả ba, không chỉ incremental |
| Mỗi lần gọi runAlgorithm, code SUM tạo lại các map và đặt tid=0 | algorithms/sum/AlgoScentedUtilityMiner.java | Chạy nhiều prefix chưa chứng minh cập nhật trạng thái giữa các batch |
| Code giảm counter của mọi item đã thấy ở mỗi giao dịch | Vòng đọc dữ liệu của runAlgorithm | Có vị trí cụ thể để khảo sát tối ưu chi phí cập nhật |
| Code giữ item khi counter >= 0; văn bản paper có chỗ yêu cầu dương/khác 0 | sortedMap; paper mục 4.2 và 5 | Phải chốt quy ước biên và kiểm thử RC=0; không âm thầm đổi nghĩa |
| Singleton được ghi ngay khi lần đầu vượt ngưỡng, trước khi hoàn tất đọc dữ liệu | saveItemset và HUISet | Có nguy cơ ghi utility chưa phải giá trị cuối và giữ singleton đã hết hiệu lực; cần ca kiểm chứng |
| m-SUM trong paper là ULB có thêm reinduction | Paper mục 6.1 | Không tự gọi mọi phiên bản sửa của SUM là m-SUM |
| Slide có m-SUM, prefix, W=150; runner đang có SUM/ULB, W=10 | Slide 13–17; experiments/MainCompareAllDatasets.java | Không sử dụng số liệu slide làm bằng chứng; tự dựng runner, cấu hình và log mới |
| Runner dùng cancel(true), nhưng chưa thấy thuật toán kiểm tra interrupt | MainCompareAllDatasets; tìm kiếm mã nguồn | Timeout có thể không dừng tác vụ, làm nhiễu các lượt sau; nên dùng JVM độc lập |
| Pivot bỏ qua cột Status | generatePivotTables | Không diễn giải thời gian timeout như thời gian hoàn tất |
| Các cấu hình Foodmart đã đọc trong CSV cho 0 HUI | results/comparison_results_all.csv | Chưa đủ để kết luận hiệu quả khai phá trên trường hợp có nhiều mẫu |

Đây là kiểm tra tĩnh và đối chiếu tài liệu, chưa phải kết luận thuật toán sai hay benchmark mới. Java/javac chưa có trên PATH của phiên làm việc; cần xác định JDK trước khi biên dịch. Các kết quả cũ được giữ nguyên.

## 4. Hướng cải tiến đầu tiên: tính reinduction bằng lần xuất hiện cuối

Tên làm việc: SUM với cập nhật reinduction bằng lastSeen. Đây là tối ưu cài đặt được đề xuất, chưa tuyên bố thuật toán mới hay tính mới học thuật.

Vấn đề cụ thể: code duyệt toàn bộ map counter để trừ 1 ở mỗi giao dịch, dù giao dịch chỉ chứa một số ít item.

Giải pháp: lưu lastSeen(i), cập nhật khi gặp item i. Tại thời điểm t, với W cố định:

`RC(i,t) = W - (t - lastSeen(i))`

Nếu giữ quy ước hiện tại RC >= 0 thì điều kiện tương đương là `t - lastSeen(i) <= W`. Nếu chọn RC > 0 theo mô tả paper, phải đổi đồng bộ baseline và oracle, ghi rõ đó là thay đổi đặc tả chứ không phải tối ưu tương đương code cũ.

Chứng minh ngắn: khi gặp i tại t, lastSeen=t nên RC=W; mỗi giao dịch tiếp theo không có i làm t tăng 1, tương đương giảm counter 1. Khi i tái xuất hiện, công thức trả về W.

Chi phí quản lý counter hiện tại tỉ lệ với tổng số item đã biết qua các bước quét. Với lastSeen, cập nhật chỉ theo số item xuất hiện trong giao dịch, cộng chi phí kiểm tra các item tại thời điểm khai phá. Không suy ra toàn bộ thuật toán nhanh hơn cùng tỷ lệ: join có thể chiếm phần lớn thời gian.

Code dự kiến: giữ bản gốc làm mốc; thêm biến thể hoặc chế độ lastSeen có thể bật/tắt; ghi riêng thời gian đọc/cập nhật, khai phá/join và ghi đầu ra. Chỉ so sánh hiệu năng khi đầu ra chuẩn hóa bằng nhau. Tối ưu này không tự giải quyết các nghi vấn về singleton, duyệt tổ hợp hoặc incremental state.

Tự xây dựng và kiểm chứng biến thể lastSeen trên baseline có nguồn gốc xác định; không chờ hoặc dựa vào bản sửa của teammate. Hướng thứ hai chỉ chọn sau profiling: giảm tạo lại danh sách key trong đệ quy hoặc tối ưu join, có kiểm thử tính tương đương. Chưa ưu tiên DFS→BFS vì ảnh hưởng bộ nhớ và thứ tự duyệt cần phân tích rộng hơn.

Phạm vi tối thiểu cho một người: baseline SUM có phiên bản và nguồn xác định, oracle độc lập, một biến thể tối ưu, runner tái lập, hai bộ dữ liệu khởi đầu, báo cáo và bài trình bày. Đánh giá oracle và baseline trước khi quyết định lastSeen có phù hợp làm đóng góp chính hay chỉ là một tối ưu phụ. Nếu baseline không khớp đặc tả, ghi nhận và giải quyết sai khác trước benchmark; tách sửa tính đúng khỏi tối ưu hiệu năng. Chỉ mở rộng thêm baseline, dữ liệu hoặc cải tiến sau khi bộ tối thiểu chạy đúng. Mọi phần tái sử dụng từ SPMF vẫn phải ghi nguồn và giữ thông tin bản quyền.

## 5. Điều kiện đúng trước khi chạy benchmark lớn

1. Chốt đầu ra: HUIM thông thường hay HUI có lọc độ mới của từng item. Các item đều mới không đồng nghĩa chúng vừa đồng xuất hiện.
2. Viết brute-force oracle trên dữ liệu nhỏ: liệt kê tập con, tính utility chính xác, lọc theo đặc tả. So sánh itemset và utility, không chỉ số lượng HUI.
3. Ca bắt buộc: utility đúng bằng ngưỡng; các singleton dưới ngưỡng nhưng tổ hợp vượt ngưỡng; RC bằng 0 và -1; item hết hiệu lực rồi trở lại; singleton vượt ngưỡng sớm nhưng utility tiếp tục tăng; cùng số HUI nhưng khác itemset.
4. Với cập nhật tăng dần thực sự: đầu ra sau mỗi batch phải khớp oracle trên toàn prefix tương ứng, theo cùng ngưỡng và quy ước độ mới.
5. Ghi rõ sai khác giữa baseline được cung cấp và đặc tả lựa chọn. Không dùng sự tương đương với baseline thay thế kiểm tra tính đúng theo bài toán.

## 6. Thiết kế thực nghiệm

- Bắt đầu Foodmart và Mushroom; thêm Chess/Connect sau khi pipeline ổn định. Xác minh kích thước, nguồn, utility thực hay được gán, thứ tự giao dịch. Không mặc định thứ tự dòng là thời gian thực.
- Giữ cùng dữ liệu, thứ tự, ngưỡng và W khi so baseline với tối ưu tương đương. Chọn dải ngưỡng qua pilot để có cả ít và nhiều HUI, ghi rõ cách chọn.
- ULB giải bài toán HUIM không lọc độ mới; chỉ so sánh như cùng nhiệm vụ khi cấu hình/đặc tả đã tương thích. Nếu lọc theo tập item active, vẫn giữ TU gốc hoặc cận trên hợp lệ và giải thích cách tính utility.
- Mỗi lượt một JVM, giới hạn heap thống nhất, timeout ở cấp process, output riêng. Ghi phiên bản Java, máy, commit, tham số, mã thoát và trạng thái hoàn tất.
- Chạy lặp ít nhất 3 lần, báo trung vị và độ phân tán. Ghi rõ có tính startup JVM và I/O hay không. Không gọi mẫu đo heap cuối lượt là peak RAM toàn tiến trình.
- Đo thời gian tổng, các pha, bộ nhớ theo định nghĩa rõ, số join/candidate và tập kết quả. Timeout/OOM là trạng thái riêng, không điền 0 hay coi thời hạn là runtime hoàn tất.
- Thực nghiệm loại bỏ thành phần: counter gốc so với lastSeen; khảo sát W và ngưỡng riêng. Thay đổi W có thể đổi nhiệm vụ/đầu ra, không phải phép chứng minh tăng tốc tương đương.

## 7. Các mốc bàn giao đề xuất

| Thời gian | Đầu ra cần có |
|---|---|
| 20–22/10 | Xác lập baseline có nguồn; dựng môi trường và ví dụ HUIM bằng tay; bảng paper–code; hỏi thầy phạm vi đóng góp |
| 23–26/10 | Đặc tả đầu ra, oracle, kiểm thử nhỏ và baseline tái chạy được |
| 27–29/10 | Code lastSeen đã áp dụng, kiểm thử tương đương, pilot; trình bày vấn đề–giải pháp–code–bằng chứng vào thứ Ba hoặc thứ Năm |
| 30/10–05/11 | Benchmark độc lập, phân tích từng thành phần, viết các mục nền tảng và thuật toán |
| 06–12/11 | Hoàn thiện thực nghiệm và bản báo cáo đầy đủ lần 1; gửi thầy xin duyệt ngày 10 hoặc 12/11 |
| 13–17/11 | Sửa theo góp ý; hoàn thiện phần hạn chế, khả năng tái lập và slide cuối |
| 19/11 | Mốc đề xuất chốt duyệt; còn thời gian xử lý góp ý |
| 20–21/11 | Rà soát, đóng gói code/dữ liệu/kết quả/báo cáo và nộp trước 23:59 ngày 21/11 |

Không chờ đến khi xong thực nghiệm mới viết. Không chờ ngày 21/11 mới xin duyệt. Kế hoạch cần điều chỉnh theo lịch thầy và thời lượng thực tế của người thực hiện; không tính thêm năng lực từ teammate.

## 8. Khung báo cáo bám đầy đủ paper

1. Introduction: bối cảnh ứng dụng, HUIM, động cơ và phạm vi đóng góp của nhóm.
2. Problem Statement: dữ liệu, utility, HUI, TWU, cập nhật tăng dần, đặc tả relevance, giả thiết.
3. Related Work: các nhóm thuật toán trong paper, baseline thực sự sử dụng và khác biệt nhiệm vụ.
4. Proposed Work: SUM gốc, cấu trúc dữ liệu, duyệt/join/cắt nhánh, cập nhật, reinduction, ngưỡng động; sau đó phần tối ưu của nhóm và lập luận đúng.
5. SUM: An Example: ví dụ tính tay xuyên suốt, cập nhật trạng thái và trace code tương ứng.
6. Experimental Results: môi trường, dữ liệu, kiểm tra đúng, tái lập, so sánh, ablation, khảo sát tham số và hạn chế. Giữ và giải thích các nhánh so sánh 6.1–6.5 của paper; phân biệt rõ nội dung tổng thuật của tác giả và thí nghiệm nhóm thực hiện, không nhận là đã tái lập những mục chưa chạy.
7. Conclusion and Future Work: những gì đã chứng minh, những gì chưa chứng minh và hướng tiếp theo.

Phụ lục: lệnh chạy, commit, cấu hình, nguồn dữ liệu, test oracle, log, bảng kết quả thô và phân công. Trích dẫn nguồn code SPMF và giữ thông tin bản quyền.

## 9. Buổi trình bày có code cần chứng minh điều gì?

Chuỗi trình bày: một giao dịch thực tế → utility của itemset → vì sao tìm kiếm khó → SUM lưu và ghép dữ liệu thế nào → đoạn code đang tốn công ở đâu → thay đổi lastSeen → chứng minh tương đương → demo test → số đo pilot → phạm vi xin thầy duyệt.

Mỗi thuật ngữ phải gắn với một phần của ví dụ. Có thể trình bày trung thực rằng tối ưu chỉ cải thiện pha cập nhật, chưa cải thiện đáng kể tổng runtime nếu đó là kết quả. Kết quả âm có phân tích vẫn hữu ích; không tạo số liệu để làm đẹp đề xuất.

## 10. Thông tin còn cần để hoàn thiện việc nộp

- Mẫu báo cáo, quy định nộp và quy trình giảng viên duyệt.
- Ngày cụ thể gặp thầy tiếp theo và phản hồi về phạm vi đề xuất.

Các thông tin này không chặn việc dựng baseline, oracle và ví dụ giải thích. Tiến độ sẽ được điều chỉnh theo kết quả thực tế từng mốc, không yêu cầu một cam kết phân công từ teammate.

Tài liệu này là kế hoạch cùng đánh giá ban đầu, không phải báo cáo nghiên cứu đã hoàn tất hoặc kết quả đã được giảng viên chấp thuận.
