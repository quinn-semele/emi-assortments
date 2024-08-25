import dev.compasses.emi_assortments.ModConstants

plugins {
    id("multiloader-common")
}

multiloader {
    mods {
        create("emi") {
            required()

            requiresRepo("TerraformersMC Maven", "https://maven.terraformersmc.com/", setOf("dev.emi"))

            artifacts {
                compileOnly("dev.emi:emi-xplat-mojmap:${ModConstants.EMI_VERSION}")
            }
        }
    }
}