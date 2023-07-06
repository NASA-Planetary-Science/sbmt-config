package edu.jhuapl.sbmt.config;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.stream.Collectors;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

import edu.jhuapl.saavtk.config.IBodyViewConfig;
import edu.jhuapl.saavtk.model.ShapeModelBody;
import edu.jhuapl.saavtk.model.ShapeModelType;
import edu.jhuapl.saavtk.util.Configuration;
import edu.jhuapl.sbmt.core.body.BodyType;
import edu.jhuapl.sbmt.core.body.BodyViewConfig;
import edu.jhuapl.sbmt.core.body.ShapeModelDataUsed;
import edu.jhuapl.sbmt.core.body.ShapeModelPopulation;
import edu.jhuapl.sbmt.core.client.Mission;
import edu.jhuapl.sbmt.core.config.FeatureConfigIOFactory;
import edu.jhuapl.sbmt.core.config.IFeatureConfig;
import edu.jhuapl.sbmt.core.config.Instrument;
import edu.jhuapl.sbmt.core.search.HierarchicalSearchSpecification;
import edu.jhuapl.sbmt.image.model.ImagingInstrument;
import edu.jhuapl.sbmt.model.bennu.spectra.otes.OTES;
import edu.jhuapl.sbmt.model.bennu.spectra.otes.OTESQuery;
import edu.jhuapl.sbmt.model.bennu.spectra.otes.OTESSpectrumMath;
import edu.jhuapl.sbmt.model.bennu.spectra.ovirs.OVIRS;
import edu.jhuapl.sbmt.model.bennu.spectra.ovirs.OVIRSQuery;
import edu.jhuapl.sbmt.model.bennu.spectra.ovirs.OVIRSSpectrumMath;
import edu.jhuapl.sbmt.model.eros.nis.NIS;
import edu.jhuapl.sbmt.model.eros.nis.NISSpectrumMath;
import edu.jhuapl.sbmt.model.eros.nis.NisQuery;
import edu.jhuapl.sbmt.model.phobos.MEGANE;
import edu.jhuapl.sbmt.model.phobos.MEGANEQuery;
import edu.jhuapl.sbmt.model.phobos.MEGANESpectrumMath;
import edu.jhuapl.sbmt.model.ryugu.nirs3.NIRS3;
import edu.jhuapl.sbmt.model.ryugu.nirs3.NIRS3Query;
import edu.jhuapl.sbmt.model.ryugu.nirs3.NIRS3SpectrumMath;
import edu.jhuapl.sbmt.pointing.spice.SpiceInfo;
import edu.jhuapl.sbmt.spectrum.model.core.BasicSpectrumInstrument;
import edu.jhuapl.sbmt.spectrum.model.core.SpectraTypeFactory;
import edu.jhuapl.sbmt.spectrum.model.core.SpectrumInstrumentFactory;
import edu.jhuapl.sbmt.spectrum.model.io.SpectrumInstrumentMetadataIO;
import edu.jhuapl.sbmt.tools.DBRunInfo;

import crucible.crust.metadata.api.Key;
import crucible.crust.metadata.api.Metadata;
import crucible.crust.metadata.api.MetadataManager;
import crucible.crust.metadata.api.Version;
import crucible.crust.metadata.impl.FixedMetadata;
import crucible.crust.metadata.impl.SettableMetadata;
import crucible.crust.metadata.impl.gson.Serializers;

public class SmallBodyViewConfigMetadataIO implements MetadataManager
{

	//TODO: This needs a new home
	static {
		SpectrumInstrumentFactory.registerType("NIS", new NIS());
		SpectrumInstrumentFactory.registerType("OTES", new OTES());
		SpectrumInstrumentFactory.registerType("OVIRS", new OVIRS());
		SpectrumInstrumentFactory.registerType("NIRS3", new NIRS3());
		SpectrumInstrumentFactory.registerType("MEGANE", new MEGANE());
		SpectraTypeFactory.registerSpectraType("OTES", OTESQuery.getInstance(), OTESSpectrumMath.getInstance(), "cm^-1", new OTES().getBandCenters());
		SpectraTypeFactory.registerSpectraType("OVIRS", OVIRSQuery.getInstance(), OVIRSSpectrumMath.getInstance(), "um", new OVIRS().getBandCenters());
		SpectraTypeFactory.registerSpectraType("NIS", NisQuery.getInstance(), NISSpectrumMath.getSpectrumMath(), "nm", new NIS().getBandCenters());
		SpectraTypeFactory.registerSpectraType("NIRS3", NIRS3Query.getInstance(), NIRS3SpectrumMath.getInstance(), "nm", new NIRS3().getBandCenters());
		SpectraTypeFactory.registerSpectraType("MEGANE", MEGANEQuery.getInstance(), MEGANESpectrumMath.getInstance(), "cm^-1", new MEGANE().getBandCenters());
	}

