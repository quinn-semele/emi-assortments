package io.github.prismwork.emitrades.util;

import java.nio.file.Path;
import java.util.ServiceLoader;

public interface XPlatUtils {
    XPlatUtils INSTANCE = ServiceLoader.load(XPlatUtils.class).findFirst().orElseThrow();

    Path getConfigPath();
}
