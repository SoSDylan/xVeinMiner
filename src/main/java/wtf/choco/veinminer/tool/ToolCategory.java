package wtf.choco.veinminer.tool;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import wtf.choco.veinminer.VeinMiner;
import wtf.choco.veinminer.api.VeinMinerManager;
import wtf.choco.veinminer.data.AlgorithmConfig;
import wtf.choco.veinminer.data.BlockList;
import wtf.choco.veinminer.utils.ItemValidator;
import wtf.choco.veinminer.utils.Pair;

/**
 * Represents a category of tools recognized by VeinMiner. Categories may possess
 * their own {@link AlgorithmConfig}, {@link BlockList} and set of {@link ToolTemplate}s
 * which further specifies its criteria when players vein mine.
 */
public class ToolCategory {

    private static final Map<String, ToolCategory> CATEGORIES = new HashMap<>();
    private static final Pattern VALID_ID = Pattern.compile("[A-Za-z0-9]+", Pattern.CASE_INSENSITIVE);

    public static final ToolCategory HAND = new ToolCategory(VeinMiner.getPlugin().getVeinMinerManager(), "Hand");
    static {
        ToolCategory.register(HAND);
    }

    private final String id;
    private final AlgorithmConfig config;
    private final List<ToolTemplate> tools;
    private final BlockList blocklist;

    /**
     * Construct a new tool category with an empty block and tool list.
     *
     * @param manager the vein miner manager instance
     * @param id the unique id of the tool category. Recommended to be a single-worded, PascalCase id.
     * Must match [A-Za-z0-9]
     * @param blocklist the category block list
     */
    public ToolCategory(@NotNull VeinMinerManager manager, @NotNull String id, @NotNull BlockList blocklist) {
        Preconditions.checkArgument(manager != null, "Vein miner manager must not be null");
        Preconditions.checkArgument(id != null && VALID_ID.matcher(id).matches(), "Invalid category ID. Must be non-null and alphanumeric with no spaces");
        Preconditions.checkArgument(blocklist != null, "Blocklist must not be null");

        this.config = new AlgorithmConfig(manager.getConfig());
        this.id = id;
        this.tools = new ArrayList<>();
        this.blocklist = blocklist;
    }

    /**
     * Construct a new tool category.
     *
     * @param manager the vein miner manager instance
     * @param id the unique id of the tool category. Recommended to be a single-worded, PascalCase id.
     * Must match [A-Za-z0-9]
     * @param blocklist the category block list
     * @param tools the tools that apply to this category
     */
    public ToolCategory(@NotNull VeinMinerManager manager, @NotNull String id, @NotNull BlockList blocklist, @NotNull ToolTemplate... tools) {
        this(manager, id, blocklist);
        Preconditions.checkArgument(tools != null, "Tools must not be null");

        for (ToolTemplate template : tools) {
            this.tools.add(template);
        }
    }

    /**
     * Construct a new tool category with an empty block list.
     *
     * @param manager the vein miner manager instance
     * @param id the unique id of the tool category. Recommended to be a single-worded, PascalCase id.
     * Must match [A-Za-z0-9]
     * @param tools the tools that apply to this category
     */
    public ToolCategory(@NotNull VeinMinerManager manager, @NotNull String id, @NotNull ToolTemplate... tools) {
        this(manager, id, new BlockList(), tools);
    }

    /**
     * Construct a new tool category with an empty block list.
     *
     * @param manager the vein miner manager instance
     * @param id the unique id of the tool category. Recommended to be a single-worded, PascalCase id.
     * Must match [A-Za-z0-9]
     */
    public ToolCategory(@NotNull VeinMinerManager manager, @NotNull String id) {
        this(manager, id, new BlockList());
    }

    /**
     * Get the unique id of this tool category.
     *
     * @return this category's id
     */
    @NotNull
    public String getId() {
        return id;
    }

    /**
     * Get the algorithm config for this tool category. This category should have precedence
     * over the global algorithm config.
     *
     * @return the algorithm config
     */
    @NotNull
    public AlgorithmConfig getConfig() {
        return config;
    }

    /**
     * Add a tool template to this tool category
     *
     * @param template the template to add
     */
    public void addTool(@NotNull ToolTemplate template) {
        Preconditions.checkArgument(template != null, "Cannot add a null template");

        if (tools.contains(template)) {
            return;
        }

        this.tools.add(template);
    }

    /**
     * Remove a tool template from this tool category
     *
     * @param template the template to remove
     *
     * @return true if removed, false otherwise
     */
    public boolean removeTool(@NotNull ToolTemplate template) {
        return tools.remove(template);
    }

