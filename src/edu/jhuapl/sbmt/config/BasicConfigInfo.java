package edu.jhuapl.sbmt.config;

import java.util.Arrays;

import edu.jhuapl.saavtk.model.ShapeModelBody;
import edu.jhuapl.saavtk.model.ShapeModelType;
import edu.jhuapl.sbmt.common.client.BodyViewConfig;
import edu.jhuapl.sbmt.common.client.Mission;
import edu.jhuapl.sbmt.common.client.SmallBodyViewConfig;

import crucible.crust.metadata.api.Key;
import crucible.crust.metadata.api.Metadata;
import crucible.crust.metadata.api.MetadataManager;
import crucible.crust.metadata.api.Version;
import crucible.crust.metadata.impl.SettableMetadata;

public class BasicConfigInfo implements MetadataManager
{

    // ******************************************************
    // HEY! HEADS-UP! IF THIS CHANGES, CHANGE:
    //      pipeline->rawdata->generic->runDataProcessing.sh
    // TO MATCH!!!!!!!!!!!!!!!!!!!
    // *******************************************************************************
    private static final String configInfoVersion = "9.4"; // READ THE COMMENT ABOVE.
    // *******************************************************************************

    public static String getConfigInfoVersion()
    {
        return configInfoVersion;
    }

    public static String getConfigPathPrefix(boolean publishedDataOnly)
    {
        return (publishedDataOnly ? "published/" : "proprietary/") + "allBodies-" + configInfoVersion /*+ "-2471"*/;
    }

    private static final Mission[] EmptyMissionArray = new Mission[0];

    ShapeModelPopulation population;
    ShapeModelBody system;
	String shapeModelName;
	String uniqueName;
	ShapeModelType author;
	BodyType type;
	ShapeModelBody body;
	ShapeModelDataUsed dataUsed;
	private String configURL;
	String version;
	String modelLabel;
	Mission[] presentInVersion = EmptyMissionArray;
	Mission[] defaultFor = EmptyMissionArray;
	boolean enabled;

	public BasicConfigInfo() {}

	public BasicConfigInfo(BodyViewConfig config, boolean publishedDataOnly)
	{
		this.type = config.type;
        this.population = config.population;
        this.system = config.system;
		this.body = config.body;
		this.dataUsed = config.dataUsed;
		this.author = config.author;
		this.shapeModelName = config.getShapeModelName();
		this.uniqueName = config.getUniqueName();
		this.version = config.version;
		this.modelLabel = config.modelLabel;
		this.presentInVersion = config.presentInMissions;
		this.defaultFor = config.defaultForMissions;
		if (defaultFor == null) defaultFor = new Mission[] {};
		if (author != ShapeModelType.CUSTOM)
		{
//			System.out.println("BasicConfigInfo: unique name " + uniqueName);
			for (Mission presentMission : presentInVersion)
			{
				//allow the body if the "present in Mission" value equals the tool's preset mission value OR if the tool's present mission value is the apl internal nightly
				if ((presentMission == edu.jhuapl.sbmt.client2.SbmtMultiMissionTool.getMission()) || (edu.jhuapl.sbmt.client2.SbmtMultiMissionTool.getMission() == Mission.APL_INTERNAL_NIGHTLY))
				{
					enabled = true;
					break;
				}
				else
					enabled = false;
			}
            if (SmallBodyViewConfig.getDefaultModelName() == null)
            {
                for (Mission defaultMission : defaultFor)
                {
                    if (defaultMission == edu.jhuapl.sbmt.client2.SbmtMultiMissionTool.getMission())
                    {
                        SmallBodyViewConfig.setDefaultModelName(uniqueName);
                        break;
                    }
                }
            }

            // Note: config.version is for when a model intrinsically has a
            // version as part of its name. It has nothing to do with the
            // metadata version. For most models config.version is null, so
            // modelVersion will add nothing.
            String modelVersion = config.version != null ? config.version.replaceAll(" ", "_") : "";

            this.configURL = "/" + getConfigPathPrefix(publishedDataOnly) + ((SmallBodyViewConfig) config).rootDirOnServer + //
                    "/" + config.author + "_" + //
                    config.body.toString().replaceAll(" ", "_") + modelVersion + //
                    "_v" + getConfigInfoVersion() + ".json";
		}
	}

    Key<String> populationKey = Key.of("population");
    Key<String> systemKey = Key.of("system");
	Key<String> typeKey = Key.of("type");
	Key<String> bodyKey = Key.of("body");
	Key<String> dataUsedKey = Key.of("dataUsed");
	Key<String> authorKey = Key.of("author");
	Key<String> shapeModelNameKey = Key.of("shapeModelName");
	Key<String> uniqueNameKey = Key.of("uniqueName");
	Key<String> configURLKey = Key.of("configURL");
	Key<String> versionKey = Key.of("version");
	Key<String> modelLabelKey = Key.of("modelLabel");
	Key<String[]> presentInVersionKey = Key.of("presentInVersion");
	Key<String[]> defaultForKey = Key.of("defaultFor");

