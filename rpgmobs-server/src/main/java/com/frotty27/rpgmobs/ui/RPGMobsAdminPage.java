package com.frotty27.rpgmobs.ui;

import com.frotty27.rpgmobs.config.CombatStyle;
import com.frotty27.rpgmobs.config.GlobalConfig;
import com.frotty27.rpgmobs.config.RPGMobsConfig;
import com.frotty27.rpgmobs.config.overlay.ConfigOverlay;
import com.frotty27.rpgmobs.config.overlay.ConfigResolver;
import com.frotty27.rpgmobs.config.overlay.ConfigWriter;
import com.frotty27.rpgmobs.config.overlay.ResolvedConfig;
import com.frotty27.rpgmobs.config.schema.YamlSerializer;
import com.frotty27.rpgmobs.config.templates.ConfigTemplate;
import com.frotty27.rpgmobs.features.IRPGMobsAbilityFeature;
import com.frotty27.rpgmobs.plugin.RPGMobsPlugin;
import com.frotty27.rpgmobs.utils.Constants;
import com.frotty27.rpgmobs.utils.MobRuleCategoryHelpers;
import com.frotty27.rpgmobs.utils.StringHelpers;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public final class RPGMobsAdminPage extends InteractiveCustomUIPage<AdminUIData> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final int SIDEBAR_PAGE_SIZE = 8;
    private static final int MAX_SUB_TABS = 8;
    private static final int LINK_POPUP_ROW_COUNT = 20;
    private static final int ITEM_PICKER_ROW_COUNT = 15;
    private static final int NPC_PICKER_ROW_COUNT = 15;
    private static final int ABIL_MOB_PAGE_SIZE = 15;
    private static final int LOOT_TPL_MOB_PAGE_SIZE = 10;

    private static final int ITEM_PICKER_GEAR_WEAPON_ADD = -10;
    private static final int ITEM_PICKER_GEAR_ARMOR_ADD = -11;
    private static final int ITEM_PICKER_GEAR_WEAPON_SWAP = -12;
    private static final int ITEM_PICKER_GEAR_ARMOR_SWAP = -13;
    private static final int ITEM_PICKER_MOB_RULE_WPN = -14;
    private static final int ITEM_PICKER_MOB_RULE_ARM = -15;
    private static final int ITEM_PICKER_ABILITY_DRINK = -16;
    private static final int RENAME_IDX_TWO_HANDED_ADD = -2;
    private static final int RENAME_IDX_RARITY_RULE_ADD = -3;
    private static final int RENAME_IDX_SUMMON_ROLE_ADD = -4;

    private enum Section { GLOBAL_CORE, GLOBAL_DEBUG, GLOBAL_MOB_RULES, GLOBAL_COMBAT_AI, GLOBAL_CONFIG, WORLD, INSTANCE }

    private static final int TAB_GENERAL = 0;
    private static final int TAB_MOB_RULES = 1;
    private static final int TAB_STATS = 2;
    private static final int TAB_LOOT = 3;
    private static final int TAB_SPAWNING = 4;
    private static final int TAB_ENTITY_EFFECTS = 5;
    private static final int TAB_ABILITIES = 6;
    private static final int TAB_VISUALS = 7;

    private static final int TAB_CONFIG_WEAPONS = 0;
    private static final int TAB_CONFIG_ARMOR = 1;
    private static final int TAB_CONFIG_RARITY = 2;
    private static final int TAB_CONFIG_COMBAT = 3;

    private static final String[] CAI_FACTION_KEYS = {"Disciplined", "Berserker", "Tactical", "Chaotic"};
    private static final String[] CAI_STYLE_DESCRIPTIONS = {
            "Steady, measured attacks with consistent timing and reliable defense.",
            "Aggressive, fast-paced pressure with relentless attacks and little retreat.",
            "Calculated approach with strafing, early retreats, and flanking at high tiers.",
            "Unpredictable timing with random bursts of aggression and erratic movement."
    };
    private static final String[] CAI_WEAPON_KEYS = {
            "Swords", "Longswords", "Daggers", "Axes", "Battleaxes", "Maces", "Clubs", "ClubsFlail", "Spears",
            "Pickaxes", "Other",
            "Shortbows", "Crossbows", "Guns", "Staves", "Spellbooks"
    };
    private static final int CAI_WEAPON_CHAIN_SLOTS = 5;
    private static final int ASSET_PICKER_ROW_COUNT = 15;
    private static final String[] CAI_TIER_BEHAVIOR_NAMES = {"Shield", "BackOff", "Retreat", "Observe", "Flank"};

    private final RPGMobsPlugin plugin;
    private final PlayerRef playerRef;
    private final List<EditableDataSource> globalDataSources = new ArrayList<>();
    private Section activeSection = Section.GLOBAL_CORE;
    private @Nullable String selectedName = null;
    private int activeSubTab = 0;
    private int worldPage = 0, instPage = 0;
    private @Nullable String saveMessage = null;
    private boolean saveMessageSuccess = false;
    private @Nullable String templateAppliedMessage = null;

    private boolean needsFieldRefresh = true;

    private final List<String> worldNames;
    private final List<String> instanceNames;

    private boolean editGlobalEnabled, editEnabledByDefault, editDebugMode;
    private int editDebugScanInterval;

    private RPGMobsConfig.CombatAIConfig editCombatAI;
    private RPGMobsConfig.CombatAIConfig savedCombatAI;
    private int caiFactionIndex = 0;
    private int caiTierIndex = 0;
    private int caiWeaponIndex = 0;
    private int caiSubTab = 0;

    private enum AssetPickerMode { ANIMATION_SET, ANIMATION, SOUND_SWING, SOUND_IMPACT, TRAIL, PARTICLE }
    private boolean assetPickerOpen = false;
    private AssetPickerMode assetPickerMode = AssetPickerMode.ANIMATION_SET;
    private String assetPickerFilter = "";
    private int assetPickerPage = 0;
    private @Nullable String assetPickerSelectedItem = null;
    private List<String> assetPickerFiltered = new ArrayList<>();
    private List<String> assetPickerSourceList = new ArrayList<>();
    private int chainAnimPickSlot = -1;

    private RPGMobsConfig.GearCategory editWeaponCategoryTree = new RPGMobsConfig.GearCategory();
    private RPGMobsConfig.GearCategory savedWeaponCategoryTree = new RPGMobsConfig.GearCategory();
    private RPGMobsConfig.GearCategory editArmorCategoryTree = new RPGMobsConfig.GearCategory();
    private RPGMobsConfig.GearCategory savedArmorCategoryTree = new RPGMobsConfig.GearCategory();

    private final Deque<RPGMobsConfig.GearCategory> wpnCatNavHistory = new ArrayDeque<>();
    private final Deque<RPGMobsConfig.GearCategory> wpnCatForwardHistory = new ArrayDeque<>();
    private RPGMobsConfig.GearCategory currentWeaponCategory = null;
    private final List<TreeItem> wpnCatTreeItems = new ArrayList<>();
    private String wpnCatTreeFilter = "";
    private int wpnCatFilterPage = 0;
    private final List<String> wpnCatTreeFilteredKeys = new ArrayList<>();

    private final Deque<RPGMobsConfig.GearCategory> armCatNavHistory = new ArrayDeque<>();
    private final Deque<RPGMobsConfig.GearCategory> armCatForwardHistory = new ArrayDeque<>();
    private RPGMobsConfig.GearCategory currentArmorCategory = null;
    private final List<TreeItem> armCatTreeItems = new ArrayList<>();
    private String armCatTreeFilter = "";
    private int armCatFilterPage = 0;
    private final List<String> armCatTreeFilteredKeys = new ArrayList<>();

    private @Nullable ConfigOverlay editOverlay = null;

    private @Nullable ResolvedConfig resolvedForSelected = null;

    private final Map<String, ConfigOverlay> pendingOverlays = new HashMap<>();

    private final Map<String, String> pendingTemplateKeys = new HashMap<>();

    private @Nullable ConfigOverlay savedOverlaySnapshot = null;

    private boolean savedGlobalEnabled, savedEnabledByDefault, savedDebugMode;
    private int savedDebugScanInterval;

    private @Nullable String lastAppliedTemplateKey = null;

    private @Nullable ConfigOverlay globalCustomPreset = null;
    private static final String GLOBAL_CUSTOM_PRESET_FILE = "globalCustomPreset.yml";

    private final List<String> activeFamilyRowKeys = new ArrayList<>();

    private final List<String> activeEnvRuleKeys = new ArrayList<>();

    private final List<String> activeTierOverrideKeys = new ArrayList<>();

    private int tierOverridePage = 0;

    private String tierOverrideFilter = "";

    private record TreeItem(String name, boolean isCategory) {}

    private final Deque<RPGMobsConfig.MobRuleCategory> mobRuleNavHistory = new ArrayDeque<>();
    private final Deque<RPGMobsConfig.MobRuleCategory> mobRuleForwardHistory = new ArrayDeque<>();
    private RPGMobsConfig.MobRuleCategory currentMobRuleCategory = null;
    private final List<TreeItem> mobRuleTreeItems = new ArrayList<>();
    private int mobRuleTreeExpandedIndex = -1;
    private String mobRuleTreeFilter = "";
    private final List<String> mobRuleTreeFilteredKeys = new ArrayList<>();

    private static final int MOB_RULE_LINKED_PAGE_SIZE = 5;
    private String mobRuleWpnCatFilter = "";
    private int mobRuleWpnCatPage = 0;
    private final List<String> mobRuleWpnCatFiltered = new ArrayList<>();
    private String mobRuleArmCatFilter = "";
    private int mobRuleArmCatPage = 0;
    private final List<String> mobRuleArmCatFiltered = new ArrayList<>();

    private final Deque<RPGMobsConfig.LootTemplateCategory> lootNavHistory = new ArrayDeque<>();
    private final Deque<RPGMobsConfig.LootTemplateCategory> lootForwardHistory = new ArrayDeque<>();
    private RPGMobsConfig.LootTemplateCategory currentLootCategory = null;
    private final List<TreeItem> lootTreeItems = new ArrayList<>();
    private int lootTemplateExpandedIndex = -1;
    private String lootTreeFilter = "";
    private final List<String> lootTreeFilteredKeys = new ArrayList<>();
    private int lootTemplateDropPage = 0;
    private String lootTplMobFilter = "";
    private int lootTplMobPage = 0;
    private final List<String> lootTplMobFiltered = new ArrayList<>();

    private Map<String, String> savedLootTemplateSnapshot = new LinkedHashMap<>();

    private boolean linkPopupOpen = false;
    private final Deque<RPGMobsConfig.MobRuleCategory> linkPopupNavHistory = new ArrayDeque<>();
    private RPGMobsConfig.MobRuleCategory linkPopupCurrentCategory = null;
    private final List<TreeItem> linkPopupTreeItems = new ArrayList<>();
    private @Nullable String linkPopupSelectedCategory = null;
    private final Deque<RPGMobsConfig.GearCategory> linkPopupGearNavHistory = new ArrayDeque<>();
    private RPGMobsConfig.GearCategory linkPopupGearCurrentCategory = null;

    private enum RenameTarget { MOB_RULE_CATEGORY, MOB_RULE_ITEM, LOOT_CATEGORY, LOOT_ITEM, WEAPON_CATEGORY, ARMOR_CATEGORY }
    private boolean renamePopupOpen = false;
    private @Nullable RenameTarget renameTarget = null;
    private int renameRowIndex = -1;
    private @Nullable String pendingRenameName = null;

    private enum MoveSourceType { MOB_RULE, LOOT, GEAR_WEAPON, GEAR_ARMOR }
    private boolean movePopupOpen = false;
    private @Nullable MoveSourceType moveSourceType = null;
    private int moveRowIdx = -1;
    private @Nullable String movePopupSelectedCategory = null;
    private final Deque<String> movePopupNavHistory = new ArrayDeque<>();
    private @Nullable String movePopupCurrentCategoryName = null;
    private final List<String> movePopupCategoryNames = new ArrayList<>();

    private boolean itemPickerOpen = false;
    private int itemPickerDropSlot = -1;
    private @Nullable String gearCatSwapOldKey = null;
    private String itemPickerFilter = "";
    private String itemPickerCustomId = "";
    private int itemPickerPage = 0;
    private @Nullable String itemPickerSelectedItem = null;
    private final List<String> itemPickerFiltered = new ArrayList<>();
    private static @Nullable List<String> allDroppableItems = null;

    private enum NpcPickerMode { MOB_RULE, ABILITY_LINKED_MOB, LOOT_TEMPLATE_LINKED_MOB, REBIND_MOB_RULE, TIER_RESTRICTION, ABILITY_EXCLUDED_MOB, SUMMON_EXCLUDE_FROM_POOL }
    private boolean npcPickerOpen = false;
    private NpcPickerMode npcPickerMode = NpcPickerMode.MOB_RULE;
    private String npcPickerFilter = "";
    private String npcPickerCustomId = "";
    private int npcPickerPage = 0;
    private @Nullable String npcPickerSelectedItem = null;
    private final List<String> npcPickerFiltered = new ArrayList<>();
    private int npcPickerHiddenCount = 0;

    private enum LinkPopupMode { LOOT_TEMPLATE_LINK, ABILITY_ADD_CATEGORY, LOOT_TEMPLATE_ADD_CATEGORY, TIER_RESTRICTION_ADD_CATEGORY, WEAPON_CATEGORY_ADD, ARMOR_CATEGORY_ADD, ABILITY_WEAPON_GATE }
    private LinkPopupMode linkPopupMode = LinkPopupMode.LOOT_TEMPLATE_LINK;

    private List<String> discoveredAbilityIds = List.of();
    private Map<String, IRPGMobsAbilityFeature> abilityFeaturesById = Map.of();
    private int abilityExpandedIndex = -1;
    private String abilityMobFilter = "";
    private int abilityMobPage = 0;
    private final List<String> abilityMobFiltered = new ArrayList<>();
    private String abilTreeFilter = "";
    private final List<String> abilTreeFiltered = new ArrayList<>();

    private static final int ABIL_CFG_LIST_PAGE_SIZE = 5;
    private final Map<String, RPGMobsConfig.AbilityConfig> editAbilityConfigs = new LinkedHashMap<>();
    private Map<String, RPGMobsConfig.AbilityConfig> savedAbilityConfigs = new LinkedHashMap<>();
    private int abilGatePage = 0;
    private int abilExclPage = 0;
    private int abilSummonRolePage = 0;
    private int abilSummonExclPage = 0;
    private int multiSlashVariantIndex = 0;
    private static final String[] MS_VARIANT_KEYS = Constants.ALL_VARIANT_KEYS;
    private static final String[] MS_VARIANT_LABELS = Constants.ALL_VARIANT_LABELS;

    private final Map<String, RPGMobsConfig.EntityEffectConfig> editEntityEffects = new LinkedHashMap<>();
    private Map<String, RPGMobsConfig.EntityEffectConfig> savedEntityEffects = new LinkedHashMap<>();
    private final List<String> entityEffectKeys = new ArrayList<>();
    private String effectTreeFilter = "";
    private final List<String> effectTreeFiltered = new ArrayList<>();
    private int effectExpandedIndex = -1;

    private static final int RARITY_RULES_PAGE_SIZE = 5;
    private static final int TWO_HANDED_PAGE_SIZE = 5;
    private static final String[] RARITY_NAMES = {"common", "uncommon", "rare", "epic", "legendary"};
    private int[] editArmorPiecesPerTier = new int[5];
    private int[] savedArmorPiecesPerTier = new int[5];
    private double[] editShieldChancePerTier = new double[5];
    private double[] savedShieldChancePerTier = new double[5];
    private boolean[][] editTierAllowedRarities = new boolean[5][5];
    private boolean[][] savedTierAllowedRarities = new boolean[5][5];
    private double[][] editTierRarityWeights = new double[5][5];
    private double[][] savedTierRarityWeights = new double[5][5];
    private List<String> editTwoHandedKeywords = new ArrayList<>();
    private List<String> savedTwoHandedKeywords = new ArrayList<>();
    private int twoHandedPage = 0;
    private LinkedHashMap<String, String> editWeaponRarityRules = new LinkedHashMap<>();
    private LinkedHashMap<String, String> savedWeaponRarityRules = new LinkedHashMap<>();
    private List<String> weaponRarityRuleKeys = new ArrayList<>();
    private int weaponRarityPage = 0;
    private LinkedHashMap<String, String> editArmorRarityRules = new LinkedHashMap<>();
    private LinkedHashMap<String, String> savedArmorRarityRules = new LinkedHashMap<>();
    private List<String> armorRarityRuleKeys = new ArrayList<>();
    private int armorRarityPage = 0;

    private enum CatPeekSource { NONE, WEAPON_CAT, ARMOR_CAT, ABILITY_LINKED, ABILITY_WEAPON_GATE, LOOT_TPL_LINKED, TIER_OVERRIDE }
    private static final int CATEGORY_PEEK_PAGE_SIZE = 15;
    private boolean categoryPeekOpen = false;
    private String categoryPeekTitle = "";
    private List<String> categoryPeekItems = List.of();
    private int categoryPeekPage = 0;
    private CatPeekSource categoryPeekSource = CatPeekSource.NONE;
    private String categoryPeekCategoryKey = "";

    private final List<String> activeDefRuleKeys = new ArrayList<>();

    private final Map<String, RPGMobsConfig.MobRule> editMobRules = new LinkedHashMap<>();

    private Map<String, RPGMobsConfig.MobRule> savedMobRules = new LinkedHashMap<>();

    private RPGMobsConfig.MobRuleCategory editMobRuleCategoryTree = null;
    private RPGMobsConfig.MobRuleCategory savedMobRuleCategoryTree = null;

    private Set<String> editDisabledMobRuleKeys = new LinkedHashSet<>();
    private RPGMobsConfig.@Nullable MobRuleCategory perWorldCurrentCategory = null;
    private Set<String> savedDisabledMobRuleKeys = new LinkedHashSet<>();

    private final Map<String, RPGMobsConfig.LootTemplate> editLootTemplates = new LinkedHashMap<>();

    public RPGMobsAdminPage(PlayerRef playerRef, RPGMobsPlugin plugin) {
        super(playerRef, CustomPageLifetime.CanDismiss, AdminUIData.CODEC);
        this.plugin = plugin;
        this.playerRef = playerRef;

        plugin.discoverWorldsAndInstances();
        ConfigResolver resolver = plugin.getConfigResolver();
        this.worldNames = new ArrayList<>(resolver.getWorldNames());
        this.instanceNames = new ArrayList<>(resolver.getInstanceTemplateNames());
        this.worldNames.sort(String.CASE_INSENSITIVE_ORDER);
        this.instanceNames.sort(String.CASE_INSENSITIVE_ORDER);

        var abilityFeatures = plugin.getFeatureRegistry().getAbilityFeatures();
        discoveredAbilityIds = abilityFeatures.stream().map(IRPGMobsAbilityFeature::id).toList();
        abilityFeaturesById = abilityFeatures.stream()
                .collect(Collectors.toMap(IRPGMobsAbilityFeature::id, feature -> feature));

        registerGlobalDataSources();
        snapshotGlobalConfig();
        snapshotAllData();
        loadGlobalCustomPreset();
    }

    private void refreshSidebarLists() {

        plugin.discoverWorldsAndInstances();
        ConfigResolver resolver = plugin.getConfigResolver();
        worldNames.clear();
        worldNames.addAll(resolver.getWorldNames());
        worldNames.sort(String.CASE_INSENSITIVE_ORDER);
        instanceNames.clear();
        instanceNames.addAll(resolver.getInstanceTemplateNames());
        instanceNames.sort(String.CASE_INSENSITIVE_ORDER);
    }

    private void registerGlobalDataSources() {
        globalDataSources.add(new EditableDataSource() {
            @Override public void applyToConfig(RPGMobsConfig config) {
                config.debugConfig.isDebugModeEnabled = editDebugMode;
                config.debugConfig.debugMobRuleScanIntervalSeconds = editDebugScanInterval;
            }
            @Override public void snapshot() {
                GlobalConfig gc = plugin.getGlobalConfig();
                if (gc != null) {
                    editDebugMode = gc.isDebugModeEnabled;
                    editDebugScanInterval = gc.debugMobRuleScanIntervalSeconds;
                    savedDebugMode = editDebugMode;
                    savedDebugScanInterval = editDebugScanInterval;
                }
            }
            @Override public boolean hasChanges() {
                return hasGlobalDebugChanges();
            }
        });
        globalDataSources.add(new EditableDataSource() {
            @Override public void applyToConfig(RPGMobsConfig config) {
                config.gearConfig.weaponCategoryTree = deepCopyGearCategory(editWeaponCategoryTree);
                config.gearConfig.armorCategoryTree = deepCopyGearCategory(editArmorCategoryTree);
            }
            @Override public void snapshot() {
                snapshotGearCategories();
            }
            @Override public boolean hasChanges() {
                return hasWeaponCategoryChanges() || hasArmorCategoryChanges();
            }
        });
        globalDataSources.add(new EditableDataSource() {
            @Override public void applyToConfig(RPGMobsConfig config) {
                config.gearConfig.armorPiecesToEquipPerTier = Arrays.copyOf(editArmorPiecesPerTier, 5);
                config.gearConfig.shieldUtilityChancePerTier = Arrays.copyOf(editShieldChancePerTier, 5);
                config.gearConfig.defaultTierAllowedRarities = buildTierAllowedRarities(editTierAllowedRarities);
                config.gearConfig.defaultTierEquipmentRarityWeights = buildTierRarityWeights(editTierRarityWeights);
                config.gearConfig.twoHandedWeaponIds = new ArrayList<>(editTwoHandedKeywords);
                config.gearConfig.defaultWeaponRarityRules = new LinkedHashMap<>(editWeaponRarityRules);
                config.gearConfig.defaultArmorRarityRules = new LinkedHashMap<>(editArmorRarityRules);
            }
            @Override public void snapshot() {
                snapshotRarityTiersConfig();
            }
            @Override public boolean hasChanges() {
                return hasRarityTiersChanges();
            }
        });
        globalDataSources.add(new EditableDataSource() {
            @Override public void applyToConfig(RPGMobsConfig config) {
                config.effectsConfig.defaultEntityEffects = new LinkedHashMap<>(editEntityEffects);
            }
            @Override public void snapshot() {
                snapshotEntityEffects();
            }
            @Override public boolean hasChanges() {
                return hasEntityEffectChanges();
            }
        });
        globalDataSources.add(new EditableDataSource() {
            @Override public void applyToConfig(RPGMobsConfig config) {
                for (var entry : editAbilityConfigs.entrySet()) {
                    config.abilitiesConfig.defaultAbilities.put(entry.getKey(),
                            deepCopyAbilityConfig(entry.getKey(), entry.getValue()));
                }
            }
            @Override public void snapshot() {
                snapshotAbilityConfigs();
            }
            @Override public boolean hasChanges() {
                return hasAbilityConfigChanges();
            }
        });
        globalDataSources.add(new EditableDataSource() {
            @Override public void applyToConfig(RPGMobsConfig config) {
                config.combatAIConfig.targetMemoryDuration = editCombatAI.targetMemoryDuration;
                config.combatAIConfig.minRunUtility = editCombatAI.minRunUtility;
                config.combatAIConfig.minActionUtility = editCombatAI.minActionUtility;
                config.combatAIConfig.factionStyles = deepCopyFactionStyles(editCombatAI.factionStyles);
                config.combatAIConfig.tierBehaviors = deepCopyTierBehaviors(editCombatAI.tierBehaviors);
                config.combatAIConfig.weaponParams = deepCopyWeaponParams(editCombatAI.weaponParams);
            }
            @Override public void snapshot() {
                snapshotCombatAI();
            }
            @Override public boolean hasChanges() {
                return hasCombatAIChanges();
            }
        });
        globalDataSources.add(new EditableDataSource() {
            @Override public void applyToConfig(RPGMobsConfig config) {
                config.mobsConfig.defaultMobRules = deepCopyMobRulesMap(editMobRules);
                config.mobsConfig.categoryTree = editMobRuleCategoryTree != null
                        ? deepCopyMobRuleCategoryTree(editMobRuleCategoryTree) : null;
            }
            @Override public void snapshot() {
                snapshotDefaultMobRules();
                snapshotMobRuleCategoryTree();
            }
            @Override public boolean hasChanges() {
                return hasGlobalMobRuleChanges();
            }
        });
    }

    private void snapshotAllData() {
        for (var ds : globalDataSources) ds.snapshot();
        snapshotLootTemplates();
        initEditLootTemplatesFromBase();
    }

    private void applyGlobalDataSources(RPGMobsConfig config) {
        for (var ds : globalDataSources) ds.applyToConfig(config);
    }

    private void snapshotGlobalConfig() {
        GlobalConfig gc = plugin.getGlobalConfig();
        if (gc != null) {
            editGlobalEnabled = gc.globalEnabled;
            savedGlobalEnabled = editGlobalEnabled;
            editEnabledByDefault = gc.enabledByDefault;
            savedEnabledByDefault = editEnabledByDefault;
        }
    }


    private void snapshotGearCategories() {
        RPGMobsConfig config = plugin.getConfig();
        if (config != null) {
            editWeaponCategoryTree = deepCopyGearCategory(config.gearConfig.weaponCategoryTree);
            savedWeaponCategoryTree = deepCopyGearCategory(config.gearConfig.weaponCategoryTree);
            editArmorCategoryTree = deepCopyGearCategory(config.gearConfig.armorCategoryTree);
            MobRuleCategoryHelpers.expandArmorMaterialsToFullIds(editArmorCategoryTree);
            savedArmorCategoryTree = deepCopyGearCategory(editArmorCategoryTree);
        }
    }

    private void snapshotRarityTiersConfig() {
        RPGMobsConfig config = plugin.getConfig();
        if (config == null) return;
        RPGMobsConfig.GearConfig gc = config.gearConfig;
        editArmorPiecesPerTier = Arrays.copyOf(gc.armorPiecesToEquipPerTier, 5);
        savedArmorPiecesPerTier = Arrays.copyOf(gc.armorPiecesToEquipPerTier, 5);
        editShieldChancePerTier = Arrays.copyOf(gc.shieldUtilityChancePerTier, 5);
        savedShieldChancePerTier = Arrays.copyOf(gc.shieldUtilityChancePerTier, 5);
        parseTierAllowedRarities(gc.defaultTierAllowedRarities, editTierAllowedRarities);
        parseTierAllowedRarities(gc.defaultTierAllowedRarities, savedTierAllowedRarities);
        parseTierRarityWeights(gc.defaultTierEquipmentRarityWeights, editTierRarityWeights);
        parseTierRarityWeights(gc.defaultTierEquipmentRarityWeights, savedTierRarityWeights);
        editTwoHandedKeywords = new ArrayList<>(gc.twoHandedWeaponIds);
        savedTwoHandedKeywords = new ArrayList<>(gc.twoHandedWeaponIds);
        editWeaponRarityRules = new LinkedHashMap<>(gc.defaultWeaponRarityRules);
        savedWeaponRarityRules = new LinkedHashMap<>(gc.defaultWeaponRarityRules);
        weaponRarityRuleKeys = new ArrayList<>(editWeaponRarityRules.keySet());
        editArmorRarityRules = new LinkedHashMap<>(gc.defaultArmorRarityRules);
        savedArmorRarityRules = new LinkedHashMap<>(gc.defaultArmorRarityRules);
        armorRarityRuleKeys = new ArrayList<>(editArmorRarityRules.keySet());
        twoHandedPage = 0;
        weaponRarityPage = 0;
        armorRarityPage = 0;
    }

    private static int rarityIndex(String name) {
        for (int i = 0; i < RARITY_NAMES.length; i++) {
            if (RARITY_NAMES[i].equals(name)) return i;
        }
        return -1;
    }

    private static void parseTierAllowedRarities(List<List<String>> src, boolean[][] target) {
        for (int t = 0; t < 5; t++) {
            Arrays.fill(target[t], false);
            if (t < src.size()) {
                for (String rarity : src.get(t)) {
                    int ri = rarityIndex(rarity);
                    if (ri >= 0) target[t][ri] = true;
                }
            }
        }
    }

    private static void parseTierRarityWeights(List<Map<String, Double>> src, double[][] target) {
        for (int t = 0; t < 5; t++) {
            Arrays.fill(target[t], 0.0);
            if (t < src.size()) {
                for (var entry : src.get(t).entrySet()) {
                    int ri = rarityIndex(entry.getKey());
                    if (ri >= 0) target[t][ri] = entry.getValue();
                }
            }
        }
    }

    private static List<List<String>> buildTierAllowedRarities(boolean[][] grid) {
        List<List<String>> result = new ArrayList<>();
        for (int t = 0; t < 5; t++) {
            List<String> tierRarities = new ArrayList<>();
            for (int r = 0; r < 5; r++) {
                if (grid[t][r]) tierRarities.add(RARITY_NAMES[r]);
            }
            result.add(tierRarities);
        }
        return result;
    }

    private static List<Map<String, Double>> buildTierRarityWeights(double[][] grid) {
        List<Map<String, Double>> result = new ArrayList<>();
        for (int t = 0; t < 5; t++) {
            Map<String, Double> tierWeights = new LinkedHashMap<>();
            for (int r = 0; r < 5; r++) {
                if (grid[t][r] > 0.0) tierWeights.put(RARITY_NAMES[r], grid[t][r]);
            }
            result.add(tierWeights);
        }
        return result;
    }

    private static RPGMobsConfig.GearCategory deepCopyGearCategory(RPGMobsConfig.GearCategory src) {
        if (src == null) return new RPGMobsConfig.GearCategory();
        var copy = new RPGMobsConfig.GearCategory();
        copy.name = src.name;
        copy.itemKeys = new ArrayList<>(src.itemKeys);
        copy.children = new ArrayList<>();
        for (RPGMobsConfig.GearCategory child : src.children) {
            copy.children.add(deepCopyGearCategory(child));
        }
        return copy;
    }

    private void resetWeaponCatNav() {
        wpnCatNavHistory.clear();
        wpnCatForwardHistory.clear();
        currentWeaponCategory = null;
        wpnCatTreeItems.clear();
        wpnCatTreeFilter = "";
        wpnCatFilterPage = 0;
        wpnCatTreeFilteredKeys.clear();
    }

    private void resetArmorCatNav() {
        armCatNavHistory.clear();
        armCatForwardHistory.clear();
        currentArmorCategory = null;
        armCatTreeItems.clear();
        armCatTreeFilter = "";
        armCatFilterPage = 0;
        armCatTreeFilteredKeys.clear();
    }

    private RPGMobsConfig.GearCategory ensureCurrentGearCat(boolean isWeapon) {
        if (isWeapon) {
            if (currentWeaponCategory == null) currentWeaponCategory = editWeaponCategoryTree;
            return currentWeaponCategory;
        } else {
            if (currentArmorCategory == null) currentArmorCategory = editArmorCategoryTree;
            return currentArmorCategory;
        }
    }

    private void navigateToGearCat(RPGMobsConfig.GearCategory target, boolean isWeapon) {
        RPGMobsConfig.GearCategory current = ensureCurrentGearCat(isWeapon);
        if (isWeapon) {
            wpnCatNavHistory.push(current);
            wpnCatForwardHistory.clear();
            currentWeaponCategory = target;
        } else {
            armCatNavHistory.push(current);
            armCatForwardHistory.clear();
            currentArmorCategory = target;
        }
        needsFieldRefresh = true;
    }

    private void gearCatBack(boolean isWeapon) {
        Deque<RPGMobsConfig.GearCategory> history = isWeapon ? wpnCatNavHistory : armCatNavHistory;
        Deque<RPGMobsConfig.GearCategory> forward = isWeapon ? wpnCatForwardHistory : armCatForwardHistory;
        if (history.isEmpty()) return;
        RPGMobsConfig.GearCategory current = ensureCurrentGearCat(isWeapon);
        forward.push(current);
        RPGMobsConfig.GearCategory prev = history.pop();
        if (isWeapon) currentWeaponCategory = prev; else currentArmorCategory = prev;
        needsFieldRefresh = true;
    }

    private void gearCatForward(boolean isWeapon) {
        Deque<RPGMobsConfig.GearCategory> history = isWeapon ? wpnCatNavHistory : armCatNavHistory;
        Deque<RPGMobsConfig.GearCategory> forward = isWeapon ? wpnCatForwardHistory : armCatForwardHistory;
        if (forward.isEmpty()) return;
        RPGMobsConfig.GearCategory current = ensureCurrentGearCat(isWeapon);
        history.push(current);
        RPGMobsConfig.GearCategory next = forward.pop();
        if (isWeapon) currentWeaponCategory = next; else currentArmorCategory = next;
        needsFieldRefresh = true;
    }

    private String buildGearCatBreadcrumb(boolean isWeapon) {
        RPGMobsConfig.GearCategory root = isWeapon ? editWeaponCategoryTree : editArmorCategoryTree;
        RPGMobsConfig.GearCategory current = ensureCurrentGearCat(isWeapon);
        if (current == root) return "> " + root.name;

        Deque<RPGMobsConfig.GearCategory> history = isWeapon ? wpnCatNavHistory : armCatNavHistory;
        StringBuilder sb = new StringBuilder();
        for (var it = history.descendingIterator(); it.hasNext(); ) {
            sb.append("> ").append(it.next().name).append(" ");
        }
        sb.append("> ").append(current.name);
        return sb.toString();
    }

    @Override
    public void build(@NonNull Ref<EntityStore> ref, UICommandBuilder commands, @NonNull UIEventBuilder events, @NonNull Store<EntityStore> store) {
        commands.append("Pages/RPGMobs_Admin.ui");

        bindAction(events, "#CloseButton", "Close");
        bindAction(events, "#DiscardButton", "Discard");
        bindAction(events, "#SaveReloadButton", "SaveReload");

        bindAction(events, "#NavGlobalCore", "NavGlobalCore");
        bindAction(events, "#NavGlobalCoreActive", "NavGlobalCore");
        bindAction(events, "#NavGlobalCoreChanged", "NavGlobalCore");
        bindAction(events, "#NavGlobalCoreChangedActive", "NavGlobalCore");
        bindAction(events, "#NavGlobalDebug", "NavGlobalDebug");
        bindAction(events, "#NavGlobalDebugActive", "NavGlobalDebug");
        bindAction(events, "#NavGlobalDebugChanged", "NavGlobalDebug");
        bindAction(events, "#NavGlobalDebugChangedActive", "NavGlobalDebug");
        bindAction(events, "#NavGlobalMobRules", "NavGlobalMobRules");
        bindAction(events, "#NavGlobalMobRulesActive", "NavGlobalMobRules");
        bindAction(events, "#NavGlobalMobRulesChanged", "NavGlobalMobRules");
        bindAction(events, "#NavGlobalMobRulesChangedActive", "NavGlobalMobRules");
        bindAction(events, "#NavGlobalCombatAI", "NavGlobalCombatAI");
        bindAction(events, "#NavGlobalCombatAIActive", "NavGlobalCombatAI");
        bindAction(events, "#NavGlobalCombatAIChanged", "NavGlobalCombatAI");
        bindAction(events, "#NavGlobalCombatAIChangedActive", "NavGlobalCombatAI");
        bindAction(events, "#NavGlobalConfig", "NavGlobalConfig");
        bindAction(events, "#NavGlobalConfigActive", "NavGlobalConfig");
        bindAction(events, "#NavGlobalConfigChanged", "NavGlobalConfig");
        bindAction(events, "#NavGlobalConfigChangedActive", "NavGlobalConfig");


        bindValueChanged(events, "#WpnCatTreeFilter", "@WpnCatTreeFilter");
        bindAction(events, "#WpnCatNavBack", "WpnCatNavBack");
        bindAction(events, "#WpnCatNavForward", "WpnCatNavForward");
        bindAction(events, "#WpnCatAddCategory", "WpnCatAddCategory");
        bindAction(events, "#WpnCatAddItem", "WpnCatAddItem");
        bindAction(events, "#WpnCatDeleteFiltered", "WpnCatDeleteFiltered");
        bindAction(events, "#WpnCatDeleteAll", "WpnCatDeleteAll");
        bindAction(events, "#WpnCatPrevPage", "WpnCatPrevPage");
        bindAction(events, "#WpnCatNextPage", "WpnCatNextPage");
        for (int i = 0; i < AdminUIData.TREE_ROW_COUNT; i++) {
            bindAction(events, "#WpnCatRowCat" + i, "WpnCatRowClick_" + i);
            bindAction(events, "#WpnCatRowItm" + i, "WpnCatRowClick_" + i);
            bindAction(events, "#WpnCatRowRen" + i, "WpnCatRowRen_" + i);
            bindAction(events, "#WpnCatRowChs" + i, "WpnCatRowChs_" + i);
            bindAction(events, "#WpnCatRowMov" + i, "WpnCatRowMov_" + i);
            bindAction(events, "#WpnCatRowDel" + i, "WpnCatRowDel_" + i);
        }

        bindValueChanged(events, "#ArmCatTreeFilter", "@ArmCatTreeFilter");
        bindAction(events, "#ArmCatNavBack", "ArmCatNavBack");
        bindAction(events, "#ArmCatNavForward", "ArmCatNavForward");
        bindAction(events, "#ArmCatAddCategory", "ArmCatAddCategory");
        bindAction(events, "#ArmCatAddItem", "ArmCatAddItem");
        bindAction(events, "#ArmCatDeleteFiltered", "ArmCatDeleteFiltered");
        bindAction(events, "#ArmCatDeleteAll", "ArmCatDeleteAll");
        bindAction(events, "#ArmCatPrevPage", "ArmCatPrevPage");
        bindAction(events, "#ArmCatNextPage", "ArmCatNextPage");
        for (int i = 0; i < AdminUIData.TREE_ROW_COUNT; i++) {
            bindAction(events, "#ArmCatRowCat" + i, "ArmCatRowClick_" + i);
            bindAction(events, "#ArmCatRowItm" + i, "ArmCatRowClick_" + i);
            bindAction(events, "#ArmCatRowRen" + i, "ArmCatRowRen_" + i);
            bindAction(events, "#ArmCatRowChs" + i, "ArmCatRowChs_" + i);
            bindAction(events, "#ArmCatRowMov" + i, "ArmCatRowMov_" + i);
            bindAction(events, "#ArmCatRowDel" + i, "ArmCatRowDel_" + i);
        }

        bindAction(events, "#GlobMobRuleArmCatAdd", "GlobMobRuleArmCatAdd");
        bindAction(events, "#GlobMobRuleArmCatAddItem", "GlobMobRuleArmCatAddItem");
        bindAction(events, "#GlobMobRuleArmCatClear", "GlobMobRuleArmCatClear");
        bindAction(events, "#GlobMobRuleArmCatPrev", "GlobMobRuleArmCatPrev");
        bindAction(events, "#GlobMobRuleArmCatNext", "GlobMobRuleArmCatNext");
        bindValueChanged(events, "#GlobMobRuleArmCatFilter", "@GlobMobRuleArmCatFilter");
        for (int i = 0; i < 5; i++) {
            bindAction(events, "#GlobMobRuleArmCatDel" + i, "GlobMobRuleArmCatDel_" + i);
            bindAction(events, "#GlobMobRuleArmCatPeek" + i, "GlobMobRuleArmCatPeek_" + i);
        }

        bindValueChanged(events, "#FieldLootDurMin", "@LootDurMin");
        bindValueChanged(events, "#FieldLootDurMax", "@LootDurMax");

        for (int i = 0; i < 5; i++) {
            bindValueChanged(events, "#FieldRarityArmorPieces" + i, "@RarityArmorPieces" + i);
            bindValueChanged(events, "#FieldRarityShieldChance" + i, "@RarityShieldChance" + i);
        }
        for (int t = 0; t < 5; t++) {
            for (int r = 0; r < 5; r++) {
                bindValueChanged(events, "#FieldRarityWt" + t + r, "@RarityWt" + t + r);
            }
            for (int r = 0; r < 5; r++) {
                String id = "T" + t + "R" + r;
                bindAction(events, "#RarityAllowedT" + t + "R" + r + "On", "RarityAllowedToggle_" + id);
                bindAction(events, "#RarityAllowedT" + t + "R" + r + "Off", "RarityAllowedToggle_" + id);
            }
        }
        bindAction(events, "#TwoHandedAdd", "TwoHandedAdd");
        for (int i = 0; i < TWO_HANDED_PAGE_SIZE; i++) {
            bindAction(events, "#TwoHandedDel" + i, "TwoHandedDel_" + i);
        }
        bindAction(events, "#TwoHandedFirstPage", "TwoHandedFirstPage");
        bindAction(events, "#TwoHandedPrevPage", "TwoHandedPrevPage");
        bindAction(events, "#TwoHandedNextPage", "TwoHandedNextPage");
        bindAction(events, "#TwoHandedLastPage", "TwoHandedLastPage");
        bindAction(events, "#WpnRarityAdd", "WpnRarityAdd");
        for (int i = 0; i < RARITY_RULES_PAGE_SIZE; i++) {
            bindAction(events, "#WpnRarityCycle" + i, "WpnRarityCycle_" + i);
            bindAction(events, "#WpnRarityDel" + i, "WpnRarityDel_" + i);
        }
        bindAction(events, "#WpnRarityFirstPage", "WpnRarityFirstPage");
        bindAction(events, "#WpnRarityPrevPage", "WpnRarityPrevPage");
        bindAction(events, "#WpnRarityNextPage", "WpnRarityNextPage");
        bindAction(events, "#WpnRarityLastPage", "WpnRarityLastPage");
        bindAction(events, "#ArmRarityAdd", "ArmRarityAdd");
        for (int i = 0; i < RARITY_RULES_PAGE_SIZE; i++) {
            bindAction(events, "#ArmRarityCycle" + i, "ArmRarityCycle_" + i);
            bindAction(events, "#ArmRarityDel" + i, "ArmRarityDel_" + i);
        }
        bindAction(events, "#ArmRarityFirstPage", "ArmRarityFirstPage");
        bindAction(events, "#ArmRarityPrevPage", "ArmRarityPrevPage");
        bindAction(events, "#ArmRarityNextPage", "ArmRarityNextPage");
        bindAction(events, "#ArmRarityLastPage", "ArmRarityLastPage");

        bindAction(events, "#MobRuleNavBack", "MobRuleNavBack");
        bindAction(events, "#MobRuleNavForward", "MobRuleNavForward");
        bindAction(events, "#AddMobRuleCategory", "AddMobRuleCategory");
        bindAction(events, "#AddMobRuleItem", "AddMobRuleItem");
        bindValueChanged(events, "#MobRuleTreeFilter", "@MobRuleTreeFilter");
        bindAction(events, "#MobRuleDeleteFiltered", "MobRuleDeleteFiltered");
        bindAction(events, "#MobRuleDeleteAll", "MobRuleDeleteAll");
        for (int i = 0; i < AdminUIData.TREE_ROW_COUNT; i++) {
            bindAction(events, "#MobRuleRowCat" + i, "MobRuleRowClick_" + i);
            bindAction(events, "#MobRuleRowItm" + i, "MobRuleRowClick_" + i);
            bindAction(events, "#MobRuleRowItmOff" + i, "MobRuleRowClick_" + i);
            bindAction(events, "#MobRuleRowRen" + i, "MobRuleRowRen_" + i);
            bindAction(events, "#MobRuleRowMov" + i, "MobRuleRowMov_" + i);
            bindAction(events, "#MobRuleRowDel" + i, "MobRuleRowDel_" + i);
            bindAction(events, "#MobRuleRowTogOn" + i, "MobRuleRowToggle_" + i);
            bindAction(events, "#MobRuleRowTogOff" + i, "MobRuleRowToggle_" + i);
        }

        bindAction(events, "#GlobMobRuleRebind", "GlobMobRuleRebind");
        bindAction(events, "#GlobMobRuleCycleMode", "GlobMobRuleCycleMode");
        bindAction(events, "#GlobMobRuleCombatStyle", "GlobMobRuleCombatStyle");
        for (int t = 0; t < 5; t++) {
            bindAction(events, "#GlobMobRuleWpnTierOn" + t, "ToggleGlobMobRuleWpnTier_" + t);
            bindAction(events, "#GlobMobRuleWpnTierOff" + t, "ToggleGlobMobRuleWpnTier_" + t);
        }
        bindValueChanged(events, "#FieldGlobMobRuleMatchExact", "@GlobMobRuleMatchExact");
        bindValueChanged(events, "#FieldGlobMobRuleMatchPrefix", "@GlobMobRuleMatchPrefix");
        bindValueChanged(events, "#FieldGlobMobRuleMatchContains", "@GlobMobRuleMatchContains");
        bindValueChanged(events, "#FieldGlobMobRuleMatchExcludes", "@GlobMobRuleMatchExcludes");
        bindAction(events, "#GlobMobRuleWpnCatAdd", "GlobMobRuleWpnCatAdd");
        bindAction(events, "#GlobMobRuleWpnCatAddItem", "GlobMobRuleWpnCatAddItem");
        bindAction(events, "#GlobMobRuleWpnCatClear", "GlobMobRuleWpnCatClear");
        bindAction(events, "#GlobMobRuleWpnCatPrev", "GlobMobRuleWpnCatPrev");
        bindAction(events, "#GlobMobRuleWpnCatNext", "GlobMobRuleWpnCatNext");
        bindValueChanged(events, "#GlobMobRuleWpnCatFilter", "@GlobMobRuleWpnCatFilter");
        for (int i = 0; i < 5; i++) {
            bindAction(events, "#GlobMobRuleWpnCatDel" + i, "GlobMobRuleWpnCatDel_" + i);
            bindAction(events, "#GlobMobRuleWpnCatPeek" + i, "GlobMobRuleWpnCatPeek_" + i);
        }
        bindAction(events, "#ArmorSlotAllOn", "ToggleArmorSlotAll");
        bindAction(events, "#ArmorSlotAllOff", "ToggleArmorSlotAll");
        bindAction(events, "#ArmorSlotHeadOn", "ToggleArmorSlot_Head");
        bindAction(events, "#ArmorSlotHeadOff", "ToggleArmorSlot_Head");
        bindAction(events, "#ArmorSlotChestOn", "ToggleArmorSlot_Chest");
        bindAction(events, "#ArmorSlotChestOff", "ToggleArmorSlot_Chest");
        bindAction(events, "#ArmorSlotHandsOn", "ToggleArmorSlot_Hands");
        bindAction(events, "#ArmorSlotHandsOff", "ToggleArmorSlot_Hands");
        bindAction(events, "#ArmorSlotLegsOn", "ToggleArmorSlot_Legs");
        bindAction(events, "#ArmorSlotLegsOff", "ToggleArmorSlot_Legs");

        bindAction(events, "#LootNavBack", "LootNavBack");
        bindAction(events, "#LootNavForward", "LootNavForward");
        bindAction(events, "#AddLootCategory", "AddLootCategory");
        bindAction(events, "#AddLootTemplate", "AddLootTemplate");
        bindValueChanged(events, "#LootTreeFilter", "@LootTreeFilter");
        bindAction(events, "#LootDeleteFiltered", "LootDeleteFiltered");
        bindAction(events, "#LootDeleteAll", "LootDeleteAll");
        for (int i = 0; i < AdminUIData.TREE_ROW_COUNT; i++) {
            bindAction(events, "#LootRowCat" + i, "LootRowClick_" + i);
            bindAction(events, "#LootRowItm" + i, "LootRowClick_" + i);
            bindAction(events, "#LootRowRen" + i, "LootRowRen_" + i);
            bindAction(events, "#LootRowMov" + i, "LootRowMov_" + i);
            bindAction(events, "#LootRowDel" + i, "LootRowDel_" + i);
        }

        bindAction(events, "#LinkPopupNavBack", "LinkPopupNavBack");
        bindAction(events, "#LinkPopupConfirmOn", "LinkPopupConfirm");
        bindAction(events, "#LinkPopupCancel", "LinkPopupCancel");
        bindAction(events, "#LinkPopupBackdrop", "LinkPopupCancel");
        for (int i = 0; i < LINK_POPUP_ROW_COUNT; i++) {
            bindAction(events, "#LinkPopupRowCat" + i, "LinkPopupRowClick_" + i);
            bindAction(events, "#LinkPopupRowItm" + i, "LinkPopupRowClick_" + i);
        }

        bindValueChanged(events, "#RenamePopupField", "@RenamePopupField");
        bindAction(events, "#RenamePopupConfirm", "RenamePopupConfirm");
        bindAction(events, "#RenamePopupCancel", "RenamePopupCancel");
        bindAction(events, "#RenamePopupBackdrop", "RenamePopupCancel");

        bindAction(events, "#MovePopupNavBack", "MovePopupNavBack");
        bindAction(events, "#MovePopupConfirm", "MovePopupConfirm");
        bindAction(events, "#MovePopupCancel", "MovePopupCancel");
        bindAction(events, "#MovePopupBackdrop", "MovePopupCancel");
        for (int i = 0; i < LINK_POPUP_ROW_COUNT; i++) {
            bindAction(events, "#MovePopupRowBtn" + i, "MovePopupRowClick_" + i);
        }

        bindValueChanged(events, "#ItemPickerFilter", "@ItemPickerFilter");
        bindValueChanged(events, "#ItemPickerCustomId", "@ItemPickerCustomId");
        bindAction(events, "#ItemPickerCancel", "ItemPickerCancel");
        bindAction(events, "#ItemPickerBackdrop", "ItemPickerCancel");
        bindAction(events, "#ItemPickerAdd", "ItemPickerAdd");
        bindAction(events, "#ItemPickerFirstPage", "ItemPickerFirstPage");
        bindAction(events, "#ItemPickerPrevPage", "ItemPickerPrevPage");
        bindAction(events, "#ItemPickerNextPage", "ItemPickerNextPage");
        bindAction(events, "#ItemPickerLastPage", "ItemPickerLastPage");
        bindAction(events, "#ItemPickerUseCustom", "ItemPickerUseCustom");
        for (int i = 0; i < ITEM_PICKER_ROW_COUNT; i++) {
            bindAction(events, "#ItemPickerRowBtn" + i, "ItemPickerRowClick_" + i);
        }

        bindValueChanged(events, "#NpcPickerFilter", "@NpcPickerFilter");
        bindValueChanged(events, "#NpcPickerCustomId", "@NpcPickerCustomId");
        bindAction(events, "#NpcPickerCancel", "NpcPickerCancel");
        bindAction(events, "#NpcPickerBackdrop", "NpcPickerCancel");
        bindAction(events, "#NpcPickerAdd", "NpcPickerAdd");
        bindAction(events, "#NpcPickerFirstPage", "NpcPickerFirstPage");
        bindAction(events, "#NpcPickerPrevPage", "NpcPickerPrevPage");
        bindAction(events, "#NpcPickerNextPage", "NpcPickerNextPage");
        bindAction(events, "#NpcPickerLastPage", "NpcPickerLastPage");
        bindAction(events, "#NpcPickerUseCustom", "NpcPickerUseCustom");
        for (int i = 0; i < NPC_PICKER_ROW_COUNT; i++) {
            bindAction(events, "#NpcPickerRowBtn" + i, "NpcPickerRowClick_" + i);
        }

        bindAction(events, "#CatPeekClose", "CatPeekClose");
        bindAction(events, "#CatPeekExtract", "CatPeekExtract");
        bindAction(events, "#CatPeekBackdrop", "CatPeekClose");
        bindAction(events, "#CatPeekFirstPage", "CatPeekFirstPage");
        bindAction(events, "#CatPeekPrevPage", "CatPeekPrevPage");
        bindAction(events, "#CatPeekNextPage", "CatPeekNextPage");
        bindAction(events, "#CatPeekLastPage", "CatPeekLastPage");

        bindValueChanged(events, "#LootTplMobFilter", "@LootTplMobFilter");
        for (int i = 0; i < LOOT_TPL_MOB_PAGE_SIZE; i++) {
            bindAction(events, "#LootTplMobDel" + i, "LootTplMobDel_" + i);
            bindAction(events, "#LootTplMobPeek" + i, "LootTplMobPeek_" + i);
        }
        bindAction(events, "#LootTplMobFirstPage", "LootTplMobFirstPage");
        bindAction(events, "#LootTplMobPrevPage", "LootTplMobPrevPage");
        bindAction(events, "#LootTplMobNextPage", "LootTplMobNextPage");
        bindAction(events, "#LootTplMobLastPage", "LootTplMobLastPage");
        bindAction(events, "#LootTplAddCategory", "LootTplAddCategory");
        bindAction(events, "#LootTplAddMob", "LootTplAddMob");
        bindAction(events, "#LootTplMobDeleteFiltered", "LootTplMobDeleteFiltered");
        bindAction(events, "#LootTplMobDeleteAll", "LootTplMobDeleteAll");

        bindAction(events, "#LootTplAddDrop", "LootTplAddDrop");
        bindAction(events, "#LootTplDropFirstPage", "LootTplDropFirstPage");
        bindAction(events, "#LootTplDropPrevPage", "LootTplDropPrevPage");
        bindAction(events, "#LootTplDropNextPage", "LootTplDropNextPage");
        bindAction(events, "#LootTplDropLastPage", "LootTplDropLastPage");
        for (int i = 0; i < AdminUIData.LOOT_TEMPLATE_DROPS_PER_PAGE; i++) {
            bindAction(events, "#LootTplDropItemBtn" + i, "LootTplDropItemPick_" + i);
            bindValueChanged(events, "#LootTplDropChance" + i, "@LootTplDropChance" + i);
            bindValueChanged(events, "#LootTplDropMinQty" + i, "@LootTplDropMinQty" + i);
            bindValueChanged(events, "#LootTplDropMaxQty" + i, "@LootTplDropMaxQty" + i);
            for (int t = 0; t < AdminUIData.TIERS_COUNT; t++) {
                bindAction(events, "#LootTplDropTierOn" + i + "T" + t, "LootTplDropTier_" + i + "_" + t);
                bindAction(events, "#LootTplDropTierOff" + i + "T" + t, "LootTplDropTier_" + i + "_" + t);
            }
            bindAction(events, "#LootTplDropDel" + i, "LootTplDropDel_" + i);
        }

        for (int i = 0; i < SIDEBAR_PAGE_SIZE; i++) {
            bindAction(events, "#NavWorld" + i, "NavWorld_" + i);
            bindAction(events, "#NavWorld" + i + "Active", "NavWorld_" + i);
            bindAction(events, "#NavWorld" + i + "Changed", "NavWorld_" + i);
            bindAction(events, "#NavWorld" + i + "ChangedActive", "NavWorld_" + i);
            bindAction(events, "#NavInst" + i, "NavInst_" + i);
            bindAction(events, "#NavInst" + i + "Active", "NavInst_" + i);
            bindAction(events, "#NavInst" + i + "Changed", "NavInst_" + i);
            bindAction(events, "#NavInst" + i + "ChangedActive", "NavInst_" + i);
        }
        bindAction(events, "#FirstWorlds", "FirstWorlds");
        bindAction(events, "#PrevWorlds", "PrevWorlds");
        bindAction(events, "#NextWorlds", "NextWorlds");
        bindAction(events, "#LastWorlds", "LastWorlds");
        bindAction(events, "#FirstInsts", "FirstInsts");
        bindAction(events, "#PrevInsts", "PrevInsts");
        bindAction(events, "#NextInsts", "NextInsts");
        bindAction(events, "#LastInsts", "LastInsts");

        for (int i = 0; i < MAX_SUB_TABS; i++) {
            bindAction(events, "#SubTab" + i, "SubTab_" + i);
            bindAction(events, "#SubTab" + i + "Active", "SubTab_" + i);
            bindAction(events, "#SubTab" + i + "Changed", "SubTab_" + i);
            bindAction(events, "#SubTab" + i + "ChangedActive", "SubTab_" + i);
        }

        bindAction(events, "#GlobalEnabledOn", "ToggleGlobalEnabled");
        bindAction(events, "#GlobalEnabledOff", "ToggleGlobalEnabled");
        bindAction(events, "#EnabledByDefaultOn", "ToggleEnabledByDefault");
        bindAction(events, "#EnabledByDefaultOff", "ToggleEnabledByDefault");
        bindAction(events, "#DebugModeOn", "ToggleDebugMode");
        bindAction(events, "#DebugModeOff", "ToggleDebugMode");
        bindValueChanged(events, "#FieldDebugScanInterval", "@DebugScanInterval");

        for (int i = 0; i < 4; i++) {
            bindAction(events, "#CaiFaction" + i, "CaiFaction_" + i);
            bindAction(events, "#CaiFaction" + i + "Active", "CaiFaction_" + i);
        }
        bindAction(events, "#CaiFacObserveOn", "CaiFacObserveToggle");
        bindAction(events, "#CaiFacObserveOff", "CaiFacObserveToggle");
        bindAction(events, "#CaiFacFlankOn", "CaiFacFlankToggle");
        bindAction(events, "#CaiFacFlankOff", "CaiFacFlankToggle");
        for (String b : CAI_TIER_BEHAVIOR_NAMES) {
            for (int t = 0; t < 5; t++) {
                bindAction(events, "#CaiTierToggle" + b + t + "On", "CaiTierToggle_" + b + "_" + t);
                bindAction(events, "#CaiTierToggle" + b + t + "Off", "CaiTierToggle_" + b + "_" + t);
            }
        }
        for (int i = 0; i < 5; i++) {
            bindAction(events, "#CaiTier" + i, "CaiTier_" + i);
            bindAction(events, "#CaiTier" + i + "Active", "CaiTier_" + i);
        }
        for (int i = 0; i < 3; i++) {
            bindAction(events, "#CaiTab" + i, "CaiSubTab_" + i);
            bindAction(events, "#CaiTab" + i + "Active", "CaiSubTab_" + i);
        }
        for (int i = 0; i < CAI_WEAPON_KEYS.length; i++) {
            bindAction(events, "#CaiWeapon" + i, "CaiWeapon_" + i);
            bindAction(events, "#CaiWeapon" + i + "Active", "CaiWeapon_" + i);
        }
        bindAction(events, "#CaiWpnAnimSetBtn", "CaiWpnAnimSetPick");
        for (int i = 0; i < CAI_WEAPON_CHAIN_SLOTS; i++) {
            bindAction(events, "#CaiWpnChainAnim" + i, "CaiWpnChainAnimPick_" + i);
            bindAction(events, "#CaiWpnChainDel" + i, "CaiWpnChainDel_" + i);
        }
        bindAction(events, "#CaiWpnChainAdd", "CaiWpnChainAdd");
        bindAction(events, "#CaiWpnSwingSoundBtn", "CaiWpnSwingSoundPick");
        bindAction(events, "#CaiWpnImpactSoundBtn", "CaiWpnImpactSoundPick");
        bindAction(events, "#CaiWpnSwingSoundPreview", "CaiWpnSwingSoundPreview");
        bindAction(events, "#CaiWpnImpactSoundPreview", "CaiWpnImpactSoundPreview");
        bindAction(events, "#CaiWpnTrailBtn", "CaiWpnTrailPick");
        bindAction(events, "#CaiWpnHitParticleBtn", "CaiWpnHitParticlePick");
        bindAction(events, "#AssetPickerBackdrop", "AssetPickerBackdropClick");
        bindAction(events, "#AssetPickerCancel", "AssetPickerCancel");
        bindAction(events, "#AssetPickerConfirm", "AssetPickerConfirm");
        for (int i = 0; i < ASSET_PICKER_ROW_COUNT; i++) {
            bindAction(events, "#AssetPickerRowBtn" + i, "AssetPickerRowClick_" + i);
            bindAction(events, "#AssetPickerRowBtnSel" + i, "AssetPickerRowClick_" + i);
        }
        bindAction(events, "#AssetPickerFirstPage", "AssetPickerFirstPage");
        bindAction(events, "#AssetPickerPrevPage", "AssetPickerPrevPage");
        bindAction(events, "#AssetPickerNextPage", "AssetPickerNextPage");
        bindAction(events, "#AssetPickerLastPage", "AssetPickerLastPage");
        bindValueChanged(events, "#AssetPickerFilter", "@AssetPickerFilter");
        bindValueChanged(events, "#FieldCaiFacAtkCdMin", "@CaiFacAtkCdMin");
        bindValueChanged(events, "#FieldCaiFacAtkCdMax", "@CaiFacAtkCdMax");
        bindValueChanged(events, "#FieldCaiFacShieldCharge", "@CaiFacShieldCharge");
        bindValueChanged(events, "#FieldCaiFacShieldSwitch", "@CaiFacShieldSwitch");
        bindValueChanged(events, "#FieldCaiFacBoDistMin", "@CaiFacBoDistMin");
        bindValueChanged(events, "#FieldCaiFacBoDistMax", "@CaiFacBoDistMax");
        bindValueChanged(events, "#FieldCaiFacBoSwitch", "@CaiFacBoSwitch");
        bindValueChanged(events, "#FieldCaiFacRetDistMin", "@CaiFacRetDistMin");
        bindValueChanged(events, "#FieldCaiFacRetDistMax", "@CaiFacRetDistMax");
        bindValueChanged(events, "#FieldCaiFacRetWeight", "@CaiFacRetWeight");
        bindValueChanged(events, "#FieldCaiFacReEngMin", "@CaiFacReEngMin");
        bindValueChanged(events, "#FieldCaiFacReEngMax", "@CaiFacReEngMax");
        bindValueChanged(events, "#FieldCaiFacReEngRandMin", "@CaiFacReEngRandMin");
        bindValueChanged(events, "#FieldCaiFacReEngRandMax", "@CaiFacReEngRandMax");
        bindValueChanged(events, "#FieldCaiFacStrafeCdMin", "@CaiFacStrafeCdMin");
        bindValueChanged(events, "#FieldCaiFacStrafeCdMax", "@CaiFacStrafeCdMax");
        bindValueChanged(events, "#FieldCaiTierCdMin", "@CaiTierCdMin");
        bindValueChanged(events, "#FieldCaiTierCdMax", "@CaiTierCdMax");
        bindValueChanged(events, "#FieldCaiTierStrCdMin", "@CaiTierStrCdMin");
        bindValueChanged(events, "#FieldCaiTierStrCdMax", "@CaiTierStrCdMax");
        bindValueChanged(events, "#FieldCaiTierShieldCharge", "@CaiTierShieldCharge");
        bindValueChanged(events, "#FieldCaiTierGuardCd", "@CaiTierGuardCd");
        bindValueChanged(events, "#FieldCaiTierRetHealth", "@CaiTierRetHealth");
        bindValueChanged(events, "#FieldCaiWpnRange", "@CaiWpnRange");
        bindValueChanged(events, "#FieldCaiWpnSpeed", "@CaiWpnSpeed");

        bindAction(events, "#OverlayEnabledOn", "ToggleOverlayEnabled");
        bindAction(events, "#OverlayEnabledOff", "ToggleOverlayEnabled");
        bindAction(events, "#FallDamageOn", "ToggleFallDamage");
        bindAction(events, "#FallDamageOff", "ToggleFallDamage");
        bindAction(events, "#HealthScalingOn", "ToggleHealthScaling");
        bindAction(events, "#HealthScalingOff", "ToggleHealthScaling");
        bindAction(events, "#DamageScalingOn", "ToggleDamageScaling");
        bindAction(events, "#DamageScalingOff", "ToggleDamageScaling");

        bindValueChanged(events, "#AbilTreeFilter", "@AbilTreeFilter");
        for (int i = 0; i < AdminUIData.TREE_ROW_COUNT; i++) {
            bindActionWithFilter(events, "#AbilRowItm" + i, "AbilRowClick_" + i, "@AbilTreeFilter", "#AbilTreeFilter");
            bindActionWithFilter(events, "#AbilRowItmOff" + i, "AbilRowClick_" + i, "@AbilTreeFilter", "#AbilTreeFilter");
            bindActionWithFilter(events, "#AbilRowTogOn" + i, "AbilRowToggle_" + i, "@AbilTreeFilter", "#AbilTreeFilter");
            bindActionWithFilter(events, "#AbilRowTogOff" + i, "AbilRowToggle_" + i, "@AbilTreeFilter", "#AbilTreeFilter");
        }

        bindValueChanged(events, "#AbilMobFilter", "@AbilMobFilter");
        for (int i = 0; i < ABIL_MOB_PAGE_SIZE; i++) {
            bindActionWithFilter(events, "#AbilMobDel" + i, "AbilMobDel_" + i, "@AbilMobFilter", "#AbilMobFilter");
            bindActionWithFilter(events, "#AbilMobPeek" + i, "AbilMobPeek_" + i, "@AbilMobFilter", "#AbilMobFilter");
            for (int t = 0; t < AdminUIData.TIERS_COUNT; t++) {
                bindActionWithFilter(events, "#AbilMobTierOn" + i + "T" + t, "AbilMobTier_" + i + "_" + t, "@AbilMobFilter", "#AbilMobFilter");
                bindActionWithFilter(events, "#AbilMobTierOff" + i + "T" + t, "AbilMobTier_" + i + "_" + t, "@AbilMobFilter", "#AbilMobFilter");
            }
        }
        bindActionWithFilter(events, "#AbilMobFirstPage", "AbilMobFirstPage", "@AbilMobFilter", "#AbilMobFilter");
        bindActionWithFilter(events, "#AbilMobPrevPage", "AbilMobPrevPage", "@AbilMobFilter", "#AbilMobFilter");
        bindActionWithFilter(events, "#AbilMobNextPage", "AbilMobNextPage", "@AbilMobFilter", "#AbilMobFilter");
        bindActionWithFilter(events, "#AbilMobLastPage", "AbilMobLastPage", "@AbilMobFilter", "#AbilMobFilter");
        bindActionWithFilter(events, "#AbilAddCategory", "AbilAddCategory", "@AbilMobFilter", "#AbilMobFilter");
        bindActionWithFilter(events, "#AbilAddMob", "AbilAddMob", "@AbilMobFilter", "#AbilMobFilter");
        bindActionWithFilter(events, "#AbilMobDeleteFiltered", "AbilMobDeleteFiltered", "@AbilMobFilter", "#AbilMobFilter");
        bindActionWithFilter(events, "#AbilMobDeleteAll", "AbilMobDeleteAll", "@AbilMobFilter", "#AbilMobFilter");

        bindAction(events, "#AbilCfgGateAdd", "AbilCfgGateAdd");
        for (int i = 0; i < ABIL_CFG_LIST_PAGE_SIZE; i++) {
            bindAction(events, "#AbilCfgGateDel" + i, "AbilCfgGateDel_" + i);
            bindAction(events, "#AbilCfgGatePeek" + i, "AbilCfgGatePeek_" + i);
        }
        bindAction(events, "#AbilCfgGateFirstPage", "AbilCfgGateFirstPage");
        bindAction(events, "#AbilCfgGatePrevPage", "AbilCfgGatePrevPage");
        bindAction(events, "#AbilCfgGateNextPage", "AbilCfgGateNextPage");
        bindAction(events, "#AbilCfgGateLastPage", "AbilCfgGateLastPage");

        bindAction(events, "#AbilCfgExclAdd", "AbilCfgExclAdd");
        for (int i = 0; i < ABIL_CFG_LIST_PAGE_SIZE; i++) {
            bindAction(events, "#AbilCfgExclDel" + i, "AbilCfgExclDel_" + i);
        }
        bindAction(events, "#AbilCfgExclFirstPage", "AbilCfgExclFirstPage");
        bindAction(events, "#AbilCfgExclPrevPage", "AbilCfgExclPrevPage");
        bindAction(events, "#AbilCfgExclNextPage", "AbilCfgExclNextPage");
        bindAction(events, "#AbilCfgExclLastPage", "AbilCfgExclLastPage");

        bindValueChanged(events, "#AbilCfgCLMinRange", "@AbilCfgCLMinRange");
        bindValueChanged(events, "#AbilCfgCLMaxRange", "@AbilCfgCLMaxRange");
        bindAction(events, "#AbilCfgCLFaceTargetOn", "AbilCfgCLFaceTarget");
        bindAction(events, "#AbilCfgCLFaceTargetOff", "AbilCfgCLFaceTarget");
        for (int i = 0; i < AdminUIData.TIERS_COUNT; i++) {
            bindValueChanged(events, "#AbilCfgCLChance" + i, "@AbilCfgCLChance" + i);
            bindValueChanged(events, "#AbilCfgCLCooldown" + i, "@AbilCfgCLCooldown" + i);
            bindValueChanged(events, "#AbilCfgCLSlamRange" + i, "@AbilCfgCLSlamRange" + i);
            bindValueChanged(events, "#AbilCfgCLSlamDmg" + i, "@AbilCfgCLSlamDmg" + i);
            bindValueChanged(events, "#AbilCfgCLForce" + i, "@AbilCfgCLForce" + i);
            bindValueChanged(events, "#AbilCfgCLKBLift" + i, "@AbilCfgCLKBLift" + i);
            bindValueChanged(events, "#AbilCfgCLKBPush" + i, "@AbilCfgCLKBPush" + i);
            bindValueChanged(events, "#AbilCfgCLKBForce" + i, "@AbilCfgCLKBForce" + i);
        }

        bindValueChanged(events, "#AbilCfgHLMinHealth", "@AbilCfgHLMinHealth");
        bindValueChanged(events, "#AbilCfgHLMaxHealth", "@AbilCfgHLMaxHealth");
        bindValueChanged(events, "#AbilCfgHLInstantChance", "@AbilCfgHLInstantChance");
        bindValueChanged(events, "#AbilCfgHLDrinkDur", "@AbilCfgHLDrinkDur");
        bindAction(events, "#AbilCfgHLDrinkItemPick", "AbilCfgHLDrinkItemPick");
        for (int i = 0; i < AdminUIData.TIERS_COUNT; i++) {
            bindValueChanged(events, "#AbilCfgHLChance" + i, "@AbilCfgHLChance" + i);
            bindValueChanged(events, "#AbilCfgHLCooldown" + i, "@AbilCfgHLCooldown" + i);
            bindValueChanged(events, "#AbilCfgHLHeal" + i, "@AbilCfgHLHeal" + i);
            bindValueChanged(events, "#AbilCfgHLForce" + i, "@AbilCfgHLForce" + i);
        }

        for (int i = 0; i < AdminUIData.TIERS_COUNT; i++) {
            bindValueChanged(events, "#AbilCfgSMChance" + i, "@AbilCfgSMChance" + i);
            bindValueChanged(events, "#AbilCfgSMCooldown" + i, "@AbilCfgSMCooldown" + i);
        }
        bindValueChanged(events, "#AbilCfgSMMaxMinions", "@AbilCfgSMMaxMinions");
        bindValueChanged(events, "#AbilCfgSMSkelW", "@AbilCfgSMSkelW");
        bindValueChanged(events, "#AbilCfgSMZombW", "@AbilCfgSMZombW");
        bindValueChanged(events, "#AbilCfgSMWraithW", "@AbilCfgSMWraithW");
        bindValueChanged(events, "#AbilCfgSMAbrrW", "@AbilCfgSMAbrrW");

        bindAction(events, "#AbilCfgSMRoleAdd", "AbilCfgSMRoleAdd");
        for (int i = 0; i < ABIL_CFG_LIST_PAGE_SIZE; i++) {
            bindAction(events, "#AbilCfgSMRoleDel" + i, "AbilCfgSMRoleDel_" + i);
        }
        bindAction(events, "#AbilCfgSMRoleFirstPage", "AbilCfgSMRoleFirstPage");
        bindAction(events, "#AbilCfgSMRolePrevPage", "AbilCfgSMRolePrevPage");
        bindAction(events, "#AbilCfgSMRoleNextPage", "AbilCfgSMRoleNextPage");
        bindAction(events, "#AbilCfgSMRoleLastPage", "AbilCfgSMRoleLastPage");

        bindAction(events, "#AbilCfgSMExclAdd", "AbilCfgSMExclAdd");
        for (int i = 0; i < ABIL_CFG_LIST_PAGE_SIZE; i++) {
            bindAction(events, "#AbilCfgSMExclDel" + i, "AbilCfgSMExclDel_" + i);
        }
        bindAction(events, "#AbilCfgSMExclFirstPage", "AbilCfgSMExclFirstPage");
        bindAction(events, "#AbilCfgSMExclPrevPage", "AbilCfgSMExclPrevPage");
        bindAction(events, "#AbilCfgSMExclNextPage", "AbilCfgSMExclNextPage");
        bindAction(events, "#AbilCfgSMExclLastPage", "AbilCfgSMExclLastPage");

        for (int i = 0; i < MS_VARIANT_KEYS.length; i++) {
            bindAction(events, "#AbilCfgMSVarBtn" + i, "AbilCfgMSVar_" + i);
            bindAction(events, "#AbilCfgMSVarBtnActive" + i, "AbilCfgMSVar_" + i);
        }

        bindAction(events, "#ProgStyleEnv", "SetProgStyle_ENVIRONMENT");
        bindAction(events, "#ProgStyleEnvActive", "SetProgStyle_ENVIRONMENT");
        bindAction(events, "#ProgStyleDist", "SetProgStyle_DISTANCE_FROM_SPAWN");
        bindAction(events, "#ProgStyleDistActive", "SetProgStyle_DISTANCE_FROM_SPAWN");
        bindAction(events, "#ProgStyleNone", "SetProgStyle_NONE");
        bindAction(events, "#ProgStyleNoneActive", "SetProgStyle_NONE");

        bindAction(events, "#PresetFull", "ApplyPreset_full");
        bindAction(events, "#PresetFullActive", "ApplyPreset_full");
        bindAction(events, "#PresetEmpty", "ApplyPreset_empty");
        bindAction(events, "#PresetEmptyActive", "ApplyPreset_empty");
        bindAction(events, "#PresetCustom", "ApplyPreset_custom");
        bindAction(events, "#PresetCustomActive", "ApplyPreset_custom");
        bindAction(events, "#PresetGlobal", "ApplyPreset_customGlobal");
        bindAction(events, "#PresetGlobalActive", "ApplyPreset_customGlobal");
        bindAction(events, "#SaveWorldCustomPreset", "SaveWorldCustomPreset");
        bindAction(events, "#SaveGlobalCustomPreset", "SaveGlobalCustomPreset");

        for (int i = 0; i < 5; i++) {
            bindValueChanged(events, "#FieldSpawnChance" + i, "@SpawnChance" + i);
            bindValueChanged(events, "#FieldHealth" + i, "@Health" + i);
            bindValueChanged(events, "#FieldDamage" + i, "@Damage" + i);
            bindValueChanged(events, "#FieldExtraRolls" + i, "@ExtraRolls" + i);
        }
        bindValueChanged(events, "#FieldDistancePerTier", "@DistPerTier");
        bindValueChanged(events, "#FieldDistanceBonusInterval", "@DistBonusInterval");
        bindValueChanged(events, "#FieldDistHealthBonus", "@DistHealthBonus");
        bindValueChanged(events, "#FieldDistDamageBonus", "@DistDamageBonus");
        bindValueChanged(events, "#FieldDistHealthCap", "@DistHealthCap");
        bindValueChanged(events, "#FieldDistDamageCap", "@DistDamageCap");
        bindValueChanged(events, "#FieldHealthVariance", "@HealthVariance");
        bindValueChanged(events, "#FieldDamageVariance", "@DamageVariance");
        bindValueChanged(events, "#FieldDropWeapon", "@DropWeapon");
        bindValueChanged(events, "#FieldDropArmor", "@DropArmor");
        bindValueChanged(events, "#FieldDropOffhand", "@DropOffhand");

        bindAction(events, "#EnableNameplatesOn", "ToggleEnableNameplates");
        bindAction(events, "#EnableNameplatesOff", "ToggleEnableNameplates");
        bindAction(events, "#EnableModelScalingOn", "ToggleEnableModelScaling");
        bindAction(events, "#EnableModelScalingOff", "ToggleEnableModelScaling");
        bindAction(events, "#NameplateModeRanked", "SetNameplateMode_RANKED_ROLE");
        bindAction(events, "#NameplateModeRankedActive", "SetNameplateMode_RANKED_ROLE");
        bindAction(events, "#AddFamilyButton", "AddFamily");
        for (int i = 0; i < 5; i++) {
            bindAction(events, "#NameplateTierOn" + i, "ToggleNameplateTier_" + i);
            bindAction(events, "#NameplateTierOff" + i, "ToggleNameplateTier_" + i);
        }

        for (int i = 0; i < 5; i++) {
            bindValueChanged(events, "#FieldNameplatePrefix" + i, "@NameplatePrefix" + i);
            bindValueChanged(events, "#FieldModelScale" + i, "@ModelScale" + i);
        }
        bindValueChanged(events, "#FieldModelVariance", "@ModelVariance");

        for (int i = 0; i < AdminUIData.MAX_FAMILIES; i++) {
            bindValueChanged(events, "#FieldFamilyKey" + i, "@FamilyKey" + i);
            bindValueChanged(events, "#FamilyT1r" + i, "@FamilyT1r" + i);
            bindValueChanged(events, "#FamilyT2r" + i, "@FamilyT2r" + i);
            bindValueChanged(events, "#FamilyT3r" + i, "@FamilyT3r" + i);
            bindValueChanged(events, "#FamilyT4r" + i, "@FamilyT4r" + i);
            bindValueChanged(events, "#FamilyT5r" + i, "@FamilyT5r" + i);
            bindAction(events, "#FamilyDelete" + i, "DeleteFamily_" + i);
        }

        bindAction(events, "#AddEnvRuleButton", "AddEnvRule");
        for (int i = 0; i < AdminUIData.MAX_ENV_RULES; i++) {
            bindValueChanged(events, "#FieldEnvKey" + i, "@EnvKey" + i);
            bindValueChanged(events, "#EnvT1r" + i, "@EnvT1r" + i);
            bindValueChanged(events, "#EnvT2r" + i, "@EnvT2r" + i);
            bindValueChanged(events, "#EnvT3r" + i, "@EnvT3r" + i);
            bindValueChanged(events, "#EnvT4r" + i, "@EnvT4r" + i);
            bindValueChanged(events, "#EnvT5r" + i, "@EnvT5r" + i);
            bindAction(events, "#EnvDelete" + i, "DeleteEnvRule_" + i);
        }

        bindValueChanged(events, "#EffectTreeFilter", "@EffectTreeFilter");
        for (int i = 0; i < AdminUIData.TREE_ROW_COUNT; i++) {
            bindAction(events, "#EffectRowItm" + i, "EffectRowClick_" + i);
            bindAction(events, "#EffectRowItmOff" + i, "EffectRowClick_" + i);
            bindAction(events, "#EffectRowTogOn" + i, "EffectRowToggle_" + i);
            bindAction(events, "#EffectRowTogOff" + i, "EffectRowToggle_" + i);
        }

        bindAction(events, "#EffectDetailInfiniteOn", "EffectDetailInfinite");
        bindAction(events, "#EffectDetailInfiniteOff", "EffectDetailInfinite");
        for (int t = 0; t < 5; t++) {
            bindAction(events, "#EffectDetailTierOn" + t, "EffectDetailTier_" + t);
            bindAction(events, "#EffectDetailTierOff" + t, "EffectDetailTier_" + t);
            bindValueChanged(events, "#EffectDetailMult" + t, "@EffectDetailMult" + t);
        }

        bindAction(events, "#RPGLevelingOverlayOn", "ToggleRPGLevelingOverlay");
        bindAction(events, "#RPGLevelingOverlayOff", "ToggleRPGLevelingOverlay");
        for (int i = 0; i < 5; i++) {
            bindValueChanged(events, "#FieldOverlayXPMult" + i, "@OverlayXPMult" + i);
        }
        bindValueChanged(events, "#FieldOverlayXPBonusPerAbility", "@OverlayXPBonusPerAbility");
        bindValueChanged(events, "#FieldOverlayMinionXPMult", "@OverlayMinionXPMult");

        bindValueChanged(events, "#TierOvrFilter", "@TierOvrFilter");
        bindActionWithFilter(events, "#AddTierOvrCategory", "AddTierOvrCategory", "@TierOvrFilter", "#TierOvrFilter");
        bindActionWithFilter(events, "#AddTierOvrMob", "AddTierOvrMob", "@TierOvrFilter", "#TierOvrFilter");
        bindActionWithFilter(events, "#TierOvrFirstPage", "TierOvrFirstPage", "@TierOvrFilter", "#TierOvrFilter");
        bindActionWithFilter(events, "#TierOvrPrevPage", "TierOvrPrevPage", "@TierOvrFilter", "#TierOvrFilter");
        bindActionWithFilter(events, "#TierOvrNextPage", "TierOvrNextPage", "@TierOvrFilter", "#TierOvrFilter");
        bindActionWithFilter(events, "#TierOvrLastPage", "TierOvrLastPage", "@TierOvrFilter", "#TierOvrFilter");
        for (int i = 0; i < AdminUIData.TIER_OVERRIDE_PAGE_SIZE; i++) {
            bindActionWithFilter(events, "#TierOvrDel" + i, "TierOvrDel_" + i, "@TierOvrFilter", "#TierOvrFilter");
            bindActionWithFilter(events, "#TierOvrPeek" + i, "TierOvrPeek_" + i, "@TierOvrFilter", "#TierOvrFilter");
            for (int t = 0; t < 5; t++) {
                bindActionWithFilter(events, "#TierOvrTierOn" + i + "T" + t, "TierOvrTier_" + i + "_" + t, "@TierOvrFilter", "#TierOvrFilter");
                bindActionWithFilter(events, "#TierOvrTierOff" + i + "T" + t, "TierOvrTier_" + i + "_" + t, "@TierOvrFilter", "#TierOvrFilter");
            }
        }

        needsFieldRefresh = true;
        populateCommands(commands);
    }

    private void bindAction(UIEventBuilder events, String selector, String action) {
        events.addEventBinding(CustomUIEventBindingType.Activating, selector, EventData.of("Action", action), false);
    }

    private void bindActionWithFilter(UIEventBuilder events, String selector, String action, String filterCodecKey, String filterSelector) {
        events.addEventBinding(CustomUIEventBindingType.Activating, selector,
                EventData.of("Action", action).append(filterCodecKey, filterSelector + ".Value"), false);
    }

    private void bindValueChanged(UIEventBuilder events, String selector, String dataKey) {
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, selector, EventData.of(dataKey, selector + ".Value"), false);
    }

    @Override
    public void handleDataEvent(@NonNull Ref<EntityStore> ref, @NonNull Store<EntityStore> store, @NonNull AdminUIData data) {
        if (data == null) return;

        processGlobalNumberFields(data);

        processOverlayNumberFields(data);

        processLootTemplateDropFields(data);

        processGlobMobRuleDetailFields(data);
        if (data.renamePopupField != null) {
            pendingRenameName = data.renamePopupField;
        }
        updateFilter(data.itemPickerFilter, itemPickerFilter, v -> itemPickerFilter = v, () -> { itemPickerPage = 0; rebuildItemPickerFiltered(); });
        if (data.itemPickerCustomId != null) {
            itemPickerCustomId = data.itemPickerCustomId;
        }
        updateFilter(data.npcPickerFilter, npcPickerFilter, v -> npcPickerFilter = v, () -> { npcPickerPage = 0; rebuildNpcPickerFiltered(); });
        if (data.npcPickerCustomId != null) {
            npcPickerCustomId = data.npcPickerCustomId;
        }
        updateFilter(data.abilTreeFilter, abilTreeFilter, v -> abilTreeFilter = v, () -> { abilityExpandedIndex = -1; rebuildAbilTreeFiltered(); });
        updateFilter(data.abilMobFilter, abilityMobFilter, v -> abilityMobFilter = v, () -> { abilityMobPage = 0; rebuildAbilMobFiltered(); });
        updateFilter(data.lootTplMobFilter, lootTplMobFilter, v -> lootTplMobFilter = v, () -> { lootTplMobPage = 0; rebuildLootTplMobFiltered(); });
        updateFilter(data.mobRuleTreeFilter, mobRuleTreeFilter, v -> mobRuleTreeFilter = v, () -> { mobRuleTreeExpandedIndex = -1; rebuildMobRuleTreeFiltered(); });
        updateFilter(data.lootTreeFilter, lootTreeFilter, v -> lootTreeFilter = v, () -> { lootTemplateExpandedIndex = -1; rebuildLootTreeFiltered(); });
        updateFilter(data.effectTreeFilter, effectTreeFilter, v -> effectTreeFilter = v, () -> { effectExpandedIndex = -1; rebuildEffectTreeFiltered(); });
        updateFilter(data.wpnCatTreeFilter, wpnCatTreeFilter, v -> wpnCatTreeFilter = v, () -> { wpnCatFilterPage = 0; rebuildGearCatTreeFiltered(true); });
        updateFilter(data.armCatTreeFilter, armCatTreeFilter, v -> armCatTreeFilter = v, () -> { armCatFilterPage = 0; rebuildGearCatTreeFiltered(false); });
        updateFilter(data.mobRuleWpnCatFilter, mobRuleWpnCatFilter, v -> mobRuleWpnCatFilter = v, () -> mobRuleWpnCatPage = 0);
        updateFilter(data.mobRuleArmCatFilter, mobRuleArmCatFilter, v -> mobRuleArmCatFilter = v, () -> mobRuleArmCatPage = 0);

        if (activeSection == Section.GLOBAL_CONFIG && activeSubTab == TAB_CONFIG_RARITY) {
            parseRarityTiersTextFields(data);
        }
        if (activeSection == Section.GLOBAL_COMBAT_AI) {
            parseCombatAITextFields(data);
        }
        if (data.action != null && !data.action.isBlank()) {
            saveMessage = null;
            if (!data.action.startsWith("ApplyPreset_")) {
                templateAppliedMessage = null;
            }
            handleAction(data.action);
        }

        sendUpdate(buildUpdate());
    }

    private void handleAction(String action) {
        switch (action) {
            case "Close" -> close();

            case "NavGlobalCore" -> { stashCurrentOverlay(); activeSection = Section.GLOBAL_CORE; activeSubTab = 0; editOverlay = null; needsFieldRefresh = true; }
            case "NavGlobalDebug" -> { stashCurrentOverlay(); activeSection = Section.GLOBAL_DEBUG; activeSubTab = 0; editOverlay = null; needsFieldRefresh = true; }
            case "NavGlobalMobRules" -> { stashCurrentOverlay(); activeSection = Section.GLOBAL_MOB_RULES; activeSubTab = 0; editOverlay = null; needsFieldRefresh = true; }
            case "NavGlobalCombatAI" -> { stashCurrentOverlay(); activeSection = Section.GLOBAL_COMBAT_AI; activeSubTab = 0; editOverlay = null; needsFieldRefresh = true; }
            case "NavGlobalConfig" -> { stashCurrentOverlay(); activeSection = Section.GLOBAL_CONFIG; activeSubTab = 0; editOverlay = null; needsFieldRefresh = true; }

            case "MobRuleNavBack" -> {
                if (isPerWorldMobRuleMode()) {
                    if (perWorldCurrentCategory != null) {
                        var root = editMobRuleCategoryTree != null ? editMobRuleCategoryTree
                                : new RPGMobsConfig.MobRuleCategory("All", List.of());
                        var parent = findParentMobRuleCategory(root, perWorldCurrentCategory);
                        perWorldCurrentCategory = (parent == root) ? null : parent;
                    }
                    needsFieldRefresh = true;
                } else {
                    mobRuleTreeExpandedIndex = -1;
                    mobRuleTreeBack();
                }
            }
            case "MobRuleNavForward" -> { mobRuleTreeExpandedIndex = -1; mobRuleTreeForward(); }
            case "AddMobRuleCategory" -> addMobRuleCategory();
            case "AddMobRuleItem" -> addMobRuleItem();

            case "GlobMobRuleRebind" -> openMobRuleRebindPicker();
            case "GlobMobRuleCycleMode" -> cycleGlobMobRuleWeaponMode();
            case "GlobMobRuleCombatStyle" -> cycleGlobMobRuleCombatStyle();

            case "LootNavBack" -> lootTreeBack();
            case "LootNavForward" -> lootTreeForward();
            case "AddLootCategory" -> addLootCategory();
            case "AddLootTemplate" -> addLootTemplateItem();

            case "LootTplAddCategory" -> openLootTplCategoryPicker();
            case "LootTplAddMob" -> openLootTplNpcPicker();
            case "LootTplMobFirstPage", "LootTplMobPrevPage", "LootTplMobNextPage", "LootTplMobLastPage" ->
                handlePaginationAction(action, "LootTplMob", lootTplMobPage, lootTplMobFiltered.size(), LOOT_TPL_MOB_PAGE_SIZE, p -> lootTplMobPage = p);
            case "LootTplMobDeleteFiltered" -> handleLootTplMobDeleteFiltered();
            case "LootTplMobDeleteAll" -> handleLootTplMobDeleteAll();

            case "LootTplAddDrop" -> addLootTemplateDrop();
            case "LootTplDropFirstPage", "LootTplDropPrevPage", "LootTplDropNextPage", "LootTplDropLastPage" -> {
                var lt = getExpandedLootTemplate();
                if (lt != null) handlePaginationAction(action, "LootTplDrop", lootTemplateDropPage, lt.drops.size(), AdminUIData.LOOT_TEMPLATE_DROPS_PER_PAGE, p -> lootTemplateDropPage = p);
                needsFieldRefresh = true;
            }

            case "LinkPopupNavBack" -> linkPopupBack();
            case "LinkPopupConfirm" -> confirmLinkPopup();
            case "LinkPopupCancel" -> closeLinkPopup();

            case "CatPeekClose" -> categoryPeekOpen = false;
            case "CatPeekExtract" -> handleCatPeekExtract();
            case "CatPeekFirstPage", "CatPeekPrevPage", "CatPeekNextPage", "CatPeekLastPage" ->
                handlePaginationAction(action, "CatPeek", categoryPeekPage, categoryPeekItems.size(), CATEGORY_PEEK_PAGE_SIZE, p -> categoryPeekPage = p);

            case "RenamePopupConfirm" -> confirmRenamePopup();
            case "RenamePopupCancel" -> closeRenamePopup();

            case "MovePopupNavBack" -> movePopupBack();
            case "MovePopupConfirm" -> confirmMovePopup();
            case "MovePopupCancel" -> closeMovePopup();

            case "ItemPickerCancel" -> closeItemPicker();
            case "ItemPickerFirstPage", "ItemPickerPrevPage", "ItemPickerNextPage", "ItemPickerLastPage" -> {
                handlePaginationAction(action, "ItemPicker", itemPickerPage, itemPickerFiltered.size(), ITEM_PICKER_ROW_COUNT, p -> itemPickerPage = p);
                needsFieldRefresh = true;
            }
            case "ItemPickerUseCustom" -> handleItemPickerUseCustom();
            case "ItemPickerAdd" -> { applyItemPickerSelection(); needsFieldRefresh = true; }

            case "NpcPickerCancel" -> closeNpcPicker();
            case "NpcPickerFirstPage", "NpcPickerPrevPage", "NpcPickerNextPage", "NpcPickerLastPage" -> {
                handlePaginationAction(action, "NpcPicker", npcPickerPage, npcPickerFiltered.size(), NPC_PICKER_ROW_COUNT, p -> npcPickerPage = p);
                needsFieldRefresh = true;
            }
            case "NpcPickerUseCustom" -> handleNpcPickerUseCustom();
            case "NpcPickerAdd" -> { applyNpcPickerSelection(); needsFieldRefresh = true; }

            case "FirstWorlds" -> worldPage = 0;
            case "PrevWorlds" -> worldPage = Math.max(0, worldPage - 1);
            case "NextWorlds" -> { int maxPage = Math.max(0, (worldNames.size() - 1) / SIDEBAR_PAGE_SIZE); worldPage = Math.min(worldPage + 1, maxPage); }
            case "LastWorlds" -> worldPage = Math.max(0, (worldNames.size() - 1) / SIDEBAR_PAGE_SIZE);
            case "FirstInsts" -> instPage = 0;
            case "PrevInsts" -> instPage = Math.max(0, instPage - 1);
            case "NextInsts" -> { int maxPage = Math.max(0, (instanceNames.size() - 1) / SIDEBAR_PAGE_SIZE); instPage = Math.min(instPage + 1, maxPage); }
            case "LastInsts" -> instPage = Math.max(0, (instanceNames.size() - 1) / SIDEBAR_PAGE_SIZE);

            case "SubTab_0","SubTab_1","SubTab_2","SubTab_3","SubTab_4","SubTab_5","SubTab_6","SubTab_7","SubTab_8" -> {
                int newTab = Integer.parseInt(action.substring("SubTab_".length()));
                if (newTab != activeSubTab) {
                    activeSubTab = newTab;
                    needsFieldRefresh = true;
                }
            }

            case "ToggleGlobalEnabled" -> editGlobalEnabled = !editGlobalEnabled;
            case "ToggleEnabledByDefault" -> editEnabledByDefault = !editEnabledByDefault;
            case "ToggleDebugMode" -> editDebugMode = !editDebugMode;
            case "ToggleOverlayEnabled" -> toggleOverlayBoolean(() -> editOverlay.enabled, v -> editOverlay.enabled = v, r -> r.enabled);

            case "AbilMobFirstPage", "AbilMobPrevPage", "AbilMobNextPage", "AbilMobLastPage" ->
                handlePaginationAction(action, "AbilMob", abilityMobPage, abilityMobFiltered.size(), ABIL_MOB_PAGE_SIZE, p -> abilityMobPage = p);
            case "AbilAddCategory" -> openAbilCategoryPicker();
            case "AbilAddMob" -> openAbilNpcPicker();
            case "AbilMobDeleteFiltered" -> handleAbilMobDeleteFiltered();
            case "AbilMobDeleteAll" -> handleAbilMobDeleteAll();
            case "EffectDetailInfinite" -> handleEffectDetailInfinite();
            case "MobRuleDeleteFiltered" -> handleMobRuleDeleteFiltered();
            case "MobRuleDeleteAll" -> handleMobRuleDeleteAll();
            case "LootDeleteFiltered" -> handleLootDeleteFiltered();
            case "LootDeleteAll" -> handleLootDeleteAll();
            case "ToggleFallDamage" -> toggleOverlayBoolean(() -> editOverlay.eliteFallDamageDisabled, v -> editOverlay.eliteFallDamageDisabled = v, r -> r.eliteFallDamageDisabled);

            case "ToggleHealthScaling" -> toggleOverlayBoolean(() -> editOverlay.enableHealthScaling, v -> editOverlay.enableHealthScaling = v, r -> r.enableHealthScaling);
            case "ToggleDamageScaling" -> toggleOverlayBoolean(() -> editOverlay.enableDamageScaling, v -> editOverlay.enableDamageScaling = v, r -> r.enableDamageScaling);

            case "ToggleRPGLevelingOverlay" -> toggleOverlayBoolean(() -> editOverlay.rpgLevelingEnabled, v -> editOverlay.rpgLevelingEnabled = v, r -> r.rpgLevelingEnabled);

            case "AddTierOvrCategory" -> openTierRestrictionCategoryPicker();
            case "AddTierOvrMob" -> openTierRestrictionNpcPicker();
            case "TierOvrFirstPage", "TierOvrPrevPage", "TierOvrNextPage", "TierOvrLastPage" -> {
                handlePaginationAction(action, "TierOvr", tierOverridePage, buildTierOverrideVisibleIndices().size(), AdminUIData.TIER_OVERRIDE_PAGE_SIZE, p -> tierOverridePage = p);
                needsFieldRefresh = true;
            }

            case "ToggleEnableNameplates" -> toggleOverlayBoolean(() -> editOverlay.enableNameplates, v -> editOverlay.enableNameplates = v, r -> r.enableNameplates);
            case "ToggleEnableModelScaling" -> toggleOverlayBoolean(() -> editOverlay.enableModelScaling, v -> editOverlay.enableModelScaling = v, r -> r.enableModelScaling);
            case "AddFamily" -> addFamilyRow();
            case "AddEnvRule" -> addEnvRuleRow();

            case "SetProgStyle_ENVIRONMENT" -> setProgressionStyle("ENVIRONMENT");
            case "SetProgStyle_DISTANCE_FROM_SPAWN" -> setProgressionStyle("DISTANCE_FROM_SPAWN");
            case "SetProgStyle_NONE" -> setProgressionStyle("NONE");

            case "SetNameplateMode_RANKED_ROLE" -> setNameplateMode("RANKED_ROLE");

            case "SaveReload" -> handleSaveReload();
            case "Discard" -> handleDiscard();
            case "SaveWorldCustomPreset" -> saveWorldCustomPreset();
            case "SaveGlobalCustomPreset" -> saveGlobalCustomPreset();

            default -> {
                if (!handleCombatAIAction(action) && !handleAbilityAction(action) && !handleAbilityConfigAction(action) && !handleMobRuleTreeAction(
                        action) && !handleMobRuleDetailAction(action) && !handleLootTreeAction(action) && !handleLootDetailAction(
                        action) && !handleGearCategoryAction(action) && !handleRarityTiersAction(action) && !handleOverlayToggleAction(
                        action) && !handlePickerAction(action)) {
                    handleSidebarAction(action);
                }
            }
        }
    }

    private boolean handleAbilityAction(String action) {
        if (action.startsWith("AbilRowClick_")) {
            handleAbilRowClick(parseIdx(action, "AbilRowClick_"));
        } else if (action.startsWith("AbilRowToggle_")) {
            handleAbilRowToggle(parseIdx(action, "AbilRowToggle_"));
        } else if (action.startsWith("AbilMobTier_")) {
            handleAbilMobTierToggle(action);
        } else if (action.startsWith("AbilMobPeek_")) {
            handleAbilMobPeek(parseIdx(action, "AbilMobPeek_"));
        } else if (action.startsWith("AbilMobDel_")) {
            handleAbilMobDelete(parseIdx(action, "AbilMobDel_"));
        } else {
            return false;
        }
        return true;
    }

    private boolean handleAbilityConfigAction(String action) {
        if (action.equals("AbilCfgGateAdd")) {
            handleAbilCfgGateAdd();
        } else if (action.startsWith("AbilCfgGateDel_")) {
            handleAbilCfgGateDel(parseIdx(action, "AbilCfgGateDel_"));
        } else if (action.startsWith("AbilCfgGatePeek_")) {
            handleAbilCfgGatePeek(parseIdx(action, "AbilCfgGatePeek_"));
        } else if (action.startsWith("AbilCfgGate") && action.endsWith("Page")) {
            RPGMobsConfig.AbilityConfig gac = getExpandedAbilityConfig();
            int gateTotal = gac != null && gac.gate != null && gac.gate.allowedWeaponCategories != null ? gac.gate.allowedWeaponCategories.size() : 0;
            handlePaginationAction(action, "AbilCfgGate", abilGatePage, gateTotal, ABIL_CFG_LIST_PAGE_SIZE, p -> abilGatePage = p);
        } else if (action.equals("AbilCfgExclAdd")) {
            handleAbilCfgExclAdd();
        } else if (action.startsWith("AbilCfgExclDel_")) {
            handleAbilCfgExclDel(parseIdx(action, "AbilCfgExclDel_"));
        } else if (action.startsWith("AbilCfgExcl") && action.endsWith("Page")) {
            RPGMobsConfig.AbilityConfig eac = getExpandedAbilityConfig();
            int exclTotal = eac != null && eac.excludeLinkedMobRuleKeys != null ? eac.excludeLinkedMobRuleKeys.size() : 0;
            handlePaginationAction(action, "AbilCfgExcl", abilExclPage, exclTotal, ABIL_CFG_LIST_PAGE_SIZE, p -> abilExclPage = p);
        } else if (action.equals("AbilCfgCLFaceTarget")) {
            handleAbilCfgCLFaceTargetToggle();
        } else if (action.equals("AbilCfgHLDrinkItemPick")) {
            handleAbilCfgHLDrinkItemPick();
        } else if (action.equals("AbilCfgSMRoleAdd")) {
            handleAbilCfgSMRoleAdd();
        } else if (action.startsWith("AbilCfgSMRoleDel_")) {
            handleAbilCfgSMRoleDel(parseIdx(action, "AbilCfgSMRoleDel_"));
        } else if (action.startsWith("AbilCfgSMRole") && action.endsWith("Page")) {
            RPGMobsConfig.AbilityConfig sac = getExpandedAbilityConfig();
            int roleTotal = sac instanceof RPGMobsConfig.SummonAbilityConfig sm && sm.roleIdentifiers != null ? sm.roleIdentifiers.size() : 0;
            handlePaginationAction(action, "AbilCfgSMRole", abilSummonRolePage, roleTotal, ABIL_CFG_LIST_PAGE_SIZE, p -> abilSummonRolePage = p);
        } else if (action.equals("AbilCfgSMExclAdd")) {
            handleAbilCfgSMExclAdd();
        } else if (action.startsWith("AbilCfgSMExclDel_")) {
            handleAbilCfgSMExclDel(parseIdx(action, "AbilCfgSMExclDel_"));
        } else if (action.startsWith("AbilCfgSMExcl") && action.endsWith("Page")) {
            RPGMobsConfig.AbilityConfig sac2 = getExpandedAbilityConfig();
            int exclTotal = sac2 instanceof RPGMobsConfig.SummonAbilityConfig sm2 && sm2.excludeFromSummonPool != null ? sm2.excludeFromSummonPool.size() : 0;
            handlePaginationAction(action, "AbilCfgSMExcl", abilSummonExclPage, exclTotal, ABIL_CFG_LIST_PAGE_SIZE, p -> abilSummonExclPage = p);
        } else if (action.startsWith("AbilCfgMSVar_")) {
            int varIdx = parseIdx(action, "AbilCfgMSVar_");
            if (varIdx >= 0 && varIdx < MS_VARIANT_KEYS.length) {
                multiSlashVariantIndex = varIdx;
                needsFieldRefresh = true;
            }
        } else {
            return false;
        }
        return true;
    }

    private boolean handleMobRuleTreeAction(String action) {
        if (action.startsWith("MobRuleRowToggle_")) {
            handleMobRuleRowToggle(parseIdx(action, "MobRuleRowToggle_"));
        } else if (action.startsWith("MobRuleRowClick_")) {
            handleMobRuleRowClick(parseIdx(action, "MobRuleRowClick_"));
        } else if (action.startsWith("MobRuleRowRen_")) {
            openRenamePopupForMobRule(parseIdx(action, "MobRuleRowRen_"));
        } else if (action.startsWith("MobRuleRowMov_")) {
            openMovePopupForMobRule(parseIdx(action, "MobRuleRowMov_"));
        } else if (action.startsWith("MobRuleRowDel_")) {
            handleMobRuleRowDelete(parseIdx(action, "MobRuleRowDel_"));
        } else {
            return false;
        }
        return true;
    }

    private boolean handleMobRuleDetailAction(String action) {
        if (action.startsWith("ToggleGlobMobRuleWpnTier_")) {
            toggleGlobMobRuleWpnTier(parseIdx(action, "ToggleGlobMobRuleWpnTier_"));
        } else if (action.equals("GlobMobRuleWpnCatAdd")) {
            handleGlobMobRuleWpnCatAdd();
        } else if (action.equals("GlobMobRuleWpnCatAddItem")) {
            handleGlobMobRuleWpnCatAddItem();
        } else if (action.startsWith("GlobMobRuleWpnCatDel_")) {
            handleGlobMobRuleWpnCatDel(parseIdx(action, "GlobMobRuleWpnCatDel_"));
        } else if (action.startsWith("GlobMobRuleWpnCatPeek_")) {
            handleGlobMobRuleWpnCatPeek(parseIdx(action, "GlobMobRuleWpnCatPeek_"));
        } else if (action.equals("GlobMobRuleWpnCatClear")) {
            handleGlobMobRuleWpnCatClear();
        } else if (action.equals("GlobMobRuleWpnCatPrev")) {
            if (mobRuleWpnCatPage > 0) { mobRuleWpnCatPage--; needsFieldRefresh = true; }
        } else if (action.equals("GlobMobRuleWpnCatNext")) {
            handleMobRuleWpnCatNextPage();
        } else if (action.equals("ToggleArmorSlotAll")) {
            toggleArmorSlotAll();
        } else if (action.startsWith("ToggleArmorSlot_")) {
            toggleArmorSlot(action.substring("ToggleArmorSlot_".length()));
        } else if (action.equals("GlobMobRuleArmCatAdd")) {
            handleGlobMobRuleArmCatAdd();
        } else if (action.equals("GlobMobRuleArmCatAddItem")) {
            handleGlobMobRuleArmCatAddItem();
        } else if (action.startsWith("GlobMobRuleArmCatDel_")) {
            handleGlobMobRuleArmCatDel(parseIdx(action, "GlobMobRuleArmCatDel_"));
        } else if (action.startsWith("GlobMobRuleArmCatPeek_")) {
            handleGlobMobRuleArmCatPeek(parseIdx(action, "GlobMobRuleArmCatPeek_"));
        } else if (action.equals("GlobMobRuleArmCatClear")) {
            handleGlobMobRuleArmCatClear();
        } else if (action.equals("GlobMobRuleArmCatPrev")) {
            if (mobRuleArmCatPage > 0) { mobRuleArmCatPage--; needsFieldRefresh = true; }
        } else if (action.equals("GlobMobRuleArmCatNext")) {
            handleMobRuleArmCatNextPage();
        } else {
            return false;
        }
        return true;
    }

    private boolean handleLootTreeAction(String action) {
        if (action.startsWith("LootRowClick_")) {
            handleLootRowClick(parseIdx(action, "LootRowClick_"));
        } else if (action.startsWith("LootRowRen_")) {
            openRenamePopupForLoot(parseIdx(action, "LootRowRen_"));
        } else if (action.startsWith("LootRowMov_")) {
            openMovePopupForLoot(parseIdx(action, "LootRowMov_"));
        } else if (action.startsWith("LootRowDel_")) {
            handleLootRowDelete(parseIdx(action, "LootRowDel_"));
        } else {
            return false;
        }
        return true;
    }

    private boolean handleLootDetailAction(String action) {
        if (action.startsWith("LootTplMobPeek_")) {
            handleLootTplMobPeek(parseIdx(action, "LootTplMobPeek_"));
        } else if (action.startsWith("LootTplMobDel_")) {
            handleLootTplMobDelete(parseIdx(action, "LootTplMobDel_"));
        } else if (action.startsWith("LootTplDropDel_")) {
            deleteLootTemplateDrop(parseIdx(action, "LootTplDropDel_"));
        } else if (action.startsWith("LootTplDropTier_")) {
            handleLootTplDropTierToggle(action);
        } else if (action.startsWith("LootTplDropItemPick_")) {
            openItemPicker(parseIdx(action, "LootTplDropItemPick_"));
        } else {
            return false;
        }
        return true;
    }

    private boolean handleGearCategoryAction(String action) {
        if (action.startsWith("WpnCatRowClick_")) {
            handleGearCatRowClick(parseIdx(action, "WpnCatRowClick_"), true);
        } else if (action.startsWith("WpnCatRowRen_")) {
            handleGearCatRowRename(parseIdx(action, "WpnCatRowRen_"), true);
        } else if (action.startsWith("WpnCatRowChs_")) {
            handleGearCatRowChoose(parseIdx(action, "WpnCatRowChs_"), true);
        } else if (action.startsWith("WpnCatRowMov_")) {
            handleGearCatRowMove(parseIdx(action, "WpnCatRowMov_"), true);
        } else if (action.startsWith("WpnCatRowDel_")) {
            handleGearCatRowDelete(parseIdx(action, "WpnCatRowDel_"), true);
        } else if (action.equals("WpnCatNavBack")) {
            gearCatBack(true);
        } else if (action.equals("WpnCatNavForward")) {
            gearCatForward(true);
        } else if (action.equals("WpnCatAddCategory")) {
            addGearCategory(true);
        } else if (action.equals("WpnCatAddItem")) {
            addGearItem(true);
        } else if (action.equals("WpnCatDeleteFiltered")) {
            deleteFilteredGearItems(true);
        } else if (action.equals("WpnCatDeleteAll")) {
            deleteAllGearItems(true);
        } else if (action.equals("WpnCatPrevPage")) {
            if (wpnCatFilterPage > 0) wpnCatFilterPage--;
        } else if (action.equals("WpnCatNextPage")) {
            int maxPage = Math.max(0, (wpnCatTreeFilteredKeys.size() - 1) / AdminUIData.TREE_ROW_COUNT);
            if (wpnCatFilterPage < maxPage) wpnCatFilterPage++;
        } else if (action.startsWith("ArmCatRowClick_")) {
            handleGearCatRowClick(parseIdx(action, "ArmCatRowClick_"), false);
        } else if (action.startsWith("ArmCatRowRen_")) {
            handleGearCatRowRename(parseIdx(action, "ArmCatRowRen_"), false);
        } else if (action.startsWith("ArmCatRowChs_")) {
            handleGearCatRowChoose(parseIdx(action, "ArmCatRowChs_"), false);
        } else if (action.startsWith("ArmCatRowMov_")) {
            handleGearCatRowMove(parseIdx(action, "ArmCatRowMov_"), false);
        } else if (action.startsWith("ArmCatRowDel_")) {
            handleGearCatRowDelete(parseIdx(action, "ArmCatRowDel_"), false);
        } else if (action.equals("ArmCatNavBack")) {
            gearCatBack(false);
        } else if (action.equals("ArmCatNavForward")) {
            gearCatForward(false);
        } else if (action.equals("ArmCatAddCategory")) {
            addGearCategory(false);
        } else if (action.equals("ArmCatAddItem")) {
            addGearItem(false);
        } else if (action.equals("ArmCatDeleteFiltered")) {
            deleteFilteredGearItems(false);
        } else if (action.equals("ArmCatDeleteAll")) {
            deleteAllGearItems(false);
        } else if (action.equals("ArmCatPrevPage")) {
            if (armCatFilterPage > 0) armCatFilterPage--;
        } else if (action.equals("ArmCatNextPage")) {
            int maxPage = Math.max(0, (armCatTreeFilteredKeys.size() - 1) / AdminUIData.TREE_ROW_COUNT);
            if (armCatFilterPage < maxPage) armCatFilterPage++;
        } else {
            return false;
        }
        return true;
    }

    private boolean handleRarityTiersAction(String action) {
        if (action.startsWith("RarityAllowedToggle_")) {
            handleRarityAllowedToggle(action.substring("RarityAllowedToggle_".length()));
        } else if (action.equals("TwoHandedAdd")) {
            handleTwoHandedAdd();
        } else if (action.startsWith("TwoHandedDel_")) {
            handleTwoHandedDel(parseIdx(action, "TwoHandedDel_"));
        } else if (handlePaginationAction(action, "TwoHanded", twoHandedPage, editTwoHandedKeywords.size(), TWO_HANDED_PAGE_SIZE, p -> twoHandedPage = p)) {
        } else if (action.equals("WpnRarityAdd")) {
            handleRarityRuleAdd(true);
        } else if (action.startsWith("WpnRarityCycle_")) {
            handleRarityRuleCycle(parseIdx(action, "WpnRarityCycle_"), true);
        } else if (action.startsWith("WpnRarityDel_")) {
            handleRarityRuleDel(parseIdx(action, "WpnRarityDel_"), true);
        } else if (handlePaginationAction(action, "WpnRarity", weaponRarityPage, weaponRarityRuleKeys.size(), RARITY_RULES_PAGE_SIZE, p -> weaponRarityPage = p)) {
        } else if (action.equals("ArmRarityAdd")) {
            handleRarityRuleAdd(false);
        } else if (action.startsWith("ArmRarityCycle_")) {
            handleRarityRuleCycle(parseIdx(action, "ArmRarityCycle_"), false);
        } else if (action.startsWith("ArmRarityDel_")) {
            handleRarityRuleDel(parseIdx(action, "ArmRarityDel_"), false);
        } else if (handlePaginationAction(action, "ArmRarity", armorRarityPage, armorRarityRuleKeys.size(), RARITY_RULES_PAGE_SIZE, p -> armorRarityPage = p)) {
        } else {
            return false;
        }
        return true;
    }

    private boolean handleOverlayToggleAction(String action) {
        if (action.startsWith("ToggleNameplateTier_")) {
            int toggleIndex = parseIdx(action, "ToggleNameplateTier_");
            toggleOverlayBooleanArray(
                    () -> editOverlay.nameplateTierEnabled,
                    v -> editOverlay.nameplateTierEnabled = v,
                    toggleIndex, 5, true,
                    resolvedForSelected != null ? resolvedForSelected.nameplateTierEnabled : null);
        } else if (action.startsWith("DeleteFamily_")) {
            deleteFamilyRow(parseIdx(action, "DeleteFamily_"));
        } else if (action.startsWith("DeleteEnvRule_")) {
            deleteEnvRuleRow(parseIdx(action, "DeleteEnvRule_"));
        } else if (action.startsWith("TierOvrTier_")) {
            handleToggleTierOvrTier(action.substring("TierOvrTier_".length()));
        } else if (action.startsWith("TierOvrPeek_")) {
            handleTierOvrPeek(parseIdx(action, "TierOvrPeek_"));
        } else if (action.startsWith("TierOvrDel_")) {
            deleteTierOverrideRow(parseIdx(action, "TierOvrDel_"));
        } else {
            return false;
        }
        return true;
    }

    private boolean handlePickerAction(String action) {
        if (action.startsWith("ItemPickerRowClick_")) {
            handleItemPickerRowClick(parseIdx(action, "ItemPickerRowClick_"));
        } else if (action.startsWith("NpcPickerRowClick_")) {
            handleNpcPickerRowClick(parseIdx(action, "NpcPickerRowClick_"));
        } else if (action.startsWith("LinkPopupRowClick_")) {
            handleLinkPopupRowClick(parseIdx(action, "LinkPopupRowClick_"));
        } else if (action.startsWith("MovePopupRowClick_")) {
            handleMovePopupRowClick(parseIdx(action, "MovePopupRowClick_"));
        } else {
            return false;
        }
        return true;
    }

    private void handleSidebarAction(String action) {
        if (action.startsWith("ApplyPreset_")) {
            applyTemplate(action.substring("ApplyPreset_".length()));
            needsFieldRefresh = true;
        } else if (action.startsWith("NavWorld_")) {
            selectSidebarItem(Section.WORLD, worldNames, worldPage, parseIdx(action, "NavWorld_"));
        } else if (action.startsWith("NavInst_")) {
            selectSidebarItem(Section.INSTANCE, instanceNames, instPage, parseIdx(action, "NavInst_"));
        } else if (action.startsWith("EffectRowClick_")) {
            handleEffectRowClick(parseIdx(action, "EffectRowClick_"));
        } else if (action.startsWith("EffectRowToggle_")) {
            handleEffectRowToggle(parseIdx(action, "EffectRowToggle_"));
        } else if (action.startsWith("EffectDetailTier_")) {
            handleEffectDetailTier(parseIdx(action, "EffectDetailTier_"));
        }
    }

    private void selectSidebarItem(Section section, List<String> names, int page, int slotIdx) {
        int absIdx = page * SIDEBAR_PAGE_SIZE + slotIdx;
        if (absIdx < 0 || absIdx >= names.size()) return;

        stashCurrentOverlay();

        activeSection = section;
        selectedName = names.get(absIdx);
        activeSubTab = 0;

        String stashKey = overlayStashKey(section, selectedName);
        ConfigOverlay stashed = pendingOverlays.get(stashKey);
        if (stashed != null) {
            editOverlay = stashed;
            lastAppliedTemplateKey = pendingTemplateKeys.get(stashKey);

            ConfigResolver resolver = plugin.getConfigResolver();
            ResolvedConfig stashedRes = (activeSection == Section.WORLD)
                    ? resolver.getWorldResolvedConfig(selectedName)
                    : resolver.getInstanceResolvedConfig(selectedName);
            if (stashedRes == null) stashedRes = resolver.getBaseResolved();
            resetFamilyRowKeys(editOverlay, stashedRes);
            resetEnvRuleKeys(editOverlay, stashedRes);
            resetTierOverrides(editOverlay);

            ConfigOverlay raw = (activeSection == Section.WORLD)
                    ? resolver.getEffectiveWorldOverlay(selectedName)
                    : resolver.getInstanceOverlay(selectedName);
            savedOverlaySnapshot = deepCopyOverlay(raw != null ? raw : new ConfigOverlay());

            RPGMobsConfig config = plugin.getConfig();
            if (savedOverlaySnapshot.lootTemplateCategoryTree == null) {
                var baseLootTree = config != null && config.lootConfig.lootTemplateTree != null
                        ? config.lootConfig.lootTemplateTree
                        : new RPGMobsConfig.LootTemplateCategory("All", List.of());
                savedOverlaySnapshot.lootTemplateCategoryTree = deepCopyLootTemplateCategoryTree(baseLootTree);
            }

            resolvedForSelected = (activeSection == Section.WORLD)
                    ? resolver.getWorldResolvedConfig(selectedName)
                    : resolver.getInstanceResolvedConfig(selectedName);
            if (resolvedForSelected == null) resolvedForSelected = resolver.getBaseResolved();

            loadDisabledMobRuleKeysFromOverlay();

            resetLootTemplatesFromOverlay();
            snapshotLootTemplates();

            var abilFeatures = plugin.getFeatureRegistry().getAbilityFeatures();
            discoveredAbilityIds = abilFeatures.stream().map(IRPGMobsAbilityFeature::id).toList();
            abilityFeaturesById = abilFeatures.stream()
                    .collect(Collectors.toMap(IRPGMobsAbilityFeature::id, f -> f));
            abilityExpandedIndex = -1;
            abilityMobFilter = "";
            abilityMobPage = 0;
            abilityMobFiltered.clear();
        } else {
            loadOverlayForEditing();
        }
        needsFieldRefresh = true;
    }

    private void stashCurrentOverlay() {
        if (editOverlay != null && selectedName != null && (activeSection == Section.WORLD || activeSection == Section.INSTANCE)) {
            syncDisabledMobRuleKeysToOverlay();
            syncLootTemplatesToOverlay();
            String key = overlayStashKey(activeSection, selectedName);

            if (savedOverlaySnapshot != null) {
                ResolvedConfig base = plugin.getConfigResolver().getBaseResolved();
                if (ConfigOverlay.effectivelyEquals(editOverlay, savedOverlaySnapshot, base)) {
                    pendingOverlays.remove(key);
                    pendingTemplateKeys.remove(key);
                    return;
                }
            }

            pendingOverlays.put(key, editOverlay);
            if (lastAppliedTemplateKey != null) {
                pendingTemplateKeys.put(key, lastAppliedTemplateKey);
            } else {
                pendingTemplateKeys.remove(key);
            }
        }
    }

    private void syncLootTemplatesToOverlay() {
        if (editOverlay == null) return;
        if (!hasLootChanges()) return;
        editOverlay.lootTemplates = new LinkedHashMap<>();
        for (var e : editLootTemplates.entrySet()) {
            editOverlay.lootTemplates.put(e.getKey(), deepCopyLootTemplate(e.getValue()));
        }
    }

    private static void backfillMobRuleDefaults(Map<String, RPGMobsConfig.MobRule> rules) {
        Map<String, RPGMobsConfig.MobRule> javaDefaults = RPGMobsConfig.defaultMobRules();
        for (var entry : rules.entrySet()) {
            RPGMobsConfig.MobRule loaded = entry.getValue();
            RPGMobsConfig.MobRule javaDefault = javaDefaults.get(entry.getKey());
            if (javaDefault == null) continue;
            if (loaded.allowedWeaponCategories.isEmpty() && !javaDefault.allowedWeaponCategories.isEmpty()) {
                loaded.allowedWeaponCategories = new ArrayList<>(javaDefault.allowedWeaponCategories);
            }
            if (loaded.allowedArmorCategories.isEmpty() && !javaDefault.allowedArmorCategories.isEmpty()) {
                loaded.allowedArmorCategories = new ArrayList<>(javaDefault.allowedArmorCategories);
            }
            migrateGearCategoryKeys(loaded.allowedWeaponCategories);
            migrateGearCategoryKeys(loaded.allowedArmorCategories);
        }
    }

    private static void migrateGearCategoryKeys(List<String> entries) {
        if (entries == null || entries.isEmpty()) return;
        for (int i = 0; i < entries.size(); i++) {
            String e = entries.get(i);
            if (e != null && !e.startsWith("category:") && !e.contains("_")) {
                entries.set(i, "category:" + e);
            }
        }
    }

    private void resetLootTemplatesFromOverlay() {
        RPGMobsConfig config = plugin.getConfig();
        editLootTemplates.clear();
        if (editOverlay != null && editOverlay.lootTemplates != null) {
            for (var e : editOverlay.lootTemplates.entrySet()) {
                editLootTemplates.put(e.getKey(), deepCopyLootTemplate(e.getValue()));
            }
        } else if (config != null && config.lootConfig.lootTemplates != null) {
            for (var e : config.lootConfig.lootTemplates.entrySet()) {
                editLootTemplates.put(e.getKey(), deepCopyLootTemplate(e.getValue()));
            }
        }
        if (editOverlay != null && editOverlay.lootTemplateCategoryTree == null) {
            var baseLootTree = config != null && config.lootConfig.lootTemplateTree != null
                    ? config.lootConfig.lootTemplateTree
                    : new RPGMobsConfig.LootTemplateCategory("All", List.of());
            editOverlay.lootTemplateCategoryTree = deepCopyLootTemplateCategoryTree(baseLootTree);
            if (savedOverlaySnapshot != null && savedOverlaySnapshot.lootTemplateCategoryTree == null) {
                savedOverlaySnapshot.lootTemplateCategoryTree = deepCopyLootTemplateCategoryTree(baseLootTree);
            }
        }
        currentLootCategory = null;
        lootNavHistory.clear();
        lootForwardHistory.clear();
        lootTemplateExpandedIndex = -1;
        lootTemplateDropPage = 0;
    }

    private static String overlayStashKey(Section section, String name) {
        return (section == Section.WORLD ? "W:" : "I:") + name;
    }

    private void loadOverlayForEditing() {
        if (selectedName == null) { editOverlay = null; resolvedForSelected = null; savedOverlaySnapshot = null; return; }

        ConfigResolver resolver = plugin.getConfigResolver();
        ConfigOverlay raw = (activeSection == Section.WORLD)
                ? resolver.getEffectiveWorldOverlay(selectedName)
                : resolver.getInstanceOverlay(selectedName);

        editOverlay = deepCopyOverlay(raw != null ? raw : new ConfigOverlay());

        savedOverlaySnapshot = deepCopyOverlay(raw != null ? raw : new ConfigOverlay());
        lastAppliedTemplateKey = null;

        resolvedForSelected = (activeSection == Section.WORLD)
                ? resolver.getWorldResolvedConfig(selectedName)
                : resolver.getInstanceResolvedConfig(selectedName);

        if (resolvedForSelected == null) {
            resolvedForSelected = resolver.getBaseResolved();
        }

        if (raw == null && !resolvedForSelected.enabled) {
            editOverlay.enabled = false;
            savedOverlaySnapshot.enabled = false;
        }

        resetFamilyRowKeys(editOverlay, resolvedForSelected);

        resetEnvRuleKeys(editOverlay, resolvedForSelected);

        resetTierOverrides(editOverlay);

        loadDisabledMobRuleKeysFromOverlay();

        RPGMobsConfig config = plugin.getConfig();
        editLootTemplates.clear();
        if (editOverlay.lootTemplates != null) {
            for (var e : editOverlay.lootTemplates.entrySet()) {
                editLootTemplates.put(e.getKey(), deepCopyLootTemplate(e.getValue()));
            }
        } else if (config != null && config.lootConfig.lootTemplates != null) {
            for (var e : config.lootConfig.lootTemplates.entrySet()) {
                editLootTemplates.put(e.getKey(), deepCopyLootTemplate(e.getValue()));
            }
        }
        if (editOverlay.lootTemplateCategoryTree == null) {
            var baseLootTree = config != null && config.lootConfig.lootTemplateTree != null
                    ? config.lootConfig.lootTemplateTree
                    : new RPGMobsConfig.LootTemplateCategory("All", List.of());
            editOverlay.lootTemplateCategoryTree = deepCopyLootTemplateCategoryTree(baseLootTree);
            if (savedOverlaySnapshot != null && savedOverlaySnapshot.lootTemplateCategoryTree == null) {
                savedOverlaySnapshot.lootTemplateCategoryTree = deepCopyLootTemplateCategoryTree(baseLootTree);
            }
        }
        currentLootCategory = null;
        lootNavHistory.clear();
        lootForwardHistory.clear();
        lootTemplateExpandedIndex = -1;
        lootTemplateDropPage = 0;
        snapshotLootTemplates();

        var abilityFeatures = plugin.getFeatureRegistry().getAbilityFeatures();
        discoveredAbilityIds = abilityFeatures.stream().map(IRPGMobsAbilityFeature::id).toList();
        abilityFeaturesById = abilityFeatures.stream()
                .collect(Collectors.toMap(IRPGMobsAbilityFeature::id, feature -> feature));
        abilityExpandedIndex = -1;
        abilityMobFilter = "";
        abilityMobPage = 0;
        abilityMobFiltered.clear();
    }

    private void toggleOverlayBoolean(Supplier<@Nullable Boolean> getter,
                                      Consumer<@Nullable Boolean> setter,
                                      Function<ResolvedConfig, Boolean> resolvedFallback) {
        if (editOverlay == null) return;
        Boolean current = getter.get();

        boolean effectiveValue;
        if (current != null) {
            effectiveValue = current;
        } else {
            ResolvedConfig res = resolvedForSelected != null ? resolvedForSelected : plugin.getConfigResolver().getBaseResolved();
            effectiveValue = resolvedFallback.apply(res);
        }
        setter.accept(!effectiveValue);
        needsFieldRefresh = true;
    }

    private void toggleOverlayBooleanArray(Supplier<boolean @Nullable []> getter,
                                           Consumer<boolean[]> setter,
                                           int toggleIndex, int size, boolean defaultVal,
                                           boolean @Nullable [] resolvedFallback) {
        if (editOverlay == null || toggleIndex < 0 || toggleIndex >= size) return;
        boolean[] arr = getter.get();
        if (arr == null) {

            if (resolvedFallback != null && resolvedFallback.length >= size) {
                arr = Arrays.copyOf(resolvedFallback, size);
            } else {
                arr = new boolean[size];
                Arrays.fill(arr, defaultVal);
            }
        }
        arr[toggleIndex] = !arr[toggleIndex];
        setter.accept(arr);
        needsFieldRefresh = true;
    }

    private void setProgressionStyle(String style) {
        if (editOverlay == null) return;
        editOverlay.progressionStyle = style;
    }

    private void setNameplateMode(String mode) {
        if (editOverlay == null) return;
        editOverlay.nameplateMode = mode;
    }

    private void applyTemplate(String presetKey) {
        if (editOverlay == null) {
            templateAppliedMessage = "Select a world or instance first.";
            return;
        }

        if ("custom".equals(presetKey)) {
            if (editOverlay.customPreset == null) {
                templateAppliedMessage = "No world custom preset saved yet.";
                return;
            }
            ConfigOverlay restored = deepCopyOverlay(editOverlay.customPreset);
            restored.customPreset = editOverlay.customPreset;
            editOverlay = restored;
            lastAppliedTemplateKey = "custom";
            templateAppliedMessage = "Applied World Custom preset. Review and Save when ready.";
            resolvedForSelected = plugin.getConfigResolver().getBaseResolved();
            resetFamilyRowKeys(editOverlay, resolvedForSelected);
            resetEnvRuleKeys(editOverlay, resolvedForSelected);
            resetTierOverrides(editOverlay);

            resetLootTemplatesFromOverlay();
            needsFieldRefresh = true;
            return;
        }

        if ("customGlobal".equals(presetKey)) {
            if (globalCustomPreset == null) {
                templateAppliedMessage = "No global custom preset saved yet.";
                return;
            }
            ConfigOverlay restored = deepCopyOverlay(globalCustomPreset);

            restored.customPreset = editOverlay.customPreset;
            editOverlay = restored;
            lastAppliedTemplateKey = "customGlobal";
            templateAppliedMessage = "Applied Global Custom preset. Review and Save when ready.";
            resolvedForSelected = plugin.getConfigResolver().getBaseResolved();
            resetFamilyRowKeys(editOverlay, resolvedForSelected);
            resetEnvRuleKeys(editOverlay, resolvedForSelected);
            resetTierOverrides(editOverlay);

            resetLootTemplatesFromOverlay();
            needsFieldRefresh = true;
            return;
        }

        ConfigTemplate template = ConfigTemplate.get(presetKey);
        if (template == null) {
            templateAppliedMessage = "Unknown preset: " + presetKey;
            return;
        }

        ConfigOverlay templateOverlay = template.getOverlay();

        ConfigOverlay savedCustom = editOverlay.customPreset;
        editOverlay = deepCopyOverlay(templateOverlay);
        editOverlay.customPreset = savedCustom;
        lastAppliedTemplateKey = presetKey;
        templateAppliedMessage = "Applied \"" + template.getName() + "\" preset. Review and Save when ready.";

        resolvedForSelected = plugin.getConfigResolver().getBaseResolved();

        resetFamilyRowKeys(editOverlay, resolvedForSelected);
        resetEnvRuleKeys(editOverlay, resolvedForSelected);
        resetTierOverrides(editOverlay);
        resetLootTemplatesFromOverlay();
        needsFieldRefresh = true;
    }

    private void saveWorldCustomPreset() {
        if (editOverlay == null) {
            templateAppliedMessage = "Select a world or instance first.";
            return;
        }
        editOverlay.customPreset = deepCopyOverlay(editOverlay);
        editOverlay.customPreset.customPreset = null;
        templateAppliedMessage = "World custom preset saved.";
    }

    private void saveGlobalCustomPreset() {
        if (editOverlay == null) {
            templateAppliedMessage = "Select a world or instance first.";
            return;
        }
        ConfigOverlay snapshot = deepCopyOverlay(editOverlay);
        snapshot.customPreset = null;
        try {
            Path filePath = plugin.getModDirectory().resolve(GLOBAL_CUSTOM_PRESET_FILE);
            ConfigWriter.writeOverlay(snapshot, filePath);
            globalCustomPreset = snapshot;
            templateAppliedMessage = "Global custom preset saved.";
        } catch (Exception e) {
            templateAppliedMessage = "Failed to save global preset: " + e.getMessage();
            LOGGER.atWarning().withCause(e).log("Failed to save global custom preset");
        }
    }

    private void loadGlobalCustomPreset() {
        Path filePath = plugin.getModDirectory().resolve(GLOBAL_CUSTOM_PRESET_FILE);
        globalCustomPreset = plugin.getConfigResolver().loadOverlayFromPath(filePath);
    }

    private @Nullable String detectMatchingTemplate(ConfigOverlay edit) {
        ResolvedConfig base = plugin.getConfigResolver().getBaseResolved();
        for (Map.Entry<String, ConfigTemplate> entry : ConfigTemplate.getAll().entrySet()) {
            ConfigOverlay tmpl = entry.getValue().getOverlay();
            if (ConfigOverlay.effectivelyEquals(edit, tmpl, base)) {
                return entry.getKey();
            }
        }
        if (edit.customPreset != null && ConfigOverlay.effectivelyEquals(edit, edit.customPreset, base)) {
            return "custom";
        }
        if (globalCustomPreset != null && ConfigOverlay.effectivelyEquals(edit, globalCustomPreset, base)) {
            return "customGlobal";
        }
        return null;
    }

    private void resetFamilyRowKeys(@Nullable ConfigOverlay overlay, @Nullable ResolvedConfig res) {
        activeFamilyRowKeys.clear();

        Map<String, List<String>> merged = new LinkedHashMap<>();
        if (res != null && res.tierPrefixesByFamily != null) {
            merged.putAll(res.tierPrefixesByFamily);
        }
        if (overlay != null && overlay.tierPrefixesByFamily != null) {
            merged.putAll(overlay.tierPrefixesByFamily);
        }
        activeFamilyRowKeys.addAll(merged.keySet());
    }

    private void populateFamilyRows(UICommandBuilder c, ConfigOverlay overlay, ResolvedConfig res) {

        Map<String, List<String>> effectiveFamilies = new LinkedHashMap<>();
        if (res.tierPrefixesByFamily != null) effectiveFamilies.putAll(res.tierPrefixesByFamily);
        if (overlay.tierPrefixesByFamily != null) effectiveFamilies.putAll(overlay.tierPrefixesByFamily);

        int numRows = Math.min(activeFamilyRowKeys.size(), AdminUIData.MAX_FAMILIES);
        for (int i = 0; i < AdminUIData.MAX_FAMILIES; i++) {
            boolean vis = i < numRows;
            c.set("#FamilyRow" + i + ".Visible", vis);
            if (vis && needsFieldRefresh) {
                String key = activeFamilyRowKeys.get(i);
                List<String> tiers = key.isBlank() ? Collections.emptyList()
                        : effectiveFamilies.getOrDefault(key, Collections.emptyList());
                c.set("#FieldFamilyKey" + i + ".Value", key);
                for (int t = 0; t < 5; t++) {
                    c.set("#FamilyT" + (t + 1) + "r" + i + ".Value", t < tiers.size() ? tiers.get(t) : "");
                }
            }
        }
        boolean full = numRows == AdminUIData.MAX_FAMILIES;
        c.set("#FamiliesFullMsg.Visible", full);
    }

    private void processGlobalNumberFields(AdminUIData data) {
        if (data.debugScanInterval != null) {
            String s = sanitizeNumericInput(data.debugScanInterval);
            try { int v = (int) Double.parseDouble(s); if (v >= 1 && v <= 600) editDebugScanInterval = v; }
            catch (NumberFormatException ignored) {}
        }
    }

    private void processOverlayNumberFields(AdminUIData data) {
        if (editOverlay == null) return;

        double[] resolvedSpawnChance  = resolvedForSelected != null ? resolvedForSelected.spawnChancePerTier          : null;
        float[]  resolvedHealth       = resolvedForSelected != null ? resolvedForSelected.healthMultiplierPerTier      : null;
        float[]  resolvedDamage       = resolvedForSelected != null ? resolvedForSelected.damageMultiplierPerTier      : null;
        int[]    resolvedExtraRolls   = resolvedForSelected != null ? resolvedForSelected.vanillaDroplistExtraRollsPerTier : null;
        float[]  resolvedModelScale   = resolvedForSelected != null ? resolvedForSelected.modelScalePerTier            : null;

        String[] sc = {data.spawnChance0, data.spawnChance1, data.spawnChance2, data.spawnChance3, data.spawnChance4};
        editOverlay.spawnChancePerTier = parseDoubleArray(sc, editOverlay.spawnChancePerTier, resolvedSpawnChance);

        editOverlay.distancePerTier = parseDoubleField(data.distPerTier, editOverlay.distancePerTier);
        editOverlay.distanceBonusInterval = parseDoubleField(data.distBonusInterval, editOverlay.distanceBonusInterval);
        editOverlay.distanceHealthBonusPerInterval = parseFloatField(data.distHealthBonus, editOverlay.distanceHealthBonusPerInterval);
        editOverlay.distanceDamageBonusPerInterval = parseFloatField(data.distDamageBonus, editOverlay.distanceDamageBonusPerInterval);
        editOverlay.distanceHealthBonusCap = parseFloatField(data.distHealthCap, editOverlay.distanceHealthBonusCap);
        editOverlay.distanceDamageBonusCap = parseFloatField(data.distDamageCap, editOverlay.distanceDamageBonusCap);

        String[] hm = {data.health0, data.health1, data.health2, data.health3, data.health4};
        editOverlay.healthMultiplierPerTier = parseFloatArray(hm, editOverlay.healthMultiplierPerTier, resolvedHealth);
        String[] dm = {data.damage0, data.damage1, data.damage2, data.damage3, data.damage4};
        editOverlay.damageMultiplierPerTier = parseFloatArray(dm, editOverlay.damageMultiplierPerTier, resolvedDamage);
        editOverlay.healthRandomVariance = parseFloatField(data.healthVariance, editOverlay.healthRandomVariance);
        editOverlay.damageRandomVariance = parseFloatField(data.damageVariance, editOverlay.damageRandomVariance);

        String[] er = {data.extraRolls0, data.extraRolls1, data.extraRolls2, data.extraRolls3, data.extraRolls4};
        editOverlay.vanillaDroplistExtraRollsPerTier = parseIntArray(er, editOverlay.vanillaDroplistExtraRollsPerTier, resolvedExtraRolls);
        editOverlay.dropWeaponChance = parseDoubleField(data.dropWeapon, editOverlay.dropWeaponChance);
        editOverlay.dropArmorPieceChance = parseDoubleField(data.dropArmor, editOverlay.dropArmorPieceChance);
        editOverlay.dropOffhandItemChance = parseDoubleField(data.dropOffhand, editOverlay.dropOffhandItemChance);
        editOverlay.droppedGearDurabilityMin = parseDoubleField(data.lootDurMin, editOverlay.droppedGearDurabilityMin);
        editOverlay.droppedGearDurabilityMax = parseDoubleField(data.lootDurMax, editOverlay.droppedGearDurabilityMax);

        String[] ms = {data.modelScale0, data.modelScale1, data.modelScale2, data.modelScale3, data.modelScale4};
        editOverlay.modelScalePerTier = parseFloatArray(ms, editOverlay.modelScalePerTier, resolvedModelScale);
        editOverlay.modelScaleVariance = parseFloatField(data.modelVariance, editOverlay.modelScaleVariance);

        editOverlay.globalCooldownMinSeconds = parseFloatField(data.globalCdMin, editOverlay.globalCooldownMinSeconds);
        editOverlay.globalCooldownMaxSeconds = parseFloatField(data.globalCdMax, editOverlay.globalCooldownMaxSeconds);

        processNameplatePrefixFields(data);

        processFamilyRows(data);

        processEnvRuleRows(data);

        processOverlayXPFields(data);

        processTierRestrictionFilter(data);

        processEntityEffectFields(data);

        processAbilCfgFields(data);
    }

    private void processEntityEffectFields(AdminUIData data) {
        if (effectExpandedIndex < 0 || effectExpandedIndex >= effectTreeFiltered.size()) return;
        String key = effectTreeFiltered.get(effectExpandedIndex);
        RPGMobsConfig.EntityEffectConfig eff = editEntityEffects.get(key);
        if (eff == null) return;
        String[] mults = data.effectDetailMult;
        for (int t = 0; t < 5; t++) {
            if (mults[t] != null && !mults[t].isBlank()) {
                try {
                    eff.amountMultiplierPerTier[t] = Float.parseFloat(mults[t].trim());
                } catch (NumberFormatException ignored) {}
            }
        }
    }

    private void processNameplatePrefixFields(AdminUIData data) {
        if (editOverlay == null) return;
        String[] inputs = {data.nameplatePrefix0, data.nameplatePrefix1, data.nameplatePrefix2,
                           data.nameplatePrefix3, data.nameplatePrefix4};
        boolean anyNonNull = false;
        for (String s : inputs) if (s != null) { anyNonNull = true; break; }
        if (!anyNonNull) return;
        String[] base = editOverlay.nameplatePrefixPerTier != null
                ? editOverlay.nameplatePrefixPerTier
                : (resolvedForSelected != null
                   ? resolvedForSelected.nameplatePrefixPerTier
                   : new String[]{"[•]","[• •]","[• • •]","[• • • •]","[• • • • •]"});
        String[] arr = Arrays.copyOf(base, 5);
        boolean changed = false;
        for (int i = 0; i < 5; i++) {
            if (inputs[i] != null && !inputs[i].equals(arr[i])) { arr[i] = inputs[i]; changed = true; }
        }
        if (changed) editOverlay.nameplatePrefixPerTier = arr;
    }

    private void processFamilyRows(AdminUIData data) {
        if (editOverlay == null) return;

        boolean anyNonNull = false;
        for (int i = 0; i < AdminUIData.MAX_FAMILIES; i++) {
            if (data.familyKeys[i] != null || data.familyT1[i] != null
                    || data.familyT2[i] != null || data.familyT3[i] != null
                    || data.familyT4[i] != null || data.familyT5[i] != null) {
                anyNonNull = true; break;
            }
        }
        if (!anyNonNull) return;

        Map<String, List<String>> effectiveFamilies = new LinkedHashMap<>();
        if (resolvedForSelected != null && resolvedForSelected.tierPrefixesByFamily != null) {
            effectiveFamilies.putAll(resolvedForSelected.tierPrefixesByFamily);
        }
        if (editOverlay.tierPrefixesByFamily != null) {
            effectiveFamilies.putAll(editOverlay.tierPrefixesByFamily);
        }

        if (activeFamilyRowKeys.isEmpty()) return;
        Map<String, List<String>> updated = new LinkedHashMap<>();
        for (int i = 0; i < activeFamilyRowKeys.size() && i < AdminUIData.MAX_FAMILIES; i++) {
            String oldKey = activeFamilyRowKeys.get(i);

            String newKey = (data.familyKeys[i] != null) ? data.familyKeys[i].trim() : oldKey;

            if (newKey == null || newKey.isBlank()) continue;

            List<String> existing = effectiveFamilies.getOrDefault(oldKey, new ArrayList<>(Arrays.asList("","","","","")));
            if (existing.size() < 5) while (existing.size() < 5) existing.add("");

            List<String> tiers = new ArrayList<>(existing);
            if (data.familyT1[i] != null) tiers.set(0, data.familyT1[i]);
            if (data.familyT2[i] != null) tiers.set(1, data.familyT2[i]);
            if (data.familyT3[i] != null) tiers.set(2, data.familyT3[i]);
            if (data.familyT4[i] != null) tiers.set(3, data.familyT4[i]);
            if (data.familyT5[i] != null) tiers.set(4, data.familyT5[i]);

            updated.put(newKey, tiers);

            if (!newKey.equals(oldKey)) activeFamilyRowKeys.set(i, newKey);
        }

        updated.entrySet().removeIf(e -> e.getValue().stream().allMatch(s -> s == null || s.isBlank()));

        if (resolvedForSelected != null && resolvedForSelected.tierPrefixesByFamily != null) {
            Map<String, List<String>> base = resolvedForSelected.tierPrefixesByFamily;
            updated.entrySet().removeIf(e -> e.getValue().equals(base.get(e.getKey())));
        }
        editOverlay.tierPrefixesByFamily = updated.isEmpty() ? null : updated;
    }

    private void addFamilyRow() {
        if (editOverlay == null) return;
        if (activeFamilyRowKeys.size() >= AdminUIData.MAX_FAMILIES) return;

        String placeholder = "";
        activeFamilyRowKeys.add(placeholder);
        needsFieldRefresh = true;
    }

    private void deleteFamilyRow(int rowIdx) {
        if (editOverlay == null || rowIdx < 0 || rowIdx >= activeFamilyRowKeys.size()) return;
        String keyToRemove = activeFamilyRowKeys.get(rowIdx);
        activeFamilyRowKeys.remove(rowIdx);

        if (editOverlay.tierPrefixesByFamily != null && !keyToRemove.isBlank()) {
            editOverlay.tierPrefixesByFamily.remove(keyToRemove);
            if (editOverlay.tierPrefixesByFamily.isEmpty()) editOverlay.tierPrefixesByFamily = null;
        }
        needsFieldRefresh = true;
    }

    private void handleSaveReload() {
        try {

            GlobalConfig gc = plugin.getGlobalConfig();
            if (gc != null) {
                gc.globalEnabled = editGlobalEnabled;
                gc.enabledByDefault = editEnabledByDefault;
                gc.isDebugModeEnabled = editDebugMode;
                gc.debugMobRuleScanIntervalSeconds = editDebugScanInterval;
            }
            plugin.writeGlobalConfig();

            syncDisabledMobRuleKeysToOverlay();

            for (RPGMobsConfig.LootTemplate template : editLootTemplates.values()) {
                template.drops.removeIf(d -> d.itemId == null || d.itemId.isBlank());
            }
            syncLootTemplatesToOverlay();

            if (editOverlay != null && selectedName != null) {
                ConfigOverlay saved = savedOverlaySnapshot != null ? savedOverlaySnapshot : new ConfigOverlay();
                if (hasOverlayChanges(editOverlay, saved)) {
                    Path modDir = plugin.getModDirectory();
                    String folder = (activeSection == Section.WORLD) ? "worlds" : "instances";
                    Path overlayFile = modDir.resolve(folder).resolve(selectedName + ".yml");
                    ConfigWriter.writeOverlay(editOverlay, overlayFile);
                }
            }

            if (!pendingOverlays.isEmpty()) {
                Path modDir = plugin.getModDirectory();
                for (Map.Entry<String, ConfigOverlay> entry : pendingOverlays.entrySet()) {
                    String key = entry.getKey();
                    boolean isWorld = key.startsWith("W:");
                    String name = key.substring(2);

                    if (name.equals(selectedName) && ((isWorld && activeSection == Section.WORLD) || (!isWorld && activeSection == Section.INSTANCE))) continue;
                    String folder = isWorld ? "worlds" : "instances";
                    Path overlayFile = modDir.resolve(folder).resolve(name + ".yml");
                    ConfigWriter.writeOverlay(entry.getValue(), overlayFile);
                }
            }

            RPGMobsConfig saveCfg = plugin.getConfig();
            if (saveCfg != null) {
                applyGlobalDataSources(saveCfg);
                Path baseDir = plugin.getModDirectory().resolve("base");
                if (!Files.isDirectory(baseDir)) baseDir = plugin.getModDirectory();
                YamlSerializer.writeOnly(baseDir, saveCfg);
            }

            boolean assetReloadNeeded = hasCombatAIChanges() || hasGlobalMobRuleChanges();
            if (assetReloadNeeded) {
                plugin.reloadConfigAndAssets();
                cachedNpcIds = null;
                allDroppableItems = null;
            } else {
                plugin.reloadConfigOnly();
            }

            RPGMobsConfig reloadedCfg = plugin.getConfig();
            if (reloadedCfg != null) {
                applyGlobalDataSources(reloadedCfg);
                reloadedCfg.populateSummonMarkerEntriesIfEmpty();
                reloadedCfg.upgradeSummonMarkerEntriesToVariantIds();
            }

            snapshotGlobalConfig();
            snapshotAllData();
            refreshSidebarLists();

            String savedMobCatName = currentMobRuleCategory != null ? currentMobRuleCategory.name : null;
            int savedMobExpanded = mobRuleTreeExpandedIndex;
            List<String> savedMobNavNames = mobRuleNavHistory.stream().map(c -> c.name).toList();
            List<String> savedMobFwdNames = mobRuleForwardHistory.stream().map(c -> c.name).toList();
            String savedPerWorldMobCatName = perWorldCurrentCategory != null ? perWorldCurrentCategory.name : null;
            String savedLootCatName = currentLootCategory != null ? currentLootCategory.name : null;
            int savedLootExpanded = lootTemplateExpandedIndex;
            int savedLootDropPage = lootTemplateDropPage;
            List<String> savedLootNavNames = lootNavHistory.stream().map(c -> c.name).toList();
            List<String> savedLootFwdNames = lootForwardHistory.stream().map(c -> c.name).toList();
            int savedAbilExpanded = abilityExpandedIndex;
            String savedAbilMobFilter = abilityMobFilter;
            int savedAbilMobPage = abilityMobPage;
            int savedEffectExpanded = effectExpandedIndex;

            if (selectedName != null) loadOverlayForEditing();

            if (savedMobCatName != null) {
                var found = findMobRuleCategoryByName(getMobRuleCategoryRoot(), savedMobCatName);
                if (found != null) {
                    currentMobRuleCategory = found;
                    mobRuleNavHistory.clear();
                    for (String name : savedMobNavNames) {
                        var cat = findMobRuleCategoryByName(getMobRuleCategoryRoot(), name);
                        if (cat != null) mobRuleNavHistory.addLast(cat);
                    }
                    mobRuleForwardHistory.clear();
                    for (String name : savedMobFwdNames) {
                        var cat = findMobRuleCategoryByName(getMobRuleCategoryRoot(), name);
                        if (cat != null) mobRuleForwardHistory.addLast(cat);
                    }
                    mobRuleTreeExpandedIndex = savedMobExpanded;
                }
            }

            if (savedPerWorldMobCatName != null && editMobRuleCategoryTree != null) {
                var found = findMobRuleCategoryByName(editMobRuleCategoryTree, savedPerWorldMobCatName);
                if (found != null && found != editMobRuleCategoryTree) {
                    perWorldCurrentCategory = found;
                }
            }

            if (savedLootCatName != null) {
                var found = findLootCategoryByName(getLootCategoryRoot(), savedLootCatName);
                if (found != null) {
                    currentLootCategory = found;
                    lootNavHistory.clear();
                    for (String name : savedLootNavNames) {
                        var cat = findLootCategoryByName(getLootCategoryRoot(), name);
                        if (cat != null) lootNavHistory.addLast(cat);
                    }
                    lootForwardHistory.clear();
                    for (String name : savedLootFwdNames) {
                        var cat = findLootCategoryByName(getLootCategoryRoot(), name);
                        if (cat != null) lootForwardHistory.addLast(cat);
                    }
                    lootTemplateExpandedIndex = savedLootExpanded;
                    lootTemplateDropPage = savedLootDropPage;
                }
            }

            abilityExpandedIndex = savedAbilExpanded;
            abilityMobFilter = savedAbilMobFilter;
            abilityMobPage = savedAbilMobPage;
            rebuildAbilTreeFiltered();
            rebuildAbilMobFiltered();

            effectExpandedIndex = savedEffectExpanded;

            wpnCatTreeFilter = "";
            wpnCatFilterPage = 0;
            wpnCatTreeFilteredKeys.clear();
            armCatTreeFilter = "";
            armCatFilterPage = 0;
            armCatTreeFilteredKeys.clear();

            pendingOverlays.clear();
            pendingTemplateKeys.clear();
            needsFieldRefresh = true;
            saveMessage = assetReloadNeeded ? "Saved & assets reloaded successfully!" : "Saved & reloaded successfully!";
            saveMessageSuccess = true;
        } catch (Throwable t) {
            LOGGER.atWarning().withCause(t).log("Failed to save RPGMobs config from admin UI");
            saveMessage = "Save failed: " + t.getMessage();
            saveMessageSuccess = false;
        }
    }

    private void handleDiscard() {
        plugin.reloadConfigOnly();
        snapshotGlobalConfig();
        snapshotAllData();
        pendingOverlays.clear();
        pendingTemplateKeys.clear();

        int savedAbilExpanded = abilityExpandedIndex;
        String savedAbilMobFilter = abilityMobFilter;
        int savedAbilMobPage = abilityMobPage;
        int savedEffectExpanded = effectExpandedIndex;

        if (selectedName != null) loadOverlayForEditing();

        abilityExpandedIndex = savedAbilExpanded;
        abilityMobFilter = savedAbilMobFilter;
        abilityMobPage = savedAbilMobPage;
        rebuildAbilTreeFiltered();
        rebuildAbilMobFiltered();

        effectExpandedIndex = savedEffectExpanded;

        needsFieldRefresh = true;
        saveMessage = "Changes discarded.";
        saveMessageSuccess = true;
    }

    private UICommandBuilder buildUpdate() {
        UICommandBuilder c = new UICommandBuilder();
        populateCommands(c);
        return c;
    }

    private void populateCommands(UICommandBuilder c) {
        boolean isConfig = activeSection == Section.GLOBAL_CONFIG;
        boolean isOverlay = activeSection == Section.WORLD || activeSection == Section.INSTANCE;

        renderSidebar(c, isConfig);
        renderSubTabs(c, isConfig, isOverlay);
        renderGlobalPanels(c);
        renderTabContent(c, isConfig, isOverlay);
        renderPopups(c);

        if (isOverlay && editOverlay != null) {
            renderOverlayContent(c);
        }

        renderStatusBar(c);

        needsFieldRefresh = false;
    }

    private void renderSidebar(UICommandBuilder c, boolean isConfig) {
        setSidebarNav4State(c, "#NavGlobalCore", activeSection == Section.GLOBAL_CORE, hasGlobalCoreChanges());
        setSidebarNav4State(c, "#NavGlobalDebug", activeSection == Section.GLOBAL_DEBUG, hasGlobalDebugChanges());
        setSidebarNav4State(c, "#NavGlobalMobRules", activeSection == Section.GLOBAL_MOB_RULES, hasGlobalMobRuleChanges());
        setSidebarNav4State(c, "#NavGlobalCombatAI", activeSection == Section.GLOBAL_COMBAT_AI, hasCombatAIChanges());
        setSidebarNav4State(c, "#NavGlobalConfig", isConfig, hasGlobalConfigChanges());

        fillSidebarSlots(c, worldNames, worldPage, "#NavWorld", Section.WORLD);
        fillSidebarSlots(c, instanceNames, instPage, "#NavInst", Section.INSTANCE);
        setPagination(c, worldNames.size(), worldPage, "#WorldPagination", "#WorldPageLabel",
                "#FirstWorlds", "#PrevWorlds", "#NextWorlds", "#LastWorlds");
        setPagination(c, instanceNames.size(), instPage, "#InstPagination", "#InstPageLabel",
                "#FirstInsts", "#PrevInsts", "#NextInsts", "#LastInsts");
    }

    private void renderSubTabs(UICommandBuilder c, boolean isConfig, boolean isOverlay) {
        c.set("#TabGlobalCore.Visible", activeSection == Section.GLOBAL_CORE);
        c.set("#TabGlobalDebug.Visible", activeSection == Section.GLOBAL_DEBUG);

        c.set("#TabConfigWeapons.Visible", isConfig && activeSubTab == TAB_CONFIG_WEAPONS);
        c.set("#TabConfigArmor.Visible", isConfig && activeSubTab == TAB_CONFIG_ARMOR);
        c.set("#TabConfigRarityTiers.Visible", isConfig && activeSubTab == TAB_CONFIG_RARITY);
        c.set("#TabConfigCombatAI.Visible", activeSection == Section.GLOBAL_COMBAT_AI);
        c.set("#TabWorldGeneral.Visible", isOverlay && activeSubTab == TAB_GENERAL);
        c.set("#TabWorldMobRules.Visible", activeSection == Section.GLOBAL_MOB_RULES || (isOverlay && activeSubTab == TAB_MOB_RULES));
        c.set("#TabWorldStats.Visible", isOverlay && activeSubTab == TAB_STATS);
        c.set("#TabWorldLoot.Visible", isOverlay && activeSubTab == TAB_LOOT);
        c.set("#TabWorldSpawning.Visible", isOverlay && activeSubTab == TAB_SPAWNING);
        c.set("#TabWorldEntityEffects.Visible", isOverlay && activeSubTab == TAB_ENTITY_EFFECTS);
        c.set("#TabWorldAbilities.Visible", isOverlay && activeSubTab == TAB_ABILITIES);
        c.set("#TabWorldVisuals.Visible", isOverlay && activeSubTab == TAB_VISUALS);

        boolean isCombatAI = activeSection == Section.GLOBAL_COMBAT_AI;
        c.set("#SubTabBar.Visible", !isCombatAI);
        c.set("#CaiTabBar.Visible", isCombatAI);
        if (isCombatAI) {
            for (int i = 0; i < 3; i++) {
                c.set("#CaiTab" + i + ".Visible", i != caiSubTab);
                c.set("#CaiTab" + i + "Active.Visible", i == caiSubTab);
            }
        }

        String[] subTabLabels;
        boolean showSubTabs;
        if (isConfig) {
            subTabLabels = new String[]{"Weapons", "Armor", "Rarity & Tiers"};
            showSubTabs = true;
        } else if (isOverlay) {
            subTabLabels = new String[]{"General", "Mob Rules", "Stats", "Loot", "Spawning", "Entity Effects", "Abilities", "Visuals"};
            showSubTabs = true;
        } else {
            subTabLabels = new String[0];
            showSubTabs = false;
        }
        boolean[] tabHasChanges = isOverlay && editOverlay != null ? computeTabChanges(editOverlay) : new boolean[MAX_SUB_TABS];
        for (int i = 0; i < MAX_SUB_TABS; i++) {
            boolean visible = showSubTabs && i < subTabLabels.length;
            c.set("#SubTabGroup" + i + ".Visible", visible);
            if (visible) {
                String label = subTabLabels[i];
                boolean active = (i == activeSubTab);
                boolean changed = isOverlay && tabHasChanges[i];
                c.set("#SubTab" + i + ".Text", label);
                c.set("#SubTab" + i + "Active.Text", label);
                c.set("#SubTab" + i + "Changed.Text", label);
                c.set("#SubTab" + i + "ChangedActive.Text", label);
                c.set("#SubTab" + i + ".Visible", !active && !changed);
                c.set("#SubTab" + i + "Active.Visible", active && !changed);
                c.set("#SubTab" + i + "Changed.Visible", !active && changed);
                c.set("#SubTab" + i + "ChangedActive.Visible", active && changed);
            }
        }

        String title;
        if (isOverlay && selectedName != null) {
            title = (activeSection == Section.WORLD ? "World" : "Instance") + ": " + selectedName;
        } else {
            title = switch (activeSection) {
                case GLOBAL_CORE -> "Core Settings";
                case GLOBAL_DEBUG -> "Debug Settings";
                case GLOBAL_MOB_RULES -> "Mob Rules";
                case GLOBAL_COMBAT_AI -> "Combat AI Config";
                case GLOBAL_CONFIG -> "Equipment Config";
                default -> "RPGMobs Config";
            };
        }
        c.set("#ContentTitle.Text", title);
        c.set("#DirtyIndicator.Visible", computeIsDirty());
    }

    private void renderGlobalPanels(UICommandBuilder c) {
        renderToggle(c, "#GlobalEnabled", editGlobalEnabled);
        renderToggle(c, "#EnabledByDefault", editEnabledByDefault);
        renderToggle(c, "#DebugMode", editDebugMode);

        if (needsFieldRefresh) {
            c.set("#FieldDebugScanInterval.Value", String.valueOf(editDebugScanInterval));
        }

    }

    private void renderTabContent(UICommandBuilder c, boolean isConfig, boolean isOverlay) {
        if (isConfig && activeSubTab == TAB_CONFIG_WEAPONS) {
            populateGearCatTree(c, true);
        }
        if (isConfig && activeSubTab == TAB_CONFIG_ARMOR) {
            populateGearCatTree(c, false);
        }
        if (isConfig && activeSubTab == TAB_CONFIG_RARITY) {
            renderRarityTiersTab(c);
        }
        if (activeSection == Section.GLOBAL_COMBAT_AI) {
            renderCombatAITab(c);
        }

        if (activeSection == Section.GLOBAL_MOB_RULES) {
            populateMobRuleTree(c);
        }
        if (isOverlay && activeSubTab == TAB_MOB_RULES) {
            populatePerWorldMobRuleList(c);
        }
        if (isOverlay && activeSubTab == TAB_LOOT) {
            populateLootTree(c);
        }
        if (isOverlay && activeSubTab == TAB_ENTITY_EFFECTS) {
            populateEntityEffects(c);
        }
        if (isOverlay && activeSubTab == TAB_ABILITIES) {
            var res = resolvedForSelected != null ? resolvedForSelected : plugin.getConfigResolver().getBaseResolved();
            c.set("#FieldGlobalCdMin.Value", fmtFloat(editOverlay.globalCooldownMinSeconds != null ? editOverlay.globalCooldownMinSeconds : res.globalCooldownMinSeconds));
            c.set("#FieldGlobalCdMax.Value", fmtFloat(editOverlay.globalCooldownMaxSeconds != null ? editOverlay.globalCooldownMaxSeconds : res.globalCooldownMaxSeconds));
            populateAbilityOverrides(c);
        }
        if (isOverlay && activeSubTab == TAB_SPAWNING) {
            populateTierRestrictionRows(c, editOverlay);
        }
    }

    private void renderPopups(UICommandBuilder c) {
        c.set("#CategoryLinkPopup.Visible", linkPopupOpen);
        if (linkPopupOpen) {
            populateLinkPopup(c);
        }

        c.set("#RenamePopup.Visible", renamePopupOpen);
        if (renamePopupOpen && needsFieldRefresh) {
            c.set("#RenamePopupField.Value", pendingRenameName != null ? pendingRenameName : "");
            String popupTitle = "Rename";
            String confirmText = "Rename";
            if (renameTarget != null) {
                if (renameTarget == RenameTarget.WEAPON_CATEGORY && renameRowIndex == RENAME_IDX_TWO_HANDED_ADD) {
                    popupTitle = "Add Two-Handed Keyword";
                    confirmText = "Add Keyword";
                } else if (renameTarget == RenameTarget.WEAPON_CATEGORY && renameRowIndex == RENAME_IDX_RARITY_RULE_ADD) {
                    popupTitle = "Add Weapon Rarity Rule";
                    confirmText = "Add Rule";
                } else if (renameTarget == RenameTarget.WEAPON_CATEGORY && renameRowIndex == RENAME_IDX_SUMMON_ROLE_ADD) {
                    popupTitle = "Add Role Identifier";
                    confirmText = "Add";
                } else if (renameTarget == RenameTarget.ARMOR_CATEGORY && renameRowIndex == RENAME_IDX_RARITY_RULE_ADD) {
                    popupTitle = "Add Armor Rarity Rule";
                    confirmText = "Add Rule";
                } else {
                    switch (renameTarget) {
                        case MOB_RULE_CATEGORY -> popupTitle = "Rename Category";
                        case MOB_RULE_ITEM -> popupTitle = "Rename Mob Rule";
                        case LOOT_CATEGORY -> popupTitle = "Rename Category";
                        case LOOT_ITEM -> popupTitle = "Rename Loot Template";
                        case WEAPON_CATEGORY -> popupTitle = "Rename Weapon Category";
                        case ARMOR_CATEGORY -> popupTitle = "Rename Armor Category";
                    }
                }
            }
            c.set("#RenamePopupTitle.Text", popupTitle);
            c.set("#RenamePopupConfirm.Text", confirmText);
        }

        c.set("#MovePopup.Visible", movePopupOpen);
        if (movePopupOpen) {
            populateMovePopup(c);
        }

        c.set("#ItemPickerPopup.Visible", itemPickerOpen);
        if (itemPickerOpen) {
            populateItemPicker(c);
        }

        c.set("#NpcPickerPopup.Visible", npcPickerOpen);
        if (npcPickerOpen) {
            populateNpcPicker(c);
        }

        renderCategoryPeekPopup(c);
    }

    private void renderOverlayContent(UICommandBuilder c) {
        ResolvedConfig res = resolvedForSelected != null ? resolvedForSelected : plugin.getConfigResolver().getBaseResolved();

        boolean overlayEnabled = editOverlay.enabled != null ? editOverlay.enabled : res.enabled;
        renderToggle(c, "#OverlayEnabled", overlayEnabled);

        boolean rpgLevelingOn = editOverlay.rpgLevelingEnabled != null ? editOverlay.rpgLevelingEnabled : res.rpgLevelingEnabled;
        renderToggle(c, "#RPGLevelingOverlay", rpgLevelingOn);

        String ps = editOverlay.progressionStyle != null ? editOverlay.progressionStyle : res.progressionStyle.name();
        boolean isEnv = "ENVIRONMENT".equalsIgnoreCase(ps);
        boolean isDist = "DISTANCE_FROM_SPAWN".equalsIgnoreCase(ps);
        boolean isNone = "NONE".equalsIgnoreCase(ps);
        c.set("#ProgStyleEnv.Visible", !isEnv);
        c.set("#ProgStyleEnvActive.Visible", isEnv);
        c.set("#ProgStyleDist.Visible", !isDist);
        c.set("#ProgStyleDistActive.Visible", isDist);
        c.set("#ProgStyleNone.Visible", !isNone);
        c.set("#ProgStyleNoneActive.Visible", isNone);

        String progDesc = isEnv ? "Tier based on the biome/zone the mob spawns in."
                : isDist ? "Tier increases the further from spawn. Distance fields below control scaling."
                : "All mobs use flat spawn chances with no progression scaling.";
        c.set("#ProgStyleDescription.Text", progDesc);

        c.set("#SpawnChanceGroup.Visible", isNone);

        c.set("#DistanceFieldsGroup.Visible", isDist);

        c.set("#EnvRulesGroup.Visible", isEnv);

        boolean fdDisabled = editOverlay.eliteFallDamageDisabled != null ? editOverlay.eliteFallDamageDisabled : res.eliteFallDamageDisabled;
        renderToggle(c, "#FallDamage", fdDisabled);

        if (needsFieldRefresh) {

            double[] sc = editOverlay.spawnChancePerTier != null ? editOverlay.spawnChancePerTier : res.spawnChancePerTier;
            for (int i = 0; i < 5; i++) c.set("#FieldSpawnChance" + i + ".Value", fmtDouble(sc[i]));

            c.set("#FieldDistancePerTier.Value", fmtDouble(editOverlay.distancePerTier != null ? editOverlay.distancePerTier : res.distancePerTier));
            c.set("#FieldDistanceBonusInterval.Value", fmtDouble(editOverlay.distanceBonusInterval != null ? editOverlay.distanceBonusInterval : res.distanceBonusInterval));
            c.set("#FieldDistHealthBonus.Value", fmtFloat(editOverlay.distanceHealthBonusPerInterval != null ? editOverlay.distanceHealthBonusPerInterval : res.distanceHealthBonusPerInterval));
            c.set("#FieldDistDamageBonus.Value", fmtFloat(editOverlay.distanceDamageBonusPerInterval != null ? editOverlay.distanceDamageBonusPerInterval : res.distanceDamageBonusPerInterval));
            c.set("#FieldDistHealthCap.Value", fmtFloat(editOverlay.distanceHealthBonusCap != null ? editOverlay.distanceHealthBonusCap : res.distanceHealthBonusCap));
            c.set("#FieldDistDamageCap.Value", fmtFloat(editOverlay.distanceDamageBonusCap != null ? editOverlay.distanceDamageBonusCap : res.distanceDamageBonusCap));

            boolean healthScalingOn = editOverlay.enableHealthScaling != null ? editOverlay.enableHealthScaling : res.enableHealthScaling;
            renderToggle(c, "#HealthScaling", healthScalingOn);
            boolean damageScalingOn = editOverlay.enableDamageScaling != null ? editOverlay.enableDamageScaling : res.enableDamageScaling;
            renderToggle(c, "#DamageScaling", damageScalingOn);

            float[] hm = editOverlay.healthMultiplierPerTier != null ? editOverlay.healthMultiplierPerTier : res.healthMultiplierPerTier;
            for (int i = 0; i < 5; i++) c.set("#FieldHealth" + i + ".Value", fmtFloat(hm[i]));
            float[] dmg = editOverlay.damageMultiplierPerTier != null ? editOverlay.damageMultiplierPerTier : res.damageMultiplierPerTier;
            for (int i = 0; i < 5; i++) c.set("#FieldDamage" + i + ".Value", fmtFloat(dmg[i]));
            c.set("#FieldHealthVariance.Value", fmtFloat(editOverlay.healthRandomVariance != null ? editOverlay.healthRandomVariance : res.healthRandomVariance));
            c.set("#FieldDamageVariance.Value", fmtFloat(editOverlay.damageRandomVariance != null ? editOverlay.damageRandomVariance : res.damageRandomVariance));

            int[] er = editOverlay.vanillaDroplistExtraRollsPerTier != null ? editOverlay.vanillaDroplistExtraRollsPerTier : res.vanillaDroplistExtraRollsPerTier;
            for (int i = 0; i < 5; i++) c.set("#FieldExtraRolls" + i + ".Value", String.valueOf(er[i]));
            c.set("#FieldDropWeapon.Value", fmtDouble(editOverlay.dropWeaponChance != null ? editOverlay.dropWeaponChance : res.dropWeaponChance));
            c.set("#FieldDropArmor.Value", fmtDouble(editOverlay.dropArmorPieceChance != null ? editOverlay.dropArmorPieceChance : res.dropArmorPieceChance));
            c.set("#FieldDropOffhand.Value", fmtDouble(editOverlay.dropOffhandItemChance != null ? editOverlay.dropOffhandItemChance : res.dropOffhandItemChance));
            c.set("#FieldLootDurMin.Value", fmtDouble(editOverlay.droppedGearDurabilityMin != null ? editOverlay.droppedGearDurabilityMin : res.droppedGearDurabilityMin));
            c.set("#FieldLootDurMax.Value", fmtDouble(editOverlay.droppedGearDurabilityMax != null ? editOverlay.droppedGearDurabilityMax : res.droppedGearDurabilityMax));

            String[] prefixes = editOverlay.nameplatePrefixPerTier != null ? editOverlay.nameplatePrefixPerTier : res.nameplatePrefixPerTier;
            for (int i = 0; i < 5; i++) c.set("#FieldNameplatePrefix" + i + ".Value", prefixes[i]);

            float[] ms = editOverlay.modelScalePerTier != null ? editOverlay.modelScalePerTier : res.modelScalePerTier;
            for (int i = 0; i < 5; i++) c.set("#FieldModelScale" + i + ".Value", fmtFloat(ms[i]));
            c.set("#FieldModelVariance.Value", fmtFloat(editOverlay.modelScaleVariance != null ? editOverlay.modelScaleVariance : res.modelScaleVariance));

            populateFamilyRows(c, editOverlay, res);

            populateEnvRuleRows(c, editOverlay, res);

            float[] xpMult = editOverlay.xpMultiplierPerTier != null ? editOverlay.xpMultiplierPerTier : res.xpMultiplierPerTier;
            for (int i = 0; i < 5; i++) c.set("#FieldOverlayXPMult" + i + ".Value", fmtFloat(xpMult[i]));
            c.set("#FieldOverlayXPBonusPerAbility.Value", fmtDouble(editOverlay.xpBonusPerAbility != null ? editOverlay.xpBonusPerAbility : res.xpBonusPerAbility));
            c.set("#FieldOverlayMinionXPMult.Value", fmtDouble(editOverlay.minionXPMultiplier != null ? editOverlay.minionXPMultiplier : res.minionXPMultiplier));

        }

        boolean npOn = editOverlay.enableNameplates != null ? editOverlay.enableNameplates : res.enableNameplates;
        renderToggle(c, "#EnableNameplates", npOn);
        boolean msOn = editOverlay.enableModelScaling != null ? editOverlay.enableModelScaling : res.enableModelScaling;
        renderToggle(c, "#EnableModelScaling", msOn);

        c.set("#NameplateModeRanked.Visible", false);
        c.set("#NameplateModeRankedActive.Visible", true);

        boolean[] nt = editOverlay.nameplateTierEnabled != null ? editOverlay.nameplateTierEnabled : res.nameplateTierEnabled;
        for (int i = 0; i < 5; i++) {
            c.set("#NameplateTierOn" + i + ".Visible", nt[i]);
            c.set("#NameplateTierOff" + i + ".Visible", !nt[i]);
        }

        updateFieldDiffMarkers(c, editOverlay, res);

        updateModifiedFieldMarkers(c, editOverlay, res);

        c.set("#PresetAppliedMsg.Visible", templateAppliedMessage != null);
        if (templateAppliedMessage != null) {
            c.set("#PresetAppliedMsg.Text", templateAppliedMessage);
        }

        String matchingPreset = detectMatchingTemplate(editOverlay);
        c.set("#PresetFull.Visible", !"full".equals(matchingPreset));
        c.set("#PresetFullActive.Visible", "full".equals(matchingPreset));
        c.set("#PresetEmpty.Visible", !"empty".equals(matchingPreset));
        c.set("#PresetEmptyActive.Visible", "empty".equals(matchingPreset));

        boolean hasWorldCustom = editOverlay.customPreset != null;
        c.set("#PresetCustom.Visible", hasWorldCustom && !"custom".equals(matchingPreset));
        c.set("#PresetCustomActive.Visible", hasWorldCustom && "custom".equals(matchingPreset));
        c.set("#PresetCustomOff.Visible", !hasWorldCustom);

        boolean hasGlobalCustom = globalCustomPreset != null;
        c.set("#PresetGlobal.Visible", hasGlobalCustom && !"customGlobal".equals(matchingPreset));
        c.set("#PresetGlobalActive.Visible", hasGlobalCustom && "customGlobal".equals(matchingPreset));
        c.set("#PresetGlobalOff.Visible", !hasGlobalCustom);
    }

    private void renderStatusBar(UICommandBuilder c) {
        c.set("#SaveMessage.Visible", saveMessage != null);
        if (saveMessage != null) {
            c.set("#SaveMessage.Text", (saveMessageSuccess ? "" : "ERROR: ") + saveMessage);
        }
        c.set("#SaveReloadButton.Text", hasCombatAIChanges() ? "Save & Reload Assets" : "Save");
    }

    private static void setSidebarNav4State(UICommandBuilder c, String base, boolean active, boolean changed) {
        c.set(base + ".Visible", !active && !changed);
        c.set(base + "Active.Visible", active && !changed);
        c.set(base + "Changed.Visible", !active && changed);
        c.set(base + "ChangedActive.Visible", active && changed);
    }

    private void fillSidebarSlots(UICommandBuilder c, List<String> names, int page, String prefix, Section sectionType) {
        int start = page * SIDEBAR_PAGE_SIZE;
        ConfigResolver resolver = plugin.getConfigResolver();
        for (int i = 0; i < SIDEBAR_PAGE_SIZE; i++) {
            int itemIndex = start + i;
            boolean vis = itemIndex < names.size();
            c.set(prefix + "Group" + i + ".Visible", vis);
            if (vis) {
                String name = names.get(itemIndex);
                boolean active = activeSection == sectionType && name.equals(selectedName);
                String stashKey = overlayStashKey(sectionType, name);

                boolean enabled;
                if (active && editOverlay != null && editOverlay.enabled != null) {
                    enabled = editOverlay.enabled;
                } else {
                    ConfigOverlay stashed = pendingOverlays.get(stashKey);
                    if (stashed != null && stashed.enabled != null) {
                        enabled = stashed.enabled;
                    } else {
                        ResolvedConfig res = (sectionType == Section.WORLD)
                                ? resolver.getWorldResolvedConfig(name)
                                : resolver.getInstanceResolvedConfig(name);
                        enabled = res != null ? res.enabled : (plugin.getGlobalConfig() != null && plugin.getGlobalConfig().enabledByDefault);
                    }
                }
                String displayText = (enabled ? "[X]  " : "[ ]  ") + name;

                boolean changed;
                if (active && editOverlay != null) {

                    boolean[] tabChanges = computeTabChanges(editOverlay);
                    changed = false;
                    for (boolean ch : tabChanges) if (ch) { changed = true; break; }
                } else {

                    ConfigOverlay stashed = pendingOverlays.get(stashKey);
                    if (stashed != null) {
                        ConfigOverlay raw = (sectionType == Section.WORLD) ? resolver.getEffectiveWorldOverlay(name) : resolver.getInstanceOverlay(name);
                        changed = hasOverlayChanges(stashed, raw != null ? raw : new ConfigOverlay());
                    } else {
                        changed = false;
                    }
                }

                c.set(prefix + i + ".Text", displayText);
                c.set(prefix + i + "Active.Text", displayText);
                c.set(prefix + i + "Changed.Text", displayText);
                c.set(prefix + i + "ChangedActive.Text", displayText);

                c.set(prefix + i + ".Visible", !active && !changed);
                c.set(prefix + i + "Active.Visible", active && !changed);
                c.set(prefix + i + "Changed.Visible", !active && changed);
                c.set(prefix + i + "ChangedActive.Visible", active && changed);
            }
        }
    }

    private void setPagination(UICommandBuilder c, int totalItems, int page, String groupId, String labelId,
                               String firstBtnId, String prevBtnId, String nextBtnId, String lastBtnId) {
        setPagination(c, totalItems, page, SIDEBAR_PAGE_SIZE, groupId, labelId, firstBtnId, prevBtnId, nextBtnId, lastBtnId);
    }

    private void setPagination(UICommandBuilder c, int totalItems, int page, int pageSize, String groupId, String labelId,
                               String firstBtnId, String prevBtnId, String nextBtnId, String lastBtnId) {
        int totalPages = Math.max(1, (int) Math.ceil(totalItems / (double) pageSize));
        c.set(groupId + ".Visible", totalPages > 1);
        if (totalPages > 1) {
            c.set(labelId + ".Text", (page + 1) + "/" + totalPages);
            c.set(firstBtnId + ".Visible", page > 0);
            c.set(prevBtnId + ".Visible", page > 0);
            c.set(nextBtnId + ".Visible", page < totalPages - 1);
            c.set(lastBtnId + ".Visible", page < totalPages - 1);
        }
    }

    private static String fmtDouble(double v) {
        if (v == (long) v) return String.valueOf((long) v);

        String s = String.format("%.6g", v);

        if (s.contains(".") && !s.contains("e") && !s.contains("E")) {
            s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        }
        return s;
    }

    private static String fmtFloat(float v) {
        if (v == (long) v) return String.valueOf((long) v);

        String s = String.format("%.5g", v);
        if (s.contains(".") && !s.contains("e") && !s.contains("E")) {
            s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        }
        return s;
    }
    private void renderToggle(UICommandBuilder c, String prefix, boolean value) {
        c.set(prefix + "On.Visible", value);
        c.set(prefix + "Off.Visible", !value);
    }

    private boolean updateFilter(@Nullable String newValue, String currentValue,
                                 Consumer<String> setter, Runnable onChanged) {
        if (newValue == null) return false;
        String trimmed = newValue.trim();
        if (!trimmed.equals(currentValue)) {
            setter.accept(trimmed);
            onChanged.run();
            return true;
        }
        return false;
    }

    private boolean handlePaginationAction(String action, String prefix, int currentPage,
                                           int totalItems, int pageSize, IntConsumer setPage) {
        if (action.equals(prefix + "FirstPage")) { setPage.accept(0); return true; }
        if (action.equals(prefix + "PrevPage")) { setPage.accept(Math.max(0, currentPage - 1)); return true; }
        if (action.equals(prefix + "NextPage")) {
            int maxPage = Math.max(0, (totalItems - 1) / pageSize);
            setPage.accept(Math.min(currentPage + 1, maxPage));
            return true;
        }
        if (action.equals(prefix + "LastPage")) {
            setPage.accept(Math.max(0, (totalItems - 1) / pageSize));
            return true;
        }
        return false;
    }

    private static int parseIdx(String action, String prefix) {
        try { return Integer.parseInt(action.substring(prefix.length())); } catch (NumberFormatException e) { return -1; }
    }

    private static String sanitizeNumericInput(String input) {
        if (input == null) return null;
        String s = input.trim().replace(',', '.');
        return s.isEmpty() ? "0" : s;
    }

    private static double @Nullable [] parseDoubleArray(String[] inputs, double @Nullable [] current, double @Nullable [] resolvedFallback) {
        boolean anyNonNull = false;
        for (String s : inputs) if (s != null) { anyNonNull = true; break; }
        if (!anyNonNull) return current;

        double[] base;
        if (current != null) {
            base = Arrays.copyOf(current, inputs.length);
        } else if (resolvedFallback != null) {
            base = Arrays.copyOf(resolvedFallback, inputs.length);
        } else {
            base = new double[inputs.length];
        }
        double[] arr = Arrays.copyOf(base, inputs.length);
        boolean changed = false;
        for (int i = 0; i < inputs.length; i++) {
            if (inputs[i] != null) {
                String s = sanitizeNumericInput(inputs[i]);
                try { double v = Double.parseDouble(s); if (v != arr[i]) { arr[i] = v; changed = true; } }
                catch (NumberFormatException ignored) {}
            }
        }
        return changed ? arr : current;
    }

    private static float @Nullable [] parseFloatArray(String[] inputs, float @Nullable [] current, float @Nullable [] resolvedFallback) {
        boolean anyNonNull = false;
        for (String s : inputs) if (s != null) { anyNonNull = true; break; }
        if (!anyNonNull) return current;

        float[] base;
        if (current != null) {
            base = Arrays.copyOf(current, inputs.length);
        } else if (resolvedFallback != null) {
            base = Arrays.copyOf(resolvedFallback, inputs.length);
        } else {
            base = new float[inputs.length];
        }
        float[] arr = Arrays.copyOf(base, inputs.length);
        boolean changed = false;
        for (int i = 0; i < inputs.length; i++) {
            if (inputs[i] != null) {
                String s = sanitizeNumericInput(inputs[i]);
                try { float v = Float.parseFloat(s); if (v != arr[i]) { arr[i] = v; changed = true; } }
                catch (NumberFormatException ignored) {}
            }
        }
        return changed ? arr : current;
    }

    private static int @Nullable [] parseIntArray(String[] inputs, int @Nullable [] current, int @Nullable [] resolvedFallback) {
        boolean anyNonNull = false;
        for (String s : inputs) if (s != null) { anyNonNull = true; break; }
        if (!anyNonNull) return current;

        int[] base;
        if (current != null) {
            base = Arrays.copyOf(current, inputs.length);
        } else if (resolvedFallback != null) {
            base = Arrays.copyOf(resolvedFallback, inputs.length);
        } else {
            base = new int[inputs.length];
        }
        int[] arr = Arrays.copyOf(base, inputs.length);
        boolean changed = false;
        for (int i = 0; i < inputs.length; i++) {
            if (inputs[i] != null) {
                String s = sanitizeNumericInput(inputs[i]);
                try { int v = (int) Double.parseDouble(s); if (v != arr[i]) { arr[i] = v; changed = true; } }
                catch (NumberFormatException ignored) {}
            }
        }
        return changed ? arr : current;
    }

    private static @Nullable Double parseDoubleField(@Nullable String input, @Nullable Double current) {
        if (input == null) return current;
        String s = sanitizeNumericInput(input);
        try {
            double v = Double.parseDouble(s);
            if (current != null && v == current) return current;
            return v;
        } catch (NumberFormatException ignored) {}
        return current;
    }

    private static float parseFloatPrimitive(@Nullable String input, float current) {
        if (input == null || input.isBlank()) return current;
        String s = sanitizeNumericInput(input);
        try { return Float.parseFloat(s); }
        catch (NumberFormatException ignored) {}
        return current;
    }

    private static @Nullable Float parseFloatField(@Nullable String input, @Nullable Float current) {
        if (input == null) return current;
        String s = sanitizeNumericInput(input);
        try {
            float v = Float.parseFloat(s);
            if (current != null && v == current) return current;
            return v;
        } catch (NumberFormatException ignored) {}
        return current;
    }

    private boolean[] computeTabChanges(ConfigOverlay current) {
        boolean[] changed = new boolean[8];
        ConfigOverlay saved = savedOverlaySnapshot != null ? savedOverlaySnapshot : new ConfigOverlay();
        ResolvedConfig b = resolvedForSelected != null ? plugin.getConfigResolver().getBaseResolved() : new ResolvedConfig();

        changed[TAB_GENERAL] = !Objects.equals(effectiveValue(current.enabled, b.enabled), effectiveValue(saved.enabled, b.enabled))
                || !Objects.equals(effectiveValue(current.rpgLevelingEnabled, b.rpgLevelingEnabled), effectiveValue(saved.rpgLevelingEnabled, b.rpgLevelingEnabled))
                || !Arrays.equals(effF(current.xpMultiplierPerTier, b.xpMultiplierPerTier), effF(saved.xpMultiplierPerTier, b.xpMultiplierPerTier))
                || !Objects.equals(effectiveValue(current.xpBonusPerAbility, b.xpBonusPerAbility), effectiveValue(saved.xpBonusPerAbility, b.xpBonusPerAbility))
                || !Objects.equals(effectiveValue(current.minionXPMultiplier, b.minionXPMultiplier), effectiveValue(saved.minionXPMultiplier, b.minionXPMultiplier))
                || !Objects.equals(effectiveValue(current.eliteFallDamageDisabled, b.eliteFallDamageDisabled), effectiveValue(saved.eliteFallDamageDisabled, b.eliteFallDamageDisabled));

        changed[TAB_MOB_RULES] = hasDisabledMobRuleChanges();

        changed[TAB_STATS] = !Objects.equals(effectiveValue(current.enableHealthScaling, b.enableHealthScaling), effectiveValue(saved.enableHealthScaling, b.enableHealthScaling))
                || !Objects.equals(effectiveValue(current.enableDamageScaling, b.enableDamageScaling), effectiveValue(saved.enableDamageScaling, b.enableDamageScaling))
                || !Arrays.equals(effF(current.healthMultiplierPerTier, b.healthMultiplierPerTier), effF(saved.healthMultiplierPerTier, b.healthMultiplierPerTier))
                || !Arrays.equals(effF(current.damageMultiplierPerTier, b.damageMultiplierPerTier), effF(saved.damageMultiplierPerTier, b.damageMultiplierPerTier))
                || !Objects.equals(effectiveValue(current.healthRandomVariance, b.healthRandomVariance), effectiveValue(saved.healthRandomVariance, b.healthRandomVariance))
                || !Objects.equals(effectiveValue(current.damageRandomVariance, b.damageRandomVariance), effectiveValue(saved.damageRandomVariance, b.damageRandomVariance));

        changed[TAB_LOOT] = !Arrays.equals(effI(current.vanillaDroplistExtraRollsPerTier, b.vanillaDroplistExtraRollsPerTier), effI(saved.vanillaDroplistExtraRollsPerTier, b.vanillaDroplistExtraRollsPerTier))
                || !Objects.equals(effectiveValue(current.dropWeaponChance, b.dropWeaponChance), effectiveValue(saved.dropWeaponChance, b.dropWeaponChance))
                || !Objects.equals(effectiveValue(current.dropArmorPieceChance, b.dropArmorPieceChance), effectiveValue(saved.dropArmorPieceChance, b.dropArmorPieceChance))
                || !Objects.equals(effectiveValue(current.dropOffhandItemChance, b.dropOffhandItemChance), effectiveValue(saved.dropOffhandItemChance, b.dropOffhandItemChance))
                || !Objects.equals(effectiveValue(current.droppedGearDurabilityMin, b.droppedGearDurabilityMin), effectiveValue(saved.droppedGearDurabilityMin, b.droppedGearDurabilityMin))
                || !Objects.equals(effectiveValue(current.droppedGearDurabilityMax, b.droppedGearDurabilityMax), effectiveValue(saved.droppedGearDurabilityMax, b.droppedGearDurabilityMax))
                || hasLootChanges()
                || !Objects.equals(current.lootTemplateCategoryTree, saved.lootTemplateCategoryTree);

        changed[TAB_SPAWNING] = !Objects.equals(effectiveValue(current.progressionStyle, b.progressionStyle.name()), effectiveValue(saved.progressionStyle, b.progressionStyle.name()))
                || !Arrays.equals(effD(current.spawnChancePerTier, b.spawnChancePerTier), effD(saved.spawnChancePerTier, b.spawnChancePerTier))
                || !Objects.equals(effectiveValue(current.distancePerTier, b.distancePerTier), effectiveValue(saved.distancePerTier, b.distancePerTier))
                || !Objects.equals(effectiveValue(current.distanceBonusInterval, b.distanceBonusInterval), effectiveValue(saved.distanceBonusInterval, b.distanceBonusInterval))
                || !Objects.equals(effectiveValue(current.distanceHealthBonusPerInterval, b.distanceHealthBonusPerInterval), effectiveValue(saved.distanceHealthBonusPerInterval, b.distanceHealthBonusPerInterval))
                || !Objects.equals(effectiveValue(current.distanceDamageBonusPerInterval, b.distanceDamageBonusPerInterval), effectiveValue(saved.distanceDamageBonusPerInterval, b.distanceDamageBonusPerInterval))
                || !Objects.equals(effectiveValue(current.distanceHealthBonusCap, b.distanceHealthBonusCap), effectiveValue(saved.distanceHealthBonusCap, b.distanceHealthBonusCap))
                || !Objects.equals(effectiveValue(current.distanceDamageBonusCap, b.distanceDamageBonusCap), effectiveValue(saved.distanceDamageBonusCap, b.distanceDamageBonusCap))
                || !envRuleMapsEqual(current.environmentTierRules, saved.environmentTierRules)
                || !Objects.equals(current.tierOverrides, saved.tierOverrides);

        changed[TAB_ENTITY_EFFECTS] = hasEntityEffectChanges();

        changed[TAB_ABILITIES] = !Objects.equals(current.abilityOverlays, saved.abilityOverlays)
                || hasAbilityConfigChanges()
                || !Objects.equals(effectiveValue(current.globalCooldownMinSeconds, b.globalCooldownMinSeconds), effectiveValue(saved.globalCooldownMinSeconds, b.globalCooldownMinSeconds))
                || !Objects.equals(effectiveValue(current.globalCooldownMaxSeconds, b.globalCooldownMaxSeconds), effectiveValue(saved.globalCooldownMaxSeconds, b.globalCooldownMaxSeconds));

        changed[TAB_VISUALS] = !Objects.equals(effectiveValue(current.enableNameplates, b.enableNameplates), effectiveValue(saved.enableNameplates, b.enableNameplates))
                || !Objects.equals(effectiveValue(current.nameplateMode, b.nameplateMode), effectiveValue(saved.nameplateMode, b.nameplateMode))
                || !Arrays.equals(effB(current.nameplateTierEnabled, b.nameplateTierEnabled), effB(saved.nameplateTierEnabled, b.nameplateTierEnabled))
                || !Arrays.equals(effS(current.nameplatePrefixPerTier, b.nameplatePrefixPerTier), effS(saved.nameplatePrefixPerTier, b.nameplatePrefixPerTier))
                || !Objects.equals(current.tierPrefixesByFamily, saved.tierPrefixesByFamily)
                || !Objects.equals(effectiveValue(current.enableModelScaling, b.enableModelScaling), effectiveValue(saved.enableModelScaling, b.enableModelScaling))
                || !Arrays.equals(effF(current.modelScalePerTier, b.modelScalePerTier), effF(saved.modelScalePerTier, b.modelScalePerTier))
                || !Objects.equals(effectiveValue(current.modelScaleVariance, b.modelScaleVariance), effectiveValue(saved.modelScaleVariance, b.modelScaleVariance));
        return changed;
    }

    private boolean hasGlobalCoreChanges() {
        return editGlobalEnabled != savedGlobalEnabled
                || editEnabledByDefault != savedEnabledByDefault;
    }


    private boolean hasGlobalDebugChanges() {
        return editDebugMode != savedDebugMode
                || editDebugScanInterval != savedDebugScanInterval;
    }

    private boolean hasGlobalConfigChanges() {
        return hasWeaponCategoryChanges() || hasArmorCategoryChanges() || hasRarityTiersChanges();
    }

    private boolean hasRarityTiersChanges() {
        if (!Arrays.equals(editArmorPiecesPerTier, savedArmorPiecesPerTier)) return true;
        if (!Arrays.equals(editShieldChancePerTier, savedShieldChancePerTier)) return true;
        if (!Arrays.deepEquals(editTierAllowedRarities, savedTierAllowedRarities)) return true;
        if (!Arrays.deepEquals(editTierRarityWeights, savedTierRarityWeights)) return true;
        if (!editTwoHandedKeywords.equals(savedTwoHandedKeywords)) return true;
        if (!editWeaponRarityRules.equals(savedWeaponRarityRules)) return true;
        return !editArmorRarityRules.equals(savedArmorRarityRules);
    }

    private boolean hasWeaponCategoryChanges() {
        return !editWeaponCategoryTree.equals(savedWeaponCategoryTree);
    }

    private boolean hasArmorCategoryChanges() {
        return !editArmorCategoryTree.equals(savedArmorCategoryTree);
    }

    private boolean hasLootChanges() {
        if (editLootTemplates.size() != savedLootTemplateSnapshot.size()) return true;
        for (var e : editLootTemplates.entrySet()) {
            String savedFingerprint = savedLootTemplateSnapshot.get(e.getKey());
            if (savedFingerprint == null) return true;
            if (!savedFingerprint.equals(lootTemplateFingerprint(e.getValue()))) return true;
        }
        return false;
    }

    private boolean hasEntityEffectChanges() {
        if (editEntityEffects.size() != savedEntityEffects.size()) return true;
        for (var e : editEntityEffects.entrySet()) {
            RPGMobsConfig.EntityEffectConfig saved = savedEntityEffects.get(e.getKey());
            if (saved == null) return true;
            RPGMobsConfig.EntityEffectConfig edit = e.getValue();
            if (edit.isEnabled != saved.isEnabled) return true;
            if (edit.infinite != saved.infinite) return true;
            if (!Arrays.equals(edit.isEnabledPerTier, saved.isEnabledPerTier)) return true;
            if (!Arrays.equals(edit.amountMultiplierPerTier, saved.amountMultiplierPerTier)) return true;
        }
        return false;
    }

    private boolean hasAbilityConfigChanges() {
        if (editAbilityConfigs.size() != savedAbilityConfigs.size()) return true;
        for (var entry : editAbilityConfigs.entrySet()) {
            RPGMobsConfig.AbilityConfig savedConfig = savedAbilityConfigs.get(entry.getKey());
            if (savedConfig == null) return true;
            RPGMobsConfig.AbilityConfig editConfig = entry.getValue();
            if (!Arrays.equals(editConfig.chancePerTier, savedConfig.chancePerTier)) return true;
            if (!Arrays.equals(editConfig.cooldownSecondsPerTier, savedConfig.cooldownSecondsPerTier)) return true;
            if (!Objects.equals(editConfig.excludeLinkedMobRuleKeys, savedConfig.excludeLinkedMobRuleKeys)) return true;
            if (!abilityGatesEqual(editConfig.gate, savedConfig.gate)) return true;
            var feature = abilityFeaturesById.get(entry.getKey());
            if (feature != null) {
                for (var field : feature.describeConfigFields()) {
                    if (field.hasChanged(editConfig, savedConfig)) return true;
                }
            }
            if (editConfig instanceof RPGMobsConfig.MultiSlashAbilityConfig editMs
                    && savedConfig instanceof RPGMobsConfig.MultiSlashAbilityConfig savedMs) {
                if (!multiSlashVariantConfigsEqual(editMs.variantConfigs, savedMs.variantConfigs)) return true;
            }
        }
        return false;
    }

    private static boolean multiSlashVariantConfigsEqual(
            Map<String, RPGMobsConfig.MultiSlashVariantConfig> a,
            Map<String, RPGMobsConfig.MultiSlashVariantConfig> b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        if (a.size() != b.size()) return false;
        for (var entry : a.entrySet()) {
            var vc2 = b.get(entry.getKey());
            if (vc2 == null) return false;
            var vc1 = entry.getValue();
            if (!Arrays.equals(vc1.slashTriggerChancePerTier, vc2.slashTriggerChancePerTier)) return false;
            if (!Arrays.equals(vc1.cooldownSecondsPerTier, vc2.cooldownSecondsPerTier)) return false;
            if (!Arrays.equals(vc1.baseDamagePerHitPerTier, vc2.baseDamagePerHitPerTier)) return false;
            if (!Arrays.equals(vc1.forwardDriftForcePerTier, vc2.forwardDriftForcePerTier)) return false;
            if (!Arrays.equals(vc1.knockbackForcePerTier, vc2.knockbackForcePerTier)) return false;
            if (Float.compare(vc1.meleeRange, vc2.meleeRange) != 0) return false;
        }
        return true;
    }

    private static boolean abilityGatesEqual(RPGMobsConfig.AbilityGate a, RPGMobsConfig.AbilityGate b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        return Objects.equals(a.allowedWeaponCategories, b.allowedWeaponCategories);
    }

    private boolean computeIsDirty() {
        if (hasGlobalCoreChanges()) return true;

        for (var ds : globalDataSources) {
            if (ds.hasChanges()) return true;
        }
        if (hasLootChanges()) return true;

        if (editOverlay != null) {
            boolean[] tabChanges = computeTabChanges(editOverlay);
            for (boolean ch : tabChanges) if (ch) return true;
        }

        if (!pendingOverlays.isEmpty()) {
            ConfigResolver resolver = plugin.getConfigResolver();
            for (Map.Entry<String, ConfigOverlay> entry : pendingOverlays.entrySet()) {
                String key = entry.getKey();
                ConfigOverlay pending = entry.getValue();
                boolean isWorld = key.startsWith("W:");
                String name = key.substring(2);
                ConfigOverlay raw = isWorld ? resolver.getEffectiveWorldOverlay(name) : resolver.getInstanceOverlay(name);
                ConfigOverlay saved = raw != null ? raw : new ConfigOverlay();
                if (hasOverlayChanges(pending, saved)) return true;
            }
        }
        return false;
    }

    private boolean hasOverlayChanges(ConfigOverlay current, ConfigOverlay saved) {
        ResolvedConfig base = plugin.getConfigResolver().getBaseResolved();
        return !ConfigOverlay.effectivelyEquals(current, saved, base);
    }

    private void updateFieldDiffMarkers(UICommandBuilder c, ConfigOverlay edit, ResolvedConfig res) {
        if (lastAppliedTemplateKey == null) {
            setAllDiffMarkers(c, false);
            return;
        }
        ConfigTemplate template = ConfigTemplate.get(lastAppliedTemplateKey);
        if (template == null) {
            setAllDiffMarkers(c, false);
            return;
        }
        ConfigOverlay tmpl = template.getOverlay();

        ResolvedConfig b = plugin.getConfigResolver().getBaseResolved();

        c.set("#ChangedEnabled.Visible", !Objects.equals(effectiveValue(edit.enabled, res.enabled), effectiveValue(tmpl.enabled, b.enabled)));

        c.set("#ChangedRPGLevelingOverlay.Visible", !Objects.equals(effectiveValue(edit.rpgLevelingEnabled, res.rpgLevelingEnabled), effectiveValue(tmpl.rpgLevelingEnabled, b.rpgLevelingEnabled)));
        c.set("#ChangedOverlayXPMult.Visible", !Arrays.equals(effF(edit.xpMultiplierPerTier, res.xpMultiplierPerTier), effF(tmpl.xpMultiplierPerTier, b.xpMultiplierPerTier)));
        c.set("#ChangedOverlayXPBonus.Visible", !Objects.equals(effectiveValue(edit.xpBonusPerAbility, res.xpBonusPerAbility), effectiveValue(tmpl.xpBonusPerAbility, b.xpBonusPerAbility)));
        c.set("#ChangedOverlayMinionXP.Visible", !Objects.equals(effectiveValue(edit.minionXPMultiplier, res.minionXPMultiplier), effectiveValue(tmpl.minionXPMultiplier, b.minionXPMultiplier)));

        c.set("#ChangedProgStyle.Visible", !Objects.equals(effectiveValue(edit.progressionStyle, res.progressionStyle.name()), effectiveValue(tmpl.progressionStyle, b.progressionStyle.name())));

        c.set("#ChangedSpawnChance.Visible", !Arrays.equals(effD(edit.spawnChancePerTier, res.spawnChancePerTier), effD(tmpl.spawnChancePerTier, b.spawnChancePerTier)));

        c.set("#ChangedHealthScaling.Visible", !Objects.equals(effectiveValue(edit.enableHealthScaling, res.enableHealthScaling), effectiveValue(tmpl.enableHealthScaling, b.enableHealthScaling)));
        c.set("#ChangedDamageScaling.Visible", !Objects.equals(effectiveValue(edit.enableDamageScaling, res.enableDamageScaling), effectiveValue(tmpl.enableDamageScaling, b.enableDamageScaling)));

        c.set("#ChangedHealth.Visible", !Arrays.equals(effF(edit.healthMultiplierPerTier, res.healthMultiplierPerTier), effF(tmpl.healthMultiplierPerTier, b.healthMultiplierPerTier)));

        c.set("#ChangedDamage.Visible", !Arrays.equals(effF(edit.damageMultiplierPerTier, res.damageMultiplierPerTier), effF(tmpl.damageMultiplierPerTier, b.damageMultiplierPerTier)));

        c.set("#ChangedAbilEnabled.Visible", !Objects.equals(edit.abilityOverlays, tmpl.abilityOverlays));
        c.set("#ChangedGlobalCdMin.Visible", !Objects.equals(effectiveValue(edit.globalCooldownMinSeconds, res.globalCooldownMinSeconds), effectiveValue(tmpl.globalCooldownMinSeconds, b.globalCooldownMinSeconds)));
        c.set("#ChangedGlobalCdMax.Visible", !Objects.equals(effectiveValue(edit.globalCooldownMaxSeconds, res.globalCooldownMaxSeconds), effectiveValue(tmpl.globalCooldownMaxSeconds, b.globalCooldownMaxSeconds)));

        c.set("#ChangedExtraRolls.Visible", !Arrays.equals(effI(edit.vanillaDroplistExtraRollsPerTier, res.vanillaDroplistExtraRollsPerTier), effI(tmpl.vanillaDroplistExtraRollsPerTier, b.vanillaDroplistExtraRollsPerTier)));

        double currentDW = effectiveValue(edit.dropWeaponChance, res.dropWeaponChance);
        double tmplDW = effectiveValue(tmpl.dropWeaponChance, b.dropWeaponChance);
        c.set("#ChangedDropWeapon.Visible", currentDW != tmplDW);

        double currentDA = effectiveValue(edit.dropArmorPieceChance, res.dropArmorPieceChance);
        double tmplDA = effectiveValue(tmpl.dropArmorPieceChance, b.dropArmorPieceChance);
        c.set("#ChangedDropArmor.Visible", currentDA != tmplDA);

        double currentDO = effectiveValue(edit.dropOffhandItemChance, res.dropOffhandItemChance);
        double tmplDO = effectiveValue(tmpl.dropOffhandItemChance, b.dropOffhandItemChance);
        c.set("#ChangedDropOffhand.Visible", currentDO != tmplDO);

        double currentDurMin = effectiveValue(edit.droppedGearDurabilityMin, res.droppedGearDurabilityMin);
        double tmplDurMin = effectiveValue(tmpl.droppedGearDurabilityMin, b.droppedGearDurabilityMin);
        c.set("#ChangedLootDurMin.Visible", currentDurMin != tmplDurMin);
        double currentDurMax = effectiveValue(edit.droppedGearDurabilityMax, res.droppedGearDurabilityMax);
        double tmplDurMax = effectiveValue(tmpl.droppedGearDurabilityMax, b.droppedGearDurabilityMax);
        c.set("#ChangedLootDurMax.Visible", currentDurMax != tmplDurMax);

        c.set("#ChangedFallDamage.Visible", !Objects.equals(effectiveValue(edit.eliteFallDamageDisabled, res.eliteFallDamageDisabled), effectiveValue(tmpl.eliteFallDamageDisabled, b.eliteFallDamageDisabled)));
    }

    private static void setAllDiffMarkers(UICommandBuilder c, boolean visible) {
        c.set("#ChangedEnabled.Visible", visible);
        c.set("#ChangedRPGLevelingOverlay.Visible", visible);
        c.set("#ChangedOverlayXPMult.Visible", visible);
        c.set("#ChangedOverlayXPBonus.Visible", visible);
        c.set("#ChangedOverlayMinionXP.Visible", visible);
        c.set("#ChangedProgStyle.Visible", visible);
        c.set("#ChangedSpawnChance.Visible", visible);
        c.set("#ChangedHealthScaling.Visible", visible);
        c.set("#ChangedHealth.Visible", visible);
        c.set("#ChangedDamageScaling.Visible", visible);
        c.set("#ChangedDamage.Visible", visible);
        c.set("#ChangedAbilEnabled.Visible", visible);
        c.set("#ChangedGlobalCdMin.Visible", visible);
        c.set("#ChangedGlobalCdMax.Visible", visible);
        c.set("#ChangedExtraRolls.Visible", visible);
        c.set("#ChangedDropWeapon.Visible", visible);
        c.set("#ChangedDropArmor.Visible", visible);
        c.set("#ChangedDropOffhand.Visible", visible);
        c.set("#ChangedLootDurMin.Visible", visible);
        c.set("#ChangedLootDurMax.Visible", visible);
        c.set("#ChangedFallDamage.Visible", visible);

        c.set("#ChangedNameplateMode.Visible", visible);

    }

    private void updateModifiedFieldMarkers(UICommandBuilder c, ConfigOverlay edit, ResolvedConfig res) {
        ConfigOverlay saved = savedOverlaySnapshot != null ? savedOverlaySnapshot : new ConfigOverlay();

        c.set("#ModifiedEnabled.Visible", !Objects.equals(effectiveValue(edit.enabled, res.enabled), effectiveValue(saved.enabled, res.enabled)));

        c.set("#ModifiedRPGLevelingOverlay.Visible", !Objects.equals(effectiveValue(edit.rpgLevelingEnabled, res.rpgLevelingEnabled), effectiveValue(saved.rpgLevelingEnabled, res.rpgLevelingEnabled)));
        c.set("#ModifiedOverlayXPMult.Visible", !Arrays.equals(effF(edit.xpMultiplierPerTier, res.xpMultiplierPerTier), effF(saved.xpMultiplierPerTier, res.xpMultiplierPerTier)));
        c.set("#ModifiedOverlayXPBonus.Visible", !Objects.equals(effectiveValue(edit.xpBonusPerAbility, res.xpBonusPerAbility), effectiveValue(saved.xpBonusPerAbility, res.xpBonusPerAbility)));
        c.set("#ModifiedOverlayMinionXP.Visible", !Objects.equals(effectiveValue(edit.minionXPMultiplier, res.minionXPMultiplier), effectiveValue(saved.minionXPMultiplier, res.minionXPMultiplier)));

        c.set("#ModifiedProgStyle.Visible", !Objects.equals(effectiveValue(edit.progressionStyle, res.progressionStyle.name()), effectiveValue(saved.progressionStyle, res.progressionStyle.name())));

        c.set("#ModifiedSpawnChance.Visible", !Arrays.equals(effD(edit.spawnChancePerTier, res.spawnChancePerTier), effD(saved.spawnChancePerTier, res.spawnChancePerTier)));

        c.set("#ModifiedEnvRules.Visible", !envRuleMapsEqual(edit.environmentTierRules, saved.environmentTierRules));

        c.set("#ModifiedTierRestrictions.Visible", !Objects.equals(edit.tierOverrides, saved.tierOverrides));

        c.set("#ModifiedHealthScaling.Visible", !Objects.equals(effectiveValue(edit.enableHealthScaling, res.enableHealthScaling), effectiveValue(saved.enableHealthScaling, res.enableHealthScaling)));
        c.set("#ModifiedDamageScaling.Visible", !Objects.equals(effectiveValue(edit.enableDamageScaling, res.enableDamageScaling), effectiveValue(saved.enableDamageScaling, res.enableDamageScaling)));

        c.set("#ModifiedHealth.Visible", !Arrays.equals(effF(edit.healthMultiplierPerTier, res.healthMultiplierPerTier), effF(saved.healthMultiplierPerTier, res.healthMultiplierPerTier)));

        c.set("#ModifiedDamage.Visible", !Arrays.equals(effF(edit.damageMultiplierPerTier, res.damageMultiplierPerTier), effF(saved.damageMultiplierPerTier, res.damageMultiplierPerTier)));

        c.set("#ModifiedHealthVariance.Visible", !Objects.equals(effectiveValue(edit.healthRandomVariance, res.healthRandomVariance), effectiveValue(saved.healthRandomVariance, res.healthRandomVariance)));
        c.set("#ModifiedDamageVariance.Visible", !Objects.equals(effectiveValue(edit.damageRandomVariance, res.damageRandomVariance), effectiveValue(saved.damageRandomVariance, res.damageRandomVariance)));

        c.set("#ModifiedAbilEnabled.Visible", !Objects.equals(edit.abilityOverlays, saved.abilityOverlays));
        c.set("#ModifiedGlobalCdMin.Visible", !Objects.equals(effectiveValue(edit.globalCooldownMinSeconds, res.globalCooldownMinSeconds), effectiveValue(saved.globalCooldownMinSeconds, res.globalCooldownMinSeconds)));
        c.set("#ModifiedGlobalCdMax.Visible", !Objects.equals(effectiveValue(edit.globalCooldownMaxSeconds, res.globalCooldownMaxSeconds), effectiveValue(saved.globalCooldownMaxSeconds, res.globalCooldownMaxSeconds)));

        c.set("#ModifiedExtraRolls.Visible", !Arrays.equals(effI(edit.vanillaDroplistExtraRollsPerTier, res.vanillaDroplistExtraRollsPerTier), effI(saved.vanillaDroplistExtraRollsPerTier, res.vanillaDroplistExtraRollsPerTier)));

        c.set("#ModifiedDropWeapon.Visible", !Objects.equals(effectiveValue(edit.dropWeaponChance, res.dropWeaponChance), effectiveValue(saved.dropWeaponChance, res.dropWeaponChance)));
        c.set("#ModifiedDropArmor.Visible", !Objects.equals(effectiveValue(edit.dropArmorPieceChance, res.dropArmorPieceChance), effectiveValue(saved.dropArmorPieceChance, res.dropArmorPieceChance)));
        c.set("#ModifiedDropOffhand.Visible", !Objects.equals(effectiveValue(edit.dropOffhandItemChance, res.dropOffhandItemChance), effectiveValue(saved.dropOffhandItemChance, res.dropOffhandItemChance)));

        c.set("#ModifiedLootDurMin.Visible", !Objects.equals(effectiveValue(edit.droppedGearDurabilityMin, res.droppedGearDurabilityMin), effectiveValue(saved.droppedGearDurabilityMin, res.droppedGearDurabilityMin)));
        c.set("#ModifiedLootDurMax.Visible", !Objects.equals(effectiveValue(edit.droppedGearDurabilityMax, res.droppedGearDurabilityMax), effectiveValue(saved.droppedGearDurabilityMax, res.droppedGearDurabilityMax)));

        c.set("#ModifiedDistPerTier.Visible", !Objects.equals(effectiveValue(edit.distancePerTier, res.distancePerTier), effectiveValue(saved.distancePerTier, res.distancePerTier)));
        c.set("#ModifiedDistBonusInt.Visible", !Objects.equals(effectiveValue(edit.distanceBonusInterval, res.distanceBonusInterval), effectiveValue(saved.distanceBonusInterval, res.distanceBonusInterval)));
        c.set("#ModifiedDistHPBonus.Visible", !Objects.equals(effectiveValue(edit.distanceHealthBonusPerInterval, res.distanceHealthBonusPerInterval), effectiveValue(saved.distanceHealthBonusPerInterval, res.distanceHealthBonusPerInterval)));
        c.set("#ModifiedDistDMGBonus.Visible", !Objects.equals(effectiveValue(edit.distanceDamageBonusPerInterval, res.distanceDamageBonusPerInterval), effectiveValue(saved.distanceDamageBonusPerInterval, res.distanceDamageBonusPerInterval)));
        c.set("#ModifiedDistHPCap.Visible", !Objects.equals(effectiveValue(edit.distanceHealthBonusCap, res.distanceHealthBonusCap), effectiveValue(saved.distanceHealthBonusCap, res.distanceHealthBonusCap)));
        c.set("#ModifiedDistDMGCap.Visible", !Objects.equals(effectiveValue(edit.distanceDamageBonusCap, res.distanceDamageBonusCap), effectiveValue(saved.distanceDamageBonusCap, res.distanceDamageBonusCap)));

        c.set("#ModifiedFallDamage.Visible", !Objects.equals(effectiveValue(edit.eliteFallDamageDisabled, res.eliteFallDamageDisabled), effectiveValue(saved.eliteFallDamageDisabled, res.eliteFallDamageDisabled)));

        c.set("#ModifiedEnableNameplates.Visible", !Objects.equals(effectiveValue(edit.enableNameplates, res.enableNameplates), effectiveValue(saved.enableNameplates, res.enableNameplates)));
        c.set("#ModifiedNameplateMode.Visible", !Objects.equals(effectiveValue(edit.nameplateMode, res.nameplateMode), effectiveValue(saved.nameplateMode, res.nameplateMode)));
        c.set("#ModifiedNameplateTiers.Visible", !Arrays.equals(effB(edit.nameplateTierEnabled, res.nameplateTierEnabled), effB(saved.nameplateTierEnabled, res.nameplateTierEnabled)));
        c.set("#ModifiedNameplatePrefix.Visible", !Arrays.equals(effS(edit.nameplatePrefixPerTier, res.nameplatePrefixPerTier), effS(saved.nameplatePrefixPerTier, res.nameplatePrefixPerTier)));
        c.set("#ModifiedFamilies.Visible", !Objects.equals(edit.tierPrefixesByFamily, saved.tierPrefixesByFamily));
        c.set("#ModifiedEnableModelScaling.Visible", !Objects.equals(effectiveValue(edit.enableModelScaling, res.enableModelScaling), effectiveValue(saved.enableModelScaling, res.enableModelScaling)));
        c.set("#ModifiedModelScale.Visible", !Arrays.equals(effF(edit.modelScalePerTier, res.modelScalePerTier), effF(saved.modelScalePerTier, res.modelScalePerTier)));
        c.set("#ModifiedModelVariance.Visible", !Objects.equals(effectiveValue(edit.modelScaleVariance, res.modelScaleVariance), effectiveValue(saved.modelScaleVariance, res.modelScaleVariance)));
    }

    private static <T> T effectiveValue(@Nullable T overlay, T base) { return overlay != null ? overlay : base; }
    private static double[] effD(double @Nullable [] overlay, double[] base) { return overlay != null ? overlay : base; }
    private static float[] effF(float @Nullable [] overlay, float[] base) { return overlay != null ? overlay : base; }
    private static boolean[] effB(boolean @Nullable [] overlay, boolean[] base) { return overlay != null ? overlay : base; }
    private static int[] effI(int @Nullable [] overlay, int[] base) { return overlay != null ? overlay : base; }
    private static String[] effS(@Nullable String[] overlay, String[] base) { return overlay != null ? overlay : base; }

    private static ConfigOverlay deepCopyOverlay(ConfigOverlay src) {
        ConfigOverlay dst = new ConfigOverlay();
        dst.enabled = src.enabled;
        dst.progressionStyle = src.progressionStyle;
        dst.spawnChancePerTier = src.spawnChancePerTier != null ? Arrays.copyOf(src.spawnChancePerTier, src.spawnChancePerTier.length) : null;
        dst.distancePerTier = src.distancePerTier;
        dst.distanceBonusInterval = src.distanceBonusInterval;
        dst.distanceHealthBonusPerInterval = src.distanceHealthBonusPerInterval;
        dst.distanceDamageBonusPerInterval = src.distanceDamageBonusPerInterval;
        dst.distanceHealthBonusCap = src.distanceHealthBonusCap;
        dst.distanceDamageBonusCap = src.distanceDamageBonusCap;
        dst.enableHealthScaling = src.enableHealthScaling;
        dst.healthMultiplierPerTier = src.healthMultiplierPerTier != null ? Arrays.copyOf(src.healthMultiplierPerTier, src.healthMultiplierPerTier.length) : null;
        dst.enableDamageScaling = src.enableDamageScaling;
        dst.damageMultiplierPerTier = src.damageMultiplierPerTier != null ? Arrays.copyOf(src.damageMultiplierPerTier, src.damageMultiplierPerTier.length) : null;
        dst.healthRandomVariance = src.healthRandomVariance;
        dst.damageRandomVariance = src.damageRandomVariance;
        if (src.abilityOverlays != null) {
            dst.abilityOverlays = new LinkedHashMap<>();
            for (Map.Entry<String, ConfigOverlay.AbilityOverlay> e : src.abilityOverlays.entrySet()) {
                ConfigOverlay.AbilityOverlay srcAo = e.getValue();
                ConfigOverlay.AbilityOverlay dstAo = new ConfigOverlay.AbilityOverlay();
                dstAo.enabled = srcAo.enabled;
                if (srcAo.linkedEntries != null) {
                    dstAo.linkedEntries = new ArrayList<>();
                    for (ConfigOverlay.AbilityLinkedEntry srcEntry : srcAo.linkedEntries) {
                        dstAo.linkedEntries.add(new ConfigOverlay.AbilityLinkedEntry(
                                srcEntry.key,
                                Arrays.copyOf(srcEntry.enabledPerTier, srcEntry.enabledPerTier.length)
                        ));
                    }
                }
                dst.abilityOverlays.put(e.getKey(), dstAo);
            }
        }
        dst.vanillaDroplistExtraRollsPerTier = src.vanillaDroplistExtraRollsPerTier != null ? Arrays.copyOf(src.vanillaDroplistExtraRollsPerTier, src.vanillaDroplistExtraRollsPerTier.length) : null;
        dst.dropWeaponChance = src.dropWeaponChance;
        dst.dropArmorPieceChance = src.dropArmorPieceChance;
        dst.dropOffhandItemChance = src.dropOffhandItemChance;
        dst.droppedGearDurabilityMin = src.droppedGearDurabilityMin;
        dst.droppedGearDurabilityMax = src.droppedGearDurabilityMax;
        dst.defaultLootTemplate = src.defaultLootTemplate;
        dst.eliteFallDamageDisabled = src.eliteFallDamageDisabled;
        dst.globalCooldownMinSeconds = src.globalCooldownMinSeconds;
        dst.globalCooldownMaxSeconds = src.globalCooldownMaxSeconds;

        dst.enableNameplates = src.enableNameplates;
        dst.nameplateMode = src.nameplateMode;
        dst.nameplateTierEnabled = src.nameplateTierEnabled != null ? Arrays.copyOf(src.nameplateTierEnabled, src.nameplateTierEnabled.length) : null;
        dst.nameplatePrefixPerTier = src.nameplatePrefixPerTier != null ? Arrays.copyOf(src.nameplatePrefixPerTier, src.nameplatePrefixPerTier.length) : null;
        if (src.tierPrefixesByFamily != null) {
            dst.tierPrefixesByFamily = new LinkedHashMap<>();
            for (Map.Entry<String, List<String>> e : src.tierPrefixesByFamily.entrySet()) {
                dst.tierPrefixesByFamily.put(e.getKey(), new ArrayList<>(e.getValue()));
            }
        }
        dst.enableModelScaling = src.enableModelScaling;
        dst.modelScalePerTier = src.modelScalePerTier != null ? Arrays.copyOf(src.modelScalePerTier, src.modelScalePerTier.length) : null;
        dst.modelScaleVariance = src.modelScaleVariance;

        dst.rpgLevelingEnabled = src.rpgLevelingEnabled;
        dst.xpMultiplierPerTier = src.xpMultiplierPerTier != null ? Arrays.copyOf(src.xpMultiplierPerTier, src.xpMultiplierPerTier.length) : null;
        dst.xpBonusPerAbility = src.xpBonusPerAbility;
        dst.minionXPMultiplier = src.minionXPMultiplier;
        if (src.tierOverrides != null) {
            dst.tierOverrides = new LinkedHashMap<>();
            for (Map.Entry<String, ConfigOverlay.TierOverride> e : src.tierOverrides.entrySet()) {
                ConfigOverlay.TierOverride srcTo = e.getValue();
                ConfigOverlay.TierOverride dstTo = new ConfigOverlay.TierOverride();
                dstTo.allowedTiers = Arrays.copyOf(srcTo.allowedTiers, srcTo.allowedTiers.length);
                dst.tierOverrides.put(e.getKey(), dstTo);
            }
        }
        if (src.environmentTierRules != null) {
            dst.environmentTierRules = new LinkedHashMap<>();
            for (Map.Entry<String, double[]> e : src.environmentTierRules.entrySet()) {
                dst.environmentTierRules.put(e.getKey(), Arrays.copyOf(e.getValue(), e.getValue().length));
            }
        }

        if (src.disabledMobRuleKeys != null) {
            dst.disabledMobRuleKeys = new LinkedHashSet<>(src.disabledMobRuleKeys);
        }

        if (src.lootTemplates != null) {
            dst.lootTemplates = deepCopyLootTemplatesMap(src.lootTemplates);
        }
        if (src.lootTemplateCategoryTree != null) {
            dst.lootTemplateCategoryTree = deepCopyLootTemplateCategoryTree(src.lootTemplateCategoryTree);
        }

        if (src.customPreset != null) {
            dst.customPreset = deepCopyOverlay(src.customPreset);
        }
        return dst;
    }

    private static Map<String, RPGMobsConfig.MobRule> deepCopyMobRulesMap(Map<String, RPGMobsConfig.MobRule> src) {
        Map<String, RPGMobsConfig.MobRule> dst = new LinkedHashMap<>();
        for (var e : src.entrySet()) {
            dst.put(e.getKey(), deepCopyMobRule(e.getValue()));
        }
        return dst;
    }

    private static RPGMobsConfig.MobRuleCategory deepCopyMobRuleCategoryTree(RPGMobsConfig.MobRuleCategory src) {
        RPGMobsConfig.MobRuleCategory dst = new RPGMobsConfig.MobRuleCategory();
        dst.name = src.name;
        dst.mobRuleKeys = new ArrayList<>(src.mobRuleKeys);
        dst.children = new ArrayList<>();
        for (RPGMobsConfig.MobRuleCategory child : src.children) {
            dst.children.add(deepCopyMobRuleCategoryTree(child));
        }
        return dst;
    }

    private static Map<String, RPGMobsConfig.LootTemplate> deepCopyLootTemplatesMap(Map<String, RPGMobsConfig.LootTemplate> src) {
        Map<String, RPGMobsConfig.LootTemplate> dst = new LinkedHashMap<>();
        for (var e : src.entrySet()) {
            dst.put(e.getKey(), deepCopyLootTemplate(e.getValue()));
        }
        return dst;
    }

    private static RPGMobsConfig.LootTemplate deepCopyLootTemplate(RPGMobsConfig.LootTemplate src) {
        RPGMobsConfig.LootTemplate dst = new RPGMobsConfig.LootTemplate();
        dst.name = src.name;
        dst.linkedMobRuleKeys = new ArrayList<>(src.linkedMobRuleKeys);
        dst.drops = new ArrayList<>();
        for (RPGMobsConfig.ExtraDropRule drop : src.drops) {
            RPGMobsConfig.ExtraDropRule d = new RPGMobsConfig.ExtraDropRule();
            d.itemId = drop.itemId;
            d.chance = drop.chance;
            d.enabledPerTier = Arrays.copyOf(drop.enabledPerTier, drop.enabledPerTier.length);
            d.minQty = drop.minQty;
            d.maxQty = drop.maxQty;
            dst.drops.add(d);
        }
        return dst;
    }

    private static RPGMobsConfig.LootTemplateCategory deepCopyLootTemplateCategoryTree(RPGMobsConfig.LootTemplateCategory src) {
        RPGMobsConfig.LootTemplateCategory dst = new RPGMobsConfig.LootTemplateCategory();
        dst.name = src.name;
        dst.templateKeys = new ArrayList<>(src.templateKeys);
        dst.children = new ArrayList<>();
        for (RPGMobsConfig.LootTemplateCategory child : src.children) {
            dst.children.add(deepCopyLootTemplateCategoryTree(child));
        }
        return dst;
    }

    private void resetEnvRuleKeys(@Nullable ConfigOverlay overlay, @Nullable ResolvedConfig res) {
        activeEnvRuleKeys.clear();

        Map<String, double[]> merged = new LinkedHashMap<>();
        if (res != null && res.environmentTierRules != null) {
            merged.putAll(res.environmentTierRules);
        }
        if (overlay != null && overlay.environmentTierRules != null) {
            merged.putAll(overlay.environmentTierRules);
        }
        activeEnvRuleKeys.addAll(merged.keySet());
    }

    private void populateEnvRuleRows(UICommandBuilder c, ConfigOverlay overlay, ResolvedConfig res) {

        Map<String, double[]> effectiveRules = new LinkedHashMap<>();
        if (res.environmentTierRules != null) effectiveRules.putAll(res.environmentTierRules);
        if (overlay.environmentTierRules != null) effectiveRules.putAll(overlay.environmentTierRules);

        int numRows = Math.min(activeEnvRuleKeys.size(), AdminUIData.MAX_ENV_RULES);
        for (int i = 0; i < AdminUIData.MAX_ENV_RULES; i++) {
            boolean vis = i < numRows;
            c.set("#EnvRuleRow" + i + ".Visible", vis);
            if (vis && needsFieldRefresh) {
                String key = activeEnvRuleKeys.get(i);
                double[] chances = key.isBlank() ? new double[5]
                        : effectiveRules.getOrDefault(key, new double[5]);
                c.set("#FieldEnvKey" + i + ".Value", key);
                for (int t = 0; t < 5; t++) {
                    double v = t < chances.length ? chances[t] : 0.0;
                    c.set("#EnvT" + (t + 1) + "r" + i + ".Value", fmtDouble(v));
                }
            }
        }
        boolean full = numRows == AdminUIData.MAX_ENV_RULES;
        c.set("#EnvRulesFullMsg.Visible", full);
    }

    private void processEnvRuleRows(AdminUIData data) {
        if (editOverlay == null) return;
        boolean anyNonNull = false;
        for (int i = 0; i < AdminUIData.MAX_ENV_RULES; i++) {
            if (data.envRuleKeys[i] != null || data.envRuleT1[i] != null
                    || data.envRuleT2[i] != null || data.envRuleT3[i] != null
                    || data.envRuleT4[i] != null || data.envRuleT5[i] != null) {
                anyNonNull = true; break;
            }
        }
        if (!anyNonNull) return;

        Map<String, double[]> effectiveRules = new LinkedHashMap<>();
        if (resolvedForSelected != null && resolvedForSelected.environmentTierRules != null) {
            for (Map.Entry<String, double[]> e : resolvedForSelected.environmentTierRules.entrySet()) {
                effectiveRules.put(e.getKey(), Arrays.copyOf(e.getValue(), e.getValue().length));
            }
        }
        if (editOverlay.environmentTierRules != null) {
            for (Map.Entry<String, double[]> e : editOverlay.environmentTierRules.entrySet()) {
                effectiveRules.put(e.getKey(), Arrays.copyOf(e.getValue(), e.getValue().length));
            }
        }

        if (activeEnvRuleKeys.isEmpty()) return;
        Map<String, double[]> updated = new LinkedHashMap<>();
        for (int i = 0; i < activeEnvRuleKeys.size() && i < AdminUIData.MAX_ENV_RULES; i++) {
            String oldKey = activeEnvRuleKeys.get(i);
            String newKey = (data.envRuleKeys[i] != null) ? data.envRuleKeys[i].trim() : oldKey;
            if (newKey == null || newKey.isBlank()) continue;

            double[] existing = effectiveRules.getOrDefault(oldKey, new double[5]);
            if (existing.length < 5) existing = Arrays.copyOf(existing, 5);
            double[] chances = Arrays.copyOf(existing, 5);

            if (data.envRuleT1[i] != null) { try { chances[0] = Double.parseDouble(sanitizeNumericInput(data.envRuleT1[i])); } catch (NumberFormatException ignored) {} }
            if (data.envRuleT2[i] != null) { try { chances[1] = Double.parseDouble(sanitizeNumericInput(data.envRuleT2[i])); } catch (NumberFormatException ignored) {} }
            if (data.envRuleT3[i] != null) { try { chances[2] = Double.parseDouble(sanitizeNumericInput(data.envRuleT3[i])); } catch (NumberFormatException ignored) {} }
            if (data.envRuleT4[i] != null) { try { chances[3] = Double.parseDouble(sanitizeNumericInput(data.envRuleT4[i])); } catch (NumberFormatException ignored) {} }
            if (data.envRuleT5[i] != null) { try { chances[4] = Double.parseDouble(sanitizeNumericInput(data.envRuleT5[i])); } catch (NumberFormatException ignored) {} }

            updated.put(newKey, chances);
            if (!newKey.equals(oldKey)) activeEnvRuleKeys.set(i, newKey);
        }

        Map<String, double[]> newEnvRules = updated.isEmpty() ? null : updated;

        if (editOverlay.environmentTierRules == null && newEnvRules != null) {
            Map<String, double[]> baseEnv = resolvedForSelected != null ? resolvedForSelected.environmentTierRules : null;
            if (envRuleMapsEqual(newEnvRules, baseEnv)) {
                return;
            }
        }
        editOverlay.environmentTierRules = newEnvRules;
    }

    private void addEnvRuleRow() {
        if (editOverlay == null) return;
        if (activeEnvRuleKeys.size() >= AdminUIData.MAX_ENV_RULES) return;
        activeEnvRuleKeys.add("");
        needsFieldRefresh = true;
    }

    private void deleteEnvRuleRow(int rowIdx) {
        if (editOverlay == null || rowIdx < 0 || rowIdx >= activeEnvRuleKeys.size()) return;
        activeEnvRuleKeys.remove(rowIdx);

        Map<String, double[]> effectiveRules = new LinkedHashMap<>();
        if (resolvedForSelected != null && resolvedForSelected.environmentTierRules != null) {
            resolvedForSelected.environmentTierRules.forEach((k, v) -> effectiveRules.put(k, Arrays.copyOf(v, v.length)));
        }
        if (editOverlay.environmentTierRules != null) {
            editOverlay.environmentTierRules.forEach((k, v) -> effectiveRules.put(k, Arrays.copyOf(v, v.length)));
        }
        Map<String, double[]> surviving = new LinkedHashMap<>();
        for (String key : activeEnvRuleKeys) {
            if (!key.isBlank()) surviving.put(key, effectiveRules.getOrDefault(key, new double[5]));
        }
        editOverlay.environmentTierRules = surviving.isEmpty() ? null : surviving;
        needsFieldRefresh = true;
    }

    private static boolean envRuleMapsEqual(@Nullable Map<String, double[]> a, @Nullable Map<String, double[]> b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        if (a.size() != b.size()) return false;
        for (Map.Entry<String, double[]> e : a.entrySet()) {
            double[] bVal = b.get(e.getKey());
            if (!Arrays.equals(e.getValue(), bVal)) return false;
        }
        return true;
    }

    private void populateAbilityOverrides(UICommandBuilder c) {
        if (needsFieldRefresh) {
            c.set("#AbilTreeFilter.Value", abilTreeFilter);
            c.set("#AbilMobFilter.Value", abilityMobFilter);
        }

        rebuildAbilTreeFiltered();
        int count = abilTreeFiltered.size();
        boolean empty = count == 0;
        c.set("#AbilTreeEmpty.Visible", empty);

        boolean hasExpanded = abilityExpandedIndex >= 0 && abilityExpandedIndex < count;

        for (int i = 0; i < AdminUIData.TREE_ROW_COUNT; i++) {
            if (hasExpanded && i > abilityExpandedIndex) {
                c.set("#AbilRow" + i + ".Visible", false);
            } else if (i < count) {
                String abilId = abilTreeFiltered.get(i);
                boolean enabled = getEffectiveAbilEnabled(abilId);
                c.set("#AbilRow" + i + ".Visible", true);
                c.set("#AbilRowItm" + i + ".Visible", enabled);
                c.set("#AbilRowItmOff" + i + ".Visible", !enabled);
                c.set("#AbilRowTogOn" + i + ".Visible", enabled);
                c.set("#AbilRowTogOff" + i + ".Visible", !enabled);
                String displayName = abilityDisplayName(abilId);
                c.set("#AbilRowItm" + i + ".Text", displayName);
                c.set("#AbilRowItmOff" + i + ".Text", displayName);
                var feature = abilityFeaturesById.get(abilId);
                c.set("#AbilRowDesc" + i + ".Text", feature != null ? feature.description() : "");
            } else {
                c.set("#AbilRow" + i + ".Visible", false);
            }
        }

        c.set("#AbilDetailPanel.Visible", hasExpanded);
        if (hasExpanded) {
            String abilId = abilTreeFiltered.get(abilityExpandedIndex);
            c.set("#AbilDetailName.Text", "Editing " + abilityDisplayName(abilId));

            boolean rowChanged = isAbilityRowChanged(abilId) || hasAbilityConfigChanges();
            c.set("#ModifiedAbilDetail.Visible", rowChanged);
            boolean rowDiffFromTemplate = isAbilityRowDiffFromTemplate(abilId);
            c.set("#ChangedAbilDetail.Visible", rowDiffFromTemplate);

            populateAbilityConfig(c, abilId);

            rebuildAbilMobFiltered();
            int totalItems = abilityMobFiltered.size();
            int totalPages = Math.max(1, (totalItems + ABIL_MOB_PAGE_SIZE - 1) / ABIL_MOB_PAGE_SIZE);
            if (abilityMobPage >= totalPages) abilityMobPage = totalPages - 1;
            int pageStart = abilityMobPage * ABIL_MOB_PAGE_SIZE;

            boolean hasMobFilter = !abilityMobFilter.isEmpty();
            c.set("#AbilMobDeleteFiltered.Visible", hasMobFilter && totalItems > 0);

            for (int j = 0; j < ABIL_MOB_PAGE_SIZE; j++) {
                int itemIndex = pageStart + j;
                boolean rowVis = itemIndex < totalItems;
                c.set("#AbilMobRow" + j + ".Visible", rowVis);
                if (rowVis) {
                    String mobKey = abilityMobFiltered.get(itemIndex);
                    boolean isCat = MobRuleCategoryHelpers.isCategoryKey(mobKey);
                    String displayName = isCat ? "[Category] " + MobRuleCategoryHelpers.fromCategoryKey(mobKey) : mobKey;
                    c.set("#AbilMobNameCat" + j + ".Visible", isCat);
                    c.set("#AbilMobNameMob" + j + ".Visible", !isCat);
                    c.set((isCat ? "#AbilMobNameCat" : "#AbilMobNameMob") + j + ".Text", displayName);
                    c.set("#AbilMobPeek" + j + ".Visible", isCat);
                    boolean[] entryTiers = getEffectiveLinkedEntryTiers(abilId, mobKey);
                    for (int t = 0; t < AdminUIData.TIERS_COUNT; t++) {
                        boolean en = entryTiers[t];
                        c.set("#AbilMobTierOn" + j + "T" + t + ".Visible", en);
                        c.set("#AbilMobTierOff" + j + "T" + t + ".Visible", !en);
                    }
                } else {
                    c.set("#AbilMobNameCat" + j + ".Visible", false);
                    c.set("#AbilMobNameMob" + j + ".Visible", false);
                    c.set("#AbilMobPeek" + j + ".Visible", false);
                }
            }

            int pageEnd = Math.min((abilityMobPage + 1) * ABIL_MOB_PAGE_SIZE, totalItems);
            c.set("#AbilMobPageInfo.Text", pageEnd + "/" + totalItems);
            c.set("#AbilMobPagination.Visible", true);
            boolean multiPage = totalPages > 1;
            c.set("#AbilMobFirstPage.Visible", multiPage && abilityMobPage > 0);
            c.set("#AbilMobPrevPage.Visible", multiPage && abilityMobPage > 0);
            c.set("#AbilMobNextPage.Visible", multiPage && abilityMobPage < totalPages - 1);
            c.set("#AbilMobLastPage.Visible", multiPage && abilityMobPage < totalPages - 1);
        }
    }

    private boolean getEffectiveAbilEnabled(String abilId) {
        if (editOverlay != null && editOverlay.abilityOverlays != null) {
            var ao = editOverlay.abilityOverlays.get(abilId);
            if (ao != null && ao.enabled != null) return ao.enabled;
        }
        if (resolvedForSelected != null) {
            var ra = resolvedForSelected.resolvedAbilities.get(abilId);
            if (ra != null) return ra.enabled;
        }
        return true;
    }

    private List<ConfigOverlay.AbilityLinkedEntry> getEffectiveLinkedEntries(String abilId) {
        if (editOverlay != null && editOverlay.abilityOverlays != null) {
            var ao = editOverlay.abilityOverlays.get(abilId);
            if (ao != null && ao.linkedEntries != null) return ao.linkedEntries;
        }
        if (resolvedForSelected != null) {
            var ra = resolvedForSelected.resolvedAbilities.get(abilId);
            if (ra != null) {
                var result = new ArrayList<ConfigOverlay.AbilityLinkedEntry>();
                for (var me : ra.linkedMobEntries.entrySet()) {
                    result.add(new ConfigOverlay.AbilityLinkedEntry(me.getKey(), Arrays.copyOf(me.getValue(), me.getValue().length)));
                }
                return result;
            }
        }
        return List.of();
    }

    private List<String> getEffectiveLinkedMobKeys(String abilId) {
        var entries = getEffectiveLinkedEntries(abilId);
        var keys = new ArrayList<String>();
        for (var entry : entries) {
            keys.add(entry.key);
        }
        return keys;
    }

    private boolean[] getEffectiveLinkedEntryTiers(String abilId, String mobKey) {
        if (editOverlay != null && editOverlay.abilityOverlays != null) {
            var ao = editOverlay.abilityOverlays.get(abilId);
            if (ao != null && ao.linkedEntries != null) {
                for (var entry : ao.linkedEntries) {
                    if (entry.key.equals(mobKey)) return entry.enabledPerTier;
                }
            }
        }
        if (resolvedForSelected != null) {
            var ra = resolvedForSelected.resolvedAbilities.get(abilId);
            if (ra != null) {
                boolean[] tiers = ra.linkedMobEntries.get(mobKey);
                if (tiers != null) return tiers;
            }
        }
        return new boolean[]{true, true, true, true, true};
    }

    private void ensureAbilityOverlay(String abilId) {
        if (editOverlay == null) return;
        if (editOverlay.abilityOverlays == null) {
            editOverlay.abilityOverlays = new LinkedHashMap<>();
        }
        editOverlay.abilityOverlays.computeIfAbsent(abilId, _ -> new ConfigOverlay.AbilityOverlay());
    }

    private void ensureAbilityOverlayWithLinkedEntries(String abilId) {
        ensureAbilityOverlay(abilId);
        var ao = editOverlay.abilityOverlays.get(abilId);
        if (ao != null && ao.linkedEntries == null) {
            ao.linkedEntries = new ArrayList<>(getEffectiveLinkedEntries(abilId));
        }
    }

    private void rebuildAbilMobFiltered() {
        abilityMobFiltered.clear();
        if (abilityExpandedIndex < 0 || abilityExpandedIndex >= abilTreeFiltered.size()) return;
        String abilId = abilTreeFiltered.get(abilityExpandedIndex);
        List<String> allKeys = getEffectiveLinkedMobKeys(abilId);
        String lowerFilter = abilityMobFilter.toLowerCase();
        for (String key : allKeys) {
            String searchable = MobRuleCategoryHelpers.isCategoryKey(key)
                    ? MobRuleCategoryHelpers.fromCategoryKey(key) : key;
            if (lowerFilter.isEmpty() || searchable.toLowerCase().contains(lowerFilter)) {
                abilityMobFiltered.add(key);
            }
        }
    }

    private int maxAbilMobPage() {
        int total = abilityMobFiltered.size();
        return Math.max(0, (total - 1) / ABIL_MOB_PAGE_SIZE);
    }

    private void rebuildAbilTreeFiltered() {
        abilTreeFiltered.clear();
        String lowerFilter = abilTreeFilter.toLowerCase();
        for (String abilId : discoveredAbilityIds) {
            if (lowerFilter.isEmpty()) {
                abilTreeFiltered.add(abilId);
            } else {
                String displayName = abilityDisplayName(abilId);
                if (displayName.toLowerCase().contains(lowerFilter) || abilId.toLowerCase().contains(lowerFilter)) {
                    abilTreeFiltered.add(abilId);
                }
            }
        }
    }

    private void handleAbilRowClick(int rowIdx) {
        if (rowIdx < 0 || rowIdx >= abilTreeFiltered.size()) return;
        if (abilityExpandedIndex == rowIdx) {
            abilityExpandedIndex = -1;
        } else {
            abilityExpandedIndex = rowIdx;
            abilityMobFilter = "";
            abilityMobPage = 0;
            abilGatePage = 0;
            abilExclPage = 0;
            abilSummonRolePage = 0;
            abilSummonExclPage = 0;
            multiSlashVariantIndex = 0;
            rebuildAbilMobFiltered();
        }
        needsFieldRefresh = true;
    }

    private void handleAbilRowToggle(int rowIdx) {
        if (editOverlay == null || rowIdx < 0 || rowIdx >= abilTreeFiltered.size()) return;
        String abilId = abilTreeFiltered.get(rowIdx);
        boolean current = getEffectiveAbilEnabled(abilId);
        ensureAbilityOverlay(abilId);
        editOverlay.abilityOverlays.get(abilId).enabled = !current;
        needsFieldRefresh = true;
    }

    private void handleAbilMobTierToggle(String action) {
        String suffix = action.substring("AbilMobTier_".length());
        String[] parts = suffix.split("_", 2);
        if (parts.length < 2) return;
        int slot, tier;
        try {
            slot = Integer.parseInt(parts[0]);
            tier = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            return;
        }
        if (editOverlay == null || tier < 0 || tier >= AdminUIData.TIERS_COUNT) return;
        if (abilityExpandedIndex < 0 || abilityExpandedIndex >= abilTreeFiltered.size()) return;
        String abilId = abilTreeFiltered.get(abilityExpandedIndex);
        int actualIdx = abilityMobPage * ABIL_MOB_PAGE_SIZE + slot;
        if (actualIdx < 0 || actualIdx >= abilityMobFiltered.size()) return;
        String mobKey = abilityMobFiltered.get(actualIdx);
        ensureAbilityOverlayWithLinkedEntries(abilId);
        var ao = editOverlay.abilityOverlays.get(abilId);
        if (ao != null && ao.linkedEntries != null) {
            boolean newValue = false;
            for (var entry : ao.linkedEntries) {
                if (entry.key.equals(mobKey)) {
                    entry.enabledPerTier[tier] = !entry.enabledPerTier[tier];
                    newValue = entry.enabledPerTier[tier];
                    needsFieldRefresh = true;
                    break;
                }
            }
            if (MobRuleCategoryHelpers.isCategoryKey(mobKey)) {
                propagateCategoryTierToChildren(ao, mobKey, tier, newValue);
            }
        }
    }

    private void propagateCategoryTierToChildren(ConfigOverlay.AbilityOverlay ao, String categoryKey, int tier, boolean newValue) {
        String catName = MobRuleCategoryHelpers.fromCategoryKey(categoryKey);
        RPGMobsConfig config = plugin.getConfig();
        if (config == null) return;
        RPGMobsConfig.MobRuleCategory tree = getMobRuleCategoryRoot();
        RPGMobsConfig.MobRuleCategory cat = MobRuleCategoryHelpers.findCategoryByName(tree, catName);
        if (cat == null) return;
        Set<String> childKeys = new HashSet<>(MobRuleCategoryHelpers.collectAllMobRuleKeys(cat));
        for (var entry : ao.linkedEntries) {
            if (childKeys.contains(entry.key)) {
                entry.enabledPerTier[tier] = newValue;
            }
        }
    }

    private void handleAbilMobDeleteFiltered() {
        if (editOverlay == null || abilityExpandedIndex < 0 || abilityExpandedIndex >= abilTreeFiltered.size()) return;
        if (abilityMobFilter.isEmpty()) return;
        String abilId = abilTreeFiltered.get(abilityExpandedIndex);
        ensureAbilityOverlayWithLinkedEntries(abilId);
        var ao = editOverlay.abilityOverlays.get(abilId);
        if (ao != null && ao.linkedEntries != null) {
            var keysToRemove = new HashSet<>(abilityMobFiltered);
            ao.linkedEntries.removeIf(e -> keysToRemove.contains(e.key));
        }
        abilityMobPage = 0;
        rebuildAbilMobFiltered();
        needsFieldRefresh = true;
    }

    private void handleAbilMobDeleteAll() {
        if (editOverlay == null || abilityExpandedIndex < 0 || abilityExpandedIndex >= abilTreeFiltered.size()) return;
        String abilId = abilTreeFiltered.get(abilityExpandedIndex);
        ensureAbilityOverlayWithLinkedEntries(abilId);
        var ao = editOverlay.abilityOverlays.get(abilId);
        if (ao != null && ao.linkedEntries != null) {
            ao.linkedEntries.clear();
        }
        abilityMobPage = 0;
        rebuildAbilMobFiltered();
        needsFieldRefresh = true;
    }

    private void handleAbilMobDelete(int slotIdx) {
        if (editOverlay == null || abilityExpandedIndex < 0 || abilityExpandedIndex >= abilTreeFiltered.size()) return;
        int actualIdx = abilityMobPage * ABIL_MOB_PAGE_SIZE + slotIdx;
        if (actualIdx < 0 || actualIdx >= abilityMobFiltered.size()) return;
        String mobKey = abilityMobFiltered.get(actualIdx);
        String abilId = abilTreeFiltered.get(abilityExpandedIndex);
        ensureAbilityOverlayWithLinkedEntries(abilId);
        var ao = editOverlay.abilityOverlays.get(abilId);
        if (ao != null && ao.linkedEntries != null) {
            ao.linkedEntries.removeIf(e -> e.key.equals(mobKey));
        }
        rebuildAbilMobFiltered();
        needsFieldRefresh = true;
    }

    private void openAbilCategoryPicker() {
        if (abilityExpandedIndex < 0 || abilityExpandedIndex >= abilTreeFiltered.size()) return;
        linkPopupMode = LinkPopupMode.ABILITY_ADD_CATEGORY;
        linkPopupOpen = true;
        linkPopupNavHistory.clear();
        linkPopupCurrentCategory = getMobRuleCategoryRoot();
        linkPopupSelectedCategory = null;
        rebuildLinkPopupItems();
        needsFieldRefresh = true;
    }

    private void openAbilNpcPicker() {
        if (abilityExpandedIndex < 0 || abilityExpandedIndex >= abilTreeFiltered.size()) return;
        npcPickerMode = NpcPickerMode.ABILITY_LINKED_MOB;
        npcPickerOpen = true;
        npcPickerFilter = "";
        npcPickerCustomId = "";
        npcPickerPage = 0;
        npcPickerSelectedItem = null;
        rebuildNpcPickerFiltered();
        needsFieldRefresh = true;
    }

    private boolean isAbilityRowChanged(String abilId) {
        if (savedOverlaySnapshot == null) return false;
        ConfigOverlay.AbilityOverlay editAo = editOverlay != null && editOverlay.abilityOverlays != null
                ? editOverlay.abilityOverlays.get(abilId) : null;
        ConfigOverlay.AbilityOverlay savedAo = savedOverlaySnapshot.abilityOverlays != null
                ? savedOverlaySnapshot.abilityOverlays.get(abilId) : null;
        return !Objects.equals(editAo, savedAo);
    }

    private boolean isAbilityRowDiffFromTemplate(String abilId) {
        if (lastAppliedTemplateKey == null) return false;
        ConfigTemplate template = ConfigTemplate.get(lastAppliedTemplateKey);
        if (template == null) return false;
        ConfigOverlay tmpl = template.getOverlay();
        ConfigOverlay.AbilityOverlay editAo = editOverlay != null && editOverlay.abilityOverlays != null
                ? editOverlay.abilityOverlays.get(abilId) : null;
        ConfigOverlay.AbilityOverlay tmplAo = tmpl.abilityOverlays != null
                ? tmpl.abilityOverlays.get(abilId) : null;
        return !Objects.equals(editAo, tmplAo);
    }

    private void processOverlayXPFields(AdminUIData data) {
        if (editOverlay == null) return;

        float[] resolvedXP = resolvedForSelected != null ? resolvedForSelected.xpMultiplierPerTier : null;
        String[] xpInputs = new String[5];
        System.arraycopy(data.overlayXPMult, 0, xpInputs, 0, 5);
        editOverlay.xpMultiplierPerTier = parseFloatArray(xpInputs, editOverlay.xpMultiplierPerTier, resolvedXP);

        editOverlay.xpBonusPerAbility = parseDoubleField(data.overlayXPBonusPerAbility, editOverlay.xpBonusPerAbility);
        editOverlay.minionXPMultiplier = parseDoubleField(data.overlayMinionXPMult, editOverlay.minionXPMultiplier);
    }

    private String abilityDisplayName(String abilityId) {
        var feature = abilityFeaturesById.get(abilityId);
        return feature != null ? feature.displayName() : abilityId;
    }

    private void resetTierOverrides(@Nullable ConfigOverlay overlay) {
        activeTierOverrideKeys.clear();
        tierOverridePage = 0;
        tierOverrideFilter = "";
        if (overlay != null && overlay.tierOverrides != null) {
            activeTierOverrideKeys.addAll(overlay.tierOverrides.keySet());
        }
    }

    private List<Integer> buildTierOverrideVisibleIndices() {
        List<Integer> visible = new ArrayList<>();
        String filter = tierOverrideFilter.trim().toLowerCase();
        for (int i = 0; i < activeTierOverrideKeys.size(); i++) {
            if (filter.isEmpty() || activeTierOverrideKeys.get(i).toLowerCase().contains(filter)) {
                visible.add(i);
            }
        }
        return visible;
    }

    private int maxTierOverridePage() {
        int totalVisible = buildTierOverrideVisibleIndices().size();
        return Math.max(0, (totalVisible - 1) / AdminUIData.TIER_OVERRIDE_PAGE_SIZE);
    }

    private void deleteTierOverrideRow(int slotIdx) {
        if (editOverlay == null) return;
        List<Integer> visibleIndices = buildTierOverrideVisibleIndices();
        int globalIdx = tierOverridePage * AdminUIData.TIER_OVERRIDE_PAGE_SIZE + slotIdx;
        if (globalIdx < 0 || globalIdx >= visibleIndices.size()) return;
        int ruleIdx = visibleIndices.get(globalIdx);
        String keyToRemove = activeTierOverrideKeys.get(ruleIdx);
        activeTierOverrideKeys.remove(ruleIdx);
        if (editOverlay.tierOverrides != null && !keyToRemove.isBlank()) {
            editOverlay.tierOverrides.remove(keyToRemove);
            if (editOverlay.tierOverrides.isEmpty()) editOverlay.tierOverrides = null;
        }
        needsFieldRefresh = true;
    }

    private void processTierRestrictionFilter(AdminUIData data) {
        if (editOverlay == null) return;
        if (data.tierOvrFilter != null) {
            String newFilter = data.tierOvrFilter;
            if (!newFilter.equals(tierOverrideFilter)) {
                tierOverrideFilter = newFilter;
                tierOverridePage = 0;
                needsFieldRefresh = true;
            }
        }
    }

    private void populateTierRestrictionRows(UICommandBuilder c, ConfigOverlay overlay) {
        List<Integer> visibleIndices = buildTierOverrideVisibleIndices();
        int page = tierOverridePage;
        int totalVisible = visibleIndices.size();
        int totalPages = Math.max(1, (totalVisible + AdminUIData.TIER_OVERRIDE_PAGE_SIZE - 1) / AdminUIData.TIER_OVERRIDE_PAGE_SIZE);

        if (page >= totalPages) { tierOverridePage = totalPages - 1; page = tierOverridePage; }
        int pageStart = page * AdminUIData.TIER_OVERRIDE_PAGE_SIZE;

        Map<String, ConfigOverlay.TierOverride> overrides = overlay.tierOverrides != null ? overlay.tierOverrides : Collections.emptyMap();

        for (int slot = 0; slot < AdminUIData.TIER_OVERRIDE_PAGE_SIZE; slot++) {
            int globalIdx = pageStart + slot;
            boolean vis = globalIdx < totalVisible;
            c.set("#TierOvrRow" + slot + ".Visible", vis);
            if (vis) {
                int ruleIdx = visibleIndices.get(globalIdx);
                String key = activeTierOverrideKeys.get(ruleIdx);
                boolean isCat = MobRuleCategoryHelpers.isCategoryKey(key);
                String displayName = isCat ? MobRuleCategoryHelpers.fromCategoryKey(key) : key;
                c.set("#TierOvrNameCat" + slot + ".Visible", isCat);
                c.set("#TierOvrNameMob" + slot + ".Visible", !isCat);
                c.set("#TierOvrPeek" + slot + ".Visible", isCat);
                if (isCat) {
                    c.set("#TierOvrNameCat" + slot + ".Text", displayName);
                } else {
                    c.set("#TierOvrNameMob" + slot + ".Text", displayName);
                }
                ConfigOverlay.TierOverride to = (!key.isBlank() && overrides.containsKey(key))
                        ? overrides.get(key)
                        : new ConfigOverlay.TierOverride();
                for (int t = 0; t < 5; t++) {
                    boolean allowed = to.allowedTiers[t];
                    c.set("#TierOvrTierOn" + slot + "T" + t + ".Visible", allowed);
                    c.set("#TierOvrTierOff" + slot + "T" + t + ".Visible", !allowed);
                }
            } else {
                c.set("#TierOvrPeek" + slot + ".Visible", false);
            }
        }

        if (needsFieldRefresh) c.set("#TierOvrFilter.Value", tierOverrideFilter);
        boolean tierOvrEmpty = activeTierOverrideKeys.isEmpty();
        c.set("#TierOvrEmpty.Visible", tierOvrEmpty);
        c.set("#TierOvrHeaders.Visible", !tierOvrEmpty);

        int pageEnd = Math.min((page + 1) * AdminUIData.TIER_OVERRIDE_PAGE_SIZE, totalVisible);
        c.set("#TierOvrPageInfo.Text", pageEnd + "/" + totalVisible);
        c.set("#TierOvrPagination.Visible", totalVisible > 0);
        boolean multiPage = totalPages > 1;
        c.set("#TierOvrFirstPage.Visible", multiPage && page > 0);
        c.set("#TierOvrPrevPage.Visible", multiPage && page > 0);
        c.set("#TierOvrNextPage.Visible", multiPage && page < totalPages - 1);
        c.set("#TierOvrLastPage.Visible", multiPage && page < totalPages - 1);
    }

    private void handleToggleTierOvrTier(String suffix) {
        String[] parts = suffix.split("_");
        if (parts.length != 2) return;
        int slotIdx;
        int tier;
        try {
            slotIdx = Integer.parseInt(parts[0]);
            tier = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) { return; }
        if (editOverlay == null || tier < 0 || tier >= 5) return;

        List<Integer> visibleIndices = buildTierOverrideVisibleIndices();
        int globalIdx = tierOverridePage * AdminUIData.TIER_OVERRIDE_PAGE_SIZE + slotIdx;
        if (globalIdx >= visibleIndices.size()) return;
        int ruleIdx = visibleIndices.get(globalIdx);
        String key = activeTierOverrideKeys.get(ruleIdx);
        if (key.isBlank()) return;

        if (editOverlay.tierOverrides == null) editOverlay.tierOverrides = new LinkedHashMap<>();
        ConfigOverlay.TierOverride to = editOverlay.tierOverrides.computeIfAbsent(key, _ -> new ConfigOverlay.TierOverride());
        to.allowedTiers[tier] = !to.allowedTiers[tier];

        boolean anyOn = false;
        for (boolean b : to.allowedTiers) { if (b) { anyOn = true; break; } }
        if (!anyOn) to.allowedTiers[4] = true;

        needsFieldRefresh = true;
    }

    private void snapshotDefaultMobRules() {
        RPGMobsConfig config = plugin.getConfig();
        activeDefRuleKeys.clear();
        editMobRules.clear();
        savedMobRules.clear();
        if (config != null && config.mobsConfig.defaultMobRules != null) {
            for (Map.Entry<String, RPGMobsConfig.MobRule> e : config.mobsConfig.defaultMobRules.entrySet()) {
                activeDefRuleKeys.add(e.getKey());
                editMobRules.put(e.getKey(), deepCopyMobRule(e.getValue()));
                savedMobRules.put(e.getKey(), deepCopyMobRule(e.getValue()));
            }
        }
        backfillMobRuleDefaults(editMobRules);
        backfillMobRuleDefaults(savedMobRules);
    }

    private void snapshotMobRuleCategoryTree() {
        String prevCatName = currentMobRuleCategory != null ? currentMobRuleCategory.name : null;
        List<String> prevNavNames = mobRuleNavHistory.stream().map(c -> c.name).toList();
        List<String> prevFwdNames = mobRuleForwardHistory.stream().map(c -> c.name).toList();
        int prevExpanded = mobRuleTreeExpandedIndex;

        RPGMobsConfig config = plugin.getConfig();
        if (config != null && config.mobsConfig.categoryTree != null) {
            editMobRuleCategoryTree = deepCopyMobRuleCategoryTree(config.mobsConfig.categoryTree);
            savedMobRuleCategoryTree = deepCopyMobRuleCategoryTree(config.mobsConfig.categoryTree);
        } else {
            editMobRuleCategoryTree = new RPGMobsConfig.MobRuleCategory("All", List.of());
            savedMobRuleCategoryTree = new RPGMobsConfig.MobRuleCategory("All", List.of());
        }

        if (prevCatName != null) {
            currentMobRuleCategory = findMobRuleCategoryByName(getMobRuleCategoryRoot(), prevCatName);
            mobRuleNavHistory.clear();
            for (String name : prevNavNames) {
                var cat = findMobRuleCategoryByName(getMobRuleCategoryRoot(), name);
                if (cat != null) mobRuleNavHistory.addLast(cat);
            }
            mobRuleForwardHistory.clear();
            for (String name : prevFwdNames) {
                var cat = findMobRuleCategoryByName(getMobRuleCategoryRoot(), name);
                if (cat != null) mobRuleForwardHistory.addLast(cat);
            }
            mobRuleTreeExpandedIndex = prevExpanded;
        } else {
            currentMobRuleCategory = null;
            mobRuleNavHistory.clear();
            mobRuleForwardHistory.clear();
            mobRuleTreeExpandedIndex = -1;
        }
    }

    private boolean hasGlobalMobRuleChanges() {
        if (hasDefRuleChanges()) return true;
        return !Objects.equals(editMobRuleCategoryTree, savedMobRuleCategoryTree);
    }

    private boolean hasDisabledMobRuleChanges() {
        return !editDisabledMobRuleKeys.equals(savedDisabledMobRuleKeys);
    }

    private void loadDisabledMobRuleKeysFromOverlay() {
        editDisabledMobRuleKeys.clear();
        savedDisabledMobRuleKeys.clear();
        perWorldCurrentCategory = null;
        if (editOverlay != null && editOverlay.disabledMobRuleKeys != null) {
            editDisabledMobRuleKeys.addAll(editOverlay.disabledMobRuleKeys);
        }
        if (savedOverlaySnapshot != null && savedOverlaySnapshot.disabledMobRuleKeys != null) {
            savedDisabledMobRuleKeys.addAll(savedOverlaySnapshot.disabledMobRuleKeys);
        }
    }

    private void syncDisabledMobRuleKeysToOverlay() {
        if (editOverlay == null) return;
        if (!hasDisabledMobRuleChanges()) return;
        if (editDisabledMobRuleKeys.isEmpty()) {
            editOverlay.disabledMobRuleKeys = null;
        } else {
            editOverlay.disabledMobRuleKeys = new LinkedHashSet<>(editDisabledMobRuleKeys);
        }
    }

    private void populatePerWorldMobRuleList(UICommandBuilder c) {
        var root = editMobRuleCategoryTree;
        if (root == null) root = new RPGMobsConfig.MobRuleCategory("All", List.of());

        var currentCat = perWorldCurrentCategory != null ? perWorldCurrentCategory : root;

        List<TreeItem> items = new ArrayList<>();
        for (var child : currentCat.children) {
            items.add(new TreeItem(child.name, true));
        }
        for (var key : currentCat.mobRuleKeys) {
            items.add(new TreeItem(key, false));
        }

        String filter = mobRuleTreeFilter.trim().toLowerCase();
        if (!filter.isEmpty()) {
            items.clear();
            collectFilteredMobRuleItems(root, filter, items);
        }

        if (needsFieldRefresh) {
            c.set("#MobRuleTreeFilter.Value", mobRuleTreeFilter);
        }

        boolean isRoot = currentCat == root;
        c.set("#MobRuleBreadcrumb.Visible", !isRoot && filter.isEmpty());
        if (!isRoot && filter.isEmpty()) {
            c.set("#MobRuleBreadcrumbText.Text", currentCat.name);
            c.set("#MobRuleNavBack.Visible", true);
            c.set("#MobRuleNavForward.Visible", false);
        }
        c.set("#MobRuleTreeEmpty.Visible", items.isEmpty());
        c.set("#MobRuleDeleteFiltered.Visible", false);
        c.set("#MobRuleDeleteAll.Visible", false);
        c.set("#AddMobRuleCategory.Visible", false);
        c.set("#AddMobRuleItem.Visible", false);
        c.set("#GlobMobRuleDetailPanel.Visible", false);

        int count = countEnabledMobRules(root);
        int total = countTotalMobRules(root);
        c.set("#PerWorldMobRuleSummary.Visible", true);
        c.set("#PerWorldMobRuleSummary.Text", count + " of " + total + " rules active in this world");

        for (int i = 0; i < AdminUIData.TREE_ROW_COUNT; i++) {
            if (i < items.size()) {
                var item = items.get(i);
                c.set("#MobRuleRow" + i + ".Visible", true);
                c.set("#MobRuleRowRen" + i + ".Visible", false);
                c.set("#MobRuleRowMov" + i + ".Visible", false);
                c.set("#MobRuleRowDel" + i + ".Visible", false);

                if (item.isCategory()) {
                    c.set("#MobRuleRowCat" + i + ".Visible", true);
                    c.set("#MobRuleRowCat" + i + ".Text", "[>] " + item.name());
                    c.set("#MobRuleRowItm" + i + ".Visible", false);
                    c.set("#MobRuleRowItmOff" + i + ".Visible", false);

                    var childCat = findChildCategoryByName(currentCat, item.name());
                    if (childCat != null) {
                        boolean allOff = areCategoryChildrenAllDisabled(childCat);
                        c.set("#MobRuleRowTogWrap" + i + ".Visible", true);
                        c.set("#MobRuleRowTogOn" + i + ".Visible", !allOff);
                        c.set("#MobRuleRowTogOff" + i + ".Visible", allOff);
                    } else {
                        c.set("#MobRuleRowTogWrap" + i + ".Visible", false);
                    }
                } else {
                    c.set("#MobRuleRowCat" + i + ".Visible", false);
                    var rule = editMobRules.get(item.name());
                    boolean globalOff = rule != null && !rule.enabled;
                    boolean perWorldOff = editDisabledMobRuleKeys.contains(item.name());
                    boolean effectiveOff = globalOff || perWorldOff;

                    c.set("#MobRuleRowTogWrap" + i + ".Visible", !globalOff);
                    c.set("#MobRuleRowTogOn" + i + ".Visible", !globalOff && !perWorldOff);
                    c.set("#MobRuleRowTogOff" + i + ".Visible", !globalOff && perWorldOff);

                    String displayName = item.name();
                    if (globalOff) displayName = "[GLOBAL OFF] " + displayName;

                    c.set("#MobRuleRowItm" + i + ".Visible", !effectiveOff);
                    c.set("#MobRuleRowItmOff" + i + ".Visible", effectiveOff);
                    c.set("#MobRuleRowItm" + i + ".Text", displayName);
                    c.set("#MobRuleRowItmOff" + i + ".Text", displayName);
                }
            } else {
                c.set("#MobRuleRow" + i + ".Visible", false);
            }
        }
    }

    private void collectFilteredMobRuleItems(RPGMobsConfig.MobRuleCategory cat, String filter, List<TreeItem> result) {
        for (var key : cat.mobRuleKeys) {
            if (key.toLowerCase().contains(filter)) result.add(new TreeItem(key, false));
        }
        for (var child : cat.children) {
            collectFilteredMobRuleItems(child, filter, result);
        }
    }

    private int countEnabledMobRules(RPGMobsConfig.MobRuleCategory cat) {
        int count = 0;
        for (var key : cat.mobRuleKeys) {
            var rule = editMobRules.get(key);
            boolean globalOff = rule != null && !rule.enabled;
            boolean perWorldOff = editDisabledMobRuleKeys.contains(key);
            if (!globalOff && !perWorldOff) count++;
        }
        for (var child : cat.children) count += countEnabledMobRules(child);
        return count;
    }

    private int countTotalMobRules(RPGMobsConfig.MobRuleCategory cat) {
        int count = cat.mobRuleKeys.size();
        for (var child : cat.children) count += countTotalMobRules(child);
        return count;
    }

    private RPGMobsConfig.@Nullable MobRuleCategory findChildCategoryByName(RPGMobsConfig.MobRuleCategory parent, String name) {
        for (var child : parent.children) {
            if (child.name.equals(name)) return child;
        }
        return null;
    }

    private boolean isCategoryAllDisabledGlobal(String categoryName) {
        RPGMobsConfig.MobRuleCategory cat = MobRuleCategoryHelpers.findCategoryByName(
                editMobRuleCategoryTree, categoryName);
        if (cat == null) return false;
        var allKeys = MobRuleCategoryHelpers.collectAllMobRuleKeys(cat);
        if (allKeys.isEmpty()) return false;
        for (String key : allKeys) {
            var rule = editMobRules.get(key);
            if (rule != null && rule.enabled) return false;
        }
        return true;
    }

    private void toggleCategoryGlobal(String categoryName) {
        RPGMobsConfig.MobRuleCategory cat = MobRuleCategoryHelpers.findCategoryByName(
                editMobRuleCategoryTree, categoryName);
        if (cat == null) return;
        boolean allDisabled = isCategoryAllDisabledGlobal(categoryName);
        var allKeys = MobRuleCategoryHelpers.collectAllMobRuleKeys(cat);
        for (String key : allKeys) {
            ensureGlobMobRuleExists(key);
            var rule = editMobRules.get(key);
            if (rule != null) rule.enabled = allDisabled;
        }
        needsFieldRefresh = true;
    }

    private boolean areCategoryChildrenAllDisabled(RPGMobsConfig.MobRuleCategory cat) {
        var allKeys = MobRuleCategoryHelpers.collectAllMobRuleKeys(cat);
        if (allKeys.isEmpty()) return false;
        for (String key : allKeys) {
            var rule = editMobRules.get(key);
            boolean globalOff = rule != null && !rule.enabled;
            boolean perWorldOff = editDisabledMobRuleKeys.contains(key);
            if (!globalOff && !perWorldOff) return false;
        }
        return true;
    }

    private void toggleCategoryMobRules(RPGMobsConfig.MobRuleCategory cat, boolean disable) {
        var allKeys = MobRuleCategoryHelpers.collectAllMobRuleKeys(cat);
        for (String key : allKeys) {
            var rule = editMobRules.get(key);
            boolean globalOff = rule != null && !rule.enabled;
            if (globalOff) continue;
            if (disable) {
                editDisabledMobRuleKeys.add(key);
            } else {
                editDisabledMobRuleKeys.remove(key);
            }
        }
    }

    private RPGMobsConfig.@Nullable MobRuleCategory findParentMobRuleCategory(RPGMobsConfig.MobRuleCategory root, RPGMobsConfig.MobRuleCategory target) {
        for (var child : root.children) {
            if (child == target) return root;
            var found = findParentMobRuleCategory(child, target);
            if (found != null) return found;
        }
        return null;
    }

    private void openTierRestrictionCategoryPicker() {
        if (editOverlay == null) return;
        linkPopupMode = LinkPopupMode.TIER_RESTRICTION_ADD_CATEGORY;
        linkPopupOpen = true;
        linkPopupNavHistory.clear();
        linkPopupCurrentCategory = getMobRuleCategoryRoot();
        linkPopupSelectedCategory = null;
        rebuildLinkPopupItems();
        needsFieldRefresh = true;
    }

    private void openTierRestrictionNpcPicker() {
        if (editOverlay == null) return;
        npcPickerMode = NpcPickerMode.TIER_RESTRICTION;
        npcPickerOpen = true;
        npcPickerFilter = "";
        npcPickerCustomId = "";
        npcPickerPage = 0;
        npcPickerSelectedItem = null;
        rebuildNpcPickerFiltered();
        needsFieldRefresh = true;
    }

    private void addTierRestrictionKey(String key) {
        if (editOverlay == null || key == null || key.isBlank()) return;
        if (activeTierOverrideKeys.contains(key)) return;
        activeTierOverrideKeys.add(key);
        if (editOverlay.tierOverrides == null) editOverlay.tierOverrides = new LinkedHashMap<>();
        editOverlay.tierOverrides.computeIfAbsent(key, _ -> new ConfigOverlay.TierOverride());
        int totalVisible = buildTierOverrideVisibleIndices().size();
        tierOverridePage = Math.max(0, (totalVisible - 1) / AdminUIData.TIER_OVERRIDE_PAGE_SIZE);
        needsFieldRefresh = true;
    }

    private static RPGMobsConfig.MobRule deepCopyMobRule(RPGMobsConfig.MobRule src) {
        RPGMobsConfig.MobRule dst = new RPGMobsConfig.MobRule();
        dst.enabled = src.enabled;
        dst.matchExact = new ArrayList<>(src.matchExact);
        dst.matchStartsWith = new ArrayList<>(src.matchStartsWith);
        dst.matchContains = new ArrayList<>(src.matchContains);
        dst.matchExcludes = new ArrayList<>(src.matchExcludes);
        dst.enableWeaponOverrideForTier = Arrays.copyOf(src.enableWeaponOverrideForTier, 5);
        dst.weaponOverrideMode = src.weaponOverrideMode;
        dst.allowedWeaponCategories = new ArrayList<>(src.allowedWeaponCategories);
        dst.allowedArmorCategories = new ArrayList<>(src.allowedArmorCategories);
        dst.allowedArmorSlots = new ArrayList<>(src.allowedArmorSlots);
        return dst;
    }

    private void snapshotLootTemplates() {
        savedLootTemplateSnapshot.clear();
        Map<String, RPGMobsConfig.LootTemplate> source;
        if (savedOverlaySnapshot != null && savedOverlaySnapshot.lootTemplates != null) {
            source = savedOverlaySnapshot.lootTemplates;
        } else {
            RPGMobsConfig config = plugin.getConfig();
            source = config != null ? config.lootConfig.lootTemplates : Map.of();
        }
        for (var e : source.entrySet()) {
            savedLootTemplateSnapshot.put(e.getKey(), lootTemplateFingerprint(e.getValue()));
        }
    }

    private void initEditLootTemplatesFromBase() {
        editLootTemplates.clear();
        RPGMobsConfig config = plugin.getConfig();
        if (config != null && config.lootConfig.lootTemplates != null) {
            for (var e : config.lootConfig.lootTemplates.entrySet()) {
                editLootTemplates.put(e.getKey(), deepCopyLootTemplate(e.getValue()));
            }
        }
    }

    private static String lootTemplateFingerprint(RPGMobsConfig.LootTemplate template) {
        var sb = new StringBuilder();
        sb.append(template.name).append('|');
        sb.append(template.linkedMobRuleKeys).append('|');
        for (RPGMobsConfig.ExtraDropRule d : template.drops) {
            sb.append(d.itemId).append(',')
              .append(d.chance).append(',')
              .append(d.minQty).append(',')
              .append(d.maxQty).append(',')
              .append(Arrays.toString(d.enabledPerTier)).append(';');
        }
        return sb.toString();
    }

    private void snapshotEntityEffects() {
        editEntityEffects.clear();
        savedEntityEffects.clear();
        entityEffectKeys.clear();
        RPGMobsConfig config = plugin.getConfig();
        if (config != null && config.effectsConfig.defaultEntityEffects != null) {
            for (Map.Entry<String, RPGMobsConfig.EntityEffectConfig> e : config.effectsConfig.defaultEntityEffects.entrySet()) {
                entityEffectKeys.add(e.getKey());
                editEntityEffects.put(e.getKey(), deepCopyEntityEffect(e.getValue()));
                savedEntityEffects.put(e.getKey(), deepCopyEntityEffect(e.getValue()));
            }
        }
    }

    private static RPGMobsConfig.EntityEffectConfig deepCopyEntityEffect(RPGMobsConfig.EntityEffectConfig src) {
        RPGMobsConfig.EntityEffectConfig dst = new RPGMobsConfig.EntityEffectConfig();
        dst.isEnabled = src.isEnabled;
        dst.isEnabledPerTier = Arrays.copyOf(src.isEnabledPerTier, src.isEnabledPerTier.length);
        dst.amountMultiplierPerTier = Arrays.copyOf(src.amountMultiplierPerTier, src.amountMultiplierPerTier.length);
        dst.infinite = src.infinite;
        dst.templates.putAll(src.templates);
        return dst;
    }

    private void snapshotAbilityConfigs() {
        editAbilityConfigs.clear();
        savedAbilityConfigs.clear();
        RPGMobsConfig config = plugin.getConfig();
        if (config != null && config.abilitiesConfig.defaultAbilities != null) {
            for (var entry : config.abilitiesConfig.defaultAbilities.entrySet()) {
                editAbilityConfigs.put(entry.getKey(), deepCopyAbilityConfig(entry.getKey(), entry.getValue()));
                savedAbilityConfigs.put(entry.getKey(), deepCopyAbilityConfig(entry.getKey(), entry.getValue()));
            }
        }
    }

    private RPGMobsConfig.AbilityConfig deepCopyAbilityConfig(String abilityId,
                                                                RPGMobsConfig.AbilityConfig source) {
        var feature = abilityFeaturesById.get(abilityId);
        RPGMobsConfig.AbilityConfig destination = feature != null
                ? feature.createDefaultConfig()
                : new RPGMobsConfig.AbilityConfig();
        if (feature != null) {
            for (var field : feature.describeConfigFields()) {
                field.deepCopy(source, destination);
            }
        }
        destination.isEnabled = source.isEnabled;
        destination.isEnabledPerTier = Arrays.copyOf(source.isEnabledPerTier, source.isEnabledPerTier.length);
        destination.gate = new RPGMobsConfig.AbilityGate();
        destination.gate.allowedWeaponCategories = new ArrayList<>(source.gate != null && source.gate.allowedWeaponCategories != null
                ? source.gate.allowedWeaponCategories : List.of());
        destination.linkedMobRuleKeys = new ArrayList<>(source.linkedMobRuleKeys != null ? source.linkedMobRuleKeys : List.of());
        destination.excludeLinkedMobRuleKeys = new ArrayList<>(source.excludeLinkedMobRuleKeys != null ? source.excludeLinkedMobRuleKeys : List.of());
        destination.chancePerTier = Arrays.copyOf(source.chancePerTier, source.chancePerTier.length);
        destination.cooldownSecondsPerTier = Arrays.copyOf(source.cooldownSecondsPerTier, source.cooldownSecondsPerTier.length);
        destination.templates.putAll(source.templates);
        if (source instanceof RPGMobsConfig.MultiSlashAbilityConfig srcMs
                && destination instanceof RPGMobsConfig.MultiSlashAbilityConfig dstMs) {
            dstMs.variantConfigs = new LinkedHashMap<>();
            if (srcMs.variantConfigs != null) {
                for (var ve : srcMs.variantConfigs.entrySet()) {
                    var sv = ve.getValue();
                    var dv = new RPGMobsConfig.MultiSlashVariantConfig(
                            Arrays.copyOf(sv.slashTriggerChancePerTier, sv.slashTriggerChancePerTier.length),
                            Arrays.copyOf(sv.cooldownSecondsPerTier, sv.cooldownSecondsPerTier.length),
                            Arrays.copyOf(sv.baseDamagePerHitPerTier, sv.baseDamagePerHitPerTier.length),
                            Arrays.copyOf(sv.forwardDriftForcePerTier, sv.forwardDriftForcePerTier.length),
                            Arrays.copyOf(sv.knockbackForcePerTier, sv.knockbackForcePerTier.length),
                            sv.meleeRange
                    );
                    dstMs.variantConfigs.put(ve.getKey(), dv);
                }
            }
        }
        return destination;
    }

    private static List<String> csvToList(String csv) {
        if (csv == null || csv.isBlank()) return new ArrayList<>();
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private static String listToCsv(List<String> list) {
        return list == null || list.isEmpty() ? "" : String.join(", ", list);
    }

    private boolean hasDefRuleChanges() {
        if (editMobRules.size() != savedMobRules.size()) return true;
        for (var e : editMobRules.entrySet()) {
            RPGMobsConfig.MobRule saved = savedMobRules.get(e.getKey());
            if (saved == null) return true;
            if (!mobRulesEqual(e.getValue(), saved)) return true;
        }
        return false;
    }

    private static boolean mobRulesEqual(RPGMobsConfig.MobRule a, RPGMobsConfig.MobRule b) {
        return a.enabled == b.enabled
                && Objects.equals(a.matchExact, b.matchExact)
                && Objects.equals(a.matchStartsWith, b.matchStartsWith)
                && Objects.equals(a.matchContains, b.matchContains)
                && Objects.equals(a.matchExcludes, b.matchExcludes)
                && Arrays.equals(a.enableWeaponOverrideForTier, b.enableWeaponOverrideForTier)
                && a.weaponOverrideMode == b.weaponOverrideMode
                && Objects.equals(a.allowedWeaponCategories, b.allowedWeaponCategories)
                && Objects.equals(a.allowedArmorCategories, b.allowedArmorCategories)
                && Objects.equals(a.allowedArmorSlots, b.allowedArmorSlots);
    }

    private RPGMobsConfig.MobRuleCategory getMobRuleCategoryRoot() {
        if (editMobRuleCategoryTree != null) {
            return editMobRuleCategoryTree;
        }
        RPGMobsConfig config = plugin.getConfig();
        return config != null && config.mobsConfig.categoryTree != null
                ? config.mobsConfig.categoryTree
                : new RPGMobsConfig.MobRuleCategory("All", List.of());
    }

    private RPGMobsConfig.MobRuleCategory ensureCurrentMobRuleCategory() {
        if (currentMobRuleCategory == null) {
            currentMobRuleCategory = getMobRuleCategoryRoot();
        }
        return currentMobRuleCategory;
    }

    private void navigateToMobRuleCategory(RPGMobsConfig.MobRuleCategory target) {
        if (currentMobRuleCategory != null) {
            mobRuleNavHistory.push(currentMobRuleCategory);
        }
        mobRuleForwardHistory.clear();
        currentMobRuleCategory = target;
        needsFieldRefresh = true;
    }

    private void mobRuleTreeBack() {
        if (mobRuleNavHistory.isEmpty()) return;
        if (currentMobRuleCategory != null) {
            mobRuleForwardHistory.push(currentMobRuleCategory);
        }
        currentMobRuleCategory = mobRuleNavHistory.pop();
        needsFieldRefresh = true;
    }

    private void mobRuleTreeForward() {
        if (mobRuleForwardHistory.isEmpty()) return;
        if (currentMobRuleCategory != null) {
            mobRuleNavHistory.push(currentMobRuleCategory);
        }
        currentMobRuleCategory = mobRuleForwardHistory.pop();
        needsFieldRefresh = true;
    }

    private void addMobRuleCategory() {
        RPGMobsConfig.MobRuleCategory cat = ensureCurrentMobRuleCategory();
        var newCat = new RPGMobsConfig.MobRuleCategory("NewCategory", List.of());
        cat.children.add(newCat);
        needsFieldRefresh = true;
    }

    private void addMobRuleItem() {
        openNpcPicker();
    }

    private void openMobRuleRebindPicker() {
        if (mobRuleTreeExpandedIndex < 0 || mobRuleTreeExpandedIndex >= mobRuleTreeItems.size()) return;
        TreeItem item = mobRuleTreeItems.get(mobRuleTreeExpandedIndex);
        if (item.isCategory) return;
        npcPickerMode = NpcPickerMode.REBIND_MOB_RULE;
        npcPickerOpen = true;
        npcPickerFilter = "";
        npcPickerCustomId = "";
        npcPickerPage = 0;
        npcPickerSelectedItem = null;
        rebuildNpcPickerFiltered();
        needsFieldRefresh = true;
    }

    private String buildMobRuleBreadcrumb() {
        var path = new ArrayList<String>();
        path.add(getMobRuleCategoryRoot().name);
        buildBreadcrumbPath(getMobRuleCategoryRoot(), ensureCurrentMobRuleCategory(), path);
        return "> " + String.join(" > ", path);
    }

    private boolean buildBreadcrumbPath(RPGMobsConfig.MobRuleCategory current,
                                        RPGMobsConfig.MobRuleCategory target,
                                        List<String> path) {
        if (current == target) return true;
        for (RPGMobsConfig.MobRuleCategory child : current.children) {
            path.add(child.name);
            if (buildBreadcrumbPath(child, target, path)) return true;
            path.removeLast();
        }
        return false;
    }

    private void populateMobRuleTree(UICommandBuilder c) {
        c.set("#PerWorldMobRuleSummary.Visible", false);
        if (needsFieldRefresh) {
            c.set("#MobRuleTreeFilter.Value", mobRuleTreeFilter);
        }
        boolean filtering = !mobRuleTreeFilter.isEmpty();

        if (filtering) {
            c.set("#MobRuleBreadcrumb.Visible", false);
            mobRuleTreeItems.clear();
            for (String key : mobRuleTreeFilteredKeys) {
                mobRuleTreeItems.add(new TreeItem(key, false));
            }
        } else {
            c.set("#MobRuleBreadcrumb.Visible", true);
            RPGMobsConfig.MobRuleCategory cat = ensureCurrentMobRuleCategory();
            c.set("#MobRuleNavBack.Visible", !mobRuleNavHistory.isEmpty());
            c.set("#MobRuleNavForward.Visible", !mobRuleForwardHistory.isEmpty());
            c.set("#MobRuleBreadcrumbText.Text", buildMobRuleBreadcrumb());

            mobRuleTreeItems.clear();
            for (RPGMobsConfig.MobRuleCategory child : cat.children) {
                mobRuleTreeItems.add(new TreeItem(child.name, true));
            }
            for (String key : cat.mobRuleKeys) {
                mobRuleTreeItems.add(new TreeItem(key, false));
            }
        }

        boolean empty = mobRuleTreeItems.isEmpty();
        c.set("#MobRuleTreeEmpty.Visible", empty);
        c.set("#MobRuleDeleteFiltered.Visible", filtering && !empty);
        c.set("#MobRuleDeleteAll.Visible", true);
        c.set("#AddMobRuleCategory.Visible", !filtering);
        c.set("#AddMobRuleItem.Visible", true);

        boolean hasExpanded = mobRuleTreeExpandedIndex >= 0 && mobRuleTreeExpandedIndex < mobRuleTreeItems.size()
                && !mobRuleTreeItems.get(mobRuleTreeExpandedIndex).isCategory;

        for (int i = 0; i < AdminUIData.TREE_ROW_COUNT; i++) {
            if (hasExpanded && i > mobRuleTreeExpandedIndex) {
                c.set("#MobRuleRow" + i + ".Visible", false);
            } else if (i < mobRuleTreeItems.size()) {
                TreeItem item = mobRuleTreeItems.get(i);
                c.set("#MobRuleRow" + i + ".Visible", true);
                c.set("#MobRuleRowCat" + i + ".Visible", item.isCategory);
                c.set("#MobRuleRowTogWrap" + i + ".Visible", true);
                c.set("#MobRuleRowRen" + i + ".Visible", !filtering);
                c.set("#MobRuleRowMov" + i + ".Visible", !filtering);
                c.set("#MobRuleRowDel" + i + ".Visible", true);
                if (item.isCategory) {
                    c.set("#MobRuleRowItm" + i + ".Visible", false);
                    c.set("#MobRuleRowItmOff" + i + ".Visible", false);
                    c.set("#MobRuleRowCat" + i + ".Text", "[>] " + item.name);
                    boolean catAllDisabled = isCategoryAllDisabledGlobal(item.name);
                    c.set("#MobRuleRowTogOn" + i + ".Visible", !catAllDisabled);
                    c.set("#MobRuleRowTogOff" + i + ".Visible", catAllDisabled);
                } else {
                    ensureGlobMobRuleExists(item.name);
                    RPGMobsConfig.MobRule rule = editMobRules.get(item.name);
                    boolean enabled = rule != null && rule.enabled;
                    c.set("#MobRuleRowItm" + i + ".Visible", enabled);
                    c.set("#MobRuleRowItmOff" + i + ".Visible", !enabled);
                    c.set("#MobRuleRowTogOn" + i + ".Visible", enabled);
                    c.set("#MobRuleRowTogOff" + i + ".Visible", !enabled);
                    c.set("#MobRuleRowItm" + i + ".Text", item.name);
                    c.set("#MobRuleRowItmOff" + i + ".Text", item.name);
                }
            } else {
                c.set("#MobRuleRow" + i + ".Visible", false);
            }
        }

        populateGlobMobRuleDetail(c);
    }

    private RPGMobsConfig.@Nullable MobRule getExpandedGlobMobRule() {
        if (mobRuleTreeExpandedIndex < 0 || mobRuleTreeExpandedIndex >= mobRuleTreeItems.size()) return null;
        TreeItem item = mobRuleTreeItems.get(mobRuleTreeExpandedIndex);
        if (item.isCategory) return null;
        return editMobRules.get(item.name);
    }

    private void ensureGlobMobRuleExists(String key) {
        if (!editMobRules.containsKey(key)) {
            RPGMobsConfig config = plugin.getConfig();
            RPGMobsConfig.MobRule base = config != null && config.mobsConfig.defaultMobRules != null
                    ? config.mobsConfig.defaultMobRules.get(key) : null;
            if (base != null) {
                RPGMobsConfig.MobRule copy = deepCopyMobRule(base);
                Map<String, RPGMobsConfig.MobRule> single = new LinkedHashMap<>();
                single.put(key, copy);
                backfillMobRuleDefaults(single);
                editMobRules.put(key, copy);
            } else {
                RPGMobsConfig.MobRule rule = new RPGMobsConfig.MobRule();
                rule.matchExact = new ArrayList<>(List.of(key));
                editMobRules.put(key, rule);
            }
        }
    }

    private void populateGlobMobRuleDetail(UICommandBuilder c) {
        if (mobRuleTreeExpandedIndex >= 0 && mobRuleTreeExpandedIndex < mobRuleTreeItems.size()) {
            TreeItem item = mobRuleTreeItems.get(mobRuleTreeExpandedIndex);
            if (!item.isCategory) {
                ensureGlobMobRuleExists(item.name);
            }
        }

        RPGMobsConfig.MobRule expanded = getExpandedGlobMobRule();
        c.set("#GlobMobRuleDetailPanel.Visible", expanded != null);
        if (expanded != null && needsFieldRefresh) {
            String expandedKey = mobRuleTreeItems.get(mobRuleTreeExpandedIndex).name;
            c.set("#GlobMobRuleDetailTitle.Text", "Editing " + expandedKey);
            c.set("#FieldGlobMobRuleMatchExact.Value", listToCsv(expanded.matchExact));
            c.set("#FieldGlobMobRuleMatchPrefix.Value", listToCsv(expanded.matchStartsWith));
            c.set("#FieldGlobMobRuleMatchContains.Value", listToCsv(expanded.matchContains));
            c.set("#FieldGlobMobRuleMatchExcludes.Value", listToCsv(expanded.matchExcludes));
            c.set("#GlobMobRuleCycleMode.Text", expanded.weaponOverrideMode.name());
            c.set("#GlobMobRuleCombatStyle.Text", CombatStyle.parse(expanded.combatStyle).displayName());
            boolean wpnEnabled = expanded.weaponOverrideMode != RPGMobsConfig.WeaponOverrideMode.NONE;
            c.set("#GlobMobRuleWpnTierTable.Visible", wpnEnabled);
            c.set("#GlobMobRuleWpnCatsSection.Visible", wpnEnabled);
            if (wpnEnabled) {
                for (int t = 0; t < 5; t++) {
                    boolean wpnOn = t < expanded.enableWeaponOverrideForTier.length && expanded.enableWeaponOverrideForTier[t];
                    c.set("#GlobMobRuleWpnTierOn" + t + ".Visible", wpnOn);
                    c.set("#GlobMobRuleWpnTierOff" + t + ".Visible", !wpnOn);
                }
                c.set("#GlobMobRuleWpnCatFilter.Value", mobRuleWpnCatFilter);
                populateMobRuleLinkedItems(c, expanded.allowedWeaponCategories, true);
            }
        }

        if (expanded != null && needsFieldRefresh) {
            c.set("#GlobMobRuleArmCatFilter.Value", mobRuleArmCatFilter);
            populateMobRuleLinkedItems(c, expanded.allowedArmorCategories, false);
        }

        if (expanded != null) {
            boolean armorAll = expanded.allowedArmorSlots.isEmpty();
            boolean armorNone = expanded.allowedArmorSlots.size() == 1 && "NONE".equals(expanded.allowedArmorSlots.getFirst());
            renderToggle(c, "#ArmorSlotAll", armorAll);
            boolean headOn = !armorAll && !armorNone && expanded.allowedArmorSlots.contains("Head");
            boolean chestOn = !armorAll && !armorNone && expanded.allowedArmorSlots.contains("Chest");
            boolean handsOn = !armorAll && !armorNone && expanded.allowedArmorSlots.contains("Hands");
            boolean legsOn = !armorAll && !armorNone && expanded.allowedArmorSlots.contains("Legs");
            renderToggle(c, "#ArmorSlotHead", headOn);
            renderToggle(c, "#ArmorSlotChest", chestOn);
            renderToggle(c, "#ArmorSlotHands", handsOn);
            renderToggle(c, "#ArmorSlotLegs", legsOn);
        }
    }

    private void handleMobRuleRowClick(int rowIdx) {
        if (isPerWorldMobRuleMode()) {
            handlePerWorldMobRuleClick(rowIdx);
            return;
        }
        if (rowIdx < 0 || rowIdx >= mobRuleTreeItems.size()) return;
        TreeItem item = mobRuleTreeItems.get(rowIdx);
        if (item.isCategory) {
            RPGMobsConfig.MobRuleCategory cat = ensureCurrentMobRuleCategory();
            for (RPGMobsConfig.MobRuleCategory child : cat.children) {
                if (child.name.equals(item.name)) {
                    mobRuleTreeExpandedIndex = -1;
                    navigateToMobRuleCategory(child);
                    return;
                }
            }
        } else {
            if (mobRuleTreeExpandedIndex == rowIdx) {
                mobRuleTreeExpandedIndex = -1;
            } else {
                mobRuleTreeExpandedIndex = rowIdx;
                mobRuleWpnCatFilter = "";
                mobRuleWpnCatPage = 0;
                mobRuleArmCatFilter = "";
                mobRuleArmCatPage = 0;
            }
            needsFieldRefresh = true;
        }
    }

    private void handleMobRuleRowDelete(int rowIdx) {
        if (rowIdx < 0 || rowIdx >= mobRuleTreeItems.size()) return;
        TreeItem item = mobRuleTreeItems.get(rowIdx);
        if (!mobRuleTreeFilter.isEmpty()) {
            if (!item.isCategory) {
                MobRuleCategoryHelpers.removeMobRuleKeyRecursive(getMobRuleCategoryRoot(), item.name);
                editMobRules.remove(item.name);
                rebuildMobRuleTreeFiltered();
            }
        } else {
            RPGMobsConfig.MobRuleCategory cat = ensureCurrentMobRuleCategory();
            if (item.isCategory) {
                unlinkMobRuleCategoryFromLootTemplates(item.name);
                cat.children.removeIf(child -> child.name.equals(item.name));
            } else {
                cat.mobRuleKeys.remove(item.name);
                editMobRules.remove(item.name);
            }
        }
        mobRuleTreeExpandedIndex = -1;
        needsFieldRefresh = true;
    }

    private void unlinkMobRuleCategoryFromLootTemplates(String categoryName) {
        for (RPGMobsConfig.LootTemplate template : editLootTemplates.values()) {
            template.linkedMobRuleKeys.remove(categoryName);
        }
    }

    private void renameMobRuleCategoryInLinkedKeys(String oldName, String newName) {
        for (RPGMobsConfig.LootTemplate template : editLootTemplates.values()) {
            MobRuleCategoryHelpers.renameCategoryInLinkedKeys(template.linkedMobRuleKeys, oldName, newName);
        }
        if (editOverlay != null && editOverlay.abilityOverlays != null) {
            for (var ao : editOverlay.abilityOverlays.values()) {
                if (ao.linkedEntries != null) {
                    String oldKey = MobRuleCategoryHelpers.toCategoryKey(oldName);
                    String newKey = MobRuleCategoryHelpers.toCategoryKey(newName);
                    for (var entry : ao.linkedEntries) {
                        if (oldKey.equals(entry.key)) {
                            entry.key = newKey;
                        }
                    }
                }
            }
        }
    }

    private void handleMobRuleRowToggle(int rowIdx) {
        if (isPerWorldMobRuleMode()) {
            handlePerWorldMobRuleToggle(rowIdx);
            return;
        }
        if (rowIdx < 0 || rowIdx >= mobRuleTreeItems.size()) return;
        TreeItem item = mobRuleTreeItems.get(rowIdx);
        if (item.isCategory) {
            toggleCategoryGlobal(item.name);
            return;
        }
        ensureGlobMobRuleExists(item.name);
        RPGMobsConfig.MobRule rule = editMobRules.get(item.name);
        if (rule != null) {
            rule.enabled = !rule.enabled;
            needsFieldRefresh = true;
        }
    }

    private boolean isPerWorldMobRuleMode() {
        return (activeSection == Section.WORLD || activeSection == Section.INSTANCE)
                && activeSubTab == TAB_MOB_RULES;
    }

    private void handlePerWorldMobRuleClick(int rowIdx) {
        var root = editMobRuleCategoryTree;
        if (root == null) return;
        var currentCat = perWorldCurrentCategory != null ? perWorldCurrentCategory : root;

        List<TreeItem> items = new ArrayList<>();
        String filter = mobRuleTreeFilter.trim().toLowerCase();
        if (!filter.isEmpty()) {
            collectFilteredMobRuleItems(root, filter, items);
        } else {
            for (var child : currentCat.children) items.add(new TreeItem(child.name, true));
            for (var key : currentCat.mobRuleKeys) items.add(new TreeItem(key, false));
        }

        if (rowIdx < 0 || rowIdx >= items.size()) return;
        var item = items.get(rowIdx);

        if (item.isCategory() && filter.isEmpty()) {
            for (var child : currentCat.children) {
                if (child.name.equals(item.name())) {
                    perWorldCurrentCategory = child;
                    needsFieldRefresh = true;
                    return;
                }
            }
        }
    }

    private void handlePerWorldMobRuleToggle(int rowIdx) {
        var root = editMobRuleCategoryTree;
        if (root == null) return;
        var currentCat = perWorldCurrentCategory != null ? perWorldCurrentCategory : root;

        List<TreeItem> items = new ArrayList<>();
        String filter = mobRuleTreeFilter.trim().toLowerCase();
        if (!filter.isEmpty()) {
            collectFilteredMobRuleItems(root, filter, items);
        } else {
            for (var child : currentCat.children) items.add(new TreeItem(child.name, true));
            for (var key : currentCat.mobRuleKeys) items.add(new TreeItem(key, false));
        }

        if (rowIdx < 0 || rowIdx >= items.size()) return;
        var item = items.get(rowIdx);

        if (item.isCategory()) {
            var childCat = findChildCategoryByName(currentCat, item.name());
            if (childCat != null) {
                boolean allOff = areCategoryChildrenAllDisabled(childCat);
                toggleCategoryMobRules(childCat, !allOff);
            }
            needsFieldRefresh = true;
            return;
        }

        var rule = editMobRules.get(item.name());
        if (rule != null && !rule.enabled) return;

        if (editDisabledMobRuleKeys.contains(item.name())) {
            editDisabledMobRuleKeys.remove(item.name());
        } else {
            editDisabledMobRuleKeys.add(item.name());
        }
        needsFieldRefresh = true;
    }

    private void rebuildMobRuleTreeFiltered() {
        mobRuleTreeFilteredKeys.clear();
        if (!mobRuleTreeFilter.isEmpty()) {
            mobRuleTreeFilteredKeys.addAll(
                    MobRuleCategoryHelpers.searchMobRuleKeysRecursive(getMobRuleCategoryRoot(), mobRuleTreeFilter));
        }
    }

    private void handleMobRuleDeleteFiltered() {
        if (mobRuleTreeFilter.isEmpty() || mobRuleTreeFilteredKeys.isEmpty()) return;
        RPGMobsConfig.MobRuleCategory root = getMobRuleCategoryRoot();
        for (String key : new ArrayList<>(mobRuleTreeFilteredKeys)) {
            MobRuleCategoryHelpers.removeMobRuleKeyRecursive(root, key);
            editMobRules.remove(key);
        }
        mobRuleTreeExpandedIndex = -1;
        rebuildMobRuleTreeFiltered();
        needsFieldRefresh = true;
    }

    private void handleMobRuleDeleteAll() {
        RPGMobsConfig.MobRuleCategory root = getMobRuleCategoryRoot();
        List<String> allKeys = MobRuleCategoryHelpers.collectAllMobRuleKeys(root);
        for (String key : allKeys) {
            editMobRules.remove(key);
        }
        root.mobRuleKeys.clear();
        root.children.clear();
        currentMobRuleCategory = root;
        mobRuleNavHistory.clear();
        mobRuleForwardHistory.clear();
        mobRuleTreeExpandedIndex = -1;
        if (!mobRuleTreeFilter.isEmpty()) {
            rebuildMobRuleTreeFiltered();
        }
        needsFieldRefresh = true;
    }

    private void cycleGlobMobRuleWeaponMode() {
        RPGMobsConfig.MobRule rule = getExpandedGlobMobRule();
        if (rule == null) return;
        rule.weaponOverrideMode = switch (rule.weaponOverrideMode) {
            case NONE -> RPGMobsConfig.WeaponOverrideMode.ONLY_IF_EMPTY;
            case ONLY_IF_EMPTY -> RPGMobsConfig.WeaponOverrideMode.ALWAYS;
            case ALWAYS -> RPGMobsConfig.WeaponOverrideMode.NONE;
        };
        needsFieldRefresh = true;
    }

    private void cycleGlobMobRuleCombatStyle() {
        RPGMobsConfig.MobRule rule = getExpandedGlobMobRule();
        if (rule == null) return;
        rule.combatStyle = CombatStyle.next(CombatStyle.parse(rule.combatStyle)).getId();
        needsFieldRefresh = true;
    }

    private void toggleGlobMobRuleWpnTier(int tier) {
        RPGMobsConfig.MobRule rule = getExpandedGlobMobRule();
        if (rule == null || tier < 0 || tier > 4) return;
        if (rule.enableWeaponOverrideForTier.length < 5) {
            rule.enableWeaponOverrideForTier = Arrays.copyOf(rule.enableWeaponOverrideForTier, 5);
        }
        rule.enableWeaponOverrideForTier[tier] = !rule.enableWeaponOverrideForTier[tier];
        needsFieldRefresh = true;
    }

    private void handleGlobMobRuleWpnCatAdd() {
        RPGMobsConfig.MobRule rule = getExpandedGlobMobRule();
        if (rule == null) return;
        linkPopupMode = LinkPopupMode.WEAPON_CATEGORY_ADD;
        linkPopupOpen = true;
        linkPopupNavHistory.clear();
        linkPopupGearNavHistory.clear();
        linkPopupGearCurrentCategory = editWeaponCategoryTree;
        linkPopupSelectedCategory = null;
        rebuildLinkPopupItems();
        needsFieldRefresh = true;
    }

    private void handleGlobMobRuleWpnCatDel(int slotIdx) {
        RPGMobsConfig.MobRule rule = getExpandedGlobMobRule();
        if (rule == null) return;
        int absIdx = mobRuleWpnCatPage * MOB_RULE_LINKED_PAGE_SIZE + slotIdx;
        List<String> filtered = mobRuleWpnCatFiltered;
        if (absIdx < 0 || absIdx >= filtered.size()) return;
        String key = filtered.get(absIdx);
        if (!(rule.allowedWeaponCategories instanceof ArrayList)) {
            rule.allowedWeaponCategories = new ArrayList<>(rule.allowedWeaponCategories);
        }
        rule.allowedWeaponCategories.remove(key);
        needsFieldRefresh = true;
    }

    private void handleGlobMobRuleWpnCatPeek(int slotIdx) {
        RPGMobsConfig.MobRule rule = getExpandedGlobMobRule();
        if (rule == null) return;
        int absIdx = mobRuleWpnCatPage * MOB_RULE_LINKED_PAGE_SIZE + slotIdx;
        List<String> filtered = mobRuleWpnCatFiltered;
        if (absIdx < 0 || absIdx >= filtered.size()) return;
        String entry = filtered.get(absIdx);
        if (MobRuleCategoryHelpers.isCategoryKey(entry)) {
            String catName = MobRuleCategoryHelpers.fromCategoryKey(entry);
            RPGMobsConfig.GearCategory cat = MobRuleCategoryHelpers.findGearCategoryByName(editWeaponCategoryTree, catName);
            List<String> items = cat != null ? MobRuleCategoryHelpers.collectAllGearItemKeys(cat) : List.of();
            openCategoryPeek("Weapon Category: " + catName, items, CatPeekSource.WEAPON_CAT, entry);
        }
    }

    private void handleGlobMobRuleWpnCatClear() {
        RPGMobsConfig.MobRule rule = getExpandedGlobMobRule();
        if (rule == null) return;
        if (!(rule.allowedWeaponCategories instanceof ArrayList)) {
            rule.allowedWeaponCategories = new ArrayList<>();
        } else {
            rule.allowedWeaponCategories.clear();
        }
        mobRuleWpnCatPage = 0;
        needsFieldRefresh = true;
    }

    private void handleGlobMobRuleWpnCatAddItem() {
        RPGMobsConfig.MobRule rule = getExpandedGlobMobRule();
        if (rule == null) return;
        itemPickerOpen = true;
        itemPickerDropSlot = ITEM_PICKER_MOB_RULE_WPN;
        itemPickerFilter = "";
        itemPickerCustomId = "";
        itemPickerPage = 0;
        itemPickerSelectedItem = null;
        rebuildItemPickerFiltered();
        needsFieldRefresh = true;
    }

    private void handleMobRuleWpnCatNextPage() {
        RPGMobsConfig.MobRule rule = getExpandedGlobMobRule();
        if (rule == null) return;
        rebuildMobRuleLinkedFiltered(rule.allowedWeaponCategories, mobRuleWpnCatFilter, mobRuleWpnCatFiltered);
        int maxPage = Math.max(0, (mobRuleWpnCatFiltered.size() - 1) / MOB_RULE_LINKED_PAGE_SIZE);
        if (mobRuleWpnCatPage < maxPage) mobRuleWpnCatPage++;
        needsFieldRefresh = true;
    }

    private void rebuildMobRuleLinkedFiltered(List<String> allEntries, String filter, List<String> out) {
        out.clear();
        String lower = filter.toLowerCase();
        for (String entry : allEntries) {
            String display = MobRuleCategoryHelpers.isCategoryKey(entry) ? MobRuleCategoryHelpers.fromCategoryKey(entry) : entry;
            if (lower.isEmpty() || display.toLowerCase().contains(lower)) {
                out.add(entry);
            }
        }
    }

    private void populateMobRuleLinkedItems(UICommandBuilder c, List<String> allEntries, boolean isWeapon) {
        String prefix = isWeapon ? "#GlobMobRuleWpnCat" : "#GlobMobRuleArmCat";
        String filter = isWeapon ? mobRuleWpnCatFilter : mobRuleArmCatFilter;
        int page = isWeapon ? mobRuleWpnCatPage : mobRuleArmCatPage;
        List<String> filtered = isWeapon ? mobRuleWpnCatFiltered : mobRuleArmCatFiltered;

        rebuildMobRuleLinkedFiltered(allEntries, filter, filtered);

        int totalItems = filtered.size();
        int totalPages = Math.max(1, (totalItems + MOB_RULE_LINKED_PAGE_SIZE - 1) / MOB_RULE_LINKED_PAGE_SIZE);
        if (page >= totalPages) {
            page = totalPages - 1;
            if (isWeapon) mobRuleWpnCatPage = page; else mobRuleArmCatPage = page;
        }
        int pageStart = page * MOB_RULE_LINKED_PAGE_SIZE;

        boolean empty = allEntries.isEmpty();
        c.set(prefix + "Empty.Visible", empty);

        for (int i = 0; i < MOB_RULE_LINKED_PAGE_SIZE; i++) {
            int absIdx = pageStart + i;
            boolean vis = absIdx < totalItems;
            c.set(prefix + i + ".Visible", vis);
            if (vis) {
                String entry = filtered.get(absIdx);
                boolean isCat = MobRuleCategoryHelpers.isCategoryKey(entry);
                String display = isCat ? "[Category] " + MobRuleCategoryHelpers.fromCategoryKey(entry) : entry;
                c.set(prefix + "NameCat" + i + ".Visible", isCat);
                c.set(prefix + "NameItm" + i + ".Visible", !isCat);
                if (isCat) {
                    c.set(prefix + "NameCat" + i + ".Text", display);
                } else {
                    c.set(prefix + "NameItm" + i + ".Text", display);
                }
                c.set(prefix + "Peek" + i + ".Visible", isCat);
            }
        }

        boolean showPagination = totalPages > 1;
        c.set(prefix + "Pagination.Visible", showPagination);
        if (showPagination) {
            c.set(prefix + "PageInfo.Text", (page + 1) + " / " + totalPages);
        }
    }

    private void handleAbilMobPeek(int slotIdx) {
        if (abilityExpandedIndex < 0 || abilityExpandedIndex >= abilTreeFiltered.size()) return;
        int absIdx = abilityMobPage * ABIL_MOB_PAGE_SIZE + slotIdx;
        if (absIdx < 0 || absIdx >= abilityMobFiltered.size()) return;
        String mobKey = abilityMobFiltered.get(absIdx);
        if (!MobRuleCategoryHelpers.isCategoryKey(mobKey)) return;
        openMobRuleCategoryPeek(mobKey, CatPeekSource.ABILITY_LINKED);
    }

    private void populateAbilityConfig(UICommandBuilder c, String abilId) {
        RPGMobsConfig.AbilityConfig ac = editAbilityConfigs.get(abilId);
        c.set("#AbilCfgSection.Visible", ac != null);
        c.set("#AbilCfgChargeLeap.Visible", ac instanceof RPGMobsConfig.ChargeLeapAbilityConfig);
        c.set("#AbilCfgHealLeap.Visible", ac instanceof RPGMobsConfig.HealLeapAbilityConfig);
        c.set("#AbilCfgSummon.Visible", ac instanceof RPGMobsConfig.SummonAbilityConfig);
        c.set("#AbilCfgDodgeRoll.Visible", ac instanceof RPGMobsConfig.DodgeRollAbilityConfig);
        c.set("#AbilCfgMultiSlash.Visible", ac instanceof RPGMobsConfig.MultiSlashAbilityConfig);
        c.set("#AbilCfgEnrage.Visible", ac instanceof RPGMobsConfig.EnrageAbilityConfig);
        c.set("#AbilCfgVolley.Visible", ac instanceof RPGMobsConfig.VolleyAbilityConfig);
        if (ac == null) return;

        List<String> gateCats = ac.gate != null ? ac.gate.allowedWeaponCategories : List.of();
        int gateTotal = gateCats.size();
        int gateTotalPages = Math.max(1, (gateTotal + ABIL_CFG_LIST_PAGE_SIZE - 1) / ABIL_CFG_LIST_PAGE_SIZE);
        if (abilGatePage >= gateTotalPages) abilGatePage = gateTotalPages - 1;
        int gateStart = abilGatePage * ABIL_CFG_LIST_PAGE_SIZE;
        for (int i = 0; i < ABIL_CFG_LIST_PAGE_SIZE; i++) {
            int itemIndex = gateStart + i;
            boolean vis = itemIndex < gateTotal;
            c.set("#AbilCfgGateRow" + i + ".Visible", vis);
            if (vis) {
                String raw = gateCats.get(itemIndex);
                String display = MobRuleCategoryHelpers.isCategoryKey(raw)
                        ? MobRuleCategoryHelpers.fromCategoryKey(raw) : raw;
                c.set("#AbilCfgGateName" + i + ".Text", display);
            }
        }
        boolean gateMultiPage = gateTotalPages > 1;
        c.set("#AbilCfgGatePagination.Visible", gateMultiPage);
        if (gateMultiPage) {
            c.set("#AbilCfgGatePageInfo.Text", (abilGatePage + 1) + "/" + gateTotalPages);
            c.set("#AbilCfgGateFirstPage.Visible", abilGatePage > 0);
            c.set("#AbilCfgGatePrevPage.Visible", abilGatePage > 0);
            c.set("#AbilCfgGateNextPage.Visible", abilGatePage < gateTotalPages - 1);
            c.set("#AbilCfgGateLastPage.Visible", abilGatePage < gateTotalPages - 1);
        }

        List<String> exclKeys = ac.excludeLinkedMobRuleKeys != null ? ac.excludeLinkedMobRuleKeys : List.of();
        int exclTotal = exclKeys.size();
        int exclTotalPages = Math.max(1, (exclTotal + ABIL_CFG_LIST_PAGE_SIZE - 1) / ABIL_CFG_LIST_PAGE_SIZE);
        if (abilExclPage >= exclTotalPages) abilExclPage = exclTotalPages - 1;
        int exclStart = abilExclPage * ABIL_CFG_LIST_PAGE_SIZE;
        for (int i = 0; i < ABIL_CFG_LIST_PAGE_SIZE; i++) {
            int itemIndex = exclStart + i;
            boolean vis = itemIndex < exclTotal;
            c.set("#AbilCfgExclRow" + i + ".Visible", vis);
            if (vis) c.set("#AbilCfgExclName" + i + ".Text", exclKeys.get(itemIndex));
        }
        boolean exclMultiPage = exclTotalPages > 1;
        c.set("#AbilCfgExclPagination.Visible", exclMultiPage);
        if (exclMultiPage) {
            c.set("#AbilCfgExclPageInfo.Text", (abilExclPage + 1) + "/" + exclTotalPages);
            c.set("#AbilCfgExclFirstPage.Visible", abilExclPage > 0);
            c.set("#AbilCfgExclPrevPage.Visible", abilExclPage > 0);
            c.set("#AbilCfgExclNextPage.Visible", abilExclPage < exclTotalPages - 1);
            c.set("#AbilCfgExclLastPage.Visible", abilExclPage < exclTotalPages - 1);
        }

        if (ac instanceof RPGMobsConfig.ChargeLeapAbilityConfig cl && needsFieldRefresh) {
            c.set("#AbilCfgCLMinRange.Value", fmtFloat(cl.minRange));
            c.set("#AbilCfgCLMaxRange.Value", fmtFloat(cl.maxRange));
            renderToggle(c, "#AbilCfgCLFaceTarget", cl.faceTarget);
            for (int t = 0; t < 5; t++) {
                c.set("#AbilCfgCLChance" + t + ".Value", fmtFloat(ac.chancePerTier[t]));
                c.set("#AbilCfgCLCooldown" + t + ".Value", fmtFloat(ac.cooldownSecondsPerTier[t]));
                c.set("#AbilCfgCLSlamRange" + t + ".Value", fmtFloat(cl.slamRangePerTier[t]));
                c.set("#AbilCfgCLSlamDmg" + t + ".Value", String.valueOf(cl.slamBaseDamagePerTier[t]));
                c.set("#AbilCfgCLForce" + t + ".Value", fmtFloat(cl.applyForcePerTier[t]));
                c.set("#AbilCfgCLKBLift" + t + ".Value", fmtFloat(cl.knockbackLiftPerTier[t]));
                c.set("#AbilCfgCLKBPush" + t + ".Value", fmtFloat(cl.knockbackPushAwayPerTier[t]));
                c.set("#AbilCfgCLKBForce" + t + ".Value", fmtFloat(cl.knockbackForcePerTier[t]));
            }
        }

        if (ac instanceof RPGMobsConfig.HealLeapAbilityConfig hl && needsFieldRefresh) {
            c.set("#AbilCfgHLMinHealth.Value", fmtFloat(hl.minHealthTriggerPercent));
            c.set("#AbilCfgHLMaxHealth.Value", fmtFloat(hl.maxHealthTriggerPercent));
            c.set("#AbilCfgHLInstantChance.Value", fmtFloat(hl.instantHealChance));
            c.set("#AbilCfgHLDrinkDur.Value", fmtFloat(hl.npcDrinkDurationSeconds));
            c.set("#AbilCfgHLDrinkItemLabel.Text", hl.npcDrinkItemId != null ? hl.npcDrinkItemId : "");
            c.set("#AbilCfgHLInterrupt.Value", String.valueOf(hl.interruptHitCount));
            for (int t = 0; t < 5; t++) {
                c.set("#AbilCfgHLChance" + t + ".Value", fmtFloat(ac.chancePerTier[t]));
                c.set("#AbilCfgHLCooldown" + t + ".Value", fmtFloat(ac.cooldownSecondsPerTier[t]));
                c.set("#AbilCfgHLHeal" + t + ".Value", fmtFloat(hl.instantHealAmountPerTier[t]));
                c.set("#AbilCfgHLForce" + t + ".Value", fmtFloat(hl.applyForcePerTier[t]));
            }
        }

        if (ac instanceof RPGMobsConfig.SummonAbilityConfig sm) {
            if (needsFieldRefresh) {
                for (int t = 0; t < 5; t++) {
                    c.set("#AbilCfgSMChance" + t + ".Value", fmtFloat(ac.chancePerTier[t]));
                    c.set("#AbilCfgSMCooldown" + t + ".Value", fmtFloat(ac.cooldownSecondsPerTier[t]));
                }
                c.set("#AbilCfgSMMaxMinions.Value", String.valueOf(sm.maxAliveMinionsPerSummoner));
                c.set("#AbilCfgSUMinCount.Value", String.valueOf(sm.summonMinCount));
                c.set("#AbilCfgSUMaxCount.Value", String.valueOf(sm.summonMaxCount));
                c.set("#AbilCfgSURadius.Value", fmtDouble(sm.summonSpawnRadius));
                c.set("#AbilCfgSUMinionMin.Value", String.valueOf(sm.minionMinTier + 1));
                c.set("#AbilCfgSUMinionMax.Value", String.valueOf(sm.minionMaxTier + 1));
                c.set("#AbilCfgSMSkelW.Value", fmtDouble(sm.skeletonArcherWeight));
                c.set("#AbilCfgSMZombW.Value", fmtDouble(sm.zombieWeight));
                c.set("#AbilCfgSMWraithW.Value", fmtDouble(sm.wraithWeight));
                c.set("#AbilCfgSMAbrrW.Value", fmtDouble(sm.aberrantWeight));
            }

            List<String> roles = sm.roleIdentifiers != null ? sm.roleIdentifiers : List.of();
            int roleTotal = roles.size();
            int roleTotalPages = Math.max(1, (roleTotal + ABIL_CFG_LIST_PAGE_SIZE - 1) / ABIL_CFG_LIST_PAGE_SIZE);
            if (abilSummonRolePage >= roleTotalPages) abilSummonRolePage = roleTotalPages - 1;
            int roleStart = abilSummonRolePage * ABIL_CFG_LIST_PAGE_SIZE;
            for (int i = 0; i < ABIL_CFG_LIST_PAGE_SIZE; i++) {
                int itemIndex = roleStart + i;
                boolean vis = itemIndex < roleTotal;
                c.set("#AbilCfgSMRoleRow" + i + ".Visible", vis);
                if (vis) c.set("#AbilCfgSMRoleName" + i + ".Text", roles.get(itemIndex));
            }
            boolean roleMultiPage = roleTotalPages > 1;
            c.set("#AbilCfgSMRolePagination.Visible", roleMultiPage);
            if (roleMultiPage) {
                c.set("#AbilCfgSMRolePageInfo.Text", (abilSummonRolePage + 1) + "/" + roleTotalPages);
                c.set("#AbilCfgSMRoleFirstPage.Visible", abilSummonRolePage > 0);
                c.set("#AbilCfgSMRolePrevPage.Visible", abilSummonRolePage > 0);
                c.set("#AbilCfgSMRoleNextPage.Visible", abilSummonRolePage < roleTotalPages - 1);
                c.set("#AbilCfgSMRoleLastPage.Visible", abilSummonRolePage < roleTotalPages - 1);
            }

            List<String> exclPool = sm.excludeFromSummonPool != null ? sm.excludeFromSummonPool : List.of();
            int epTotal = exclPool.size();
            int epTotalPages = Math.max(1, (epTotal + ABIL_CFG_LIST_PAGE_SIZE - 1) / ABIL_CFG_LIST_PAGE_SIZE);
            if (abilSummonExclPage >= epTotalPages) abilSummonExclPage = epTotalPages - 1;
            int epStart = abilSummonExclPage * ABIL_CFG_LIST_PAGE_SIZE;
            for (int i = 0; i < ABIL_CFG_LIST_PAGE_SIZE; i++) {
                int itemIndex = epStart + i;
                boolean vis = itemIndex < epTotal;
                c.set("#AbilCfgSMExclRow" + i + ".Visible", vis);
                if (vis) c.set("#AbilCfgSMExclName" + i + ".Text", exclPool.get(itemIndex));
            }
            boolean epMultiPage = epTotalPages > 1;
            c.set("#AbilCfgSMExclPagination.Visible", epMultiPage);
            if (epMultiPage) {
                c.set("#AbilCfgSMExclPageInfo.Text", (abilSummonExclPage + 1) + "/" + epTotalPages);
                c.set("#AbilCfgSMExclFirstPage.Visible", abilSummonExclPage > 0);
                c.set("#AbilCfgSMExclPrevPage.Visible", abilSummonExclPage > 0);
                c.set("#AbilCfgSMExclNextPage.Visible", abilSummonExclPage < epTotalPages - 1);
                c.set("#AbilCfgSMExclLastPage.Visible", abilSummonExclPage < epTotalPages - 1);
            }
        }

        if (ac instanceof RPGMobsConfig.DodgeRollAbilityConfig dr && needsFieldRefresh) {
            c.set("#AbilCfgDRDodgeForce.Value", fmtFloat(dr.dodgeForce));
            c.set("#AbilCfgDRInvulnDur.Value", fmtFloat(dr.invulnerabilityDuration));
            c.set("#AbilCfgDRChargedMult.Value", fmtFloat(dr.chargedAttackDodgeMultiplier));
            for (int t = 0; t < 5; t++) {
                c.set("#AbilCfgDRChance" + t + ".Value", fmtFloat(ac.chancePerTier[t]));
                c.set("#AbilCfgDRCooldown" + t + ".Value", fmtFloat(ac.cooldownSecondsPerTier[t]));
                c.set("#AbilCfgDRDodgeChance" + t + ".Value", fmtFloat(dr.dodgeChancePerTier[t]));
            }
        }

        if (ac instanceof RPGMobsConfig.MultiSlashAbilityConfig ms) {
            if (multiSlashVariantIndex < 0 || multiSlashVariantIndex >= MS_VARIANT_KEYS.length) {
                multiSlashVariantIndex = 0;
            }
            for (int v = 0; v < MS_VARIANT_KEYS.length; v++) {
                boolean active = v == multiSlashVariantIndex;
                c.set("#AbilCfgMSVarBtn" + v + ".Visible", !active);
                c.set("#AbilCfgMSVarBtnActive" + v + ".Visible", active);
            }
            String variantKey = MS_VARIANT_KEYS[multiSlashVariantIndex];
            c.set("#AbilCfgMSVarLabel.Text", MS_VARIANT_LABELS[multiSlashVariantIndex] + " Variant");
            RPGMobsConfig.MultiSlashVariantConfig vc = ms.variantConfigs != null
                    ? ms.variantConfigs.get(variantKey) : null;
            if (vc == null) vc = ms.getVariantOrDefault(variantKey);
            if (needsFieldRefresh) {
                c.set("#AbilCfgMSMeleeRange.Value", fmtFloat(vc.meleeRange));
                for (int t = 0; t < 5; t++) {
                    c.set("#AbilCfgMSChance" + t + ".Value", fmtFloat(ac.chancePerTier[t]));
                    c.set("#AbilCfgMSCooldown" + t + ".Value", fmtFloat(vc.cooldownSecondsPerTier[t]));
                    c.set("#AbilCfgMSTrigger" + t + ".Value", fmtFloat(vc.slashTriggerChancePerTier[t]));
                    c.set("#AbilCfgMSDamage" + t + ".Value", String.valueOf(vc.baseDamagePerHitPerTier[t]));
                    c.set("#AbilCfgMSDrift" + t + ".Value", fmtFloat(vc.forwardDriftForcePerTier[t]));
                    c.set("#AbilCfgMSKB" + t + ".Value", fmtFloat(vc.knockbackForcePerTier[t]));
                }
            }
        }

        if (ac instanceof RPGMobsConfig.EnrageAbilityConfig en && needsFieldRefresh) {
            for (int t = 0; t < 5; t++) {
                c.set("#AbilCfgENChance" + t + ".Value", fmtFloat(ac.chancePerTier[t]));
                c.set("#AbilCfgENCooldown" + t + ".Value", fmtFloat(ac.cooldownSecondsPerTier[t]));
                c.set("#AbilCfgENHealthPct" + t + ".Value", fmtFloat(en.triggerHealthPercentPerTier[t]));
                c.set("#AbilCfgENDmgMult" + t + ".Value", fmtFloat(en.lightPunchDamagePerTier[t]));
                c.set("#AbilCfgENSpdMult" + t + ".Value", fmtFloat(en.heavyPunchDamagePerTier[t]));
            }
        }

        if (ac instanceof RPGMobsConfig.VolleyAbilityConfig vl && needsFieldRefresh) {
            c.set("#AbilCfgVLMinRange.Value", fmtFloat(vl.minRange));
            c.set("#AbilCfgVLMaxRange.Value", fmtFloat(vl.maxRange));
            for (int t = 0; t < 5; t++) {
                c.set("#AbilCfgVLChance" + t + ".Value", fmtFloat(ac.chancePerTier[t]));
                c.set("#AbilCfgVLCooldown" + t + ".Value", fmtFloat(ac.cooldownSecondsPerTier[t]));
                c.set("#AbilCfgVLTrigger" + t + ".Value", fmtFloat(vl.volleyTriggerChancePerTier[t]));
                c.set("#AbilCfgVLProjectiles" + t + ".Value", String.valueOf(vl.projectileCountPerTier[t]));
                c.set("#AbilCfgVLSpread" + t + ".Value", fmtFloat(vl.spreadAnglePerTier[t]));
                c.set("#AbilCfgVLDamage" + t + ".Value", String.valueOf(vl.baseDamagePerProjectilePerTier[t]));
            }
        }
    }

    private void processAbilCfgFields(AdminUIData data) {
        if (abilityExpandedIndex < 0 || abilityExpandedIndex >= abilTreeFiltered.size()) return;
        String abilId = abilTreeFiltered.get(abilityExpandedIndex);
        RPGMobsConfig.AbilityConfig ac = editAbilityConfigs.get(abilId);
        switch (ac) {
            case RPGMobsConfig.ChargeLeapAbilityConfig cl -> {
                cl.minRange = parseFloatField(data.abilCfgCLMinRange, cl.minRange);
                cl.maxRange = parseFloatField(data.abilCfgCLMaxRange, cl.maxRange);
                for (int t = 0; t < 5; t++) {
                    ac.chancePerTier[t] = parseFloatField(data.abilCfgCLChance[t], ac.chancePerTier[t]);
                    ac.cooldownSecondsPerTier[t] = parseFloatField(data.abilCfgCLCooldown[t],
                                                                   ac.cooldownSecondsPerTier[t]
                    );
                    cl.slamRangePerTier[t] = parseFloatField(data.abilCfgCLSlamRange[t], cl.slamRangePerTier[t]);
                    cl.slamBaseDamagePerTier[t] = parseIntField(data.abilCfgCLSlamDmg[t], cl.slamBaseDamagePerTier[t]);
                    cl.applyForcePerTier[t] = parseFloatField(data.abilCfgCLForce[t], cl.applyForcePerTier[t]);
                    cl.knockbackLiftPerTier[t] = parseFloatField(data.abilCfgCLKBLift[t], cl.knockbackLiftPerTier[t]);
                    cl.knockbackPushAwayPerTier[t] = parseFloatField(data.abilCfgCLKBPush[t],
                                                                     cl.knockbackPushAwayPerTier[t]
                    );
                    cl.knockbackForcePerTier[t] = parseFloatField(data.abilCfgCLKBForce[t],
                                                                  cl.knockbackForcePerTier[t]
                    );
                }
            }
            case RPGMobsConfig.HealLeapAbilityConfig hl -> {
                hl.minHealthTriggerPercent = parseFloatField(data.abilCfgHLMinHealth, hl.minHealthTriggerPercent);
                hl.maxHealthTriggerPercent = parseFloatField(data.abilCfgHLMaxHealth, hl.maxHealthTriggerPercent);
                hl.instantHealChance = parseFloatField(data.abilCfgHLInstantChance, hl.instantHealChance);
                hl.npcDrinkDurationSeconds = parseFloatField(data.abilCfgHLDrinkDur, hl.npcDrinkDurationSeconds);
                hl.interruptHitCount = parseIntField(data.abilCfgHLInterrupt, hl.interruptHitCount);
                for (int t = 0; t < 5; t++) {
                    ac.chancePerTier[t] = parseFloatField(data.abilCfgHLChance[t], ac.chancePerTier[t]);
                    ac.cooldownSecondsPerTier[t] = parseFloatField(data.abilCfgHLCooldown[t],
                                                                   ac.cooldownSecondsPerTier[t]
                    );
                    hl.instantHealAmountPerTier[t] = parseFloatField(data.abilCfgHLHeal[t],
                                                                     hl.instantHealAmountPerTier[t]
                    );
                    hl.applyForcePerTier[t] = parseFloatField(data.abilCfgHLForce[t], hl.applyForcePerTier[t]);
                }
            }
            case RPGMobsConfig.SummonAbilityConfig sm -> {
                for (int t = 0; t < 5; t++) {
                    ac.chancePerTier[t] = parseFloatField(data.abilCfgSMChance[t], ac.chancePerTier[t]);
                    ac.cooldownSecondsPerTier[t] = parseFloatField(data.abilCfgSMCooldown[t],
                                                                   ac.cooldownSecondsPerTier[t]
                    );
                }
                sm.maxAliveMinionsPerSummoner = parseIntField(data.abilCfgSMMaxMinions, sm.maxAliveMinionsPerSummoner);
                sm.skeletonArcherWeight = parseDoubleField(data.abilCfgSMSkelW, sm.skeletonArcherWeight);
                sm.zombieWeight = parseDoubleField(data.abilCfgSMZombW, sm.zombieWeight);
                sm.wraithWeight = parseDoubleField(data.abilCfgSMWraithW, sm.wraithWeight);
                sm.aberrantWeight = parseDoubleField(data.abilCfgSMAbrrW, sm.aberrantWeight);
                sm.summonMinCount = parseIntField(data.abilCfgSUMinCount, sm.summonMinCount);
                sm.summonMaxCount = parseIntField(data.abilCfgSUMaxCount, sm.summonMaxCount);
                sm.summonSpawnRadius = parseDoubleField(data.abilCfgSURadius, sm.summonSpawnRadius);
                var minionMinRaw = parseIntField(data.abilCfgSUMinionMin, sm.minionMinTier + 1);
                sm.minionMinTier = minionMinRaw - 1;
                var minionMaxRaw = parseIntField(data.abilCfgSUMinionMax, sm.minionMaxTier + 1);
                sm.minionMaxTier = minionMaxRaw - 1;
            }
            case RPGMobsConfig.DodgeRollAbilityConfig dr -> {
                dr.dodgeForce = parseFloatField(data.abilCfgDRDodgeForce, dr.dodgeForce);
                dr.invulnerabilityDuration = parseFloatField(data.abilCfgDRInvulnDur, dr.invulnerabilityDuration);
                dr.chargedAttackDodgeMultiplier = parseFloatField(data.abilCfgDRChargedMult, dr.chargedAttackDodgeMultiplier);
                for (int t = 0; t < 5; t++) {
                    ac.chancePerTier[t] = parseFloatField(data.abilCfgDRChance[t], ac.chancePerTier[t]);
                    ac.cooldownSecondsPerTier[t] = parseFloatField(data.abilCfgDRCooldown[t],
                                                                   ac.cooldownSecondsPerTier[t]);
                    dr.dodgeChancePerTier[t] = parseFloatField(data.abilCfgDRDodgeChance[t], dr.dodgeChancePerTier[t]);
                }
            }
            case RPGMobsConfig.MultiSlashAbilityConfig ms -> {
                if (multiSlashVariantIndex >= 0 && multiSlashVariantIndex < MS_VARIANT_KEYS.length) {
                    String variantKey = MS_VARIANT_KEYS[multiSlashVariantIndex];
                    if (ms.variantConfigs == null) ms.variantConfigs = new LinkedHashMap<>();
                    RPGMobsConfig.MultiSlashVariantConfig vc = ms.variantConfigs.get(variantKey);
                    if (vc == null) {
                        vc = ms.getVariantOrDefault(variantKey);
                        ms.variantConfigs.put(variantKey, vc);
                    }
                    vc.meleeRange = parseFloatField(data.abilCfgMSMeleeRange, vc.meleeRange);
                    for (int t = 0; t < 5; t++) {
                        ac.chancePerTier[t] = parseFloatField(data.abilCfgMSChance[t], ac.chancePerTier[t]);
                        vc.cooldownSecondsPerTier[t] = parseFloatField(data.abilCfgMSCooldown[t],
                                                                       vc.cooldownSecondsPerTier[t]);
                        vc.slashTriggerChancePerTier[t] = parseFloatField(data.abilCfgMSTrigger[t],
                                                                          vc.slashTriggerChancePerTier[t]);
                        vc.baseDamagePerHitPerTier[t] = parseIntField(data.abilCfgMSDamage[t],
                                                                       vc.baseDamagePerHitPerTier[t]);
                        vc.forwardDriftForcePerTier[t] = parseFloatField(data.abilCfgMSDrift[t],
                                                                         vc.forwardDriftForcePerTier[t]);
                        vc.knockbackForcePerTier[t] = parseFloatField(data.abilCfgMSKB[t], vc.knockbackForcePerTier[t]);
                    }
                }
            }
            case RPGMobsConfig.EnrageAbilityConfig en -> {
                for (int t = 0; t < 5; t++) {
                    ac.chancePerTier[t] = parseFloatField(data.abilCfgENChance[t], ac.chancePerTier[t]);
                    ac.cooldownSecondsPerTier[t] = parseFloatField(data.abilCfgENCooldown[t],
                                                                   ac.cooldownSecondsPerTier[t]);
                    en.triggerHealthPercentPerTier[t] = parseFloatField(data.abilCfgENHealthPct[t],
                                                                        en.triggerHealthPercentPerTier[t]);
                    en.lightPunchDamagePerTier[t] = parseFloatField(data.abilCfgENDmgMult[t],
                                                                    en.lightPunchDamagePerTier[t]);
                    en.heavyPunchDamagePerTier[t] = parseFloatField(data.abilCfgENSpdMult[t],
                                                                     en.heavyPunchDamagePerTier[t]);
                }
            }
            case RPGMobsConfig.VolleyAbilityConfig vl -> {
                vl.minRange = parseFloatField(data.abilCfgVLMinRange, vl.minRange);
                vl.maxRange = parseFloatField(data.abilCfgVLMaxRange, vl.maxRange);
                for (int t = 0; t < 5; t++) {
                    ac.chancePerTier[t] = parseFloatField(data.abilCfgVLChance[t], ac.chancePerTier[t]);
                    ac.cooldownSecondsPerTier[t] = parseFloatField(data.abilCfgVLCooldown[t],
                                                                   ac.cooldownSecondsPerTier[t]);
                    vl.volleyTriggerChancePerTier[t] = parseFloatField(data.abilCfgVLTrigger[t],
                                                                       vl.volleyTriggerChancePerTier[t]);
                    vl.projectileCountPerTier[t] = parseIntField(data.abilCfgVLProjectiles[t],
                                                                  vl.projectileCountPerTier[t]);
                    vl.spreadAnglePerTier[t] = parseFloatField(data.abilCfgVLSpread[t], vl.spreadAnglePerTier[t]);
                    vl.baseDamagePerProjectilePerTier[t] = parseIntField(data.abilCfgVLDamage[t],
                                                                         vl.baseDamagePerProjectilePerTier[t]);
                }
            }
            case null, default -> {
            }
        }

    }

    private RPGMobsConfig.@Nullable AbilityConfig getExpandedAbilityConfig() {
        if (abilityExpandedIndex < 0 || abilityExpandedIndex >= abilTreeFiltered.size()) return null;
        return editAbilityConfigs.get(abilTreeFiltered.get(abilityExpandedIndex));
    }

    private void handleAbilCfgGateAdd() {
        if (getExpandedAbilityConfig() == null) return;
        linkPopupMode = LinkPopupMode.ABILITY_WEAPON_GATE;
        linkPopupOpen = true;
        linkPopupNavHistory.clear();
        linkPopupGearNavHistory.clear();
        linkPopupGearCurrentCategory = editWeaponCategoryTree;
        linkPopupSelectedCategory = null;
        rebuildLinkPopupItems();
        needsFieldRefresh = true;
    }

    private void handleAbilCfgGateDel(int slotIdx) {
        RPGMobsConfig.AbilityConfig ac = getExpandedAbilityConfig();
        if (ac == null || ac.gate == null) return;
        int absIdx = abilGatePage * ABIL_CFG_LIST_PAGE_SIZE + slotIdx;
        if (absIdx < 0 || absIdx >= ac.gate.allowedWeaponCategories.size()) return;
        if (!(ac.gate.allowedWeaponCategories instanceof ArrayList)) {
            ac.gate.allowedWeaponCategories = new ArrayList<>(ac.gate.allowedWeaponCategories);
        }
        ac.gate.allowedWeaponCategories.remove(absIdx);
        needsFieldRefresh = true;
    }

    private void handleAbilCfgGatePeek(int slotIdx) {
        RPGMobsConfig.AbilityConfig ac = getExpandedAbilityConfig();
        if (ac == null || ac.gate == null) return;
        int absIdx = abilGatePage * ABIL_CFG_LIST_PAGE_SIZE + slotIdx;
        if (absIdx < 0 || absIdx >= ac.gate.allowedWeaponCategories.size()) return;
        String catKey = ac.gate.allowedWeaponCategories.get(absIdx);
        String catName = MobRuleCategoryHelpers.isCategoryKey(catKey)
                ? MobRuleCategoryHelpers.fromCategoryKey(catKey) : catKey;
        RPGMobsConfig.GearCategory gearCat = MobRuleCategoryHelpers.findGearCategoryByName(editWeaponCategoryTree, catName);
        if (gearCat != null) {
            var items = MobRuleCategoryHelpers.collectAllGearItemKeys(gearCat);
            openCategoryPeek("Weapon Category: " + catName, new ArrayList<>(items), CatPeekSource.ABILITY_WEAPON_GATE, catKey);
        }
    }

    private void handleAbilCfgExclAdd() {
        if (getExpandedAbilityConfig() == null) return;
        npcPickerMode = NpcPickerMode.ABILITY_EXCLUDED_MOB;
        npcPickerOpen = true;
        npcPickerFilter = "";
        npcPickerCustomId = "";
        npcPickerPage = 0;
        npcPickerSelectedItem = null;
        rebuildNpcPickerFiltered();
        needsFieldRefresh = true;
    }

    private void handleAbilCfgExclDel(int slotIdx) {
        RPGMobsConfig.AbilityConfig ac = getExpandedAbilityConfig();
        if (ac == null || ac.excludeLinkedMobRuleKeys == null) return;
        int absIdx = abilExclPage * ABIL_CFG_LIST_PAGE_SIZE + slotIdx;
        if (absIdx < 0 || absIdx >= ac.excludeLinkedMobRuleKeys.size()) return;
        ac.excludeLinkedMobRuleKeys.remove(absIdx);
        needsFieldRefresh = true;
    }

    private void handleAbilCfgCLFaceTargetToggle() {
        RPGMobsConfig.AbilityConfig ac = getExpandedAbilityConfig();
        if (ac instanceof RPGMobsConfig.ChargeLeapAbilityConfig cl) {
            cl.faceTarget = !cl.faceTarget;
            needsFieldRefresh = true;
        }
    }

    private void handleAbilCfgHLDrinkItemPick() {
        RPGMobsConfig.AbilityConfig ac = getExpandedAbilityConfig();
        if (!(ac instanceof RPGMobsConfig.HealLeapAbilityConfig)) return;
        itemPickerOpen = true;
        itemPickerDropSlot = ITEM_PICKER_ABILITY_DRINK;
        itemPickerFilter = "";
        itemPickerCustomId = "";
        itemPickerPage = 0;
        itemPickerSelectedItem = null;
        rebuildItemPickerFiltered();
        needsFieldRefresh = true;
    }

    private void handleAbilCfgSMRoleAdd() {
        renameTarget = RenameTarget.WEAPON_CATEGORY;
        renameRowIndex = RENAME_IDX_SUMMON_ROLE_ADD;
        renamePopupOpen = true;
        pendingRenameName = null;
        needsFieldRefresh = true;
    }

    private void handleAbilCfgSMRoleDel(int slotIdx) {
        RPGMobsConfig.AbilityConfig ac = getExpandedAbilityConfig();
        if (!(ac instanceof RPGMobsConfig.SummonAbilityConfig sm)) return;
        if (sm.roleIdentifiers == null) return;
        int absIdx = abilSummonRolePage * ABIL_CFG_LIST_PAGE_SIZE + slotIdx;
        if (absIdx < 0 || absIdx >= sm.roleIdentifiers.size()) return;
        sm.roleIdentifiers.remove(absIdx);
        needsFieldRefresh = true;
    }

    private void handleAbilCfgSMExclAdd() {
        if (!(getExpandedAbilityConfig() instanceof RPGMobsConfig.SummonAbilityConfig)) return;
        npcPickerMode = NpcPickerMode.SUMMON_EXCLUDE_FROM_POOL;
        npcPickerOpen = true;
        npcPickerFilter = "";
        npcPickerCustomId = "";
        npcPickerPage = 0;
        npcPickerSelectedItem = null;
        rebuildNpcPickerFiltered();
        needsFieldRefresh = true;
    }

    private void handleAbilCfgSMExclDel(int slotIdx) {
        RPGMobsConfig.AbilityConfig ac = getExpandedAbilityConfig();
        if (!(ac instanceof RPGMobsConfig.SummonAbilityConfig sm)) return;
        if (sm.excludeFromSummonPool == null) return;
        int absIdx = abilSummonExclPage * ABIL_CFG_LIST_PAGE_SIZE + slotIdx;
        if (absIdx < 0 || absIdx >= sm.excludeFromSummonPool.size()) return;
        sm.excludeFromSummonPool.remove(absIdx);
        needsFieldRefresh = true;
    }

    private void addSummonRoleIdentifier(String identifier) {
        RPGMobsConfig.AbilityConfig ac = getExpandedAbilityConfig();
        if (!(ac instanceof RPGMobsConfig.SummonAbilityConfig sm)) return;
        if (sm.roleIdentifiers == null) sm.roleIdentifiers = new ArrayList<>();
        if (!(sm.roleIdentifiers instanceof ArrayList)) sm.roleIdentifiers = new ArrayList<>(sm.roleIdentifiers);
        if (!sm.roleIdentifiers.contains(identifier)) {
            sm.roleIdentifiers.add(identifier);
        }
        needsFieldRefresh = true;
    }

    private void handleLootTplMobPeek(int slotIdx) {
        int absIdx = lootTplMobPage * LOOT_TPL_MOB_PAGE_SIZE + slotIdx;
        if (absIdx < 0 || absIdx >= lootTplMobFiltered.size()) return;
        String mobKey = lootTplMobFiltered.get(absIdx);
        if (!MobRuleCategoryHelpers.isCategoryKey(mobKey)) return;
        openMobRuleCategoryPeek(mobKey, CatPeekSource.LOOT_TPL_LINKED);
    }

    private void handleTierOvrPeek(int slotIdx) {
        List<Integer> visibleIndices = buildTierOverrideVisibleIndices();
        int globalIdx = tierOverridePage * AdminUIData.TIER_OVERRIDE_PAGE_SIZE + slotIdx;
        if (globalIdx < 0 || globalIdx >= visibleIndices.size()) return;
        int ruleIdx = visibleIndices.get(globalIdx);
        String key = activeTierOverrideKeys.get(ruleIdx);
        if (!MobRuleCategoryHelpers.isCategoryKey(key)) return;
        openMobRuleCategoryPeek(key, CatPeekSource.TIER_OVERRIDE);
    }

    private void openMobRuleCategoryPeek(String categoryKey, CatPeekSource source) {
        String catName = MobRuleCategoryHelpers.fromCategoryKey(categoryKey);
        RPGMobsConfig.MobRuleCategory tree = getMobRuleCategoryRoot();
        RPGMobsConfig.MobRuleCategory cat = MobRuleCategoryHelpers.findCategoryByName(tree, catName);
        List<String> items = cat != null ? MobRuleCategoryHelpers.collectAllMobRuleKeys(cat) : List.of();
        openCategoryPeek("Mob Category: " + catName, items, source, categoryKey);
    }

    private void openCategoryPeek(String title, List<String> items, CatPeekSource source, String categoryKey) {
        categoryPeekOpen = true;
        categoryPeekTitle = title;
        categoryPeekItems = new ArrayList<>(items);
        categoryPeekPage = 0;
        categoryPeekSource = source;
        categoryPeekCategoryKey = categoryKey;
    }

    private void handleCatPeekExtract() {
        if (categoryPeekItems.isEmpty() || categoryPeekCategoryKey.isEmpty()) return;
        List<String> items = new ArrayList<>(categoryPeekItems);
        String catKey = categoryPeekCategoryKey;

        switch (categoryPeekSource) {
            case WEAPON_CAT -> {
                RPGMobsConfig.MobRule rule = getExpandedGlobMobRule();
                if (rule == null) break;
                if (!(rule.allowedWeaponCategories instanceof ArrayList)) {
                    rule.allowedWeaponCategories = new ArrayList<>(rule.allowedWeaponCategories);
                }
                rule.allowedWeaponCategories.remove(catKey);
                for (String item : items) {
                    if (!rule.allowedWeaponCategories.contains(item)) {
                        rule.allowedWeaponCategories.add(item);
                    }
                }
            }
            case ARMOR_CAT -> {
                RPGMobsConfig.MobRule rule = getExpandedGlobMobRule();
                if (rule == null) break;
                if (!(rule.allowedArmorCategories instanceof ArrayList)) {
                    rule.allowedArmorCategories = new ArrayList<>(rule.allowedArmorCategories);
                }
                rule.allowedArmorCategories.remove(catKey);
                for (String item : items) {
                    if (!rule.allowedArmorCategories.contains(item)) {
                        rule.allowedArmorCategories.add(item);
                    }
                }
            }
            case ABILITY_LINKED -> {
                if (editOverlay == null || abilityExpandedIndex < 0 || abilityExpandedIndex >= abilTreeFiltered.size()) break;
                String abilId = abilTreeFiltered.get(abilityExpandedIndex);
                ensureAbilityOverlayWithLinkedEntries(abilId);
                var ao = editOverlay.abilityOverlays.get(abilId);
                if (ao == null || ao.linkedEntries == null) break;
                boolean[] tiers = null;
                var iter = ao.linkedEntries.iterator();
                while (iter.hasNext()) {
                    var entry = iter.next();
                    if (entry.key.equals(catKey)) {
                        tiers = entry.enabledPerTier.clone();
                        iter.remove();
                        break;
                    }
                }
                if (tiers == null) tiers = new boolean[]{true, true, true, true, true};
                for (String item : items) {
                    boolean exists = ao.linkedEntries.stream().anyMatch(e -> e.key.equals(item));
                    if (!exists) {
                        ao.linkedEntries.add(new ConfigOverlay.AbilityLinkedEntry(item, tiers.clone()));
                    }
                }
                rebuildAbilMobFiltered();
            }
            case ABILITY_WEAPON_GATE -> {
                RPGMobsConfig.AbilityConfig ac = getExpandedAbilityConfig();
                if (ac == null || ac.gate == null) break;
                if (!(ac.gate.allowedWeaponCategories instanceof ArrayList)) {
                    ac.gate.allowedWeaponCategories = new ArrayList<>(ac.gate.allowedWeaponCategories);
                }
                ac.gate.allowedWeaponCategories.remove(catKey);
                for (String item : items) {
                    if (!ac.gate.allowedWeaponCategories.contains(item)) {
                        ac.gate.allowedWeaponCategories.add(item);
                    }
                }
            }
            case LOOT_TPL_LINKED -> {
                RPGMobsConfig.LootTemplate template = getExpandedLootTemplate();
                if (template == null) break;
                template.linkedMobRuleKeys.remove(catKey);
                for (String item : items) {
                    if (!template.linkedMobRuleKeys.contains(item)) {
                        template.linkedMobRuleKeys.add(item);
                    }
                }
                rebuildLootTplMobFiltered();
            }
            case TIER_OVERRIDE -> {
                if (editOverlay == null || editOverlay.tierOverrides == null) break;
                ConfigOverlay.TierOverride existing = editOverlay.tierOverrides.remove(MobRuleCategoryHelpers.fromCategoryKey(catKey));
                if (existing == null) break;
                int insertIdx = activeTierOverrideKeys.indexOf(catKey);
                if (insertIdx >= 0) activeTierOverrideKeys.remove(insertIdx);
                else insertIdx = activeTierOverrideKeys.size();
                for (String item : items) {
                    if (!activeTierOverrideKeys.contains(item)) {
                        var copy = new ConfigOverlay.TierOverride();
                        copy.allowedTiers = existing.allowedTiers.clone();
                        editOverlay.tierOverrides.put(item, copy);
                        activeTierOverrideKeys.add(insertIdx, item);
                        insertIdx++;
                    }
                }
            }
            default -> { }
        }
        categoryPeekOpen = false;
        needsFieldRefresh = true;
    }

    private static final List<String> ARMOR_SLOT_NAMES = List.of("Head", "Chest", "Hands", "Legs");

    private void toggleArmorSlotAll() {
        RPGMobsConfig.MobRule rule = getExpandedGlobMobRule();
        if (rule == null) return;
        boolean isCurrentlyAll = rule.allowedArmorSlots.isEmpty();
        if (isCurrentlyAll) {
            rule.allowedArmorSlots = new ArrayList<>(List.of("NONE"));
        } else {
            rule.allowedArmorSlots = new ArrayList<>();
        }
        needsFieldRefresh = true;
    }

    private void toggleArmorSlot(String slotName) {
        RPGMobsConfig.MobRule rule = getExpandedGlobMobRule();
        if (rule == null) return;
        var slots = new ArrayList<>(rule.allowedArmorSlots);
        slots.remove("NONE");
        if (slots.contains(slotName)) {
            slots.remove(slotName);
            if (slots.isEmpty()) {
                rule.allowedArmorSlots = new ArrayList<>(List.of("NONE"));
            } else {
                rule.allowedArmorSlots = slots;
            }
        } else {
            slots.add(slotName);
            if (slots.containsAll(ARMOR_SLOT_NAMES)) {
                rule.allowedArmorSlots = new ArrayList<>();
            } else {
                rule.allowedArmorSlots = slots;
            }
        }
        needsFieldRefresh = true;
    }

    private void processGlobMobRuleDetailFields(AdminUIData data) {
        RPGMobsConfig.MobRule rule = getExpandedGlobMobRule();
        if (rule == null) return;
        if (data.globMobRuleMatchExact != null) rule.matchExact = csvToList(data.globMobRuleMatchExact);
        if (data.globMobRuleMatchPrefix != null) rule.matchStartsWith = csvToList(data.globMobRuleMatchPrefix);
        if (data.globMobRuleMatchContains != null) rule.matchContains = csvToList(data.globMobRuleMatchContains);
        if (data.globMobRuleMatchExcludes != null) rule.matchExcludes = csvToList(data.globMobRuleMatchExcludes);
    }

    private void rebuildGearCatTreeFiltered(boolean isWeapon) {
        RPGMobsConfig.GearCategory root = isWeapon ? editWeaponCategoryTree : editArmorCategoryTree;
        String filter = isWeapon ? wpnCatTreeFilter : armCatTreeFilter;
        List<String> filtered = isWeapon ? wpnCatTreeFilteredKeys : armCatTreeFilteredKeys;
        filtered.clear();
        if (!filter.isEmpty()) {
            filtered.addAll(MobRuleCategoryHelpers.searchGearItemKeysRecursive(root, filter));
        }
    }

    private void handleGearCatRowClick(int rowIdx, boolean isWeapon) {
        List<TreeItem> items = isWeapon ? wpnCatTreeItems : armCatTreeItems;
        if (rowIdx < 0 || rowIdx >= items.size()) return;
        TreeItem item = items.get(rowIdx);
        if (item.isCategory) {
            RPGMobsConfig.GearCategory cat = ensureCurrentGearCat(isWeapon);
            for (RPGMobsConfig.GearCategory child : cat.children) {
                if (child.name.equals(item.name)) {
                    navigateToGearCat(child, isWeapon);
                    return;
                }
            }
        }
    }

    private void handleGearCatRowDelete(int rowIdx, boolean isWeapon) {
        List<TreeItem> items = isWeapon ? wpnCatTreeItems : armCatTreeItems;
        String filter = isWeapon ? wpnCatTreeFilter : armCatTreeFilter;
        if (rowIdx < 0 || rowIdx >= items.size()) return;
        TreeItem item = items.get(rowIdx);
        if (!filter.isEmpty()) {
            if (!item.isCategory) {
                RPGMobsConfig.GearCategory root = isWeapon ? editWeaponCategoryTree : editArmorCategoryTree;
                MobRuleCategoryHelpers.removeGearItemKeyRecursive(root, item.name);
                rebuildGearCatTreeFiltered(isWeapon);
            }
        } else {
            RPGMobsConfig.GearCategory cat = ensureCurrentGearCat(isWeapon);
            if (item.isCategory) {
                String oldName = item.name;
                cat.children.removeIf(child -> child.name.equals(oldName));
                propagateGearCategoryRename(oldName, null, isWeapon);
            } else {
                cat.itemKeys.remove(item.name);
            }
        }
    }

    private void handleGearCatRowRename(int rowIdx, boolean isWeapon) {
        List<TreeItem> items = isWeapon ? wpnCatTreeItems : armCatTreeItems;
        if (rowIdx < 0 || rowIdx >= items.size()) return;
        TreeItem item = items.get(rowIdx);
        renamePopupOpen = true;
        renameTarget = isWeapon ? RenameTarget.WEAPON_CATEGORY : RenameTarget.ARMOR_CATEGORY;
        renameRowIndex = rowIdx;
        pendingRenameName = item.name;
        needsFieldRefresh = true;
    }

    private void handleGearCatRowChoose(int rowIdx, boolean isWeapon) {
        List<TreeItem> items = isWeapon ? wpnCatTreeItems : armCatTreeItems;
        if (rowIdx < 0 || rowIdx >= items.size()) return;
        TreeItem item = items.get(rowIdx);
        if (item.isCategory) return;
        gearCatSwapOldKey = item.name;
        itemPickerOpen = true;
        itemPickerDropSlot = isWeapon ? ITEM_PICKER_GEAR_WEAPON_SWAP : ITEM_PICKER_GEAR_ARMOR_SWAP;
        itemPickerFilter = isWeapon ? "" : "Armor_";
        itemPickerCustomId = "";
        itemPickerPage = 0;
        itemPickerSelectedItem = null;
        rebuildItemPickerFiltered();
        needsFieldRefresh = true;
    }

    private void handleGearCatRowMove(int rowIdx, boolean isWeapon) {
        List<TreeItem> items = isWeapon ? wpnCatTreeItems : armCatTreeItems;
        if (rowIdx < 0 || rowIdx >= items.size()) return;
        TreeItem item = items.get(rowIdx);
        RPGMobsConfig.GearCategory root = isWeapon ? editWeaponCategoryTree : editArmorCategoryTree;
        movePopupOpen = true;
        moveSourceType = isWeapon ? MoveSourceType.GEAR_WEAPON : MoveSourceType.GEAR_ARMOR;
        moveRowIdx = rowIdx;
        movePopupSelectedCategory = null;
        movePopupNavHistory.clear();
        movePopupCurrentCategoryName = root.name;
        rebuildGearMovePopupCategories(isWeapon, item);
    }

    private void rebuildGearMovePopupCategories(boolean isWeapon, TreeItem movingItem) {
        RPGMobsConfig.GearCategory root = isWeapon ? editWeaponCategoryTree : editArmorCategoryTree;
        RPGMobsConfig.GearCategory navCat = MobRuleCategoryHelpers.findGearCategoryByName(root, movePopupCurrentCategoryName);
        if (navCat == null) navCat = root;
        movePopupCategoryNames.clear();
        Set<String> excluded = new HashSet<>();
        if (movingItem.isCategory) {
            RPGMobsConfig.GearCategory movingCat = MobRuleCategoryHelpers.findGearCategoryByName(root, movingItem.name);
            if (movingCat != null) {
                MobRuleCategoryHelpers.collectGearCategoryNames(movingCat, excluded);
            }
        }
        RPGMobsConfig.GearCategory current = ensureCurrentGearCat(isWeapon);
        String currentName = current.name;
        for (RPGMobsConfig.GearCategory child : navCat.children) {
            if (!excluded.contains(child.name) && !child.name.equals(currentName)) {
                movePopupCategoryNames.add(child.name);
            }
        }
    }

    private void addGearCategory(boolean isWeapon) {
        RPGMobsConfig.GearCategory cat = ensureCurrentGearCat(isWeapon);
        String baseName = "New Category";
        String name = baseName;
        int counter = 1;
        Set<String> existing = new HashSet<>();
        for (RPGMobsConfig.GearCategory child : cat.children) existing.add(child.name);
        while (existing.contains(name)) {
            name = baseName + " " + counter++;
        }
        var newCat = new RPGMobsConfig.GearCategory();
        newCat.name = name;
        cat.children.add(newCat);
        renamePopupOpen = true;
        renameTarget = isWeapon ? RenameTarget.WEAPON_CATEGORY : RenameTarget.ARMOR_CATEGORY;
        renameRowIndex = -1;
        pendingRenameName = name;
        needsFieldRefresh = true;
    }

    private void addGearItem(boolean isWeapon) {
        itemPickerOpen = true;
        itemPickerDropSlot = isWeapon ? ITEM_PICKER_GEAR_WEAPON_ADD : ITEM_PICKER_GEAR_ARMOR_ADD;
        itemPickerFilter = isWeapon ? "" : "Armor_";
        itemPickerCustomId = "";
        itemPickerPage = 0;
        itemPickerSelectedItem = null;
        rebuildItemPickerFiltered();
        needsFieldRefresh = true;
    }

    private void deleteFilteredGearItems(boolean isWeapon) {
        List<String> filtered = isWeapon ? wpnCatTreeFilteredKeys : armCatTreeFilteredKeys;
        RPGMobsConfig.GearCategory root = isWeapon ? editWeaponCategoryTree : editArmorCategoryTree;
        for (String key : new ArrayList<>(filtered)) {
            MobRuleCategoryHelpers.removeGearItemKeyRecursive(root, key);
        }
        rebuildGearCatTreeFiltered(isWeapon);
    }

    private void deleteAllGearItems(boolean isWeapon) {
        RPGMobsConfig.GearCategory root = isWeapon ? editWeaponCategoryTree : editArmorCategoryTree;
        List<String> allKeys = MobRuleCategoryHelpers.collectAllGearItemKeys(root);
        for (String key : allKeys) {
            MobRuleCategoryHelpers.removeGearItemKeyRecursive(root, key);
        }
        rebuildGearCatTreeFiltered(isWeapon);
    }

    private void renameGearCategory(int rowIdx, String newName, boolean isWeapon) {
        List<TreeItem> items = isWeapon ? wpnCatTreeItems : armCatTreeItems;
        if (rowIdx == -1) {
            RPGMobsConfig.GearCategory cat = ensureCurrentGearCat(isWeapon);
            for (RPGMobsConfig.GearCategory child : cat.children) {
                if (child.name.equals(pendingRenameName)) {
                    String oldName = child.name;
                    child.name = newName;
                    propagateGearCategoryRename(oldName, newName, isWeapon);
                    needsFieldRefresh = true;
                    return;
                }
            }
            return;
        }
        if (rowIdx < 0 || rowIdx >= items.size()) return;
        TreeItem item = items.get(rowIdx);
        if (item.name.equals(newName)) return;
        RPGMobsConfig.GearCategory cat = ensureCurrentGearCat(isWeapon);
        if (item.isCategory) {
            for (RPGMobsConfig.GearCategory child : cat.children) {
                if (child.name.equals(item.name)) {
                    String oldName = child.name;
                    child.name = newName;
                    items.set(rowIdx, new TreeItem(newName, true));
                    propagateGearCategoryRename(oldName, newName, isWeapon);
                    needsFieldRefresh = true;
                    return;
                }
            }
        } else {
            int keyIdx = cat.itemKeys.indexOf(item.name);
            if (keyIdx >= 0) {
                String oldKey = cat.itemKeys.get(keyIdx);
                cat.itemKeys.set(keyIdx, newName);
                items.set(rowIdx, new TreeItem(newName, false));
                RPGMobsConfig.GearCategory root = isWeapon ? editWeaponCategoryTree : editArmorCategoryTree;
                MobRuleCategoryHelpers.renameGearItemKeyRecursive(root, oldKey, newName);
                needsFieldRefresh = true;
            }
        }
    }

    private void propagateGearCategoryRename(String oldName, @Nullable String newName, boolean isWeapon) {
        for (RPGMobsConfig.MobRule rule : editMobRules.values()) {
            List<String> cats = isWeapon ? rule.allowedWeaponCategories : rule.allowedArmorCategories;
            if (cats.contains(oldName)) {
                if (!(cats instanceof ArrayList)) {
                    cats = new ArrayList<>(cats);
                    if (isWeapon) rule.allowedWeaponCategories = cats; else rule.allowedArmorCategories = cats;
                }
                int position = cats.indexOf(oldName);
                if (position >= 0) {
                    if (newName != null) {
                        cats.set(position, newName);
                    } else {
                        cats.remove(position);
                    }
                }
            }
        }
        if (isWeapon) {
            for (RPGMobsConfig.AbilityConfig ac : editAbilityConfigs.values()) {
                if (ac.gate != null && ac.gate.allowedWeaponCategories != null) {
                    List<String> gateList = ac.gate.allowedWeaponCategories;
                    int position = gateList.indexOf(oldName);
                    if (position >= 0) {
                        if (!(gateList instanceof ArrayList)) {
                            gateList = new ArrayList<>(gateList);
                            ac.gate.allowedWeaponCategories = gateList;
                        }
                        if (newName != null) {
                            gateList.set(position, newName);
                        } else {
                            gateList.remove(position);
                        }
                    }
                }
            }
        }
    }

    private void populateGearCatTree(UICommandBuilder c, boolean isWeapon) {
        List<TreeItem> items = isWeapon ? wpnCatTreeItems : armCatTreeItems;
        String filter = isWeapon ? wpnCatTreeFilter : armCatTreeFilter;
        List<String> filteredKeys = isWeapon ? wpnCatTreeFilteredKeys : armCatTreeFilteredKeys;
        Deque<RPGMobsConfig.GearCategory> navHistory = isWeapon ? wpnCatNavHistory : armCatNavHistory;
        Deque<RPGMobsConfig.GearCategory> fwdHistory = isWeapon ? wpnCatForwardHistory : armCatForwardHistory;
        String prefix = isWeapon ? "#WpnCat" : "#ArmCat";
        int filterPage = isWeapon ? wpnCatFilterPage : armCatFilterPage;

        if (needsFieldRefresh) {
            c.set(prefix + "TreeFilter.Value", filter);
        }

        items.clear();
        boolean inFilterMode = !filter.isEmpty();
        c.set(prefix + "Breadcrumb.Visible", !inFilterMode);

        if (inFilterMode) {
            int totalFiltered = filteredKeys.size();
            int totalPages = Math.max(1, (totalFiltered + AdminUIData.TREE_ROW_COUNT - 1) / AdminUIData.TREE_ROW_COUNT);
            if (filterPage >= totalPages) {
                filterPage = totalPages - 1;
                if (isWeapon) wpnCatFilterPage = filterPage; else armCatFilterPage = filterPage;
            }
            int pageStart = filterPage * AdminUIData.TREE_ROW_COUNT;
            int pageEnd = Math.min(pageStart + AdminUIData.TREE_ROW_COUNT, totalFiltered);
            for (int j = pageStart; j < pageEnd; j++) {
                items.add(new TreeItem(filteredKeys.get(j), false));
            }
            boolean showPagination = totalPages > 1;
            c.set(prefix + "Pagination.Visible", showPagination);
            if (showPagination) {
                c.set(prefix + "PageInfo.Text", (filterPage + 1) + " / " + totalPages);
            }
        } else {
            c.set(prefix + "Pagination.Visible", false);
            RPGMobsConfig.GearCategory cat = ensureCurrentGearCat(isWeapon);
            c.set(prefix + "NavBack.Visible", !navHistory.isEmpty());
            c.set(prefix + "NavForward.Visible", !fwdHistory.isEmpty());
            c.set(prefix + "BreadcrumbText.Text", buildGearCatBreadcrumb(isWeapon));
            for (RPGMobsConfig.GearCategory child : cat.children) {
                items.add(new TreeItem(child.name, true));
            }
            for (String key : cat.itemKeys) {
                items.add(new TreeItem(key, false));
            }
        }

        for (int i = 0; i < AdminUIData.TREE_ROW_COUNT; i++) {
            boolean vis = i < items.size();
            if (vis) {
                TreeItem item = items.get(i);
                c.set(prefix + "RowIcon" + i + ".Visible", !item.isCategory);
                if (!item.isCategory) {
                    c.set(prefix + "RowIcon" + i + ".ItemId", item.name);
                }
                c.set(prefix + "Row" + i + ".Visible", true);
                c.set(prefix + "RowCat" + i + ".Visible", item.isCategory);
                c.set(prefix + "RowItm" + i + ".Visible", !item.isCategory);
                if (item.isCategory) {
                    c.set(prefix + "RowCat" + i + ".Text", "[>] " + item.name);
                } else {
                    c.set(prefix + "RowItm" + i + ".Text", item.name);
                }
                c.set(prefix + "RowRen" + i + ".Visible", !inFilterMode && item.isCategory);
                c.set(prefix + "RowChs" + i + ".Visible", !inFilterMode && !item.isCategory);
                c.set(prefix + "RowMov" + i + ".Visible", !inFilterMode && item.isCategory);
                c.set(prefix + "RowDel" + i + ".Visible", true);
            } else {
                c.set(prefix + "RowIcon" + i + ".Visible", false);
                c.set(prefix + "Row" + i + ".Visible", false);
            }
        }
    }

    private void handleGlobMobRuleArmCatAdd() {
        RPGMobsConfig.MobRule rule = getExpandedGlobMobRule();
        if (rule == null) return;
        linkPopupMode = LinkPopupMode.ARMOR_CATEGORY_ADD;
        linkPopupOpen = true;
        linkPopupNavHistory.clear();
        linkPopupGearNavHistory.clear();
        linkPopupGearCurrentCategory = editArmorCategoryTree;
        linkPopupSelectedCategory = null;
        rebuildLinkPopupItems();
        needsFieldRefresh = true;
    }

    private void handleGlobMobRuleArmCatDel(int slotIdx) {
        RPGMobsConfig.MobRule rule = getExpandedGlobMobRule();
        if (rule == null) return;
        int absIdx = mobRuleArmCatPage * MOB_RULE_LINKED_PAGE_SIZE + slotIdx;
        List<String> filtered = mobRuleArmCatFiltered;
        if (absIdx < 0 || absIdx >= filtered.size()) return;
        String key = filtered.get(absIdx);
        if (!(rule.allowedArmorCategories instanceof ArrayList)) {
            rule.allowedArmorCategories = new ArrayList<>(rule.allowedArmorCategories);
        }
        rule.allowedArmorCategories.remove(key);
        needsFieldRefresh = true;
    }

    private void handleGlobMobRuleArmCatPeek(int slotIdx) {
        RPGMobsConfig.MobRule rule = getExpandedGlobMobRule();
        if (rule == null) return;
        int absIdx = mobRuleArmCatPage * MOB_RULE_LINKED_PAGE_SIZE + slotIdx;
        List<String> filtered = mobRuleArmCatFiltered;
        if (absIdx < 0 || absIdx >= filtered.size()) return;
        String entry = filtered.get(absIdx);
        if (MobRuleCategoryHelpers.isCategoryKey(entry)) {
            String catName = MobRuleCategoryHelpers.fromCategoryKey(entry);
            RPGMobsConfig.GearCategory cat = MobRuleCategoryHelpers.findGearCategoryByName(editArmorCategoryTree, catName);
            List<String> items = cat != null ? MobRuleCategoryHelpers.collectAllGearItemKeys(cat) : List.of();
            openCategoryPeek("Armor Category: " + catName, items, CatPeekSource.ARMOR_CAT, entry);
        }
    }

    private void handleGlobMobRuleArmCatClear() {
        RPGMobsConfig.MobRule rule = getExpandedGlobMobRule();
        if (rule == null) return;
        if (!(rule.allowedArmorCategories instanceof ArrayList)) {
            rule.allowedArmorCategories = new ArrayList<>();
        } else {
            rule.allowedArmorCategories.clear();
        }
        mobRuleArmCatPage = 0;
        needsFieldRefresh = true;
    }

    private void handleGlobMobRuleArmCatAddItem() {
        RPGMobsConfig.MobRule rule = getExpandedGlobMobRule();
        if (rule == null) return;
        itemPickerOpen = true;
        itemPickerDropSlot = ITEM_PICKER_MOB_RULE_ARM;
        itemPickerFilter = "";
        itemPickerCustomId = "";
        itemPickerPage = 0;
        itemPickerSelectedItem = null;
        rebuildItemPickerFiltered();
        needsFieldRefresh = true;
    }

    private void handleMobRuleArmCatNextPage() {
        RPGMobsConfig.MobRule rule = getExpandedGlobMobRule();
        if (rule == null) return;
        rebuildMobRuleLinkedFiltered(rule.allowedArmorCategories, mobRuleArmCatFilter, mobRuleArmCatFiltered);
        int maxPage = Math.max(0, (mobRuleArmCatFiltered.size() - 1) / MOB_RULE_LINKED_PAGE_SIZE);
        if (mobRuleArmCatPage < maxPage) mobRuleArmCatPage++;
        needsFieldRefresh = true;
    }

    private RPGMobsConfig.LootTemplateCategory getLootCategoryRoot() {
        if (editOverlay != null && editOverlay.lootTemplateCategoryTree != null) {
            return editOverlay.lootTemplateCategoryTree;
        }
        RPGMobsConfig config = plugin.getConfig();
        return config != null && config.lootConfig.lootTemplateTree != null
                ? config.lootConfig.lootTemplateTree
                : new RPGMobsConfig.LootTemplateCategory("All", List.of());
    }

    private RPGMobsConfig.LootTemplateCategory ensureCurrentLootCategory() {
        if (currentLootCategory == null) {
            currentLootCategory = getLootCategoryRoot();
        }
        return currentLootCategory;
    }

    private void navigateToLootCategory(RPGMobsConfig.LootTemplateCategory target) {
        if (currentLootCategory != null) {
            lootNavHistory.push(currentLootCategory);
        }
        lootForwardHistory.clear();
        currentLootCategory = target;
        lootTemplateExpandedIndex = -1;
        lootTemplateDropPage = 0;
        needsFieldRefresh = true;
    }

    private void lootTreeBack() {
        if (lootNavHistory.isEmpty()) return;
        if (currentLootCategory != null) {
            lootForwardHistory.push(currentLootCategory);
        }
        currentLootCategory = lootNavHistory.pop();
        lootTemplateExpandedIndex = -1;
        lootTemplateDropPage = 0;
        needsFieldRefresh = true;
    }

    private void lootTreeForward() {
        if (lootForwardHistory.isEmpty()) return;
        if (currentLootCategory != null) {
            lootNavHistory.push(currentLootCategory);
        }
        currentLootCategory = lootForwardHistory.pop();
        lootTemplateExpandedIndex = -1;
        lootTemplateDropPage = 0;
        needsFieldRefresh = true;
    }

    private void addLootCategory() {
        RPGMobsConfig.LootTemplateCategory cat = ensureCurrentLootCategory();
        var newCat = new RPGMobsConfig.LootTemplateCategory("NewCategory", List.of());
        cat.children.add(newCat);
        needsFieldRefresh = true;
    }

    private void addLootTemplateItem() {
        RPGMobsConfig.LootTemplateCategory cat = ensureCurrentLootCategory();
        String newKey = "New_Template";
        int suffix = 1;
        while (editLootTemplates.containsKey(newKey)) {
            newKey = "New_Template_" + suffix++;
        }
        RPGMobsConfig.LootTemplate template = new RPGMobsConfig.LootTemplate();
        template.name = newKey;
        editLootTemplates.put(newKey, template);
        cat.templateKeys.add(newKey);
        needsFieldRefresh = true;
    }

    private String buildLootBreadcrumb() {
        var path = new ArrayList<String>();
        path.add(getLootCategoryRoot().name);
        buildLootBreadcrumbPath(getLootCategoryRoot(), ensureCurrentLootCategory(), path);
        return "> " + String.join(" > ", path);
    }

    private boolean buildLootBreadcrumbPath(RPGMobsConfig.LootTemplateCategory current,
                                            RPGMobsConfig.LootTemplateCategory target,
                                            List<String> path) {
        if (current == target) return true;
        for (RPGMobsConfig.LootTemplateCategory child : current.children) {
            path.add(child.name);
            if (buildLootBreadcrumbPath(child, target, path)) return true;
            path.removeLast();
        }
        return false;
    }

    private void populateEntityEffects(UICommandBuilder c) {
        if (needsFieldRefresh) {
            c.set("#EffectTreeFilter.Value", effectTreeFilter);
        }

        rebuildEffectTreeFiltered();
        int count = effectTreeFiltered.size();
        boolean empty = count == 0;
        c.set("#EffectTreeEmpty.Visible", empty);

        boolean hasExpanded = effectExpandedIndex >= 0 && effectExpandedIndex < count;

        for (int i = 0; i < AdminUIData.TREE_ROW_COUNT; i++) {
            if (hasExpanded && i > effectExpandedIndex) {
                c.set("#EffectRow" + i + ".Visible", false);
            } else if (i < count) {
                String key = effectTreeFiltered.get(i);
                RPGMobsConfig.EntityEffectConfig eff = editEntityEffects.get(key);
                boolean enabled = eff != null && eff.isEnabled;
                c.set("#EffectRow" + i + ".Visible", true);
                c.set("#EffectRowItm" + i + ".Visible", enabled);
                c.set("#EffectRowItmOff" + i + ".Visible", !enabled);
                c.set("#EffectRowTogOn" + i + ".Visible", enabled);
                c.set("#EffectRowTogOff" + i + ".Visible", !enabled);
                String displayName = StringHelpers.toDisplayName(key);
                c.set("#EffectRowItm" + i + ".Text", displayName);
                c.set("#EffectRowItmOff" + i + ".Text", displayName);
            } else {
                c.set("#EffectRow" + i + ".Visible", false);
            }
        }

        c.set("#EffectDetailPanel.Visible", hasExpanded);
        if (hasExpanded) {
            String key = effectTreeFiltered.get(effectExpandedIndex);
            RPGMobsConfig.EntityEffectConfig eff = editEntityEffects.get(key);
            if (eff == null) return;

            c.set("#EffectDetailName.Text", "Editing " + StringHelpers.toDisplayName(key));
            renderToggle(c, "#EffectDetailInfinite", eff.infinite);

            for (int t = 0; t < 5; t++) {
                boolean tierOn = t < eff.isEnabledPerTier.length && eff.isEnabledPerTier[t];
                c.set("#EffectDetailTierOn" + t + ".Visible", tierOn);
                c.set("#EffectDetailTierOff" + t + ".Visible", !tierOn);
            }

            if (needsFieldRefresh) {
                for (int t = 0; t < 5; t++) {
                    float val = t < eff.amountMultiplierPerTier.length ? eff.amountMultiplierPerTier[t] : 0f;
                    c.set("#EffectDetailMult" + t + ".Value", fmtFloat(val));
                }
            }
        }
    }

    private void rebuildEffectTreeFiltered() {
        effectTreeFiltered.clear();
        String lowerFilter = effectTreeFilter.toLowerCase();
        for (String key : entityEffectKeys) {
            if (lowerFilter.isEmpty()) {
                effectTreeFiltered.add(key);
            } else {
                String displayName = StringHelpers.toDisplayName(key);
                if (displayName.toLowerCase().contains(lowerFilter) || key.toLowerCase().contains(lowerFilter)) {
                    effectTreeFiltered.add(key);
                }
            }
        }
    }

    private void handleEffectRowClick(int rowIdx) {
        if (rowIdx < 0 || rowIdx >= effectTreeFiltered.size()) return;
        if (effectExpandedIndex == rowIdx) {
            effectExpandedIndex = -1;
        } else {
            effectExpandedIndex = rowIdx;
            needsFieldRefresh = true;
        }
    }

    private void handleEffectRowToggle(int rowIdx) {
        if (rowIdx < 0 || rowIdx >= effectTreeFiltered.size()) return;
        String key = effectTreeFiltered.get(rowIdx);
        var eff = editEntityEffects.get(key);
        if (eff != null) eff.isEnabled = !eff.isEnabled;
        needsFieldRefresh = true;
    }

    private void handleEffectDetailInfinite() {
        if (effectExpandedIndex < 0 || effectExpandedIndex >= effectTreeFiltered.size()) return;
        String key = effectTreeFiltered.get(effectExpandedIndex);
        var eff = editEntityEffects.get(key);
        if (eff != null) eff.infinite = !eff.infinite;
        needsFieldRefresh = true;
    }

    private void handleEffectDetailTier(int tierIdx) {
        if (tierIdx < 0 || tierIdx >= 5) return;
        if (effectExpandedIndex < 0 || effectExpandedIndex >= effectTreeFiltered.size()) return;
        String key = effectTreeFiltered.get(effectExpandedIndex);
        var eff = editEntityEffects.get(key);
        if (eff != null && tierIdx < eff.isEnabledPerTier.length) {
            eff.isEnabledPerTier[tierIdx] = !eff.isEnabledPerTier[tierIdx];
            needsFieldRefresh = true;
        }
    }

    private void populateLootTree(UICommandBuilder c) {
        if (needsFieldRefresh) {
            c.set("#LootTreeFilter.Value", lootTreeFilter);
        }
        boolean filtering = !lootTreeFilter.isEmpty();

        if (filtering) {
            c.set("#LootBreadcrumb.Visible", false);
            lootTreeItems.clear();
            for (String key : lootTreeFilteredKeys) {
                lootTreeItems.add(new TreeItem(key, false));
            }
        } else {
            c.set("#LootBreadcrumb.Visible", true);
            RPGMobsConfig.LootTemplateCategory cat = ensureCurrentLootCategory();
            c.set("#LootNavBack.Visible", !lootNavHistory.isEmpty());
            c.set("#LootNavForward.Visible", !lootForwardHistory.isEmpty());
            c.set("#LootBreadcrumbText.Text", buildLootBreadcrumb());

            lootTreeItems.clear();
            for (RPGMobsConfig.LootTemplateCategory child : cat.children) {
                lootTreeItems.add(new TreeItem(child.name, true));
            }
            for (String key : cat.templateKeys) {
                lootTreeItems.add(new TreeItem(key, false));
            }
        }

        boolean empty = lootTreeItems.isEmpty();
        c.set("#LootTreeEmpty.Visible", empty);
        c.set("#LootDeleteFiltered.Visible", filtering && !empty);

        boolean hasExpanded = lootTemplateExpandedIndex >= 0 && lootTemplateExpandedIndex < lootTreeItems.size()
                && !lootTreeItems.get(lootTemplateExpandedIndex).isCategory;

        for (int i = 0; i < AdminUIData.TREE_ROW_COUNT; i++) {
            if (hasExpanded && i > lootTemplateExpandedIndex) {
                c.set("#LootRow" + i + ".Visible", false);
            } else if (i < lootTreeItems.size()) {
                TreeItem item = lootTreeItems.get(i);
                c.set("#LootRow" + i + ".Visible", true);
                c.set("#LootRowCat" + i + ".Visible", item.isCategory);
                c.set("#LootRowItm" + i + ".Visible", !item.isCategory);
                c.set("#LootRowRen" + i + ".Visible", !filtering);
                c.set("#LootRowMov" + i + ".Visible", !filtering);
                if (item.isCategory) {
                    c.set("#LootRowCat" + i + ".Text", "[>] " + item.name);
                } else {
                    c.set("#LootRowItm" + i + ".Text", item.name);
                }
            } else {
                c.set("#LootRow" + i + ".Visible", false);
            }
        }

        populateLootTemplateDetail(c);
    }

    private void handleLootRowClick(int rowIdx) {
        if (rowIdx < 0 || rowIdx >= lootTreeItems.size()) return;
        TreeItem item = lootTreeItems.get(rowIdx);
        if (item.isCategory) {
            RPGMobsConfig.LootTemplateCategory cat = ensureCurrentLootCategory();
            for (RPGMobsConfig.LootTemplateCategory child : cat.children) {
                if (child.name.equals(item.name)) {
                    lootTemplateExpandedIndex = -1;
                    navigateToLootCategory(child);
                    return;
                }
            }
        } else {
            if (lootTemplateExpandedIndex == rowIdx) {
                lootTemplateExpandedIndex = -1;
            } else {
                lootTemplateExpandedIndex = rowIdx;
                lootTemplateDropPage = 0;
                lootTplMobFilter = "";
                lootTplMobPage = 0;
                rebuildLootTplMobFiltered();
            }
            needsFieldRefresh = true;
        }
    }

    private void handleLootRowDelete(int rowIdx) {
        if (rowIdx < 0 || rowIdx >= lootTreeItems.size()) return;
        TreeItem item = lootTreeItems.get(rowIdx);
        if (!lootTreeFilter.isEmpty()) {
            if (!item.isCategory) {
                MobRuleCategoryHelpers.removeLootTemplateKeyRecursive(getLootCategoryRoot(), item.name);
                editLootTemplates.remove(item.name);
                rebuildLootTreeFiltered();
            }
        } else {
            RPGMobsConfig.LootTemplateCategory cat = ensureCurrentLootCategory();
            if (item.isCategory) {
                cat.children.removeIf(child -> child.name.equals(item.name));
            } else {
                cat.templateKeys.remove(item.name);
                editLootTemplates.remove(item.name);
            }
        }
        lootTemplateExpandedIndex = -1;
        needsFieldRefresh = true;
    }

    private void rebuildLootTreeFiltered() {
        lootTreeFilteredKeys.clear();
        if (!lootTreeFilter.isEmpty()) {
            lootTreeFilteredKeys.addAll(
                    MobRuleCategoryHelpers.searchLootTemplateKeysRecursive(getLootCategoryRoot(), lootTreeFilter));
        }
    }

    private void handleLootDeleteFiltered() {
        if (lootTreeFilter.isEmpty() || lootTreeFilteredKeys.isEmpty()) return;
        RPGMobsConfig.LootTemplateCategory root = getLootCategoryRoot();
        for (String key : new ArrayList<>(lootTreeFilteredKeys)) {
            MobRuleCategoryHelpers.removeLootTemplateKeyRecursive(root, key);
            editLootTemplates.remove(key);
        }
        lootTemplateExpandedIndex = -1;
        rebuildLootTreeFiltered();
        needsFieldRefresh = true;
    }

    private void handleLootDeleteAll() {
        RPGMobsConfig.LootTemplateCategory root = getLootCategoryRoot();
        List<String> allKeys = collectAllLootTemplateKeys(root);
        for (String key : allKeys) {
            editLootTemplates.remove(key);
        }
        root.templateKeys.clear();
        root.children.clear();
        currentLootCategory = root;
        lootNavHistory.clear();
        lootForwardHistory.clear();
        lootTemplateExpandedIndex = -1;
        if (!lootTreeFilter.isEmpty()) {
            rebuildLootTreeFiltered();
        }
        needsFieldRefresh = true;
    }

    private List<String> collectAllLootTemplateKeys(RPGMobsConfig.LootTemplateCategory cat) {
        List<String> keys = new ArrayList<>(cat.templateKeys);
        for (RPGMobsConfig.LootTemplateCategory child : cat.children) {
            keys.addAll(collectAllLootTemplateKeys(child));
        }
        return keys;
    }

    private RPGMobsConfig.@Nullable LootTemplate getExpandedLootTemplate() {
        if (lootTemplateExpandedIndex < 0 || lootTemplateExpandedIndex >= lootTreeItems.size()) return null;
        TreeItem item = lootTreeItems.get(lootTemplateExpandedIndex);
        if (item.isCategory) return null;
        return editLootTemplates.get(item.name);
    }

    private void populateLootTemplateDetail(UICommandBuilder c) {
        RPGMobsConfig.LootTemplate template = getExpandedLootTemplate();
        boolean showDetail = template != null;
        c.set("#LootTemplateDetail.Visible", showDetail);
        if (!showDetail) return;

        String templateName = lootTreeItems.get(lootTemplateExpandedIndex).name;
        c.set("#LootTplDetailTitle.Text", "Editing " + templateName);

        List<String> links = template.linkedMobRuleKeys;
        int totalMobs = lootTplMobFiltered.size();
        boolean mobsEmpty = links.isEmpty();
        c.set("#LootTplMobsEmpty.Visible", mobsEmpty);
        c.set("#LootTplMobDeleteFiltered.Visible", !lootTplMobFilter.isEmpty() && !lootTplMobFiltered.isEmpty());

        if (needsFieldRefresh) {
            c.set("#LootTplMobFilter.Value", lootTplMobFilter);
        }

        int mobPages = Math.max(1, (totalMobs + LOOT_TPL_MOB_PAGE_SIZE - 1) / LOOT_TPL_MOB_PAGE_SIZE);
        if (lootTplMobPage >= mobPages) lootTplMobPage = mobPages - 1;
        int mobPageStart = lootTplMobPage * LOOT_TPL_MOB_PAGE_SIZE;

        for (int j = 0; j < LOOT_TPL_MOB_PAGE_SIZE; j++) {
            int absIdx = mobPageStart + j;
            boolean vis = absIdx < totalMobs;
            c.set("#LootTplMobRow" + j + ".Visible", vis);
            if (vis) {
                String key = lootTplMobFiltered.get(absIdx);
                boolean isCat = MobRuleCategoryHelpers.isCategoryKey(key);
                String displayName = isCat ? "[Category] " + MobRuleCategoryHelpers.fromCategoryKey(key) : key;
                c.set("#LootTplMobNameCat" + j + ".Visible", isCat);
                c.set("#LootTplMobNameMob" + j + ".Visible", !isCat);
                c.set((isCat ? "#LootTplMobNameCat" : "#LootTplMobNameMob") + j + ".Text", displayName);
                c.set("#LootTplMobPeek" + j + ".Visible", isCat);
            } else {
                c.set("#LootTplMobNameCat" + j + ".Visible", false);
                c.set("#LootTplMobNameMob" + j + ".Visible", false);
                c.set("#LootTplMobPeek" + j + ".Visible", false);
            }
        }

        int mobPageEnd = Math.min((lootTplMobPage + 1) * LOOT_TPL_MOB_PAGE_SIZE, totalMobs);
        c.set("#LootTplMobPageInfo.Text", mobPageEnd + "/" + totalMobs);
        c.set("#LootTplMobPagination.Visible", true);
        boolean mobMultiPage = mobPages > 1;
        c.set("#LootTplMobFirstPage.Visible", mobMultiPage && lootTplMobPage > 0);
        c.set("#LootTplMobPrevPage.Visible", mobMultiPage && lootTplMobPage > 0);
        c.set("#LootTplMobNextPage.Visible", mobMultiPage && lootTplMobPage < mobPages - 1);
        c.set("#LootTplMobLastPage.Visible", mobMultiPage && lootTplMobPage < mobPages - 1);

        List<RPGMobsConfig.ExtraDropRule> drops = template.drops;
        int totalDrops = drops.size();
        int pageSize = AdminUIData.LOOT_TEMPLATE_DROPS_PER_PAGE;
        int totalPages = Math.max(1, (totalDrops + pageSize - 1) / pageSize);
        if (lootTemplateDropPage >= totalPages) lootTemplateDropPage = totalPages - 1;
        int pageStart = lootTemplateDropPage * pageSize;

        for (int slot = 0; slot < pageSize; slot++) {
            int dropIdx = pageStart + slot;
            boolean vis = dropIdx < totalDrops;
            if (vis) {
                RPGMobsConfig.ExtraDropRule drop = drops.get(dropIdx);
                String itemId = drop.itemId != null ? drop.itemId : "";
                boolean hasItemId = !itemId.isBlank();

                c.set("#LootTplDropIcon" + slot + ".Visible", hasItemId);
                if (hasItemId) c.set("#LootTplDropIcon" + slot + ".ItemId", itemId);
                c.set("#LootTplDropItemBtn" + slot + ".Text", hasItemId ? itemId : "(select item)");
                for (int t = 0; t < AdminUIData.TIERS_COUNT; t++) {
                    boolean en = drop.enabledPerTier[t];
                    c.set("#LootTplDropTierOn" + slot + "T" + t + ".Visible", en);
                    c.set("#LootTplDropTierOff" + slot + "T" + t + ".Visible", !en);
                }
                if (needsFieldRefresh) {
                    c.set("#LootTplDropChance" + slot + ".Value", String.valueOf(drop.chance));
                    c.set("#LootTplDropMinQty" + slot + ".Value", String.valueOf(drop.minQty));
                    c.set("#LootTplDropMaxQty" + slot + ".Value", String.valueOf(drop.maxQty));
                }
            } else {
                c.set("#LootTplDropIcon" + slot + ".Visible", false);
            }
            c.set("#LootTplDrop" + slot + ".Visible", vis);
        }

        c.set("#LootTplDropsEmpty.Visible", totalDrops == 0);

        int dropPageEnd = Math.min((lootTemplateDropPage + 1) * pageSize, totalDrops);
        c.set("#LootTplDropPageInfo.Text", dropPageEnd + "/" + totalDrops);
        c.set("#LootTplDropPagination.Visible", true);
        boolean multiPage = totalPages > 1;
        c.set("#LootTplDropFirstPage.Visible", multiPage && lootTemplateDropPage > 0);
        c.set("#LootTplDropPrevPage.Visible", multiPage && lootTemplateDropPage > 0);
        c.set("#LootTplDropNextPage.Visible", multiPage && lootTemplateDropPage < totalPages - 1);
        c.set("#LootTplDropLastPage.Visible", multiPage && lootTemplateDropPage < totalPages - 1);
    }

    private void processLootTemplateDropFields(AdminUIData data) {
        RPGMobsConfig.LootTemplate template = getExpandedLootTemplate();
        if (template == null) return;

        int pageStart = lootTemplateDropPage * AdminUIData.LOOT_TEMPLATE_DROPS_PER_PAGE;
        for (int slot = 0; slot < AdminUIData.LOOT_TEMPLATE_DROPS_PER_PAGE; slot++) {
            int dropIdx = pageStart + slot;
            if (dropIdx >= template.drops.size()) break;
            RPGMobsConfig.ExtraDropRule drop = template.drops.get(dropIdx);

            if (data.lootTplDropChances[slot] != null) {
                try { drop.chance = Double.parseDouble(data.lootTplDropChances[slot].trim()); } catch (NumberFormatException ignored) {}
            }
            if (data.lootTplDropMinQtys[slot] != null) {
                try { drop.minQty = Integer.parseInt(data.lootTplDropMinQtys[slot].trim()); } catch (NumberFormatException ignored) {}
            }
            if (data.lootTplDropMaxQtys[slot] != null) {
                try { drop.maxQty = Integer.parseInt(data.lootTplDropMaxQtys[slot].trim()); } catch (NumberFormatException ignored) {}
            }
        }
    }

    private void handleLootTplMobDelete(int slotIdx) {
        RPGMobsConfig.LootTemplate template = getExpandedLootTemplate();
        if (template == null) return;
        int absIdx = lootTplMobPage * LOOT_TPL_MOB_PAGE_SIZE + slotIdx;
        if (absIdx < 0 || absIdx >= lootTplMobFiltered.size()) return;
        String key = lootTplMobFiltered.get(absIdx);
        template.linkedMobRuleKeys.remove(key);
        rebuildLootTplMobFiltered();
        needsFieldRefresh = true;
    }

    private void handleLootTplMobDeleteFiltered() {
        RPGMobsConfig.LootTemplate template = getExpandedLootTemplate();
        if (template == null || lootTplMobFilter.isEmpty() || lootTplMobFiltered.isEmpty()) return;
        template.linkedMobRuleKeys.removeAll(new ArrayList<>(lootTplMobFiltered));
        lootTplMobPage = 0;
        rebuildLootTplMobFiltered();
        needsFieldRefresh = true;
    }

    private void handleLootTplMobDeleteAll() {
        RPGMobsConfig.LootTemplate template = getExpandedLootTemplate();
        if (template == null) return;
        template.linkedMobRuleKeys.clear();
        lootTplMobPage = 0;
        rebuildLootTplMobFiltered();
        needsFieldRefresh = true;
    }

    private void rebuildLootTplMobFiltered() {
        lootTplMobFiltered.clear();
        RPGMobsConfig.LootTemplate template = getExpandedLootTemplate();
        if (template == null) return;
        String lower = lootTplMobFilter.toLowerCase();
        for (String key : template.linkedMobRuleKeys) {
            String searchable = MobRuleCategoryHelpers.isCategoryKey(key)
                    ? MobRuleCategoryHelpers.fromCategoryKey(key) : key;
            if (lower.isEmpty() || searchable.toLowerCase().contains(lower)) {
                lootTplMobFiltered.add(key);
            }
        }
    }

    private int maxLootTplMobPage() {
        int total = lootTplMobFiltered.size();
        int pages = Math.max(1, (total + LOOT_TPL_MOB_PAGE_SIZE - 1) / LOOT_TPL_MOB_PAGE_SIZE);
        return pages - 1;
    }

    private void addLootTemplateDrop() {
        RPGMobsConfig.LootTemplate template = getExpandedLootTemplate();
        if (template == null) return;
        var drop = new RPGMobsConfig.ExtraDropRule();
        drop.itemId = "";
        drop.chance = 1.0;
        drop.minQty = 1;
        drop.maxQty = 1;
        Arrays.fill(drop.enabledPerTier, true);
        template.drops.add(drop);
        int totalDrops = template.drops.size();
        int pageSize = AdminUIData.LOOT_TEMPLATE_DROPS_PER_PAGE;
        lootTemplateDropPage = Math.max(0, (totalDrops - 1) / pageSize);
        needsFieldRefresh = true;
    }

    private void deleteLootTemplateDrop(int slotIdx) {
        RPGMobsConfig.LootTemplate template = getExpandedLootTemplate();
        if (template == null) return;
        int dropIdx = lootTemplateDropPage * AdminUIData.LOOT_TEMPLATE_DROPS_PER_PAGE + slotIdx;
        if (dropIdx < 0 || dropIdx >= template.drops.size()) return;
        template.drops.remove(dropIdx);
        needsFieldRefresh = true;
    }

    private void handleLootTplDropTierToggle(String action) {
        String suffix = action.substring("LootTplDropTier_".length());
        String[] parts = suffix.split("_", 2);
        if (parts.length < 2) return;
        int slot, tier;
        try {
            slot = Integer.parseInt(parts[0]);
            tier = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            return;
        }
        RPGMobsConfig.LootTemplate template = getExpandedLootTemplate();
        if (template == null) return;
        int dropIdx = lootTemplateDropPage * AdminUIData.LOOT_TEMPLATE_DROPS_PER_PAGE + slot;
        if (dropIdx < 0 || dropIdx >= template.drops.size()) return;
        if (tier < 0 || tier >= AdminUIData.TIERS_COUNT) return;
        template.drops.get(dropIdx).enabledPerTier[tier] = !template.drops.get(dropIdx).enabledPerTier[tier];
        needsFieldRefresh = true;
    }

    private void openLootTplCategoryPicker() {
        if (getExpandedLootTemplate() == null) return;
        linkPopupMode = LinkPopupMode.LOOT_TEMPLATE_ADD_CATEGORY;
        linkPopupOpen = true;
        linkPopupNavHistory.clear();
        linkPopupCurrentCategory = getMobRuleCategoryRoot();
        linkPopupSelectedCategory = null;
        rebuildLinkPopupItems();
        needsFieldRefresh = true;
    }

    private void openLootTplNpcPicker() {
        if (getExpandedLootTemplate() == null) return;
        npcPickerMode = NpcPickerMode.LOOT_TEMPLATE_LINKED_MOB;
        npcPickerOpen = true;
        npcPickerFilter = "";
        npcPickerCustomId = "";
        npcPickerPage = 0;
        npcPickerSelectedItem = null;
        rebuildNpcPickerFiltered();
        needsFieldRefresh = true;
    }

    private void closeLinkPopup() {
        linkPopupOpen = false;
        linkPopupSelectedCategory = null;
        needsFieldRefresh = true;
    }

    private void confirmLinkPopup() {
        if (linkPopupSelectedCategory == null) {
            closeLinkPopup();
            return;
        }
        if (linkPopupMode == LinkPopupMode.WEAPON_CATEGORY_ADD) {
            RPGMobsConfig.MobRule rule = getExpandedGlobMobRule();
            String catKey = MobRuleCategoryHelpers.toCategoryKey(linkPopupSelectedCategory);
            if (rule != null && !rule.allowedWeaponCategories.contains(catKey)) {
                if (!(rule.allowedWeaponCategories instanceof ArrayList)) {
                    rule.allowedWeaponCategories = new ArrayList<>(rule.allowedWeaponCategories);
                }
                rule.allowedWeaponCategories.add(catKey);
            }
            closeLinkPopup();
            needsFieldRefresh = true;
            return;
        }
        if (linkPopupMode == LinkPopupMode.ARMOR_CATEGORY_ADD) {
            RPGMobsConfig.MobRule rule = getExpandedGlobMobRule();
            String catKey = MobRuleCategoryHelpers.toCategoryKey(linkPopupSelectedCategory);
            if (rule != null && !rule.allowedArmorCategories.contains(catKey)) {
                if (!(rule.allowedArmorCategories instanceof ArrayList)) {
                    rule.allowedArmorCategories = new ArrayList<>(rule.allowedArmorCategories);
                }
                rule.allowedArmorCategories.add(catKey);
            }
            closeLinkPopup();
            needsFieldRefresh = true;
            return;
        }
        if (linkPopupMode == LinkPopupMode.ABILITY_WEAPON_GATE) {
            if (abilityExpandedIndex >= 0 && abilityExpandedIndex < abilTreeFiltered.size()) {
                String abilId = abilTreeFiltered.get(abilityExpandedIndex);
                RPGMobsConfig.AbilityConfig ac = editAbilityConfigs.get(abilId);
                if (ac != null) {
                    String catKey = MobRuleCategoryHelpers.toCategoryKey(linkPopupSelectedCategory);
                    if (!(ac.gate.allowedWeaponCategories instanceof ArrayList)) {
                        ac.gate.allowedWeaponCategories = new ArrayList<>(ac.gate.allowedWeaponCategories);
                    }
                    if (!ac.gate.allowedWeaponCategories.contains(catKey)) {
                        ac.gate.allowedWeaponCategories.add(catKey);
                    }
                }
            }
            closeLinkPopup();
            needsFieldRefresh = true;
            return;
        }
        var cat = findMobRuleCategoryByName(getMobRuleCategoryRoot(), linkPopupSelectedCategory);
        if (linkPopupMode == LinkPopupMode.ABILITY_ADD_CATEGORY) {
            if (editOverlay != null && abilityExpandedIndex >= 0 && abilityExpandedIndex < abilTreeFiltered.size()) {
                String abilId = abilTreeFiltered.get(abilityExpandedIndex);
                if (cat != null) {
                    ensureAbilityOverlayWithLinkedEntries(abilId);
                    var ao = editOverlay.abilityOverlays.get(abilId);
                    if (ao != null && ao.linkedEntries != null) {
                        String catKey = MobRuleCategoryHelpers.toCategoryKey(linkPopupSelectedCategory);
                        boolean exists = ao.linkedEntries.stream().anyMatch(e -> e.key.equals(catKey));
                        if (!exists) {
                            ao.linkedEntries.add(new ConfigOverlay.AbilityLinkedEntry(
                                    catKey, new boolean[]{true, true, true, true, true}));
                        }
                    }
                }
            }
        } else if (linkPopupMode == LinkPopupMode.LOOT_TEMPLATE_ADD_CATEGORY) {
            RPGMobsConfig.LootTemplate template = getExpandedLootTemplate();
            if (template != null && cat != null) {
                String catKey = MobRuleCategoryHelpers.toCategoryKey(linkPopupSelectedCategory);
                if (!template.linkedMobRuleKeys.contains(catKey)) {
                    template.linkedMobRuleKeys.add(catKey);
                }
                rebuildLootTplMobFiltered();
            }
        } else if (linkPopupMode == LinkPopupMode.TIER_RESTRICTION_ADD_CATEGORY) {
            if (cat != null) {
                String catKey = MobRuleCategoryHelpers.toCategoryKey(linkPopupSelectedCategory);
                addTierRestrictionKey(catKey);
            }
        } else {
            RPGMobsConfig.LootTemplate template = getExpandedLootTemplate();
            if (template != null && !template.linkedMobRuleKeys.contains(linkPopupSelectedCategory)) {
                template.linkedMobRuleKeys.add(linkPopupSelectedCategory);
                rebuildLootTplMobFiltered();
            }
        }
        closeLinkPopup();
    }

    private boolean isGearLinkMode() {
        return linkPopupMode == LinkPopupMode.WEAPON_CATEGORY_ADD || linkPopupMode == LinkPopupMode.ARMOR_CATEGORY_ADD
                || linkPopupMode == LinkPopupMode.ABILITY_WEAPON_GATE;
    }

    private void linkPopupBack() {
        if (isGearLinkMode()) {
            if (linkPopupGearNavHistory.isEmpty()) return;
            linkPopupGearCurrentCategory = linkPopupGearNavHistory.pop();
        } else {
            if (linkPopupNavHistory.isEmpty()) return;
            linkPopupCurrentCategory = linkPopupNavHistory.pop();
        }
        linkPopupSelectedCategory = null;
        rebuildLinkPopupItems();
        needsFieldRefresh = true;
    }

    private void handleLinkPopupRowClick(int rowIdx) {
        if (rowIdx < 0 || rowIdx >= linkPopupTreeItems.size()) return;
        TreeItem item = linkPopupTreeItems.get(rowIdx);
        if (isGearLinkMode()) {
            if (item.isCategory) {
                if (linkPopupGearCurrentCategory != null) {
                    for (RPGMobsConfig.GearCategory child : linkPopupGearCurrentCategory.children) {
                        if (child.name.equals(item.name)) {
                            linkPopupGearNavHistory.push(linkPopupGearCurrentCategory);
                            linkPopupGearCurrentCategory = child;
                            linkPopupSelectedCategory = null;
                            rebuildLinkPopupItems();
                            needsFieldRefresh = true;
                            return;
                        }
                    }
                }
            } else {
                linkPopupSelectedCategory = item.name;
                needsFieldRefresh = true;
            }
        } else {
            if (item.isCategory) {
                if (linkPopupCurrentCategory != null) {
                    for (RPGMobsConfig.MobRuleCategory child : linkPopupCurrentCategory.children) {
                        if (child.name.equals(item.name)) {
                            linkPopupNavHistory.push(linkPopupCurrentCategory);
                            linkPopupCurrentCategory = child;
                            linkPopupSelectedCategory = null;
                            rebuildLinkPopupItems();
                            needsFieldRefresh = true;
                            return;
                        }
                    }
                }
            } else {
                linkPopupSelectedCategory = linkPopupCurrentCategory != null
                        ? linkPopupCurrentCategory.name : "All";
                needsFieldRefresh = true;
            }
        }
    }

    private void rebuildLinkPopupItems() {
        linkPopupTreeItems.clear();
        if (isGearLinkMode()) {
            if (linkPopupGearCurrentCategory == null) return;
            for (RPGMobsConfig.GearCategory child : linkPopupGearCurrentCategory.children) {
                linkPopupTreeItems.add(new TreeItem(child.name, child.children != null && !child.children.isEmpty()));
            }
            for (String key : linkPopupGearCurrentCategory.itemKeys) {
                linkPopupTreeItems.add(new TreeItem(key, false));
            }
            if (linkPopupTreeItems.isEmpty()) {
                linkPopupTreeItems.add(new TreeItem(linkPopupGearCurrentCategory.name, false));
            }
        } else {
            if (linkPopupCurrentCategory == null) return;
            linkPopupTreeItems.add(new TreeItem(linkPopupCurrentCategory.name, false));
            for (RPGMobsConfig.MobRuleCategory child : linkPopupCurrentCategory.children) {
                linkPopupTreeItems.add(new TreeItem(child.name, true));
            }
        }
    }

    private String buildLinkPopupBreadcrumb() {
        if (isGearLinkMode()) {
            RPGMobsConfig.GearCategory root = (linkPopupMode == LinkPopupMode.WEAPON_CATEGORY_ADD
                    || linkPopupMode == LinkPopupMode.ABILITY_WEAPON_GATE)
                    ? editWeaponCategoryTree : editArmorCategoryTree;
            if (linkPopupGearCurrentCategory == null || linkPopupGearCurrentCategory == root) {
                return "> " + (root != null ? root.name : "All");
            }
            var path = new ArrayList<String>();
            path.add(root != null ? root.name : "All");
            buildGearBreadcrumbPath(root, linkPopupGearCurrentCategory, path);
            return "> " + String.join(" > ", path);
        }
        var path = new ArrayList<String>();
        path.add(getMobRuleCategoryRoot().name);
        if (linkPopupCurrentCategory != null && linkPopupCurrentCategory != getMobRuleCategoryRoot()) {
            buildBreadcrumbPath(getMobRuleCategoryRoot(), linkPopupCurrentCategory, path);
        }
        return "> " + String.join(" > ", path);
    }

    private boolean buildGearBreadcrumbPath(RPGMobsConfig.GearCategory current, RPGMobsConfig.GearCategory target,
                                             List<String> path) {
        for (RPGMobsConfig.GearCategory child : current.children) {
            if (child == target) {
                path.add(child.name);
                return true;
            }
            if (buildGearBreadcrumbPath(child, target, path)) {
                path.add(path.size() - 1, child.name);
                return true;
            }
        }
        return false;
    }

    private void populateLinkPopup(UICommandBuilder c) {
        boolean hasBack = isGearLinkMode() ? !linkPopupGearNavHistory.isEmpty() : !linkPopupNavHistory.isEmpty();
        c.set("#LinkPopupNavBack.Visible", hasBack);
        c.set("#LinkPopupBreadcrumbText.Text", buildLinkPopupBreadcrumb());

        boolean empty = linkPopupTreeItems.isEmpty();
        c.set("#LinkPopupEmpty.Visible", empty);

        for (int i = 0; i < LINK_POPUP_ROW_COUNT; i++) {
            if (i < linkPopupTreeItems.size()) {
                TreeItem item = linkPopupTreeItems.get(i);
                c.set("#LinkPopupRow" + i + ".Visible", true);
                c.set("#LinkPopupRowCat" + i + ".Visible", item.isCategory);
                c.set("#LinkPopupRowItm" + i + ".Visible", !item.isCategory);
                if (item.isCategory) {
                    c.set("#LinkPopupRowCat" + i + ".Text", "[>] " + item.name);
                } else {
                    c.set("#LinkPopupRowItm" + i + ".Text", item.name);
                }
            } else {
                c.set("#LinkPopupRow" + i + ".Visible", false);
            }
        }

        boolean hasSelection = linkPopupSelectedCategory != null;
        String selectedLabel = hasSelection
                ? "Selected: " + linkPopupSelectedCategory
                : "Click a category name (green) to select it";
        c.set("#LinkPopupSelected.Text", selectedLabel);
        renderToggle(c, "#LinkPopupConfirm", hasSelection);
    }

    private void openRenamePopupForMobRule(int rowIdx) {
        if (rowIdx < 0 || rowIdx >= mobRuleTreeItems.size()) return;
        TreeItem item = mobRuleTreeItems.get(rowIdx);
        renameTarget = item.isCategory ? RenameTarget.MOB_RULE_CATEGORY : RenameTarget.MOB_RULE_ITEM;
        renameRowIndex = rowIdx;
        pendingRenameName = item.name;
        renamePopupOpen = true;
        needsFieldRefresh = true;
    }

    private void openRenamePopupForLoot(int rowIdx) {
        if (rowIdx < 0 || rowIdx >= lootTreeItems.size()) return;
        TreeItem item = lootTreeItems.get(rowIdx);
        renameTarget = item.isCategory ? RenameTarget.LOOT_CATEGORY : RenameTarget.LOOT_ITEM;
        renameRowIndex = rowIdx;
        pendingRenameName = item.name;
        renamePopupOpen = true;
        lootTemplateExpandedIndex = -1;
        needsFieldRefresh = true;
    }

    private void closeRenamePopup() {
        renamePopupOpen = false;
        renameTarget = null;
        renameRowIndex = -1;
        pendingRenameName = null;
        needsFieldRefresh = true;
    }

    private void confirmRenamePopup() {
        if (pendingRenameName == null || pendingRenameName.isBlank() || renameTarget == null) {
            closeRenamePopup();
            return;
        }
        String newName = pendingRenameName.trim();
        switch (renameTarget) {
            case MOB_RULE_CATEGORY -> renameMobRuleCategory(renameRowIndex, newName);
            case MOB_RULE_ITEM -> renameMobRuleItem(renameRowIndex, newName);
            case LOOT_CATEGORY -> renameLootCategory(renameRowIndex, newName);
            case LOOT_ITEM -> renameLootItem(renameRowIndex, newName);
            case WEAPON_CATEGORY -> {
                if (renameRowIndex == RENAME_IDX_TWO_HANDED_ADD) {
                    addTwoHandedKeyword(newName);
                } else if (renameRowIndex == RENAME_IDX_RARITY_RULE_ADD) {
                    addRarityRule(newName, true);
                } else if (renameRowIndex == RENAME_IDX_SUMMON_ROLE_ADD) {
                    addSummonRoleIdentifier(newName);
                } else {
                    renameGearCategory(renameRowIndex, newName, true);
                }
            }
            case ARMOR_CATEGORY -> {
                if (renameRowIndex == RENAME_IDX_RARITY_RULE_ADD) {
                    addRarityRule(newName, false);
                } else {
                    renameGearCategory(renameRowIndex, newName, false);
                }
            }
        }
        closeRenamePopup();
    }

    private void renameMobRuleCategory(int rowIdx, String newName) {
        if (rowIdx < 0 || rowIdx >= mobRuleTreeItems.size()) return;
        TreeItem item = mobRuleTreeItems.get(rowIdx);
        if (!item.isCategory || item.name.equals(newName)) return;
        RPGMobsConfig.MobRuleCategory cat = ensureCurrentMobRuleCategory();
        for (RPGMobsConfig.MobRuleCategory child : cat.children) {
            if (child.name.equals(item.name)) {
                renameMobRuleCategoryInLinkedKeys(item.name, newName);
                child.name = newName;
                mobRuleTreeItems.set(rowIdx, new TreeItem(newName, true));
                needsFieldRefresh = true;
                return;
            }
        }
    }

    private void renameMobRuleItem(int rowIdx, String newName) {
        if (rowIdx < 0 || rowIdx >= mobRuleTreeItems.size()) return;
        TreeItem item = mobRuleTreeItems.get(rowIdx);
        if (item.isCategory || item.name.equals(newName)) return;
        RPGMobsConfig.MobRuleCategory cat = ensureCurrentMobRuleCategory();
        int keyIdx = cat.mobRuleKeys.indexOf(item.name);
        if (keyIdx >= 0) {
            cat.mobRuleKeys.set(keyIdx, newName);
            mobRuleTreeItems.set(rowIdx, new TreeItem(newName, false));
            RPGMobsConfig.MobRule rule = editMobRules.remove(item.name);
            if (rule != null) editMobRules.put(newName, rule);
            propagateMobRuleKeyRenameToAbilityConfigs(item.name, newName);
            needsFieldRefresh = true;
        }
    }

    private void propagateMobRuleKeyRenameToAbilityConfigs(String oldKey, String newKey) {
        for (RPGMobsConfig.AbilityConfig ac : editAbilityConfigs.values()) {
            if (ac.excludeLinkedMobRuleKeys != null) {
                int position = ac.excludeLinkedMobRuleKeys.indexOf(oldKey);
                if (position >= 0) {
                    if (!(ac.excludeLinkedMobRuleKeys instanceof ArrayList))
                        ac.excludeLinkedMobRuleKeys = new ArrayList<>(ac.excludeLinkedMobRuleKeys);
                    ac.excludeLinkedMobRuleKeys.set(position, newKey);
                }
            }
        }
    }

    private void renameLootCategory(int rowIdx, String newName) {
        if (rowIdx < 0 || rowIdx >= lootTreeItems.size()) return;
        TreeItem item = lootTreeItems.get(rowIdx);
        if (!item.isCategory || item.name.equals(newName)) return;
        RPGMobsConfig.LootTemplateCategory cat = ensureCurrentLootCategory();
        for (RPGMobsConfig.LootTemplateCategory child : cat.children) {
            if (child.name.equals(item.name)) {
                child.name = newName;
                lootTreeItems.set(rowIdx, new TreeItem(newName, true));
                needsFieldRefresh = true;
                return;
            }
        }
    }

    private void renameLootItem(int rowIdx, String newName) {
        if (rowIdx < 0 || rowIdx >= lootTreeItems.size()) return;
        TreeItem item = lootTreeItems.get(rowIdx);
        if (item.isCategory || item.name.equals(newName)) return;
        if (editLootTemplates.containsKey(newName)) return;
        RPGMobsConfig.LootTemplate template = editLootTemplates.remove(item.name);
        if (template == null) return;
        template.name = newName;
        editLootTemplates.put(newName, template);
        RPGMobsConfig.LootTemplateCategory cat = ensureCurrentLootCategory();
        int keyIdx = cat.templateKeys.indexOf(item.name);
        if (keyIdx >= 0) {
            cat.templateKeys.set(keyIdx, newName);
        }
        lootTreeItems.set(rowIdx, new TreeItem(newName, false));
        needsFieldRefresh = true;
    }

    private void openMovePopupForMobRule(int rowIdx) {
        if (rowIdx < 0 || rowIdx >= mobRuleTreeItems.size()) return;
        moveSourceType = MoveSourceType.MOB_RULE;
        moveRowIdx = rowIdx;
        movePopupOpen = true;
        movePopupNavHistory.clear();
        movePopupSelectedCategory = null;
        movePopupCurrentCategoryName = getMobRuleCategoryRoot().name;
        rebuildMovePopupItems();
        needsFieldRefresh = true;
    }

    private void openMovePopupForLoot(int rowIdx) {
        if (rowIdx < 0 || rowIdx >= lootTreeItems.size()) return;
        moveSourceType = MoveSourceType.LOOT;
        moveRowIdx = rowIdx;
        movePopupOpen = true;
        movePopupNavHistory.clear();
        movePopupSelectedCategory = null;
        movePopupCurrentCategoryName = getLootCategoryRoot().name;
        lootTemplateExpandedIndex = -1;
        rebuildMovePopupItems();
        needsFieldRefresh = true;
    }

    private void closeMovePopup() {
        movePopupOpen = false;
        moveSourceType = null;
        moveRowIdx = -1;
        movePopupSelectedCategory = null;
        movePopupNavHistory.clear();
        movePopupCurrentCategoryName = null;
        movePopupCategoryNames.clear();
        needsFieldRefresh = true;
    }

    private void movePopupBack() {
        if (movePopupNavHistory.isEmpty()) return;
        movePopupCurrentCategoryName = movePopupNavHistory.pop();
        movePopupSelectedCategory = null;
        rebuildMovePopupItems();
        needsFieldRefresh = true;
    }

    private void handleMovePopupRowClick(int rowIdx) {
        if (rowIdx < 0 || rowIdx >= movePopupCategoryNames.size()) return;
        String clickedName = movePopupCategoryNames.get(rowIdx);
        if (moveSourceType == MoveSourceType.MOB_RULE) {
            RPGMobsConfig.MobRuleCategory clickedCat = findMobRuleCategoryByName(getMobRuleCategoryRoot(), clickedName);
            if (clickedCat != null && !clickedCat.children.isEmpty()) {
                movePopupNavHistory.push(movePopupCurrentCategoryName);
                movePopupCurrentCategoryName = clickedName;
                movePopupSelectedCategory = null;
                rebuildMovePopupItems();
            } else {
                movePopupSelectedCategory = clickedName;
            }
        } else if (moveSourceType == MoveSourceType.LOOT) {
            RPGMobsConfig.LootTemplateCategory clickedCat = findLootCategoryByName(getLootCategoryRoot(), clickedName);
            if (clickedCat != null && !clickedCat.children.isEmpty()) {
                movePopupNavHistory.push(movePopupCurrentCategoryName);
                movePopupCurrentCategoryName = clickedName;
                movePopupSelectedCategory = null;
                rebuildMovePopupItems();
            } else {
                movePopupSelectedCategory = clickedName;
            }
        } else {
            boolean isWeapon = moveSourceType == MoveSourceType.GEAR_WEAPON;
            RPGMobsConfig.GearCategory root = isWeapon ? editWeaponCategoryTree : editArmorCategoryTree;
            RPGMobsConfig.GearCategory clickedCat = MobRuleCategoryHelpers.findGearCategoryByName(root, clickedName);
            if (clickedCat != null && !clickedCat.children.isEmpty()) {
                movePopupNavHistory.push(movePopupCurrentCategoryName);
                movePopupCurrentCategoryName = clickedName;
                movePopupSelectedCategory = null;
                rebuildMovePopupItems();
            } else {
                movePopupSelectedCategory = clickedName;
            }
        }
        needsFieldRefresh = true;
    }

    private void confirmMovePopup() {
        String targetName = movePopupSelectedCategory != null ? movePopupSelectedCategory : movePopupCurrentCategoryName;
        if (targetName == null || moveSourceType == null) {
            closeMovePopup();
            return;
        }
        switch (moveSourceType) {
            case MOB_RULE -> moveMobRuleTreeItem(moveRowIdx, targetName);
            case LOOT -> moveLootTreeItem(moveRowIdx, targetName);
            case GEAR_WEAPON -> moveGearTreeItem(moveRowIdx, targetName, true);
            case GEAR_ARMOR -> moveGearTreeItem(moveRowIdx, targetName, false);
        }
        closeMovePopup();
    }

    private void moveMobRuleTreeItem(int rowIdx, String targetCategoryName) {
        if (rowIdx < 0 || rowIdx >= mobRuleTreeItems.size()) return;
        TreeItem item = mobRuleTreeItems.get(rowIdx);
        RPGMobsConfig.MobRuleCategory sourceCat = ensureCurrentMobRuleCategory();
        RPGMobsConfig.MobRuleCategory targetCat = findMobRuleCategoryByName(getMobRuleCategoryRoot(), targetCategoryName);
        if (targetCat == null || targetCat == sourceCat) return;
        if (item.isCategory) {
            RPGMobsConfig.MobRuleCategory childToMove = null;
            for (RPGMobsConfig.MobRuleCategory child : sourceCat.children) {
                if (child.name.equals(item.name)) {
                    childToMove = child;
                    break;
                }
            }
            if (childToMove == null) return;
            var descendantNames = new HashSet<String>();
            collectMobRuleCategoryNames(childToMove, descendantNames);
            if (descendantNames.contains(targetCategoryName)) return;
            sourceCat.children.remove(childToMove);
            targetCat.children.add(childToMove);
        } else {
            if (!sourceCat.mobRuleKeys.remove(item.name)) return;
            targetCat.mobRuleKeys.add(item.name);
        }
        needsFieldRefresh = true;
    }

    private void moveLootTreeItem(int rowIdx, String targetCategoryName) {
        if (rowIdx < 0 || rowIdx >= lootTreeItems.size()) return;
        TreeItem item = lootTreeItems.get(rowIdx);
        RPGMobsConfig.LootTemplateCategory sourceCat = ensureCurrentLootCategory();
        RPGMobsConfig.LootTemplateCategory targetCat = findLootCategoryByName(getLootCategoryRoot(), targetCategoryName);
        if (targetCat == null || targetCat == sourceCat) return;
        if (item.isCategory) {
            RPGMobsConfig.LootTemplateCategory childToMove = null;
            for (RPGMobsConfig.LootTemplateCategory child : sourceCat.children) {
                if (child.name.equals(item.name)) {
                    childToMove = child;
                    break;
                }
            }
            if (childToMove == null) return;
            var descendantNames = new HashSet<String>();
            collectLootCategoryNames(childToMove, descendantNames);
            if (descendantNames.contains(targetCategoryName)) return;
            sourceCat.children.remove(childToMove);
            targetCat.children.add(childToMove);
        } else {
            if (!sourceCat.templateKeys.remove(item.name)) return;
            targetCat.templateKeys.add(item.name);
        }
        needsFieldRefresh = true;
    }

    private void moveGearTreeItem(int rowIdx, String targetCategoryName, boolean isWeapon) {
        List<TreeItem> items = isWeapon ? wpnCatTreeItems : armCatTreeItems;
        if (rowIdx < 0 || rowIdx >= items.size()) return;
        TreeItem item = items.get(rowIdx);
        RPGMobsConfig.GearCategory sourceCat = ensureCurrentGearCat(isWeapon);
        RPGMobsConfig.GearCategory root = isWeapon ? editWeaponCategoryTree : editArmorCategoryTree;
        RPGMobsConfig.GearCategory targetCat = MobRuleCategoryHelpers.findGearCategoryByName(root, targetCategoryName);
        if (targetCat == null || targetCat == sourceCat) return;
        if (item.isCategory) {
            RPGMobsConfig.GearCategory childToMove = null;
            for (RPGMobsConfig.GearCategory child : sourceCat.children) {
                if (child.name.equals(item.name)) {
                    childToMove = child;
                    break;
                }
            }
            if (childToMove == null) return;
            var descendantNames = new HashSet<String>();
            MobRuleCategoryHelpers.collectGearCategoryNames(childToMove, descendantNames);
            if (descendantNames.contains(targetCategoryName)) return;
            sourceCat.children.remove(childToMove);
            targetCat.children.add(childToMove);
        } else {
            if (!sourceCat.itemKeys.remove(item.name)) return;
            targetCat.itemKeys.add(item.name);
        }
        needsFieldRefresh = true;
    }

    private Set<String> getMovingCategoryExcluded() {
        if (moveRowIdx < 0) return Set.of();
        if (moveSourceType == MoveSourceType.MOB_RULE) {
            if (moveRowIdx >= mobRuleTreeItems.size()) return Set.of();
            TreeItem item = mobRuleTreeItems.get(moveRowIdx);
            if (!item.isCategory) return Set.of();
            RPGMobsConfig.MobRuleCategory movingCat = findMobRuleCategoryByName(getMobRuleCategoryRoot(), item.name);
            if (movingCat == null) return Set.of();
            var excluded = new HashSet<String>();
            collectMobRuleCategoryNames(movingCat, excluded);
            return excluded;
        } else if (moveSourceType == MoveSourceType.LOOT) {
            if (moveRowIdx >= lootTreeItems.size()) return Set.of();
            TreeItem item = lootTreeItems.get(moveRowIdx);
            if (!item.isCategory) return Set.of();
            RPGMobsConfig.LootTemplateCategory movingCat = findLootCategoryByName(getLootCategoryRoot(), item.name);
            if (movingCat == null) return Set.of();
            var excluded = new HashSet<String>();
            collectLootCategoryNames(movingCat, excluded);
            return excluded;
        } else {
            boolean isWeapon = moveSourceType == MoveSourceType.GEAR_WEAPON;
            List<TreeItem> items = isWeapon ? wpnCatTreeItems : armCatTreeItems;
            if (moveRowIdx >= items.size()) return Set.of();
            TreeItem item = items.get(moveRowIdx);
            if (!item.isCategory) return Set.of();
            RPGMobsConfig.GearCategory root = isWeapon ? editWeaponCategoryTree : editArmorCategoryTree;
            RPGMobsConfig.GearCategory movingCat = MobRuleCategoryHelpers.findGearCategoryByName(root, item.name);
            if (movingCat == null) return Set.of();
            var excluded = new HashSet<String>();
            MobRuleCategoryHelpers.collectGearCategoryNames(movingCat, excluded);
            return excluded;
        }
    }

    private void collectMobRuleCategoryNames(RPGMobsConfig.MobRuleCategory cat, Set<String> names) {
        names.add(cat.name);
        for (RPGMobsConfig.MobRuleCategory child : cat.children) {
            collectMobRuleCategoryNames(child, names);
        }
    }

    private void collectLootCategoryNames(RPGMobsConfig.LootTemplateCategory cat, Set<String> names) {
        names.add(cat.name);
        for (RPGMobsConfig.LootTemplateCategory child : cat.children) {
            collectLootCategoryNames(child, names);
        }
    }

    private void rebuildMovePopupItems() {
        movePopupCategoryNames.clear();
        Set<String> excluded = getMovingCategoryExcluded();
        if (moveSourceType == MoveSourceType.MOB_RULE) {
            RPGMobsConfig.MobRuleCategory cat = findMobRuleCategoryByName(getMobRuleCategoryRoot(), movePopupCurrentCategoryName);
            if (cat == null) return;
            if (!excluded.contains(cat.name)) movePopupCategoryNames.add(cat.name);
            for (RPGMobsConfig.MobRuleCategory child : cat.children) {
                if (!excluded.contains(child.name)) movePopupCategoryNames.add(child.name);
            }
        } else if (moveSourceType == MoveSourceType.LOOT) {
            RPGMobsConfig.LootTemplateCategory cat = findLootCategoryByName(getLootCategoryRoot(), movePopupCurrentCategoryName);
            if (cat == null) return;
            if (!excluded.contains(cat.name)) movePopupCategoryNames.add(cat.name);
            for (RPGMobsConfig.LootTemplateCategory child : cat.children) {
                if (!excluded.contains(child.name)) movePopupCategoryNames.add(child.name);
            }
        } else {
            boolean isWeapon = moveSourceType == MoveSourceType.GEAR_WEAPON;
            RPGMobsConfig.GearCategory root = isWeapon ? editWeaponCategoryTree : editArmorCategoryTree;
            RPGMobsConfig.GearCategory cat = MobRuleCategoryHelpers.findGearCategoryByName(root, movePopupCurrentCategoryName);
            if (cat == null) return;
            if (!excluded.contains(cat.name)) movePopupCategoryNames.add(cat.name);
            for (RPGMobsConfig.GearCategory child : cat.children) {
                if (!excluded.contains(child.name)) movePopupCategoryNames.add(child.name);
            }
        }
        if (movePopupSelectedCategory != null && !movePopupCategoryNames.contains(movePopupSelectedCategory)) {
            movePopupSelectedCategory = null;
        }
        if (movePopupSelectedCategory == null && !movePopupCategoryNames.isEmpty()) {
            movePopupSelectedCategory = movePopupCategoryNames.getFirst();
        }
    }

    private String buildMovePopupBreadcrumb() {
        if (moveSourceType == MoveSourceType.MOB_RULE) {
            RPGMobsConfig.MobRuleCategory cat = findMobRuleCategoryByName(getMobRuleCategoryRoot(), movePopupCurrentCategoryName);
            if (cat == null) return "> All";
            var path = new ArrayList<String>();
            path.add(getMobRuleCategoryRoot().name);
            if (cat != getMobRuleCategoryRoot()) {
                buildBreadcrumbPath(getMobRuleCategoryRoot(), cat, path);
            }
            return "> " + String.join(" > ", path);
        } else {
            RPGMobsConfig.LootTemplateCategory cat = findLootCategoryByName(getLootCategoryRoot(), movePopupCurrentCategoryName);
            if (cat == null) return "> All";
            var path = new ArrayList<String>();
            path.add(getLootCategoryRoot().name);
            if (cat != getLootCategoryRoot()) {
                buildLootBreadcrumbPath(getLootCategoryRoot(), cat, path);
            }
            return "> " + String.join(" > ", path);
        }
    }

    private void populateMovePopup(UICommandBuilder c) {
        c.set("#MovePopupNavBack.Visible", !movePopupNavHistory.isEmpty());
        c.set("#MovePopupBreadcrumbText.Text", buildMovePopupBreadcrumb());

        boolean empty = movePopupCategoryNames.isEmpty();
        c.set("#MovePopupEmpty.Visible", empty);

        for (int i = 0; i < LINK_POPUP_ROW_COUNT; i++) {
            if (i < movePopupCategoryNames.size()) {
                String catName = movePopupCategoryNames.get(i);
                boolean isSelected = catName.equals(movePopupSelectedCategory);
                c.set("#MovePopupRow" + i + ".Visible", true);
                c.set("#MovePopupRowBtn" + i + ".Visible", true);
                boolean isCurrentLevel = (i == 0);
                String prefix = isCurrentLevel ? "" : "[>] ";
                String suffix = isSelected ? " *" : "";
                c.set("#MovePopupRowBtn" + i + ".Text", prefix + catName + suffix);
            } else {
                c.set("#MovePopupRow" + i + ".Visible", false);
            }
        }

        String selectedLabel = movePopupSelectedCategory != null
                ? "Move to: " + movePopupSelectedCategory
                : "Click a category to select it";
        c.set("#MovePopupSelected.Text", selectedLabel);
    }

    private RPGMobsConfig.@Nullable MobRuleCategory findMobRuleCategoryByName(RPGMobsConfig.MobRuleCategory root, String name) {
        if (root.name.equals(name)) return root;
        for (RPGMobsConfig.MobRuleCategory child : root.children) {
            RPGMobsConfig.MobRuleCategory found = findMobRuleCategoryByName(child, name);
            if (found != null) return found;
        }
        return null;
    }

    private RPGMobsConfig.@Nullable LootTemplateCategory findLootCategoryByName(RPGMobsConfig.LootTemplateCategory root, String name) {
        if (root.name.equals(name)) return root;
        for (RPGMobsConfig.LootTemplateCategory child : root.children) {
            RPGMobsConfig.LootTemplateCategory found = findLootCategoryByName(child, name);
            if (found != null) return found;
        }
        return null;
    }

    private void collectAllMobRuleKeys(RPGMobsConfig.MobRuleCategory cat, Set<String> keys) {
        keys.addAll(cat.mobRuleKeys);
        for (RPGMobsConfig.MobRuleCategory child : cat.children) {
            collectAllMobRuleKeys(child, keys);
        }
    }

    private void openNpcPicker() {
        npcPickerMode = NpcPickerMode.MOB_RULE;
        npcPickerOpen = true;
        npcPickerFilter = "";
        npcPickerCustomId = "";
        npcPickerPage = 0;
        npcPickerSelectedItem = null;
        rebuildNpcPickerFiltered();
        needsFieldRefresh = true;
    }

    private void closeNpcPicker() {
        npcPickerOpen = false;
        npcPickerFilter = "";
        npcPickerCustomId = "";
        npcPickerPage = 0;
        npcPickerSelectedItem = null;
        npcPickerFiltered.clear();
        npcPickerHiddenCount = 0;
    }

    private void applyNpcPickerSelection() {
        if (npcPickerSelectedItem == null || npcPickerSelectedItem.isBlank()) return;
        String selected = npcPickerSelectedItem.trim();
        if (npcPickerMode == NpcPickerMode.ABILITY_LINKED_MOB) {
            if (editOverlay != null && abilityExpandedIndex >= 0 && abilityExpandedIndex < abilTreeFiltered.size()) {
                String abilId = abilTreeFiltered.get(abilityExpandedIndex);
                ensureAbilityOverlayWithLinkedEntries(abilId);
                var ao = editOverlay.abilityOverlays.get(abilId);
                if (ao != null && ao.linkedEntries != null) {
                    boolean exists = ao.linkedEntries.stream().anyMatch(e -> e.key.equals(selected));
                    if (!exists) {
                        ao.linkedEntries.add(new ConfigOverlay.AbilityLinkedEntry(
                                selected, new boolean[]{true, true, true, true, true}));
                    }
                }
            }
        } else if (npcPickerMode == NpcPickerMode.LOOT_TEMPLATE_LINKED_MOB) {
            RPGMobsConfig.LootTemplate template = getExpandedLootTemplate();
            if (template != null && !template.linkedMobRuleKeys.contains(selected)) {
                template.linkedMobRuleKeys.add(selected);
                rebuildLootTplMobFiltered();
            }
        } else if (npcPickerMode == NpcPickerMode.TIER_RESTRICTION) {
            addTierRestrictionKey(selected);
        } else if (npcPickerMode == NpcPickerMode.ABILITY_EXCLUDED_MOB) {
            RPGMobsConfig.AbilityConfig ac = getExpandedAbilityConfig();
            if (ac != null) {
                if (ac.excludeLinkedMobRuleKeys == null) ac.excludeLinkedMobRuleKeys = new ArrayList<>();
                if (!(ac.excludeLinkedMobRuleKeys instanceof ArrayList))
                    ac.excludeLinkedMobRuleKeys = new ArrayList<>(ac.excludeLinkedMobRuleKeys);
                if (!ac.excludeLinkedMobRuleKeys.contains(selected)) {
                    ac.excludeLinkedMobRuleKeys.add(selected);
                }
            }
        } else if (npcPickerMode == NpcPickerMode.SUMMON_EXCLUDE_FROM_POOL) {
            RPGMobsConfig.AbilityConfig ac = getExpandedAbilityConfig();
            if (ac instanceof RPGMobsConfig.SummonAbilityConfig sm) {
                if (sm.excludeFromSummonPool == null) sm.excludeFromSummonPool = new ArrayList<>();
                if (!(sm.excludeFromSummonPool instanceof ArrayList))
                    sm.excludeFromSummonPool = new ArrayList<>(sm.excludeFromSummonPool);
                if (!sm.excludeFromSummonPool.contains(selected)) {
                    sm.excludeFromSummonPool.add(selected);
                }
            }
        } else if (npcPickerMode == NpcPickerMode.REBIND_MOB_RULE) {
            if (mobRuleTreeExpandedIndex >= 0 && mobRuleTreeExpandedIndex < mobRuleTreeItems.size()) {
                TreeItem item = mobRuleTreeItems.get(mobRuleTreeExpandedIndex);
                if (!item.isCategory && !item.name.equals(selected)) {
                    String oldName = item.name;
                    MobRuleCategoryHelpers.renameMobRuleKeyRecursive(getMobRuleCategoryRoot(), oldName, selected);
                    RPGMobsConfig.MobRule rule = editMobRules.remove(oldName);
                    if (rule != null) {
                        editMobRules.put(selected, rule);
                    }
                    mobRuleTreeItems.set(mobRuleTreeExpandedIndex, new TreeItem(selected, false));
                    propagateMobRuleKeyRenameToAbilityConfigs(oldName, selected);
                    if (!mobRuleTreeFilter.isEmpty()) {
                        rebuildMobRuleTreeFiltered();
                    }
                }
            }
        } else {
            RPGMobsConfig.MobRuleCategory cat = ensureCurrentMobRuleCategory();
            cat.mobRuleKeys.add(selected);
            ensureGlobMobRuleExists(selected);
        }
        closeNpcPicker();
        needsFieldRefresh = true;
    }

    private void handleNpcPickerRowClick(int rowIdx) {
        int actualIdx = npcPickerPage * NPC_PICKER_ROW_COUNT + rowIdx;
        if (actualIdx < 0 || actualIdx >= npcPickerFiltered.size()) return;
        npcPickerSelectedItem = npcPickerFiltered.get(actualIdx);
        needsFieldRefresh = true;
    }

    private void handleNpcPickerUseCustom() {
        if (npcPickerCustomId != null && !npcPickerCustomId.isBlank()) {
            npcPickerSelectedItem = npcPickerCustomId.trim();
            needsFieldRefresh = true;
        }
    }

    private void rebuildNpcPickerFiltered() {
        npcPickerFiltered.clear();

        boolean useMobRuleKeysAsSource =
                npcPickerMode == NpcPickerMode.LOOT_TEMPLATE_LINKED_MOB
                || npcPickerMode == NpcPickerMode.ABILITY_LINKED_MOB
                || npcPickerMode == NpcPickerMode.ABILITY_EXCLUDED_MOB
                || npcPickerMode == NpcPickerMode.SUMMON_EXCLUDE_FROM_POOL
                || npcPickerMode == NpcPickerMode.TIER_RESTRICTION;

        Set<String> alreadyLinked = new HashSet<>();
        if (npcPickerMode == NpcPickerMode.TIER_RESTRICTION) {
            for (String key : activeTierOverrideKeys) {
                if (!MobRuleCategoryHelpers.isCategoryKey(key)) alreadyLinked.add(key);
            }
        } else if (npcPickerMode == NpcPickerMode.ABILITY_LINKED_MOB) {
            if (abilityExpandedIndex >= 0 && abilityExpandedIndex < abilTreeFiltered.size()) {
                for (String key : getEffectiveLinkedMobKeys(abilTreeFiltered.get(abilityExpandedIndex))) {
                    if (!MobRuleCategoryHelpers.isCategoryKey(key)) alreadyLinked.add(key);
                }
            }
        } else if (npcPickerMode == NpcPickerMode.LOOT_TEMPLATE_LINKED_MOB) {
            RPGMobsConfig.LootTemplate template = getExpandedLootTemplate();
            if (template != null) {
                for (String key : template.linkedMobRuleKeys) {
                    if (!MobRuleCategoryHelpers.isCategoryKey(key)) alreadyLinked.add(key);
                }
            }
        } else if (npcPickerMode == NpcPickerMode.ABILITY_EXCLUDED_MOB) {
            RPGMobsConfig.AbilityConfig ac = getExpandedAbilityConfig();
            if (ac != null && ac.excludeLinkedMobRuleKeys != null) {
                alreadyLinked.addAll(ac.excludeLinkedMobRuleKeys);
            }
        } else if (npcPickerMode == NpcPickerMode.SUMMON_EXCLUDE_FROM_POOL) {
            RPGMobsConfig.AbilityConfig ac = getExpandedAbilityConfig();
            if (ac instanceof RPGMobsConfig.SummonAbilityConfig sm && sm.excludeFromSummonPool != null) {
                alreadyLinked.addAll(sm.excludeFromSummonPool);
            }
        } else if (npcPickerMode == NpcPickerMode.REBIND_MOB_RULE) {
            collectAllMobRuleKeys(getMobRuleCategoryRoot(), alreadyLinked);
            if (mobRuleTreeExpandedIndex >= 0 && mobRuleTreeExpandedIndex < mobRuleTreeItems.size()) {
                alreadyLinked.remove(mobRuleTreeItems.get(mobRuleTreeExpandedIndex).name);
            }
        } else {
            collectAllMobRuleKeys(getMobRuleCategoryRoot(), alreadyLinked);
        }

        List<String> sourceList;
        if (useMobRuleKeysAsSource) {
            Set<String> allMobRuleKeys = new LinkedHashSet<>();
            collectAllMobRuleKeys(getMobRuleCategoryRoot(), allMobRuleKeys);
            sourceList = new ArrayList<>(allMobRuleKeys);
        } else {
            sourceList = getRuntimeNpcIds();
        }

        String lowerFilter = npcPickerFilter.toLowerCase();
        int totalItems = sourceList.size();
        for (String id : sourceList) {
            if (alreadyLinked.contains(id)) continue;
            if (lowerFilter.isEmpty() || id.toLowerCase().contains(lowerFilter)) {
                npcPickerFiltered.add(id);
            }
        }
        npcPickerHiddenCount = totalItems - npcPickerFiltered.size();
        if (!lowerFilter.isEmpty()) {
            int availableWithoutFilter = 0;
            for (String id : sourceList) {
                if (!alreadyLinked.contains(id)) availableWithoutFilter++;
            }
            npcPickerHiddenCount = totalItems - availableWithoutFilter;
        }
    }

    private void populateNpcPicker(UICommandBuilder c) {
        if (needsFieldRefresh) {
            c.set("#NpcPickerFilter.Value", npcPickerFilter);
            c.set("#NpcPickerCustomId.Value", npcPickerCustomId);
        }

        boolean isMobRuleSource = npcPickerMode == NpcPickerMode.LOOT_TEMPLATE_LINKED_MOB
                || npcPickerMode == NpcPickerMode.ABILITY_LINKED_MOB
                || npcPickerMode == NpcPickerMode.ABILITY_EXCLUDED_MOB
                || npcPickerMode == NpcPickerMode.SUMMON_EXCLUDE_FROM_POOL
                || npcPickerMode == NpcPickerMode.TIER_RESTRICTION;
        if (npcPickerHiddenCount > 0) {
            String reason = npcPickerMode == NpcPickerMode.MOB_RULE
                    ? " NPCs not shown  - already have mob rules."
                    : isMobRuleSource
                            ? " mob rules not shown  - already linked."
                            : " NPCs not shown  - already linked.";
            c.set("#NpcPickerInfoLabel.Text", npcPickerHiddenCount + reason);
        } else {
            String prompt = npcPickerMode == NpcPickerMode.MOB_RULE
                    ? "Select an NPC to create a mob rule for."
                    : isMobRuleSource
                            ? "Select a mob rule to link."
                            : "Select an NPC to link.";
            c.set("#NpcPickerInfoLabel.Text", prompt);
        }

        int totalItems = npcPickerFiltered.size();
        int totalPages = Math.max(1, (totalItems + NPC_PICKER_ROW_COUNT - 1) / NPC_PICKER_ROW_COUNT);
        if (npcPickerPage >= totalPages) npcPickerPage = totalPages - 1;
        int pageStart = npcPickerPage * NPC_PICKER_ROW_COUNT;

        for (int i = 0; i < NPC_PICKER_ROW_COUNT; i++) {
            int itemIdx = pageStart + i;
            boolean vis = itemIdx < totalItems;
            c.set("#NpcPickerRow" + i + ".Visible", vis);
            if (vis) {
                String npcId = npcPickerFiltered.get(itemIdx);
                boolean selected = npcId.equals(npcPickerSelectedItem);
                c.set("#NpcPickerRowBtn" + i + ".Text", selected ? "> " + npcId : npcId);
            }
        }
        c.set("#NpcPickerEmpty.Visible", totalItems == 0);

        boolean hasSelection = npcPickerSelectedItem != null && !npcPickerSelectedItem.isBlank();
        c.set("#NpcPickerSelectedLabel.Text", hasSelection ? npcPickerSelectedItem : "None");
        c.set("#NpcPickerAdd.Visible", hasSelection);

        int npcPageEnd = Math.min((npcPickerPage + 1) * NPC_PICKER_ROW_COUNT, totalItems);
        c.set("#NpcPickerPageInfo.Text", npcPageEnd + "/" + totalItems);
        c.set("#NpcPickerPagination.Visible", true);
        boolean multiPage = totalPages > 1;
        c.set("#NpcPickerFirstPage.Visible", multiPage && npcPickerPage > 0);
        c.set("#NpcPickerPrevPage.Visible", multiPage && npcPickerPage > 0);
        c.set("#NpcPickerNextPage.Visible", multiPage && npcPickerPage < totalPages - 1);
        c.set("#NpcPickerLastPage.Visible", multiPage && npcPickerPage < totalPages - 1);
    }

    private static List<String> cachedNpcIds = null;

    private static List<String> getRuntimeNpcIds() {
        if (cachedNpcIds == null) {
            try {
                var npcPlugin = NPCPlugin.get();
                if (npcPlugin != null) {
                    var names = npcPlugin.getRoleTemplateNames(false);
                    if (names != null) {
                        var filtered = new ArrayList<String>();
                        for (var name : names) {
                            if (name.startsWith("Template_") || name.startsWith("Component_")
                                    || name.startsWith("Test_") || name.startsWith("RPGMobs_")) continue;
                            filtered.add(name);
                        }
                        filtered.sort(String.CASE_INSENSITIVE_ORDER);
                        cachedNpcIds = filtered;
                    }
                }
            } catch (Exception e) {
                LOGGER.atWarning().log("Failed to enumerate NPCs at runtime: %s", e.getMessage());
            }
            if (cachedNpcIds == null) cachedNpcIds = List.of();
        }
        return cachedNpcIds;
    }

    private static List<String> buildDroppableItemList() {
        try {
            var itemMap = Item.getAssetMap();
            if (itemMap != null) {
                var keys = new ArrayList<>(itemMap.getAssetMap().keySet());
                keys.sort(String.CASE_INSENSITIVE_ORDER);
                return keys;
            }
        } catch (Exception e) {
            LOGGER.atWarning().log("Failed to enumerate items at runtime: %s", e.getMessage());
        }
        return List.of();
    }

    private static List<String> getAllDroppableItems() {
        if (allDroppableItems == null) {
            allDroppableItems = buildDroppableItemList();
        }
        return allDroppableItems;
    }

    private void openItemPicker(int dropSlot) {
        RPGMobsConfig.LootTemplate template = getExpandedLootTemplate();
        if (template == null) return;
        int dropIdx = lootTemplateDropPage * AdminUIData.LOOT_TEMPLATE_DROPS_PER_PAGE + dropSlot;
        if (dropIdx >= template.drops.size()) return;

        itemPickerOpen = true;
        itemPickerDropSlot = dropSlot;
        itemPickerFilter = "";
        itemPickerCustomId = "";
        itemPickerPage = 0;
        itemPickerSelectedItem = template.drops.get(dropIdx).itemId;
        rebuildItemPickerFiltered();
        needsFieldRefresh = true;
    }

    private void closeItemPicker() {
        itemPickerOpen = false;
        itemPickerDropSlot = -1;
        itemPickerFilter = "";
        itemPickerCustomId = "";
        itemPickerPage = 0;
        itemPickerSelectedItem = null;
        gearCatSwapOldKey = null;
        itemPickerFiltered.clear();
    }

    private void applyItemPickerSelection() {
        if (itemPickerSelectedItem == null || itemPickerSelectedItem.isBlank()) return;
        String selectedItem = itemPickerSelectedItem.trim();
        if (itemPickerDropSlot == ITEM_PICKER_GEAR_WEAPON_ADD || itemPickerDropSlot == ITEM_PICKER_GEAR_ARMOR_ADD) {
            boolean isWeapon = itemPickerDropSlot == ITEM_PICKER_GEAR_WEAPON_ADD;
            RPGMobsConfig.GearCategory cat = ensureCurrentGearCat(isWeapon);
            if (!cat.itemKeys.contains(selectedItem)) {
                cat.itemKeys.add(selectedItem);
            }
            closeItemPicker();
            needsFieldRefresh = true;
            return;
        }
        if (itemPickerDropSlot == ITEM_PICKER_GEAR_WEAPON_SWAP || itemPickerDropSlot == ITEM_PICKER_GEAR_ARMOR_SWAP) {
            boolean isWeapon = itemPickerDropSlot == ITEM_PICKER_GEAR_WEAPON_SWAP;
            RPGMobsConfig.GearCategory cat = ensureCurrentGearCat(isWeapon);
            if (gearCatSwapOldKey != null) {
                int position = cat.itemKeys.indexOf(gearCatSwapOldKey);
                if (position >= 0) {
                    cat.itemKeys.set(position, selectedItem);
                }
            }
            gearCatSwapOldKey = null;
            closeItemPicker();
            needsFieldRefresh = true;
            return;
        }
        if (itemPickerDropSlot == ITEM_PICKER_ABILITY_DRINK) {
            RPGMobsConfig.AbilityConfig ac = getExpandedAbilityConfig();
            if (ac instanceof RPGMobsConfig.HealLeapAbilityConfig hl) {
                hl.npcDrinkItemId = selectedItem;
            }
            closeItemPicker();
            needsFieldRefresh = true;
            return;
        }
        if (itemPickerDropSlot == ITEM_PICKER_MOB_RULE_WPN || itemPickerDropSlot == ITEM_PICKER_MOB_RULE_ARM) {
            RPGMobsConfig.MobRule rule = getExpandedGlobMobRule();
            if (rule == null) { closeItemPicker(); return; }
            boolean isWeapon = itemPickerDropSlot == ITEM_PICKER_MOB_RULE_WPN;
            List<String> list = isWeapon ? rule.allowedWeaponCategories : rule.allowedArmorCategories;
            if (!(list instanceof ArrayList)) {
                list = new ArrayList<>(list);
                if (isWeapon) rule.allowedWeaponCategories = list; else rule.allowedArmorCategories = list;
            }
            if (!list.contains(selectedItem)) {
                list.add(selectedItem);
            }
            closeItemPicker();
            needsFieldRefresh = true;
            return;
        }
        RPGMobsConfig.LootTemplate template = getExpandedLootTemplate();
        if (template == null) return;
        int dropIdx = lootTemplateDropPage * AdminUIData.LOOT_TEMPLATE_DROPS_PER_PAGE + itemPickerDropSlot;
        if (dropIdx >= template.drops.size()) return;

        template.drops.get(dropIdx).itemId = selectedItem;
        closeItemPicker();
        needsFieldRefresh = true;
    }

    private void handleItemPickerRowClick(int rowIdx) {
        int actualIdx = itemPickerPage * ITEM_PICKER_ROW_COUNT + rowIdx;
        if (actualIdx < 0 || actualIdx >= itemPickerFiltered.size()) return;
        itemPickerSelectedItem = itemPickerFiltered.get(actualIdx);
        needsFieldRefresh = true;
    }

    private void handleItemPickerUseCustom() {
        if (itemPickerCustomId != null && !itemPickerCustomId.isBlank()) {
            itemPickerSelectedItem = itemPickerCustomId.trim();
            needsFieldRefresh = true;
        }
    }

    private void rebuildItemPickerFiltered() {
        itemPickerFiltered.clear();
        String lowerFilter = itemPickerFilter.toLowerCase();
        List<String> source = getItemPickerSourceList();
        for (String id : source) {
            if (lowerFilter.isEmpty() || id.toLowerCase().contains(lowerFilter)) {
                itemPickerFiltered.add(id);
            }
        }
    }

    private List<String> getItemPickerSourceList() {
        if (itemPickerDropSlot == ITEM_PICKER_MOB_RULE_WPN) {
            return MobRuleCategoryHelpers.collectAllGearItemKeys(editWeaponCategoryTree);
        } else if (itemPickerDropSlot == ITEM_PICKER_MOB_RULE_ARM) {
            return MobRuleCategoryHelpers.collectAllGearItemKeys(editArmorCategoryTree);
        }
        return getAllDroppableItems();
    }

    private void populateItemPicker(UICommandBuilder c) {
        if (needsFieldRefresh) {
            c.set("#ItemPickerFilter.Value", itemPickerFilter);
            c.set("#ItemPickerCustomId.Value", itemPickerCustomId);
        }

        int totalItems = itemPickerFiltered.size();
        int totalPages = Math.max(1, (totalItems + ITEM_PICKER_ROW_COUNT - 1) / ITEM_PICKER_ROW_COUNT);
        if (itemPickerPage >= totalPages) itemPickerPage = totalPages - 1;
        int pageStart = itemPickerPage * ITEM_PICKER_ROW_COUNT;

        for (int i = 0; i < ITEM_PICKER_ROW_COUNT; i++) {
            int itemIdx = pageStart + i;
            boolean vis = itemIdx < totalItems;
            c.set("#ItemPickerRow" + i + ".Visible", vis);
            if (vis) {
                String itemId = itemPickerFiltered.get(itemIdx);
                c.set("#ItemPickerRowIcon" + i + ".ItemId", itemId);
                boolean selected = itemId.equals(itemPickerSelectedItem);
                c.set("#ItemPickerRowBtn" + i + ".Text", selected ? "> " + itemId : itemId);
            }
        }
        c.set("#ItemPickerEmpty.Visible", totalItems == 0);

        boolean hasSelection = itemPickerSelectedItem != null && !itemPickerSelectedItem.isBlank();
        c.set("#ItemPickerSelectedLabel.Text", hasSelection ? itemPickerSelectedItem : "None");
        c.set("#ItemPickerSelectedLabel.Visible", true);
        c.set("#ItemPickerAdd.Visible", hasSelection);

        int itemPageEnd = Math.min((itemPickerPage + 1) * ITEM_PICKER_ROW_COUNT, totalItems);
        c.set("#ItemPickerPageInfo.Text", itemPageEnd + "/" + totalItems);
        c.set("#ItemPickerPagination.Visible", true);
        boolean multiPage = totalPages > 1;
        c.set("#ItemPickerFirstPage.Visible", multiPage && itemPickerPage > 0);
        c.set("#ItemPickerPrevPage.Visible", multiPage && itemPickerPage > 0);
        c.set("#ItemPickerNextPage.Visible", multiPage && itemPickerPage < totalPages - 1);
        c.set("#ItemPickerLastPage.Visible", multiPage && itemPickerPage < totalPages - 1);
    }

    private void renderCategoryPeekPopup(UICommandBuilder c) {
        c.set("#CategoryPeekPopup.Visible", categoryPeekOpen);
        if (!categoryPeekOpen) return;

        c.set("#CatPeekTitle.Text", categoryPeekTitle);

        int totalItems = categoryPeekItems.size();
        int totalPages = Math.max(1, (totalItems + CATEGORY_PEEK_PAGE_SIZE - 1) / CATEGORY_PEEK_PAGE_SIZE);
        if (categoryPeekPage >= totalPages) categoryPeekPage = totalPages - 1;
        int pageStart = categoryPeekPage * CATEGORY_PEEK_PAGE_SIZE;

        for (int i = 0; i < CATEGORY_PEEK_PAGE_SIZE; i++) {
            int itemIndex = pageStart + i;
            boolean rowVis = itemIndex < totalItems;
            c.set("#CatPeekRow" + i + ".Visible", rowVis);
            if (rowVis) {
                String itemId = categoryPeekItems.get(itemIndex);
                c.set("#CatPeekItem" + i + ".Text", itemId);
                c.set("#CatPeekIcon" + i + ".Visible", true);
                c.set("#CatPeekIcon" + i + ".ItemId", itemId);
            } else {
                c.set("#CatPeekIcon" + i + ".Visible", false);
            }
        }

        c.set("#CatPeekEmpty.Visible", totalItems == 0);

        boolean multiPage = totalPages > 1;
        c.set("#CatPeekPagination.Visible", multiPage);
        if (multiPage) {
            int pageEnd = Math.min((categoryPeekPage + 1) * CATEGORY_PEEK_PAGE_SIZE, totalItems);
            c.set("#CatPeekPageInfo.Text", pageEnd + "/" + totalItems);
            c.set("#CatPeekFirstPage.Visible", categoryPeekPage > 0);
            c.set("#CatPeekPrevPage.Visible", categoryPeekPage > 0);
            c.set("#CatPeekNextPage.Visible", categoryPeekPage < totalPages - 1);
            c.set("#CatPeekLastPage.Visible", categoryPeekPage < totalPages - 1);
        }

        boolean canExtract = categoryPeekSource != CatPeekSource.NONE && totalItems > 0;
        c.set("#CatPeekExtract.Visible", canExtract);
        c.set("#CatPeekExtractInfo.Visible", canExtract);
        if (canExtract) {
            String extractLabel = switch (categoryPeekSource) {
                case WEAPON_CAT, ABILITY_WEAPON_GATE -> "Extract Weapons";
                case ARMOR_CAT -> "Extract Armor";
                case ABILITY_LINKED, LOOT_TPL_LINKED, TIER_OVERRIDE -> "Extract Mobs";
                default -> "Extract Items";
            };
            c.set("#CatPeekExtract.Text", extractLabel);
            String infoLabel = switch (categoryPeekSource) {
                case WEAPON_CAT, ABILITY_WEAPON_GATE -> "Replace the category with its individual weapons.";
                case ARMOR_CAT -> "Replace the category with its individual armor pieces.";
                case ABILITY_LINKED, LOOT_TPL_LINKED, TIER_OVERRIDE -> "Replace the category with its individual mob rules.";
                default -> "Replace the category with its individual items.";
            };
            c.set("#CatPeekExtractInfo.Text", infoLabel);
        }
    }

    private void handleRarityAllowedToggle(String id) {
        if (id.length() < 4) return;
        int t = id.charAt(1) - '0';
        int r = id.charAt(3) - '0';
        if (t < 0 || t >= 5 || r < 0 || r >= 5) return;
        editTierAllowedRarities[t][r] = !editTierAllowedRarities[t][r];
    }

    private void handleTwoHandedAdd() {
        renamePopupOpen = true;
        renameTarget = RenameTarget.WEAPON_CATEGORY;
        renameRowIndex = RENAME_IDX_TWO_HANDED_ADD;
        pendingRenameName = "";
        needsFieldRefresh = true;
    }

    private void handleTwoHandedDel(int slotIdx) {
        int absIdx = twoHandedPage * TWO_HANDED_PAGE_SIZE + slotIdx;
        if (absIdx < 0 || absIdx >= editTwoHandedKeywords.size()) return;
        editTwoHandedKeywords.remove(absIdx);
        int maxPage = Math.max(0, (editTwoHandedKeywords.size() - 1) / TWO_HANDED_PAGE_SIZE);
        if (twoHandedPage > maxPage) twoHandedPage = maxPage;
    }

    private void handleRarityRuleAdd(boolean isWeapon) {
        renamePopupOpen = true;
        renameTarget = isWeapon ? RenameTarget.WEAPON_CATEGORY : RenameTarget.ARMOR_CATEGORY;
        renameRowIndex = RENAME_IDX_RARITY_RULE_ADD;
        pendingRenameName = "";
        needsFieldRefresh = true;
    }

    private void handleRarityRuleCycle(int slotIdx, boolean isWeapon) {
        List<String> keys = isWeapon ? weaponRarityRuleKeys : armorRarityRuleKeys;
        Map<String, String> rules = isWeapon ? editWeaponRarityRules : editArmorRarityRules;
        int page = isWeapon ? weaponRarityPage : armorRarityPage;
        int absIdx = page * RARITY_RULES_PAGE_SIZE + slotIdx;
        if (absIdx < 0 || absIdx >= keys.size()) return;
        String key = keys.get(absIdx);
        String current = rules.getOrDefault(key, "common");
        int curIdx = rarityIndex(current);
        int nextIdx = (curIdx + 1) % RARITY_NAMES.length;
        rules.put(key, RARITY_NAMES[nextIdx]);
    }

    private void handleRarityRuleDel(int slotIdx, boolean isWeapon) {
        List<String> keys = isWeapon ? weaponRarityRuleKeys : armorRarityRuleKeys;
        Map<String, String> rules = isWeapon ? editWeaponRarityRules : editArmorRarityRules;
        int page = isWeapon ? weaponRarityPage : armorRarityPage;
        int absIdx = page * RARITY_RULES_PAGE_SIZE + slotIdx;
        if (absIdx < 0 || absIdx >= keys.size()) return;
        String key = keys.get(absIdx);
        rules.remove(key);
        keys.remove(absIdx);
        int maxPage = Math.max(0, (keys.size() - 1) / RARITY_RULES_PAGE_SIZE);
        if (isWeapon) {
            if (weaponRarityPage > maxPage) weaponRarityPage = maxPage;
        } else {
            if (armorRarityPage > maxPage) armorRarityPage = maxPage;
        }
    }

    private void addTwoHandedKeyword(String keyword) {
        if (editTwoHandedKeywords.contains(keyword)) return;
        editTwoHandedKeywords.add(keyword);
        twoHandedPage = Math.max(0, (editTwoHandedKeywords.size() - 1) / TWO_HANDED_PAGE_SIZE);
    }

    private void addRarityRule(String fragment, boolean isWeapon) {
        Map<String, String> rules = isWeapon ? editWeaponRarityRules : editArmorRarityRules;
        List<String> keys = isWeapon ? weaponRarityRuleKeys : armorRarityRuleKeys;
        if (rules.containsKey(fragment)) return;
        rules.put(fragment, "common");
        keys.add(fragment);
        if (isWeapon) {
            weaponRarityPage = Math.max(0, (keys.size() - 1) / RARITY_RULES_PAGE_SIZE);
        } else {
            armorRarityPage = Math.max(0, (keys.size() - 1) / RARITY_RULES_PAGE_SIZE);
        }
    }

    private void parseRarityTiersTextFields(AdminUIData data) {
        for (int i = 0; i < 5; i++) {
            editArmorPiecesPerTier[i] = parseIntField(data.rarityArmorPieces[i], editArmorPiecesPerTier[i]);
            editShieldChancePerTier[i] = parseDoubleField(data.rarityShieldChance[i], editShieldChancePerTier[i]);
        }
        for (int t = 0; t < 5; t++) {
            for (int r = 0; r < 5; r++) {
                editTierRarityWeights[t][r] = parseDoubleField(data.rarityWt[t][r], editTierRarityWeights[t][r]);
            }
        }
    }

    private static double parseDoubleField(String text, double fallback) {
        if (text == null || text.isBlank()) return fallback;
        try { return Double.parseDouble(text.trim()); } catch (NumberFormatException e) { return fallback; }
    }

    private static int parseIntField(String text, int fallback) {
        if (text == null || text.isBlank()) return fallback;
        try { return Integer.parseInt(text.trim()); } catch (NumberFormatException e) { return fallback; }
    }

    private void renderRarityTiersTab(UICommandBuilder c) {
        if (needsFieldRefresh) {
            for (int i = 0; i < 5; i++) {
                c.set("#FieldRarityArmorPieces" + i + ".Value", String.valueOf(editArmorPiecesPerTier[i]));
                c.set("#FieldRarityShieldChance" + i + ".Value", String.valueOf(editShieldChancePerTier[i]));
            }
        }

        for (int t = 0; t < 5; t++) {
            for (int r = 0; r < 5; r++) {
                renderToggle(c, "#RarityAllowedT" + t + "R" + r, editTierAllowedRarities[t][r]);
            }
        }

        if (needsFieldRefresh) {
            for (int t = 0; t < 5; t++) {
                for (int r = 0; r < 5; r++) {
                    c.set("#FieldRarityWt" + t + r + ".Value", String.valueOf(editTierRarityWeights[t][r]));
                }
            }
        }

        int thTotal = editTwoHandedKeywords.size();
        int thStart = twoHandedPage * TWO_HANDED_PAGE_SIZE;
        for (int i = 0; i < TWO_HANDED_PAGE_SIZE; i++) {
            int absIdx = thStart + i;
            boolean vis = absIdx < thTotal;
            c.set("#TwoHandedRow" + i + ".Visible", vis);
            if (vis) c.set("#TwoHandedName" + i + ".Text", editTwoHandedKeywords.get(absIdx));
        }
        setPagination(c, thTotal, twoHandedPage, TWO_HANDED_PAGE_SIZE, "#TwoHandedPagination", "#TwoHandedPageLabel",
                "#TwoHandedFirstPage", "#TwoHandedPrevPage", "#TwoHandedNextPage", "#TwoHandedLastPage");

        renderRarityRulesSection(c, weaponRarityRuleKeys, editWeaponRarityRules, weaponRarityPage, "#WpnRarity");

        renderRarityRulesSection(c, armorRarityRuleKeys, editArmorRarityRules, armorRarityPage, "#ArmRarity");
    }

    private void renderRarityRulesSection(UICommandBuilder c, List<String> keys, Map<String, String> rules,
                                           int page, String prefix) {
        int total = keys.size();
        int start = page * RARITY_RULES_PAGE_SIZE;
        for (int i = 0; i < RARITY_RULES_PAGE_SIZE; i++) {
            int absIdx = start + i;
            boolean vis = absIdx < total;
            c.set(prefix + "Row" + i + ".Visible", vis);
            if (vis) {
                String key = keys.get(absIdx);
                c.set(prefix + "Key" + i + ".Text", key);
                c.set(prefix + "Cycle" + i + ".Text", rules.getOrDefault(key, "common"));
            }
        }
        setPagination(c, total, page, RARITY_RULES_PAGE_SIZE, prefix + "Pagination", prefix + "PageLabel",
                prefix + "FirstPage", prefix + "PrevPage", prefix + "NextPage", prefix + "LastPage");
    }

    private void snapshotCombatAI() {
        RPGMobsConfig config = plugin.getConfig();
        if (config == null) return;
        editCombatAI = deepCopyCombatAIConfig(config.combatAIConfig);
        savedCombatAI = deepCopyCombatAIConfig(config.combatAIConfig);
        assetPickerOpen = false;
    }

    private static RPGMobsConfig.CombatAIConfig deepCopyCombatAIConfig(RPGMobsConfig.CombatAIConfig src) {
        var dst = new RPGMobsConfig.CombatAIConfig();
        dst.targetMemoryDuration = src.targetMemoryDuration;
        dst.minRunUtility = src.minRunUtility;
        dst.minActionUtility = src.minActionUtility;
        dst.factionStyles = deepCopyFactionStyles(src.factionStyles);
        dst.tierBehaviors = deepCopyTierBehaviors(src.tierBehaviors);
        dst.weaponParams = deepCopyWeaponParams(src.weaponParams);
        return dst;
    }

    private static Map<String, RPGMobsConfig.FactionStyle> deepCopyFactionStyles(Map<String, RPGMobsConfig.FactionStyle> src) {
        var dst = new LinkedHashMap<String, RPGMobsConfig.FactionStyle>();
        for (var e : src.entrySet()) {
            var s = e.getValue();
            dst.put(e.getKey(), new RPGMobsConfig.FactionStyle(
                    s.attackCooldownMin, s.attackCooldownMax, s.shieldChargeFor, s.shieldSwitchPoint,
                    s.backOffDistanceMin, s.backOffDistanceMax, s.backOffSwitchPoint,
                    s.healthRetreatDistanceMin, s.healthRetreatDistanceMax, s.healthRetreatWeight,
                    s.reEngageXRangeMin, s.reEngageXRangeMax, s.reEngageRandomiserMin, s.reEngageRandomiserMax,
                    s.strafeCooldownMin, s.strafeCooldownMax, s.enableGroupObserve, s.enableFlanking, s.npcGroupName,
                    s.guardRandomiserMin, s.guardRandomiserMax, s.backOffRandomiserMin, s.backOffRandomiserMax,
                    s.retreatCooldown, s.reEngageDistanceMin, s.reEngageDistanceMax,
                    s.groupObserveDistanceMin, s.groupObserveDistanceMax, s.flankingAngle));
        }
        return dst;
    }

    private static List<RPGMobsConfig.TierBehavior> deepCopyTierBehaviors(List<RPGMobsConfig.TierBehavior> src) {
        var dst = new ArrayList<RPGMobsConfig.TierBehavior>();
        for (var s : src) {
            dst.add(new RPGMobsConfig.TierBehavior(
                    s.cooldownMin, s.cooldownMax, s.strafeCooldownMin, s.strafeCooldownMax,
                    s.hasShield, s.hasBackOff, s.hasRetreat, s.hasGroupObserve, s.hasFlanking,
                    s.shieldChargeFor, s.shieldGuardCooldown, s.retreatHealthThreshold, s.movementSpeedMultiplier));
        }
        return dst;
    }

    private static Map<String, RPGMobsConfig.WeaponCombatParams> deepCopyWeaponParams(Map<String, RPGMobsConfig.WeaponCombatParams> src) {
        var dst = new LinkedHashMap<String, RPGMobsConfig.WeaponCombatParams>();
        for (var e : src.entrySet()) {
            var s = e.getValue();
            var copy = new RPGMobsConfig.WeaponCombatParams(s.maxRange, s.speedMultiplier, s.attackRootInteraction, s.isRanged,
                    s.animationSetId, s.attackChainAnimations, s.swingSoundId, s.impactSoundId, s.weaponTrailId, s.hitParticleId,
                    s.hitboxEndDistance, s.hitboxConeLength, s.hitboxConeYaw, s.swingWindUpTime, s.swingRecoveryTime, s.combatSpeed);
            dst.put(e.getKey(), copy);
        }
        return dst;
    }

    private boolean hasCombatAIChanges() {
        if (editCombatAI == null || savedCombatAI == null) return false;
        if (editCombatAI.targetMemoryDuration != savedCombatAI.targetMemoryDuration) return true;
        if (editCombatAI.minRunUtility != savedCombatAI.minRunUtility) return true;
        if (editCombatAI.minActionUtility != savedCombatAI.minActionUtility) return true;
        for (var key : CAI_FACTION_KEYS) {
            var ef = editCombatAI.factionStyles.get(key);
            var sf = savedCombatAI.factionStyles.get(key);
            if (ef == null || sf == null) { if (ef != sf) return true; continue; }
            if (ef.attackCooldownMin != sf.attackCooldownMin || ef.attackCooldownMax != sf.attackCooldownMax) return true;
            if (ef.shieldChargeFor != sf.shieldChargeFor || ef.shieldSwitchPoint != sf.shieldSwitchPoint) return true;
            if (ef.backOffDistanceMin != sf.backOffDistanceMin || ef.backOffDistanceMax != sf.backOffDistanceMax || ef.backOffSwitchPoint != sf.backOffSwitchPoint) return true;
            if (ef.healthRetreatDistanceMin != sf.healthRetreatDistanceMin || ef.healthRetreatDistanceMax != sf.healthRetreatDistanceMax || ef.healthRetreatWeight != sf.healthRetreatWeight) return true;
            if (ef.reEngageXRangeMin != sf.reEngageXRangeMin || ef.reEngageXRangeMax != sf.reEngageXRangeMax) return true;
            if (ef.reEngageRandomiserMin != sf.reEngageRandomiserMin || ef.reEngageRandomiserMax != sf.reEngageRandomiserMax) return true;
            if (ef.strafeCooldownMin != sf.strafeCooldownMin || ef.strafeCooldownMax != sf.strafeCooldownMax) return true;
            if (ef.enableGroupObserve != sf.enableGroupObserve || ef.enableFlanking != sf.enableFlanking) return true;
            if (ef.guardRandomiserMin != sf.guardRandomiserMin || ef.guardRandomiserMax != sf.guardRandomiserMax) return true;
            if (ef.backOffRandomiserMin != sf.backOffRandomiserMin || ef.backOffRandomiserMax != sf.backOffRandomiserMax) return true;
            if (ef.retreatCooldown != sf.retreatCooldown) return true;
            if (ef.reEngageDistanceMin != sf.reEngageDistanceMin || ef.reEngageDistanceMax != sf.reEngageDistanceMax) return true;
            if (ef.groupObserveDistanceMin != sf.groupObserveDistanceMin || ef.groupObserveDistanceMax != sf.groupObserveDistanceMax) return true;
            if (ef.flankingAngle != sf.flankingAngle) return true;
        }
        if (editCombatAI.tierBehaviors.size() != savedCombatAI.tierBehaviors.size()) return true;
        for (int i = 0; i < editCombatAI.tierBehaviors.size(); i++) {
            var et = editCombatAI.tierBehaviors.get(i);
            var st = savedCombatAI.tierBehaviors.get(i);
            if (et.cooldownMin != st.cooldownMin || et.cooldownMax != st.cooldownMax) return true;
            if (et.strafeCooldownMin != st.strafeCooldownMin || et.strafeCooldownMax != st.strafeCooldownMax) return true;
            if (et.hasShield != st.hasShield || et.hasBackOff != st.hasBackOff || et.hasRetreat != st.hasRetreat) return true;
            if (et.hasGroupObserve != st.hasGroupObserve || et.hasFlanking != st.hasFlanking) return true;
            if (et.shieldChargeFor != st.shieldChargeFor || et.shieldGuardCooldown != st.shieldGuardCooldown) return true;
            if (et.retreatHealthThreshold != st.retreatHealthThreshold) return true;
        }
        for (var key : CAI_WEAPON_KEYS) {
            var ew = editCombatAI.weaponParams.get(key);
            var sw = savedCombatAI.weaponParams.get(key);
            if (ew == null || sw == null) { if (ew != sw) return true; continue; }
            if (ew.maxRange != sw.maxRange || ew.speedMultiplier != sw.speedMultiplier) return true;
            if (!ew.animationSetId.equals(sw.animationSetId)) return true;
            if (!ew.attackChainAnimations.equals(sw.attackChainAnimations)) return true;
            if (!ew.swingSoundId.equals(sw.swingSoundId) || !ew.impactSoundId.equals(sw.impactSoundId)) return true;
            if (!ew.weaponTrailId.equals(sw.weaponTrailId) || !ew.hitParticleId.equals(sw.hitParticleId)) return true;
        }
        return false;
    }

    private boolean handleCombatAIAction(String action) {
        if (action.startsWith("CaiSubTab_")) {
            caiSubTab = parseIdx(action, "CaiSubTab_");
            needsFieldRefresh = true;
        } else if (action.startsWith("CaiFaction_")) {
            caiFactionIndex = parseIdx(action, "CaiFaction_");
            needsFieldRefresh = true;
        } else if (action.startsWith("CaiTier_")) {
            caiTierIndex = parseIdx(action, "CaiTier_");
            needsFieldRefresh = true;
        } else if (action.startsWith("CaiWeapon_")) {
            caiWeaponIndex = parseIdx(action, "CaiWeapon_");
            needsFieldRefresh = true;
        } else if (action.startsWith("CaiTierToggle_")) {
            handleCaiTierToggle(action);
        } else if (action.equals("CaiFacObserveToggle")) {
            handleCaiFacObserveToggle();
        } else if (action.equals("CaiFacFlankToggle")) {
            handleCaiFacFlankToggle();
        } else if (action.equals("CaiWpnAnimSetPick")) {
            openAssetPicker(AssetPickerMode.ANIMATION_SET, "Select Animation Set", "Choose the animation set for this weapon type.");
        } else if (action.startsWith("CaiWpnChainAnimPick_")) {
            chainAnimPickSlot = parseIdx(action, "CaiWpnChainAnimPick_");
            openAssetPicker(AssetPickerMode.ANIMATION, "Select Animation", "Choose a swing animation from the current animation set.");
        } else if (action.startsWith("CaiWpnChainDel_")) {
            handleChainAnimDelete(parseIdx(action, "CaiWpnChainDel_"));
        } else if (action.equals("CaiWpnChainAdd")) {
            handleChainAnimAdd();
        } else if (action.equals("CaiWpnSwingSoundPick")) {
            openAssetPicker(AssetPickerMode.SOUND_SWING, "Select Swing Sound", "Choose the sound effect played on each swing.");
        } else if (action.equals("CaiWpnImpactSoundPick")) {
            openAssetPicker(AssetPickerMode.SOUND_IMPACT, "Select Impact Sound", "Choose the sound effect played on hit.");
        } else if (action.equals("CaiWpnSwingSoundPreview")) {
            previewCurrentWeaponSound(true);
        } else if (action.equals("CaiWpnImpactSoundPreview")) {
            previewCurrentWeaponSound(false);
        } else if (action.equals("CaiWpnTrailPick")) {
            openAssetPicker(AssetPickerMode.TRAIL, "Select Weapon Trail", "Choose the trail effect attached to the weapon during swings.");
        } else if (action.equals("CaiWpnHitParticlePick")) {
            openAssetPicker(AssetPickerMode.PARTICLE, "Select Hit Particle", "Choose the particle effect played on hit.");
        } else if (action.startsWith("AssetPickerRowClick_")) {
            int idx = parseIdx(action, "AssetPickerRowClick_");
            handleAssetPickerRowClick(idx);
        } else if (action.equals("AssetPickerConfirm")) {
            handleAssetPickerConfirm();
        } else if (action.equals("AssetPickerCancel") || action.equals("AssetPickerBackdropClick")) {
            assetPickerOpen = false;
        } else if (action.equals("AssetPickerFirstPage")) {
            assetPickerPage = 0;
        } else if (action.equals("AssetPickerPrevPage")) {
            assetPickerPage = Math.max(0, assetPickerPage - 1);
        } else if (action.equals("AssetPickerNextPage")) {
            int maxPage = Math.max(0, (assetPickerFiltered.size() - 1) / ASSET_PICKER_ROW_COUNT);
            assetPickerPage = Math.min(maxPage, assetPickerPage + 1);
        } else if (action.equals("AssetPickerLastPage")) {
            assetPickerPage = Math.max(0, (assetPickerFiltered.size() - 1) / ASSET_PICKER_ROW_COUNT);
        } else {
            return false;
        }
        return true;
    }

    private String assetPickerTitle = "";
    private String assetPickerSubtitle = "";

    private void openAssetPicker(AssetPickerMode mode, String title, String subtitle) {
        assetPickerMode = mode;
        assetPickerOpen = true;
        assetPickerFilter = "";
        assetPickerPage = 0;
        assetPickerSelectedItem = null;
        assetPickerTitle = title;
        assetPickerSubtitle = subtitle;
        assetPickerSourceList = buildAssetPickerSourceList(mode);
        assetPickerFiltered = new ArrayList<>(assetPickerSourceList);
    }

    private List<String> buildAssetPickerSourceList(AssetPickerMode mode) {
        var list = new ArrayList<String>();
        try {
            switch (mode) {
                case ANIMATION_SET -> {
                    var assetMap = com.hypixel.hytale.server.core.asset.type.itemanimation.config.ItemPlayerAnimations.getAssetMap();
                    if (assetMap != null) {
                        assetMap.getAssetMap().forEach((key, val) -> list.add(key));
                    }
                }
                case ANIMATION -> {
                    String wpnKey = CAI_WEAPON_KEYS[caiWeaponIndex];
                    var wp = editCombatAI != null ? editCombatAI.weaponParams.get(wpnKey) : null;
                    if (wp != null && !wp.animationSetId.isEmpty()) {
                        var assetMap = com.hypixel.hytale.server.core.asset.type.itemanimation.config.ItemPlayerAnimations.getAssetMap();
                        if (assetMap != null) {
                            var animSet = assetMap.getAsset(wp.animationSetId);
                            if (animSet != null) {
                                animSet.getAnimations().forEach((key, val) -> list.add(key));
                            }
                        }
                    }
                }
                case SOUND_SWING, SOUND_IMPACT -> {
                    var assetMap = com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent.getAssetMap();
                    if (assetMap != null) {
                        assetMap.getAssetMap().forEach((key, val) -> {
                            if (key.startsWith("SFX_")) list.add(key);
                        });
                    }
                }
                case TRAIL -> {
                    var assetMap = com.hypixel.hytale.server.core.asset.type.trail.config.Trail.getAssetMap();
                    if (assetMap != null) {
                        assetMap.getAssetMap().forEach((key, val) -> list.add(key));
                    }
                }
                case PARTICLE -> {
                    var assetMap = com.hypixel.hytale.server.core.asset.type.particle.config.ParticleSystem.getAssetMap();
                    if (assetMap != null) {
                        assetMap.getAssetMap().forEach((key, val) -> list.add(key));
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("Failed to enumerate assets for " + mode);
        }
        java.util.Collections.sort(list);
        return list;
    }

    private void applyAssetPickerFilter() {
        if (assetPickerFilter.isEmpty()) {
            assetPickerFiltered = new ArrayList<>(assetPickerSourceList);
        } else {
            String lowerFilter = assetPickerFilter.toLowerCase();
            assetPickerFiltered = new ArrayList<>();
            for (var item : assetPickerSourceList) {
                if (item.toLowerCase().contains(lowerFilter)) {
                    assetPickerFiltered.add(item);
                }
            }
        }
        assetPickerPage = 0;
        assetPickerSelectedItem = null;
    }

    private void handleAssetPickerRowClick(int rowIdx) {
        int idx = assetPickerPage * ASSET_PICKER_ROW_COUNT + rowIdx;
        if (idx >= 0 && idx < assetPickerFiltered.size()) {
            assetPickerSelectedItem = assetPickerFiltered.get(idx);
            if (isSoundPickerMode() && assetPickerSelectedItem != null) {
                playSoundPreview(assetPickerSelectedItem);
            }
        }
    }

    private void previewCurrentWeaponSound(boolean swing) {
        if (editCombatAI == null) return;
        String wpnKey = CAI_WEAPON_KEYS[caiWeaponIndex];
        var wp = editCombatAI.weaponParams.get(wpnKey);
        if (wp == null) return;
        String soundId = swing ? wp.swingSoundId : wp.impactSoundId;
        if (soundId != null && !soundId.isEmpty()) {
            playSoundPreview(soundId);
        }
    }

    private void playSoundPreview(String soundEventId) {
        try {
            var assetMap = com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent.getAssetMap();
            if (assetMap == null) return;
            int index = assetMap.getIndexOrDefault(soundEventId, -1);
            if (index < 0) return;
            com.hypixel.hytale.server.core.universe.world.SoundUtil.playSoundEvent2dToPlayer(
                    playerRef, index, com.hypixel.hytale.protocol.SoundCategory.SFX);
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("Failed to preview sound: " + soundEventId);
        }
    }

    private boolean isSoundPickerMode() {
        return assetPickerMode == AssetPickerMode.SOUND_SWING || assetPickerMode == AssetPickerMode.SOUND_IMPACT;
    }

    private void handleAssetPickerConfirm() {
        if (assetPickerSelectedItem == null || editCombatAI == null) return;
        String wpnKey = CAI_WEAPON_KEYS[caiWeaponIndex];
        var wp = editCombatAI.weaponParams.get(wpnKey);
        if (wp == null) return;

        switch (assetPickerMode) {
            case ANIMATION_SET -> wp.animationSetId = assetPickerSelectedItem;
            case ANIMATION -> {
                if (chainAnimPickSlot >= 0 && chainAnimPickSlot < wp.attackChainAnimations.size()) {
                    wp.attackChainAnimations.set(chainAnimPickSlot, assetPickerSelectedItem);
                }
            }
            case SOUND_SWING -> wp.swingSoundId = assetPickerSelectedItem;
            case SOUND_IMPACT -> wp.impactSoundId = assetPickerSelectedItem;
            case TRAIL -> wp.weaponTrailId = assetPickerSelectedItem;
            case PARTICLE -> wp.hitParticleId = assetPickerSelectedItem;
        }
        assetPickerOpen = false;
    }

    private void handleChainAnimDelete(int slot) {
        if (editCombatAI == null) return;
        String wpnKey = CAI_WEAPON_KEYS[caiWeaponIndex];
        var wp = editCombatAI.weaponParams.get(wpnKey);
        if (wp != null && slot >= 0 && slot < wp.attackChainAnimations.size()) {
            wp.attackChainAnimations.remove(slot);
        }
    }

    private void handleChainAnimAdd() {
        if (editCombatAI == null) return;
        String wpnKey = CAI_WEAPON_KEYS[caiWeaponIndex];
        var wp = editCombatAI.weaponParams.get(wpnKey);
        if (wp != null && wp.attackChainAnimations.size() < CAI_WEAPON_CHAIN_SLOTS) {
            wp.attackChainAnimations.add("SwingLeft");
        }
    }

    private void handleCaiTierToggle(String action) {
        if (editCombatAI == null) return;
        var parts = action.substring("CaiTierToggle_".length()).split("_");
        if (parts.length != 2) return;
        String behavior = parts[0];
        int tier = parseIntField(parts[1], -1);
        if (tier < 0 || tier >= editCombatAI.tierBehaviors.size()) return;
        var tb = editCombatAI.tierBehaviors.get(tier);
        switch (behavior) {
            case "Shield" -> tb.hasShield = !tb.hasShield;
            case "BackOff" -> tb.hasBackOff = !tb.hasBackOff;
            case "Retreat" -> tb.hasRetreat = !tb.hasRetreat;
            case "Observe" -> tb.hasGroupObserve = !tb.hasGroupObserve;
            case "Flank" -> tb.hasFlanking = !tb.hasFlanking;
        }
    }

    private void handleCaiFacObserveToggle() {
        if (editCombatAI == null) return;
        String key = CAI_FACTION_KEYS[caiFactionIndex];
        var fs = editCombatAI.factionStyles.get(key);
        if (fs != null) fs.enableGroupObserve = !fs.enableGroupObserve;
    }

    private void handleCaiFacFlankToggle() {
        if (editCombatAI == null) return;
        String key = CAI_FACTION_KEYS[caiFactionIndex];
        var fs = editCombatAI.factionStyles.get(key);
        if (fs != null) fs.enableFlanking = !fs.enableFlanking;
    }

    private void parseCombatAITextFields(AdminUIData data) {
        if (editCombatAI == null) return;
        String facKey = CAI_FACTION_KEYS[caiFactionIndex];
        var fs = editCombatAI.factionStyles.get(facKey);
        if (fs != null) {
            fs.attackCooldownMin = parseDoubleField(data.caiFacAtkCdMin, fs.attackCooldownMin);
            fs.attackCooldownMax = parseDoubleField(data.caiFacAtkCdMax, fs.attackCooldownMax);
            fs.shieldChargeFor = parseDoubleField(data.caiFacShieldCharge, fs.shieldChargeFor);
            fs.shieldSwitchPoint = parseDoubleField(data.caiFacShieldSwitch, fs.shieldSwitchPoint);
            fs.backOffDistanceMin = parseDoubleField(data.caiFacBoDistMin, fs.backOffDistanceMin);
            fs.backOffDistanceMax = parseDoubleField(data.caiFacBoDistMax, fs.backOffDistanceMax);
            fs.backOffSwitchPoint = parseDoubleField(data.caiFacBoSwitch, fs.backOffSwitchPoint);
            fs.healthRetreatDistanceMin = parseDoubleField(data.caiFacRetDistMin, fs.healthRetreatDistanceMin);
            fs.healthRetreatDistanceMax = parseDoubleField(data.caiFacRetDistMax, fs.healthRetreatDistanceMax);
            fs.healthRetreatWeight = parseDoubleField(data.caiFacRetWeight, fs.healthRetreatWeight);
            fs.reEngageXRangeMin = parseDoubleField(data.caiFacReEngMin, fs.reEngageXRangeMin);
            fs.reEngageXRangeMax = parseDoubleField(data.caiFacReEngMax, fs.reEngageXRangeMax);
            fs.reEngageRandomiserMin = parseDoubleField(data.caiFacReEngRandMin, fs.reEngageRandomiserMin);
            fs.reEngageRandomiserMax = parseDoubleField(data.caiFacReEngRandMax, fs.reEngageRandomiserMax);
            fs.strafeCooldownMin = parseDoubleField(data.caiFacStrafeCdMin, fs.strafeCooldownMin);
            fs.strafeCooldownMax = parseDoubleField(data.caiFacStrafeCdMax, fs.strafeCooldownMax);
            fs.guardRandomiserMin = parseDoubleField(data.caiFacGuardRandMin, fs.guardRandomiserMin);
            fs.guardRandomiserMax = parseDoubleField(data.caiFacGuardRandMax, fs.guardRandomiserMax);
            fs.backOffRandomiserMin = parseDoubleField(data.caiFacBoRandMin, fs.backOffRandomiserMin);
            fs.backOffRandomiserMax = parseDoubleField(data.caiFacBoRandMax, fs.backOffRandomiserMax);
            fs.retreatCooldown = parseDoubleField(data.caiFacRetCooldown, fs.retreatCooldown);
            fs.reEngageDistanceMin = parseDoubleField(data.caiFacReEngDistMin, fs.reEngageDistanceMin);
            fs.reEngageDistanceMax = parseDoubleField(data.caiFacReEngDistMax, fs.reEngageDistanceMax);
            fs.groupObserveDistanceMin = parseDoubleField(data.caiFacObsDistMin, fs.groupObserveDistanceMin);
            fs.groupObserveDistanceMax = parseDoubleField(data.caiFacObsDistMax, fs.groupObserveDistanceMax);
            fs.flankingAngle = parseDoubleField(data.caiFacFlankAngle, fs.flankingAngle);
        }
        if (caiTierIndex < editCombatAI.tierBehaviors.size()) {
            var tb = editCombatAI.tierBehaviors.get(caiTierIndex);
            tb.cooldownMin = parseDoubleField(data.caiTierCdMin, tb.cooldownMin);
            tb.cooldownMax = parseDoubleField(data.caiTierCdMax, tb.cooldownMax);
            tb.strafeCooldownMin = parseDoubleField(data.caiTierStrCdMin, tb.strafeCooldownMin);
            tb.strafeCooldownMax = parseDoubleField(data.caiTierStrCdMax, tb.strafeCooldownMax);
            tb.shieldChargeFor = parseDoubleField(data.caiTierShieldCharge, tb.shieldChargeFor);
            tb.shieldGuardCooldown = parseDoubleField(data.caiTierGuardCd, tb.shieldGuardCooldown);
            tb.retreatHealthThreshold = parseDoubleField(data.caiTierRetHealth, tb.retreatHealthThreshold);
        }
        String wpnKey = CAI_WEAPON_KEYS[caiWeaponIndex];
        var wp = editCombatAI.weaponParams.get(wpnKey);
        if (wp != null) {
            wp.maxRange = parseDoubleField(data.caiWpnRange, wp.maxRange);
            wp.speedMultiplier = parseDoubleField(data.caiWpnSpeed, wp.speedMultiplier);
        }
        if (data.assetPickerFilter != null && assetPickerOpen) {
            String newFilter = data.assetPickerFilter;
            if (!newFilter.equals(assetPickerFilter)) {
                assetPickerFilter = newFilter;
                applyAssetPickerFilter();
            }
        }
    }

    private void renderCombatAITab(UICommandBuilder c) {
        if (editCombatAI == null) return;

        c.set("#CaiSubFactionStyles.Visible", caiSubTab == 0);
        c.set("#CaiSubTierBehavior.Visible", caiSubTab == 1);
        c.set("#CaiSubWeaponCombat.Visible", caiSubTab == 2);

        if (caiSubTab == 0) {
            renderCombatAIFactionTab(c);
        } else if (caiSubTab == 1) {
            renderCombatAITierTab(c);
        } else if (caiSubTab == 2) {
            renderCombatAIWeaponTab(c);
        }

        renderAssetPickerPopup(c);
    }

    private void renderCombatAIFactionTab(UICommandBuilder c) {
        for (int i = 0; i < 4; i++) {
            c.set("#CaiFaction" + i + ".Visible", i != caiFactionIndex);
            c.set("#CaiFaction" + i + "Active.Visible", i == caiFactionIndex);
        }
        String facKey = CAI_FACTION_KEYS[caiFactionIndex];
        c.set("#CaiFactionGroupLabel.Text", CAI_STYLE_DESCRIPTIONS[caiFactionIndex]);
        var fs = editCombatAI.factionStyles.get(facKey);
        if (fs != null && needsFieldRefresh) {
            c.set("#FieldCaiFacAtkCdMin.Value", fmtDouble(fs.attackCooldownMin));
            c.set("#FieldCaiFacAtkCdMax.Value", fmtDouble(fs.attackCooldownMax));
            c.set("#FieldCaiFacShieldCharge.Value", fmtDouble(fs.shieldChargeFor));
            c.set("#FieldCaiFacShieldSwitch.Value", fmtDouble(fs.shieldSwitchPoint));
            c.set("#FieldCaiFacBoDistMin.Value", fmtDouble(fs.backOffDistanceMin));
            c.set("#FieldCaiFacBoDistMax.Value", fmtDouble(fs.backOffDistanceMax));
            c.set("#FieldCaiFacBoSwitch.Value", fmtDouble(fs.backOffSwitchPoint));
            c.set("#FieldCaiFacRetDistMin.Value", fmtDouble(fs.healthRetreatDistanceMin));
            c.set("#FieldCaiFacRetDistMax.Value", fmtDouble(fs.healthRetreatDistanceMax));
            c.set("#FieldCaiFacRetWeight.Value", fmtDouble(fs.healthRetreatWeight));
            c.set("#FieldCaiFacReEngMin.Value", fmtDouble(fs.reEngageXRangeMin));
            c.set("#FieldCaiFacReEngMax.Value", fmtDouble(fs.reEngageXRangeMax));
            c.set("#FieldCaiFacReEngRandMin.Value", fmtDouble(fs.reEngageRandomiserMin));
            c.set("#FieldCaiFacReEngRandMax.Value", fmtDouble(fs.reEngageRandomiserMax));
            c.set("#FieldCaiFacStrafeCdMin.Value", fmtDouble(fs.strafeCooldownMin));
            c.set("#FieldCaiFacStrafeCdMax.Value", fmtDouble(fs.strafeCooldownMax));
            c.set("#FieldCaiFacGuardRandMin.Value", fmtDouble(fs.guardRandomiserMin));
            c.set("#FieldCaiFacGuardRandMax.Value", fmtDouble(fs.guardRandomiserMax));
            c.set("#FieldCaiFacBoRandMin.Value", fmtDouble(fs.backOffRandomiserMin));
            c.set("#FieldCaiFacBoRandMax.Value", fmtDouble(fs.backOffRandomiserMax));
            c.set("#FieldCaiFacRetCooldown.Value", fmtDouble(fs.retreatCooldown));
            c.set("#FieldCaiFacReEngDistMin.Value", fmtDouble(fs.reEngageDistanceMin));
            c.set("#FieldCaiFacReEngDistMax.Value", fmtDouble(fs.reEngageDistanceMax));
            c.set("#FieldCaiFacObsDistMin.Value", fmtDouble(fs.groupObserveDistanceMin));
            c.set("#FieldCaiFacObsDistMax.Value", fmtDouble(fs.groupObserveDistanceMax));
            c.set("#FieldCaiFacFlankAngle.Value", fmtDouble(fs.flankingAngle));
        }
        if (fs != null) {
            renderToggle(c, "#CaiFacObserve", fs.enableGroupObserve);
            renderToggle(c, "#CaiFacFlank", fs.enableFlanking);
        }
    }

    private void renderCombatAITierTab(UICommandBuilder c) {
        for (int t = 0; t < editCombatAI.tierBehaviors.size() && t < 5; t++) {
            var tb = editCombatAI.tierBehaviors.get(t);
            renderToggle(c, "#CaiTierToggleShield" + t, tb.hasShield);
            renderToggle(c, "#CaiTierToggleBackOff" + t, tb.hasBackOff);
            renderToggle(c, "#CaiTierToggleRetreat" + t, tb.hasRetreat);
            renderToggle(c, "#CaiTierToggleObserve" + t, tb.hasGroupObserve);
            renderToggle(c, "#CaiTierToggleFlank" + t, tb.hasFlanking);
        }

        for (int i = 0; i < 5; i++) {
            c.set("#CaiTier" + i + ".Visible", i != caiTierIndex);
            c.set("#CaiTier" + i + "Active.Visible", i == caiTierIndex);
        }
        if (caiTierIndex < editCombatAI.tierBehaviors.size() && needsFieldRefresh) {
            var tb = editCombatAI.tierBehaviors.get(caiTierIndex);
            c.set("#FieldCaiTierCdMin.Value", fmtDouble(tb.cooldownMin));
            c.set("#FieldCaiTierCdMax.Value", fmtDouble(tb.cooldownMax));
            c.set("#FieldCaiTierStrCdMin.Value", fmtDouble(tb.strafeCooldownMin));
            c.set("#FieldCaiTierStrCdMax.Value", fmtDouble(tb.strafeCooldownMax));
            c.set("#FieldCaiTierShieldCharge.Value", fmtDouble(tb.shieldChargeFor));
            c.set("#FieldCaiTierGuardCd.Value", fmtDouble(tb.shieldGuardCooldown));
            c.set("#FieldCaiTierRetHealth.Value", fmtDouble(tb.retreatHealthThreshold));
        }
    }

    private void renderCombatAIWeaponTab(UICommandBuilder c) {
        for (int i = 0; i < CAI_WEAPON_KEYS.length; i++) {
            c.set("#CaiWeapon" + i + ".Visible", i != caiWeaponIndex);
            c.set("#CaiWeapon" + i + "Active.Visible", i == caiWeaponIndex);
        }
        String wpnKey = CAI_WEAPON_KEYS[caiWeaponIndex];
        var wp = editCombatAI.weaponParams.get(wpnKey);
        if (wp != null) {
            if (needsFieldRefresh) {
                c.set("#FieldCaiWpnRange.Value", fmtDouble(wp.maxRange));
                c.set("#FieldCaiWpnSpeed.Value", fmtDouble(wp.speedMultiplier));
            }
            c.set("#CaiWpnRangedLabel.Visible", wp.isRanged);
            c.set("#CaiWpnMeleeDetail.Visible", !wp.isRanged);

            if (!wp.isRanged) {
                c.set("#CaiWpnAnimSetBtn.Text", wp.animationSetId.isEmpty() ? "(none)" : wp.animationSetId);

                var chain = wp.attackChainAnimations;
                for (int i = 0; i < CAI_WEAPON_CHAIN_SLOTS; i++) {
                    boolean visible = i < chain.size();
                    c.set("#CaiWpnChainRow" + i + ".Visible", visible);
                    if (visible) {
                        c.set("#CaiWpnChainNum" + i + ".Text", String.valueOf(i + 1));
                        c.set("#CaiWpnChainAnim" + i + ".Text", chain.get(i));
                    }
                }
                c.set("#CaiWpnChainAdd.Visible", chain.size() < CAI_WEAPON_CHAIN_SLOTS);

                c.set("#CaiWpnSwingSoundBtn.Text", wp.swingSoundId.isEmpty() ? "(none)" : wp.swingSoundId);
                c.set("#CaiWpnImpactSoundBtn.Text", wp.impactSoundId.isEmpty() ? "(none)" : wp.impactSoundId);
                c.set("#CaiWpnTrailBtn.Text", wp.weaponTrailId.isEmpty() ? "(none)" : wp.weaponTrailId);
                c.set("#CaiWpnHitParticleBtn.Text", wp.hitParticleId.isEmpty() ? "(none)" : wp.hitParticleId);
            }
        }
    }

    private void renderAssetPickerPopup(UICommandBuilder c) {
        c.set("#AssetPickerPopup.Visible", assetPickerOpen);
        if (!assetPickerOpen) return;

        c.set("#AssetPickerTitle.Text", assetPickerTitle);
        c.set("#AssetPickerSubtitle.Text", assetPickerSubtitle);

        int total = assetPickerFiltered.size();
        int start = assetPickerPage * ASSET_PICKER_ROW_COUNT;
        for (int i = 0; i < ASSET_PICKER_ROW_COUNT; i++) {
            int idx = start + i;
            boolean visible = idx < total;
            c.set("#AssetPickerRow" + i + ".Visible", visible);
            if (visible) {
                String name = assetPickerFiltered.get(idx);
                boolean selected = name.equals(assetPickerSelectedItem);
                c.set("#AssetPickerRowBtn" + i + ".Text", name);
                c.set("#AssetPickerRowBtn" + i + ".Visible", !selected);
                c.set("#AssetPickerRowBtnSel" + i + ".Text", name);
                c.set("#AssetPickerRowBtnSel" + i + ".Visible", selected);
            } else {
                c.set("#AssetPickerRowBtnSel" + i + ".Visible", false);
            }
        }
        c.set("#AssetPickerEmpty.Visible", total == 0);
        c.set("#AssetPickerSelectedLabel.Text", assetPickerSelectedItem != null ? assetPickerSelectedItem : "None");
        c.set("#AssetPickerConfirm.Visible", assetPickerSelectedItem != null);

        setPagination(c, total, assetPickerPage, ASSET_PICKER_ROW_COUNT, "#AssetPickerPagination", "#AssetPickerPageInfo",
                "#AssetPickerFirstPage", "#AssetPickerPrevPage", "#AssetPickerNextPage", "#AssetPickerLastPage");
    }
}
