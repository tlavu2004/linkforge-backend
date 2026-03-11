## 1. Đề bài:
Thiết kế một hệ thống cho phép người dùng nhập URL dài và nhận lại một URL ngắn. Khi truy cập URL ngắn sẽ redirect về URL gốc. Hệ thống cần xử lý lượng truy cập lớn, độ trễ thấp và đảm bảo tính sẵn sàng cao.

---

## 2. Hướng tư duy:

- Trước hết cần làm rõ yêu cầu: hệ thống phải tạo link ngắn nhanh, redirect nhanh, đảm bảo link không bị trùng và có khả năng mở rộng khi traffic tăng.
- Về functional, cần có API tạo short link và API redirect. Non-functional tập trung vào scalability, high availability và low latency.
- Tiếp theo là ước lượng: giả sử vài trăm triệu link, đọc nhiều hơn ghi rất nhiều -> read heavy system. Điều này ảnh hưởng đến việc chọn cache và database.

---

## 3. Thiết kế giải pháp

### 3.1. High level flow

User gửi URL -> API server -> tạo short code -> lưu DB -> trả về short URL.
Khi truy cập short URL -> load balancer -> app server -> cache/DB -> redirect.

### 3.2. Thành phần chính

- Load Balancer để phân phối traffic (hiện tại: **Render managed LB**)
- App servers xử lý logic (hiện tại: **Spring Boot**, kiến trúc **Hexagonal/Clean Architecture**)
- Database để lưu trữ dữ liệu (hiện tại: **PostgreSQL** — phù hợp quy mô MVP, kiến trúc Hexagonal cho phép swap sang NoSQL khi cần scale lớn mà không cần refactor business logic)
- Cache (**Redis**, TTL 24h, strategy **Cache-Aside**) để tăng tốc redirect
- ID generator để tạo short code unique (hiện tại: **TSID + Base62** — xem 3.3)

> **Ghi chú về Database**: Đề xuất ban đầu là NoSQL (DynamoDB/Cassandra) để scale tốt. Tuy nhiên, PostgreSQL kết hợp Read Replicas và Table Partitioning hoàn toàn đáp ứng được yêu cầu ở quy mô hiện tại. Kiến trúc Hexagonal (Ports & Adapters) cho phép swap database engine bất kỳ lúc nào — chỉ cần implement adapter mới cho `ShortLinkRepository` port.

### 3.3. Cách tạo short URL

Có 2 cách phổ biến:
- Hash URL rồi encode Base62 -> nhanh nhưng có thể collision
- ~~Dùng auto-increment ID rồi encode Base62 -> đơn giản và đảm bảo unique~~

**Giải pháp đã chọn: TSID (Time-Sorted ID) + Base62 Encoding**

So với auto-increment ID + Base62 (đề xuất ban đầu), TSID vượt trội hơn ở nhiều điểm:

| Tiêu chí | Auto-increment ID | TSID (đã chọn) |
|---|---|---|
| Uniqueness | ✅ Unique (phụ thuộc DB sequence) | ✅ Unique (không cần DB) |
| Distributed | ❌ Cần centralized DB sequence | ✅ Distributed-safe (mỗi node tự generate) |
| Predictability | ❌ Dễ enumerate (1, 2, 3...) | ✅ Không đoán được |
| Sortable | ✅ Theo thứ tự tạo | ✅ Time-sorted (tốt hơn — sortable + random) |
| Performance | ⚠️ Cần roundtrip DB | ✅ O(1), không cần DB |

### 3.4. Chi tiết xử lý redirect

Khi request đến short link:
- Check Redis cache trước (Cache-Aside strategy)
- Nếu miss -> query DB
- Trả về URL gốc và set cache (TTL 24h)
- Increment click count **async** (Spring Events + `@Async`) — không block redirect response

Điều này giúp giảm tải DB rất nhiều vì traffic đọc lớn.

> **Thực tế đã triển khai**: `ResolveShortLinkUseCaseImpl` thực hiện đúng flow trên. Click tracking xử lý non-blocking qua `ShortLinkEventListener` (`@Async` + `@EventListener`).

### 3.5. Scale hệ thống

Khi traffic tăng:
- Horizontal scale app servers (kiến trúc **stateless**, sẵn sàng scale)
- DB sharding theo short code (kế hoạch: PostgreSQL **Table Partitioning** theo `created_at`)
- Cache cluster (Redis external, có thể nâng lên **Redis Cluster**)
- Thêm CDN để giảm latency global (kế hoạch: **Cloudflare / CloudFront**)

---

## 4. Các vấn đề cần lưu ý

- **Collision khi generate code**: ✅ Đã giải quyết — TSID đảm bảo unique, xác suất collision gần bằng 0
- **Hot key (link viral)**: ✅ Redis cache 24h + kế hoạch thêm L1 cache (**Caffeine**) cho hot keys
- **Spam link**: ✅ Đã triển khai — **Redis Rate Limiter** + **Lua script** (atomic, sliding window, chặn theo IP)
- **Analytics tracking** (optional): ⚠️ Hiện có `clickCount` + **Prometheus metrics** (cache hit/miss, links created/resolved). Kế hoạch bổ sung: geo tracking, device tracking, time-series analytics