/**
 * © David Attias 2015
 */
package io.spacedog.utils;

import java.util.List;
import java.util.Map;

public class MailTemplate {

	public static class Reference {

		public Reference() {
		}

		public Reference(String type, String id) {
			this.type = type;
			this.id = id;
		}

		public String type;
		public String id;
	}

	public String from;
	public List<String> to;
	public List<String> cc;
	public List<String> bcc;
	public String subject;
	public String text;
	public Map<String, String> parameters;
	public Map<String, Reference> references;
}