    public static void main(String[] args) throws IOException
    {
        if (args.length < 1 || args.length > 2)
        {
            System.err.println("Usage: SmallBodyViewConfigMetadataIO.sh <output-directory-full-path> [ -pub ]\n\n\t-pub means include only published data in the output model metadata; if omitted, include ALL data\n\n\tThe output directory will be created if it does not exist");
            System.exit(1);
        }

        boolean publishedDataOnly = args.length > 1 && (args[1].equalsIgnoreCase("-pub") || args[1].equalsIgnoreCase("--pub"));

        String configInfoVersion = BasicConfigInfo.getConfigInfoVersion();

        SettableMetadata allBodiesMetadata = SettableMetadata.of(Version.of(configInfoVersion));
        Configuration.setAPLVersion(true);
        edu.jhuapl.sbmt.client2.SbmtMultiMissionTool.configureMission();
        Configuration.authenticate();
        SmallBodyViewConfig.initializeWithStaticConfigs(publishedDataOnly);
        for (IBodyViewConfig each: SmallBodyViewConfig.getBuiltInConfigs())
        {
            each.enable(true);
        }

        String rootDir = args[0].replaceFirst("/*$", "/") + BasicConfigInfo.getConfigPathPrefix(publishedDataOnly) + "/";

        // Create the directory just in case. Then make sure it exists before proceeding.
        File rootDirFile = new File(rootDir);
        rootDirFile.mkdirs();
        if (!rootDirFile.isDirectory())
        {
            throw new IOException("Unable to create root config directory " + rootDir);
        }

        List<IBodyViewConfig> builtInConfigs = SmallBodyViewConfig.getBuiltInConfigs();
        System.out.println("SmallBodyViewConfigMetadataIO: main: ---------------------------------");
        for (IBodyViewConfig config : builtInConfigs)
        {
            try
            {
            	SmallBodyViewConfigMetadataIO io = new SmallBodyViewConfigMetadataIO(config);
                String version = config.getVersion() == null ? "" : config.getVersion();
                File file = null;
                if (!config.hasSystemBodies())
                	file = new File(rootDir + ((SmallBodyViewConfig) config).rootDirOnServer + "/" + config.getAuthor() + "_" + config.getBody().toString().replaceAll(" ", "_") + version.replaceAll(" ", "_") + "_v" + configInfoVersion + ".json");
                else
                {
                	String bodyName = config.getBody().toString();
                	bodyName = bodyName.replaceAll(" ", "_");

                	String systemRoot = "/" + config.getBody().name().replaceAll("[\\s-_]+", "-").toLowerCase() + "/" + config.getAuthor().name().replaceAll("[\\s-_]+", "-").toLowerCase();
                    systemRoot = systemRoot.replaceAll("\\(", "");
                    systemRoot = systemRoot.replaceAll("\\)", "");
                    systemRoot = systemRoot.replaceAll("-\\w*-center", "");

                	String fileNameString = rootDir + systemRoot + "/" + config.getAuthor() /*+ "_" + config.getBody().toString().replaceAll(" ", "_") + "_System_"  + bodyName.toLowerCase() + "center" + version.replaceAll(" ", "_") */ + "_v" + configInfoVersion + ".json";

                	fileNameString = fileNameString.replaceAll("\\(", "");
                	fileNameString = fileNameString.replaceAll("\\)", "");
                	file = new File(fileNameString);
                }
                BasicConfigInfo configInfo = new BasicConfigInfo((BodyViewConfig)config, publishedDataOnly);
                allBodiesMetadata.put(Key.of(config.getUniqueName()), configInfo.store());

                if (!file.exists()) file.getParentFile().mkdirs();
                Metadata outgoingMetadata = io.store();
                io.write(config.getUniqueName(), file, outgoingMetadata);

                //read in data from file to do sanity check
                SmallBodyViewConfig cfg = new SmallBodyViewConfig();
                SmallBodyViewConfigMetadataIO io2 = new SmallBodyViewConfigMetadataIO(cfg);
                FixedMetadata metadata = Serializers.deserialize(file, config.getUniqueName());
                io2.metadataID = config.getUniqueName();
                io2.retrieve(metadata);
                checkEquality(cfg, config);

            }
            catch (Exception e)
            {
                System.err.println("WARNING: EXCEPTION! SKIPPING CONFIG " + config.getUniqueName());
                e.printStackTrace();
            }
        }

        Serializers.serialize("AllBodies", allBodiesMetadata, new File(rootDir + "allBodies_v" + configInfoVersion + ".json"));


    }

    /**
     * Perform the equality check in its own method so that one can more easily
     * debug. Set a breakpoint at the println, then drop-to-frame and re-run the
     * call to equals.
     */
    private static void checkEquality(IBodyViewConfig cfg, IBodyViewConfig config)
    {
        if (!cfg.equals(config))
            System.err.println("SmallBodyViewConfigMetadataIO: main: cfg equals config is " + (cfg.equals(config) + " for " + config.getUniqueName()));
    }

    private List<IBodyViewConfig> configs;
    private String metadataID;

    public SmallBodyViewConfigMetadataIO()
    {
        this(ImmutableList.of());
    }

    public SmallBodyViewConfigMetadataIO(List<? extends IBodyViewConfig> configs)
    {
        this.configs = new Vector<>(configs);
    }

    public SmallBodyViewConfigMetadataIO(IBodyViewConfig config)
    {
        this.configs = new Vector<IBodyViewConfig>();
        this.configs.add(config);
    }

    public void write(File file, String metadataID) throws IOException
    {
        Serializers.serialize(metadataID, store(), file);
    }

    private void write(String metadataID, File file, Metadata metadata) throws IOException
    {
        Serializers.serialize(metadataID, metadata, file);
    }

    public void read(File file, String metadataID, SmallBodyViewConfig config) throws IOException
    {
    	String[] modelFileNames = config.getShapeModelFileNames();
        FixedMetadata metadata = Serializers.deserialize(file, metadataID);
        this.metadataID = metadataID;
        configs.add(config);
        retrieve(metadata);
        config.setShapeModelFileNames(modelFileNames);
    }

