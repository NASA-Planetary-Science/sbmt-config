package edu.jhuapl.sbmt.config;

import edu.jhuapl.saavtk.util.SafeURLPaths;
import edu.jhuapl.saavtk.util.file.FileLocator;
import edu.jhuapl.saavtk.util.file.FileLocators;
import edu.jhuapl.sbmt.model.image.Instrument;

public class SBMTFileLocators
{
    public static SBMTFileLocator of(SBMTBodyConfiguration bodyConfig, ShapeModelConfiguration modelConfig, Instrument instrument, String imageFileSuffix, String infoFileSuffix, String sumFileSuffix, String galleryFileSuffix)
    {
        String lcInstrument = Instrument.IMAGING_DATA.equals(instrument) ? "imaging" : instrument.toString().toLowerCase();

        FileLocator topPathLocator = replaceServerPath(bodyConfig, modelConfig, lcInstrument);
        FileLocator imageFileLocator = FileLocators.concatenate(replaceServerPath(bodyConfig, modelConfig, lcInstrument + "/images"), FileLocators.replaceSuffix(imageFileSuffix));
        FileLocator infoFileLocator = FileLocators.concatenate(replaceServerPath(bodyConfig, modelConfig, lcInstrument + "/infofiles"), FileLocators.replaceSuffix(infoFileSuffix));
        FileLocator sumFileLocator = FileLocators.concatenate(replaceServerPath(bodyConfig, modelConfig, lcInstrument + "/sumfiles"), FileLocators.replaceSuffix(sumFileSuffix));
        FileLocator galleryFileLocator = FileLocators.concatenate(replaceServerPath(bodyConfig, modelConfig, lcInstrument + "/gallery"), FileLocators.replaceSuffix(galleryFileSuffix));

        return SBMTFileLocator.builder(topPathLocator, imageFileLocator)
                .put(SBMTFileLocator.INFO_FILE, infoFileLocator)
                .put(SBMTFileLocator.SUM_FILE, sumFileLocator)
                .put(SBMTFileLocator.GALLERY_FILE, galleryFileLocator)
                .build();
    }

    public static FileLocator replaceServerPath(final SBMTBodyConfiguration bodyConfig, final ShapeModelConfiguration modelConfig, final String subPath) {
        return new FileLocator() {
            @Override
            public String getLocation(String name)
            {
                String serverPath = serverPath(bodyConfig, modelConfig);
                name = name.replaceAll(".*[/\\\\]", "");
                return SafeURLPaths.instance().getString(serverPath, subPath, name);
            }
        };
    }

    public static FileLocator prependServerPath(final SBMTBodyConfiguration bodyConfig, final ShapeModelConfiguration modelConfig, final String subPath) {
        return new FileLocator() {
            @Override
            public String getLocation(String name)
            {
                String serverPath = serverPath(bodyConfig, modelConfig);
                return SafeURLPaths.instance().getString(serverPath, subPath, name);
            }
        };
    }

    private static String serverPath(SBMTBodyConfiguration bodyConfig, ShapeModelConfiguration modelConfig) {
        return SafeURLPaths.instance().getString("/" + bodyConfig.get(SBMTBodyConfiguration.BODY_NAME), modelConfig.get(ShapeModelConfiguration.AUTHOR)).toLowerCase();
    }
}
