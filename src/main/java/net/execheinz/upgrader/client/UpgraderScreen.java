package net.execheinz.upgrader.client;

import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.execheinz.upgrader.UpgraderConfig;
import net.execheinz.upgrader.logic.MotionCurve;
import net.execheinz.upgrader.logic.ResultAmountPolicy;
import net.execheinz.upgrader.logic.UpgradeMode;
import net.execheinz.upgrader.menu.UpgraderMenu;
import net.execheinz.upgrader.network.ModNetwork;
import net.execheinz.upgrader.network.packet.ClientboundResultPacket;
import net.execheinz.upgrader.network.packet.ClientboundSyncPacket;
import net.execheinz.upgrader.network.packet.ServerboundModePacket;
import net.execheinz.upgrader.network.packet.ServerboundSetTargetPacket;
import net.execheinz.upgrader.network.packet.ServerboundStartPacket;
import net.execheinz.upgrader.registry.ModSounds;
import net.execheinz.upgrader.value.TargetCatalog;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.registries.ForgeRegistries;

public final class UpgraderScreen extends AbstractContainerScreen<UpgraderMenu> {
    private static final int WIDTH = 336;
    private static final int HEIGHT = 240;
    private static final int ROOT_BORDER = 0xFF273246;
    private static final int ROOT_TOP = 0xFF111722;
    private static final int ROOT_BOTTOM = 0xFF070A10;
    private static final int CARD_BORDER = 0xFF40506A;
    private static final int CARD_TOP = 0xFF202A3A;
    private static final int CARD_BOTTOM = 0xFF0C1119;
    private static final int PANEL = 0xFF0D131D;
    private static final int GOLD = 0xFFFFCA35;
    private static final int GOLD_LIGHT = 0xFFFFED9A;
    private static final int GREEN = 0xFF42D889;
    private static final int RED = 0xFFF05C63;
    private static final int TEXT = 0xFFF0F3F9;
    private static final int DIM = 0xFF8D9AAF;
    private static final int GRID_COLS = 8;
    private static final int GRID_ROWS = 6;
    private static final int GRID_PAGE_SIZE = GRID_COLS * GRID_ROWS;

    private final List<AbstractWidget> mainWidgets = new ArrayList<>();
    private CasinoButton upgradeButton;
    private EditBox search;
    private EditBox quantity;
    private List<TargetCatalog.CatalogEntry> filtered = List.of();
    private final Map<TargetCatalog.CatalogEntry, ItemPreview> previewCache = new IdentityHashMap<>();
    private ItemPreview preview = ItemPreview.EMPTY;
    private boolean pickerOpen;
    private boolean booksOnly;
    private boolean quantityOpen;
    private boolean awaitingModeTarget;
    private ItemStack pendingTarget = ItemStack.EMPTY;
    private int scrollRow;
    private long spinStart;
    private long spinDuration;
    private float spinTotalAngle;
    private float spinChance;
    private boolean spinning;
    private boolean resultSuccess;
    private long flashUntil;
    private long celebrationStart;
    private String cachedWagerName = "";
    private List<String> cachedWagerNameLines = List.of();
    private String cachedTargetName = "";
    private List<String> cachedTargetNameLines = List.of();

    public UpgraderScreen(UpgraderMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = WIDTH;
        imageHeight = HEIGHT;
        inventoryLabelX = UpgraderMenu.INVENTORY_X;
        inventoryLabelY = 150;
    }

    @Override
    protected void init() {
        super.init();
        mainWidgets.clear();
        upgradeButton = addRenderableWidget(new CasinoButton(leftPos + 112, topPos + 104, 112, 25,
            Component.translatable("gui.upgrader.upgrade"), CasinoButton.Style.PRIMARY,
            ignored -> ModNetwork.sendToServer(new ServerboundStartPacket(UUID.randomUUID()))));
        mainWidgets.add(upgradeButton);

        for (int i = 0; i < UpgradeMode.values().length; i++) {
            UpgradeMode mode = UpgradeMode.values()[i];
            CasinoButton button = addRenderableWidget(new CasinoButton(leftPos + 18 + i * 50, topPos + 130, 48, 19,
                Component.literal(mode.label()), CasinoButton.Style.CHIP, ignored -> {
                    awaitingModeTarget = true;
                    ModNetwork.sendToServer(new ServerboundModePacket(mode));
                }));
            mainWidgets.add(button);
        }

        search = new EditBox(font, leftPos + 20, topPos + 21, 296, 17, Component.translatable("gui.upgrader.search"));
        search.setMaxLength(80);
        search.setResponder(value -> { scrollRow = 0; refilter(); });
        search.setVisible(false);
        addRenderableWidget(search);

        quantity = new EditBox(font, leftPos + 137, topPos + 132, 62, 18, Component.translatable("gui.upgrader.quantity"));
        quantity.setFilter(this::validQuantityInput);
        quantity.setMaxLength(2);
        quantity.setVisible(false);
        addWidget(quantity);
    }