    private SettableMetadata storeConfig(IBodyViewConfig config)
    {
        SmallBodyViewConfig c = (SmallBodyViewConfig)config;
        SettableMetadata configMetadata = SettableMetadata.of(Version.of(BasicConfigInfo.getConfigInfoVersion()));
        writeEnum(body, c.body, configMetadata);
        writeEnum(type, c.type, configMetadata);
        write(version, c.version, configMetadata);
        writeEnum(population, c.population, configMetadata);
        writeEnum(system, c.system, configMetadata);
        writeEnum(dataUsed, c.dataUsed, configMetadata);
        write(author, c.author.name(), configMetadata);
        write(modelLabel, c.modelLabel, configMetadata);
        write(bodyLowestResModelName, c.bodyLowestResModelName, configMetadata);
        write(rootDirOnServer, c.rootDirOnServer, configMetadata);
        write(shapeModelFileExtension, c.getShapeModelFileExtension(), configMetadata);
        write(shapeModelFileBaseName, c.getShapeModelFileBaseName(), configMetadata);
        write(shapeModelFileNamesKey, c.getShapeModelFileNames(), configMetadata);
        String[] resolutionsToSave = new String[c.getResolutionLabels().size()];
        Integer[] platesPerResToSave = new Integer[c.getResolutionNumberElements().size()];
        write(resolutions, c.getResolutionLabels().toArray(resolutionsToSave), configMetadata);
        write(platesPerRes, c.getResolutionNumberElements().toArray(platesPerResToSave), configMetadata);
        write(timeHistoryFile, c.timeHistoryFile, configMetadata);
//        write(hasImageMap, c.hasImageMap, configMetadata);
        write(hasStateHistory, c.hasStateHistory, configMetadata);
        if (c.spiceInfo != null)
        	write(spiceInfo, c.spiceInfo, configMetadata);
        if (c.stateHistoryStartDate != null)
        {
        	writeDate(stateHistoryStartDate, c.stateHistoryStartDate, configMetadata);
        	writeDate(stateHistoryEndDate, c.stateHistoryEndDate, configMetadata);
        }
        write(baseMapConfig, c.baseMapConfigName, configMetadata);

        write(density, c.density, configMetadata);
        write(rotationRate, c.rotationRate, configMetadata);
        write(bodyReferencePotential, c.bodyReferencePotential, configMetadata);
        write(useMinimumReferencePotential, c.useMinimumReferencePotential, configMetadata);

        write(customBodyCubeSize, c.customBodyCubeSize, configMetadata);
        write(hasCustomBodyCubeSize, c.hasCustomBodyCubeSize, configMetadata);
        write(hasColoringData, c.hasColoringData, configMetadata);


        writeMetadataArray(imagingInstruments, c.imagingInstruments, configMetadata);

//        Metadata[] spectrumInstrumentMetadata = new Metadata[c.spectralInstruments.size()];
//        int i=0;
//        for (BasicSpectrumInstrument inst : c.spectralInstruments)
//    	{
////        	spectrumInstrumentMetadata[i++] = InstanceGetter.defaultInstanceGetter().providesMetadataFromGenericObject(BasicSpectrumInstrument.class).provide(inst);
//        	spectrumInstrumentMetadata[i++] = inst.store();
//    	}
//        Key<Metadata[]> spectralInstrumentsMetadataKey = Key.of("spectralInstruments");
//        configMetadata.put(spectralInstrumentsMetadataKey, spectrumInstrumentMetadata);
////        writeMetadataArray(spectralInstrumentsMetadataKey, spectrumInstrumentMetadata, configMetadata);
////        writeMetadataArray(spectralInstruments, spectrumInstrumentMetadata, configMetadata);
        write(spectralInstruments, c.spectralInstruments, configMetadata);

        write(hasLidarData, c.hasLidarData, configMetadata);
        write(hasHypertreeBasedLidarSearch, c.hasHypertreeBasedLidarSearch, configMetadata);
        write(hasMapmaker, c.hasMapmaker, configMetadata);
        write(hasSpectralData, c.hasSpectralData, configMetadata);
        write(hasLineamentData, c.hasLineamentData, configMetadata);

        writeDate(imageSearchDefaultStartDate, c.imageSearchDefaultStartDate, configMetadata);
        writeDate(imageSearchDefaultEndDate, c.imageSearchDefaultEndDate, configMetadata);
        write(imageSearchFilterNames, c.imageSearchFilterNames, configMetadata);
        write(imageSearchUserDefinedCheckBoxesNames, c.imageSearchUserDefinedCheckBoxesNames, configMetadata);
        write(imageSearchDefaultMaxSpacecraftDistance, c.imageSearchDefaultMaxSpacecraftDistance, configMetadata);
        write(imageSearchDefaultMaxResolution, c.imageSearchDefaultMaxResolution, configMetadata);
        write(hasHierarchicalImageSearch, c.hasHierarchicalImageSearch, configMetadata);
        if (c.hasHierarchicalImageSearch && c.hierarchicalImageSearchSpecification != null)
            write(hierarchicalImageSearchSpecification, c.hierarchicalImageSearchSpecification.getMetadataManager().store(), configMetadata);

        if (c.hasSpectralData && c.spectralInstruments.size() > 0)
        {
        	write(hasHierarchicalSpectraSearch, c.hasHierarchicalSpectraSearch, configMetadata);
        	write(hasHypertreeBasedSpectraSearch, c.hasHypertreeBasedSpectraSearch, configMetadata);
        	write(spectraSearchDataSourceMap, c.spectraSearchDataSourceMap, configMetadata);
        	write(spectrumMetadataFile, c.spectrumMetadataFile, configMetadata);
        }

//        if (c.hasHierarchicalSpectraSearch && c.hierarchicalSpectraSearchSpecification != null)
      	if (c.hierarchicalSpectraSearchSpecification != null)
        {
//        	try
//			{
//				c.hierarchicalSpectraSearchSpecification.loadMetadata();
//			}
//        	catch (FileNotFoundException e)
//			{
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//            Metadata spectralMetadata = InstanceGetter.defaultInstanceGetter().providesMetadataFromGenericObject(SpectrumInstrumentMetadataIO.class).provide(c.hierarchicalSpectraSearchSpecification);
            configMetadata.put(hierarchicalSpectraSearchSpecification, c.hierarchicalSpectraSearchSpecification);
//            write(hierarchicalSpectraSearchSpecification, spectralMetadata, configMetadata);
        }

        //dtm
      	write(hasDTM, c.hasDTMs, configMetadata);
        if (c.dtmBrowseDataSourceMap.size() > 0 )
        	write(dtmBrowseDataSourceMap, c.dtmBrowseDataSourceMap, configMetadata);
        if (c.dtmSearchDataSourceMap.size() > 0 )
        	write(dtmSearchDataSourceMap, c.dtmSearchDataSourceMap, configMetadata);
        write(hasBigmap, c.hasBigmap, configMetadata);

        //lidar
        write(lidarBrowseIntensityEnabled, c.lidarBrowseIntensityEnabled, configMetadata);
        writeDate(lidarSearchDefaultStartDate, c.lidarSearchDefaultStartDate, configMetadata);
        writeDate(lidarSearchDefaultEndDate, c.lidarSearchDefaultEndDate, configMetadata);
        write(lidarSearchDataSourceMap, c.lidarSearchDataSourceMap, configMetadata);
        write(lidarBrowseDataSourceMap, c.lidarBrowseDataSourceMap, configMetadata);
        if (lidarBrowseWithPointsDataSourceMap != null)
        	write(lidarBrowseWithPointsDataSourceMap, c.lidarBrowseWithPointsDataSourceMap, configMetadata);

        write(lidarSearchDataSourceTimeMap, c.lidarSearchDataSourceTimeMap, configMetadata);
        write(orexSearchTimeMap, c.orexSearchTimeMap, configMetadata);

        write(lidarBrowseXYZIndices, c.lidarBrowseXYZIndices, configMetadata);
        write(lidarBrowseSpacecraftIndices, c.lidarBrowseSpacecraftIndices, configMetadata);
        write(lidarBrowseIsLidarInSphericalCoordinates, c.lidarBrowseIsLidarInSphericalCoordinates, configMetadata);
        write(lidarBrowseIsSpacecraftInSphericalCoordinates, c.lidarBrowseIsSpacecraftInSphericalCoordinates, configMetadata);
        write(lidarBrowseIsRangeExplicitInData, c.lidarBrowseIsRangeExplicitInData, configMetadata);
        write(lidarBrowseRangeIndex, c.lidarBrowseRangeIndex, configMetadata);

        write(lidarBrowseIsTimeInET, c.lidarBrowseIsTimeInET, configMetadata);
        write(lidarBrowseTimeIndex, c.lidarBrowseTimeIndex, configMetadata);
        write(lidarBrowseNoiseIndex, c.lidarBrowseNoiseIndex, configMetadata);
        write(lidarBrowseOutgoingIntensityIndex, c.lidarBrowseOutgoingIntensityIndex, configMetadata);
        write(lidarBrowseReceivedIntensityIndex, c.lidarBrowseReceivedIntensityIndex, configMetadata);
        write(lidarBrowseFileListResourcePath, c.lidarBrowseFileListResourcePath, configMetadata);
        write(lidarBrowseNumberHeaderLines, c.lidarBrowseNumberHeaderLines, configMetadata);
        write(lidarBrowseIsInMeters, c.lidarBrowseIsInMeters, configMetadata);
        write(lidarBrowseIsBinary, c.lidarBrowseIsBinary, configMetadata);
        write(lidarBrowseBinaryRecordSize, c.lidarBrowseBinaryRecordSize, configMetadata);
        write(lidarOffsetScale, c.lidarOffsetScale, configMetadata);
        writeEnum(lidarInstrumentName, c.lidarInstrumentName, configMetadata);

        int i;
        if (c.defaultForMissions != null)
        {
	        String[] defaultStrings = new String[c.defaultForMissions.length];
	        i=0;
	        for (Mission mission : c.defaultForMissions)
	        {
	        	defaultStrings[i++] = mission.getHashedName();
	        }
	        write(defaultForMissions, defaultStrings, configMetadata);
        }

        String[] presentStrings = new String[c.presentInMissions.length];
        i=0;
        for (Mission mission : c.presentInMissions)
        {
        	presentStrings[i++] = mission.getHashedName();
        }
        write(presentInMissions, presentStrings, configMetadata);

        writeMetadataArray(runInfos, c.databaseRunInfos, configMetadata);

        write(systemBodies, c.hasSystemBodies, configMetadata);

        List<SmallBodyViewConfig> systemConfigs = c.systemConfigs;
        List<String> systemConfigUniqueNames = systemConfigs.stream().map(cfg -> cfg.getBody().toString() + ","  + cfg.author.toString() + "," + cfg.version).collect(Collectors.toList());
        write(systemBodyConfigs, systemConfigUniqueNames, configMetadata);

        Map<String, Metadata> featureMap = Maps.newHashMap();
        for (Class configClass : c.getFeatureConfigs().keySet())
        {
        	List<IFeatureConfig> featureConfig = c.getFeatureConfigs().get(configClass);
        	Metadata metadata = FeatureConfigIOFactory.getMetadataForFeatureConfig(configClass.getSimpleName(), featureConfig.get(0));
        	featureMap.put(configClass.getSimpleName(), metadata);
        }
        write(featureConfigs, featureMap, configMetadata);
        return configMetadata;
    }

