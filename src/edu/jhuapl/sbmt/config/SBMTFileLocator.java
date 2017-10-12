package edu.jhuapl.sbmt.config;

import edu.jhuapl.saavtk.config.Entry;
import edu.jhuapl.saavtk.config.ExtensibleTypedLookup;
import edu.jhuapl.saavtk.config.FixedTypedLookup;
import edu.jhuapl.saavtk.config.Key;
import edu.jhuapl.saavtk.util.file.FileLocator;

public class SBMTFileLocator extends ExtensibleTypedLookup
{
    // Required keys.
    public static final Key<FileLocator> TOP_PATH = Key.of("Path to the top of the data file tree");
    public static final Key<FileLocator> IMAGE_FILE = Key.of("Image file base name");

    // Optional keys.
    public static final Key<FileLocator> INFO_FILE = Key.of("Info file base name");
    public static final Key<FileLocator> SUM_FILE = Key.of("Sum file base name");
    public static final Key<FileLocator> GALLERY_FILE = Key.of("Gallery file base name");

    private static final Key<FixedTypedLookup.Builder> BUILDER_KEY = Key.of("ImageFileLocator builder");

    public static ExtensibleTypedLookup.Builder<SBMTFileLocator> builder(FileLocator topPathLocator, FileLocator imageFileLocator) {
        if (topPathLocator == null || imageFileLocator == null) throw new NullPointerException();

        final FixedTypedLookup.Builder fixedBuilder = FixedTypedLookup.builder(BUILDER_KEY);

        // Required keys.
        fixedBuilder.put(Entry.of(TOP_PATH, topPathLocator));
        fixedBuilder.put(Entry.of(IMAGE_FILE, imageFileLocator));

        return new Builder<SBMTFileLocator>(fixedBuilder) {
            @Override
            public SBMTFileLocator build()
            {
                if (!containsKey(INFO_FILE) && !containsKey(SUM_FILE))
                {
                    throw new UnsupportedOperationException("Need a pointing file locator");
                }
                return new SBMTFileLocator(fixedBuilder);
            }
        };
    }

    protected SBMTFileLocator(FixedTypedLookup.Builder builder)
    {
        super(BUILDER_KEY, builder);
    }
}
