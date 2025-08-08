/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.http.management;

import org.springframework.integration.graph.Graph;
import org.springframework.integration.graph.IntegrationGraphServer;
import org.springframework.integration.http.config.HttpContextUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The REST Controller to provide the management API over {@link IntegrationGraphServer}.
 *
 * @author Artem Bilan
 *
 * @since 4.3
 */
@RestController
@RequestMapping(IntegrationGraphController.REQUEST_MAPPING_PATH_VARIABLE)
public class IntegrationGraphController {

	static final String REQUEST_MAPPING_PATH_VARIABLE =
			"${" + HttpContextUtils.GRAPH_CONTROLLER_PATH_PROPERTY + ":" +
					HttpContextUtils.GRAPH_CONTROLLER_DEFAULT_PATH + "}";

	private final IntegrationGraphServer integrationGraphServer;

	public IntegrationGraphController(IntegrationGraphServer integrationGraphServer) {
		this.integrationGraphServer = integrationGraphServer;
	}

	@GetMapping(name = "getGraph")
	public Graph getGraph() {
		return this.integrationGraphServer.getGraph();
	}

	@GetMapping(path = "/refresh", name = "refreshGraph")
	public Graph refreshGraph() {
		return this.integrationGraphServer.rebuild();
	}

}
