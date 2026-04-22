package com.jdar.zenithteamsfabric.utils;

import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.collection.DefaultedList;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

public class ItemSerializer {

    public static String toBase64(DefaultedList<ItemStack> items, RegistryWrapper.WrapperLookup registries) {
        try {
            NbtCompound nbt = new NbtCompound();
            Inventories.writeNbt(nbt, items, registries);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            NbtIo.writeCompressed(nbt, outputStream);
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            return "";
        }
    }

    public static DefaultedList<ItemStack> fromBase64(String data, RegistryWrapper.WrapperLookup registries, int size) {
        DefaultedList<ItemStack> items = DefaultedList.ofSize(size, ItemStack.EMPTY);
        if (data == null || data.isEmpty()) return items;

        try {
            byte[] bytes = Base64.getDecoder().decode(data);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
            NbtCompound nbt = NbtIo.readCompressed(inputStream, NbtSizeTracker.ofUnlimitedBytes());

            Inventories.readNbt(nbt, items, registries);
        } catch (Exception ignored) {}

        return items;
    }
}
