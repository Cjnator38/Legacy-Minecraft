package wily.legacy.client;

import com.google.gson.*;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import wily.factoryapi.FactoryAPI;
import wily.factoryapi.util.DynamicUtil;
import wily.factoryapi.util.ListMap;
import wily.legacy.Legacy4J;
import wily.legacy.client.screen.LegacyTabButton;
import wily.legacy.util.JsonUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;

import static wily.legacy.util.JsonUtil.*;

public class LegacyCraftingTabListing implements LegacyTabInfo {
    public static final Codec<LegacyCraftingTabListing> CODEC = RecordCodecBuilder.create(i-> i.group(ResourceLocation.CODEC.fieldOf("id").forGetter(LegacyCraftingTabListing::id), DynamicUtil.getComponentCodec().optionalFieldOf("name",null).forGetter(LegacyCraftingTabListing::name),LegacyTabButton.ICON_HOLDER_CODEC.optionalFieldOf("icon",null).forGetter(LegacyCraftingTabListing::iconHolder),RecipeInfo.Filter.LISTING_CODEC.fieldOf("listing").forGetter(LegacyCraftingTabListing::craftings)).apply(i,LegacyCraftingTabListing::new));
    public static final ListMap<ResourceLocation,LegacyCraftingTabListing> map = new ListMap<>();
    private static final String CRAFTING_TAB_LISTING = "crafting_tab_listing.json";
    private final ResourceLocation id;
    public Component name;
    public LegacyTabButton.IconHolder<?> iconHolder;
    private final Map<String, List<RecipeInfo.Filter>> craftings;

    @Deprecated
    public LegacyCraftingTabListing(ResourceLocation id, Component name, LegacyTabButton.IconHolder<?> iconHolder){
        this(id,name,iconHolder,new LinkedHashMap<>());
    }

    public LegacyCraftingTabListing(ResourceLocation id, Component name, LegacyTabButton.IconHolder<?> iconHolder, Map<String,List<RecipeInfo.Filter>> craftings){
        this.id = id;
        this.name = name;
        this.iconHolder = iconHolder;
        this.craftings = craftings;
    }

    @Override
    public boolean isValid() {
        return LegacyTabInfo.super.isValid() && !craftings.isEmpty();
    }

    public ResourceLocation id(){
        return id;
    }

    public Component name(){
        return name;
    }

    public LegacyTabButton.IconHolder<?> iconHolder(){
        return iconHolder;
    }

    public final Map<String, List<RecipeInfo.Filter>> craftings(){
        return craftings;
    }

    public void addFrom(LegacyCraftingTabListing otherListing){
        if (otherListing.name != null) name = otherListing.name;
        if (otherListing.iconHolder != null) iconHolder = otherListing.iconHolder;
        otherListing.craftings.forEach((s,f)->{
            if (craftings.containsKey(s)) craftings.get(s).addAll(f);
            else craftings.put(s,f);
        });
    }

    public static class Manager implements ResourceManagerReloadListener {
        @Override
        public void onResourceManagerReload(ResourceManager manager) {
            map.clear();
            JsonUtil.getOrderedNamespaces(manager).forEach(name->manager.getResource(FactoryAPI.createLocation(name, CRAFTING_TAB_LISTING)).ifPresent(r->{
                try (BufferedReader bufferedReader = r.openAsReader()) {
                    JsonElement element = JsonParser.parseReader(bufferedReader);
                    if (element instanceof JsonArray a) a.forEach(e-> CODEC.parse(JsonOps.INSTANCE,e).result().ifPresent(listing->{
                        if (map.containsKey(listing.id)){
                            map.get(listing.id).addFrom(listing);
                        } else if (listing.isValid()) map.put(listing.id,listing);
                    }));
                } catch (IOException exception) {
                    Legacy4J.LOGGER.warn(exception.getMessage());
                }
            }));
        }

        @Override
        public String getName() {
            return "legacy:crafting_tab_listing";
        }
    }
}