    @Override
    public Metadata store()
    {
        List<IBodyViewConfig> builtInConfigs = configs;
        if (builtInConfigs.size() == 1)
        {
            SettableMetadata configMetadata = storeConfig(builtInConfigs.get(0));
            return configMetadata;
        }
        else
        {
            SettableMetadata result = SettableMetadata.of(Version.of(BasicConfigInfo.getConfigInfoVersion()));
            for (IBodyViewConfig config : builtInConfigs)
            {
                SettableMetadata configMetadata = storeConfig(config);
                Key<SettableMetadata> metadata = Key.of(config.getUniqueName());
                result.put(metadata, configMetadata);
            }
            return result;
        }
    }

    private <T> void write(Key<T> key, T value, SettableMetadata configMetadata)
    {
        if (value != null)
        {
            configMetadata.put(key, value);
        }
    }

    private <T> void writeEnum(Key<String> key, Enum value, SettableMetadata configMetadata)
    {
        if (value != null)
        {
            configMetadata.put(key, value.name());
        }
    }


    private <T> void writeDate(Key<Long> key, Date value, SettableMetadata configMetadata)
    {
        if (value != null)
        {
            configMetadata.put(key, value.getTime());
        }
    }

    private <T> void writeMetadataArray(Key<Metadata[]> key, MetadataManager[] values, SettableMetadata configMetadata)
    {
        if (values != null)
        {
            Metadata[] data = new Metadata[values.length];
            int i=0;
            for (MetadataManager val : values) data[i++] = val.store();
            configMetadata.put(key, data);
        }
    }

    private Metadata[] readMetadataArray(Key<Metadata[]> key, Metadata configMetadata)
    {
        Metadata[] values = configMetadata.get(key);
        if (values != null)
        {
            return values;
        }
        return null;
    }

    private <T> T read(Key<T> key, Metadata configMetadata)
    {
        if (configMetadata.hasKey(key) == false) return null;
        T value = configMetadata.get(key);
        if (value != null)
            return value;
        return null;
    }

    @Override
    public void retrieve(Metadata configMetadata)
    {
    	retrieve(configMetadata, false);
    }


