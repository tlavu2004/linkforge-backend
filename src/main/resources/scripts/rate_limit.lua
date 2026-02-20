-- Redis Lua Script for Simple Rate Limiting (Fixed Window)
-- KEYS[1] = Rate limit key (e.g., "rate_limit:ip:192.168.1.1")
-- ARGV[1] = Maximum allowed requests
-- ARGV[2] = Time window in seconds

local key = KEYS[1]
local maxRequests = tonumber(ARGV[1])
local timeWindow = tonumber(ARGV[2])

local currentRequests = redis.call("INCR", key)

if currentRequests == 1 then
    -- It's the first request in the window, set the expiration
    redis.call("EXPIRE", key, timeWindow)
end

if currentRequests > maxRequests then
    -- Limit exceeded
    return 0
else
    -- Request allowed
    return 1
end
