package edu.jhuapl.sbmt.config;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.beust.jcommander.internal.Lists;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import edu.jhuapl.saavtk.config.ConfigArrayList;
import edu.jhuapl.saavtk.config.IBodyViewConfig;
import edu.jhuapl.saavtk.config.ViewConfig;
import edu.jhuapl.saavtk.model.ShapeModelBody;
import edu.jhuapl.saavtk.model.ShapeModelType;
import edu.jhuapl.saavtk.util.Configuration;
import edu.jhuapl.saavtk.util.FileCache;
import edu.jhuapl.saavtk.util.SafeURLPaths;
import edu.jhuapl.sbmt.client2.SbmtMultiMissionTool;
import edu.jhuapl.sbmt.client2.configs.AsteroidConfigs;
import edu.jhuapl.sbmt.client2.configs.BennuConfigs;
import edu.jhuapl.sbmt.client2.configs.CometConfigs;
import edu.jhuapl.sbmt.client2.configs.DartConfigs;
import edu.jhuapl.sbmt.client2.configs.MarsConfigs;
import edu.jhuapl.sbmt.client2.configs.NewHorizonsConfigs;
import edu.jhuapl.sbmt.client2.configs.RyuguConfigs;
import edu.jhuapl.sbmt.client2.configs.SaturnConfigs;
import edu.jhuapl.sbmt.core.body.BodyViewConfig;
import edu.jhuapl.sbmt.core.client.Mission;
import edu.jhuapl.sbmt.core.config.ISmallBodyViewConfig;
import edu.jhuapl.sbmt.core.config.Instrument;
import edu.jhuapl.sbmt.core.search.HierarchicalSearchSpecification;
import edu.jhuapl.sbmt.image.interfaces.ImageKeyInterface;
import edu.jhuapl.sbmt.spectrum.model.core.search.SpectraHierarchicalSearchSpecification;

import crucible.crust.metadata.api.Key;
import crucible.crust.metadata.impl.FixedMetadata;
import crucible.crust.metadata.impl.SettableMetadata;
import crucible.crust.metadata.impl.gson.Serializers;

/**
* A SmallBodyConfig is a class for storing all which models should be instantiated
* together with a particular small body. For example some models like Eros
* have imaging, spectral, and lidar data whereas other models may only have
* imaging data. This class is also used when creating (to know which tabs
* to create).
*/
public class SmallBodyViewConfig extends BodyViewConfig implements ISmallBodyViewConfig
{
    protected static final Mission[] DefaultForNoMissions = new Mission[] {};

    //system bodies
    public List<SmallBodyViewConfig> systemConfigs = Lists.newArrayList();
    public boolean hasSystemBodies = false;

    static public boolean fromServer = false;
	private static final List<BasicConfigInfo> CONFIG_INFO = new ArrayList<>();
    private static final Map<String, BasicConfigInfo> VIEWCONFIG_IDENTIFIERS = new HashMap<>();
    private static final Map<String, SmallBodyViewConfig> LOADED_VIEWCONFIGS = new HashMap<>();

    protected String baseMapConfigName = "config.txt";
    protected String baseMapConfigNamev2 = "basemap_config.txt";

    static public List<BasicConfigInfo> getConfigIdentifiers() { return CONFIG_INFO; }

    static public SmallBodyViewConfig getSmallBodyConfig(BasicConfigInfo configInfo)
    {
    	ShapeModelBody bodyType = configInfo.getBody();
    	ShapeModelType authorType = configInfo.getAuthor();
    	String version = configInfo.getVersion();

    	if (authorType == ShapeModelType.CUSTOM)
    	{
    		return SmallBodyViewConfig.ofCustom(configInfo.getShapeModelName(), false);
    	}
    	if (version != null)
    	{
    		return getSmallBodyConfig(bodyType, authorType, version);
    	}
    	else
    	{
    		return getSmallBodyConfig(bodyType, authorType, false);
    	}


    }

    /**
     * Return the model config uniquely identified by the arguments, none of
     * which may be null.
     *
     * @param name the shape model's body
     * @param author the shape model type/author
     * @return the config
     * @see #getSmallBodyConfig(String) for more details
     */
    public static SmallBodyViewConfig getSmallBodyConfig(ShapeModelBody name, ShapeModelType author)
    {
       return getSmallBodyConfig(name, author, false);
    }