    public void retrieve(Metadata configMetadata, boolean partOfSystem)
    {
//    	System.out.println("SmallBodyViewConfigMetadataIO: retrieve: metadata " + configMetadata);
        SmallBodyViewConfig c = (SmallBodyViewConfig)configs.get(0);
        c.body = ShapeModelBody.valueOf(read(body, configMetadata));
        c.type = BodyType.valueOf(read(type, configMetadata));
        c.version = read(version, configMetadata);
        c.population = ShapeModelPopulation.valueOf(read(population, configMetadata));
        String systemString = read(system, configMetadata);
        c.system = systemString != null ? ShapeModelBody.valueOf(systemString) : null;
        c.dataUsed =ShapeModelDataUsed.valueOf(read(dataUsed, configMetadata));
        c.author = ShapeModelType.provide(read(author, configMetadata));
        c.modelLabel = read(modelLabel, configMetadata);
        c.bodyLowestResModelName = read(bodyLowestResModelName, configMetadata);
        c.rootDirOnServer = read(rootDirOnServer, configMetadata);
        c.setShapeModelFileExtension(read(shapeModelFileExtension, configMetadata));
        c.setShapeModelFileBaseName(read(shapeModelFileBaseName, configMetadata));
        String[] resolutionsToAdd = read(resolutions, configMetadata);
        c.setShapeModelFileNames(read(shapeModelFileNamesKey, configMetadata));
        Integer[] platesPerResToAdd = read(platesPerRes, configMetadata);
        if (resolutionsToAdd != null && platesPerResToAdd != null)
        	c.setResolution(ImmutableList.copyOf(resolutionsToAdd), ImmutableList.copyOf(platesPerResToAdd));
        c.timeHistoryFile = read(timeHistoryFile, configMetadata);
//        c.hasImageMap = read(hasImageMap, configMetadata);
        c.hasStateHistory = read(hasStateHistory, configMetadata);
        if (configMetadata.hasKey(spiceInfo))
        	c.spiceInfo = read(spiceInfo, configMetadata);
        if (configMetadata.hasKey(stateHistoryStartDate))
        {
        	//This is a bandaid to handle the fact that the metadata for Dates
        	//is written in Eastern time and not UTC
        	DateTime dt = new DateTime(read(stateHistoryStartDate, configMetadata));
        	DateTimeZone dtZone = DateTimeZone.forID("America/New_York");
        	DateTime dtus = dt.withZone(dtZone);
        	Date startDate =  dtus.toLocalDateTime().toDateTime().toDate();

        	DateTime dt2 = new DateTime(read(stateHistoryEndDate, configMetadata));
        	DateTimeZone dtZone2 = DateTimeZone.forID("America/New_York");
        	DateTime dtus2 = dt2.withZone(dtZone2);
        	Date endDate =  dtus2.toLocalDateTime().toDateTime().toDate();

        	c.stateHistoryStartDate = startDate;
        	c.stateHistoryEndDate = endDate;
        }
        c.baseMapConfigName = read(baseMapConfig, configMetadata);

        c.density = read(density, configMetadata);
        c.rotationRate = read(rotationRate, configMetadata);
        c.bodyReferencePotential = read(bodyReferencePotential, configMetadata);
        c.useMinimumReferencePotential = read(useMinimumReferencePotential, configMetadata);

        c.customBodyCubeSize = read(customBodyCubeSize, configMetadata);
        c.hasCustomBodyCubeSize = read(hasCustomBodyCubeSize, configMetadata);
        c.hasColoringData = read(hasColoringData, configMetadata);

        Metadata[] imagingMetadata = readMetadataArray(imagingInstruments, configMetadata);
        c.imagingInstruments = new ImagingInstrument[imagingMetadata.length];
        int i=0;
        for (Metadata data : imagingMetadata)
        {
            ImagingInstrument inst = new ImagingInstrument();
            inst.retrieve(data);
            c.imagingInstruments[i++] = inst;
        }

        if (configMetadata.get(hasSpectralData) == true)
        {
        	try
        	{
        		c.spectralInstruments = configMetadata.get(spectralInstruments);
        	}
        	catch (ClassCastException cce)	//fall back to the old method
        	{
        		final Key<Metadata[]> spectralInstrumentsOldFormat = Key.of("spectralInstruments");
        		Metadata[] spectralMetadata = readMetadataArray(spectralInstrumentsOldFormat, configMetadata);
                i=0;
                for (Metadata data : spectralMetadata)
                {
                    String instrumentName = (String)data.get(Key.of("displayName"));
                    BasicSpectrumInstrument inst = SpectrumInstrumentFactory.getInstrumentForName(instrumentName);
                    inst.retrieveOldFormat(data);
                    c.spectralInstruments.add(inst);
                }
        	}
        }


        c.hasLidarData = read(hasLidarData, configMetadata);
        c.hasHypertreeBasedLidarSearch = read(hasHypertreeBasedLidarSearch, configMetadata);
        c.hasMapmaker = read(hasMapmaker, configMetadata);
        c.hasSpectralData = read(hasSpectralData, configMetadata);
        c.hasLineamentData = read(hasLineamentData, configMetadata);

        if (c.imagingInstruments.length > 0)
        {
	        Long imageSearchDefaultStart = read(imageSearchDefaultStartDate, configMetadata);
	        Long imageSearchDefaultEnd = read(imageSearchDefaultEndDate, configMetadata);

        	//This is a bandaid to handle the fact that the metadata for Dates
        	//is written in Eastern time and not UTC
        	DateTime dt = new DateTime(imageSearchDefaultStart);
        	DateTimeZone dtZone = DateTimeZone.forID("America/New_York");
        	DateTime dtus = dt.withZone(dtZone);
        	Date startDate =  dtus.toLocalDateTime().toDateTime().toDate();

        	DateTime dt2 = new DateTime(imageSearchDefaultEnd);
        	DateTimeZone dtZone2 = DateTimeZone.forID("America/New_York");
        	DateTime dtus2 = dt2.withZone(dtZone2);
        	Date endDate =  dtus2.toLocalDateTime().toDateTime().toDate();

        	c.imageSearchDefaultStartDate = startDate;
        	c.imageSearchDefaultEndDate  = endDate;

	        c.imageSearchFilterNames = read(imageSearchFilterNames, configMetadata);
	        c.imageSearchUserDefinedCheckBoxesNames = read(imageSearchUserDefinedCheckBoxesNames, configMetadata);
	        c.imageSearchDefaultMaxSpacecraftDistance = read(imageSearchDefaultMaxSpacecraftDistance, configMetadata);
	        c.imageSearchDefaultMaxResolution = read(imageSearchDefaultMaxResolution, configMetadata);
	        if (configMetadata.hasKey(hasHierarchicalImageSearch))
	        {
	        	c.hasHierarchicalImageSearch = read(hasHierarchicalImageSearch, configMetadata);
	        	if (c.hasHierarchicalImageSearch)
	        	{
	        	    Metadata md = read(hierarchicalImageSearchSpecification, configMetadata);
	        	    c.hierarchicalImageSearchSpecification = new HierarchicalSearchSpecification();
	        	    c.hierarchicalImageSearchSpecification.getMetadataManager().retrieve(md);
	        	}
	        }

//        	c.hierarchicalImageSearchSpecification.getMetadataManager().retrieve(read(hierarchicalImageSearchSpecification, configMetadata));


        }

        if (c.hasSpectralData && c.spectralInstruments.size() > 0)
        {
        	if (configMetadata.hasKey(hasHierarchicalSpectraSearch))
        		c.hasHierarchicalSpectraSearch = read(hasHierarchicalSpectraSearch, configMetadata);
        	if (configMetadata.hasKey(hasHypertreeBasedSpectraSearch))
        		c.hasHypertreeBasedSpectraSearch = read(hasHypertreeBasedSpectraSearch, configMetadata);
	        c.spectraSearchDataSourceMap = read(spectraSearchDataSourceMap, configMetadata);
	        c.spectrumMetadataFile = read(spectrumMetadataFile, configMetadata);

	        if (configMetadata.hasKey(hierarchicalSpectraSearchSpecification))
	        {
	        	try
	        	{
	        		c.hierarchicalSpectraSearchSpecification = configMetadata.get(hierarchicalSpectraSearchSpecification);
	        	}
	        	catch (ClassCastException cce)	//fall back to the old method
	        	{
	        	    Key<Metadata> hierarchicalSpectraSearchSpecificationOldFormat = Key.of("hierarchicalSpectraSearchSpecification");

	        		c.hierarchicalSpectraSearchSpecification = new SpectrumInstrumentMetadataIO("");
	        		c.hierarchicalSpectraSearchSpecification.retrieveOldFormat(configMetadata.get(hierarchicalSpectraSearchSpecificationOldFormat));
	        		c.hierarchicalSpectraSearchSpecification.getSelectedDatasets();
	        	}
	        }
        }

        if (configMetadata.hasKey(dtmSearchDataSourceMap))
        	c.dtmSearchDataSourceMap = read(dtmSearchDataSourceMap, configMetadata);
        if (configMetadata.hasKey(dtmBrowseDataSourceMap))
        	c.dtmBrowseDataSourceMap = read(dtmBrowseDataSourceMap, configMetadata);
        c.hasBigmap = read(hasBigmap, configMetadata);
        if (configMetadata.hasKey(hasDTM))
        	c.hasDTMs = read(hasDTM, configMetadata);

        if (c.hasLidarData)
        {
        	c.lidarBrowseIntensityEnabled = read(lidarBrowseIntensityEnabled, configMetadata);
	        Long lidarSearchDefaultStart = read(lidarSearchDefaultStartDate, configMetadata);
	        if (lidarSearchDefaultStart == null) lidarSearchDefaultStart = 0L;
	        c.lidarSearchDefaultStartDate = new Date(lidarSearchDefaultStart);
	        Long lidarSearchDefaultEnd = read(lidarSearchDefaultEndDate, configMetadata);
	        if (lidarSearchDefaultEnd == null) lidarSearchDefaultEnd = 0L;
	        c.lidarSearchDefaultEndDate = new Date(lidarSearchDefaultEnd);
	        c.lidarSearchDataSourceMap = read(lidarSearchDataSourceMap, configMetadata);
	        c.lidarBrowseDataSourceMap = read(lidarBrowseDataSourceMap, configMetadata);
	        if (configMetadata.hasKey(lidarBrowseWithPointsDataSourceMap))
	        	c.lidarBrowseWithPointsDataSourceMap = read(lidarBrowseWithPointsDataSourceMap, configMetadata);

	        c.lidarSearchDataSourceTimeMap = read(lidarSearchDataSourceTimeMap, configMetadata);
	        c.orexSearchTimeMap = read(orexSearchTimeMap, configMetadata);

	        c.lidarBrowseXYZIndices = read(lidarBrowseXYZIndices, configMetadata);
	        c.lidarBrowseSpacecraftIndices = read(lidarBrowseSpacecraftIndices, configMetadata);
	        c.lidarBrowseIsLidarInSphericalCoordinates = read(lidarBrowseIsLidarInSphericalCoordinates, configMetadata);
	        c.lidarBrowseIsSpacecraftInSphericalCoordinates = read(lidarBrowseIsSpacecraftInSphericalCoordinates, configMetadata);
	        c.lidarBrowseIsRangeExplicitInData = read(lidarBrowseIsRangeExplicitInData, configMetadata);
	        c.lidarBrowseRangeIndex = read(lidarBrowseRangeIndex, configMetadata);

	        c.lidarBrowseIsTimeInET = read(lidarBrowseIsTimeInET, configMetadata);
	        c.lidarBrowseTimeIndex = read(lidarBrowseTimeIndex, configMetadata);
	        c.lidarBrowseNoiseIndex = read(lidarBrowseNoiseIndex, configMetadata);
	        c.lidarBrowseOutgoingIntensityIndex = read(lidarBrowseOutgoingIntensityIndex, configMetadata);
	        c.lidarBrowseReceivedIntensityIndex = read(lidarBrowseReceivedIntensityIndex, configMetadata);
	        c.lidarBrowseFileListResourcePath = read(lidarBrowseFileListResourcePath, configMetadata);
	        c.lidarBrowseNumberHeaderLines = read(lidarBrowseNumberHeaderLines, configMetadata);
	        c.lidarBrowseIsInMeters = read(lidarBrowseIsInMeters, configMetadata);
	        c.lidarBrowseIsBinary = read(lidarBrowseIsBinary, configMetadata);
	        c.lidarBrowseBinaryRecordSize = read(lidarBrowseBinaryRecordSize, configMetadata);
	        c.lidarOffsetScale = read(lidarOffsetScale, configMetadata);
	        c.lidarInstrumentName = Instrument.valueOf(""+read(lidarInstrumentName, configMetadata));

        }

        String[] presentInMissionStrings = read(presentInMissions, configMetadata);
        if (presentInMissionStrings == null)
        {
        	presentInMissionStrings = new String[Mission.values().length];
        	int ii=0;
        	for (Mission mission : Mission.values())
        		presentInMissionStrings[ii++] = mission.getHashedName();
        }
        c.presentInMissions = new Mission[presentInMissionStrings.length];
        int m=0;
        for (String defStr : presentInMissionStrings)
        {
        	c.presentInMissions[m++] = Mission.getMissionForName(defStr);
        }

        if (configMetadata.hasKey(defaultForMissions))
        {
	        String[] defaultsForMissionStrings = read(defaultForMissions, configMetadata);
	        c.defaultForMissions = new Mission[defaultsForMissionStrings.length];
	        int k=0;
	        for (String defStr : defaultsForMissionStrings)
	        {
	        	c.defaultForMissions[k++] = Mission.getMissionForName(defStr);
	        }
//	        if (missionsToAdd != null)
//	        {
//	            for (String mission : missionsToAdd)
//	            {
//	                SbmtMultiMissionTool.Mission msn = SbmtMultiMissionTool.Mission.getMissionForName(mission);
//	                c.missions.add(msn);
//	                if (SbmtMultiMissionTool.getMission() == msn)
//	                {
//	                    ViewConfig.setFirstTimeDefaultModelName(c.getUniqueName());
//	                }
//	            }
//	        }
        }

        if (configMetadata.hasKey(runInfos))
        {
	        Metadata[] runInfoMetadata = readMetadataArray(runInfos, configMetadata);
	        c.databaseRunInfos = new DBRunInfo[runInfoMetadata.length];
	        i=0;
	        for (Metadata data : runInfoMetadata)
	        {
	        	DBRunInfo info = new DBRunInfo();
	        	info.retrieve(data);
	        	c.databaseRunInfos[i++] = info;
	        }
        }

        if (c.author == ShapeModelType.CUSTOM)
        {
        	c.modelLabel = metadataID;
        }

        if (configMetadata.hasKey(systemBodies)) c.hasSystemBodies = read(systemBodies, configMetadata);
        if (configMetadata.hasKey(systemBodyConfigs) && SmallBodyViewConfig.getConfigIdentifiers().size() != 0 && partOfSystem == false)
        {
        	List<String> systemBodyConfigStrings = read(systemBodyConfigs, configMetadata);
        	c.systemConfigs = systemBodyConfigStrings
        		.stream()
        		.map( x -> {
        		String[] splits = x.split(",");
        		if ((splits.length == 2) || ((splits.length == 3) && (splits[2].equals("null"))))
        		{
        			SmallBodyViewConfig config = SmallBodyViewConfig.getSmallBodyConfig(ShapeModelBody.valueFor(splits[0]), ShapeModelType.provide(splits[1]), true);
        			return config;
        		}
        		else
        		{
        			SmallBodyViewConfig config = SmallBodyViewConfig.getSmallBodyConfig(ShapeModelBody.valueFor(splits[0]), ShapeModelType.provide(splits[1]), splits[2]);
        			return config;
        		}
        	}).toList();
        }


        Map<String, Metadata> featureConfigMetadata = read(featureConfigs, configMetadata);
        for (String configClassName : featureConfigMetadata.keySet())
        {
        	IFeatureConfig config = FeatureConfigIOFactory.getFeatureConfigForMetadata(configClassName, featureConfigMetadata.get(configClassName));
        	c.addFeatureConfig(config.getClass(), config);
        }
    }

