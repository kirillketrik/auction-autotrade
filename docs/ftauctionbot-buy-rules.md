# `ftauctionbot-buy-rules.json`

Файл лежит по пути `run/config/ftauctionbot-buy-rules.json`.

## Общая структура

```json
{
  "scanIntervalSeconds": 15,
  "scanPageLimit": 6,
  "scanLogMode": "MATCHED_ONLY",
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
- `scanPageLimit`: сколько страниц аукциона сканировать за цикл.
- `scanLogMode`: режим логирования. Допустимые значения:
  - `MATCHED_ONLY`
  - `ALL`
- `buyRules`: список правил покупки.

## Поля правила

- `id`: технический идентификатор правила.
- `name`: отображаемое имя правила.
- `enabled`: `true` или `false`.
- `conditions`: массив условий. Если массив пустой, правило ничего не матчает.

## Поддерживаемые `conditions.type`

- `minecraft_id`
- `display_name_contains`
- `display_name_equals`
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

### `display_name_contains`

```json
{
  "type": "display_name_contains",
  "value": "Святая вода"
}
```

### `display_name_equals`

```json
{
  "type": "display_name_equals",
  "value": "[★] Святая вода"
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
  "scanPageLimit": 6,
  "scanLogMode": "MATCHED_ONLY",
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
          "type": "display_name_contains",
          "value": "Святая вода"
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
- Для списочных условий значения лежат внутри `value`.
- Для зелий `level` задается как реальный уровень эффекта из интерфейса игры.
- Для зелий `durationSeconds` задается в секундах, не в тиках.
