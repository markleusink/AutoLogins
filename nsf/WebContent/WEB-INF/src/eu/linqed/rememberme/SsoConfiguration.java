package eu.linqed.rememberme;

/*
 * <<
 * Auto Logins for IBM Domino/ XWork server
 * Copyright 2012 Mark Leusink - http://linqed.eu
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this 
 * file except in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF 
 * ANY KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License
 * >>
 */

import lotus.domino.Database;
import lotus.domino.Document;
import lotus.domino.NotesError;
import lotus.domino.NotesException;
import lotus.domino.View;

public class SsoConfiguration {

	private String name;
	private String unid;
	
	private String ltpaDominoSecret;
	private int tokenExpiration;
	private String tokenDomain;

	private boolean timeout;
	private int timeoutMinutes;
	
	public SsoConfiguration( String configUnid) throws NotesException {
		
		//open directory database and SSO view
		Database dbDirectory = (Database) Utils.getSessionAsSigner().getDatabase(Utils.getSession().getServerName(), Configuration.DIRECTORY_DB_PATH);
		
		Document docSsoConfig = null;
		try {
			docSsoConfig = dbDirectory.getDocumentByUNID(configUnid);
		} catch (NotesException e) { }
		
		if (docSsoConfig == null) {
			throw new NotesException(NotesError.NOTES_ERR_SSOCONFIG, "SSO config document not found.");
		}
		
		readConfig(docSsoConfig);
	
		docSsoConfig.recycle();
		
	}
	
	public SsoConfiguration( String configName, String configOrganisation ) throws NotesException {
		
		//open directory database and SSO view
		Database dbDirectory = (Database) Utils.getSessionAsSigner().getDatabase(Utils.getSession().getServerName(), Configuration.DIRECTORY_DB_PATH);
		View vwSsoConfigs = dbDirectory.getView("($WebSSOConfigs)");
		
		String key = (configOrganisation != null ? configOrganisation + ":" : "") + configName;
		
		Document docSsoConfig = vwSsoConfigs.getDocumentByKey(key, true);
		
		vwSsoConfigs.recycle();
		
		if (docSsoConfig == null) {
			throw new NotesException(NotesError.NOTES_ERR_SSOCONFIG, "SSO config document not found.");
		}
		
		readConfig(docSsoConfig);
		
		docSsoConfig.recycle();

	}
	
	//read values from SSO config document
	private void readConfig(Document docSsoConfig) throws NotesException {
		
		this.name = docSsoConfig.getItemValueString("LTPA_TokenName");
		this.unid = docSsoConfig.getUniversalID();
		
		this.ltpaDominoSecret = docSsoConfig.getItemValueString("LTPA_DominoSecret");
		this.tokenExpiration = docSsoConfig.getItemValueInteger("LTPA_TokenExpiration");
		this.tokenDomain = docSsoConfig.getItemValueString("LTPA_TokenDomain");
		this.timeout = docSsoConfig.getItemValueString("LTPA_timeout").equals("1");
		this.timeoutMinutes = docSsoConfig.getItemValueInteger("LTPA_toMinutes");
		
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		
		sb.append("SsoConfiguration[");
		sb.append("ltpaDominoSecret:" + ltpaDominoSecret + ", " );
		sb.append("tokenExpiration:" + tokenExpiration + ", " );
		sb.append("tokenDomain:" + tokenDomain + ", " );
		sb.append("timeout:" + timeout + ", " );
		sb.append("timeoutMinutes:" + timeoutMinutes );
		sb.append("]");
		
		return sb.toString();
	}

	public String getName() {
		return name;
	}
	public String getUnid() {
		return unid;
	}
	public String getLtpaDominoSecret() {
		return ltpaDominoSecret;
	}
	public int getTokenExpiration() {
		return tokenExpiration;
	}
	public String getTokenDomain() {
		return tokenDomain;
	}
	public boolean isTimeout() {
		return timeout;
	}
	public int getTimeoutMinutes() {
		return timeoutMinutes;
	}
	
}
