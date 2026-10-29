// No Fabric or mod code is loaded here: this speaks the vanilla Minecraft protocol.
const mineflayer = require('mineflayer')
const nbt = require('prismarine-nbt')
const { Vec3 } = require('vec3')
const assert = require('node:assert/strict')
const phase = process.argv[2] || 'create'
const bot = mineflayer.createBot({ host: '127.0.0.1', port: Number(process.env.BICOLOR_TEST_PORT || 25576), username: 'VanillaProbe', auth: 'offline', version: '1.21.11' })
bot.on('message', message => { if (process.env.BICOLOR_DEBUG) console.log('CHAT', message.toString()) })
if (process.env.BICOLOR_DEBUG) {
  const write = bot._client.write.bind(bot._client)
  bot._client.write = (name, data) => { if (['block_place', 'player_loaded', 'entity_action'].includes(name)) console.log('SEND', name, JSON.stringify(data)); return write(name, data) }
}
const deadline = setTimeout(() => { console.error('Vanilla probe timed out'); process.exit(1) }, 120000)
bot.on('error', error => { console.error(error); process.exit(1) })
bot.on('kicked', reason => { console.error('Disconnected:', reason); process.exit(1) })
const owned = item => {
  const component = item?.componentMap?.get('custom_data')
  return component ? nbt.simplify(component.data)?.bicolor_shulkerboxes : undefined
}
async function waitFor (predicate, message, timeout = 10000) {
  const end = Date.now() + timeout
  while (Date.now() < end) { if (predicate()) return; await new Promise(resolve => setTimeout(resolve, 50)) }
  throw new Error(message)
}
async function command (command) { bot.chat('/' + command); await bot.waitForTicks(4) }
async function putOne (predicate, destination) {
  const window = bot.currentWindow || bot.inventory
  const source = window.slots.findIndex((item, slot) => slot >= window.inventoryStart && item && predicate(item))
  assert(source >= 0, 'Missing ingredient for slot ' + destination)
  await bot.clickWindow(source, 0, 0)
  await bot.clickWindow(destination, 1, 0)
  if (window.selectedItem) await bot.clickWindow(source, 0, 0)
  await bot.waitForTicks(2)
}
async function takeOutput () {
  const window = bot.currentWindow || bot.inventory
  await waitFor(() => window.slots[0], 'Server did not supply crafting result')
  await bot.clickWindow(0, 0, 0)
  const empty = window.firstEmptyInventorySlot()
  assert(empty >= 0, 'No space for output')
  await bot.clickWindow(empty, 0, 0)
  await bot.waitForTicks(3)
}
async function pickupBox () {
  await bot.dig(bot.blockAt(new Vec3(0, -60, 0)))
  await command('tp @s 0.5 -60 0.5')
  await waitFor(() => bot.inventory.items().some(item => owned(item)?.kind === 'box'), 'Dropped box lost its colors')
}
async function placeBox () {
  const box = bot.inventory.items().find(item => owned(item)?.kind === 'box')
  assert(box, 'Bicolor box missing')
  await command('tp @s 0.5 -60 2.5')
  await bot.equip(box, 'hand')
  await bot.placeBlock(bot.blockAt(new Vec3(0, -61, 0)), new Vec3(0, 1, 0))
  await bot.waitForTicks(3)
}
bot.once('spawn', async () => {
  try {
    await bot.waitForChunksToLoad()
    // Mineflayer does not yet send the 1.21.9+ load acknowledgement automatically.
    bot._client.write('player_loaded', {})
    await bot.waitForTicks(2)
    await command('tp @s 0.5 -60 2.5')
    if (phase === 'create') {
      await command('clear @s')
      await command('tp @s 0.5 -60 2.5')
      await command('setblock 1 -60 0 minecraft:crafting_table')
      for (const item of ['shulker_shell 2', 'red_dye', 'blue_dye', 'chest']) await command('give @s minecraft:' + item)
      for (const color of ['red', 'blue']) {
        await putOne(item => item.name === 'shulker_shell' && !owned(item), 1)
        await putOne(item => item.name === color + '_dye', 2)
        await takeOutput()
      }
      assert(bot.inventory.items().some(item => owned(item)?.color === 'red'))
      assert(bot.inventory.items().some(item => owned(item)?.color === 'blue'))
      const window = await bot.openBlock(bot.blockAt(new Vec3(1, -60, 0)))
      await putOne(item => owned(item)?.color === 'red', 1)
      await putOne(item => item.name === 'chest', 4)
      await putOne(item => owned(item)?.color === 'blue', 7)
      await takeOutput()
      bot.closeWindow(window)
      await bot.waitForTicks(3)
      const box = bot.inventory.items().find(item => owned(item)?.kind === 'box')
      assert.equal(box?.name, 'blue_shulker_box')
      assert.equal(owned(box).lid, 'red'); assert.equal(owned(box).base, 'blue')
      assert(box.componentMap.has('lore'), 'Vanilla tooltip missing')
      await placeBox()
      await command('give @s minecraft:diamond 37')
      const container = await bot.openContainer(bot.blockAt(new Vec3(0, -60, 0)))
      await container.deposit(bot.registry.itemsByName.diamond.id, null, 37)
      assert.equal(container.containerItems().find(item => item.name === 'diamond')?.count, 37)
      container.close()
      await pickupBox()
      await placeBox()
      console.log('PASS vanilla connection, shell dye, table crafting, tooltip, place, fill, break, pickup and replace')
    } else {
      assert.equal(bot.blockAt(new Vec3(0, -60, 0))?.name, 'blue_shulker_box', 'Restart lost block')
      await command('data get block 0 -60 0')
      console.log('RESTART BLOCK', bot.blockAt(new Vec3(0, -60, 0))?.name, 'POSITION', bot.entity.position, 'HEALTH', bot.health)
      const container = await bot.openContainer(bot.blockAt(new Vec3(0, -60, 0)))
      assert.equal(container.containerItems().find(item => item.name === 'diamond')?.count, 37, 'Restart lost contents')
      container.close()
      await pickupBox()
      const box = bot.inventory.items().find(item => owned(item)?.kind === 'box')
      assert.equal(owned(box).lid, 'red'); assert.equal(owned(box).base, 'blue')
      await command('give @s minecraft:green_dye')
      await putOne(item => owned(item)?.kind === 'box', 1)
      await putOne(item => item.name === 'green_dye', 2)
      await takeOutput()
      const green = bot.inventory.items().find(item => item.name === 'green_shulker_box')
      assert(green && !owned(green), 'Recolor did not remove bicolor metadata')
      assert(green.componentMap.has('container'), 'Recolor lost container data')
      console.log('PASS full server restart, retained colors and contents, and whole-box recoloring')
    }
    clearTimeout(deadline)
    bot.quit()
    setTimeout(() => process.exit(0), 300)
  } catch (error) {
    console.error(error)
    console.error('Inventory:', JSON.stringify(bot.inventory.items(), (_, value) => typeof value === 'bigint' ? value.toString() : value))
    bot.quit(); process.exit(1)
  }
})