    /**
     * Return the model config uniquely identified by the arguments, none of
     * which may be null.
     *
     * @param name the shape model's body
     * @param author the shape model type/author
     * @return the config
     * @see #getSmallBodyConfig(String) for more details
     */
    public static SmallBodyViewConfig getSmallBodyConfig(ShapeModelBody name, ShapeModelType author, boolean partOfSystem)
    {
        Preconditions.checkNotNull(name);
        Preconditions.checkNotNull(author);

        return getSmallBodyConfig(author.toString() + "/" + name.toString(), partOfSystem);
    }

    /**
     * Return the model config uniquely identified by the arguments, none of
     * which may be null.
     *
     * @param name the shape model's body
     * @param author the shape model type/author
     * @param version the version
     * @return the config
     * @see #getSmallBodyConfig(String) for more details
     */
    public static SmallBodyViewConfig getSmallBodyConfig(ShapeModelBody name, ShapeModelType author, String version)
    {
        Preconditions.checkNotNull(name);
        Preconditions.checkNotNull(author);
        Preconditions.checkNotNull(version);

        return getSmallBodyConfig(author.toString() + "/" + name.toString() + " (" + version + ")", false);
    }

    /**
     * Return the model config uniquely identified by the specified identifier,
     * loading the config from the server. The first time the config is
     * successfully loaded, it is cached and subsequent calls to this method
     * simply return the cached instance.
     * <p>
     * This method never returns null; if it has a problem retrieving the config
     * it throws an exception.
     *
     * @param configId the identifier of the config
     * @return the config
     * @throws various {@link RuntimeException}s
     * @see #fetchRemoteConfig(String, String)
     */
    protected static SmallBodyViewConfig getSmallBodyConfig(String configId, boolean partOfSystem)
    {
        SmallBodyViewConfig config = LOADED_VIEWCONFIGS.get(configId);
        if (config == null || config.hasSystemBodies())
        {
            Preconditions.checkArgument(VIEWCONFIG_IDENTIFIERS.containsKey(configId), "No configuration available for model " + configId);

            config = fetchRemoteConfig(configId, VIEWCONFIG_IDENTIFIERS.get(configId).getConfigURL(), partOfSystem);

            LOADED_VIEWCONFIGS.put(configId, config);
        }
        return config;
    }