    public List<IBodyViewConfig> getConfigs()
    {
        return configs;
    }

    final Key<String> body = Key.of("body");
    final Key<String> type = Key.of("type");
    final Key<String> version = Key.of("version");
    final Key<String> population = Key.of("population");
    final Key<String> system = Key.of("system");
    final Key<String> dataUsed = Key.of("dataUsed");
    final Key<String> author = Key.of("author");
    final Key<String> modelLabel = Key.of("modelLabel");
    final Key<String> rootDirOnServer = Key.of("rootDirOnServer");
    final Key<String> bodyLowestResModelName = Key.of("bodyLowestResModelName");
    final Key<String> shapeModelFileExtension = Key.of("shapeModelFileExtension");
    final Key<String> shapeModelFileBaseName = Key.of("shapeModelFileBaseName");
    final Key<String[]> shapeModelFileNamesKey = Key.of("shapeModelFileNames");
    final Key<String[]> resolutions = Key.of("resolutions");
    final Key<Integer[]> platesPerRes = Key.of("platesPerRes");
    final Key<String> timeHistoryFile = Key.of("timeHistoryFile");
//    final Key<Boolean> hasImageMap = Key.of("hasImageMap");
    final Key<Boolean> hasStateHistory = Key.of("hasStateHistory");
    final Key<SpiceInfo> spiceInfo = Key.of("spiceInfo");
    final Key<Long> stateHistoryStartDate = Key.of("stateHistoryStartDate");
    final Key<Long> stateHistoryEndDate = Key.of("stateHistoryEndDate");
    final Key<String[]> presentInMissions = Key.of("presentInMissions");
    final Key<String[]> defaultForMissions = Key.of("defaultForMissions");
    final Key<String> baseMapConfig = Key.of("baseMapConfig");

