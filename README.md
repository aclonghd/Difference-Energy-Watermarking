# _Difference Energy Watermarking_
_Project cuối kỳ môn KTGT Khóa D18 PTIT_

Chương trình giấu tin trong video dựa trên sự khác biệt năng lượng

### Mô tả
Sử dụng thành phần luma Y trong không gian màu YCbCr để biến đổi DCT và nhúng các nhãn bit.


### Chức năng
- Giấu tin trong video
- Hỗ trợ tiếng Việt
- Tách tin đã giấu trong video

### Thuật toán giấu tin:
#### Đầu vào
- Filepath video
- Khác biệt năng lượng D
- Kích thước vùng LC n
- Giá trị minC để giới hạn vùng cắt C
- Thông điệp
#### Tiền xử lý
1. Lấy 1 frame I ngẫu nhiên (trong chương trình này lấy frame thứ 120), biến đổi thông điệp thành nhị phân
#### Tiến hành giấu tin
2. Chia frame thành các khối 8x8 pixel.
3. DCT từng khối và hiển thị ra màn hình.
4. Vòng lặp tìm kiếm vùng LC thích hợp sao cho giá trị c của vùng cắt C thỏa mãn công thức: 
  
    **𝑐(𝑛,𝑄,𝐷,𝑐_min )=max⁡{𝑐_min, max⁡{𝑔∈{1,63}|(𝐸𝐴(𝑔,𝑛,𝑄)) > 𝐷 ∧ (𝐸𝐵(𝑔,𝑛,𝑄) > 𝐷)}}**

    Trong đó:
      + D là chênh lệch năng lượng cần thiết để biểu diễn 1 bit trong vùng lc,
      + cmin là chỉ số nhỏ nhất dùng để giới hạn vùng cắt c.
      + n là vị trí của khối DCT trong vùng con.
5. Sau khi tìm được vùng LC thích hợp ta tiến hành nhúng tin dựa trên nguyên tắc sau: 
    
    + Bit nhãn 1: Tiến hành loại bỏ các hệ số DCT trong vùng cắt C ở vùng con EA
    + Bit nhãn 0: Tiến hành loại bỏ các hệ số DCT trong vùng cắt C ở vùng con EB
6. Hiển thị các khối DCT đã sửa ra màn hình
7. Tiến hành khôi phục lại hình ảnh và hiển thị ra màn hình
8. Mã hóa lại video và sinh ra tệp tin khóa chứa vị trí các vùng LC đã giấu tin.

### Thuật toán tách tin:
#### Đầu vào
- Filepath video
- Khác biệt năng lượng DD (0 < DD <= D)
- Kích thước vùng LC n
- Filepath key chứa vị trị vùng LC

#### Tiến hành tách tin
1. Lấy frame đã giấu, chia frame thành các khối 8x8 pixel
2. DCT từng khối
3. Xác định vùng cắt C ở các vùng LC theo công thức: **𝑐(𝑛,𝑄,𝐷𝐷)=m𝑎𝑥⁡{m𝑎𝑥⁡{𝑔∈{1,63}|(𝐸𝐴 (𝑔,𝑛,𝑄) > 𝐷𝐷)}, m𝑎𝑥⁡{𝑔∈{1,63}|(𝐸_𝐵 (𝑔,𝑛,𝑄) > 𝐷𝐷)}}**
4. Tính toán sự khác biệt năng lượng trong vùng con C: EA - EB
    Nguyên tắc: 
      + EA - EB > 0: bit nhãn là 1
      + EA - EB < 0: bit nhãn là 0
