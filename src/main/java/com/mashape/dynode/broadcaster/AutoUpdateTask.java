package com.mashape.dynode.broadcaster;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mashape.dynode.broadcaster.io.ServerLauncher;
import com.mashape.unirest.http.HttpMethod;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

public class AutoUpdateTask implements Runnable {

	private static final Logger LOG = LoggerFactory.getLogger(AutoUpdateTask.class);
	private ServerLauncher serverLauncher;
	
	public AutoUpdateTask(ServerLauncher serverLauncher) {
		this.serverLauncher = serverLauncher;
	}

	@Override
	public void run() {
		LOG.info("Autoupdating..");
		
		String url = DynodeConfiguration.getServersAutoupdateUrl();
		Map<String, Object> parameters = new HashMap<String, Object>();
		String rawParameters = DynodeConfiguration.getServersAutoupdateParameters();
		if (StringUtils.isNotBlank(rawParameters)) {
			String[] rawParametersParts = rawParameters.split("&");
			for(String rawParametersPart : rawParametersParts) {
				String[] parts = rawParametersPart.split("=");
				if (parts.length == 2) {
					parameters.put(parts[0], parts[1]);
				} else {
					throw new RuntimeException("Wrong auto-update parameters format");
				}
			}
		}
		
		HttpMethod method = DynodeConfiguration.getServersAutoupdateMethod();
		try {
			HttpResponse<JsonNode> response = null;
			switch (method) {
			case GET:
				response = Unirest.get(url).fields(parameters).asJson();
				break;
			case POST:
				response = Unirest.post(url).fields(parameters).asJson();
				break;
			case PUT:
				response = Unirest.put(url).fields(parameters).asJson();
				break;
			case PATCH:
				response = Unirest.patch(url).fields(parameters).asJson();
				break;
			case DELETE:
				response = Unirest.delete(url).fields(parameters).asJson();
				break;
			default:
				break;
			}

			if (response.getCode() == 200) {
				JSONArray servers = response.getBody().getArray();
				for(int i=0;i<servers.length();i++) {
					String server = servers.getString(i);
					InetSocketAddress address = DynodeConfiguration.getAddress(server);
					if (address != null) {
						serverLauncher.getBackendServerManager().addServer(address);
					}
				}
			} else {
				LOG.error("The auto update URL returned " + response.getCode());
			}
			
		} catch (UnirestException | JSONException e) {
			LOG.error("Exception", e);
		}
		
	}

}
