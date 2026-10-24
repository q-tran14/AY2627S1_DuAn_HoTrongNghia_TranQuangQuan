# Phân tích paper SUM, mã nguồn công bố và giải pháp đề xuất

Ngày kiểm chứng: 20/10/2026. Người dùng yêu cầu phân tích trực tiếp để lựa chọn vấn đề nghiên cứu; tài liệu này trình bày kết quả, không phải bài hướng dẫn tự học.

## Kết luận lựa chọn hướng

Vấn đề ưu tiên là **tính đầy đủ của phép duyệt/cắt nhánh trong bản SUM được phân phối bởi SPMF**, trước khi bàn về tăng tốc. Có phản ví dụ tái hiện trên source chính thức: một giao dịch chứa ba item utility 4, ngưỡng 10, nhưng SUM không xuất bộ ba utility 12. Đề xuất thay cơ chế mở rộng bằng duyệt có thứ tự duy nhất và cắt nhánh bằng cận trên bao phủ mọi tập mở rộng; giữ residue map và chính sách reinduction rõ ràng. Sau đó nghiên cứu chi phí của từng thành phần trên một baseline đã kiểm tra đúng.

Không chọn Adaptive-W làm hướng chính lúc này: nó thay đổi tập mục đủ điều kiện và làm khó việc chứng minh so sánh cùng bài toán. lastSeen là tối ưu phụ có thể bổ sung sau, không giải quyết lỗi bỏ sót.

## 1. Nguồn và phạm vi xác minh

- Paper chính: Sra & Chand, A Reinduction-Based Approach for Efficient High Utility Itemset Mining from Incremental Datasets, Data Science and Engineering 9:73–87. DOI: https://doi.org/10.1007/s41019-023-00229-4. Đã đọc PDF người dùng cung cấp và xem ảnh trang in 77–80 để xác nhận pseudocode và các hình.
- Tài liệu thuật toán: https://www.philippe-fournier-viger.com/spmf/ScentedMiner_utility.php.
- Source tải trực tiếp: https://www.philippe-fournier-viger.com/spmf/spmf.zip, ngày 20/10/2026. SHA-256 của ZIP: `48599087a24bd43fc1f32dac05cd2390f3895da437e092a7c0b7e6c7426a9c10`.
- Class chính thức: `ca/pfv/spmf/algorithms/frequentpatterns/rminer_sminer/AlgoScentedUtilityMiner.java`.
- So sánh với class local: khác package, import MemoryLogger và bốn lệnh ghi metadata kèm dòng trắng. Logic khai phá giống nhau. Không quy các lỗi khai phá đã tái hiện cho teammate.
- Đã biên dịch class chính thức cùng ResidueMap và MemoryLogger chính thức, không sửa logic. Harness riêng chỉ truyền input/output/tham số và đọc giá trị trả về.
- Script tái lập: `scripts/audit_sum.py`; báo cáo máy đọc: `results/local/official-audit/report.json`; diff: `results/local/official-audit/official-vs-local.diff`. ZIP cần có tại `tmp/audit/spmf.zip` trước khi chạy script. Script dùng Python chuẩn và JDK bundled của extension Java hiện có.

Giới hạn: đây là bản SPMF tải được hiện nay, chưa chứng minh nó chính là binary/source dùng tạo các biểu đồ năm 2023 của paper. Vì vậy kết luận lỗi cài đặt luôn gắn với bản đã kiểm tra; không suy ra mọi kết quả thực nghiệm của tác giả sai.

## 2. SUM trong paper và trong code

Paper kết hợp residue map để lưu TID/utility, master map sắp theo tổng utility, reinduction để chọn singleton còn liên quan, duyệt đệ quy để tạo tập mục và chính sách tăng ngưỡng giữa các cập nhật. Trong source, BitSet được giao để tìm TID chung; utility của các tập rời nhau được cộng trên các TID đó. Residue không được lưu thành field mà được tính qua `minUtility - totUtil`: khác cách biểu diễn này tự nó không phải lỗi.

