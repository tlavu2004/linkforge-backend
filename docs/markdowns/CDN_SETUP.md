# CDN Setup Guide — Cloudflare

> Hướng dẫn cấu hình Cloudflare CDN cho LinkForge để cache redirect responses tại edge, giảm latency cho user toàn cầu.

---

## 1. Tổng quan

```
User (EU) → Cloudflare Edge (EU) → Cache HIT → 301 Redirect (không về origin)
                                 → Cache MISS → Origin (Render, SG) → 301 + Cache tại Edge
```

Redirect 301 (VIP) đã có header `Cache-Control: public, max-age=86400` → Cloudflare tự động cache trong 24h.
Redirect 302 (non-VIP) có `Cache-Control: no-store` → Cloudflare **không cache**.

---

## 2. Cấu hình Cloudflare

### 2.1 DNS Setup

1. Đăng nhập [Cloudflare Dashboard](https://dash.cloudflare.com/)
2. **Add Site** → nhập domain (ví dụ: `linkforge.app`)
3. Chuyển nameservers về Cloudflare (theo hướng dẫn của registrar)
4. Tạo DNS records:

| Type | Name | Content | Proxy |
|---|---|---|---|
| CNAME | `@` | `linkforge-backend-r0tk.onrender.com` | ☁️ Proxied |
| CNAME | `api` | `linkforge-backend-r0tk.onrender.com` | ☁️ Proxied |

> **Quan trọng**: Bật **Proxied** (☁️) để traffic đi qua Cloudflare edge → mới có cache.

### 2.2 SSL/TLS

1. Vào **SSL/TLS → Overview**
2. Chọn mode: **Full (strict)**
   - Cloudflare ↔ Origin: encrypted
   - Yêu cầu origin có valid SSL cert (Render cung cấp sẵn)

### 2.3 Cache Rules cho Redirect

1. Vào **Rules → Page Rules** (hoặc **Cache Rules** nếu dùng new UI)
2. Tạo rule:

**Page Rule:**
| Setting | Value |
|---|---|
| URL match | `*linkforge.app/r/*` |
| Cache Level | **Cache Everything** |
| Edge Cache TTL | **1 day** (24h, khớp với `max-age=86400`) |

> Chỉ `/r/*` path cần cache. API paths (`/api/*`) **không nên cache**.

3. Tạo thêm rule chặn cache API:

| Setting | Value |
|---|---|
| URL match | `*linkforge.app/api/*` |
| Cache Level | **Bypass** |

> **Thứ tự quan trọng**: Rule Bypass API phải đặt **trên** rule Cache Redirect.

### 2.4 DDoS Protection (mặc định)

Cloudflare tự bật DDoS protection. Kiểm tra:
1. **Security → DDoS** → Verify "Active"
2. **Security → WAF** → Enable managed rules (free tier có basic protection)

---

## 3. Kiểm tra CDN hoạt động

### Bằng curl

```bash
curl -sI https://linkforge.app/r/{shortCode}
```

**Response headers mong đợi khi CDN MISS (lần đầu):**
```
HTTP/2 301
location: https://google.com
cache-control: max-age=86400, public
vary: Accept
cf-cache-status: MISS
cf-ray: abc123-SIN
```

**Response headers mong đợi khi CDN HIT (lần sau):**
```
HTTP/2 301
location: https://google.com
cache-control: max-age=86400, public
vary: Accept
cf-cache-status: HIT
cf-ray: def456-CDG
age: 3600
```

Kiểm tra:
- ✅ `cf-cache-status: HIT` → CDN đã cache
- ✅ `cf-ray: ...CDG` → served từ edge gần user (Paris)
- ✅ `age: 3600` → đã cache được 1 giờ

### Bằng Postman

1. `GET https://linkforge.app/r/{shortCode}` (tắt auto-follow redirects)
2. Tab **Headers** → tìm `cf-cache-status`
3. Gửi lần 2 → giá trị chuyển từ `MISS` → `HIT`

---

## 4. Purge Cache (khi cần)

Khi link bị xóa hoặc cập nhật, cần purge CDN cache:

1. **Cloudflare Dashboard → Caching → Purge Cache**
2. Chọn **Custom Purge** → nhập URL: `https://linkforge.app/r/{shortCode}`
3. Hoặc dùng API:

```bash
curl -X POST "https://api.cloudflare.com/client/v4/zones/{zone_id}/purge_cache" \
  -H "Authorization: Bearer {api_token}" \
  -H "Content-Type: application/json" \
  --data '{"files":["https://linkforge.app/r/{shortCode}"]}'
```

> **Tương lai**: Tích hợp Cloudflare API vào `DeleteShortLinkUseCase` để auto-purge khi link bị xóa.

---

## 5. Chi phí

| Plan | Giá | Ghi chú |
|---|---|---|
| Free | $0 | Đủ dùng: CDN, DDoS, SSL, Page Rules (3 rules) |
| Pro | $20/mo | Thêm WAF advanced, image optimization |

> Với quy mô hiện tại, **Free plan** hoàn toàn đủ.
