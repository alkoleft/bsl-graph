#!/bin/bash

# Скрипт для загрузки конфигурации "УправлениеПаркомАттракционов" через консольное приложение

echo "🎢 === ЗАГРУЗКА КОНФИГУРАЦИИ 'УПРАВЛЕНИЕПАРКОМАТТРАКЦИОНОВ' ЧЕРЕЗ КОНСОЛЬНОЕ ПРИЛОЖЕНИЕ ==="
echo

# Проверяем, что Nebula Graph запущен
echo "🔍 Проверяем статус Nebula Graph..."
if docker-compose -f docker-compose-lite.yaml ps | grep -q "Up"; then
    echo "✅ Nebula Graph запущен"
else
    echo "❌ Nebula Graph не запущен"
    echo "🚀 Запускаем Nebula Graph..."
    docker-compose -f docker-compose-lite.yaml up -d
    
    echo "⏳ Ждем запуска сервисов..."
    sleep 15
    
    if docker-compose -f docker-compose-lite.yaml ps | grep -q "Up"; then
        echo "✅ Nebula Graph успешно запущен"
    else
        echo "❌ Не удалось запустить Nebula Graph"
        echo "Проверьте логи: docker-compose -f docker-compose-lite.yaml logs"
        exit 1
    fi
fi

echo

# Проверяем, что путь к конфигурации существует
CONFIG_PATH="/home/alko/develop/open-source/amusement-park-vibe/УправлениеПаркомАттракционов/src"
if [ -d "$CONFIG_PATH" ]; then
    echo "✅ Конфигурация найдена: $CONFIG_PATH"
else
    echo "❌ Конфигурация не найдена: $CONFIG_PATH"
    echo "Убедитесь, что путь к конфигурации правильный"
    exit 1
fi

# Собираем проект
echo "🔨 Собираем проект..."
if ./gradlew build -x test; then
    echo "✅ Проект собран успешно"
else
    echo "❌ Ошибка при сборке проекта"
    exit 1
fi

echo

# Проверяем подключение к Nebula Graph
echo "🔗 Тестируем подключение к Nebula Graph..."
echo

if java -jar build/libs/app.jar --test-nebula; then
    echo "✅ Подключение к Nebula Graph работает"
else
    echo "❌ Проблемы с подключением к Nebula Graph"
    echo "Проверьте логи и настройки подключения"
    exit 1
fi

echo

# Загружаем конфигурацию
echo "📥 Загружаем конфигурацию 'УправлениеПаркомАттракционов'..."
echo "Путь: $CONFIG_PATH"
echo

if java -jar build/libs/app.jar "$CONFIG_PATH"; then
    echo
    echo "🎉 === ЗАГРУЗКА ЗАВЕРШЕНА УСПЕШНО ==="
    echo
    echo "📊 Просматриваем статистику графа..."
    echo
    java -jar build/libs/app.jar --stats
    
    echo
    echo "✅ Конфигурация успешно загружена в Nebula Graph!"
    echo
    echo "💡 Теперь вы можете:"
    echo "   • Выполнять запросы к графу знаний"
    echo "   • Анализировать структуру конфигурации"
    echo "   • Искать связи между объектами метаданных"
    echo "   • Визуализировать данные"
    
else
    echo
    echo "❌ === ЗАГРУЗКА ЗАВЕРШЕНА С ОШИБКАМИ ==="
    echo
    echo "🔍 Возможные причины:"
    echo "   • Проблемы с подключением к Nebula Graph"
    echo "   • Ошибки в структуре конфигурации"
    echo "   • Недостаток памяти или ресурсов"
    echo
    echo "📋 Для диагностики:"
    echo "   • Проверьте логи: docker-compose -f docker-compose-lite.yaml logs"
    echo "   • Тестируйте подключение: java -jar build/libs/app.jar --test-nebula"
    echo "   • Проверьте доступность конфигурации"
fi

echo
echo "🏁 === СКРИПТ ЗАВЕРШЕН ==="
