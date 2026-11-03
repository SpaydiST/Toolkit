# Промпт: интеграция с ТНТ-шлемом из плагина Toolkit

Скопируй блок ниже в сессию Claude, которая работает над другим плагином.

---

Ты работаешь над Paper-плагином (Minecraft 1.21+, Java). Нужно интегрироваться с «ТНТ-шлемом» из плагина **Toolkit** (`org.example1.toolkit`). Исходники: `Toolkit/src/main/java/org/example1/toolkit/tnthat/TntHat.java`.

## Как устроен шлем
- Предмет: `Material.TNT`, на нём компонент `EQUIPPABLE` (слот HEAD, swappable), maxStackSize = 1, имя «ТНТ-шлем» (красное), лор «Взрывается при получении урона».
- **Маркер предмета:** PDC-ключ `toolkit:tnthead`, тип `PersistentDataType.BYTE`, значение `1`. Старые предметы имеют ключ `toolkit:tnt-hat` — Toolkit принимает оба. Для нового кода проверяй `toolkit:tnthead`, а `toolkit:tnt-hat` — только ради обратной совместимости.
- Крафт: бесшаблонный, любой `*_HELMET` + `TNT`. Ключ рецепта `toolkit:tnt-hat`.
- Поведение: когда игрок в шлеме получает урон (`EntityDamageEvent`, MONITOR, `getFinalDamage() > 0`, не отменён), шлем удаляется с головы, и на следующем тике на месте игрока спавнится `TNTPrimed`: fuse 0, yield 4.0, не зажигает, `source` = игрок.
- **Маркер взрыва:** у этого `TNTPrimed` PDC-ключ `toolkit:tnt-hat-explosion` (BYTE 1). Игроки, получившие урон от него, при `EntityKnockbackEvent` (cause EXPLOSION) получают отбрасывание ×2.

## Как проверять из другого плагина
```java
private static final NamespacedKey TNT_HEAD = new NamespacedKey("toolkit", "tnthead");

static boolean isTntHead(ItemStack item) {
    if (item == null || item.getType() != Material.TNT) return false;
    ItemMeta meta = item.getItemMeta();
    return meta != null && meta.getPersistentDataContainer().has(TNT_HEAD, PersistentDataType.BYTE);
}
```
Не добавляй compile-зависимость на Toolkit, если хватает PDC-проверки. Если нужен вызов `TntHat.isHat(ItemStack)` (он `public static`), объяви Toolkit как `compileOnly` и в `paper-plugin.yml` добавь зависимость (`load: BEFORE`, `required: false`), а при отсутствии Toolkit используй PDC-вариант.

## Что нужно сделать
1. Опиши, что именно должен делать мой плагин со шлемом (задай мне вопрос, если неясно): например, реагировать на надетый шлем, запрещать/разрешать эффекты, считать статистику, выдавать шлем.
2. Реализуй проверку надетого шлема через `player.getInventory().getHelmet()` + `isTntHead`.
3. Если нужно реагировать на взрыв шлема — слушай `EntityDamageByEntityEvent`/`EntityExplodeEvent` и проверяй PDC `toolkit:tnt-hat-explosion` на `TNTPrimed`. Помни, что шлем снимается в момент `MONITOR`-обработки урона, а взрыв происходит на следующем тике.
4. Не меняй поведение Toolkit и не копируй его логику взрыва.

## Известные нюансы
- Любой урон с `finalDamage > 0` (падение, яд, огонь, голод) активирует шлем.
- Взрыв ломает блоки (yield 4.0, как у TNT) и цепно активирует чужие ТНТ-шлемы поблизости.
- Если игрок гибнет от удара, шлем всё равно удаляется и не выпадает.