| Thành phần | Paper | Source đã kiểm tra | Đánh giá |
|---|---|---|---|
| Utility và residue | Mục 2, 4, 5 | ResidueMap lưu tidUtil, totUtil; residue tính khi so sánh | Tương thích về biểu diễn cơ bản |
| Reinduction | Mục 4.2 giảm mỗi giao dịch, reset khi xuất hiện | Vòng quét làm đúng kiểu giảm/reset | Có ở code |
| Biên relevance | Văn bản mục 5 nói counter dương | sortedMap giữ RC >= 0 | Cần công bố quy ước; khác biệt ảnh hưởng biên |
| Cập nhật qua nhiều batch | Mục 4.1 giữ master map để cập nhật | runAlgorithm khởi tạo lại map, tid=0 | API hiện có chưa cung cấp cập nhật bền trạng thái như mô tả |
| Tăng ngưỡng | Mục 4.3: MU_next=MU_current+TU_min | Trả dbMinUtility, nhưng launcher và adapter SPMF không dùng để tăng ngưỡng | Chưa thực hiện chính sách đầy đủ trong đường chạy công bố |
| HUI cuối cùng | Định nghĩa utility cộng trên giao dịch chứa itemset | Singleton ghi trong lúc quét và chống ghi lại | Utility đầu ra có thể sai |
| Khai phá đầy đủ | Mục 2 nói khai phá tất cả HUI | Điều kiện residue chặn đường mở rộng ở một số ca | Đã có phản ví dụ độc lập với relevance |

## 3. Vấn đề chính: điều kiện cho một phép ghép bị dùng để chặn đường đến tập lớn hơn

Input hợp lệ, utility dương:

```text
1 2 3:12:4 4 4
```

minUtility=10, maxR=10. Mọi item đều vừa xuất hiện. Bộ ba {1,2,3} có utility 12 và là HUI duy nhất; mọi singleton utility 4, mọi cặp utility 8.

Source kiểm tra tại `exploreAndJoinItemsets` (local dòng 309):

```java
sortedMap.get(key).totUtil >= (minUtility - inputMap.totUtil)
```

Trong ca này là `4 >= 10 - 4`, sai. Các phép ghép cặp không được thực hiện, nên không có điểm xuất phát cho đệ quy tạo bộ ba. Chạy source chính thức trả tập rỗng; vét cạn và tính tay đều cho {1,2,3}:12.

Điểm logic quan trọng: với utility không âm và hai tập rời nhau A,B, `U(A union B) <= U(A)+U(B)` là đúng. Vì vậy tổng dưới ngưỡng có thể loại chính cặp A union B. Nhưng điều đó **không** chứng minh các tập A union B union C đều dưới ngưỡng. Cài đặt cần một đường sinh khác bao phủ những tập lớn hơn hoặc cận trên cho toàn bộ nhánh. Phản ví dụ chứng minh đường sinh hiện tại không đáp ứng yêu cầu này.

Không chỉ bỏ câu if: còn phải xác định một thứ tự mở rộng, tránh trùng tập và chứng minh không bỏ sót. Paper SUM không trình bày đầy đủ thân hàm đệ quy ở Algorithm 1 để quy kết chính xác lỗi này cho pseudocode; kết luận chắc chắn là lỗi của cài đặt đã chạy.

## 4. Các vấn đề bổ sung đã xác nhận

### 4.1. Singleton ghi utility chưa hoàn tất

Input `1:10:10` rồi `1:5:5`, ngưỡng 10. Tổng đúng 15; source chính thức ghi 10. saveItemset gọi khi vừa vượt ngưỡng, HUISet ngăn cập nhật lần sau. Đây là lỗi đầu ra utility, không liên quan chọn RC>0 hay RC>=0. Với relevance ở thời điểm cuối, còn có ca singleton hết hiệu lực nhưng vẫn tồn tại trong output vì đã ghi sớm.

Giải pháp: chỉ xuất singleton ở thời điểm khai phá sau khi cập nhật xong batch, dùng cùng điều kiện relevance như các tập lớn hơn. Nếu muốn luồng output theo thời gian thì phải thêm mốc thời gian và cơ chế cập nhật/thu hồi rõ ràng; định dạng hiện tại không thể hiện ngữ nghĩa đó.

### 4.2. dbMinUtility không phải minimum của các tổng cuối

Code chỉ cập nhật biến này khi một singleton được gặp lại, và giữ minimum của các tổng trung gian. Ca item 1 có utility lần lượt 1,1,98, item 2 có utility 5: tổng cuối là 100 và 5 nhưng giá trị trả về là 2. Nếu không item nào lặp, giá trị có thể là Long.MAX_VALUE; đã kiểm chứng.

