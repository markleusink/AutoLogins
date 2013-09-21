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

import java.io.Serializable;
import java.security.SecureRandom;
import java.util.Vector;

import lotus.domino.NotesException;
import lotus.domino.Document;

public class Configuration implements Serializable {

	private static final long serialVersionUID = -4501332639738999054L;

	private static final String BEAN_NAME = "alConfigBean";
	
	public static final String ROLE_ADMIN = "[admin]";
	public static final String VIEW_TOKENS_BY_ID = "vwTokensById";
	public static final String DIRECTORY_DB_PATH = "names.nsf";
	
	private boolean debug;
	private boolean configLoaded;
	
	private String cookieName;
	private int rememberMeDays;
	private String ssoConfigName;
	private String ssoOrganisationName;
	private String ssoDomain;
	private String ssoConfigUnid;
	
	private Vector<String> allowedIPPatterns;
	
	private String unid;
	
	private SecureRandom secRandom;
	
	public Configuration() {
		
		allowedIPPatterns = new Vector<String>();
		this.reload();
		
	}

	@SuppressWarnings("unchecked")
	public void reload() {
		
		try {
			
			System.out.println("(rememberMe) (re)loading configuration...");
			
			this.configLoaded = false;
			
			//read configuration document from the current database (as signer)
			Document docConfig;
			
			if (unid != null) {
				docConfig = Utils.getCurrentDatabaseAsSigner().getDocumentByUNID(unid);
			} else {
				docConfig = Utils.getCurrentDatabaseAsSigner().getView("vwConfig").getFirstDocument();
			}
			
			if (null != docConfig) {
				
				unid = docConfig.getUniversalID();
				cookieName = docConfig.getItemValueString("cookieName");
				rememberMeDays = docConfig.getItemValueInteger("rememberMeDays");
				ssoConfigName = docConfig.getItemValueString("ssoConfigName");
				ssoOrganisationName = docConfig.getItemValueString("ssoOrganisationName");
				
				allowedIPPatterns.clear();
				if (docConfig.hasItem("allowedIPPatterns") && docConfig.getItemValueString("allowedIPPatternsEnabled").equals("1") ) {
					allowedIPPatterns = docConfig.getItemValue("allowedIPPatterns");
				}
				
				debug = docConfig.getItemValueString("debug").equals("1");
				
				if (debug) {
					System.out.println("(rememberMe) debug mode ENABLED");
					System.out.println("(rememberMe) - cookie name: " + cookieName);
					System.out.println("(rememberMe) - remember me days: " + rememberMeDays);
					System.out.println("(rememberMe) - IP restrictions: " + (allowedIPPatterns.size()==0 ? "disabled" : "enabled with " + allowedIPPatterns.size() + " ranges"));
					System.out.println("(rememberMe) - sso config name: " + ssoConfigName);
					System.out.println("(rememberMe) - sso organisation name: " + ssoOrganisationName);

				}
				
				//we could have stored the entire sso config here, but that would also store the secret in memory
				SsoConfiguration ssoConfig = new SsoConfiguration( ssoConfigName, ssoOrganisationName );
				this.ssoDomain = ssoConfig.getTokenDomain();
				this.ssoConfigUnid = ssoConfig.getUnid();
				
				if (debug) {
					System.out.println("(rememberMe) - sso config:");
					System.out.println( ssoConfig.toString() );
				}
				
				//initialize secure random object
				this.secRandom = new SecureRandom();
				
				this.configLoaded = true;
				
				docConfig.recycle();
				
				System.out.println("(rememberMe) configuration loaded successfully");
				
			} else {
				
				System.out.println("(rememberMe) Error: configuration document not found, auto logins disabled");
				
			}
			
		} catch (NotesException e) {
			System.out.println("(rememberMe) error while reading configuration: ");
			e.printStackTrace();
		}
		
	}
	
	//access to the configuration bean
	public static Configuration get() {
		return (Configuration) Utils.resolveVariable(BEAN_NAME);
	}

	public String getCookieName() {
		return cookieName;
	}
	public int getRememberMeDays() {
		return rememberMeDays;
	}
	public String getSsoConfigName() {
		return ssoConfigName;
	}
	public String getSsoOrganisationName() {
		return ssoOrganisationName;
	}
	public String getSsoDomain() {
		return ssoDomain;
	}
	public String getSsoConfigUnid() {
		return ssoConfigUnid;
	}
	public boolean isDebug() {
		return debug;
	}
	public SecureRandom getSecureRandom() {
		return secRandom;
	}
	public boolean isConfigLoaded() {
		return configLoaded;
	}
	
	public Vector<String> getAllowedIPPatterns() {
		return allowedIPPatterns;
	}
	
	public String getUnid() {
		return unid;
	}
}
