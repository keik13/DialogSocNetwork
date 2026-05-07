#!lua name=dialog_manager

-- Функция отправки сообщения
local function send_msg(keys, args)
    local dialog_id = args[1]
    local from_user = args[2]
    local to_user = args[3]
    local message = args[4]
    local ts = args[5]

    -- Сохраняем в Stream (имя ключа: dialog:ID)
    -- Мы добавляем сообщение и получаем его ID в Redis
    return redis.call('XADD', 'dialog:' .. dialog_id, '*',
        'from', from_user,
        'to', to_user,
        'msg', message,
        'ts', ts)
end

-- Функция получения истории
local function get_history(keys, args)
    local dialog_id = args[1]
    local count = args[2] or "50"

    -- Читаем последние сообщения
    return redis.call('XREVRANGE', 'dialog:' .. dialog_id, '+', '-', 'COUNT', count)
end

-- Регистрируем функции в библиотеке
redis.register_function('send_message', send_msg)
redis.register_function('get_messages', get_history)