	@Override
	public Metadata store()
	{
		SettableMetadata configMetadata = SettableMetadata.of(Version.of(1, 1));
        configMetadata.put(populationKey, population.toString());
        configMetadata.put(systemKey, system != null ? system.toString() : null);
        configMetadata.put(typeKey, type.toString());
        configMetadata.put(bodyKey, body.toString());
        configMetadata.put(dataUsedKey, dataUsed.toString());
        configMetadata.put(authorKey, author.toString());
        configMetadata.put(shapeModelNameKey, shapeModelName);
        configMetadata.put(uniqueNameKey, uniqueName);
        configMetadata.put(configURLKey, configURL);
        configMetadata.put(versionKey, version);
        configMetadata.put(modelLabelKey, modelLabel);

        if (author != ShapeModelType.CUSTOM)
		{
	        String[] presentStrings = new String[presentInVersion.length];
	        int i=0;
	        for (Mission mission : presentInVersion) { presentStrings[i++] = mission.getHashedName(); }
	        configMetadata.put(presentInVersionKey, presentStrings);

	        if (defaultFor != null)
	        {
		        String[] defaultStrings = new String[defaultFor.length];
		        i=0;
		        for (Mission mission : defaultFor) { defaultStrings[i++] = mission.getHashedName(); }
		        configMetadata.put(defaultForKey, defaultStrings);

	        }
		}
        return configMetadata;
	}

	@Override
	public void retrieve(Metadata source)
	{
		type = BodyType.valueFor(source.get(typeKey));
		population = ShapeModelPopulation.valueFor(source.get(populationKey));
		system = source.hasKey(systemKey) ? ShapeModelBody.valueFor(source.get(systemKey)) : null;
		body = ShapeModelBody.valueFor(source.get(bodyKey));
		dataUsed = ShapeModelDataUsed.valueFor(source.get(dataUsedKey));
		author = ShapeModelType.provide(source.get(authorKey)); // creates if necessary.
		shapeModelName = source.get(shapeModelNameKey);
		uniqueName = source.get(uniqueNameKey);
		configURL = source.get(configURLKey);
		version = source.get(versionKey);
		modelLabel = source.get(modelLabelKey);
		if (source.hasKey(presentInVersionKey))
		{
			String[] presentStrings = source.get(presentInVersionKey);
			int i=0;
			presentInVersion = new Mission[presentStrings.length];
			for (String present : presentStrings)
			{
				presentInVersion[i++] = Mission.getMissionForName(present);
			}
		}
		if (source.hasKey(defaultForKey))
		{
			String[] defaultStrings = source.get(defaultForKey);
			int i=0;
			defaultFor = new Mission[defaultStrings.length];
			for (String defaultStr : defaultStrings)
			{
				defaultFor[i++] = Mission.getMissionForName(defaultStr);
			}

		}

		if (author != ShapeModelType.CUSTOM)
		{
			for (Mission presentMission : presentInVersion)
			{
				if ((presentMission == edu.jhuapl.sbmt.client2.SbmtMultiMissionTool.getMission()) || (edu.jhuapl.sbmt.client2.SbmtMultiMissionTool.getMission() == Mission.APL_INTERNAL_NIGHTLY))
				{
					enabled = true;
					break;
				}
				else
					enabled = false;
			}
            if (SmallBodyViewConfig.getDefaultModelName() == null)
            {
                for (Mission defaultMission : defaultFor)
                {
                    if (defaultMission == edu.jhuapl.sbmt.client2.SbmtMultiMissionTool.getMission())
                    {
                        SmallBodyViewConfig.setDefaultModelName(uniqueName);
                        break;
                    }
                }
            }
		}
	}

	public Mission[] getPresentInVersion()
	{
		return presentInVersion;
	}

	public Mission[] getDefaultFor()
	{
		return defaultFor;
	}

	public String getUniqueName()
	{
		return uniqueName;
	}

	public boolean isEnabled()
	{
		return enabled;
	}

	public String getConfigURL()
	{
		return configURL;
	}

	public ShapeModelType getAuthor()
	{
		return author;
	}

	public ShapeModelBody getBody()
	{
		return body;
	}

	public String getVersion()
	{
		return version;
	}

	public String getShapeModelName()
	{
		return shapeModelName;
	}

	public BodyType getType()
	{
		return type;
	}

	public ShapeModelDataUsed getDataUsed()
	{
		return dataUsed;
	}

	public String getModelLabel()
	{
		return modelLabel;
	}

	public ShapeModelPopulation getPopulation()
	{
		return population;
	}

	public ShapeModelBody getSystem()
	{
		return system;
	}

	@Override
	public String toString()
	{
		return "BasicConfigInfo [population=" + population + ", system=" + system + ", shapeModelName=" + shapeModelName + ", uniqueName="
				+ uniqueName + ", author=" + author + ", type=" + type + ", body=" + body + ", dataUsed=" + dataUsed
				+ ", configURL=" + getConfigURL() + ", version=" + version + ", modelLabel=" + modelLabel
				+ ", presentInVersion=" + Arrays.toString(presentInVersion) + ", defaultFor="
				+ Arrays.toString(defaultFor) + ", enabled=" + enabled + "]";
	}
}