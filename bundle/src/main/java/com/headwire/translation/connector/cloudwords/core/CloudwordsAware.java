package com.headwire.translation.connector.cloudwords.core;

import com.cloudwords.api.client.CloudwordsCustomerClient;

public interface CloudwordsAware {
	public CloudwordsCustomerClient getClient();
}
