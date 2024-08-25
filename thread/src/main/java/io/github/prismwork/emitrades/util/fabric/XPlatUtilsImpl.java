package io.github.prismwork.emitrades.util.fabric;

import io.github.prismwork.emitrades.util.XPlatUtils;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public final class XPlatUtilsImpl implements XPlatUtils {
    @Override
    public Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir();
    }
}