    public void onSync(ClientboundSyncPacket packet) {
        if (awaitingModeTarget && !packet.target().isEmpty() && !packet.processing()) {
            awaitingModeTarget = false;
            if (ResultAmountPolicy.needsPopup(packet.target())) openQuantity(packet.target());
        }
    }

    public void onResult(ClientboundResultPacket packet) {
        spinning = true;
        resultSuccess = packet.success();
        spinChance = packet.chance();
        spinStart = System.currentTimeMillis();
        spinDuration = Math.max(1L, packet.durationTicks() * 50L);
        spinTotalAngle = 360.0F * 7.0F + packet.landingAngle();
        flashUntil = 0L;
        celebrationStart = 0L;
        closeOverlays();
        playUi(ModSounds.WHEEL_START.get(), 1.0F, 0.60F);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (!spinning) return;
        float progress = progress();
        if (progress >= 1.0F) {
            spinning = false;
            long now = System.currentTimeMillis();
            flashUntil = now + (resultSuccess ? 2600L : 1900L);
            celebrationStart = resultSuccess ? now : 0L;
            playUi(resultSuccess ? ModSounds.UPGRADE_SUCCESS.get() : ModSounds.UPGRADE_FAILURE.get(), 1.0F, resultSuccess ? 0.70F : 0.52F);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        updateWidgets();
        super.render(graphics, mouseX, mouseY, partialTick);
        if (!pickerOpen && !quantityOpen) renderTooltip(graphics, mouseX, mouseY);
    }

    private void updateWidgets() {
        boolean overlay = pickerOpen || quantityOpen;
        menu.setClientSlotsVisible(!overlay);
        boolean enabled = !overlay && !menu.processing() && !spinning;
        for (AbstractWidget widget : mainWidgets) {
            widget.visible = !overlay;
            widget.active = enabled && !menu.inputStack().isEmpty();
        }
        upgradeButton.active = enabled && !menu.inputStack().isEmpty() && !menu.targetStack().isEmpty();
        search.setVisible(pickerOpen);
        search.setEditable(pickerOpen);
        quantity.setVisible(quantityOpen);
        quantity.setEditable(quantityOpen);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        // The picker covers the entire menu. Do not render the wheel, inventory,
        // cards and their item models underneath an opaque overlay.
        if (pickerOpen) {
            renderPicker(graphics, mouseX, mouseY);
            return;
        }
        boolean flashing = System.currentTimeMillis() < flashUntil;
        if (flashing) CasinoRenderer.glow(graphics, leftPos, topPos, WIDTH, HEIGHT, 8, resultSuccess ? 0x8042D889 : 0x70F05C63);
        CasinoRenderer.card(graphics, leftPos, topPos, WIDTH, HEIGHT, 8, ROOT_BORDER, ROOT_TOP, ROOT_BOTTOM);
        CasinoRenderer.cornerBrackets(graphics, leftPos + 2, topPos + 2, WIDTH - 4, HEIGHT - 4, 0x804D607C);
        graphics.fillGradient(leftPos + 3, topPos + 3, leftPos + WIDTH - 3, topPos + 18, 0xB3222C3D, 0x00101620);
        CasinoRenderer.accentRail(graphics, leftPos + 7, topPos + 18, WIDTH - 14, 0xB0FFBE24);
        CasinoRenderer.diagonalWatermark(graphics, leftPos + 5, topPos + 22, WIDTH - 10, 91, 0x0D8190A8);
        CasinoRenderer.chevrons(graphics, leftPos + 133, topPos + 11, 3, 0x70FFBC22);
        CasinoRenderer.chevrons(graphics, leftPos + 184, topPos + 11, 3, 0x70FFBC22);
        CasinoRenderer.roundedGradient(graphics, leftPos + 13, topPos + 126, WIDTH - 26, 27, 5, 0xB0182130, 0xC0080C12);
        CasinoRenderer.cornerBrackets(graphics, leftPos + 15, topPos + 128, WIDTH - 30, 23, 0x30485A75);
        if (celebrationStart > 0L) {
            CasinoFireworks.render(graphics, leftPos, topPos, System.currentTimeMillis() - celebrationStart);
        }
        renderItemCard(graphics, leftPos + 10, topPos + 25, true);
        renderItemCard(graphics, leftPos + 232, topPos + 25, false);
        renderInventorySlots(graphics);
        renderWheel(graphics);
        if (quantityOpen) renderQuantity(graphics);
    }

    private void renderItemCard(GuiGraphics graphics, int x, int y, boolean wager) {
        int accent = wager ? 0x704B5F7B : 0xC0FFC126;
        CasinoRenderer.raisedPanel(graphics, x, y, 94, 85, 6, CARD_BORDER, CARD_TOP, CARD_BOTTOM, accent);
        CasinoRenderer.diagonalWatermark(graphics, x + 2, y + 18, 90, 64, 0x0F91A0B7);
        CasinoRenderer.cornerBrackets(graphics, x + 3, y + 3, 88, 79, wager ? 0x305E7190 : 0x65FFD05A);
        Component label = Component.translatable(wager ? "gui.upgrader.wager" : "gui.upgrader.target");
        graphics.drawCenteredString(font, label, x + 47, y + 5, wager ? TEXT : GOLD_LIGHT);
        graphics.fill(x + 18, y + 17, x + 76, y + 18, wager ? 0x50394A62 : 0x70A77A12);
        int slotX = leftPos + (wager ? UpgraderMenu.INPUT_X : UpgraderMenu.TARGET_X);
        int slotY = topPos + (wager ? UpgraderMenu.INPUT_Y : UpgraderMenu.TARGET_Y);
        slot(graphics, slotX, slotY, !wager && !menu.targetStack().isEmpty());
        ItemStack stack = wager ? menu.inputStack() : menu.targetStack();
        if (!wager && !stack.isEmpty()) graphics.renderItem(stack, slotX, slotY);
        if (!stack.isEmpty()) {
            List<String> nameLines = cardNameLines(wager, stack.getHoverName().getString());
            int nameY = nameLines.size() == 1 ? y + 47 : y + 42;
            for (int line = 0; line < nameLines.size(); line++) {
                drawCenteredFitted(graphics, Component.literal(nameLines.get(line)), x + 47, nameY + line * 9, 86, TEXT, 0.76F);
            }
            if (wager) {
                drawCenteredFitted(graphics, Component.translatable("gui.upgrader.value_short", format(menu.inputValue())), x + 47, y + 69, 86, DIM, 0.66F);
            } else {
                drawCenteredFitted(graphics, Component.translatable("gui.upgrader.unit_value", format(menu.unitValue())), x + 47, y + 60, 86, DIM, 0.66F);
                drawCenteredFitted(graphics, Component.translatable("gui.upgrader.amount", menu.targetAmount()), x + 47, y + 68, 86, DIM, 0.66F);
                drawCenteredFitted(graphics, Component.translatable("gui.upgrader.total", format(menu.totalValue())), x + 47, y + 76, 86, GOLD, 0.66F);
            }
        } else {
            graphics.drawCenteredString(font, Component.translatable(wager ? "gui.upgrader.pick_input" : "gui.upgrader.pick_target"), x + 47, y + 55, DIM);
        }
    }

    private List<String> cardNameLines(boolean wager, String name) {
        String cachedName = wager ? cachedWagerName : cachedTargetName;
        if (name.equals(cachedName)) return wager ? cachedWagerNameLines : cachedTargetNameLines;
        List<String> lines = splitCardName(name, 86);
        if (wager) {
            cachedWagerName = name;
            cachedWagerNameLines = lines;
        } else {
            cachedTargetName = name;
            cachedTargetNameLines = lines;
        }
        return lines;
    }

    private List<String> splitCardName(String name, int maxWidth) {
        if (font.width(name) <= maxWidth) return List.of(name);
        String firstFit = font.plainSubstrByWidth(name, maxWidth);
        int split = firstFit.lastIndexOf(' ');
        if (split < Math.max(3, firstFit.length() / 2)) split = firstFit.length();
        String first = name.substring(0, split).stripTrailing();
        String rest = name.substring(split).stripLeading();
        if (font.width(rest) > maxWidth) {
            String ellipsis = "…";
            rest = font.plainSubstrByWidth(rest, Math.max(1, maxWidth - font.width(ellipsis))).stripTrailing() + ellipsis;
        }
        return rest.isEmpty() ? List.of(first) : List.of(first, rest);
    }

    private void drawCenteredFitted(GuiGraphics graphics, Component text, int centerX, int y, int maxWidth, int color, float minimumScale) {
        int textWidth = font.width(text);
        float scale = textWidth <= maxWidth ? 1.0F : Math.max(minimumScale, maxWidth / (float) Math.max(1, textWidth));
        graphics.pose().pushPose();
        graphics.pose().translate(centerX, y, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(font, text, -textWidth / 2, 0, color, false);
        graphics.pose().popPose();
    }

    private void renderWheel(GuiGraphics graphics) {
        int cx = leftPos + WIDTH / 2;
        int cy = topPos + 58;
        float chance = spinning ? spinChance : menu.chance();
        boolean flashing = System.currentTimeMillis() < flashUntil;
        if (flashing && resultSuccess) CasinoRenderer.glow(graphics, cx - 44, cy - 44, 88, 88, 44, 0x8042D889);
        CasinoRenderer.disc(graphics, cx, cy, 45, 0xFF1B2636);
        CasinoRenderer.tickRing(graphics, cx, cy, 43, 47, 0xFF455671, 0xFFFFC62B);
        CasinoRenderer.disc(graphics, cx, cy, 41, 0xFF080C12);
        CasinoRenderer.arc(graphics, cx, cy, 31, 39, -90.0F, 360.0F, 0xFF303B50);
        CasinoRenderer.arc(graphics, cx, cy, 31, 39, -90.0F, Math.max(1.2F, chance * 360.0F), flashing && resultSuccess ? GREEN : GOLD);
        CasinoRenderer.disc(graphics, cx, cy, 28, 0xFF111823);
        float needleAngle = -90.0F + (spinning ? spinTotalAngle * MotionCurve.progress(progress()) : 0.0F);
        CasinoRenderer.needle(graphics, cx, cy, needleAngle, GOLD);
        // The rotating result marker must never cross the chance/status text.
        // Mask its inner part and render the text on a layer above the marker.
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 20.0F);
        CasinoRenderer.disc(graphics, cx, cy, 30, 0xFF111823);
        int chanceColor = chance >= 0.66F ? GREEN : chance >= 0.34F ? GOLD_LIGHT : RED;
        graphics.drawCenteredString(font, String.format(Locale.ROOT, "%.2f%%", chance * 100.0F), cx, cy - 9, chanceColor);
        Component status = spinning ? Component.translatable("gui.upgrader.processing").withStyle(ChatFormatting.YELLOW)
            : flashing ? Component.translatable(resultSuccess ? "gui.upgrader.success" : "gui.upgrader.failure").withStyle(resultSuccess ? ChatFormatting.GREEN : ChatFormatting.RED)
            : Component.translatable("gui.upgrader.chance");
        graphics.drawCenteredString(font, status, cx, cy + 5, DIM);
        graphics.pose().popPose();
    }

    private void renderInventorySlots(GuiGraphics graphics) {
        int x = leftPos + UpgraderMenu.INVENTORY_X - 6;
        int y = topPos + UpgraderMenu.INVENTORY_Y - 6;
        CasinoRenderer.raisedPanel(graphics, x, y, 174, 84, 5, 0xFF29374B, 0xCC121A26, 0xD9080C12, 0x6041536D);
        CasinoRenderer.accentRail(graphics, x + 7, y + 2, 160, 0x60FFC52C);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) slot(graphics, leftPos + UpgraderMenu.INVENTORY_X + col * 18, topPos + UpgraderMenu.INVENTORY_Y + row * 18, false);
        }
        for (int col = 0; col < 9; col++) slot(graphics, leftPos + UpgraderMenu.INVENTORY_X + col * 18, topPos + UpgraderMenu.INVENTORY_Y + 58, false);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        if (pickerOpen || quantityOpen) return;
        graphics.drawCenteredString(font, Component.literal("UPGRADER"), WIDTH / 2 + 1, 3, 0x702D2104);
        graphics.drawCenteredString(font, Component.literal("UPGRADER"), WIDTH / 2, 2, GOLD);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, DIM, false);
    }

    private void openPicker() {
        if (menu.processing() || spinning || minecraft == null || minecraft.level == null) return;
        pickerOpen = true;
        booksOnly = false;
        scrollRow = 0;
        search.setValue("");
        setFocused(search);
        refilter();
    }

    private void refilter() {
        if (minecraft == null || minecraft.level == null || !pickerOpen) return;
        String needle = search.getValue().toLowerCase(Locale.ROOT);
        List<TargetCatalog.CatalogEntry> result = new ArrayList<>();
        for (TargetCatalog.CatalogEntry entry : TargetCatalog.INSTANCE.entries(minecraft.level)) {
            if (booksOnly && !(entry.stack().getItem() instanceof EnchantedBookItem)) continue;
            if (needle.isBlank() || entry.searchKey().contains(needle)) result.add(entry);
        }
        filtered = List.copyOf(result);
        scrollRow = 0;
        setPreview(filtered.isEmpty() ? null : filtered.get(0));
    }

    private void renderPicker(GuiGraphics graphics, int mouseX, int mouseY) {
        CasinoRenderer.glow(graphics, leftPos + 8, topPos + 8, 320, 222, 7, 0x502D3B51);
        CasinoRenderer.raisedPanel(graphics, leftPos + 8, topPos + 8, 320, 222, 7, CARD_BORDER, ROOT_TOP, ROOT_BOTTOM, 0xA0FFC42C);
        CasinoRenderer.diagonalWatermark(graphics, leftPos + 11, topPos + 43, 310, 165, 0x0D8190A8);
        CasinoRenderer.accentRail(graphics, leftPos + 17, topPos + 40, 300, 0x90FFC42C);
        CasinoRenderer.chevrons(graphics, leftPos + 151, topPos + 12, 5, 0x70FFC42C);
        graphics.drawString(font, booksOnly ? Component.translatable("gui.upgrader.books") : Component.translatable("gui.upgrader.all_items"), leftPos + 19, topPos + 44, TEXT, false);
        CasinoRenderer.card(graphics, leftPos + 249, topPos + 41, 67, 15, 4, booksOnly ? GOLD : CARD_BORDER, CARD_TOP, CARD_BOTTOM);
        graphics.drawCenteredString(font, Component.translatable(booksOnly ? "gui.upgrader.show_all" : "gui.upgrader.books_short"), leftPos + 282, topPos + 45, booksOnly ? GOLD_LIGHT : DIM);

        int first = scrollRow * GRID_PAGE_SIZE;
        TargetCatalog.CatalogEntry hovered = null;
        for (int i = 0; i < GRID_PAGE_SIZE && first + i < filtered.size(); i++) {
            int x = leftPos + 18 + i % GRID_COLS * 21;
            int y = topPos + 60 + i / GRID_COLS * 20;
            boolean over = inBox(mouseX, mouseY, x, y, 18, 18);
            pickerSlot(graphics, x, y, over);
            TargetCatalog.CatalogEntry entry = filtered.get(first + i);
            graphics.renderItem(entry.stack(), x, y);
            if (over) hovered = entry;
        }
        if (hovered != null) setPreview(hovered);
        renderItemPreview(graphics);
        int pages = pageCount();
        graphics.drawCenteredString(font, Component.translatable("gui.upgrader.picker_footer", filtered.size(), scrollRow + 1, pages), leftPos + WIDTH / 2, topPos + 220, DIM);
    }

    private void pickerSlot(GuiGraphics graphics, int x, int y, boolean hovered) {
        // Two flat quads instead of a full rounded gradient per catalogue cell.
        graphics.fill(x - 1, y - 1, x + 18, y + 18, hovered ? GOLD : CARD_BORDER);
        graphics.fill(x, y, x + 17, y + 17, hovered ? 0xFF343E50 : 0xFF121925);
    }

    private int pageCount() {
        return Math.max(1, (filtered.size() + GRID_PAGE_SIZE - 1) / GRID_PAGE_SIZE);
    }

    private void setPreview(TargetCatalog.CatalogEntry entry) {
        preview = entry == null ? ItemPreview.EMPTY : previewCache.computeIfAbsent(entry, this::buildPreview);
    }

    private ItemPreview buildPreview(TargetCatalog.CatalogEntry entry) {
        List<FormattedCharSequence> names = limited(font.split(Component.literal(entry.displayName()), 116), 2);
        List<FormattedCharSequence> details = new ArrayList<>();
        ItemStack stack = entry.stack();
        try {
            if (stack.getItem() instanceof ArmorItem armor) {
                addDetail(details, Component.translatable("gui.upgrader.stat.armor", formatDecimal(armor.getDefense())));
                addDetail(details, Component.translatable("gui.upgrader.stat.toughness", formatDecimal(armor.getToughness())));
                double resistance = armor.getMaterial().getKnockbackResistance();
                if (resistance > 0.0D) addDetail(details, Component.translatable("gui.upgrader.stat.knockback", formatDecimal(resistance * 100.0D)));
            }

            Multimap<Attribute, AttributeModifier> modifiers = stack.getAttributeModifiers(EquipmentSlot.MAINHAND);
            if (!modifiers.get(Attributes.ATTACK_DAMAGE).isEmpty()) {
                double damage = attributeValue(modifiers, Attributes.ATTACK_DAMAGE, 1.0D);
                double speed = attributeValue(modifiers, Attributes.ATTACK_SPEED, 4.0D);
                addDetail(details, Component.translatable("gui.upgrader.stat.damage", formatDecimal(damage)));
                addDetail(details, Component.translatable("gui.upgrader.stat.speed", formatDecimal(speed)));
                addDetail(details, Component.translatable("gui.upgrader.stat.dps", formatDecimal(damage * Math.max(0.0D, speed))));
            }
            if (stack.isDamageableItem()) {
                addDetail(details, Component.translatable("gui.upgrader.stat.durability", stack.getMaxDamage()));
            }
        } catch (RuntimeException | LinkageError ignored) {
            // A modded item with broken attributes should still remain selectable.
        }

        // Build third-party descriptions once on hover, not once per rendered frame.
        try {
            List<Component> tooltip = stack.getTooltipLines(minecraft == null ? null : minecraft.player, TooltipFlag.Default.NORMAL);
            for (int i = 1; i < tooltip.size() && details.size() < 8; i++) {
                Component line = tooltip.get(i);
                String plain = line.getString().trim();
                if (plain.isEmpty() || isRedundantAttributeLine(plain)) continue;
                addDetail(details, line);
            }
        } catch (RuntimeException | LinkageError ignored) {}
        return new ItemPreview(entry, names, List.copyOf(details));
    }

    private void addDetail(List<FormattedCharSequence> details, Component component) {
        for (FormattedCharSequence line : font.split(component, 116)) {
            if (details.size() >= 8) return;
            details.add(line);
        }
    }

    private void renderItemPreview(GuiGraphics graphics) {
        int x = leftPos + 190;
        int y = topPos + 59;
        int width = 128;
        CasinoRenderer.raisedPanel(graphics, x, y, width, 150, 5, CARD_BORDER, 0xFF192231, 0xFF0D131D, 0x70FFC42C);
        CasinoRenderer.cornerBrackets(graphics, x + 2, y + 2, width - 4, 146, 0x355E7190);
        if (preview == ItemPreview.EMPTY) {
            graphics.drawCenteredString(font, Component.translatable("gui.upgrader.preview.hover"), x + width / 2, y + 67, DIM);
            return;
        }

        graphics.renderItem(preview.entry.stack(), x + 56, y + 6);
        int nameY = y + 25;
        for (FormattedCharSequence line : preview.nameLines) {
            graphics.drawString(font, line, x + (width - font.width(line)) / 2, nameY, TEXT, false);
            nameY += 9;
        }
        graphics.drawCenteredString(font, Component.translatable("gui.upgrader.value_short", format(Math.round(preview.entry.unitValue()))), x + width / 2, y + 47, GOLD_LIGHT);
        int separatorOffset = 70;
        if (menu.inputValue() > 0L) {
            double chance = menu.previewChance(preview.entry.unitValue());
            drawCenteredWrapped(graphics, Component.translatable("gui.upgrader.preview_chance", String.format(Locale.ROOT, "%.2f", chance * 100.0D)), x, width, y + 58, chance >= 0.34D ? GREEN : RED, 1);
        } else {
            drawCenteredWrapped(graphics, Component.translatable("gui.upgrader.preview_no_wager"), x, width, y + 55, DIM, 2);
            separatorOffset = 76;
        }
        graphics.fill(x + 6, y + separatorOffset, x + width - 6, y + separatorOffset + 1, 0xFF35445C);
        int detailY = y + separatorOffset + 5;
        for (FormattedCharSequence line : preview.detailLines) {
            if (detailY > y + 140) break;
            graphics.drawString(font, line, x + 6, detailY, DIM, false);
            detailY += 9;
        }
    }

    private void drawCenteredWrapped(GuiGraphics graphics, Component text, int x, int width, int y, int color, int maxLines) {
        List<FormattedCharSequence> lines = limited(font.split(text, width - 12), maxLines);
        for (FormattedCharSequence line : lines) {
            graphics.drawString(font, line, x + (width - font.width(line)) / 2, y, color, false);
            y += 9;
        }
    }

    private static double attributeValue(Multimap<Attribute, AttributeModifier> modifiers, Attribute attribute, double base) {
        double additive = 0.0D;
        double multiplyBase = 0.0D;
        double multiplyTotal = 1.0D;
        for (AttributeModifier modifier : modifiers.get(attribute)) {
            if (modifier.getOperation() == AttributeModifier.Operation.ADDITION) additive += modifier.getAmount();
            else if (modifier.getOperation() == AttributeModifier.Operation.MULTIPLY_BASE) multiplyBase += modifier.getAmount();
            else if (modifier.getOperation() == AttributeModifier.Operation.MULTIPLY_TOTAL) multiplyTotal *= 1.0D + modifier.getAmount();
        }
        return Math.max(0.0D, (base + additive + base * multiplyBase) * multiplyTotal);
    }

    private static boolean isRedundantAttributeLine(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        return lower.startsWith("when in ") || lower.startsWith("когда в ") || lower.contains("attack damage")
            || lower.contains("attack speed") || lower.contains("урон") || lower.contains("скорость атаки")
            || lower.contains("armor toughness") || lower.contains("прочность брони");
    }

    private static String formatDecimal(double value) {
        return Math.abs(value - Math.rint(value)) < 0.001D ? Long.toString(Math.round(value)) : String.format(Locale.ROOT, "%.2f", value);
    }

    private static <T> List<T> limited(List<T> source, int max) {
        return source.size() <= max ? List.copyOf(source) : List.copyOf(source.subList(0, max));
    }

    private record ItemPreview(TargetCatalog.CatalogEntry entry, List<FormattedCharSequence> nameLines,
                               List<FormattedCharSequence> detailLines) {
        private static final ItemPreview EMPTY = new ItemPreview(null, List.of(), List.of());
    }

    private void openQuantity(ItemStack target) {
        if (!ResultAmountPolicy.needsPopup(target)) {
            sendTarget(target, 1);
            closeOverlays();
            return;
        }
        pickerOpen = false;
        pendingTarget = target.copy();
        quantityOpen = true;
        quantity.setValue(Integer.toString(Mth.clamp(menu.targetAmount(), 1, maxResultAmount())));
        setFocused(quantity);
    }

    private void renderQuantity(GuiGraphics graphics) {
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 600.0F);
        CasinoRenderer.glow(graphics, leftPos + 91, topPos + 65, 154, 112, 7, 0x60FFD34E);
        CasinoRenderer.raisedPanel(graphics, leftPos + 91, topPos + 65, 154, 112, 7, GOLD, CARD_TOP, ROOT_BOTTOM, 0xD0FFC42C);
        CasinoRenderer.cornerBrackets(graphics, leftPos + 94, topPos + 68, 148, 106, 0x90FFE17A);
        CasinoRenderer.chevrons(graphics, leftPos + 151, topPos + 69, 5, 0x60FFC42C);
        graphics.drawCenteredString(font, Component.translatable("gui.upgrader.quantity_title"), leftPos + 168, topPos + 75, TEXT);
        graphics.renderItem(pendingTarget, leftPos + 160, topPos + 89);
        graphics.drawCenteredString(font, font.plainSubstrByWidth(pendingTarget.getHoverName().getString(), 138), leftPos + 168, topPos + 105, GOLD_LIGHT);
        quantityControl(graphics, leftPos + 104, topPos + 133, 28, "−");
        CasinoRenderer.card(graphics, leftPos + 137, topPos + 133, 62, 15, 4, GOLD, 0xFF283244, 0xFF171E29);
        String amount = quantity.getValue().isEmpty() ? "1" : quantity.getValue();
        graphics.drawCenteredString(font, amount, leftPos + 168, topPos + 137, TEXT);
        if (quantity.isFocused() && (System.currentTimeMillis() / 450L & 1L) == 0L) {
            int cursorX = leftPos + 169 + font.width(amount) / 2;
            graphics.fill(cursorX, topPos + 136, cursorX + 1, topPos + 146, GOLD_LIGHT);
        }
        quantityControl(graphics, leftPos + 204, topPos + 133, 28, "+");
        quantityControl(graphics, leftPos + 104, topPos + 155, 47, "MAX");
        CasinoRenderer.card(graphics, leftPos + 157, topPos + 155, 75, 17, 4, GOLD, 0xFFFFD657, 0xFFD89C0B);
        graphics.drawCenteredString(font, Component.translatable("gui.upgrader.confirm"), leftPos + 194, topPos + 160, 0xFF171A21);
        graphics.pose().popPose();
    }

    private void quantityControl(GuiGraphics graphics, int x, int y, int width, String text) {
        CasinoRenderer.card(graphics, x, y, width, 15, 4, CARD_BORDER, 0xFF283244, 0xFF171E29);
        graphics.drawCenteredString(font, text, x + width / 2, y + 4, TEXT);
    }

    private void sendTarget(ItemStack stack, int amount) {
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (itemId == null) return;
        String enchantId = "";
        int enchantLevel = 0;
        if (stack.getItem() instanceof EnchantedBookItem) {
            Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(stack);
            if (enchantments.size() != 1) return;
            Map.Entry<Enchantment, Integer> entry = enchantments.entrySet().iterator().next();
            ResourceLocation key = ForgeRegistries.ENCHANTMENTS.getKey(entry.getKey());
            if (key == null) return;
            enchantId = key.toString();
            enchantLevel = entry.getValue();
        }
        ModNetwork.sendToServer(new ServerboundSetTargetPacket(itemId.toString(), enchantId, enchantLevel, amount));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (quantityOpen) {
            if (inBox(mouseX, mouseY, leftPos + 104, topPos + 133, 28, 15)) { adjustQuantity(-1); return true; }
            if (inBox(mouseX, mouseY, leftPos + 204, topPos + 133, 28, 15)) { adjustQuantity(1); return true; }
            if (inBox(mouseX, mouseY, leftPos + 104, topPos + 155, 47, 15)) { playUi(ModSounds.UI_CLICK.get(), 1.04F, 0.34F); quantity.setValue(Integer.toString(maxResultAmount())); return true; }
            if (inBox(mouseX, mouseY, leftPos + 157, topPos + 155, 75, 17)) { confirmQuantity(); return true; }
            if (quantity.isMouseOver(mouseX, mouseY)) return super.mouseClicked(mouseX, mouseY, button);
            return true;
        }
        if (pickerOpen) {
            if (inBox(mouseX, mouseY, leftPos + 249, topPos + 41, 67, 15)) {
                playUi(ModSounds.UI_CLICK.get(), 1.0F, 0.34F);
                booksOnly = !booksOnly;
                scrollRow = 0;
                refilter();
                return true;
            }
            int first = scrollRow * GRID_PAGE_SIZE;
            for (int i = 0; i < GRID_PAGE_SIZE && first + i < filtered.size(); i++) {
                int x = leftPos + 18 + i % GRID_COLS * 21;
                int y = topPos + 60 + i / GRID_COLS * 20;
                if (inBox(mouseX, mouseY, x, y, 18, 18)) {
                    playUi(ModSounds.ITEM_SELECT.get(), 1.0F, 0.50F);
                    ItemStack selected = filtered.get(first + i).stack();
                    if (ResultAmountPolicy.needsPopup(selected)) openQuantity(selected);
                    else { sendTarget(selected, 1); closeOverlays(); }
                    return true;
                }
            }
            if (search.isMouseOver(mouseX, mouseY)) return super.mouseClicked(mouseX, mouseY, button);
            return true;
        }
        if (inBox(mouseX, mouseY, leftPos + UpgraderMenu.TARGET_X - 1, topPos + UpgraderMenu.TARGET_Y - 1, 20, 20)) {
            playUi(ModSounds.UI_CLICK.get(), 1.0F, 0.34F);
            openPicker();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (pickerOpen) {
            int max = pageCount() - 1;
            int previous = scrollRow;
            scrollRow = Mth.clamp(scrollRow + (delta < 0 ? 1 : -1), 0, max);
            if (scrollRow != previous) playUi(ModSounds.KEY_TAP.get(), 0.92F, 0.22F);
            int first = scrollRow * GRID_PAGE_SIZE;
            setPreview(first < filtered.size() ? filtered.get(first) : null);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256 && (pickerOpen || quantityOpen)) { playUi(ModSounds.UI_CLICK.get(), 0.92F, 0.30F); closeOverlays(); return true; }
        if (quantityOpen && keyCode == 257) { confirmQuantity(); return true; }
        EditBox editor = pickerOpen && search.isFocused() ? search : quantityOpen && quantity.isFocused() ? quantity : null;
        if (editor != null) {
            boolean handled = editor.keyPressed(keyCode, scanCode, modifiers);
            if (handled && (keyCode == 259 || keyCode == 261 || keyCode == 262 || keyCode == 263 || keyCode == 268 || keyCode == 269)) {
                playUi(ModSounds.KEY_TAP.get(), 0.96F, 0.20F);
            }
            // Consume every non-Escape key while an editor is focused. Printable
            // characters arrive through charTyped; allowing this key event to
            // reach AbstractContainerScreen would make the inventory key (E/У)
            // close the Upgrader before the character can be inserted.
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        EditBox editor = pickerOpen && search.isFocused() ? search : quantityOpen && quantity.isFocused() ? quantity : null;
        boolean handled = editor != null ? editor.charTyped(codePoint, modifiers) : super.charTyped(codePoint, modifiers);
        if (handled && editor != null) playUi(ModSounds.KEY_TAP.get(), 0.96F + codePoint % 5 * 0.015F, 0.20F);
        return handled;
    }

    private void adjustQuantity(int delta) {
        playUi(ModSounds.UI_CLICK.get(), delta > 0 ? 1.06F : 0.94F, 0.34F);
        quantity.setValue(Integer.toString(Mth.clamp(parseQuantity() + delta, 1, maxResultAmount())));
    }

    private int parseQuantity() {
        try { return Mth.clamp(Integer.parseInt(quantity.getValue()), 1, maxResultAmount()); }
        catch (NumberFormatException ignored) { return 1; }
    }

    private boolean validQuantityInput(String value) {
        if (value.isEmpty()) return true;
        if (!value.chars().allMatch(Character::isDigit)) return false;
        try {
            int amount = Integer.parseInt(value);
            return amount >= 1 && amount <= maxResultAmount();
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private static int maxResultAmount() {
        return ResultAmountPolicy.maximum(UpgraderConfig.MAX_RESULT_AMOUNT.get());
    }

    private void confirmQuantity() {
        playUi(ModSounds.UI_CLICK.get(), 1.08F, 0.38F);
        sendTarget(pendingTarget, parseQuantity());
        closeOverlays();
    }

    private void closeOverlays() {
        pickerOpen = false;
        quantityOpen = false;
        awaitingModeTarget = false;
        pendingTarget = ItemStack.EMPTY;
        menu.setClientSlotsVisible(true);
        setFocused(null);
    }

    private void playUi(SoundEvent sound, float pitch, float volume) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
    }

    private float progress() {
        return spinDuration <= 0L ? 1.0F : Mth.clamp((float) (System.currentTimeMillis() - spinStart) / spinDuration, 0.0F, 1.0F);
    }

    private static void slot(GuiGraphics graphics, int x, int y, boolean gold) {
        int border = gold ? GOLD : 0xFF46546A;
        if (gold) CasinoRenderer.glow(graphics, x - 1, y - 1, 19, 19, 4, 0x50FFD34E);
        CasinoRenderer.card(graphics, x - 1, y - 1, 19, 19, 3, border, 0xFF151C27, 0xFF090D13);
    }

    private static boolean inBox(double x, double y, int bx, int by, int width, int height) {
        return x >= bx && x < bx + width && y >= by && y < by + height;
    }

    private static String format(long value) {
        return String.format(Locale.ROOT, "%,d", value).replace(',', ' ');
    }
}
