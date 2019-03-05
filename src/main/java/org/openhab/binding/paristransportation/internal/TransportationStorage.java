package org.openhab.binding.paristransportation.internal;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;

/** @author Steffen Jacobs */
public class TransportationStorage {

	private static final Logger LOG = LoggerFactory.getLogger(TransportationStorage.class);

	/** all existing lines */
	private Map<String, MetroLine> linesByNameFromJson;
	/** all directions for a given line */
	private Map<String, LineDirection> lineDirectionsByNameFromJson;
	/** all stops for a given line */
	private Map<String, StationDTO> stationsByNameFromJson;

	private final String lineId;

	private final String stationId;

	private final String directionId;

	public TransportationStorage(String line, String direction, String station) throws ClientProtocolException, IOException, InterruptedException, ExecutionException {
		
		//get line id
		CompletableFuture<String> lineIdFromLineName = getLineIdFromLineName(line);
		lineId = lineIdFromLineName.get();
		
		//get direction and station for line id
		CompletableFuture<String> directionIdForLine = getDirectionIdForLine(lineId, direction);
		CompletableFuture<String> stationByIdForLine = getStationByIdForLine(lineId, station);
		directionId = directionIdForLine.get();
		stationId = stationByIdForLine.get();
		
		//get trips from station for line in direction 
//		CompletableFuture<Collection<String>> nextTripsForLineAndStationAndWay = getNextTripsForLineAndStationAndWay(lineId, stationId, directionId);
//		Collection<String> collection = nextTripsForLineAndStationAndWay.get();
//		
//		for(String s : collection) {
//			LOG.info(s);
//		}
	}
	
	public Collection<String> getArrivals() throws ClientProtocolException, InterruptedException, ExecutionException, IOException{
		return getNextTripsForLineAndStationAndWay(lineId, stationId, directionId).get();
		
	}

	private CompletableFuture<String> getLineIdFromLineName(String linename) throws ClientProtocolException, IOException {
		CompletableFuture<String> future = new CompletableFuture<>();
		String url = "http://restratpws.azurewebsites.net/api/lines/metro";
		if (linesByNameFromJson == null) {
			Request.Get(url).execute().handleResponse(new ResponseHandler<Response>() {

				@Override
				public Response handleResponse(HttpResponse r) throws ClientProtocolException, IOException {
					LOG.info("Requested lines - {}", r.getStatusLine().getStatusCode());
					String msg = EntityUtils.toString(r.getEntity());
					linesByNameFromJson = getLinesByNameFromJson(msg);
					completeWithLineIdByName(linename, future);
					return null;
				}
			});
		} else {
			completeWithLineIdByName(linename, future);
		}
		return future;
	}

	private CompletableFuture<String> getDirectionIdForLine(String lineId, String direction) throws ClientProtocolException, IOException {
		CompletableFuture<String> future = new CompletableFuture<>();
		String url = "http://restratpws.azurewebsites.net/api/directions/" + lineId;
		if (lineDirectionsByNameFromJson == null) {
			Request.Get(url).execute().handleResponse(new ResponseHandler<Response>() {
				@Override
				public Response handleResponse(HttpResponse r) throws ClientProtocolException, IOException {
					LOG.info("Requested line directions - {}", r.getStatusLine().getStatusCode());
					String msg = EntityUtils.toString(r.getEntity());
					lineDirectionsByNameFromJson = getLineDirectionsByNameFromJson(msg);
					completeWithDirectionIdByName(direction, future);
					return null;
				}
			});
		} else {
			completeWithDirectionIdByName(direction, future);
		}
		return future;
	}

	private CompletableFuture<String> getStationByIdForLine(String lineId, String stationName) throws ClientProtocolException, IOException {
		CompletableFuture<String> future = new CompletableFuture<>();
		String url = "http://restratpws.azurewebsites.net/api/stations/" + lineId;
		if (stationsByNameFromJson == null) {
			Request.Get(url).execute().handleResponse(new ResponseHandler<Response>() {
				@Override
				public Response handleResponse(HttpResponse r) throws ClientProtocolException, IOException {
					LOG.info("Requested line directions - {}", r.getStatusLine().getStatusCode());
					String msg = EntityUtils.toString(r.getEntity());
					stationsByNameFromJson = getStationByNameFromJson(msg);
					completeWithStationIdByName(stationName, future);
					return null;
				}
			});
		} else {
			completeWithStationIdByName(stationName, future);
		}
		return future;
	}