    final Key<Double> density = Key.of("density");
    final Key<Double> rotationRate = Key.of("rotationRate");
    final Key<Double> bodyReferencePotential = Key.of("bodyReferencePotential");
    final Key<Boolean> useMinimumReferencePotential = Key.of("useMinimumReferencePotential");

    final Key<Boolean> hasCustomBodyCubeSize = Key.of("hasCustomBodyCubeSize");
    final Key<Double> customBodyCubeSize = Key.of("customBodyCubeSize");

    final Key<Boolean> hasColoringData = Key.of("hasColoringData");

    //capture imaging instruments here
    final Key<Metadata[]> imagingInstruments = Key.of("imagingInstruments");


    //capture spectral instruments here
    final Key<List<BasicSpectrumInstrument>> spectralInstruments = Key.of("spectralInstruments");

    //DTM
    final Key<Map> dtmSearchDataSourceMap = Key.of("dtmSearchDataSourceMap");
    final Key<Map> dtmBrowseDataSourceMap = Key.of("dtmBrowseDataSourceMap");
    final Key<Boolean> hasBigmap = Key.of("hasBigmap");
    final Key<Boolean> hasDTM = Key.of("hasDTM");


    final Key<Boolean> hasLidarData = Key.of("hasLidarData");
    final Key<Boolean> hasHypertreeBasedLidarSearch = Key.of("hasHypertreeBasedLidarSearch");
    final Key<Boolean> hasMapmaker = Key.of("hasMapmaker");
    final Key<Boolean> hasSpectralData = Key.of("hasSpectralData");
    final Key<Boolean> hasLineamentData = Key.of("hasLineamentData");

