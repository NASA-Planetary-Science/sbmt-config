package edu.jhuapl.sbmt.config;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.stream.Collectors;

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
import edu.jhuapl.sbmt.core.io.DBRunInfo;

import edu.jhuapl.ses.jsqrl.api.Key;
import edu.jhuapl.ses.jsqrl.api.Metadata;
import edu.jhuapl.ses.jsqrl.api.MetadataManager;
import edu.jhuapl.ses.jsqrl.api.Version;
import edu.jhuapl.ses.jsqrl.impl.FixedMetadata;
import edu.jhuapl.ses.jsqrl.impl.SettableMetadata;
import edu.jhuapl.ses.jsqrl.impl.gson.Serializers;

public class SmallBodyViewConfigMetadataIO implements MetadataManager
{

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
        Mission.configureMission();
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
                io2.setMetadataID(config.getUniqueName());
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

    public void write(String metadataID, File file, Metadata metadata) throws IOException
    {
        Serializers.serialize(metadataID, metadata, file);
    }

    public void read(File file, String metadataID, SmallBodyViewConfig config) throws IOException
    {
    	String[] modelFileNames = config.getShapeModelFileNames();
        FixedMetadata metadata = Serializers.deserialize(file, metadataID);
        this.setMetadataID(metadataID);
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
        write(hasStateHistory, c.hasStateHistory, configMetadata);
        write(baseMapConfig, c.getBaseMapConfigName(), configMetadata);

        write(density, c.density, configMetadata);
        write(rotationRate, c.rotationRate, configMetadata);
        write(bodyReferencePotential, c.bodyReferencePotential, configMetadata);
        write(useMinimumReferencePotential, c.useMinimumReferencePotential, configMetadata);

        write(customBodyCubeSize, c.customBodyCubeSize, configMetadata);
        write(hasCustomBodyCubeSize, c.hasCustomBodyCubeSize, configMetadata);
        write(hasColoringData, c.hasColoringData, configMetadata);

        write(hasMapmaker, c.hasMapmaker, configMetadata);
        write(hasLineamentData, c.hasLineamentData, configMetadata);



        //dtm 
      	write(hasDTM, c.hasDTMs, configMetadata);

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
        for (Class<?> configClass : c.getFeatureConfigs().keySet())
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

    private <T> void writeEnum(Key<String> key, Enum<?> value, SettableMetadata configMetadata)
    {
        if (value != null)
        {
            configMetadata.put(key, value.name());
        }
    }


    @SuppressWarnings("unused")
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
        c.hasStateHistory = read(hasStateHistory, configMetadata);

        c.setBaseMapConfigName(read(baseMapConfig, configMetadata));

        c.density = read(density, configMetadata);
        c.rotationRate = read(rotationRate, configMetadata);
        c.bodyReferencePotential = read(bodyReferencePotential, configMetadata);
        c.useMinimumReferencePotential = read(useMinimumReferencePotential, configMetadata);

        c.customBodyCubeSize = read(customBodyCubeSize, configMetadata);
        c.hasCustomBodyCubeSize = read(hasCustomBodyCubeSize, configMetadata);
        c.hasColoringData = read(hasColoringData, configMetadata);
        c.hasMapmaker = read(hasMapmaker, configMetadata);
        c.hasLineamentData = read(hasLineamentData, configMetadata);

        if (configMetadata.hasKey(hasDTM))
        	c.hasDTMs = read(hasDTM, configMetadata);


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
        }

        if (configMetadata.hasKey(runInfos))
        {
	        Metadata[] runInfoMetadata = readMetadataArray(runInfos, configMetadata);
	        c.databaseRunInfos = new DBRunInfo[runInfoMetadata.length];
	        int i=0;
	        for (Metadata data : runInfoMetadata)
	        {
	        	DBRunInfo info = new DBRunInfo();
	        	info.retrieve(data);
	        	c.databaseRunInfos[i++] = info;
	        }
        }

        if (c.author == ShapeModelType.CUSTOM)
        {
        	c.modelLabel = getMetadataID();
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


        @SuppressWarnings("unchecked")
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

    public String getMetadataID()
	{
		return metadataID;
	}

	public void setMetadataID(String metadataID)
	{
		this.metadataID = metadataID;
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
    final Key<Boolean> hasStateHistory = Key.of("hasStateHistory");
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

    //DTM
    final Key<Boolean> hasDTM = Key.of("hasDTM");

    final Key<Boolean> hasMapmaker = Key.of("hasMapmaker");
    final Key<Boolean> hasLineamentData = Key.of("hasLineamentData");

    final Key<Metadata[]> runInfos = Key.of("runInfos");

    @SuppressWarnings("rawtypes")
	final Key<Map> featureConfigs = Key.of("featureConfigs");

    final Key<Boolean> systemBodies = Key.of("systemBodies");
    final Key<List<String>> systemBodyConfigs = Key.of("systemBodyConfigs");

}
