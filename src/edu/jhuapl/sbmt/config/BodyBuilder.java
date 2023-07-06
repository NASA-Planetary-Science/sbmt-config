package edu.jhuapl.sbmt.config;

import edu.jhuapl.sbmt.core.body.SmallBodyModel;

@FunctionalInterface
public interface BodyBuilder<BodyViewConfig> 
{
	SmallBodyModel buildBody(BodyViewConfig config);
}
