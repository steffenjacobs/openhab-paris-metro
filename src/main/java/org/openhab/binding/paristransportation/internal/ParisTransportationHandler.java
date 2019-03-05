/**
 * Copyright (c) 2014,2019 by the respective copyright holders.
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.paristransportation.internal;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.ExecutionException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ParisTransportationHandler} is responsible for handling commands,
 * which are sent to one of the channels.
 *
 * @author Steffen Jacobs - Initial contribution
 */
@NonNullByDefault
public class ParisTransportationHandler extends BaseThingHandler {

	private final Logger logger = LoggerFactory.getLogger(ParisTransportationHandler.class);

	@Nullable
	private TransportationStorage transportationStorage;

	@Nullable
	private ParisTransportationConfiguration config;

	public ParisTransportationHandler(Thing thing) {
		super(thing);
	}

	@Override
	public void handleCommand(ChannelUID channelUID, Command command) {
		if (ParisTransportationBindingConstants.REFRESH_CHANNEL.equals(channelUID.getId())) {
			if (command instanceof RefreshType) {
				try {

					Collection<String> arrivals = transportationStorage.getArrivals();

					StringBuilder sb = new StringBuilder();
					for (String arrival : arrivals) {
						sb.append(arrival);
						sb.append("\n");
					}
					updateState(ParisTransportationBindingConstants.OUTPUT_CHANNEL, new StringType(sb.toString()));
				} catch (IOException | InterruptedException | ExecutionException e) {
					updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
				}
			}
		}
	}

	@Override
	public void initialize() {
		logger.debug("Start initializing!");
		config = getConfigAs(ParisTransportationConfiguration.class);
		updateStatus(ThingStatus.UNKNOWN);

		// Example for background initialization:
		scheduler.execute(() -> {
			if (config == null) {
				updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Invalid configuration");
			} else if (config.line == null || config.line.isEmpty()) {
				updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Invalid configuration for metro line");
			} else if (config.direction == null || config.direction.isEmpty()) {
				updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Invalid configuration for metro line direction");
			} else if (config.station == null || config.station.isEmpty()) {
				updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Invalid configuration for metro station");
			} else {
				try {
					transportationStorage = new TransportationStorage(config.line, config.direction, config.station);
					updateStatus(ThingStatus.ONLINE);
				} catch (IOException | InterruptedException | ExecutionException e) {
					updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
					e.printStackTrace();
				}
			}
		});

		logger.debug("Finished initializing!");
	}
}