    /**
     * Remove all tool templates from this tool category that match the provided item.
     *
     * @param item the item to remove
     *
     * @return true if removed, false otherwise
     */
    public boolean removeTool(@NotNull ItemStack item) {
        return tools.removeIf(t -> t.matches(item));
    }

    /**
     * Remove all tool templates from this tool category that match the provided material.
     * This will not remove any templates that have specific meta such as a name or lore,
     * only material templates (i.e. a regular diamond pickaxe or stone axe).
     *
     * @param material the material to remove
     *
     * @return true if removed, false otherwise
     */
    public boolean removeTool(@NotNull Material material) {
        return tools.removeIf(t -> t instanceof ToolTemplateMaterial && ((ToolTemplateMaterial) t).getMaterial() == material);
    }

    /**
     * Check whether or not the provided item is a part of this category. The item's name
     * and lore will be taken into consideration.
     *
     * @param item the item to check
     *
     * @return true if contained, false otherwise
     */
    public boolean containsTool(ItemStack item) {
        return tools.stream().anyMatch(t -> (t instanceof ToolTemplateItemStack) && t.matches(item));
    }

    /**
     * Check whether or not the provided material is a part of this category.
     *
     * @param material the material to check
     *
     * @return true if contained, false otherwise
     */
    public boolean containsTool(Material material) {
        ItemStack item = new ItemStack(material);
        return tools.stream().anyMatch(t -> (t instanceof ToolTemplateMaterial) && t.matches(item));
    }

    /**
     * Get a list of all tool templates that apply to this category. Any changes made to
     * the returned collection will not reflect upon the category.
     *
     * @return the tool templates
     */
    @NotNull
    public List<ToolTemplate> getTools() {
        return new ArrayList<>(tools);
    }

    /**
     * Clear all tool templates from this category.
     */
    public void clearTools() {
        this.tools.clear();
    }

    /**
     * Get the blocklist for this category.
     *
     * @return the blocklist
     */
    @NotNull
    public BlockList getBlocklist() {
        return blocklist;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof ToolCategory && id.equals(((ToolCategory) obj).id));
    }

    /**
     * Get a tool category based on its (case-insensitive) id.
     *
     * @param id the id of the category to get
     *
     * @return the tool category. null if none
     */
    @Nullable
    public static ToolCategory get(@NotNull String id) {
        return CATEGORIES.get(id.toLowerCase());
    }

    /**
     * Get a tool category based on the provided tool. If the tool applies to a category,
     * it will be returned. If more than one category includes this tool, the category that
     * was registered first will be returned.
     *
     * @param item the item whose category to get
     *
     * @return the tool category. null if none
     */
    @Nullable
    public static ToolCategory get(@Nullable ItemStack item) {
        if (ItemValidator.isEmpty(item)) {
            return ToolCategory.HAND;
        }

        for (ToolCategory category : CATEGORIES.values()) {
            if (category.getTools().stream().anyMatch(t -> t.matches(item))) {
                return category;
            }
        }

        return null;
    }

    /**
     * Register a tool category.
     *
     * @param category the category to register
     */
    public static void register(@NotNull ToolCategory category) {
        CATEGORIES.put(category.id.toLowerCase(), category);
    }

    /**
     * Get a tool category based on the provided tool as well as the template against which
     * the tool was matched. If the tool applies to a category, it will be returned. If more
     * than one category includes this tool, the category that was registered first will be
     * returned.
     *
     * @param item the item whose category to get
     *
     * @return the tool category and matched template. null if none
     */
    @NotNull
    public static Pair<ToolCategory, ToolTemplate> getWithTemplate(@Nullable ItemStack item) {
        if (ItemValidator.isEmpty(item)) {
            return new Pair<>(ToolCategory.HAND, null);
        }

        for (ToolCategory category : CATEGORIES.values()) {
            for (ToolTemplate template : category.getTools()) {
                if (template.matches(item)) {
                    return new Pair<>(category, template);
                }
            }
        }

        return Pair.empty();
    }

    /**
     * Get the amount of tool categories registered.
     *
     * @return the amount of registered categories
     */
    public static int getRegisteredAmount() {
        return CATEGORIES.size();
    }

    /**
     * Get an immutable collection of all registered tool categories.
     *
     * @return all tool categories
     */
    @NotNull
    public static Collection<ToolCategory> getAll() {
        return ImmutableList.copyOf(CATEGORIES.values());
    }

    /**
     * Clear all registered categories.
     */
    public static void clearCategories() {
        CATEGORIES.clear();
    }

}
