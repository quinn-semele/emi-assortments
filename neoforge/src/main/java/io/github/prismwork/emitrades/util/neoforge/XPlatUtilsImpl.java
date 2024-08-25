package io.github.prismwork.emitrades.util.neoforge;

import io.github.prismwork.emitrades.util.XPlatUtils;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Path;

public final class XPlatUtilsImpl implements XPlatUtils {
    @Override
    public Path getConfigPath() {
        return FMLPaths.CONFIGDIR.get();
    }
}