Giải pháp: sau cập nhật, tính minimum trên tập residue map được định nghĩa rõ. Paper chưa làm rõ nhất quán lấy tất cả map hay chỉ map active; phải công bố lựa chọn và trường hợp tập rỗng. Đường chạy hiện tại bỏ qua giá trị trả về, nên lỗi này chưa trực tiếp gây lỗi ở các lần chạy ngưỡng cố định.

### 4.3. Độ mới của item không đồng nghĩa độ mới của itemset

Mục 4.2 tính counter của singleton. Một cặp có thể xuất hiện chung rất lâu trước, nhưng mỗi item vừa xuất hiện riêng lẻ thì cả hai vẫn active. Đây là giới hạn của chính sách relevance, không tự động là lỗi cài đặt. Không được diễn giải SUM như bảo đảm mọi itemset đầu ra vừa đồng xuất hiện hoặc utility chỉ tính trong cửa sổ. Đề xuất hiện tại giữ chính sách singleton để không tự đổi bài toán.

## 5. Những điểm không nhất quán ngay trong paper

Đã xem trực tiếp hình và pseudocode, không chỉ dựa vào text extraction:

- Fig.5: w có utility 3 ở T1 và 6 ở T5, tổng 9. Fig.6/8/9 và văn bản nói tổng 11. Với ngưỡng 11, từ Fig.5 thì {w} không là HUI. Nhập đúng Fig.5 vào source chính thức thu được {q,t}:11, {t,y}:17, {q,t,y}:11 và không có {w}; khớp vét cạn.
- Mục 5.1 nói minimum là 2 rồi viết 11+3=14. Theo công thức mục 4.3, nếu minimum 2 thì ngưỡng phải là 13. Không có căn cứ âm thầm chọn một số để nói tái lập đúng paper.
- Fig.14 có các giá trị không khớp Fig.5+13: r chỉ có T1:2,T7:2,T8:4 nên tổng 8, nhưng hình thêm T2:4 và ghi 12; q tổng 15 nhưng residue 4 không khớp ngưỡng 13 hay 14. Đây là bất nhất ví dụ, chưa đủ phủ nhận thuật toán tổng quát.
- Algorithm 1 đặt vòng giảm counter ở dòng 1–3, trước vòng giao dịch dòng 4; mô tả mục 4.2 yêu cầu giảm theo từng giao dịch. Code theo mô tả mục 4.2. Cần nêu rõ khi tái lập.
- Property 3 mục 4.1, nếu UX được hiểu là utility trong phần thêm N, thiếu utility cũ: ví dụ U_D(X)=6, U_N(X)=5, ngưỡng 10 thì tổng 11 đạt ngưỡng dù UX=5<10. Công thức đúng cho ngưỡng cố định là U_D(X)+U_N(X)>=theta. Nếu UX định nghĩa là tổng sau cập nhật thì phát biểu trở thành hiển nhiên, nhưng câu chữ hiện tại không rõ.

Các lỗi số học/diễn đạt phù hợp đưa vào phần tái lập và hạn chế; không nên dùng chúng làm toàn bộ đóng góp đề tài.

## 6. Giải pháp chính đề xuất: mở rộng đầy đủ, cắt nhánh bằng cận trên an toàn

Tên mô tả làm việc: **Kiểm chứng và cải tiến cơ chế duyệt, cắt nhánh của SUM**. Đây không phải tuyên bố phát minh remaining utility; kỹ thuật này đã có trong HUIM, và code HUI-Miner trong repository cũng dùng sumIutils+sumRutils để quyết định mở rộng.

Thiết kế:

1. Chọn thứ tự toàn phần trên các item active tại lần khai phá; mỗi tiền tố chỉ thêm item đứng sau phần tử cuối. Mỗi itemset có đúng một đường sinh.
2. Giữ residue map/TID utility cho tiền tố; ghép với singleton bằng giao TID như SUM, chỉ cộng utility trên giao dịch chung.
3. Xuất tiền tố X nếu U(X)>=theta, nhưng không dừng mở rộng chỉ vì U(X)<theta.
4. Với mỗi giao dịch T chứa X, tính RU(X,T) là tổng utility của các item còn được phép thêm theo thứ tự. Cận trên:

   `UB(X) = sum over T containing X of [u(X,T) + RU(X,T)]`.

