# `ftauctionbot-buy-rules.json`

Файл лежит по пути `run/config/ftauctionbot-buy-rules.json`.

## GUI редактор

Конфиг можно редактировать не только вручную, но и через встроенный GUI:

- `F7` открывает fullscreen editor поверх игры
- `/ftab gui` открывает тот же экран командой

GUI редактирует тот же самый файл `run/config/ftauctionbot-buy-rules.json`. Кнопка `Save` сохраняет JSON и сразу делает runtime reload, отдельный `/ftab reload` после этого не нужен.

## Общая структура

```json
{
  "scanIntervalSeconds": 15,
  "scanIntervalJitterSeconds": 2,
  "scanPageLimit": 6,
  "pageSwitchDelayMs": 200,
  "pageSwitchDelayJitterMs": 80,
  "scanLogMode": "MATCHED_ONLY",
  "antiAfkEnabled": true,
  "antiAfkActionIntervalSeconds": 7,
  "antiAfkJumpChancePercent": 20,
  "marketResearchTargetMarginPercent": 15,
  "marketResearchRiskBufferPercent": 5,
  "buyRules": [
    {
      "id": "rule-id",
      "name": "Человекочитаемое имя",
      "enabled": true,
      "conditions": [
        {
          "type": "minecraft_id",
          "minecraftId": "minecraft:splash_potion"
        }
      ]
    }
  ]
}
```

## Корневые поля

- `scanIntervalSeconds`: интервал между циклами autobuy в секундах.
- `scanIntervalJitterSeconds`: случайный разброс для интервала между циклами autobuy. Итоговая пауза считается как `scanIntervalSeconds ± scanIntervalJitterSeconds`.
- `scanPageLimit`: сколько страниц аукциона сканировать за цикл.
- `pageSwitchDelayMs`: базовая задержка между переключениями страниц при сканировании.
- `pageSwitchDelayJitterMs`: случайный разброс для задержки смены страниц. Итоговая задержка считается как `pageSwitchDelayMs ± pageSwitchDelayJitterMs`.
- `scanLogMode`: режим логирования. Допустимые значения:
  - `MATCHED_ONLY`
  - `ALL`
- `antiAfkEnabled`: включает анти-AFK действия во время паузы между циклами автобая.
- `antiAfkActionIntervalSeconds`: как часто во время паузы бот делает анти-AFK действие.
- `antiAfkJumpChancePercent`: вероятность прыжка при анти-AFK действии. Если прыжок не выбран, бот делает короткое случайное движение.
- `marketResearchTargetMarginPercent`: целевая маржа в процентах для рекомендаций покупки/продажи на вкладке исследования рынка.
- `marketResearchRiskBufferPercent`: дополнительный буфер риска в процентах для тех же рекомендаций.
- `buyRules`: список правил покупки.

## Поля правила

- `id`: технический идентификатор правила.
- `name`: отображаемое имя правила.
- `enabled`: `true` или `false`.
- `conditions`: массив условий. Если массив пустой, правило ничего не матчает.

## Поддерживаемые `conditions.type`

- `minecraft_id`
- `max_total_price`
- `max_unit_price`
- `min_count`
- `max_count`
- `required_enchantments`
- `required_potion_effects`
- `seller_allow_list`
- `seller_deny_list`

## Формат каждого условия

### `minecraft_id`

```json
{
  "type": "minecraft_id",
  "minecraftId": "minecraft:splash_potion"
}
```

### `max_total_price`

```json
{
  "type": "max_total_price",
  "value": 700000
}
```

### `max_unit_price`

```json
{
  "type": "max_unit_price",
  "value": 550000
}
```

### `min_count`

```json
{
  "type": "min_count",
  "value": 1
}
```

### `max_count`

```json
{
  "type": "max_count",
  "value": 16
}
```

### `required_enchantments`

Важно: тип называется именно `required_enchantments`, во множественном числе.

```json
{
  "type": "required_enchantments",
  "value": [
    {
      "id": "minecraft:sharpness",
      "level": 3
    },
    {
      "id": "minecraft:unbreaking",
      "level": 2
    }
  ]
}
```

### `required_potion_effects`

Важно: `durationSeconds` задается в секундах.

```json
{
  "type": "required_potion_effects",
  "value": [
    {
      "id": "minecraft:regeneration",
      "level": 2,
      "durationSeconds": 45
    },
    {
      "id": "minecraft:invisibility",
      "level": 2,
      "durationSeconds": 600
    }
  ]
}
```

### `seller_allow_list`

```json
{
  "type": "seller_allow_list",
  "value": ["trusted_seller_1", "trusted_seller_2"]
}
```

### `seller_deny_list`

```json
{
  "type": "seller_deny_list",
  "value": ["bad_seller_1", "bad_seller_2"]
}
```

## Полный пример

```json
{
  "scanIntervalSeconds": 15,
  "scanIntervalJitterSeconds": 2,
  "scanPageLimit": 6,
  "pageSwitchDelayMs": 200,
  "pageSwitchDelayJitterMs": 80,
  "scanLogMode": "MATCHED_ONLY",
  "antiAfkEnabled": true,
  "antiAfkActionIntervalSeconds": 7,
  "antiAfkJumpChancePercent": 20,
  "marketResearchTargetMarginPercent": 15,
  "marketResearchRiskBufferPercent": 5,
  "buyRules": [
    {
      "id": "holy-water",
      "name": "Святая вода",
      "enabled": true,
      "conditions": [
        {
          "type": "minecraft_id",
          "minecraftId": "minecraft:splash_potion"
        },
        {
          "type": "max_unit_price",
          "value": 550000
        },
        {
          "type": "required_potion_effects",
          "value": [
            {
              "id": "minecraft:regeneration",
              "level": 2,
              "durationSeconds": 45
            },
            {
              "id": "minecraft:invisibility",
              "level": 2,
              "durationSeconds": 600
            },
            {
              "id": "minecraft:instant_health",
              "level": 2,
              "durationSeconds": 0
            }
          ]
        }
      ]
    },
    {
      "id": "emerald-sword",
      "name": "Изумрудный меч",
      "enabled": true,
      "conditions": [
        {
          "type": "minecraft_id",
          "minecraftId": "minecraft:diamond_sword"
        },
        {
          "type": "required_enchantments",
          "value": [
            {
              "id": "minecraft:sharpness",
              "level": 3
            }
          ]
        }
      ]
    }
  ]
}
```

## Частые ошибки

- Нельзя использовать старый flat-формат правила без массива `conditions`.
- Нельзя писать `required_enchantment`. Нужно `required_enchantments`.
- Для поиска `/ah search` используется поле `name` у правила.
- Рекомендации на вкладке исследования рынка используют глобальные `marketResearchTargetMarginPercent` и `marketResearchRiskBufferPercent`.
- Для списочных условий значения лежат внутри `value`.
- Для зелий `level` задается как реальный уровень эффекта из интерфейса игры.
- Для зелий `durationSeconds` задается в секундах, не в тиках.
