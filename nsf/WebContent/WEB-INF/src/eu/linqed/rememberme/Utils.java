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

import java.util.Date;

import javax.faces.context.FacesContext;
import lotus.domino.*;

public class Utils {

	//resolve a variable from the current facesContext
	public static Object resolveVariable( String name ) {
		
		FacesContext context = FacesContext.getCurrentInstance();
		return context.getApplication().getVariableResolver().resolveVariable( context, name );
   }
	
	public static Session getSessionAsSigner() {
		return (Session) resolveVariable("sessionAsSigner");
	}
	public static Session getSession() {
		return (Session) resolveVariable("session");
	}
	
	public static Database getCurrentDatabase() {
		return (Database) resolveVariable("database");
	}
	
	//returns a handle to the current database using a sessionAsSigner
	public static Database getCurrentDatabaseAsSigner() throws NotesException {
		Database db = Utils.getCurrentDatabase();
		return getSessionAsSigner().getDatabase(db.getServer(), db.getFilePath());
	}
	
	public static Date toJavaDateSafe( DateTime dt ) {
		Date date = null;
		if (dt != null) {
			try {
				date = dt.toJavaDate();
			} catch (NotesException ne) {
				// do nothing
			} catch (Throwable t) {
				t.printStackTrace();
			} finally {
				try {
					dt.recycle();
				} catch (NotesException nex) { }
			}
		}
		return date;
	}

}
