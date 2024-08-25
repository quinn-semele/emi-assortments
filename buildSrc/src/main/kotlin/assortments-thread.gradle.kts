import dev.compasses.emi_assortments.ModConstants
import dev.compasses.multiloader.extension.MultiLoaderExtension

plugins {
    id("multiloader-loader")
}

extensions.configure<MultiLoaderExtension>("multiloader") {
    mods {
        create("emi") {
            required()

            requiresRepo("TerraformersMC Maven", "https://maven.terraformersmc.com/", setOf("dev.emi"))

            artifacts {
                add("modImplementation", "dev.emi:emi-fabric:${ModConstants.EMI_VERSION}")
            }
        }
    }
}