    private static List<ViewConfig> addRemoteEntries()
    {
    	ConfigArrayList<ViewConfig> configs = new ConfigArrayList<>();
        try
        {
            File allBodies = FileCache.getFileFromServer(BasicConfigInfo.getConfigPathPrefix(SbmtMultiMissionTool.getMission().isPublishedDataOnly()) + "/" + "allBodies_v" + BasicConfigInfo.getConfigInfoVersion() + ".json");
            FixedMetadata metadata = Serializers.deserialize(allBodies, "AllBodies");
            for (Key<?> key : metadata.getKeys())
            {
            	//Dynamically add a ShapeModelType if needed
            	SettableMetadata infoMetadata = (SettableMetadata)metadata.get(key);
            	BasicConfigInfo configInfo = new BasicConfigInfo();
            	configInfo.retrieve(infoMetadata);
            	CONFIG_INFO.add(configInfo);
            	VIEWCONFIG_IDENTIFIERS.put(key.toString(), configInfo);
            	if (configInfo.getUniqueName().equals("Gaskell/433 Eros"))
            	{
            		configs.add(getSmallBodyConfig(ShapeModelBody.EROS, ShapeModelType.GASKELL, false));
            	}
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return configs;

    }

    /**
     * Fetch the config from the model metadata file located on the server,
     * downloading/updating the file cache as needed. This method does not
     * return null but throws an exception if it fails to load the model
     * metadata.
     *
     * @param name of the model metadata object in the file
     * @param url from which to download/load the metadata
     * @return the config
     */
    private static SmallBodyViewConfig fetchRemoteConfig(String name, String url, boolean partOfSystem)
    {
        ConfigArrayList<SmallBodyViewConfig> ioConfigs = new ConfigArrayList<>();
        ioConfigs.add(new SmallBodyViewConfig(ImmutableList.<String>copyOf(DEFAULT_GASKELL_LABELS_PER_RESOLUTION), ImmutableList.<Integer>copyOf(DEFAULT_GASKELL_NUMBER_PLATES_PER_RESOLUTION)));
        SmallBodyViewConfigMetadataIO io = new SmallBodyViewConfigMetadataIO(ioConfigs);
        try
        {
            File configFile = FileCache.getFileFromServer(url);
            FixedMetadata metadata = Serializers.deserialize(configFile, name);
            io.retrieve(metadata, partOfSystem);
            SmallBodyViewConfig c = (SmallBodyViewConfig) io.getConfigs().get(0);
            if (c == null)
            {
                throw new NullPointerException(String.format("Unable to load the configuration for model \"%s\" from URL %s", name, url));
            }
            return c;
        }
        catch (RuntimeException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }

    }

    public static SmallBodyViewConfig ofCustom(String name, boolean temporary)
    {
        SmallBodyViewConfig config = new SmallBodyViewConfig(ImmutableList.<String>of(name), ImmutableList.<Integer>of(1));
        config.modelLabel = name;
        config.customTemporary = temporary;
        config.author = ShapeModelType.CUSTOM;

        SafeURLPaths safeUrlPaths = SafeURLPaths.instance();
        String fileName = temporary ? safeUrlPaths.getUrl(config.modelLabel) : safeUrlPaths.getUrl(safeUrlPaths.getString(Configuration.getImportedShapeModelsDir(), config.modelLabel, "model.vtk"));

        config.shapeModelFileNames = new String[] { fileName };

        return config;
    }

    static void initializeWithStaticConfigs(boolean publicOnly)
    {
    	ConfigArrayList<IBodyViewConfig> configArray = getBuiltInConfigs();
		AsteroidConfigs.initialize(configArray);
		BennuConfigs.initialize(configArray, publicOnly);
		DartConfigs.instance().initialize(configArray);
		CometConfigs.initialize(configArray);
		MarsConfigs.initialize(configArray, publicOnly);
		NewHorizonsConfigs.initialize(configArray);
		RyuguConfigs.initialize(configArray);
		SaturnConfigs.initialize(configArray);
    }



    public static void initialize()
    {
    	ConfigArrayList<IBodyViewConfig> configArray = getBuiltInConfigs();
        configArray.addAll(addRemoteEntries());
    }

    private List<ImageKeyInterface> imageMapKeys = null;

    public SmallBodyViewConfig(Iterable<String> resolutionLabels, Iterable<Integer> resolutionNumberElements)
    {
        super(resolutionLabels, resolutionNumberElements);
    }

    SmallBodyViewConfig()
    {
        super(ImmutableList.<String>copyOf(DEFAULT_GASKELL_LABELS_PER_RESOLUTION), ImmutableList.<Integer>copyOf(DEFAULT_GASKELL_NUMBER_PLATES_PER_RESOLUTION));
    }

    @Override
    public SmallBodyViewConfig clone() // throws CloneNotSupportedException
    {
        SmallBodyViewConfig c = (SmallBodyViewConfig) super.clone();
        return c;
    }

    @Override
    public boolean isAccessible()
    {
        return FileCache.instance().isAccessible(getShapeModelFileNames()[0]);
    }

    @Override
    public Instrument getLidarInstrument()
    {
        return lidarInstrumentName;
    }

    public boolean hasHypertreeLidarSearch()
    {
        return hasHypertreeBasedLidarSearch;
    }

    public SpectraHierarchicalSearchSpecification<?> getHierarchicalSpectraSearchSpecification()
    {
        return hierarchicalSpectraSearchSpecification;
    }

//    @Deprecated
//    @Override
//    public List<ImageKeyInterface> getImageMapKeys()
//    {
//        if (!hasImageMap)
//        {
//            return ImmutableList.of();
//        }
//
//            if (imageMapKeys == null)
//            {
//                List<ImageKeyInterface> imageMapKeys = ImmutableList.of();
//
//                // Newest/best way to specify maps is with metadata, if this model has it.
//                String metadataFileName = SafeURLPaths.instance().getString(serverPath("basemap"), baseMapConfigName);
//                File metadataFile;
//                try
//                {
//                    metadataFile = FileCache.getFileFromServer(metadataFileName);
//                }
//                catch (Exception ignored)
//                {
//                    // This file is optional.
//                    metadataFile = null;
//                    return ImmutableList.of();
//                }
//
//                if (metadataFile != null && metadataFile.isFile())
//                {
//                    // Proceed using metadata.
//                    try
//                    {
//                        Metadata metadata = Serializers.deserialize(metadataFile, "CustomImages");
//                        imageMapKeys = metadata.get(Key.of("customImages"));
//                    }
//                    catch (Exception e)
//                    {
//                        // This ought to have worked so report this exception.
//                        e.printStackTrace();
//                    }
//                }
//                else
//                {
//                // Final option (legacy behavior). The key is hardwired. The file could be in
//                // either of two places.
//                    if (FileCache.isFileGettable(serverPath("image_map.png")))
//                    {
//                        imageMapKeys = ImmutableList.of(new CustomCylindricalImageKey("image_map", "image_map.png", ImageType.GENERIC_IMAGE, PointingSource.IMAGE_MAP, new Date(), "image_map"));
//                    }
//                    else if (FileCache.isFileGettable(serverPath("basemap/image_map.png")))
//                    {
//                        imageMapKeys = ImmutableList.of(new CustomCylindricalImageKey("image_map", "basemap/image_map.png", ImageType.GENERIC_IMAGE, PointingSource.IMAGE_MAP, new Date(), "image_map"));
//                    }
//                }
//
//                this.imageMapKeys = correctMapKeys(imageMapKeys, metadataFile.getParent());
//            }
//
//        return imageMapKeys;
//    }

//    @Override
//    public List<BasemapImage> getBasemapImages()
//    {
//    	List<BasemapImage> basemapImages = Lists.newArrayList();
//    	String metadataFileName = SafeURLPaths.instance().getString(serverPath("basemap"), baseMapConfigNamev2);
//    	File metadataFile;
//        try
//        {
//            metadataFile = FileCache.getFileFromServer(metadataFileName);
//            FixedMetadata readInMetadata = Serializers.deserialize(metadataFile, "Basemaps");
//    		basemapImages = readInMetadata.get(Key.of("basemapCollection"));
//    		for (BasemapImage image : basemapImages)
//    		{
//    			image.setPointingFileName(SafeURLPaths.instance().getString(serverPath("basemap"), image.getPointingFileName()));
//    			image.setImageFilename(SafeURLPaths.instance().getString(serverPath("basemap"), image.getImageFilename()));
//    		}
//        }
//        catch (Exception ignored)
//        {
//            return ImmutableList.of();
//        }
//
//		return basemapImages;
//    }

//    /**
//     * This converts keys with short names, file names, and original names to
//     * full-fledged keys that image creators can handle. The short form is more
//     * convenient and idiomatic for storage and for configuration purposes, but the
//     * longer form can actually be used to create a cylindrical image object.
//     *
//     * If/when image key classes are revamped, the shorter form would actually be
//     * preferable. The name is actually supposed to be the display name, and the
//     * original name is most likely intended to hold the "original file name" in
//     * cases where a file is imported into the custom area.
//     *
//     * @param keys the input (shorter) keys
//     * @return the output (full-fledged) keys
//     */
//    private List<ImageKeyInterface> correctMapKeys(List<ImageKeyInterface> keys, String metadataDir)
//    {
//        ImmutableList.Builder<ImageKeyInterface> builder = ImmutableList.builder();
//        for (ImageKeyInterface k : keys)
//        {
//        	if (k instanceof CustomCylindricalImageKey)
//        	{
//        		CustomCylindricalImageKey key = (CustomCylindricalImageKey)k;
//	            String fileName = serverPath(key.getImageFilename());
//
//	            CustomCylindricalImageKey correctedKey = new CustomCylindricalImageKey(fileName, fileName, ImageType.GENERIC_IMAGE, PointingSource.IMAGE_MAP, new Date(), key.getOriginalName());
//
//	            correctedKey.setLllat(key.getLllat());
//	            correctedKey.setLllon(key.getLllon());
//	            correctedKey.setUrlat(key.getUrlat());
//	            correctedKey.setUrlon(key.getUrlon());
//
//	            builder.add(correctedKey);
//	        }
//        	else
//        	{
//        		CustomPerspectiveImageKey key = (CustomPerspectiveImageKey)k;
//        		String imageFileName = SafeURLPaths.instance().getString(serverPath("basemap"), key.getImageFilename());
//        		String infoFileName = SafeURLPaths.instance().getString(serverPath("basemap"), key.getPointingFile());
//        		FileCache.getFileFromServer(imageFileName);
//        		FileCache.getFileFromServer(infoFileName);
//        		String fileName = SafeURLPaths.instance().getUrl(metadataDir + File.separator + key.getImageFilename());
//        		CustomPerspectiveImageKey correctedKey = new CustomPerspectiveImageKey(fileName, fileName, key.getSource(), key.getImageType(), key.getRotation(), key.getFlip(), key.getFileType(), infoFileName,
//        				key.getDate(), key.getOriginalName());
//
//        		builder.add(correctedKey);
//        	}
//        }
//
//        return builder.build();
//    }

	@Override
	public boolean hasHypertreeBasedSpectraSearch()
	{
		return hasHypertreeBasedSpectraSearch;
	}

	@Override
	public boolean hasHierarchicalSpectraSearch()
	{
		return hasHierarchicalSpectraSearch;
	}

	@Override
	public Date getDefaultImageSearchStartDate()
	{
		return imageSearchDefaultStartDate;
	}

	@Override
	public Date getDefaultImageSearchEndDate()
	{
		return imageSearchDefaultEndDate;
	}

	@Override
	public HierarchicalSearchSpecification getHierarchicalImageSearchSpecification()
	{
		return hierarchicalImageSearchSpecification;
	}

	@Override
	public String getTimeHistoryFile()
	{
		return timeHistoryFile;
	}

	@Override
	public ShapeModelType getAuthor()
	{
		return author;
	}

	@Override
	public String getRootDirOnServer()
	{
		return rootDirOnServer;
	}

	@Override
	public boolean isCustomTemporary()
	{
		return super.isCustomTemporary();
	}

	public boolean hasSystemBodies()
	{
		return hasSystemBodies;
	};

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((imageMapKeys == null) ? 0 : imageMapKeys.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
		{
			return true;
		}
		if (!super.equals(obj))
		{
			return false;
		}
//		if (getClass() != obj.getClass())
//		{
//			System.out.println("SmallBodyViewConfig: equals: classes are wrong " + getClass() + " " + obj.getClass());
//			return false;
//		}
		SmallBodyViewConfig other = (SmallBodyViewConfig) obj;
		if (imageMapKeys == null)
		{
			if (other.imageMapKeys != null)
			{
				return false;
			}
		} else if (!imageMapKeys.equals(other.imageMapKeys))
		{
			return false;
		}

//		System.out.println("SmallBodyViewConfig: equals: match, returning true!!!!");
		return true;
	}

	@Override
	public String toString()
	{
		return "SmallBodyViewConfig [imageMapKeys=" + imageMapKeys + ", rootDirOnServer=" + rootDirOnServer
				+ ", shapeModelFileBaseName=" + shapeModelFileBaseName + ", shapeModelFileExtension="
				+ shapeModelFileExtension + ", shapeModelFileNames=" + Arrays.toString(shapeModelFileNames)
				+ ", timeHistoryFile=" + timeHistoryFile + ", density=" + density + ", rotationRate=" + rotationRate
				+ ", hasFlybyData=" + hasFlybyData + ", hasStateHistory=" + hasStateHistory + ", hasColoringData="
				+ hasColoringData + ", hasMapmaker=" + hasMapmaker
				+ ", hasRemoteMapmaker=" + hasRemoteMapmaker + ", bodyReferencePotential=" + bodyReferencePotential + ", bodyLowestResModelName="
				+ bodyLowestResModelName + ", hasBigmap=" + hasBigmap + ", hasSpectralData=" + hasSpectralData
				+ ", hasLineamentData=" + hasLineamentData + ", imageSearchDefaultStartDate="
				+ imageSearchDefaultStartDate + ", imageSearchDefaultEndDate=" + imageSearchDefaultEndDate
				+ ", imageSearchFilterNames=" + Arrays.toString(imageSearchFilterNames)
				+ ", imageSearchUserDefinedCheckBoxesNames=" + Arrays.toString(imageSearchUserDefinedCheckBoxesNames)
				+ ", imageSearchDefaultMaxSpacecraftDistance=" + imageSearchDefaultMaxSpacecraftDistance
				+ ", imageSearchDefaultMaxResolution=" + imageSearchDefaultMaxResolution
				+ ", hasHierarchicalImageSearch=" + hasHierarchicalImageSearch + ", hasHierarchicalSpectraSearch="
				+ hasHierarchicalSpectraSearch + ", hierarchicalImageSearchSpecification="
				+ hierarchicalImageSearchSpecification + ", hierarchicalSpectraSearchSpecification="
				+ hierarchicalSpectraSearchSpecification + ", spectrumMetadataFile=" + spectrumMetadataFile
				+ ", hasHypertreeBasedSpectraSearch=" + hasHypertreeBasedSpectraSearch + ", spectraSearchDataSourceMap="
				+ spectraSearchDataSourceMap + ", hasHypertreeBasedLidarSearch=" + hasHypertreeBasedLidarSearch
				+ ", lidarSearchDataSourceMap=" + lidarSearchDataSourceMap + ", lidarBrowseDataSourceMap="
				+ lidarBrowseDataSourceMap + ", lidarSearchDataSourceTimeMap=" + lidarSearchDataSourceTimeMap
				+ ", orexSearchTimeMap=" + orexSearchTimeMap + ", lidarBrowseOrigPathRegex=" + lidarBrowseOrigPathRegex
				+ ", lidarBrowsePathTop=" + lidarBrowsePathTop + ", lidarBrowseXYZIndices="
				+ Arrays.toString(lidarBrowseXYZIndices) + ", lidarBrowseSpacecraftIndices="
				+ Arrays.toString(lidarBrowseSpacecraftIndices) + ", lidarBrowseOutgoingIntensityIndex="
				+ lidarBrowseOutgoingIntensityIndex + ", lidarBrowseReceivedIntensityIndex="
				+ lidarBrowseReceivedIntensityIndex + ", lidarBrowseRangeIndex=" + lidarBrowseRangeIndex
				+ ", lidarBrowseIsRangeExplicitInData=" + lidarBrowseIsRangeExplicitInData
				+ ", lidarBrowseIntensityEnabled=" + lidarBrowseIntensityEnabled
				+ ", lidarBrowseIsLidarInSphericalCoordinates=" + lidarBrowseIsLidarInSphericalCoordinates
				+ ", lidarBrowseIsSpacecraftInSphericalCoordinates=" + lidarBrowseIsSpacecraftInSphericalCoordinates
				+ ", lidarBrowseIsTimeInET=" + lidarBrowseIsTimeInET + ", lidarBrowseTimeIndex=" + lidarBrowseTimeIndex
				+ ", lidarBrowseNoiseIndex=" + lidarBrowseNoiseIndex + ", lidarBrowseFileListResourcePath="
				+ lidarBrowseFileListResourcePath + ", lidarBrowseNumberHeaderLines=" + lidarBrowseNumberHeaderLines
				+ ", lidarBrowseIsBinary=" + lidarBrowseIsBinary + ", lidarBrowseBinaryRecordSize="
				+ lidarBrowseBinaryRecordSize + ", lidarBrowseIsInMeters=" + lidarBrowseIsInMeters
				+ ", lidarOffsetScale=" + lidarOffsetScale + ", hasLidarData=" + hasLidarData
				+ ", lidarSearchDefaultStartDate=" + lidarSearchDefaultStartDate + ", lidarSearchDefaultEndDate="
				+ lidarSearchDefaultEndDate + ", presentInMissions=" + Arrays.toString(presentInMissions)
				+ ", defaultForMissions=" + Arrays.toString(defaultForMissions) + ", dtmBrowseDataSourceMap="
				+ dtmBrowseDataSourceMap + ", dtmSearchDataSourceMap=" + dtmSearchDataSourceMap + ", type=" + type
				+ ", population=" + population + ", system=" + system + ", dataUsed=" + dataUsed + ", imagingInstruments="
				+ Arrays.toString(imagingInstruments) + ", lidarInstrumentName=" + lidarInstrumentName
				+ ", spectralInstruments=" + spectralInstruments + ", databaseRunInfos="
				+ Arrays.toString(databaseRunInfos) + ", modelLabel=" + modelLabel + ", customTemporary="
				+ customTemporary + ", author=" + author + ", version=" + version + ", body=" + body
				+ ", useMinimumReferencePotential=" + useMinimumReferencePotential + ", hasCustomBodyCubeSize="
				+ hasCustomBodyCubeSize + ", customBodyCubeSize=" + customBodyCubeSize + "]";
	}
}
