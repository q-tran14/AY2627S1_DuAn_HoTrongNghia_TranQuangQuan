# Chạy và đọc source SUM

Kiểm tra ngày 20/10/2026. Ưu tiên hiện tại: chạy source → hiểu luồng xử lý → sửa và kiểm thử → sau đó mới làm slide/kịch bản.

## 1. Chạy ngay

Mở PowerShell tại thư mục dự án:

```powershell
./scripts/run-sum.ps1
```

Script tìm JDK qua tham số `-JdkHome`, JAVA_HOME, PATH hoặc JDK của extension Java trong VS Code. Máy này đã chạy bằng Temurin 25.0.4.1 đi kèm extension; không cài thêm và không sửa PATH hệ thống.

Script biên dịch riêng SUM và các phụ thuộc vào `build/classes`, chạy với heap tối đa 512 MB. Đây không phải build toàn repository: UP-Growth đang thiếu Item, Itemset, UPNode, UPTree; MainTestEx1 còn import sai package. Những thành phần đó chưa cần cho việc học SUM.

Đầu vào mặc định: `datasets/DB_Utility.txt`, minUtility=30, maxR=10. Đầu ra: `results/local/sum-example.txt`. Lần chạy kiểm tra trả về 8 HUI, khớp oracle cả itemset lẫn utility. Thời gian của lần chạy nhỏ không dùng làm benchmark.

Đổi tham số:

```powershell
./scripts/run-sum.ps1 -InputFile datasets/DB_Utility.txt -OutputFile results/local/example-20.txt -MinUtility 20 -MaxR 10
```

Đường dẫn tương đối được tính từ thư mục dự án, kể cả khi gọi script từ thư mục khác. Nếu muốn dùng JDK khác, thêm `-JdkHome 'đường dẫn đến thư mục JDK'`.

## 2. Phần đã sửa để chạy được

`algorithms/sum/MainTestAlgoScentedUtilityMiner.java` trước đây gọi `getResource("DB_Utility.txt")`: Java tìm tài nguyên trong package của class, không phải trong thư mục datasets. Bản sửa dùng đường dẫn file, nhận bốn tham số input/output/minUtility/maxR, kiểm tra đầu vào và tạo thư mục đầu ra. Chặn trường hợp input và output là cùng file.

Chưa sửa thuật toán trong `AlgoScentedUtilityMiner.java`. Như vậy các ca sai bên dưới vẫn tái hiện được trên logic khai phá ban đầu của repository. Chưa xác nhận file này giống từng dòng với một bản phát hành SPMF chính thức.

## 3. Đọc dữ liệu trước khi đọc thuật toán

Ví dụ dòng đầu:

```text
3 5 1 2 4 6:30:1 3 5 10 6 5
```

Đây là một giao dịch, gồm item 3,5,1,2,4,6. Utility tương ứng là 1,3,5,10,6,5; tổng bằng 30. Các số ở phần cuối là utility của item trong giao dịch, không mặc định là số lượng hàng. TID là số thứ tự giao dịch hợp lệ, bắt đầu từ 1 trong code này.

Ví dụ X={2,4}: T1 đóng góp 10+6=16; T2 đóng góp 8+6=14; các giao dịch khác không chứa đủ X. Tổng utility=30, nên đầu ra có `2 4 #UTIL: 30`.

## 4. Thứ tự đọc source và đặt breakpoint

| File / hàm | Theo dõi điều gì? |
|---|---|
| MainTestAlgoScentedUtilityMiner.main | Giá trị input, output, minUtility, maxReinductionCount |
| AlgoScentedUtilityMiner.runAlgorithm | Khởi tạo trạng thái, đọc từng dòng và tăng tid |
| ResidueMap.addToMap | tidUtil lưu utility theo TID, totUtil cộng dồn |
| Vòng giảm mapItemToReinductionCount | Mọi item đã thấy bị giảm 1, item trong giao dịch hiện tại được reset về maxR |
| Đoạn tạo entries và sortedMap | Sắp tăng theo totUtil; giữ item có TWU >= ngưỡng và RC >= 0 |
| exploreAndJoinItemsets | Chọn cặp cần ghép và kiểm tra điều kiện residue |
| computeBitJoin | Giao hai BitSet tìm giao dịch chứa đủ hai tập; cộng utility tại TID chung |
| recursiveJoinAndEvaluate | Mở rộng tập mục; theo dõi currentIndex và điều kiện dừng |
| saveItemset | HUISet chống ghi trùng; writer ghi utility tại thời điểm gọi |

Luồng gọi chính:

