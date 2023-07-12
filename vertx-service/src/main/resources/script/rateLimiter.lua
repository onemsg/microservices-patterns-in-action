-- rateLimiter.lua
local key = KEYS[1]
local limit = tonumber(ARGV[1])
local duration = tonumber(ARGV[2])

local exists = redis.call('exists', key)

local now = tonumber(redis.call('time')[1])

if (exists == 0) then
    local reset = now + duration
    redis.call('HSET', key, 'count', 1, 'reset', reset)
    redis.call('EXPIREAT', key, reset)
    return {1, reset}
else
    local result = redis.call('HMGET', key, 'count', 'reset')
    local count, reset = tonumber(result[1]), tonumber(result[2])

    if (now >= reset) then
        local newReset = now + duration
        redis.call('HSET', key, 'count', 1, 'reset', newReset)
        redis.call('EXPIREAT', key, newReset)
        return {1, newReset}
    end
    redis.call("HINCRBY", key, 'count', 1)
    return {count + 1, reset}
end