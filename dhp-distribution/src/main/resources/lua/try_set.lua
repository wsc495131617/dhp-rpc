local value1 = redis.call('get', KEYS[1]);
if value1 then
    if value1 == ARGV[2] then
        return redis.call('set', KEYS[1], ARGV[1])
    else
        return "0"
    end
else
    return redis.call('set', KEYS[1], ARGV[1])
end