	private CompletableFuture<Collection<String>> getNextTripsForLineAndStationAndWay(String lineId, String stationId, String directionId) throws ClientProtocolException, IOException {
		CompletableFuture<Collection<String>> future = new CompletableFuture<>();
		String url = "http://restratpws.azurewebsites.net/api/missions/" + lineId + "/from/" + stationId + "/way/" + directionId;
		Request.Get(url).execute().handleResponse(new ResponseHandler<Response>() {
			@Override
			public Response handleResponse(HttpResponse r) throws ClientProtocolException, IOException {
				LOG.info("Requested trips on line {} from station {} in direction {} - {}", lineId, stationId, directionId, r.getStatusLine().getStatusCode());
				String msg = EntityUtils.toString(r.getEntity());
				future.complete(getStringListFromJson(msg));
				return null;
			}
		});
		return future;
	}

	private void completeWithStationIdByName(String stationName, CompletableFuture<String> future) {
		StationDTO station = stationsByNameFromJson.get(stationName);
		if (station != null) {
			future.complete(station.getId());
		} else {
			future.completeExceptionally(new RuntimeException("Station by name not found."));
		}
	}

	private void completeWithDirectionIdByName(String linename, CompletableFuture<String> future) {
		LineDirection lineDirection = lineDirectionsByNameFromJson.get(linename);
		if (lineDirection != null) {
			future.complete(lineDirection.getWay());
		} else {
			future.completeExceptionally(new RuntimeException("Line Direction by name not found."));
		}
	}

	private void completeWithLineIdByName(String linename, CompletableFuture<String> future) {
		MetroLine metroLine = linesByNameFromJson.get(linename);
		if (metroLine != null) {
			future.complete(metroLine.getId());
		} else {
			future.completeExceptionally(new RuntimeException("Line by name not found."));
		}
	}

	private Map<String, MetroLine> getLinesByNameFromJson(String json) throws JsonParseException, JsonMappingException, IOException {
		final ObjectMapper mapper = new ObjectMapper();
		final CollectionType javaType = mapper.getTypeFactory().constructCollectionType(List.class, MetroLine.class);
		Collection<MetroLine> lines = mapper.readValue(json, javaType);

		final Map<String, MetroLine> map = new HashMap<>();
		for (MetroLine m : lines) {
			map.put(m.getShortName(), m);
		}
		LOG.info("Successfully parsed {} lines.", lines.size());
		return map;
	}

	private Map<String, LineDirection> getLineDirectionsByNameFromJson(String json) throws JsonParseException, JsonMappingException, IOException {
		final ObjectMapper mapper = new ObjectMapper();
		final CollectionType javaType = mapper.getTypeFactory().constructCollectionType(List.class, LineDirection.class);
		Collection<LineDirection> directions = mapper.readValue(json, javaType);

		final Map<String, LineDirection> map = new HashMap<>();
		for (LineDirection ld : directions) {
			map.put(ld.getName(), ld);
		}
		LOG.info("Successfully parsed {} lines.", directions.size());
		return map;
	}

	private Map<String, StationDTO> getStationByNameFromJson(String json) throws JsonParseException, JsonMappingException, IOException {
		final ObjectMapper mapper = new ObjectMapper();
		final CollectionType javaType = mapper.getTypeFactory().constructCollectionType(List.class, StationDTO.class);
		Collection<StationDTO> stations = mapper.readValue(json, javaType);

		final Map<String, StationDTO> map = new HashMap<>();
		for (StationDTO s : stations) {
			map.put(s.getName(), s);
		}
		LOG.info("Successfully parsed {} stations.", stations.size());
		return map;
	}
	private Collection<String> getStringListFromJson(String json) throws JsonParseException, JsonMappingException, IOException {
		final ObjectMapper mapper = new ObjectMapper();
		final CollectionType javaType = mapper.getTypeFactory().constructCollectionType(List.class, String.class);
		Collection<String> strings = mapper.readValue(json, javaType);
		LOG.info("Successfully parsed {} strings.", strings.size());
		return strings;
	}

}
