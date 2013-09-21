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

import java.io.IOException;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.DatatypeConverter;

import org.apache.shiro.web.servlet.SimpleCookie;

import lotus.domino.NotesException;
import lotus.domino.ViewEntry;
import lotus.domino.View;
import lotus.domino.ViewEntryCollection;

import com.ibm.designer.runtime.directory.DirectoryUser;
import com.ibm.xsp.designer.context.XSPContext;

import com.developi.openntf.LtpaGenerator;


import com.ibm.xsp.webapp.XspHttpServletResponse;

public class RememberMe implements Serializable {

	private static final long serialVersionUID = 4616493807945084615L;
	
	private Configuration config;
	
	public RememberMe() {
		config = Configuration.get();
	}

	/*
	 * This function is called from the "validate" XPage. Users
	 * who already have a "Remember me"-cookie are automatically
	 * redirected from the login screen to that XPage. The key in
	 * the cookie is compared to the hash of that key that is stored in
	 * the current database. If the key is correct, the user is automatically
	 * logged in (by sending him an LTPA token) and a new key is generated. 
	 */	
	@SuppressWarnings("unchecked")
	public void validateRememberMeCookie() {

		try {
			
			log("validate remember me cookie");
			
			FacesContext context = FacesContext.getCurrentInstance();
			ExternalContext exContext = context.getExternalContext();
			Map<String, Cookie> cookies = exContext.getRequestCookieMap();
			Map paramMap = exContext.getRequestParameterMap();
			XspHttpServletResponse response = (XspHttpServletResponse) exContext.getResponse();
			
			String redirectTo = (String) paramMap.get("to");
			if (redirectTo == null) {		//redirect to server root if "to" parameter not found
				redirectTo = "/";
			}
			
			if ( config.isConfigLoaded() && User.get().isAllowedIp() ) {
				
				if (User.get().isLtpaTokenSet()) {
					
					//if the configuration is correct: this shouldn't happen
					System.out.println("(rememberMe) Error: LtpaToken was set already but not accepted: check your configuration");
					invalidateCookie(response);
					
				} else {
			
					Cookie rememberMe = cookies.get(config.getCookieName());
					
					Token tokenDoc = isRememberMeCookieValid(rememberMe);
					
					if (tokenDoc != null) {
						
						// generate and add ltpa token to start a session for this user
						setLtpaCookie(response, tokenDoc.getUserName());
						
						User.get().setLtpaTokenSet(true);
						
						// update the rememberMe token/ cookie cookie and redirect the user
						log("set remember me cookie");
						setRememberMeCookie(response, tokenDoc.getUserName(), tokenDoc);
						
					} else {
						
						invalidateCookie(response);
						
					}
				}
				
			} else {
				
				System.out.println("(rememberMe) config not loaded or not allowed: Auto Logins feature disabled");

			}
			
			// redirect the user to the requested page
			log("redirect user to: " + redirectTo);
			response.sendRedirect(redirectTo);

		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	//returns null or a valid Token
	private Token isRememberMeCookieValid( Cookie rememberMeCookie ) throws NotesException {
		
		if (rememberMeCookie == null) {
			log("remember me cookie not found");
			return null;
		}
			
		if (rememberMeCookie.getValue().length() == 0 || rememberMeCookie.getValue().indexOf(":") == -1) {
			
			//remember me cookie has an invalid syntax: remove the invalid cookie and abort
			log("remember me cookie invalid");
			return null;
		}
			
		// retrieve and validate remember me cookie
		String[] cookieValues = rememberMeCookie.getValue().split(":");
		String tokenId = cookieValues[0];
		String token = cookieValues[1];
				
		log("remember me cookie found, token id: " + tokenId + ", token: " + token);
	
		if (tokenId.length()==0 || token.length()==0) {
			
			//remember me cookie has an invalid syntax: remove the invalid cookie and abort
			log("remember me cookie invalid");
			return null;
			
		}
					
		//check if the supplied token can be found in the database and is valid
		return getValidToken(tokenId, token, config.isDebug());
		
	}

	
	//invalidate the remember me cookie
	private void invalidateCookie( XspHttpServletResponse response ) {
		
		log("removing remember me cookie");
		
		Cookie rememberMe = new javax.servlet.http.Cookie( config.getCookieName(), "");
		rememberMe.setPath("/");
		rememberMe.setDomain( config.getSsoDomain());
		rememberMe.setMaxAge(0);
		
		response.addCookie(rememberMe);
	}
	
	/*
	 * This function is called from a page that is opened when a user has
	 * succesfully logged in, enabled the "Remember me" option and doesn't
	 * have a "Remember me" cookie yet.
	 * 
	 * It generates a random key and sends that as a cookie to the user. A
	 * hash of this key is stored in the curent database and validate upon
	 * next logins.
	 */
	private void setRememberMeCookie( XspHttpServletResponse response, String userName, Token tokenDoc ) {
		
		//create new token document
		if (tokenDoc == null) {
			
			log("saving new token document for " + userName);
			
			// calculate valid until date
			Date validUntil = new Date();

			Calendar cal = Calendar.getInstance();
			cal.setTime(validUntil);
			cal.add(Calendar.DATE, config.getRememberMeDays());
			
			tokenDoc = new Token(userName, cal);
		}
		
		String token = generateToken();
		tokenDoc.save(token);
		
		int maxAge = config.getRememberMeDays() * (60 * 60 * 24);
		
		if (tokenDoc != null) {		//date specified: calculate the expiration date for this cookie
			maxAge =  (int) (( tokenDoc.getValidUntil().getTime() - ( new Date()).getTime() ) / 1000);
		}
		
		try {
			
			log("create simple cookie");
			SimpleCookie c = new SimpleCookie(config.getCookieName());
			c.setValue(tokenDoc.getTokenId() + ":" + token);
			c.setPath("/");
			c.setDomain(config.getSsoDomain());
			c.setMaxAge(maxAge);
			c.setSecure(true);
			
			//response.addCookie( c);
			
/*	Cookie rememberMe = new Cookie(config.getCookieName(), tokenDoc.getTokenId() + ":" + token);
			rememberMe.setPath("/");
			rememberMe.setDomain(config.getSsoDomain());
			rememberMe.setMaxAge( maxAge);

			// add the cookies to the response
			response.addCookie(rememberMe);*/
			
			log("add simple cookie");
			
			c.saveTo(null, response);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	//convenience method to set a remember me cookie for the current user
	@SuppressWarnings("unchecked")
	public void setRememberMeCookie() {
		
		try {
			
			log("set remember me cookie");
			
			FacesContext context = FacesContext.getCurrentInstance();
			ExternalContext exContext = context.getExternalContext();
			XspHttpServletResponse response = (XspHttpServletResponse) exContext.getResponse();
			Map paramMap = exContext.getRequestParameterMap();
			
			String redirectTo = (String) paramMap.get("to");
			if (redirectTo == null) {
				redirectTo = "/";
			}
			
			if (config.isConfigLoaded()  && User.get().isAllowedIp()) {
			
				DirectoryUser dirUser = XSPContext.getXSPContext(FacesContext.getCurrentInstance()).getUser();
				String userName = dirUser.getDistinguishedName();
				this.setRememberMeCookie(response, userName, null);
					
			} else {
					
				System.out.println("(rememberMe) config not loaded or not allowed: Auto Logins feature disabled");
					
			}
			
			// redirect the user to the requested page
			log("redirect user to: " + redirectTo);
			response.sendRedirect(redirectTo);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	//validate the token from the remember me cookie
	//returns null or a valid token
	private Token getValidToken(String tokenId, String cookieToken, boolean debug) throws NotesException {

		log("check for valid token for token id " + tokenId);
		
		// open tokens view as signer and get all tokens with the specified token id 
		View vwTokens = Utils.getCurrentDatabaseAsSigner().getView(Configuration.VIEW_TOKENS_BY_ID);
		ViewEntryCollection vecTokens = vwTokens.getAllEntriesByKey(tokenId, true);
		
		log("found " + vecTokens.getCount() + " token documents");

		Token tokenDoc = null;
		boolean valid = false;
		
		ViewEntry veTmp;
		ViewEntry veToken = vecTokens.getFirstEntry();
		while (veToken != null && !valid) {
			
			tokenDoc = new Token( veToken.getUniversalID() );
			log("validating token:" + tokenDoc.toString());
			
			valid = (!tokenDoc.isExpired() && tokenDoc.isValidToken(cookieToken) );
			
			veTmp = vecTokens.getNextEntry(veToken);
			veToken.recycle();
			veToken = veTmp;
		}
		
		vecTokens.recycle();
		vwTokens.recycle();
		
		if (valid) {
			log("valid token found (id: " + tokenId + ", username: " + tokenDoc.getUserName() + ")");
			return tokenDoc;
		} else {
			log("no valid token found");
			return null;
		}

	}

	private void setLtpaCookie(XspHttpServletResponse response, String userName) throws NotesException {
		
		log("generate Ltpa cookie (for " + userName + ")");

		SsoConfiguration ssoConfig = new SsoConfiguration( config.getSsoConfigUnid() );

		LtpaGenerator ltpaGenerator = new LtpaGenerator( ssoConfig.getLtpaDominoSecret(), ssoConfig.getTokenExpiration());			 
		String token = ltpaGenerator.generateLtpaToken(userName);
		
		log("Ltpa token generated (for " + userName + "): " + token);
				 
		// set the cookie
		Cookie cookieLtpa = new Cookie("LtpaToken", token);
		cookieLtpa.setPath("/");
		cookieLtpa.setDomain(config.getSsoDomain());
		
		
		response.addCookie(cookieLtpa);
		log("Ltpa cookie added");

	}

	// generate 128 bits random key used as a token for this user
	private String generateToken() {

		log("generate random token");
		
		byte[] aesKey = new byte[16]; // 16 bytes, 128 bits random string
		config.getSecureRandom().nextBytes(aesKey);
		
		String key = DatatypeConverter.printBase64Binary(aesKey);

		log("token generated: " + key);
		
		return key;
	}
	
	private void log(String message) {
		if (config.isDebug()) {
			System.out.println("(rememberMe) " + message);
		}
	}

}