```text
main → runAlgorithm
       ├─ đọc giao dịch → cập nhật ResidueMap, TWU, BitSet, counter
       ├─ sắp xếp/lọc singleton → sortedMap
       └─ exploreAndJoinItemsets
           ├─ computeBitJoin
           ├─ saveItemset
           └─ recursiveJoinAndEvaluate → ghép tiếp
```

Sau T1, với item 2: tidUtil={1:10}, totUtil=10, TWU=30, BitSet có TID 1, counter=10. Sau T2: tidUtil={1:10,2:8}, totUtil=18, TWU=50, BitSet có TID 1 và 2, counter được reset=10. Dùng các giá trị này để tự kiểm tra khi debug.

Chú ý: residue không được lưu thành một field trong ResidueMap hiện tại; điều kiện dùng trực tiếp `minUtility - inputMap.totUtil`. Mỗi lần runAlgorithm tạo lại các map. Trả về dbMinUtility không có nghĩa chương trình gọi đã thực hiện chiến lược ngưỡng động của paper.

## 5. Kiểm tra tính đúng độc lập

```powershell
./scripts/run-sum.ps1 -Check
```

Oracle trong `experiments/CheckSumCorrectness.java` liệt kê mọi tập con trên dữ liệu nhỏ, cộng utility trong tất cả giao dịch chứa tập đó. Đặc tả kiểm tra: utility toàn lịch sử, mọi item trong tập thỏa RC >= 0 tại giao dịch cuối; không đòi hỏi cả tập vừa đồng xuất hiện. Đây là quy ước được chọn để kiểm tra code hiện tại, chưa giải quyết khác biệt RC>0 trong văn bản paper.

Input, output và báo cáo nằm trong `results/local/checks`. Lệnh hiện trả exit code 1 có chủ ý vì phát hiện sai khác; đó không phải lỗi biên dịch.

| Ca | Kết quả kiểm tra |
|---|---|
| spmf-example | PASS: đúng 8 itemset và utility |
| singleton-final-utility | FAIL: hai giao dịch `1:10:10`, `1:5:5`, ngưỡng 10. Cần utility 15 nhưng ghi 10 |
| expired-singleton | FAIL theo đặc tả độ mới đã chọn: item 1 hết hiệu lực cuối dữ liệu nhưng vẫn nằm trong output |
| triple-only-high | FAIL: `1 2 3:12:4 4 4`, ngưỡng 10. Bộ ba utility 12 bị bỏ sót |
| counter-zero | PASS theo biên RC >= 0 |
| reinduction | PASS: item vắng rồi xuất hiện lại |

Nguyên nhân cần đọc:

1. Singleton được ghi trong lúc đọc dữ liệu; HUISet ngăn ghi lại khi utility tăng. Có thể sửa bằng cách chỉ xuất singleton sau khi quét xong và áp dụng đúng điều kiện relevance. Cần thống nhất đầu ra cuối cùng trước khi sửa.
2. Ca bộ ba: mỗi singleton utility 4; tổng của bất kỳ cặp nào là 8 < 10. Code không tạo cặp, nên không tới bộ ba 12. Một điều kiện đủ để loại cặp chưa chắc đủ để loại mọi tập mở rộng của cặp. Cần phân tích lại đường duyệt và cận trên trước khi sửa, không chỉ bỏ một câu `if` rồi kết luận đúng.

Các ca nhỏ chứng minh sai khác cụ thể; 6 ca không đủ chứng minh tính đúng toàn thuật toán. Chưa tối ưu lastSeen và chưa chạy benchmark lớn. Ưu tiên sửa tính đúng và kiểm tra thêm trước khi đo tốc độ.

## 6. Cách thực hiện một sửa đổi có thể giải thích

1. Chọn một ca sai, tính đáp án bằng tay.
2. Chạy baseline và lưu expected/actual.
3. Đặt breakpoint tại hàm gây sai, giải thích trạng thái trước/sau.
4. Sửa một nhóm nguyên nhân, giữ diff nhỏ; ghi rõ thay đổi đặc tả nếu có.
5. Chạy lại toàn bộ ca, bổ sung dữ liệu ngẫu nhiên nhỏ để so oracle.
6. Khi tính đúng ổn định mới đo hiệu năng và chọn cải tiến nghiên cứu.

Mục tiêu học trước mắt: tự giải thích vì sao chương trình tạo ra `{2,4}:30`, vì sao singleton bị ghi 10 thay vì 15, và vì sao bộ ba utility 12 bị bỏ sót. Đây sẽ là cơ sở thực tế để xây dựng slide sau này.
