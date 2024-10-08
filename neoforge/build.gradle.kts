import dev.compasses.emi_assortments.ModConstants

plugins {
    id("multiloader-neoforge")
}

multiloader {
    mods {
        create("emi") {
            required()

            requiresRepo("TerraformersMC Maven", "https://maven.terraformersmc.com/", setOf("dev.emi"))

            artifacts {
                implementation("dev.emi:emi-neoforge:${ModConstants.EMI_VERSION}")
            }
        }
    }
}