    final Key<Long> imageSearchDefaultStartDate = Key.of("imageSearchDefaultStartDate");
    final Key<Long> imageSearchDefaultEndDate = Key.of("imageSearchDefaultEndDate");
    final Key<String[]> imageSearchFilterNames = Key.of("imageSearchFilterNames");
    final Key<String[]> imageSearchUserDefinedCheckBoxesNames = Key.of("imageSearchUserDefinedCheckBoxesNames");
    final Key<Double> imageSearchDefaultMaxSpacecraftDistance = Key.of("imageSearchDefaultMaxSpacecraftDistance");
    final Key<Double> imageSearchDefaultMaxResolution = Key.of("imageSearchDefaultMaxResolution");
    final Key<Boolean> hasHierarchicalImageSearch = Key.of("hasHierarchicalImageSearch");
    final Key<Metadata> hierarchicalImageSearchSpecification = Key.of("hierarchicalImageSearchSpecification");


    final Key<Boolean> hasHierarchicalSpectraSearch = Key.of("hasHierarchicalSpectraSearch");
    final Key<Boolean> hasHypertreeBasedSpectraSearch = Key.of("hasHypertreeSpectraSearch");
    final Key<Map> spectraSearchDataSourceMap = Key.of("spectraSearchDataSourceMap");
    final Key<String> spectrumMetadataFile = Key.of("spectrumMetadataFile");
    final Key<SpectrumInstrumentMetadataIO> hierarchicalSpectraSearchSpecification = Key.of("hierarchicalSpectraSearchSpecification");

    final Key<Boolean> lidarBrowseIntensityEnabled = Key.of("lidarBrowseIntensityEnabled");
    final Key<Long> lidarSearchDefaultStartDate = Key.of("lidarSearchDefaultStartDate");
    final Key<Long> lidarSearchDefaultEndDate = Key.of("lidarSearchDefaultEndDate");

    final Key<Map> lidarSearchDataSourceMap = Key.of("lidarSearchDataSourceMap");
    final Key<Map> lidarBrowseDataSourceMap = Key.of("lidarBrowseDataSourceMap");
    final Key<Map> lidarBrowseWithPointsDataSourceMap = Key.of("lidarBrowseWithPointsDataSourceMap");

    final Key<Map> lidarSearchDataSourceTimeMap = Key.of("lidarSearchDataSourceTimeMap");
    final Key<Map> orexSearchTimeMap = Key.of("orexSearchTimeMap");

    final Key<int[]> lidarBrowseXYZIndices = Key.of("lidarBrowseXYZIndices");
    final Key<int[]> lidarBrowseSpacecraftIndices = Key.of("lidarBrowseSpacecraftIndices");

    final Key<Boolean> lidarBrowseIsSpacecraftInSphericalCoordinates = Key.of("lidarBrowseIsSpacecraftInSphericalCoordinates");
    final Key<Boolean> lidarBrowseIsLidarInSphericalCoordinates = Key.of("lidarBrowseIsLidarInSphericalCoordinates");
    final Key<Boolean> lidarBrowseIsRangeExplicitInData = Key.of("lidarBrowseIsRangeExplicitInData");
    final Key<Boolean> lidarBrowseIsTimeInET = Key.of("lidarBrowseIsTimeInET");
    final Key<Integer> lidarBrowseRangeIndex = Key.of("lidarBrowseRangeIndex");

    final Key<Integer> lidarBrowseTimeIndex = Key.of("lidarBrowseTimeIndex");
    final Key<Integer> lidarBrowseNoiseIndex = Key.of("lidarBrowseNoiseIndex");
    final Key<Integer> lidarBrowseOutgoingIntensityIndex = Key.of("lidarBrowseOutgoingIntensityIndex");
    final Key<Integer> lidarBrowseReceivedIntensityIndex = Key.of("lidarBrowseReceivedIntensityIndex");
    final Key<String> lidarBrowseFileListResourcePath = Key.of("lidarBrowseFileListResourcePath");
    final Key<Integer> lidarBrowseNumberHeaderLines = Key.of("lidarBrowseNumberHeaderLines");
    final Key<Boolean> lidarBrowseIsInMeters = Key.of("lidarBrowseIsInMeters");
    final Key<Double> lidarOffsetScale = Key.of("lidarOffsetScale");
    final Key<Boolean> lidarBrowseIsBinary = Key.of("lidarBrowseIsBinary");
    final Key<Integer> lidarBrowseBinaryRecordSize = Key.of("lidarBrowseBinaryRecordSize");
    final Key<String> lidarInstrumentName = Key.of("lidarInstrumentName");

    final Key<Metadata[]> runInfos = Key.of("runInfos");

    final Key<Map> featureConfigs = Key.of("featureConfigs");

    final Key<Boolean> systemBodies = Key.of("systemBodies");
    final Key<List<String>> systemBodyConfigs = Key.of("systemBodyConfigs");

}
