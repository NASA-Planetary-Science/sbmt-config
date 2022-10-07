package edu.jhuapl.sbmt.config;

import edu.jhuapl.sbmt.common.client.SmallBodyModel;

@FunctionalInterface
public interface BodyBuilder<BodyViewConfig> 
{
	SmallBodyModel buildBody(BodyViewConfig config);
}