5. Chỉ dừng toàn bộ nhánh khi UB(X)<theta hoặc không còn giao dịch chung/item mở rộng.

Lập luận: với utility không âm, mọi mở rộng Y của X trên nhánh đều chỉ xuất hiện ở một tập con các giao dịch chứa X; utility từng giao dịch không vượt u(X,T)+RU(X,T). Do đó U(Y)<=UB(X). Duyệt theo thứ tự duy nhất bao phủ mọi tập con active, nên cắt bằng UB không bỏ sót HUI trong đặc tả đã chọn.

Trong phản ví dụ bộ ba, UB({1})=12, UB({1,2})=12 nên vẫn mở rộng tới {1,2,3}. Cận tổng utility của cặp 8 không còn được dùng để cắt cả nhánh.

Giữ riêng hai lớp công việc: sửa tính đúng của baseline; sau đó nghiên cứu hiệu năng bằng bật/tắt cận trên, cách tổ chức join hoặc lastSeen. Không so thời gian với bản bỏ sót nhiều kết quả rồi tuyên bố nhanh/chậm hơn như cùng nhiệm vụ.

## 7. Kiểm chứng giải pháp đã làm và phần chưa làm

- Đã viết nguyên mẫu Python độc lập trong `bounded_prototype` của script audit, có thứ tự mở rộng và cận RU, đối chiếu với vét cạn.
- Khớp tất cả 9 ca cụ thể của audit và 500 cơ sở dữ liệu ngẫu nhiên nhỏ; seed 20261020, tối đa 7 item, 10 giao dịch, utility 1–15, ngưỡng 1–100, W 0–12. Đây là kiểm tra nguyên mẫu giải pháp, không phải 500 lần kiểm tra source SPMF.
- Source chính thức chạy 9 ca cụ thể: 3 sai khác đầu ra đã nêu; hai ca bổ sung phơi bày giá trị dbMinUtility sai dù tập HUI rỗng vẫn khớp.
- Chưa tích hợp giải pháp vào Java SUM; chưa benchmark dữ liệu lớn; chưa tuyên bố nhanh hơn hoặc mới về học thuật. Kiểm tra hữu hạn không thay thế lập luận đúng.
- Chưa tái lập toàn bộ hình thực nghiệm của paper, chưa truy cập code thực nghiệm gốc năm 2023. Không dùng các phát hiện này để kết luận các biểu đồ của paper là sai.

## 8. Phạm vi đóng góp đề tài nên trình bày

Câu hỏi nghiên cứu: “Cơ chế sinh và cắt nhánh trong bản SUM công bố có bảo đảm tìm đủ các HUI theo đặc tả không; có thể tổ chức lại phép duyệt trên residue map với cận trên an toàn và giảm chi phí khai phá đến mức nào?”

Đầu ra: bảng đối chiếu paper–code; bộ phản ví dụ và oracle; bản Java có đặc tả rõ; lập luận đầy đủ và cắt nhánh an toàn; thực nghiệm bật/tắt từng thành phần. Để bám bối cảnh incremental, sau khi mining kernel đúng, tách appendBatch và mine, giữ state qua batch và kiểm chứng mỗi prefix. Việc này hỗ trợ đề tài, không thay thế vấn đề trọng tâm là khai phá đúng và hiệu quả.

Thực nghiệm công bằng: baseline gốc báo riêng completeness/utility errors; so runtime giữa các bản cùng đầu ra (duyệt đúng không cắt, duyệt đúng có cận RU, tùy chọn lastSeen). Đối chiếu thêm ULB/HUI-Miner trên cùng bài toán sau khi xử lý relevance nhất quán. Đo thời gian, bộ nhớ có định nghĩa, số join, số nhánh cắt, kích thước kết quả. Bắt đầu hai dataset, nhiều ngưỡng, độ dài giao dịch khác nhau; không kết luận miền tối ưu khi chưa có số liệu.

Mức độ đóng góp có thể bảo vệ là tái lập/kiểm chứng, sửa cơ chế khai phá và đánh giá thành phần. Cần giảng viên chấp thuận phạm vi này; không quảng bá sửa lỗi cài đặt thành một thuật toán mới hoàn toàn.
